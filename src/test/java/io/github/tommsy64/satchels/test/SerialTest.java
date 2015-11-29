package io.github.tommsy64.satchels.test;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import org.junit.Test;

import io.github.tommsy64.satchels.util.SerialUtil;

public class SerialTest {
    @Test
    public void shouldMatchOriginalObject() throws Exception {
        Serializable[] objects = new Serializable[] { new String("HIAZ"), 2, new Integer(1337), 129347102347L, true, false };
        for (Serializable obj : objects) {
            String str = SerialUtil.objectToString(obj);
            Serializable output = SerialUtil.stringToObject(str);
            assertEquals(obj, output);
        }
    }
}
