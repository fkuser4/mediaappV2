package com.tvz.mediaapp.frontend.viewmodel;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.MediaType;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.model.Status;
import com.tvz.mediaapp.frontend.repository.PostApiRepository;
import com.tvz.mediaapp.frontend.service.NotificationService;
import com.tvz.mediaapp.frontend.utils.PostMapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PostViewModalViewModel {
    private static final Logger logger = LoggerFactory.getLogger(PostViewModalViewModel.class);

    private final PostApiRepository postApiRepository;
    private final NotificationService notificationService;

    private final ObjectProperty<Post> currentPost = new SimpleObjectProperty<>();
    private final ObjectProperty<Status> selectedStatus = new SimpleObjectProperty<>();
    private final ObservableList<MediaItem> mediaItems = FXCollections.observableArrayList();
    private final Map<String, ObjectProperty<Image>> thumbnailProperties = new ConcurrentHashMap<>();

    private Runnable onCloseCallback;
    private Runnable onUpdateCallback;

    public static class MediaItem {
        public final String uniqueId;
        public final String serverFilename;
        public MediaItem(String serverFilename) {
            this.uniqueId = serverFilename;
            this.serverFilename = serverFilename;
        }
    }

    @Inject
    public PostViewModalViewModel(PostApiRepository postApiRepository, NotificationService notificationService) {
        this.postApiRepository = postApiRepository;
        this.notificationService = notificationService;

        selectedStatus.addListener((obs, oldStatus, newStatus) -> {
            Post post = currentPost.get();
            if (post != null && newStatus != null && newStatus != post.getStatus()) {
                updatePostStatus(newStatus);
            }
        });
    }

    public void loadPost(Post post, Runnable onUpdateCallback) {
        this.onUpdateCallback = onUpdateCallback;
        Post postCopy = new Post(post);
        this.currentPost.set(postCopy);

        this.selectedStatus.set(postCopy.getStatus());

        mediaItems.clear();
        thumbnailProperties.clear();
        if (postCopy.getMediaType() == MediaType.IMAGE || postCopy.getMediaType() == MediaType.VIDEO) {
            if (postCopy.getMediaUris() != null) {
                for (String serverFilename : postCopy.getMediaUris()) {
                    MediaItem item = new MediaItem(serverFilename);
                    mediaItems.add(item);
                    loadThumbnailAsync(item);
                }
            }
        }
    }

    private void loadThumbnailAsync(MediaItem mediaItem) {
        CompletableFuture.runAsync(() -> {
            try {
                String thumbFilename = getThumbnailFileName(mediaItem.serverFilename);
                Map<String, String> urls = postApiRepository.generateDownloadUrls(currentPost.get().getUuid(), Collections.singletonList(thumbFilename)).get();
                if (urls.containsKey(thumbFilename)) {
                    try (InputStream in = new URL(urls.get(thumbFilename)).openStream()) {
                        Image thumbnail = new Image(in);
                        updateThumbnailProperty(mediaItem.uniqueId, thumbnail);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load thumbnail for {}", mediaItem.serverFilename, e);
            }
        });
    }

    private void updatePostStatus(Status newStatus) {
        Post postToUpdate = currentPost.get();
        postToUpdate.setStatus(newStatus);

        postApiRepository.updatePost(postToUpdate.getUuid(), PostMapper.toRequestDto(postToUpdate))
                .thenAccept(updatedPostDto -> javafx.application.Platform.runLater(() -> {
                    notificationService.showSuccess("Status updated to " + newStatus.getDisplayName());
                    if (onUpdateCallback != null) {
                        onUpdateCallback.run();
                    }
                }))
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        notificationService.showError("Failed to update status.");
                        selectedStatus.set(postToUpdate.getStatus());
                    });
                    logger.error("Failed to update post status", ex);
                    return null;
                });
    }

    public void downloadMedia(String serverFilename, File targetDirectory) {
        if (targetDirectory == null) {
            logger.warn("Download cancelled by user.");
            return;
        }

        notificationService.showInfo("Starting download for " + serverFilename);

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> urls = postApiRepository.generateDownloadUrls(currentPost.get().getUuid(), Collections.singletonList(serverFilename)).get();
                String downloadUrl = urls.get(serverFilename);
                if (downloadUrl == null || downloadUrl.isEmpty()) {
                    throw new IOException("Could not retrieve download URL for " + serverFilename);
                }

                URL url = new URL(downloadUrl);
                Path targetPath = targetDirectory.toPath().resolve(serverFilename);
                try (InputStream in = url.openStream()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }

                logger.info("Successfully downloaded {} to {}", serverFilename, targetPath);
                javafx.application.Platform.runLater(() -> notificationService.showSuccess("Download complete: " + serverFilename));

            } catch (Exception e) {
                logger.error("Download failed for {}", serverFilename, e);
                javafx.application.Platform.runLater(() -> notificationService.showError("Download failed: " + serverFilename));
            }
        });
    }

    private String getThumbnailFileName(String originalFileName) {
        int lastDotIndex = originalFileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? originalFileName + "_thumb.jpg" : originalFileName.substring(0, lastDotIndex) + "_thumb.jpg";
    }

    private void updateThumbnailProperty(String uniqueId, Image image) {
        javafx.application.Platform.runLater(() -> {
            getThumbnailProperty(uniqueId).set(image);
        });
    }

    public ObjectProperty<Post> currentPostProperty() { return currentPost; }
    public ObjectProperty<Status> selectedStatusProperty() { return selectedStatus; }
    public ObservableList<MediaItem> getMediaItems() { return mediaItems; }
    public ObjectProperty<Image> getThumbnailProperty(String uniqueId) {
        return thumbnailProperties.computeIfAbsent(uniqueId, k -> new SimpleObjectProperty<>());
    }

    public void setOnCloseCallback(Runnable callback) { this.onCloseCallback = callback; }
    public void close() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}