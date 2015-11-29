package io.github.tommsy64.satchels.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerialUtil {

  public static String objectToString(Serializable object) {
    String encoded = null;
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(object);
      objectOutputStream.close();
      encoded = Base64Util.encode(byteArrayOutputStream.toByteArray());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return encoded;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Serializable> T stringToObject(String string) {
    T object = null;
    try {
      byte[] bytes = Base64Util.decode(string.getBytes());
      ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
      object = (T) objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException | ClassCastException e) {
      e.printStackTrace();
    }
    return object;
  }
}
