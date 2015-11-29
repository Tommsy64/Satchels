package io.github.tommsy64.satchels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
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

    private int cacheClearInterval;
    private ItemListener itemListener;

    private ArrayList<Backpack> backpacks;

    @Override
    public void onEnable() {
        instance = this;

        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.options().copyHeader(true);
        saveDefaultConfig();
        rc5Key = config.getString("rc5.key");
        cacheClearInterval = config.getInt("cacheClearInterval", 60 * 60);

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
        }.runTaskTimer(this, 20 * cacheClearInterval, 20 * cacheClearInterval);

        backpacks = new ArrayList<>();
        loadSatchels();
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

    private void loadSatchels() {
        ConfigurationSection config = this.getConfig().getConfigurationSection("backpacks");

        for (String key : config.getKeys(false)) {
            try {
                ConfigurationSection bpSection = config.getConfigurationSection(key);

                Backpack backpack = new Backpack();
                backpack.setMaterial(Material.matchMaterial(bpSection.getString("item")));
                backpack.setItemName(ChatColor.translateAlternateColorCodes('&', bpSection.getString("itemName")));
                List<String> lore = bpSection.getStringList("lore");
                lore.forEach(string -> ChatColor.translateAlternateColorCodes('&', string));
                backpack.setLore(lore);
                backpack.setPermission(key);
                backpack.setTitle(ChatColor.translateAlternateColorCodes('&', bpSection.getString("title")));
                backpack.setRows(bpSection.getInt("rows"));

                ConfigurationSection rSection = bpSection.getConfigurationSection("recipe");
                ConfigurationSection iSection = rSection.getConfigurationSection("ingredients");
                Recipe recipe;
                boolean shaped = rSection.getBoolean("shaped");
                if (shaped) {
                    List<String> matrix = rSection.getStringList("matrix");
                    ShapedRecipe shapedRecipe = new ShapedRecipe(backpack.generateItemStack()).shape(matrix.toArray(new String[matrix.size()]));

                    for (String iKey : iSection.getKeys(false)) {
                        if (iKey.length() != 1)
                            continue;
                        try {
                            shapedRecipe.setIngredient(iKey.charAt(0), Material.matchMaterial(iSection.getString(iKey)));
                        } catch (IllegalArgumentException iae) {
                            Material m = Material.matchMaterial(iSection.getString(iKey));
                            getLogger().warning("Broken ingredient in " + backpack.getPermission() + ": " + iKey + ": " + (m == null ? "null" : m.toString()));
                        }
                    }
                    recipe = shapedRecipe;
                } else {
                    ShapelessRecipe shapelessRecipe = new ShapelessRecipe(backpack.generateItemStack());
                    for (String iKey : iSection.getKeys(false))
                        shapelessRecipe.addIngredient(Material.matchMaterial(iSection.getString(iKey)));
                    recipe = shapelessRecipe;
                }
                backpack.setRecipe(recipe);
                Bukkit.addRecipe(recipe);
                backpacks.add(backpack);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error loading a backpack!", e);
            }
        }
        getLogger().info("Loaded " + backpacks.size() + " backpack" + (backpacks.size() > 1 ? "s" : "") + "!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("help")))
            if (Permissions.HELP.check(sender)) {
                sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "reload" + ChatColor.BLUE + " - Reloads the plugin");
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "create <title> <rows>" + ChatColor.BLUE
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
                            p.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "create <title> <rows>" + ChatColor.BLUE
                                    + " - Makes the item in your hand a satchel.");
                    } else
                        sendMessage(sender, Messages.getNoPermission());
                else
                    ;
            }
        }
        return true;
    }

    public static boolean createSatchel(ItemStack item, String title, String rowsStr) {
        int size;
        try {
            size = Integer.parseInt(rowsStr);
        } catch (NumberFormatException nfe) {
            return false;
        }
        Backpack.createSatchel(item, title, size);
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
