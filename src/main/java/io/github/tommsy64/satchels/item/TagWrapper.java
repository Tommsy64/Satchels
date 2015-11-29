package io.github.tommsy64.satchels.item;

import static io.github.tommsy64.satchels.util.ReflectionUtil.createObject;
import static io.github.tommsy64.satchels.util.ReflectionUtil.getCraftClass;
import static io.github.tommsy64.satchels.util.ReflectionUtil.getMethod;
import static io.github.tommsy64.satchels.util.ReflectionUtil.getNetClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.inventory.ItemStack;

import io.github.tommsy64.satchels.Satchels;

public class TagWrapper {
    private static final Class<?> NMSItemStack = getNetClass("ItemStack");
    private static final Class<?> NBTTagCompound = getNetClass("NBTTagCompound");
    private static final Class<?> CraftItemStack = getCraftClass("inventory.CraftItemStack");

    private static final Method hasTag = getMethod(NMSItemStack, "hasTag");
    private static final Method setTag = getMethod(NMSItemStack, "setTag", NBTTagCompound);
    private static final Method getTag = getMethod(NMSItemStack, "getTag");
    private static final Method setInt = getMethod(NBTTagCompound, "setInt", String.class, int.class);

    // Static Methods
    private static final Method asNMSCopy = getMethod(CraftItemStack, "asNMSCopy", ItemStack.class);

    // Arg1, NMS NBTTagCompound
    public static void setInt(Object tag, String tagName, int i) {
        try {
            setInt.invoke(tag, tagName, i);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            Satchels.log(Level.WARNING, "Error setting integer in TagCompound!", e);
        }
    }

    // Arg1, NMS NBTTagCompound
    public static void setTag(Object tag, ItemStack itemStack) {
        try {
            setTag(tag, asNMSCopy.invoke(null, itemStack));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            Satchels.log(Level.WARNING, "Error setting tag in ItemStack!", e);
        }
    }

    // Arg1, NMS NBTTagCompound
    // Arg2, NMS ItemStack
    public static void setTag(Object tag, Object itemStack) {
        try {
            setTag.invoke(itemStack, tag);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            Satchels.log(Level.WARNING, "Error setting tag in NMSItemStack!", e);
        }
    }

    public static Object getTag(ItemStack itemStack) {
        try {
            return getTag(asNMSCopy.invoke(null, itemStack));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            if (itemStack != null)
                Satchels.log().warning("Error getting tag for " + itemStack.toString() + " : " + e.getLocalizedMessage());
            Satchels.log(Level.WARNING, "Error getting tag for in ItemStack!", e);
            return null;
        }
    }

    // Arg1, NMS ItemStack
    // Return NMS NBTTagCompound
    public static Object getTag(Object itemStack2) {
        // NMS NBTTagCompound
        Object tag = null;
        try {
            if (hasTag.invoke(itemStack2, (Object[]) null).equals(true))
                tag = getTag.invoke(itemStack2, (Object[]) null);
            else {
                tag = createObject(NBTTagCompound);
                setTag.invoke(itemStack2, tag);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            Satchels.log().warning("Error getting tag for " + itemStack2.toString() + " : " + e.getLocalizedMessage());
        }
        return tag;
    }
}