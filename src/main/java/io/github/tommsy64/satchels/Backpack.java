package io.github.tommsy64.satchels;

import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import lombok.Data;

@Data
public class Backpack {
    private String title;
    private String itemName;
    private List<String> lore;
    private Material material;
    private String permission;
    private int rows = 1;
    private Recipe recipe;

    public ItemStack generateItemStack() {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(itemName);
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        itemStack = createSatchel(itemStack, title, rows);
        return itemStack;
    }

    public static ItemStack createSatchel(ItemStack item, String title, int rows) {
        PersistantData pd = PersistantData.get();
        item = pd.storeData(Keys.IS_BACKPACK.key, item, true);
        item = pd.storeData(Keys.TITLE.key, item, title);
        item = pd.storeData(Keys.ROWS.key, item, rows);
        item = pd.storeData(Keys.UUID.key, item, UUID.randomUUID().toString());
        return item;
    }
}
