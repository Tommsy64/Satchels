package io.github.tommsy64.satchels;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.tommsy64.satchels.item.AttributeHider;
import io.github.tommsy64.satchels.item.PersistantData;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class Backpack {
    private String title;
    private String itemName;
    private List<String> lore;
    private Material material;
    private String permission;
    private int rows;
    private Recipe recipe;

    public ItemStack generateItemStack() {
        return convert(this, new ItemStack(material));
    }

    public static ItemStack convert(Backpack bp, ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(bp.itemName);
        itemMeta.setLore(bp.lore);
        itemStack.setItemMeta(itemMeta);

        PersistantData pd = PersistantData.get();
        itemStack = pd.storeData(Keys.IS_BACKPACK.key, itemStack, true);
        itemStack = pd.storeData(Keys.TITLE.key, itemStack, bp.title);
        itemStack = pd.storeData(Keys.ROWS.key, itemStack, bp.rows);
        if (bp.permission != null)
            itemStack = pd.storeData(Keys.PERMISSION.key, itemStack, bp.permission);

        itemStack = AttributeHider.removeAttributes(itemStack);
        return itemStack;
    }

    public static enum Keys {
        IS_BACKPACK("isBackpack"), TITLE("backpack-name"), CONTENTS("backpack-contents"), ROWS("backpack-size"), UUID("backpack-uuid"), PERMISSION(
                "backpack-permission");

        public final String key;

        private Keys(@NonNull String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
