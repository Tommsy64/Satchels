package io.github.tommsy64.satchels.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import io.github.tommsy64.satchels.util.RC5;

public class RC5Test {

    private static RC5 rc5;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        rc5 = new RC5();
    }

    @Test
    public void decryptShouldMatchOriginalRandom() throws Exception {
        long end = System.currentTimeMillis() + 1000; // 1 second
        int i = 0;
        while (System.currentTimeMillis() < end) {
            encryptDecrypt(Utils.generateRandomString(i));
            i++;
        }

        Utils.log.info("Completed " + i + " encrypts, decrypts, and asserts.");
    }

    @Test
    public void decryptShouldMatchOriginalCharacters() throws Exception {
        for (int i = 1; i < Utils.characters.length(); i++)
            encryptDecrypt(Utils.characters.substring(i - 1, i));
    }

    private static void encryptDecrypt(final String data) throws Exception {
        assertEquals(data, new String(rc5.decrypt(rc5.encrypt(data.getBytes()))).trim());
    }
}
