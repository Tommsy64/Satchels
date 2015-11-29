package io.github.tommsy64.satchels;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.HashBiMap;

import io.github.tommsy64.satchels.Backpack.Keys;
import io.github.tommsy64.satchels.item.PersistantData;

public class BagHandler implements Listener {

    private HashBiMap<String, Inventory> inventoryCache = HashBiMap.create();

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType().equals(Material.AIR) || (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR))
            return;
        PersistantData pd = PersistantData.get();
        if (!pd.readBoolean(Keys.IS_BACKPACK.key, itemStack))
            return;
        String uuid = pd.readString(Keys.UUID.key, itemStack);
        if (uuid == null)
            return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String perm = pd.readString(Keys.PERMISSION.key, itemStack);
        if (perm != null && !player.hasPermission("satchels.use." + perm))
            return;
        Inventory inv = inventoryCache.get(uuid);
        if (inv == null) {
            String title = pd.readString(Keys.TITLE.key, itemStack, "Satchel");
            int size = pd.readInt(Keys.ROWS.key, itemStack, 1) * 9;
            ConfigurationSerializable[] cs = pd.readSerializable(Keys.CONTENTS.key, itemStack, new ItemStack[size]);
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < cs.length; i++)
                items[i] = (ItemStack) cs[i];

            inv = Bukkit.createInventory(player, size, title);
            inv.setContents(items);
            inventoryCache.put(uuid, inv);
        }
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        saveInventory(event.getInventory());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (inventoryCache.containsValue(event.getInventory()))
            event.setCancelled(PersistantData.get().readBoolean(Keys.IS_BACKPACK.key, event.getCurrentItem()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClickMonitor(InventoryClickEvent event) {
        saveInventory(event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        saveInventory(event.getInventory());
    }

    private void saveInventory(Inventory inv) {
        String uuid = inventoryCache.inverse().get(inv);
        if (uuid != null) {
            PersistantData pd = PersistantData.get();
            inv.getHolder().getInventory().forEach((item) -> {
                if (uuid.equals(pd.readString(Keys.UUID.key, item))) {
                    PersistantData.get().storeData(Keys.CONTENTS.key, item, inv.getContents());
                    return;
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void customRecipes(PrepareItemCraftEvent e) {
        ItemStack result = e.getRecipe().getResult();
        PersistantData pd = PersistantData.get();
        if (!pd.readBoolean(Keys.IS_BACKPACK.key, result))
            return;
        String perm = pd.readString(Keys.PERMISSION.key, result, null);
        if (perm != null && !e.getView().getPlayer().hasPermission("satchels.craft." + perm)) {
            e.getInventory().setResult(null);
            return;
        }

        result = pd.storeData(Keys.UUID.key, result, UUID.randomUUID().toString());
        e.getInventory().setResult(result);
    }

    public void clearCache() {
        this.inventoryCache.clear();
    }
}
