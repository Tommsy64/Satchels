package io.github.tommsy64.satchels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcstats.MetricsLite;

import io.github.tommsy64.satchels.Backpack.BackpackBuilder;
import io.github.tommsy64.satchels.item.PersistantData;
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
    private BagHandler bagHandler;

    private ArrayList<Backpack> backpacks;

    @Override
    public void onEnable() {
        instance = this;

        FileConfiguration config = getConfig();
        boolean copyDefaults = config.get("backpacks", null) == null;
        config.options().copyDefaults(copyDefaults);
        config.options().copyHeader(true);
        saveDefaultConfig();
        if (copyDefaults)
            getLogger().info("No backpacks found. Copied example backpack.");
        rc5Key = config.getString("RC5.key");
        cacheClearInterval = config.getInt("cache-clear-interval", 60 * 30);

        PersistantData pd = PersistantData.get();
        getConfig().set("RC5.key", rc5Key);
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
        bagHandler = new BagHandler();
        getServer().getPluginManager().registerEvents(bagHandler, this);
        new BukkitRunnable() {
            @Override
            public void run() {
                getLogger().log(Level.INFO, "Clearing Inventory Cache!");
                clearCache();
            }
        }.runTaskTimer(this, 20 * cacheClearInterval, 20 * cacheClearInterval);

        backpacks = new ArrayList<>();
        loadSatchels();

        SatchelsCommandExecutor ch = new SatchelsCommandExecutor();
        PluginCommand cmd = this.getCommand("satchels");
        cmd.setExecutor(ch);
        cmd.setTabCompleter(ch);
    }

    @Override
    public void onDisable() {
        bagHandler.clearCache();
        getLogger().info("Unregistering recipes...");
        int total = 0;
        for (Backpack bp : backpacks) {
            Iterator<Recipe> iter = Bukkit.recipeIterator();
            while (iter.hasNext()) {
                Recipe recipe = iter.next();
                if (recipe.getResult().equals(bp.getRecipe().getResult())) {
                    iter.remove();
                    total++;
                }
            }
        }
        getLogger().info("Unregistered " + total + " recipe" + (total != 1 ? "s" : ""));
        PersistantData.unload();
        saveConfig();
    }

    public static void reload() {
        instance.reloadConfig(); // Don't overwrite changes made to the config file
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.disablePlugin(instance);
        pm.enablePlugin(instance);
    }

    public static void clearCache() {
        instance.bagHandler.clearCache();
    }

    private void loadSatchels() {
        ConfigurationSection config = this.getConfig().getConfigurationSection("backpacks");
        if (config != null)
            for (String key : config.getKeys(false)) {
                try {
                    ConfigurationSection bpSection = config.getConfigurationSection(key);

                    BackpackBuilder builder = Backpack.builder();
                    builder.material(Material.matchMaterial(bpSection.getString("item")));
                    builder.itemName(ChatColor.translateAlternateColorCodes('&', bpSection.getString("item-name")));
                    List<String> lore = bpSection.getStringList("lore").stream().map(s -> ChatColor.translateAlternateColorCodes('&', s))
                            .collect(Collectors.toList());
                    builder.lore(lore);
                    if (bpSection.getBoolean("check-permission"))
                        builder.permission(key);
                    builder.title(ChatColor.translateAlternateColorCodes('&', bpSection.getString("title")));
                    builder.rows(bpSection.getInt("rows"));
                    Backpack backpack = builder.build();

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
                                getLogger()
                                        .warning("Broken ingredient in " + backpack.getPermission() + ": " + iKey + ": " + (m == null ? "null" : m.toString()));
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
        getLogger().info("Loaded " + backpacks.size() + " backpack" + (backpacks.size() != 1 ? "s" : "") + "!");
    }

    public static Logger log() {
        return instance.getLogger();
    }

    public static void log(Level level, String msg) {
        instance.getLogger().log(level, msg);
    }

    public static void log(Level level, String msg, Exception e) {
        instance.getLogger().log(level, msg, e);
    }
}
