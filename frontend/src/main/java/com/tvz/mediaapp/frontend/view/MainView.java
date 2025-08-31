package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.service.ContentNavigationManager;
import com.tvz.mediaapp.frontend.utils.ButtonStyler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Objects;

public class MainView implements View {

    @FXML private StackPane contentArea;
    @FXML private ImageView logoImageView;
    @FXML private Button homeButton;
    @FXML private Button postsButton;
    @FXML private Button previewButton;
    @FXML private Button settingsButton;
    @FXML private Label versionLabel;
    @FXML private Label titleLabel;

    private final ContentNavigationManager contentNavigationManager;
    private Parent root;
    private Button currentSelectedButton;

    @Inject
    public MainView( ContentNavigationManager contentNavigationManager) {
        this.contentNavigationManager = contentNavigationManager;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            loader.setController(this);
            this.root = loader.load();
            this.root.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/main-view.css")).toExternalForm()
            );
            this.root.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/button-styler.css")).toExternalForm()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for MainView", e);
        }
    }

    @FXML
    public void initialize() {
        contentNavigationManager.init(contentArea);

        setupLogo();
        styleNavButtons();

        onHomeClick();
    }

    private void setupLogo() {
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
            logoImageView.setImage(logo);
        } catch (Exception e) {
        }
    }

    private void styleNavButtons() {
        String normalSvg = "#5d5b80";
        String hoverBg = "transparent";
        String selectedSvg = "#654f9e";
        String selectedBg = "#282836";

        styleButton(homeButton, "home.svg", normalSvg, hoverBg, selectedSvg, selectedBg);
        styleButton(postsButton, "stacks.svg", normalSvg, hoverBg, selectedSvg, selectedBg);
        styleButton(settingsButton, "settings.svg", normalSvg, hoverBg, selectedSvg, selectedBg);
        styleButton(previewButton, "eye.svg", normalSvg, hoverBg, selectedSvg, selectedBg);
    }

    private void styleButton(Button button, String svgFile, String nS, String hB, String sS, String sB) {
        ButtonStyler.with(button)
                .svgPath("src/main/resources/svg/" + svgFile)
                .iconSize(16)
                .normalColors(nS, hB)
                .hoverColors(nS, hB)
                .selectedColors(sS, sB)
                .apply();
    }

    private void setSelectedButton(Button button) {
        if (currentSelectedButton != null) {
            currentSelectedButton.getStyleClass().remove("selected");
        }
        button.getStyleClass().add("selected");
        currentSelectedButton = button;
    }

    @FXML private void onHomeClick() {
        setSelectedButton(homeButton);
        titleLabel.setText("Dashboard");
        contentNavigationManager.showHome();
    }

    @FXML private void onPreviewClick() {
        setSelectedButton(previewButton);
        titleLabel.setText("Preview");
        contentNavigationManager.showPreview();
    }

    @FXML private void onPostsClick() {
        setSelectedButton(postsButton);
        titleLabel.setText("Posts");
        contentNavigationManager.showPosts();
    }

    @FXML private void onSettingsClick() {
        setSelectedButton(settingsButton);
        titleLabel.setText("Settings");
        contentNavigationManager.showSettings();
    }



    @Override
    public Parent getView() {
        return root;
    }
}