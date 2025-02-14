package com.eseabsolute.swapserver.commands;

import com.eseabsolute.swapserver.SwapServer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class QueueCommand implements TabExecutor, PluginMessageListener, Listener {
    private final Map<UUID, ScheduledTask> queueTasks = new HashMap<>();
    private List<String> cachedServers = new ArrayList<>();
    private final SwapServer plugin;

    public QueueCommand() {
        plugin = SwapServer.getInstance();
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        requestServerList();
        Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                task -> requestServerList(),
                0,
                30 * 1000,
                TimeUnit.MILLISECONDS
        );
    }
    private void requestServerList() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");
        Player randomPlayer = Bukkit.getOnlinePlayers().iterator().next();
        randomPlayer.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (queueTasks.containsKey(playerUUID)) {
            ScheduledTask task = queueTasks.remove(playerUUID);
            task.cancel();
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return null; }
        if (args.length == 1) {
            if (cachedServers.stream().filter(s -> s.startsWith(args[0])).toList().isEmpty()) {
                requestServerList();
            }
            List<String> ret = new ArrayList<>(cachedServers.stream()
                    .filter(s -> s.startsWith(args[0])).toList());
            ret.addFirst("~cancel");
            return ret;
        }
        return Collections.emptyList();
    }

    private void pushInQueue(Player player, String serverName) {
        UUID playerUUID = player.getUniqueId();

        ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> forwardPlayer(player, serverName),
                1,
                5 * 1000,
                TimeUnit.MILLISECONDS
        );

        queueTasks.put(playerUUID, task);
    }

    private void forwardPlayer(Player player, String serverName) {
        UUID playerUUID = player.getUniqueId();
        Player p = Bukkit.getPlayer(playerUUID);

        if (p == null || !p.isOnline()) {
            ScheduledTask task = queueTasks.remove(playerUUID);
            task.cancel();
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此命令！");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("用法: /queue <服务器名>");
            return true;
        }

        String serverName = args[0];
        UUID playerUUID = player.getUniqueId();

        if (serverName.equalsIgnoreCase("~cancel")) {
            if (queueTasks.containsKey(playerUUID)) {
                ScheduledTask task = queueTasks.remove(playerUUID);
                task.cancel();
                player.sendMessage("§a已取消排队");
            } else {
                player.sendMessage("§e你当前并未在队列中");
            }
            return true;
        }

        String permissionNode = "swapserver.server." + serverName.toLowerCase();
        if (!player.hasPermission(permissionNode)) {
            player.sendMessage("§c你没有权限传送到该服务器！");
            return true;
        }

        if (queueTasks.containsKey(playerUUID)) {
            player.sendMessage("§e你已经在队列中，请耐心等待...");
            return true;
        }

        if (!cachedServers.contains(serverName)) {
            player.sendMessage("§c该服务器不存在");
            return true;
        }

        player.sendMessage("§a开始排队前往 " + serverName + " ...");
        pushInQueue(player, serverName);
        return true;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("GetServers")) {
            String servers = in.readUTF();
            cachedServers = Arrays.asList(servers.split(", "));
        }
    }
}
