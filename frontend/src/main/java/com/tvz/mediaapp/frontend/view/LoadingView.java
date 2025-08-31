package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.animation.LoadingCircle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class LoadingView implements View {
    private Parent root;

    @FXML
    private StackPane stackPane;

    @Inject
    public LoadingView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/loading-view.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for LoadingView", e);
        }
    }

    @FXML
    public void initialize() {
        setupLoadingCircle();
    }

    private void setupLoadingCircle() {
        LoadingCircle circle = new LoadingCircle();

        stackPane.getChildren().add(circle);
    }

    @Override
    public Parent getView() { return root; }
}