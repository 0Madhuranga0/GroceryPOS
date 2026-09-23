package com.gp.grocerypos.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for BCrypt password hashing and verification.
 * <p>
 * Uses the {@code at.favre.lib:bcrypt} library which wraps the original
 * jBCrypt implementation with a modern, security-focused API.
 * <p>
 * Cost factor 12 is used — a good balance between security and speed
 * on typical hardware (~300 ms per hash). Increase to 13 or 14 for
 * higher-security deployments.
 * <p>
 * <b>Never store plain-text passwords anywhere</b> — pass them to
 * {@link #hash(String)} immediately and discard the original.
 */
public final class HashUtil {

    private static final Logger log = LoggerFactory.getLogger(HashUtil.class);

    /** BCrypt work factor (cost). Higher = slower = more secure. */
    private static final int BCRYPT_COST = 12;

    private HashUtil() {
        // Utility class — no instances
    }

    /**
     * Hashes a plain-text password using BCrypt.
     * <p>
     * This is intentionally slow (~300 ms). Do not call on the JavaFX
     * Application Thread — use a background task.
     *
     * @param plainPassword the raw password entered by the user
     * @return the BCrypt hash string (includes salt, ready to store in DB)
     * @throws IllegalArgumentException if the password is null or empty
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        String hashed = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
        log.debug("Password hashed with BCrypt cost={}", BCRYPT_COST);
        return hashed;
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     * <p>
     * Safe against timing attacks — comparison uses constant-time equality.
     *
     * @param plainPassword  the raw password entered by the user
     * @param storedHash     the BCrypt hash loaded from the database
     * @return {@code true} if the password matches the hash
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isEmpty()) return false;
        if (storedHash   == null || storedHash.isEmpty())   return false;

        BCrypt.Result result = BCrypt.verifyer()
                .verify(plainPassword.toCharArray(), storedHash);
        return result.verified;
    }
}
