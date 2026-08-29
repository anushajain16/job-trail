package com.example.anusha.job_trail.googlecalendar;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM at rest for the one long-lived external credential this app
 * stores: a user's Google refresh token (see {@link GoogleConnection}).
 * Unlike {@code RefreshTokenService}'s SHA-256 hashing of this app's own
 * refresh tokens (which only ever need to be *verified*, never read back),
 * this credential has to be recovered in full to call Google's APIs, so it
 * has to be reversible — encryption, not hashing.
 *
 * <p>Output is {@code base64(iv) + ":" + base64(ciphertext+tag)}; a fresh
 * random 96-bit IV is generated per call to {@link #encrypt}, per GCM's own
 * requirement that an (key, IV) pair is never reused.
 */
@Component
public class TokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenCipher(GoogleCalendarProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.tokenEncryptionKey());
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.google-calendar.token-encryption-key must decode to 32 bytes (AES-256), got " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return encode(iv) + ":" + encode(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt token", e);
        }
    }

    public String decrypt(String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed encrypted token");
        }
        try {
            byte[] iv = decode(parts[0]);
            byte[] ciphertext = decode(parts[1]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt token", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
