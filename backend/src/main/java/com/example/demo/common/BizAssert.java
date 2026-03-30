package com.example.demo.common;

public final class BizAssert {

    private BizAssert() {
    }

    public static void isTrue(boolean condition, int code, String message) {
        if (!condition) {
            throw new BusinessException(code, message);
        }
    }

    public static void notNull(Object value, int code, String message) {
        if (value == null) {
            throw new BusinessException(code, message);
        }
    }
}
