package com.sharesystem.common.util;

import com.sharesystem.common.dto.UserDTO;
import com.alibaba.fastjson.JSON;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 (JDK 21 compatible, no jjwt dependency)
 */
public class JwtUtil {

    private static final String SECRET = "ShareSystem@SpringCloud2024!@#$%SecretKey";
    private static final long EXPIRE = 60 * 60 * 1000;
    private static final String USER_KEY = "user";

    public static String generateToken(UserDTO user) {
        try {
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String headerB64 = base64UrlEncode(header.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> claims = new HashMap<>();
            claims.put(USER_KEY, JSON.toJSONString(user));
            claims.put("sub", user.getId().toString());
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", (System.currentTimeMillis() + EXPIRE) / 1000);
            String payloadB64 = base64UrlEncode(JSON.toJSONString(claims).getBytes(StandardCharsets.UTF_8));

            String signingInput = headerB64 + "." + payloadB64;
            String signature = hmacSha256Base64Url(signingInput, SECRET);

            return headerB64 + "." + payloadB64 + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("JWT generation failed", e);
        }
    }

    public static UserDTO getUserFromToken(String token) {
        try {
            Map<String, Object> claims = parsePayload(token);
            if (claims == null) return null;
            String userJson = (String) claims.get(USER_KEY);
            if (userJson == null) return null;
            return JSON.parseObject(userJson, UserDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) return false;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = hmacSha256Base64Url(signingInput, SECRET);
            if (!parts[2].equals(expectedSig)) return false;

            Map<String, Object> claims = parsePayload(token);
            if (claims == null) return false;
            Object expObj = claims.get("exp");
            if (expObj == null) return false;
            long exp = ((Number) expObj).longValue();
            return System.currentTimeMillis() / 1000 < exp;
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserId(String token) {
        try {
            Map<String, Object> claims = parsePayload(token);
            if (claims == null) return null;
            String sub = (String) claims.get("sub");
            return sub != null ? Long.parseLong(sub) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String refreshToken(String token) {
        UserDTO user = getUserFromToken(token);
        if (user != null) {
            return generateToken(user);
        }
        return null;
    }

    private static Map<String, Object> parsePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        byte[] payloadBytes = base64UrlDecode(parts[1]);
        if (payloadBytes == null) return null;
        String json = new String(payloadBytes, StandardCharsets.UTF_8);
        return JSON.parseObject(json, Map.class);
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String b64) {
        try {
            return Base64.getUrlDecoder().decode(b64);
        } catch (Exception e) {
            try {
                return Base64.getDecoder().decode(b64);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static String hmacSha256Base64Url(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }
}