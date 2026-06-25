package com.sharesystem.common.util;

import java.util.Random;
import java.util.UUID;

public class CodeUtil {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateShareCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static String generateShareCode() {
        return generateShareCode(8);
    }

    public static String generateExtractCode() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String generateNickname() {
        return "用户" + System.currentTimeMillis() % 100000;
    }
}
