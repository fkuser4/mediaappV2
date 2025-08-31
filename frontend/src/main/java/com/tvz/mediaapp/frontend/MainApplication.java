package com.tvz.mediaapp.frontend;

import com.tvz.mediaapp.frontend.di.AppInjector;
import com.tvz.mediaapp.frontend.service.AuthService;
import com.tvz.mediaapp.frontend.service.NavigationManager;
import com.tvz.mediaapp.frontend.service.ViewCache;
import com.tvz.mediaapp.frontend.view.LoadingView;
import com.tvz.mediaapp.frontend.view.LoginView;
import com.tvz.mediaapp.frontend.view.PreviewView;
import com.tvz.mediaapp.frontend.viewmodel.HomeViewModel;
import com.tvz.mediaapp.frontend.viewmodel.LoginViewModel;
import com.tvz.mediaapp.frontend.viewmodel.PostsViewModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class MainApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

    private AuthService authService;
    private NavigationManager navigationManager;
    private LoginViewModel loginViewModel;
    private ViewCache viewCache;
    private PostsViewModel postsViewModel;
    private HomeViewModel homeViewModel;

    static {
        try {
            logger.info("Loading custom fonts...");
            Font.loadFont(MainApplication.class.getResourceAsStream("/fonts/Inter-Regular.ttf"), 10);
            Font.loadFont(MainApplication.class.getResourceAsStream("/fonts/Inter-Medium.ttf"), 10);
            Font.loadFont(MainApplication.class.getResourceAsStream("/fonts/Inter-SemiBold.ttf"), 10);
            Font.loadFont(MainApplication.class.getResourceAsStream("/fonts/Inter-Bold.ttf"), 10);
            logger.info("Custom fonts loaded successfully.");
        } catch (Exception e) {
            logger.error("Failed to load custom fonts.", e);
        }
    }

    @Override
    public void init() {
        AppInjector.createInjector();
        this.authService = AppInjector.getInjector().getInstance(AuthService.class);
        this.navigationManager = AppInjector.getInjector().getInstance(NavigationManager.class);
        this.loginViewModel = AppInjector.getInjector().getInstance(LoginViewModel.class);
        this.viewCache = AppInjector.getInjector().getInstance(ViewCache.class);
        this.postsViewModel = AppInjector.getInjector().getInstance(PostsViewModel.class);
        this.homeViewModel = AppInjector.getInjector().getInstance(HomeViewModel.class);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Application starting...");
        navigationManager.init(primaryStage);

        logger.info("Pre-warming initial views (Login and Loading)...");
        viewCache.getView(LoginView.class);
        viewCache.getView(LoadingView.class);
        logger.info("Initial views are ready.");

        Runnable initializeAndShowApp = () -> CompletableFuture.runAsync(() -> {
            logger.info("Starting background DATA initialization...");
            postsViewModel.initialize();
            homeViewModel.initialize();
            logger.info("Background DATA initialization finished.");
        }).thenRun(() -> Platform.runLater(() -> {
            logger.info("Starting UI view pre-loading on the FX thread...");
            viewCache.preloadViews();
            logger.info("UI view pre-loading finished.");

            PreviewView previewController = viewCache.getViewController(PreviewView.class);
            previewController.getWebViewReadyFuture().thenRun(() -> Platform.runLater(() -> {
                logger.info("WebView shell is ready. Transitioning to MainView.");
                navigationManager.showMain();
            }));
        }));

        loginViewModel.setLoginSuccessCallback(() -> {
            navigationManager.showLoading();
            initializeAndShowApp.run();
        });

        if (authService.hasLocalSession()) {
            logger.info("Local session found. Showing loading screen and attempting silent refresh...");
            navigationManager.showLoading();

            authService.silentlyRefreshSession().thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    logger.info("Silent refresh successful. Initializing application.");
                    initializeAndShowApp.run();
                } else {
                    logger.warn("Silent refresh failed. Forcing logout and showing login screen.");
                    authService.logout();
                }
            }));
        } else {
            logger.info("No local session found. Showing login screen.");
            navigationManager.showLogin();
        }

        navigationManager.showStage();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Application shutting down.");
        if (postsViewModel != null) {
            postsViewModel.stopPolling();
        }
        super.stop();
    }

    public static void main(String[] args) {
        System.setProperty("prism.vsync", "true");
        System.setProperty("prism.allowhidpi", "true");
        System.setProperty("javafx.animation.pulse", "60");
        launch(args);
    }
}