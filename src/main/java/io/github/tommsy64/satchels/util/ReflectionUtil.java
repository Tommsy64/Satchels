package io.github.tommsy64.satchels.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;

import org.bukkit.Bukkit;

import io.github.tommsy64.satchels.Satchels;

public class ReflectionUtil {

    public static Class<?> getNetClass(String className) {
        return getNetClass(className, false);
    }

    public static Class<?> getNetClass(String className, boolean array) {
        return getCustomClass((array == true ? "[L" : "") + "net.minecraft.server." + Internal.getVersion() + className);
    }

    public static Class<?> getCraftClass(String className) {
        return getCustomClass("org.bukkit.craftbukkit." + Internal.getVersion() + className);
    }

    public static LinkedList<Field> getAllFields(Class<?> clazz) {
        LinkedList<Field> fields = new LinkedList<>();
        if (clazz.getSuperclass() != null)
            fields.addAll(getAllFields(clazz.getSuperclass()));
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        return fields;
    }

    public static <T> Class<? super T> getSecondFinalSuperClass(Class<T> clazz) {
        Class<? super T> superClazz;
        if (!hasSuper(clazz))
            return clazz;
        else
            superClazz = clazz;
        while (hasSuper(superClazz) && hasSuper(superClazz.getSuperclass()))
            superClazz = superClazz.getSuperclass();
        return superClazz;
    }

    private static boolean hasSuper(Class<?> clazz) {
        return clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class);
    }

    public static Method getMethod(Class<?> cl, String method) {
        return getMethod(cl, method, new Class<?>[] {});
    }

    public static Method getMethod(Class<?> cl, String method, Class<?>... args) {
        Method m = Internal.getMethod(cl, method, args);
        if (m == null)
            Satchels.log().warning("Could not find method: " + method + " in " + cl.getName() + " with arguments: " + Arrays.toString(args));
        return m;
    }

    public static Object createObject(Class<?> clazz) {
        return createObject(clazz, null, (Object[]) null);
    }

    public static Object createObject(Class<?> clazz, Class<?>[] classes, Object... args) {
        return createObject(getConstructor(clazz, classes), args);
    }

    public static Object createObject(Constructor<?> ctor, Object... args) {
        try {
            return Internal.createObject(ctor, args);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Satchels.log().warning("Error creating object using constructor: " + ctor.toGenericString() + ": " + e.getMessage());
        }
        return null;
    }

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... argTypes) {
        Constructor<?> c = Internal.getConstructor(clazz, argTypes);
        if (c == null)
            Satchels.log().warning("Constructor  " + Arrays.toString(argTypes) + " not found for " + clazz.getName());
        return c;
    }

    public static Class<?> getComponent(Class<?> clazz) {
        if (clazz.isArray())
            return clazz.getComponentType();
        else
            return clazz;
    }

    public static Class<?> getCustomClass(String className) {
        try {
            return Internal.getCustomClass(className);
        } catch (ClassNotFoundException e) {
            Satchels.log().warning("Could not find class: " + className);
        }
        return null;
    }

    public static Field getField(Class<?> cl, String fieldName) {
        Field f = Internal.getField(cl, fieldName);
        if (f == null)
            Satchels.log().warning("Could not find field: " + fieldName + " in " + cl.getName());
        return f;
    }

    public static class Internal {

        private static String version;

        private static String getVersion() {
            if (version == null) {
                String name = Bukkit.getServer().getClass().getPackage().getName();
                version = name.substring(name.lastIndexOf('.') + 1) + ".";
            }
            return version;
        }

        public static Class<?> getCustomClass(String className) throws ClassNotFoundException {
            return Class.forName(className);
        }

        public static Field getField(Class<?> cl, String fieldName) {
            Field f = null;
            try {
                f = cl.getDeclaredField(fieldName);
                if (!f.isAccessible())
                    f.setAccessible(true);
            } catch (NoSuchFieldException e) {
                if (cl.getSuperclass() != null)
                    f = getField(cl.getSuperclass(), fieldName);
            }
            return f;
        }

        public static Method getMethod(Class<?> cl, String method, Class<?>... args) {
            for (Method m : cl.getMethods())
                if (m.getName().equals(method) && Arrays.equals(args, m.getParameterTypes()))
                    return m;
            for (Method m : cl.getDeclaredMethods())
                if (m.getName().equals(method) && Arrays.equals(args, m.getParameterTypes()))
                    return m;
            if (cl.getSuperclass() != null)
                return getMethod(cl.getSuperclass(), method, args);
            return null;
        }

        public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... argTypes) {
            if (argTypes != null && argTypes.length > 0)
                try {
                    Constructor<?> c = clazz.getConstructor(argTypes);
                    if (!c.isAccessible())
                        c.setAccessible(true);
                    return c;
                } catch (NoSuchMethodException | SecurityException e) {
                }
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            for (int i = 0; i < ctors.length; i++)
                if (((argTypes == null || argTypes.length < 1) && ctors[i].getGenericParameterTypes().length == 0)
                        || Arrays.equals(ctors[i].getGenericParameterTypes(), argTypes)) {
                    if (!ctors[i].isAccessible())
                        ctors[i].setAccessible(true);
                    return ctors[i];
                }
            return null;
        }

        public static Object createObject(Constructor<?> ctor, Object... args)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {
            if (ctor == null)
                return null;
            if (args != null && args.length > 0)
                return ctor.newInstance(args);
            else
                return ctor.newInstance();
        }
    }
}