package io.github.tommsy64.satchels.item;

import static io.github.tommsy64.satchels.util.ReflectionUtil.getCraftClass;
import static io.github.tommsy64.satchels.util.ReflectionUtil.getMethod;
import static io.github.tommsy64.satchels.util.ReflectionUtil.getNetClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.inventory.ItemStack;

import io.github.tommsy64.satchels.Satchels;

public class AttributeHider {
    private static final Class<?> NMSItemStack = getNetClass("ItemStack");
    private static final Class<?> CraftItemStack = getCraftClass("inventory.CraftItemStack");

    // Static Methods
    private static final Method asNMSCopy = getMethod(CraftItemStack, "asNMSCopy", ItemStack.class);
    private static final Method asCraftMirror = getMethod(CraftItemStack, "asCraftMirror", NMSItemStack);

    public static ItemStack removeAttributes(ItemStack itemStack1) {
        return removeAttributes(itemStack1, 2);
    }

    /**
     * @param flag
     *            A value between 1 and 63. 1 = Enchantments, 2 = Attributes modifiers, 4 = Unbreakable, 8 = CanDestroy, 16 = CanPlaceOn, 32 = Others, such as
     *            potion effects
     */
    public static ItemStack removeAttributes(ItemStack itemStack1, int flag) {
        try {
            return (ItemStack) asCraftMirror.invoke(null, removeAttributes(asNMSCopy.invoke(null, itemStack1)));
        } catch (NullPointerException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Satchels.log().warning("Error removing attributes for item: " + itemStack1.toString() + " : " + e.getMessage());
            return itemStack1;
        }
    }

    // Arg1, NMS ItemStack
    // Returns NMS ItemStack
    private static Object removeAttributes(Object itemStack1) {
        if (itemStack1 == null)
            return itemStack1;
        try {
            Object tag = TagWrapper.getTag(itemStack1);
            TagWrapper.setInt(tag, "HideFlags", 2);
            TagWrapper.setTag(tag, itemStack1);
        } catch (NullPointerException e) {
            Satchels.log().warning("Error removing attributes for item: " + itemStack1.toString() + " : " + e.getMessage());
        }
        return itemStack1;
    }
}
