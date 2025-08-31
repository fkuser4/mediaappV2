package com.tvz.mediaapp.frontend.service;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.view.PreviewView;
import com.tvz.mediaapp.frontend.view.View;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class ViewCache {
    private static final Logger logger = LoggerFactory.getLogger(ViewCache.class);

    private final Map<Class<? extends View>, View> viewCache = new HashMap<>();
    private final Injector injector;
    private Pane preloadArea;

    @Inject
    public ViewCache(Injector injector) {
        this.injector = injector;
    }

    public void setPreloadArea(Pane preloadArea) {
        this.preloadArea = preloadArea;
    }

    public void preloadViews() {
        logger.info("Pre-loading all content views...");
        getView(com.tvz.mediaapp.frontend.view.HomeView.class);
        getView(com.tvz.mediaapp.frontend.view.PostsView.class);
        getView(com.tvz.mediaapp.frontend.view.SettingsView.class);
        getView(com.tvz.mediaapp.frontend.view.PreviewView.class);
        logger.info("All views pre-loaded successfully.");
    }

    public <T extends View> Parent getView(Class<T> viewClass) {
        return getViewController(viewClass).getView();
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T getViewController(Class<T> viewClass) {
        if (viewCache.containsKey(viewClass)) {
            return (T) viewCache.get(viewClass);
        }

        logger.debug("Creating new instance of view: {}", viewClass.getSimpleName());
        T viewInstance = injector.getInstance(viewClass);
        viewCache.put(viewClass, viewInstance);

        Parent uiComponent = viewInstance.getView();

        if (viewClass.equals(PreviewView.class) && preloadArea != null) {
            logger.info("Pre-rendering PreviewView off-screen to initialize WebView engine...");
            Platform.runLater(() -> {
                preloadArea.getChildren().add(uiComponent);
                Platform.runLater(() -> {
                    preloadArea.getChildren().remove(uiComponent);
                    logger.info("PreviewView pre-rendering complete and removed from off-screen area.");
                });
            });
        }
        return viewInstance;
    }

    public void clear() {
        logger.info("Clearing all cached views to reset UI state.");
        viewCache.clear();
    }
}