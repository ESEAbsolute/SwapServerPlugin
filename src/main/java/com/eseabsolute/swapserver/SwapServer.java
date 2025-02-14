package com.eseabsolute.swapserver;

import com.eseabsolute.swapserver.commands.QueueCommand;
import com.eseabsolute.swapserver.commands.ServerCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.Objects;

public final class SwapServer extends JavaPlugin {
    private static SwapServer instance;

    @Override
    public void onEnable() {
        instance = this;
        Objects.requireNonNull(getCommand("server")).setTabCompleter(new ServerCommand());
        Objects.requireNonNull(getCommand("server")).setExecutor(new ServerCommand());
        Objects.requireNonNull(getCommand("queue")).setTabCompleter(new QueueCommand());
        Objects.requireNonNull(getCommand("queue")).setExecutor(new QueueCommand());
    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable has been invoked!");
    }

    public static SwapServer getInstance() { return instance; }
}
