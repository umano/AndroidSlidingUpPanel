package com.sothree.slidinguppanel.util;

public class ReflectionUtils {
    private ReflectionUtils() {
        throw new AssertionError("No instances.");
    }

    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }
}
