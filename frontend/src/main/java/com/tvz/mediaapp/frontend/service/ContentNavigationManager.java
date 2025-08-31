package com.tvz.mediaapp.frontend.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.view.*;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

@Singleton
public class ContentNavigationManager {
    private StackPane contentArea;
    private final ViewCache viewCache;

    @Inject
    public ContentNavigationManager(ViewCache viewCache) {
        this.viewCache = viewCache;
    }

    public void init(StackPane contentArea) {
        this.contentArea = contentArea;
    }


    public void showHome() {
        Node viewNode = viewCache.getView(HomeView.class);
        switchContentView(viewNode);
    }

    public void showPosts() {
        Node viewNode = viewCache.getView(PostsView.class);
        switchContentView(viewNode);
    }

    public void showSettings() {
        Node viewNode = viewCache.getView(SettingsView.class);
        switchContentView(viewNode);
    }

    public void showPreview() {
        Node viewNode = viewCache.getView(PreviewView.class);
        switchContentView(viewNode);
    }

    private void switchContentView(Node newNode) {
        if (contentArea.getChildren().isEmpty() || contentArea.getChildren().get(0) != newNode) {
            contentArea.getChildren().setAll(newNode);
        }
    }
}