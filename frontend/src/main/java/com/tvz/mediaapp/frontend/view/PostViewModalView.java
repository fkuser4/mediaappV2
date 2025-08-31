package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.MediaType;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.model.Status;
import com.tvz.mediaapp.frontend.utils.ButtonStyler;
import com.tvz.mediaapp.frontend.viewmodel.PostViewModalViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class PostViewModalView implements View {
    private static final String VIDEO_ICON_SVG_PATH = "M10 8 L10 16 L16 12 Z";

    private final Parent root;
    private PostViewModalViewModel viewModel;

    @FXML private Label postTitleLabel;
    @FXML private Button closeButton;
    @FXML private TextArea contentDisplayArea;
    @FXML private VBox mediaDisplayContainer;
    @FXML private Label mediaLabel;
    @FXML private HBox linkDisplayContainer;
    @FXML private TextField linkDisplayField;
    @FXML private ScrollPane mediaGridScroll;
    @FXML private FlowPane mediaGrid;
    @FXML private HBox statusContainer;

    @Inject
    public PostViewModalView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/post-view-modal.fxml"));
            loader.setController(this);
            root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/post-view-modal.css")).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for PostViewModalView", e);
        }
    }

    public void setViewModel(PostViewModalViewModel viewModel) {
        this.viewModel = viewModel;
        setupBindings();
        setupStatusCapsules();
        setupMediaGrid();

        Platform.runLater(() -> {
            if (viewModel.currentPostProperty().get() != null) {
                displayPostData(viewModel.currentPostProperty().get());
            }
            updateMediaGridDisplay();
        });
    }

    @FXML
    public void initialize() {
        setupCloseButton();
    }

    private void setupBindings() {
        viewModel.currentPostProperty().addListener((obs, oldPost, newPost) -> {
            if (newPost != null) {
                displayPostData(newPost);
            }
        });
    }

    private void setupStatusCapsules() {
        statusContainer.getChildren().clear();
        ToggleGroup statusToggleGroup = new ToggleGroup();

        for (Status status : Status.values()) {
            ToggleButton capsule = new ToggleButton(status.getDisplayName());
            capsule.setUserData(status);
            capsule.getStyleClass().addAll("status-capsule", status.getStyleClass());
            capsule.setToggleGroup(statusToggleGroup);
            statusContainer.getChildren().add(capsule);
        }

        statusToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                Status newStatus = (Status) newToggle.getUserData();
                if (viewModel.selectedStatusProperty().get() != newStatus) {
                    viewModel.selectedStatusProperty().set(newStatus);
                }
            }
        });

        viewModel.selectedStatusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (newStatus != null) {
                for(Node node : statusContainer.getChildren()) {
                    if (node instanceof ToggleButton toggle && toggle.getUserData() == newStatus) {
                        toggle.setSelected(true);
                        break;
                    }
                }
            } else {
                statusToggleGroup.selectToggle(null);
            }
        });
    }

    private void setupMediaGrid() {
        viewModel.getMediaItems().addListener((ListChangeListener<PostViewModalViewModel.MediaItem>) c -> Platform.runLater(this::updateMediaGridDisplay));
    }

    private void displayPostData(Post post) {
        postTitleLabel.setText(post.getTitle());
        contentDisplayArea.setText(post.getContent());

        for (Node node : statusContainer.getChildren()) {
            if (node instanceof ToggleButton capsule) {
                if (capsule.getUserData() == post.getStatus()) {
                    capsule.setSelected(true);
                    break;
                }
            }
        }

        MediaType type = post.getMediaType();
        boolean hasMedia = type != null && type != MediaType.NONE && post.getMediaUris() != null && !post.getMediaUris().isEmpty();

        mediaDisplayContainer.setVisible(hasMedia);
        mediaDisplayContainer.setManaged(hasMedia);

        linkDisplayContainer.setVisible(type == MediaType.LINK);
        linkDisplayContainer.setManaged(type == MediaType.LINK);

        boolean showGrid = type == MediaType.IMAGE || type == MediaType.VIDEO;
        mediaGridScroll.setVisible(showGrid);
        mediaGridScroll.setManaged(showGrid);


        if (type == MediaType.LINK) {
            mediaLabel.setText("Link");
            linkDisplayField.setText(post.getMediaUris().get(0));
        } else if (type == MediaType.IMAGE) {
            mediaLabel.setText("Images");
        } else if (type == MediaType.VIDEO) {
            mediaLabel.setText("Video");
        }
    }

    private void updateMediaGridDisplay() {
        mediaGrid.getChildren().clear();

        for (PostViewModalViewModel.MediaItem item : viewModel.getMediaItems()) {
            Node thumbnail = createMediaThumbnail(item);
            mediaGrid.getChildren().add(thumbnail);
        }

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> refreshExistingThumbnails()));
        timeline.play();
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
                            imageView.imageProperty().unbind();
                            imageView.imageProperty().bind(prop);
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

    private Node createMediaThumbnail(PostViewModalViewModel.MediaItem item) {
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.getStyleClass().add("media-thumbnail");
        thumbnailPane.setUserData(item.uniqueId);

        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("thumbnail-image");
        imageView.setFitWidth(108);
        imageView.setFitHeight(108);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        ObjectProperty<Image> thumbnailProperty = viewModel.getThumbnailProperty(item.uniqueId);
        imageView.imageProperty().bind(thumbnailProperty);

        Node previewNode = imageView;
        if (viewModel.currentPostProperty().get() != null &&
                viewModel.currentPostProperty().get().getMediaType() == MediaType.VIDEO) {
            SVGPath videoIcon = new SVGPath();
            videoIcon.setContent(VIDEO_ICON_SVG_PATH);
            videoIcon.getStyleClass().add("video-thumbnail-icon");
            previewNode = new StackPane(imageView, videoIcon);
        }

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("media-context-menu");
        MenuItem downloadItem = new MenuItem("Download");
        downloadItem.setOnAction(e -> handleDownload(item.serverFilename));
        contextMenu.getItems().add(downloadItem);

        thumbnailPane.setOnContextMenuRequested(event ->
                contextMenu.show(thumbnailPane, event.getScreenX(), event.getScreenY()));

        thumbnailPane.getChildren().add(previewNode);

        return thumbnailPane;
    }

    private void handleDownload(String serverFilename) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Download Location");
        File selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

        if (selectedDirectory != null) {
            viewModel.downloadMedia(serverFilename, selectedDirectory);
        }
    }

    private void setupCloseButton() {
        ButtonStyler.with(closeButton)
                .svgPath("src/main/resources/svg/close.svg")
                .iconSize(14).normalColors("#b3b3b5", "transparent").hoverColors("#ffffff", "rgba(255, 255, 255, 0.1)")
                .apply();
    }

    @FXML
    private void handleClose() {
        viewModel.close();
    }

    @Override
    public Parent getView() {
        return root;
    }
}