package io.github.tommsy64.satchels;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

import lombok.Getter;
import lombok.Setter;

public class Satchels extends JavaPlugin {

    @Getter
    private static Satchels instance;

    @Getter
    @Setter
    private String rc5Key;

    private MetricsLite metrics;

    @Override
    public void onEnable() {
        instance = this;

        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.options().copyHeader(true);
        saveDefaultConfig();
        rc5Key = config.getString("rc5.key");

        PersistantData pd = PersistantData.get();
        getConfig().set("rc5.key", rc5Key);
        saveConfig();

        if (pd == null) {
            Satchels.log(Level.SEVERE, ChatColor.DARK_RED + "Satchels will now disable!");
            Bukkit.getServer().getPluginManager().disablePlugin(Satchels.getInstance());
            return;
        }

        Messages.setNoPermission(config.getString("messages.noPermission", ""));

        try {
            if (metrics == null) {
                metrics = new MetricsLite(this);
                metrics.start();
            }
        } catch (IOException e) {
            getLogger().warning(ChatColor.RED + "Failed to start plugin metrics...");
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    private void reload() {
        reloadConfig(); // Don't overwrite changes made to the config file
        onDisable();
        onEnable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("help")))
            if (sender.hasPermission("satchels.help")) {
                sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "reload" + ChatColor.BLUE + " - Reloads the plugin");
            } else
                sendMessage(sender, Messages.getNoPermission());
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("satchels.reload")) {
                    reload();
                    sender.sendMessage(ChatColor.BLUE + "Satchels " + ChatColor.GREEN + "successfully" + ChatColor.BLUE + " reloaded!");
                } else
                    sendMessage(sender, Messages.getNoPermission());
            }
        }
        return true;
    }

    private void sendMessage(CommandSender sender, String msg) {
        if (!msg.isEmpty())
            sender.sendMessage(msg);
    }

    public static void log(Level level, String msg) {
        instance.getLogger().log(level, msg);
    }

    public static void log(Level level, String msg, Exception e) {
        instance.getLogger().log(level, msg, e);
    }
}
