package com.tvz.mediaapp.frontend.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.utils.WindowResizeHandler;
import com.tvz.mediaapp.frontend.view.GlobalTopBarView;
import com.tvz.mediaapp.frontend.view.View;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Singleton
public class NavigationManager {
    private static final Logger logger = LoggerFactory.getLogger(NavigationManager.class);

    private final ViewCache viewCache;
    private final NotificationService notificationService;
    private Stage primaryStage;
    private StackPane rootShell;
    private Node currentContentView;
    private StackPane modalOverlay;
    private Node topBar;
    private Pane preloadArea;

    private FadeTransition currentTransition;
    private boolean isTransitioning = false;

    @Inject
    private GlobalTopBarView globalTopBarView;

    private static final double MIN_WIDTH = 1400;
    private static final double MIN_HEIGHT = 800;

    @Inject
    public NavigationManager(ViewCache viewCache, NotificationService notificationService) {
        this.viewCache = viewCache;
        this.notificationService = notificationService;
    }

    public void init(Stage stage) {
        this.primaryStage = stage;
        this.rootShell = new StackPane();

        this.preloadArea = new Pane();
        this.preloadArea.setManaged(false);
        this.preloadArea.setVisible(false);
        rootShell.getChildren().add(preloadArea);
        this.viewCache.setPreloadArea(preloadArea);

        setupStage();
        setupModalLayer();
        setupNotifications();
    }

    private void setupModalLayer() {
        modalOverlay = new StackPane();
        modalOverlay.setAlignment(Pos.CENTER);
        modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        rootShell.getChildren().add(modalOverlay);

        ensureProperLayering();
    }

    private void setupNotifications() {
        Node notificationContainer = notificationService.createNotificationContainer(getTopBarHeight());
        rootShell.getChildren().add(notificationContainer);
        ensureProperLayering();
    }

    private double getTopBarHeight() {
        return topBar != null ? topBar.getBoundsInLocal().getHeight() : 60.0;
    }

    public void showModal(Node modalContent) {
        modalOverlay.getChildren().setAll(modalContent);
        modalOverlay.setManaged(true);
        modalOverlay.setVisible(true);

        ensureProperLayering();
    }

    public void hideModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        modalOverlay.getChildren().clear();
    }

    private void ensureProperLayering() {
        if (topBar != null && rootShell.getChildren().contains(topBar)) {
            rootShell.getChildren().remove(topBar);
            rootShell.getChildren().add(topBar);
            StackPane.setAlignment(topBar, Pos.TOP_CENTER);
        }

        ensureNotificationLayering();
    }

    private void ensureNotificationLayering() {
        Node notificationContainer = rootShell.getChildren().stream()
                .filter(node -> node instanceof VBox &&
                        ((VBox) node).getAlignment() == Pos.BOTTOM_RIGHT)
                .findFirst()
                .orElse(null);

        if (notificationContainer != null) {
            rootShell.getChildren().remove(notificationContainer);

            int insertIndex = rootShell.getChildren().size();

            for (int i = rootShell.getChildren().size() - 1; i >= 0; i--) {
                Node child = rootShell.getChildren().get(i);
                if (child == modalOverlay || child == topBar) {
                    insertIndex = i;
                }
            }

            rootShell.getChildren().add(insertIndex, notificationContainer);
        }
    }

    private void setupStage() {
        topBar = globalTopBarView.getView();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(MIN_WIDTH, screenBounds.getWidth() * 0.6);
        double height = Math.max(MIN_HEIGHT, screenBounds.getHeight() * 0.7);

        Scene scene = new Scene(rootShell, width, height);

        rootShell.getChildren().add(topBar);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm());
        } catch (Exception e) {
            logger.warn("Could not load main.css: " + e.getMessage());
        }

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setX((screenBounds.getWidth() - width) / 2);
        primaryStage.setY((screenBounds.getHeight() - height) / 2);

        WindowResizeHandler resizeHandler = new WindowResizeHandler(primaryStage);
        resizeHandler.addResizeListener(rootShell);
    }

    public void showStage() {
        primaryStage.show();
        primaryStage.centerOnScreen();
    }

    public void showLogin() {
        switchView(com.tvz.mediaapp.frontend.view.LoginView.class);
    }

    public void showLoading() {
        switchView(com.tvz.mediaapp.frontend.view.LoadingView.class);
    }

    public void showMain() {
        switchView(com.tvz.mediaapp.frontend.view.MainView.class);
    }

    private <T extends View> void switchView(Class<T> viewClass) {
        Node newContentView = viewCache.getView(viewClass);

        if (currentTransition != null && currentTransition.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            currentTransition.stop();
            currentTransition = null;
        }

        if (isTransitioning) {
            if (currentContentView != null) {
                rootShell.getChildren().remove(currentContentView);
            }
            rootShell.getChildren().add(0, newContentView);
            currentContentView = newContentView;
            StackPane.setAlignment(currentContentView, Pos.TOP_LEFT);

            ensureProperLayering();

            isTransitioning = false;
            logger.debug("Forced immediate view switch to: {}", viewClass.getSimpleName());
            return;
        }

        if (currentContentView == null) {
            rootShell.getChildren().add(0, newContentView);
            currentContentView = newContentView;
            StackPane.setAlignment(currentContentView, Pos.TOP_LEFT);
            logger.debug("Initial view set to: {}", viewClass.getSimpleName());
        } else {
            isTransitioning = true;
            Node oldContentView = currentContentView;

            logger.debug("Starting transition from {} to {}",
                    oldContentView.getClass().getSimpleName(),
                    viewClass.getSimpleName());

            FadeTransition fadeOut = getFadeTransition(viewClass, oldContentView, newContentView);
            currentTransition = fadeOut;
            fadeOut.play();
        }
    }

    private <T extends View> FadeTransition getFadeTransition(Class<T> viewClass, Node oldContentView, Node newContentView) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), oldContentView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            if (oldContentView == currentContentView) {
                rootShell.getChildren().remove(oldContentView);
                rootShell.getChildren().add(0, newContentView);
                currentContentView = newContentView;
                StackPane.setAlignment(currentContentView, Pos.TOP_LEFT);

                ensureProperLayering();

                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), newContentView);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setOnFinished(e -> {
                    isTransitioning = false;
                    currentTransition = null;
                    logger.debug("Transition completed to: {}", viewClass.getSimpleName());
                });
                currentTransition = fadeIn;
                fadeIn.play();
            } else {
                isTransitioning = false;
                logger.debug("Transition cancelled - view already changed");
            }
        });
        return fadeOut;
    }
}