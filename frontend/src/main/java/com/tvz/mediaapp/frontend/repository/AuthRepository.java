package com.tvz.mediaapp.frontend.repository;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuthRepository.class);
    private static final String SERVICE_NAME = "com.tvz.mediaapp";
    private static final String ACCOUNT_NAME_KEY = "userRefreshToken";

    private final Keyring keyring;

    @Inject
    public AuthRepository(Keyring keyring) {
        this.keyring = keyring;
    }

    public void saveRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            clearRefreshToken();
            return;
        }
        logger.debug("Saving refresh token to secure keyring.");
        try {
            keyring.setPassword(SERVICE_NAME, ACCOUNT_NAME_KEY, token);
        } catch (PasswordAccessException e) {
            logger.error("Failed to save refresh token to keyring. 'Remember Me' will not function.", e);
        }
    }

    public String getRefreshToken() {
        logger.debug("Retrieving refresh token from secure keyring.");
        try {
            return keyring.getPassword(SERVICE_NAME, ACCOUNT_NAME_KEY);
        } catch (PasswordAccessException e) {
            logger.debug("No refresh token found in keyring.", e.getMessage());
            return null;
        }
    }

    public void clearRefreshToken() {
        logger.debug("Clearing refresh token from secure keyring.");
        try {
            keyring.deletePassword(SERVICE_NAME, ACCOUNT_NAME_KEY);
        } catch (PasswordAccessException e) {
            logger.warn("Could not delete token from keyring, it might not have existed.", e.getMessage());
        }
    }
}