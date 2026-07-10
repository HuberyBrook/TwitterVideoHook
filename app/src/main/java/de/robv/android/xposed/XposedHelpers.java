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

    // No parameter types (hook no-arg method)
    public static XC_MethodHook.Unhook findAndHookMethod(
            String className, ClassLoader classLoader,
            String methodName, XC_MethodHook callback) {
        return findAndHookMethod(className, classLoader, methodName, new Class<?>[0], callback);
    }

    // One parameter type
    public static XC_MethodHook.Unhook findAndHookMethod(
            String className, ClassLoader classLoader,
            String methodName, Class<?> paramType, XC_MethodHook callback) {
        return findAndHookMethod(className, classLoader, methodName, new Class<?>[]{paramType}, callback);
    }

    private static XC_MethodHook.Unhook findAndHookMethod(
            String className, ClassLoader classLoader,
            String methodName, Class<?>[] paramTypes, XC_MethodHook callback) {
        try {
            Class<?> clazz = findClass(className, classLoader);
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
