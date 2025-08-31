package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.Platform;
import com.tvz.mediaapp.frontend.model.SessionManager;
import com.tvz.mediaapp.frontend.service.AuthService;
import com.tvz.mediaapp.frontend.service.NavigationManager;
import com.tvz.mediaapp.frontend.service.UserPreferencesService;
import com.tvz.mediaapp.frontend.viewmodel.PostsViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class SettingsView implements View {
    private static final Logger logger = LoggerFactory.getLogger(SettingsView.class);

    @FXML private RadioButton ddmmyyyyRadio;
    @FXML private RadioButton mmddyyyyRadio;
    @FXML private ToggleGroup dateFormatGroup;

    @FXML private HBox facebookBox;
    @FXML private HBox xBox;
    @FXML private HBox instagramBox;
    @FXML private HBox tiktokBox;
    @FXML private HBox youtubeBox;

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Button logoutButton;
    @FXML private VBox contentVBox;


    private final Parent root;
    private final UserPreferencesService userPreferencesService;
    private final AuthService authService;
    private final NavigationManager navigationManager;
    private final SessionManager sessionManager;
    private final PostsViewModel postsViewModel;

    @Inject
    public SettingsView(UserPreferencesService userPreferencesService,
                        AuthService authService,
                        NavigationManager navigationManager,
                        SessionManager sessionManager,
                        PostsViewModel postsViewModel) {
        this.userPreferencesService = userPreferencesService;
        this.authService = authService;
        this.navigationManager = navigationManager;
        this.sessionManager = sessionManager;
        this.postsViewModel = postsViewModel;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings-view.fxml"));
            loader.setController(this);
            this.root = loader.load();
            this.root.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/settings-view.css")).toExternalForm()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for SettingsView", e);
        }
    }

    @FXML
    public void initialize() {
        setupDateFormatBindings();
        setupPlatformBindings();
        setupAccountInfo();
        setupLogoutButton();

        javafx.application.Platform.runLater(() -> {
            if (ddmmyyyyRadio != null && mmddyyyyRadio != null) {
                setupDateFormatBindings();
            }

            if (arePlatformBoxesReady()) {
                updatePlatformBoxes();
            }

            if (usernameLabel != null && emailLabel != null) {
                updateAccountInfo();
            }

            if (logoutButton != null) {
                setupLogoutButton();
            }
        });

        javafx.application.Platform.runLater(this::setupDynamicHeight);

        logger.info("SettingsView initialized successfully");
    }
    private void setupDynamicHeight() {
        Scene scene = root.getScene();
        if (scene != null && contentVBox != null) {
            contentVBox.minHeightProperty().bind(scene.heightProperty().subtract(115));
        }
    }

    private void setupDateFormatBindings() {
        if (ddmmyyyyRadio == null || mmddyyyyRadio == null) {
            return;
        }

        if (dateFormatGroup == null) {
            dateFormatGroup = new ToggleGroup();
        }

        ddmmyyyyRadio.setToggleGroup(dateFormatGroup);
        mmddyyyyRadio.setToggleGroup(dateFormatGroup);

        ddmmyyyyRadio.setSelected(!userPreferencesService.isUseAmericanDateFormat());
        mmddyyyyRadio.setSelected(userPreferencesService.isUseAmericanDateFormat());

        dateFormatGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                boolean useAmerican = newToggle == mmddyyyyRadio;
                userPreferencesService.setUseAmericanDateFormat(useAmerican);
            }
        });

        userPreferencesService.useAmericanDateFormatProperty().addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                if (ddmmyyyyRadio != null && mmddyyyyRadio != null) {
                    if (newVal) {
                        mmddyyyyRadio.setSelected(true);
                    } else {
                        ddmmyyyyRadio.setSelected(true);
                    }
                }
            });
        });
    }

    private void setupPlatformBindings() {
        if (arePlatformBoxesReady()) {
            updatePlatformBoxes();
        }

        userPreferencesService.getSelectedPlatforms().addListener((javafx.collections.SetChangeListener<Platform>) change -> {
            javafx.application.Platform.runLater(() -> {
                if (arePlatformBoxesReady()) {
                    updatePlatformBoxes();
                }
            });
        });
    }

    private boolean arePlatformBoxesReady() {
        return facebookBox != null && xBox != null && instagramBox != null &&
                tiktokBox != null && youtubeBox != null;
    }

    private void updatePlatformBoxes() {
        if (!arePlatformBoxesReady()) {
            return;
        }

        updatePlatformBox(facebookBox, Platform.FACEBOOK);
        updatePlatformBox(xBox, Platform.X);
        updatePlatformBox(instagramBox, Platform.INSTAGRAM);
        updatePlatformBox(tiktokBox, Platform.TIKTOK);
        updatePlatformBox(youtubeBox, Platform.YOUTUBE);

        updatePlatformStates();
    }

    private void updatePlatformBox(HBox box, Platform platform) {
        if (box == null) {
            return;
        }

        boolean isSelected = userPreferencesService.isPlatformSelected(platform);
        box.getStyleClass().removeAll("selected", "disabled");
        if (isSelected) {
            box.getStyleClass().add("selected");
        }
    }

    private void handlePlatformBoxClick(Platform platform, HBox box) {
        if (box == null || box.getStyleClass().contains("disabled")) {
            return;
        }

        if (userPreferencesService.isPlatformSelected(platform)) {
            if (userPreferencesService.canDeselectPlatform(platform)) {
                userPreferencesService.deselectPlatform(platform);
            }
        } else {
            userPreferencesService.selectPlatform(platform);
        }
        updatePlatformStates();
    }

    private void updatePlatformStates() {
        if (!arePlatformBoxesReady()) {
            return;
        }

        boolean onlyOneSelected = userPreferencesService.getSelectedPlatforms().size() == 1;

        updatePlatformBoxState(facebookBox, Platform.FACEBOOK, onlyOneSelected);
        updatePlatformBoxState(xBox, Platform.X, onlyOneSelected);
        updatePlatformBoxState(instagramBox, Platform.INSTAGRAM, onlyOneSelected);
        updatePlatformBoxState(tiktokBox, Platform.TIKTOK, onlyOneSelected);
        updatePlatformBoxState(youtubeBox, Platform.YOUTUBE, onlyOneSelected);
    }

    private void updatePlatformBoxState(HBox box, Platform platform, boolean onlyOneSelected) {
        if (box == null) {
            return;
        }

        boolean isSelected = userPreferencesService.isPlatformSelected(platform);

        box.getStyleClass().removeAll("selected", "disabled");

        if (isSelected) {
            box.getStyleClass().add("selected");
            if (onlyOneSelected) {
                box.getStyleClass().add("disabled");
            }
        }
    }

    @FXML
    private void handleFacebookClick() {
        if (facebookBox != null) {
            handlePlatformBoxClick(Platform.FACEBOOK, facebookBox);
        }
    }

    @FXML
    private void handleXClick() {
        if (xBox != null) {
            handlePlatformBoxClick(Platform.X, xBox);
        }
    }

    @FXML
    private void handleInstagramClick() {
        if (instagramBox != null) {
            handlePlatformBoxClick(Platform.INSTAGRAM, instagramBox);
        }
    }

    @FXML
    private void handleTiktokClick() {
        if (tiktokBox != null) {
            handlePlatformBoxClick(Platform.TIKTOK, tiktokBox);
        }
    }

    @FXML
    private void handleYoutubeClick() {
        if (youtubeBox != null) {
            handlePlatformBoxClick(Platform.YOUTUBE, youtubeBox);
        }
    }

    private void setupAccountInfo() {
        if (usernameLabel == null || emailLabel == null) {
            return;
        }

        sessionManager.currentUserProperty().addListener((obs, oldUser, newUser) -> {
            javafx.application.Platform.runLater(this::updateAccountInfo);
        });

        updateAccountInfo();
    }

    private void updateAccountInfo() {
        if (usernameLabel == null || emailLabel == null) {
            return;
        }

        if (sessionManager.getCurrentUser() != null) {
            usernameLabel.setText(sessionManager.getCurrentUser().getUsername());
            emailLabel.setText(sessionManager.getCurrentUser().getEmail());
        } else {
            usernameLabel.setText("Not logged in");
            emailLabel.setText("Not logged in");
        }
    }

    private void setupLogoutButton() {
        if (logoutButton == null) {
            return;
        }
        logoutButton.setOnAction(e -> handleLogout());
    }

    @FXML
    private void handleLogout() {
        logger.info("User initiated logout");

        performLogout();
    }

    private void performLogout() {
        authService.logout();
    }

    @Override
    public Parent getView() {
        return root;
    }
}