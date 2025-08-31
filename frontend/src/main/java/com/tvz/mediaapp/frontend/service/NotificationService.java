package com.tvz.mediaapp.frontend.service;

import com.google.inject.Singleton;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_NOTIFICATIONS = 5;
    private static final double NOTIFICATION_WIDTH = 350;
    private static final double NOTIFICATION_HEIGHT = 60;
    private static final double NOTIFICATION_SPACING = 10;
    private static final double EDGE_MARGIN = 20;
    private static final double AUTO_REMOVE_DELAY = 5.0;

    private VBox notificationContainer;
    private final List<StackPane> activeNotifications = new ArrayList<>();
    private double topBarHeight = 60.0;

    public Node createNotificationContainer(double topBarHeight) {
        this.topBarHeight = topBarHeight;

        notificationContainer = new VBox(NOTIFICATION_SPACING);
        notificationContainer.setAlignment(Pos.BOTTOM_RIGHT);
        notificationContainer.setMouseTransparent(true);
        notificationContainer.setPrefWidth(NOTIFICATION_WIDTH);
        notificationContainer.setMaxWidth(NOTIFICATION_WIDTH);

        StackPane.setAlignment(notificationContainer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(notificationContainer, new javafx.geometry.Insets(
                topBarHeight + NOTIFICATION_SPACING,
                EDGE_MARGIN,
                EDGE_MARGIN,
                0
        ));

        logger.debug("Notification container created with top bar height: {}", topBarHeight);
        return notificationContainer;
    }



    public void showSuccess(String message) {
        showNotification(message, NotificationType.SUCCESS);
    }

    public void showError(String message) {
        showNotification(message, NotificationType.ERROR);
    }

    public void showInfo(String message) {
        showNotification(message, NotificationType.INFO);
    }

    public void showWarning(String message) {
        showNotification(message, NotificationType.WARNING);
    }

    public void showFailure(String message) {
        showError(message);
    }

    private void showNotification(String message, NotificationType type) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showNotification(message, type));
            return;
        }

        if (notificationContainer == null) {
            logger.warn("Notification container not initialized, cannot show notification: {}", message);
            return;
        }

        if (activeNotifications.size() >= MAX_NOTIFICATIONS) {
            removeNotification(activeNotifications.get(0), false);
        }

        StackPane notification = createNotification(message, type);
        activeNotifications.add(notification);

        notificationContainer.getChildren().add(0, notification);

        animateIn(notification);

        Timeline autoRemove = new Timeline();
        autoRemove.getKeyFrames().add(new javafx.animation.KeyFrame(
                Duration.seconds(AUTO_REMOVE_DELAY),
                e -> removeNotification(notification, true)
        ));
        autoRemove.play();

        logger.debug("Showing {} notification: {}", type, message);
    }

    private StackPane createNotification(String message, NotificationType type) {
        StackPane notification = new StackPane();
        notification.getStyleClass().addAll("notification", "notification-" + type.name().toLowerCase());
        notification.setPrefSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);
        notification.setMaxSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);

        Label icon = new Label(getIconForType(type));
        icon.getStyleClass().add("notification-icon");
        icon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(NOTIFICATION_WIDTH - 60);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        StackPane.setMargin(icon, new javafx.geometry.Insets(0, 0, 0, 16));
        StackPane.setAlignment(messageLabel, Pos.CENTER_LEFT);
        StackPane.setMargin(messageLabel, new javafx.geometry.Insets(0, 16, 0, 50));

        notification.getChildren().addAll(icon, messageLabel);

        notification.setOnMouseClicked(e -> removeNotification(notification, true));

        notification.setStyle(getBaseStyleForType(type));

        return notification;
    }

    private String getIconForType(NotificationType type) {
        return switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "✕";
            case WARNING -> "⚠";
            case INFO -> "ℹ";
        };
    }

    private String getBaseStyleForType(NotificationType type) {
        String baseStyle = "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 12, 0, 0, 4); " +
                "-fx-cursor: hand; ";

        return baseStyle + switch (type) {
            case SUCCESS -> "-fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a); -fx-border-color: #4ade80; ";
            case ERROR -> "-fx-background-color: linear-gradient(to bottom, #ef4444, #dc2626); -fx-border-color: #f87171; ";
            case WARNING -> "-fx-background-color: linear-gradient(to bottom, #f59e0b, #d97706); -fx-border-color: #fbbf24; ";
            case INFO -> "-fx-background-color: linear-gradient(to bottom, #3b82f6, #2563eb); -fx-border-color: #60a5fa; ";
        };
    }

    private void animateIn(Node notification) {
        notification.setTranslateX(NOTIFICATION_WIDTH + EDGE_MARGIN);
        notification.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notification);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notification);
        fadeIn.setToValue(1.0);

        slideIn.play();
        fadeIn.play();
    }

    private void animateOut(Node notification, Runnable onComplete) {
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), notification);
        slideOut.setToX(NOTIFICATION_WIDTH + EDGE_MARGIN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), notification);
        fadeOut.setToValue(0);

        slideOut.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });

        slideOut.play();
        fadeOut.play();
    }

    private void removeNotification(StackPane notification, boolean animate) {
        if (!activeNotifications.contains(notification)) {
            return;
        }

        activeNotifications.remove(notification);

        if (animate) {
            animateOut(notification, () -> Platform.runLater(() -> notificationContainer.getChildren().remove(notification)));
        } else {
            notificationContainer.getChildren().remove(notification);
        }
    }

    private enum NotificationType {
        SUCCESS, ERROR, WARNING, INFO
    }
}