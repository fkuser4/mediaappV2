package com.tvz.mediaapp.frontend.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.dto.AuthResponseDto;
import com.tvz.mediaapp.frontend.model.SessionManager;
import com.tvz.mediaapp.frontend.repository.AuthRepository;
import com.tvz.mediaapp.frontend.repository.UserApiRepository;
import com.tvz.mediaapp.frontend.viewmodel.HomeViewModel;
import com.tvz.mediaapp.frontend.viewmodel.PostsViewModel;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Singleton
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Inject private UserApiRepository userApiRepository;
    @Inject private AuthRepository authRepository;
    @Inject private SessionManager sessionManager;
    @Inject private  ViewCache viewCache;
    @Inject private  NavigationManager navigationManager;
    @Inject private  PostsViewModel postsViewModel;
    @Inject private  HomeViewModel homeViewModel;

    public boolean hasLocalSession() {
        return authRepository.getRefreshToken() != null;
    }

    public CompletableFuture<Boolean> silentlyRefreshSession() {
        String refreshToken = authRepository.getRefreshToken();
        if (refreshToken == null) {
            logger.warn("Cannot perform silent refresh - no refresh token available.");
            return CompletableFuture.completedFuture(false);
        }

        try {
            return userApiRepository.refreshToken(refreshToken)
                    .thenApply(authResponse -> {
                        updateSession(authResponse, true);
                        return true;
                    })
                    .exceptionally(ex -> {
                        logger.error("Silent refresh failed. Clearing invalid session.", ex.getCause());
                        logout();
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Unexpected error during silent refresh.", e);
            logout();
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Void> login(String username, String password, boolean rememberMe) {
        logger.info("Attempting login for user: {} (Remember Me: {})", username, rememberMe);
        try {
            return userApiRepository.login(username, password)
                    .thenAccept(authResponse -> {
                        updateSession(authResponse, rememberMe);
                        logger.info("Login successful for user: {}", authResponse.getUser().getUsername());
                    })
                    .exceptionally(throwable -> {
                        logger.error("Login failed for user: {}", username, throwable.getCause());
                        throw new RuntimeException("Login failed", throwable);
                    });
        } catch (Exception e) {
            logger.error("Failed to initiate login request", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void logout() {
        try {
            logger.info("Logout process initiated...");

            postsViewModel.reset();

            sessionManager.clear();
            authRepository.clearRefreshToken();

            viewCache.clear();

            Platform.runLater(navigationManager::showLogin);

            logger.info("User logged out and application state reset successfully.");

        } catch (Exception e) {
            logger.error("A critical error occurred during the logout process. Exiting application to ensure security.", e);
            Platform.exit();
        }
    }

    private void updateSession(AuthResponseDto authResponse, boolean saveRefreshToken) {
        sessionManager.setAccessToken(authResponse.getAccessToken());
        sessionManager.setCurrentUser(authResponse.getUser());

        if (saveRefreshToken) {
            authRepository.saveRefreshToken(authResponse.getRefreshToken());
        } else {
            authRepository.clearRefreshToken();
        }
    }
}