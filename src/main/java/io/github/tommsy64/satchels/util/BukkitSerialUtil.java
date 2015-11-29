package io.github.tommsy64.satchels.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import io.github.tommsy64.satchels.Satchels;

public class BukkitSerialUtil {

  /**
   * Serializes an {@link ConfigurationSerializable} array to Base64 String.
   * <p />
   * Based off of {@link #toBase64(ConfigurationSerializable)}.
   * 
   * @param items
   *            to turn into a Base64 String.
   * @return Base64 string of the items.
   * @throws IllegalStateException
   */
  public static String toBase64(ConfigurationSerializable... items) {
    byte[] data = toByteArray(items);
    if (data != null)
      return Base64Util.encode(data);
    return null;
  }

  public static byte[] toByteArray(ConfigurationSerializable... items) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

      // Write the size
      dataOutput.writeInt(items.length);

      // Save every element in the list
      for (int i = 0; i < items.length; i++) {
        dataOutput.writeObject(items[i]);
      }

      dataOutput.close();
      return outputStream.toByteArray();
    } catch (IOException e) {
      if (items != null)
    	  Satchels.log(Level.SEVERE, "Error serializing object: " + Arrays.asList(items), e);
      else
    	  Satchels.log(Level.SEVERE, "Error serializing object!", e);
    }
    return null;
  }

  /**
   * Gets an array of ConfigurationSerializable from Base64 string.
   * <p />
   * 
   * @param data
   *            Base64 string to convert to ItemStack array.
   * @return ItemStack array created from the Base64 string.
   * @throws IOException
   */
  public static ConfigurationSerializable[] fromBase64(String data) {
    return fromByteArray(Base64Util.decode(data));
  }

  public static ConfigurationSerializable[] fromByteArray(byte[] data) {
    try {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
      BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
      ConfigurationSerializable[] items = new ConfigurationSerializable[dataInput.readInt()];

      for (int i = 0; i < items.length; i++)
        items[i] = (ConfigurationSerializable) dataInput.readObject();

      dataInput.close();
      return items;
    } catch (ClassNotFoundException | IOException e) {
      Satchels.log(Level.SEVERE, "Error deserializing object!", e);
    }
    return null;
  }
}
