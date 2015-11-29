package io.github.tommsy64.satchels.test;

import java.util.Random;
import java.util.logging.Logger;

public class Utils {
    public static final Random rand = new Random();
    public static final String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890~!@#$%^&*()_+=-`[]{};':\",.<>/\\|";
    public static final Logger log = Logger.getLogger("UnitTestLogger");

    public static String generateRandomString(int length) throws Exception {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++)
            buffer.append(characters.charAt((int) rand.nextDouble() * characters.length()));
        return buffer.toString();
    }
}
