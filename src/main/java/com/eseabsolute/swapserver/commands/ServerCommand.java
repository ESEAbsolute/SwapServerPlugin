package com.eseabsolute.swapserver.commands;

import com.eseabsolute.swapserver.SwapServer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

public class ServerCommand implements TabExecutor, PluginMessageListener {
    private List<String> cachedServers = new ArrayList<>();
    private final SwapServer plugin;

    public ServerCommand() {
        plugin = SwapServer.getInstance();
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
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

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return null; }
        if (args.length == 1) {
            if (cachedServers.stream().filter(s -> s.startsWith(args[0])).toList().isEmpty()) {
                requestServerList();
            }
            return cachedServers.stream()
                    .filter(s -> s.startsWith(args[0])).toList();
        }
        return Collections.emptyList();
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此命令！");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("用法: /server <服务器名>");
            return true;
        }

        String serverName = args[0];

        String permissionNode = "swapserver.server." + serverName.toLowerCase();
        if (!player.hasPermission(permissionNode)) {
            player.sendMessage("§c你没有权限传送到该服务器！");
            return true;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
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
