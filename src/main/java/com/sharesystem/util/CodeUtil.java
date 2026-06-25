package com.sharesystem.util;

import java.util.UUID;
import java.util.Random;

/**
 * 编码生成工具类
 */
public class CodeUtil {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();

    /**
     * 生成UUID（去除横线）
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成指定长度的随机分享码
     */
    public static String generateShareCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成8位分享码
     */
    public static String generateShareCode() {
        return generateShareCode(8);
    }

    /**
     * 生成4位提取码
     */
    public static String generateExtractCode() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成用户默认昵称
     */
    public static String generateNickname() {
        return "用户" + System.currentTimeMillis() % 100000;
    }
}
