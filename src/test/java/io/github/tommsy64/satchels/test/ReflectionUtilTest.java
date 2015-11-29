package io.github.tommsy64.satchels.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.github.tommsy64.satchels.util.ReflectionUtil;

public class ReflectionUtilTest {

    private int i = 0;
    private static long l = 0;

    @Test
    public void getField() throws Exception {
        assertEquals(ReflectionUtil.Internal.getField(getClass(), "i"), this.getClass().getDeclaredField("i"));
    }

    @Test
    public void fieldShouldBeAccesible() throws Exception {
        ReflectionUtil.Internal.getField(getClass(), "i").set(this, 1);
        assertEquals(1, i);

        ReflectionUtil.Internal.getField(getClass(), "l").set(null, 1);
        assertEquals(1, l);
    }

    @Test
    public void methodWithWrongArgsShouldBeNull() throws Exception {
        assertNull(ReflectionUtil.Internal.getMethod(getClass(), "testMethod"));
    }

    @Test
    public void methodShouldBeAccesible() throws Exception {
        assertEquals(4, ReflectionUtil.Internal.getMethod(getClass(), "testMethod", int.class).invoke(this, 2));
    }

    @SuppressWarnings("unused")
    private int testMethod(int testInt) {
        return testInt * testInt;
    }
}