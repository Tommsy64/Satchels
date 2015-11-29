package io.github.tommsy64.satchels.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import io.github.tommsy64.satchels.Satchels;
import io.github.tommsy64.satchels.item.storage.AttributeStorage;
import io.github.tommsy64.satchels.util.Base64Util;
import io.github.tommsy64.satchels.util.BukkitSerialUtil;
import io.github.tommsy64.satchels.util.RC5;
import io.github.tommsy64.satchels.util.SerialUtil;

/**
 * Creator: Tommsy64, LordLambda Date: 5/3/15 Usage: A manager of persistent Data on items.
 *
 * This class manages storing persistent data to an actual ItemStack itself. This allows for many many things. The way this works is by storing it in NBTData ,
 * this allows to us to add little to no latency on the player, and not have it be stored in a database/flat-file, etc. Since technically players can edit
 * NBTData we actually encrypt our data.
 */
public class PersistantData {

    private static PersistantData instance;
    private RC5 rc5;
    // For some reason there are an extra 24 bytes appended.
    public static final int MAX_SIZE = 32767 - 24;
    // Limitation of Minecraft networking protocol
    public static final int GLOBAL_MAX_SIZE = 2000000;

    protected PersistantData() {
        try {
            String rc5Key = Satchels.getInstance().getRc5Key();
            if (rc5Key == null || rc5Key.length() == 0) {
                Satchels.log(Level.INFO, "No RC5 key found, generated a new one!");
                rc5 = new RC5();
                Satchels.getInstance().setRc5Key(new String(rc5.baseKey));
            } else
                rc5 = new RC5(rc5Key);
            Satchels.log(Level.INFO, "Loaded RC5 engine.");
        } catch (Exception e) {
            Satchels.log(Level.SEVERE, "Error creating RC5 engine!", e);
        }
    }

    public ItemStack storeData(String key, ItemStack on, ConfigurationSerializable... data) {
        return storeData(key, on, BukkitSerialUtil.toBase64(data));
    }

    public ItemStack storeData(String key, ItemStack on, Serializable data) {
        return storeData(key, on, SerialUtil.objectToString(data));
    }

    public ItemStack storeData(String key, ItemStack on, char data) {
        return storeData(key, on, String.valueOf(data));
    }

    public ItemStack storeData(String key, ItemStack on, float data) {
        return storeData(key, on, String.valueOf(data));
    }

    public ItemStack storeData(String key, ItemStack on, double data) {
        return storeData(key, on, String.valueOf(data));
    }

    public ItemStack storeData(String key, ItemStack on, long data) {
        return storeData(key, on, String.valueOf(data));
    }

    public ItemStack storeData(String key, ItemStack on, int data) {
        return storeData(key, on, String.valueOf(data));
    }

    public ItemStack storeData(String key, ItemStack on, boolean data) {
        return storeData(key, on, String.valueOf(data));
    }

    public ItemStack storeData(String key, ItemStack on, String data) {
        return storeData(key, on, data.getBytes());
    }

    public ItemStack storeData(String key, ItemStack on, byte[] data) {
        if (on == null || on.getType() == Material.AIR)
            return on;
        if (data.length > GLOBAL_MAX_SIZE)
            throw new UnsupportedOperationException("Data length cannot be larger than " + GLOBAL_MAX_SIZE);
        byte[][] dataParts = splitData(data);
        AttributeStorage storage = null;
        for (int i = 0; i < dataParts.length; i++) {
            storage = AttributeStorage.newTarget(on, UUID.nameUUIDFromBytes(key.concat("." + (i + 1)).getBytes()));
            storage.setData(Base64Util.encode(rc5.encrypt(dataParts[i])));
        }
        return storage.getTarget();
    }

    public ConfigurationSerializable[] readSerializable(String key, ItemStack from, ConfigurationSerializable... defaultValue) {
        String str = readString(key, from);
        if (str == null)
            return defaultValue;
        return BukkitSerialUtil.fromBase64(str);
    }

    public <T extends Serializable> T readSerializable(String key, ItemStack from, T defaultValue) {
        String data = readString(key, from, null);
        if (data == null)
            return defaultValue;
        return SerialUtil.stringToObject(data);
    }

    public char readChar(String key, ItemStack from) {
        return readChar(key, from, '\u0000');
    }

    public char readChar(String key, ItemStack from, char defaultValue) {
        char[] chars = readString(key, from, String.valueOf(defaultValue)).toCharArray();
        if (chars.length > 0)
            return chars[0];
        return defaultValue;
    }

    public float readFloat(String key, ItemStack from) {
        return readFloat(key, from, 0);
    }

    public float readFloat(String key, ItemStack from, float defaultValue) {
        try {
            return Float.parseFloat(readString(key, from, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public double readDouble(String key, ItemStack from) {
        return readDouble(key, from, 0);
    }

    public double readDouble(String key, ItemStack from, double defaultValue) {
        try {
            return Double.parseDouble(readString(key, from, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public long readLong(String key, ItemStack from) {
        return readLong(key, from, 0);
    }

    public long readLong(String key, ItemStack from, long defaultValue) {
        try {
            return Long.parseLong(readString(key, from, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public int readInt(String key, ItemStack from) {
        return readInt(key, from, 0);
    }

    public int readInt(String key, ItemStack from, int defaultValue) {
        try {
            return Integer.parseInt(readString(key, from, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public boolean readBoolean(String key, ItemStack from) {
        return readBoolean(key, from, false);
    }

    public boolean readBoolean(String key, ItemStack from, boolean defaultValue) {
        return Boolean.valueOf(readString(key, from, String.valueOf(defaultValue)));
    }

    public String readString(String key, ItemStack from) {
        return readString(key, from, null);
    }

    public String readString(String key, ItemStack from, String defaultValue) {
        if (from == null || from.getType() == Material.AIR)
            return defaultValue;
        try {
            ArrayList<byte[]> data = new ArrayList<>();
            int i = 1;
            while (true) {
                UUID uuid = UUID.nameUUIDFromBytes(key.concat("." + i).getBytes());
                String dataPart = AttributeStorage.newTarget(from, uuid).getData(null);
                if (dataPart == null)
                    break;
                data.add(Base64Util.decode(dataPart.getBytes()));
                i++;
            }
            if (data.isEmpty())
                return defaultValue;
            String[] decryptedData = new String[data.size()];
            for (i = 0; i < decryptedData.length; i++)
                decryptedData[i] = new String(rc5.decrypt(data.get(i))).trim();
            StringBuilder sb = new StringBuilder();
            for (String str : decryptedData)
                sb.append(str);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    private byte[][] splitData(byte[] data) {
        if (data.length < MAX_SIZE)
            return new byte[][] { data };
        int parts = data.length / MAX_SIZE;
        if (parts * MAX_SIZE <= data.length)
            parts++;
        byte[][] dataParts = new byte[parts][0];
        if (dataParts.length == 0)
            return new byte[][] { data };
        for (int i = 0; i < dataParts.length; i++) {
            if ((i + 1) * MAX_SIZE > data.length)
                dataParts[i] = Arrays.copyOfRange(data, i * MAX_SIZE, i * MAX_SIZE + (data.length - i * MAX_SIZE));
            else
                dataParts[i] = Arrays.copyOfRange(data, i * MAX_SIZE, (i + 1) * MAX_SIZE);
        }
        return dataParts;
    }

    public static PersistantData get() {
        if (instance == null) {
            synchronized (PersistantData.class) {
                if (instance == null)
                    instance = new PersistantData();
            }
        }
        return instance.rc5 != null ? instance : null;
    }

    public static void unload() {
        instance = null;
    }
}
