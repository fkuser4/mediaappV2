package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.MediaType;
import com.tvz.mediaapp.frontend.model.Platform;
import com.tvz.mediaapp.frontend.service.UserPreferencesService;
import com.tvz.mediaapp.frontend.utils.ButtonStyler;
import com.tvz.mediaapp.frontend.viewmodel.PostCreateEditModalViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PostCreateEditModalView implements View {
    private static final String VIDEO_ICON_SVG_PATH = "M10 8 L10 16 L16 12 Z";

    private final Parent root;
    private PostCreateEditModalViewModel viewModel;
    private final UserPreferencesService userPreferencesService;

    @FXML private Label modalTitleLabel;
    @FXML private Button closeButton;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private TextField dateField;
    @FXML private FlowPane platformContainer;
    @FXML private ToggleButton linkToggle;
    @FXML private ToggleButton imageToggle;
    @FXML private ToggleButton videoToggle;
    @FXML private VBox linkInputContainer;
    @FXML private TextField linkField;
    @FXML private VBox mediaUploadContainer;
    @FXML private VBox dragDropArea;
    @FXML private Label dragDropText;
    @FXML private ScrollPane mediaGridScroll;
    @FXML private FlowPane mediaGrid;
    @FXML private Button resetButton;
    @FXML private Button saveButton;
    @FXML private Label titleErrorLabel;
    @FXML private Label contentErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label platformsErrorLabel;
    @FXML private Label linkUrlErrorLabel;
    @FXML private Label generalErrorLabel;

    @Inject
    public PostCreateEditModalView(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/post-create-edit-modal.fxml"));
            loader.setController(this);
            root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/post-create-edit-modal.css")).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML", e);
        }
    }

    public void setViewModel(PostCreateEditModalViewModel viewModel) {
        this.viewModel = viewModel;
        setupDataBindings();
        setupFieldValidationAndLiveErrors();
        setupMediaTypeSelection();
        setupDragAndDrop();
        setupMediaDisplay();
        setupModeDetection();

        javafx.application.Platform.runLater(this::updateMediaGrid);
    }

    @FXML
    public void initialize() {
        setupCloseButton();
    }

    public void prepareForDisplay() {

        setupPlatformToggles();
        setupFieldValidationAndLiveErrors();
        updatePlatformSelection();
        updateToggleSelection();

        LocalDate currentDate = viewModel.publishDateProperty().get();
        if (currentDate != null) {
            dateField.setText(userPreferencesService.formatDateForDisplay(currentDate));
        } else {
            dateField.setText("");
        }

        MediaType currentType = viewModel.mediaTypeProperty().get();
        updateMediaTypeView(currentType);

        javafx.application.Platform.runLater(this::updateMediaGrid);

    }

    private void setupDataBindings() {
        titleField.textProperty().bindBidirectional(viewModel.titleProperty());
        contentArea.textProperty().bindBidirectional(viewModel.contentProperty());
        linkField.textProperty().bindBidirectional(viewModel.linkUrlProperty());

        bindErrorLabel(titleErrorLabel, viewModel.titleErrorProperty(), titleField);
        bindErrorLabel(contentErrorLabel, viewModel.contentErrorProperty(), contentArea);
        bindErrorLabel(dateErrorLabel, viewModel.dateErrorProperty(), dateField);
        bindErrorLabel(platformsErrorLabel, viewModel.platformsErrorProperty(), platformContainer);
        bindErrorLabel(linkUrlErrorLabel, viewModel.linkUrlErrorProperty(), linkField);
        bindErrorLabel(generalErrorLabel, viewModel.generalErrorProperty(), null);

        saveButton.disableProperty().bind(
                viewModel.isSavingProperty().or(viewModel.hasActiveTasksProperty())
        );

        saveButton.textProperty().bind(
                Bindings.when(viewModel.isSavingProperty()).then("Saving...")
                        .otherwise(Bindings.when(viewModel.hasActiveTasksProperty()).then("Processing...").otherwise("Save Post"))
        );

        viewModel.publishDateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !dateField.isFocused()) {
                dateField.setText(newVal.format(userPreferencesService.getDateFormatter()));
            }
        });
    }

    @FXML
    private void handleClose() {
        viewModel.close();
    }

    private void setupCloseButton() {
        ButtonStyler.with(closeButton)
                .svgPath("src/main/resources/svg/close.svg")
                .iconSize(14).normalColors("#b3b3b5", "transparent").hoverColors("#ffffff", "rgba(255, 255, 255, 0.1)")
                .apply();
    }

    private void bindErrorLabel(Label label, StringProperty property, Node inputField) {
        label.textProperty().bind(property);
        BooleanBinding hasError = property.isNotNull().and(property.isNotEmpty());
        label.visibleProperty().bind(hasError);
        label.managedProperty().bind(hasError);

        if (inputField != null) {
            hasError.addListener((obs, oldVal, newVal) -> {
                if (Boolean.TRUE.equals(newVal)) {
                    if (!inputField.getStyleClass().contains("error")) inputField.getStyleClass().add("error");
                } else {
                    inputField.getStyleClass().remove("error");
                }
            });
        }
    }

    private void setupFieldValidationAndLiveErrors() {
        titleField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().length() > 255 ? null : change));

        titleField.textProperty().addListener(c -> viewModel.titleErrorProperty().set(null));
        contentArea.textProperty().addListener(c -> viewModel.contentErrorProperty().set(null));
        viewModel.selectedPlatformsProperty().addListener((ListChangeListener<Platform>) c -> viewModel.platformsErrorProperty().set(null));
        linkField.textProperty().addListener(c -> viewModel.linkUrlErrorProperty().set(null));

        dateField.setTextFormatter(null);
        dateField.setPromptText(userPreferencesService.isUseAmericanDateFormat() ? "MM/DD/YYYY" : "DD/MM/YYYY");
        dateField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (wasFocused) { validateAndSetDate(dateField.getText()); }
        });
        dateField.textProperty().addListener((obs, oldVal, newVal) -> validateAndSetDate(newVal));
    }

    private void validateAndSetDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            viewModel.publishDateProperty().set(null);
            viewModel.dateErrorProperty().set(null);
            return;
        }
        try {
            viewModel.publishDateProperty().set(LocalDate.parse(dateText.trim(), userPreferencesService.getDateFormatter()));
            viewModel.dateErrorProperty().set(null);
        } catch (DateTimeParseException e) {
            viewModel.publishDateProperty().set(null);
        }
    }

    private void setupModeDetection() {
        modalTitleLabel.textProperty().bind(Bindings.when(viewModel.editingPostProperty().isNotNull()).then("Edit Post").otherwise("Create New Post"));
    }

    private void setupPlatformToggles() {
        platformContainer.getChildren().clear();
        for (Platform p : userPreferencesService.getSelectedPlatforms()) {
            ToggleButton tb = new ToggleButton(p.toString());
            tb.getStyleClass().add("platform-toggle");
            tb.setUserData(p);
            tb.setOnAction(event -> viewModel.togglePlatform(p));
            platformContainer.getChildren().add(tb);
        }
        viewModel.selectedPlatformsProperty().addListener((ListChangeListener<Platform>) c -> updatePlatformSelection());
        updatePlatformSelection();
    }

    private void updatePlatformSelection() {
        platformContainer.getChildren().forEach(node -> {
            if (node instanceof ToggleButton tb) {
                boolean isSelected = viewModel.selectedPlatformsProperty().contains((Platform) tb.getUserData());
                tb.setSelected(isSelected);
                if (isSelected) {
                    if (!tb.getStyleClass().contains("selected")) tb.getStyleClass().add("selected");
                } else {
                    tb.getStyleClass().remove("selected");
                }
            }
        });
    }

    private void setupMediaTypeSelection() {
        ToggleGroup mediaToggleGroup = new ToggleGroup();
        linkToggle.setToggleGroup(mediaToggleGroup);
        imageToggle.setToggleGroup(mediaToggleGroup);
        videoToggle.setToggleGroup(mediaToggleGroup);
        mediaToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            MediaType selectedType = MediaType.NONE;
            if (newVal == linkToggle) selectedType = MediaType.LINK;
            else if (newVal == imageToggle) selectedType = MediaType.IMAGE;
            else if (newVal == videoToggle) selectedType = MediaType.VIDEO;
            viewModel.mediaTypeProperty().set(selectedType);
        });
        viewModel.mediaTypeProperty().addListener((obs, oldType, newType) -> {
            updateMediaTypeView(newType);
            updateMediaGrid();
        });
    }

    private void updateMediaTypeView(MediaType type) {
        type = (type == null) ? MediaType.NONE : type;

        linkInputContainer.setVisible(type == MediaType.LINK);
        linkInputContainer.setManaged(type == MediaType.LINK);

        boolean showMediaUpload = type == MediaType.IMAGE || type == MediaType.VIDEO;
        mediaUploadContainer.setVisible(showMediaUpload);
        mediaUploadContainer.setManaged(showMediaUpload);

        if (type == MediaType.IMAGE) {
            dragDropText.setText("📸 Drag & Drop Images Here");
        } else if (type == MediaType.VIDEO) {
            dragDropText.setText("🎥 Drag & Drop Video Here");
        }
    }

    private void updateToggleSelection() {
        MediaType type = viewModel.mediaTypeProperty().get();
        if (type == MediaType.LINK) linkToggle.setSelected(true);
        else if (type == MediaType.IMAGE) imageToggle.setSelected(true);
        else if (type == MediaType.VIDEO) videoToggle.setSelected(true);
        else if (linkToggle.getToggleGroup() != null) {
            linkToggle.getToggleGroup().selectToggle(null);
        }
    }

    private void setupDragAndDrop() {
        dragDropArea.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                dragDropArea.getStyleClass().add("drag-over");
            }
            event.consume();
        });
        dragDropArea.setOnDragExited(event -> dragDropArea.getStyleClass().remove("drag-over"));
        dragDropArea.setOnDragDropped(event -> {
            if (event.getDragboard().hasFiles()) handleFiles(event.getDragboard().getFiles());
            dragDropArea.getStyleClass().remove("drag-over");
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void setupMediaDisplay() {
        viewModel.getImageMediaFiles().addListener((ListChangeListener<PostCreateEditModalViewModel.MediaItem>) c -> {
            updateMediaGrid();
        });
        viewModel.getVideoMediaFiles().addListener((ListChangeListener<PostCreateEditModalViewModel.MediaItem>) c -> {
            updateMediaGrid();
        });
    }

    private void updateMediaGrid() {
        mediaGrid.getChildren().clear();

        MediaType currentType = viewModel.mediaTypeProperty().get();
        ObservableList<PostCreateEditModalViewModel.MediaItem> filesToShow =
                (currentType == MediaType.IMAGE) ? viewModel.getImageMediaFiles() :
                        (currentType == MediaType.VIDEO) ? viewModel.getVideoMediaFiles() : null;

        if (filesToShow != null) {

            for (PostCreateEditModalViewModel.MediaItem mediaFile : filesToShow) {
                Node thumbnail = createMediaThumbnail(mediaFile);
                mediaGrid.getChildren().add(thumbnail);
            }

            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> refreshExistingThumbnails()));
            timeline.play();
        }

        boolean hasMedia = filesToShow != null && !filesToShow.isEmpty();
        mediaGridScroll.setVisible(hasMedia);
        mediaGridScroll.setManaged(hasMedia);

    }

    private void refreshExistingThumbnails() {
        for (Node node : mediaGrid.getChildren()) {
            if (node instanceof StackPane stackPane) {
                ImageView imageView = findImageView(stackPane);
                if (imageView != null && imageView.getImage() == null) {
                    String mediaId = (String) stackPane.getUserData();
                    if (mediaId != null) {
                        ObjectProperty<Image> prop = viewModel.getThumbnailProperty(mediaId);
                        if (prop.get() != null) {
                            imageView.setImage(prop.get());
                        }
                    }
                }
            }
        }
    }

    private ImageView findImageView(StackPane parent) {
        for (Node child : parent.getChildren()) {
            if (child instanceof ImageView) {
                return (ImageView) child;
            } else if (child instanceof StackPane) {
                ImageView found = findImageView((StackPane) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Node createMediaThumbnail(PostCreateEditModalViewModel.MediaItem mediaItem) {
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.getStyleClass().add("media-thumbnail");
        thumbnailPane.setUserData(mediaItem.uniqueId);

        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("thumbnail-image");
        imageView.setFitWidth(108);
        imageView.setFitHeight(108);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        ObjectProperty<Image> thumbnailProperty = viewModel.getThumbnailProperty(mediaItem.uniqueId);

        imageView.imageProperty().bind(thumbnailProperty);

        thumbnailProperty.addListener((obs, oldImg, newImg) -> {
            if (newImg != null && imageView.getImage() == null) {
                javafx.application.Platform.runLater(() -> {
                    imageView.imageProperty().unbind();
                    imageView.imageProperty().bind(thumbnailProperty);
                });
            }
        });

        Node previewNode = imageView;
        MediaType currentType = viewModel.mediaTypeProperty().get();
        if (currentType == MediaType.VIDEO) {
            SVGPath videoIcon = new SVGPath();
            videoIcon.setContent(VIDEO_ICON_SVG_PATH);
            videoIcon.getStyleClass().add("video-thumbnail-icon");
            previewNode = new StackPane(imageView, videoIcon);
        }

        Button removeBtn = new Button("×");
        removeBtn.getStyleClass().add("remove-media-button");
        removeBtn.setOnAction(e -> viewModel.removeMedia(mediaItem.uniqueId));
        StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);

        thumbnailPane.getChildren().addAll(previewNode, removeBtn);

        return thumbnailPane;
    }

    private void handleFiles(List<File> files) {
        if (!validateMediaFiles(files)) return;
        viewModel.addMediaFiles(files);
    }

    private boolean validateMediaFiles(List<File> files) {
        MediaType currentType = viewModel.mediaTypeProperty().get();
        viewModel.generalErrorProperty().set(null);

        for (File file : files) {
            String name = file.getName().toLowerCase();

            boolean isImage = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                    name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");

            boolean isVideo = name.endsWith(".mp4");

            if (currentType == MediaType.IMAGE && !isImage) {
                viewModel.generalErrorProperty().set("Invalid file type for images: " + file.getName());
                return false;
            }
            if (currentType == MediaType.VIDEO && !isVideo) {
                viewModel.generalErrorProperty().set("Invalid file type for video: " + file.getName());
                return false;
            }
        }
        return true;
    }

    @FXML private void handleBrowseFiles() {
        FileChooser fileChooser = new FileChooser();
        MediaType type = viewModel.mediaTypeProperty().get();
        if (type == MediaType.IMAGE) {
            fileChooser.setTitle("Select Image Files");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
            List<File> files = fileChooser.showOpenMultipleDialog(root.getScene().getWindow());
            if (files != null) handleFiles(files);
        } else if (type == MediaType.VIDEO) {
            fileChooser.setTitle("Select Video File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.avi", "*.mkv", "*.webm", "*.m4v"));
            File file = fileChooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) handleFiles(Collections.singletonList(file));
        }
    }

    @FXML private void handleReset() {
        viewModel.resetFields();
        dateField.setText("");
    }

    @FXML private void handleSave() {
        validateAndSetDate(dateField.getText());
        viewModel.save();
    }

    @Override public Parent getView() { return root; }
}