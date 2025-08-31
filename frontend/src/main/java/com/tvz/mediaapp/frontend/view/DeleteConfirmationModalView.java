package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.service.NavigationManager;
import com.tvz.mediaapp.frontend.service.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DeleteConfirmationModalView implements View {
    private static final Logger logger = LoggerFactory.getLogger(DeleteConfirmationModalView.class);

    private final Parent root;
    private final NavigationManager navigationManager;
    private final NotificationService notificationService;

    @FXML private Label postTitleLabel;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;

    private Post postToDelete;
    private Function<Post, CompletableFuture<Void>> onDeleteConfirmed;

    @Inject
    public DeleteConfirmationModalView(NavigationManager navigationManager,
                                       NotificationService notificationService) {
        this.navigationManager = navigationManager;
        this.notificationService = notificationService;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/delete-confirmation-modal.fxml"));
            loader.setController(this);
            root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/delete-confirmation-modal.css")).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for DeleteConfirmationModalView", e);
        }
    }

    @FXML
    public void initialize() {
    }

    public void showDeleteConfirmation(Post post, Function<Post, CompletableFuture<Void>> onDeleteConfirmed) {
        this.postToDelete = post;
        this.onDeleteConfirmed = onDeleteConfirmed;

        if (post != null) {
            postTitleLabel.setText(post.getTitle());
        }

        navigationManager.showModal(this.getView());
    }

    @FXML
    private void handleCancel() {
        navigationManager.hideModal();
    }

    @FXML
    private void handleDelete() {
        if (postToDelete != null && onDeleteConfirmed != null) {
            deleteButton.setDisable(true);
            deleteButton.setText("Deleting...");

            CompletableFuture<Void> deletionFuture = onDeleteConfirmed.apply(postToDelete);

            deletionFuture.whenComplete((result, throwable) -> {
                Platform.runLater(() -> {
                    deleteButton.setDisable(false);
                    deleteButton.setText("Delete");

                    if (throwable == null) {
                        navigationManager.hideModal();
                    }
                });
            });
        }
    }

    @Override
    public Parent getView() {
        return root;
    }
}