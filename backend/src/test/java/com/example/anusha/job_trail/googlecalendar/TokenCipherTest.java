package com.example.anusha.job_trail.googlecalendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenCipherTest {

    // Valid base64 of exactly 32 bytes — same dev-fallback key as
    // application.yml's app.google-calendar.token-encryption-key.
    private static final String KEY = "ZGV2LW9ubHktaW5zZWN1cmUtMzItYnl0ZS1rZXkhISE=";

    private final TokenCipher tokenCipher = new TokenCipher(
            new GoogleCalendarProperties(null, null, null, null, KEY));

    @Test
    void encryptThenDecrypt_roundTripsTheOriginalValue() {
        String plaintext = "1//0gABCDEF-a-real-looking-refresh-token";

        String encrypted = tokenCipher.encrypt(plaintext);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(tokenCipher.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_neverProducesTheSameCiphertextTwice() {
        String plaintext = "same-refresh-token";

        // A fresh random IV per call is what GCM requires for a reused
        // key — asserting this catches a regression to a fixed/zero IV.
        assertThat(tokenCipher.encrypt(plaintext)).isNotEqualTo(tokenCipher.encrypt(plaintext));
    }

    @Test
    void decrypt_rejectsATamperedCiphertext() {
        String encrypted = tokenCipher.encrypt("a-refresh-token");
        String[] parts = encrypted.split(":", 2);
        // Flip a character in the middle of the ciphertext block, not
        // right at either end — an edge character can also encode base64
        // padding bits, so tampering it can fail as a decode error
        // instead of exercising GCM's own tamper detection, which is what
        // this test is actually after.
        int middle = parts[1].length() / 2;
        char original = parts[1].charAt(middle);
        String tamperedCiphertext = parts[1].substring(0, middle) + (original == 'A' ? 'B' : 'A') + parts[1].substring(middle + 1);
        String tampered = parts[0] + ":" + tamperedCiphertext;

        assertThatThrownBy(() -> tokenCipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_rejectsAKeyThatIsNotExactly32Bytes() {
        String shortKey = "dG9vLXNob3J0"; // base64 of "too-short" — not 32 bytes
        assertThatThrownBy(() -> new TokenCipher(new GoogleCalendarProperties(null, null, null, null, shortKey)))
                .isInstanceOf(IllegalStateException.class);
    }
}
