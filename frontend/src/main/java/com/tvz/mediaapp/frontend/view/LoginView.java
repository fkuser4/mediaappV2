package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.animation.FloatingIcon;
import com.tvz.mediaapp.frontend.viewmodel.LoginViewModel;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class LoginView implements View {
    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);
    private static final int MAX_ICONS = 15;

    @FXML private StackPane rootStack;
    @FXML private GridPane gridPane;
    @FXML private VBox leftPanel;
    @FXML private StackPane rightPanel;
    @FXML private Canvas animationCanvas;
    @FXML private ImageView logoImageView;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;

    private final LoginViewModel viewModel;
    private Parent root;

    private final Set<FloatingIcon> floatingIcons = new HashSet<>();
    private Timeline iconGenerator;
    private AnimationTimer animationTimer;

    @Inject
    public LoginView(LoginViewModel viewModel) {
        this.viewModel = viewModel;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            loader.setController(this);
            root = loader.load();

            root.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/login-view.css")).toExternalForm()
            );
        } catch (IOException e) {
            logger.error("Failed to load FXML for LoginView", e);
            throw new RuntimeException("Failed to load FXML for LoginView", e);
        }
    }

    @FXML
    public void initialize() {
        setupViewModelBindings();
        setupLogo();
        setupCanvas();
        startAnimation();
    }

    private void setupViewModelBindings() {
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        rememberMeCheckBox.selectedProperty().bindBidirectional(viewModel.rememberMeProperty());

        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());

        loginButton.disableProperty().bind(viewModel.isLoggingInProperty());
        viewModel.isLoggingInProperty().addListener((obs, wasLoading, isLoading) ->
                loginButton.setText(isLoading ? "LOGGING IN..." : "LOGIN")
        );

        usernameField.textProperty().addListener((obs, o, n) -> viewModel.clearError());
        passwordField.textProperty().addListener((obs, o, n) -> viewModel.clearError());
    }

    private void setupLogo() {
        try {
            Image logo = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))
            );
            logoImageView.setImage(logo);
            logoImageView.preserveRatioProperty().setValue(true);
        } catch (Exception e) {
            logger.warn("Could not load logo image: " + e.getMessage());
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }
    }

    private void setupCanvas() {
        animationCanvas.widthProperty().bind(rightPanel.widthProperty());
        animationCanvas.heightProperty().bind(rightPanel.heightProperty());

        animationCanvas.widthProperty().addListener((obs, oldW, newW) -> generateIcon());
        animationCanvas.heightProperty().addListener((obs, oldH, newH) -> generateIcon());
    }

    private void startAnimation() {
        startIconGenerator();
        startAnimationTimer();
    }

    private void startIconGenerator() {
        iconGenerator = new Timeline(
                new KeyFrame(Duration.seconds(1.5), e -> {
                    if (floatingIcons.size() < MAX_ICONS && animationCanvas.getWidth() > 0) {
                        generateIcon();
                    }
                })
        );
        iconGenerator.setCycleCount(Timeline.INDEFINITE);
        iconGenerator.play();
    }

    private void generateIcon() {
        if (floatingIcons.size() < MAX_ICONS && animationCanvas.getWidth() > 0) {
            double x = ThreadLocalRandom.current().nextDouble(0, Math.max(1, animationCanvas.getWidth() - 50));
            floatingIcons.add(new FloatingIcon(x, -50));
        }
    }

    private void startAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAndDrawIcons();
            }
        };
        animationTimer.start();
    }

    private void updateAndDrawIcons() {
        GraphicsContext gc = animationCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, animationCanvas.getWidth(), animationCanvas.getHeight());

        Iterator<FloatingIcon> iterator = floatingIcons.iterator();
        while (iterator.hasNext()) {
            FloatingIcon icon = iterator.next();
            icon.update();
            if (icon.isOffScreen((int) animationCanvas.getWidth(), (int) animationCanvas.getHeight())) {
                iterator.remove();
            } else {
                icon.draw(gc);
            }
        }
    }

    @FXML private void handleLogin() { viewModel.login(); }
    @FXML private void handleForgotPassword() { }


    @Override public Parent getView() { return root; }
}