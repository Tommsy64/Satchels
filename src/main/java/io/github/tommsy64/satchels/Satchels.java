package io.github.tommsy64.satchels;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcstats.MetricsLite;

import io.github.tommsy64.satchels.listeners.ItemListener;
import lombok.Getter;
import lombok.Setter;

public class Satchels extends JavaPlugin {

    @Getter
    private static Satchels instance;
    private MetricsLite metrics;

    @Getter
    @Setter
    private String rc5Key;

    private int cacheDelay;
    private ItemListener itemListener;

    @Override
    public void onEnable() {
        instance = this;

        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.options().copyHeader(true);
        saveDefaultConfig();
        rc5Key = config.getString("rc5.key");
        cacheDelay = config.getInt("cacheClearInterval", 60 * 60);

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
        itemListener = new ItemListener();
        getServer().getPluginManager().registerEvents(itemListener, this);
        new BukkitRunnable() {
            @Override
            public void run() {
                getLogger().log(Level.INFO, "Clearing Inventory Cache!");
                itemListener.clearCache();
            }
        }.runTaskTimer(this, 20 * cacheDelay, 20 * cacheDelay);
    }

    @Override
    public void onDisable() {
        itemListener.clearCache();
        saveConfig();
    }

    private void reload() {
        reloadConfig(); // Don't overwrite changes made to the config file
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.disablePlugin(this);
        pm.enablePlugin(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("help")))
            if (Permissions.HELP.check(sender)) {
                sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "reload" + ChatColor.BLUE + " - Reloads the plugin");
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "create <title> <size>" + ChatColor.BLUE
                            + " - Makes the item in your hand a satchel.");
                }
            } else
                sendMessage(sender, Messages.getNoPermission());
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (Permissions.RELOAD.check(sender)) {
                    reload();
                    sender.sendMessage(ChatColor.BLUE + "Satchels " + ChatColor.GREEN + "successfully" + ChatColor.BLUE + " reloaded!");
                } else
                    sendMessage(sender, Messages.getNoPermission());
            } else if (args[0].equalsIgnoreCase("clearCache")) {
                if (Permissions.CLEAR_CACHE.check(sender)) {
                    itemListener.clearCache();
                    if (sender instanceof Player) {
                        getLogger().log(Level.INFO, sender.getName() + " cleared the Inventory Cache!");
                        sender.sendMessage(ChatColor.BLUE + "Inventory Cache " + ChatColor.GREEN + "succesfully" + ChatColor.BLUE + " cleared!");
                    }
                } else
                    sendMessage(sender, Messages.getNoPermission());
            } else if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args[0].equalsIgnoreCase("create"))
                    if (Permissions.CREATE.check(p)) {
                        ItemStack item = p.getItemInHand();
                        if (item == null || item.getType().equals(Material.AIR))
                            p.sendMessage(ChatColor.RED + "You must be holding an item!");
                        else if (args.length > 2 && createSatchel(item, args[1], args[2])) {
                            p.sendMessage(ChatColor.GREEN + "Succefully" + ChatColor.BLUE + " turned item into a satchel!");
                        } else
                            p.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "create <title> <size>" + ChatColor.BLUE
                                    + " - Makes the item in your hand a satchel.");
                    } else
                        sendMessage(sender, Messages.getNoPermission());
                else
                    ;
            }
        }
        return true;
    }

    private boolean createSatchel(ItemStack item, String title, String sizeStr) {
        int size;
        try {
            size = Integer.parseInt(sizeStr);
        } catch (NumberFormatException nfe) {
            return false;
        }
        createSatchel(item, title, size);
        return true;
    }

    private void createSatchel(ItemStack item, String title, int size) {
        if (size % 9 != 0)
            size = (size - size % 9) + 9; // Make a multiple of 9
        size = Math.min(size, 90);
        PersistantData pd = PersistantData.get();
        pd.storeData(Keys.IS_BACKPACK.key, item, true);
        pd.storeData(Keys.TITLE.key, item, title);
        pd.storeData(Keys.SIZE.key, item, size);
        pd.storeData(Keys.UUID.key, item, UUID.randomUUID().toString());
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
