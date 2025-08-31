package com.tvz.mediaapp.frontend.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.service.AuthService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoginViewModel {
    private static final Logger logger = LoggerFactory.getLogger(LoginViewModel.class);

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty rememberMe = new SimpleBooleanProperty(false);
    private final BooleanProperty isLoggingIn = new SimpleBooleanProperty(false);

    private Runnable loginSuccessCallback;

    @Inject private AuthService authService;

    public void setLoginSuccessCallback(Runnable callback) {
        this.loginSuccessCallback = callback;
    }

    public void login() {
        if (!validateInput()) {
            return;
        }

        isLoggingIn.set(true);
        clearError();

        try {
            authService.login(username.get(), password.get(), rememberMe.get())
                    .thenAccept(aVoid -> {
                        logger.info("Login successful for user: {}", username.get());
                        Platform.runLater(() -> {
                            isLoggingIn.set(false);
                            if (loginSuccessCallback != null) {
                                loginSuccessCallback.run();
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            isLoggingIn.set(false);
                            handleLoginError(throwable);
                        });
                        return null;
                    });
        } catch (Exception e) {
            Platform.runLater(() -> {
                isLoggingIn.set(false);
                logger.error("Unexpected error setting up login attempt", e);
                setErrorMessage("An unexpected error occurred. Please try again.");
            });
        }
    }

    private boolean validateInput() {
        if (username.get() == null || username.get().trim().isEmpty()) {
            setErrorMessage("Please enter your username.");
            return false;
        }
        if (password.get() == null || password.get().trim().isEmpty()) {
            setErrorMessage("Please enter your password.");
            return false;
        }
        return true;
    }

    private void handleLoginError(Throwable throwable) {
        logger.error("Login failed", throwable);
        password.set("");
        String message = "Login failed. Please check your credentials.";
        if (throwable != null) {
            Throwable cause = throwable.getCause();
            if (cause != null && cause.getMessage() != null) {
                if (cause.getMessage().contains("401") || cause.getMessage().contains("Unauthorized")) {
                    message = "Invalid username or password.";
                } else if (cause.getMessage().contains("ConnectException")) {
                    message = "Could not connect to the server. Please check your connection.";
                }
            }
        }
        setErrorMessage(message);
    }

    public void clearError() {
        setErrorMessage("");
    }

    public StringProperty usernameProperty() { return username; }
    public StringProperty passwordProperty() { return password; }
    public StringProperty errorMessageProperty() { return errorMessage; }
    public BooleanProperty rememberMeProperty() { return rememberMe; }
    public BooleanProperty isLoggingInProperty() { return isLoggingIn; }
    private void setErrorMessage(String message) { this.errorMessage.set(message); }
}