package com.planmate.planmate_backend.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenHasher {

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashToken(String token, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String saltedToken = token + salt; // 토큰과 salt 결합
        byte[] hash = digest.digest(saltedToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyToken(String token, String salt, String hashedToken) throws NoSuchAlgorithmException {
        String newHash = hashToken(token, salt);
        return newHash.equals(hashedToken);
    }
}
