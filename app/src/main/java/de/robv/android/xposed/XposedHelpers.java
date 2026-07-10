package de.robv.android.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class XposedHelpers {

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            String className, ClassLoader classLoader,
            String methodName, Object... parameterTypesAndCallback) {
        try {
            Class<?> clazz = findClass(className, classLoader);
            Class<?>[] paramTypes = new Class<?>[parameterTypesAndCallback.length - 1];
            for (int i = 0; i < paramTypes.length; i++) {
                if (parameterTypesAndCallback[i] instanceof Class) {
                    paramTypes[i] = (Class<?>) parameterTypesAndCallback[i];
                }
            }
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return new XC_MethodHook.Unhook();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
