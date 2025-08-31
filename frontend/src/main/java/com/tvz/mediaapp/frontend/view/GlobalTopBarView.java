package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.utils.ButtonStyler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class GlobalTopBarView implements View {
    private static final Logger logger = LoggerFactory.getLogger(GlobalTopBarView.class);

    private Parent root;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private HBox globalTopBar;
    @FXML private Region dragOverlay;

    @Inject
    public GlobalTopBarView() {
        loadFXML();
    }

    @FXML
    public void initialize() {
        setupWindowControls();
        setupWindowDrag();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/global-top-bar.fxml"));
            loader.setController(this);
            root = loader.load();

            root.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/global-top-bar.css")).toExternalForm()
            );
            root.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/button-styler.css")).toExternalForm()
            );
        } catch (IOException e) {
            logger.error("Failed to load FXML for GlobalTopBarView", e);
            throw new RuntimeException("Failed to load FXML for GlobalTopBarView", e);
        }
    }

    private void setupWindowControls() {
        ButtonStyler.with(minimizeButton)
                .svgPath("src/main/resources/svg/minimize.svg")
                .iconSize(14)
                .normalColors("#afaeb4", "transparent")
                .hoverColors("#FFFFFF", "transparent")
                .apply();

        ButtonStyler.with(closeButton)
                .svgPath("src/main/resources/svg/close.svg")
                .iconSize(14)
                .normalColors("#afaeb4", "transparent")
                .hoverColors("#FFFFFF", "transparent")
                .apply();

        minimizeButton.setPrefSize(32, 32);
        closeButton.setPrefSize(32, 32);
    }

    private void setupWindowDrag() {
        dragOverlay.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        dragOverlay.setOnMouseDragged(event -> {
            Stage stage = getStage();
            if (stage != null) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    @FXML
    private void handleMinimize() {
        Stage stage = getStage();
        if (stage != null) stage.setIconified(true);
    }

    @FXML
    private void handleClose() {
        Platform.exit();
    }

    private Stage getStage() {
        if (root.getScene() == null) return null;
        return (Stage) root.getScene().getWindow();
    }

    @Override
    public Parent getView() {
        return root;
    }
}