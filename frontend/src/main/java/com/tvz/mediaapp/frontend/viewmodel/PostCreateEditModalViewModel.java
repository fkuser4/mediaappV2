package com.tvz.mediaapp.frontend.viewmodel;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.MediaType;
import com.tvz.mediaapp.frontend.model.Platform;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.model.Status;
import com.tvz.mediaapp.frontend.repository.PostApiRepository;
import com.tvz.mediaapp.frontend.service.NotificationService;
import com.tvz.mediaapp.frontend.service.UserPreferencesService;
import com.tvz.mediaapp.frontend.utils.PostMapper;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PostCreateEditModalViewModel {
    private static final Logger logger = LoggerFactory.getLogger(PostCreateEditModalViewModel.class);
    private static final int THUMBNAIL_SIZE = 150;

    public abstract static class MediaItem {
        public final String uniqueId;
        public final String displayName;
        protected MediaItem(String uniqueId, String displayName) {
            this.uniqueId = uniqueId;
            this.displayName = displayName;
        }
    }

    public static class NewMediaItem extends MediaItem {
        public final File file;
        public NewMediaItem(File file) {
            super(UUID.randomUUID().toString(), file.getName());
            this.file = file;
        }
    }

    public static class ExistingMediaItem extends MediaItem {
        public final String serverFilename;
        public ExistingMediaItem(String serverFilename) {
            super(serverFilename, serverFilename);
            this.serverFilename = serverFilename;
        }
    }

    private final PostApiRepository postApiRepository;
    private final NotificationService notificationService;
    private final UserPreferencesService userPreferencesService;

    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty content = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> publishDate = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IN_PROGRESS);
    private final ObservableList<Platform> selectedPlatforms = FXCollections.observableArrayList();
    private final ObjectProperty<MediaType> mediaType = new SimpleObjectProperty<>(MediaType.NONE);
    private final StringProperty linkUrl = new SimpleStringProperty("");
    private final BooleanProperty isSaving = new SimpleBooleanProperty(false);
    private final ObjectProperty<Post> editingPost = new SimpleObjectProperty<>();

    private final ObservableList<MediaItem> imageMediaFiles = FXCollections.observableArrayList();
    private final ObservableList<MediaItem> videoMediaFiles = FXCollections.observableArrayList();

    private final StringProperty titleError = new SimpleStringProperty();
    private final StringProperty contentError = new SimpleStringProperty();
    private final StringProperty dateError = new SimpleStringProperty();
    private final StringProperty platformsError = new SimpleStringProperty();
    private final StringProperty linkUrlError = new SimpleStringProperty();
    private final StringProperty generalError = new SimpleStringProperty();

    private final Map<String, String> uploadedFileNames = new ConcurrentHashMap<>();
    private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
    private final Map<String, ObjectProperty<Image>> thumbnailProperties = new ConcurrentHashMap<>();

    private final Set<String> activeTasks = ConcurrentHashMap.newKeySet();
    private final BooleanProperty hasActiveTasks = new SimpleBooleanProperty(false);

    private Runnable onCloseCallback;
    private Runnable onSaveCallback;

    @Inject
    public PostCreateEditModalViewModel(PostApiRepository postApiRepository, NotificationService notificationService, UserPreferencesService userPreferencesService) {
        this.postApiRepository = postApiRepository;
        this.notificationService = notificationService;
        this.userPreferencesService = userPreferencesService;
    }

    public StringProperty titleProperty() { return title; }
    public StringProperty contentProperty() { return content; }
    public ObjectProperty<LocalDate> publishDateProperty() { return publishDate; }
    public ObservableList<Platform> selectedPlatformsProperty() { return selectedPlatforms; }
    public ObjectProperty<MediaType> mediaTypeProperty() { return mediaType; }
    public StringProperty linkUrlProperty() { return linkUrl; }
    public BooleanProperty isSavingProperty() { return isSaving; }
    public ReadOnlyBooleanProperty hasActiveTasksProperty() { return hasActiveTasks; }
    public ObservableList<MediaItem> getImageMediaFiles() { return imageMediaFiles; }
    public ObservableList<MediaItem> getVideoMediaFiles() { return videoMediaFiles; }
    public ObjectProperty<Post> editingPostProperty() { return editingPost; }

    public StringProperty titleErrorProperty() { return titleError; }
    public StringProperty contentErrorProperty() { return contentError; }
    public StringProperty dateErrorProperty() { return dateError; }
    public StringProperty platformsErrorProperty() { return platformsError; }
    public StringProperty linkUrlErrorProperty() { return linkUrlError; }
    public StringProperty generalErrorProperty() { return generalError; }

    public void setOnCloseCallback(Runnable callback) { this.onCloseCallback = callback; }
    public void setOnSaveCallback(Runnable callback) { this.onSaveCallback = callback; }

    public void togglePlatform(Platform platform) {
        if (selectedPlatforms.contains(platform)) selectedPlatforms.remove(platform);
        else selectedPlatforms.add(platform);
    }

    public void addMediaFiles(List<File> files) {
        clearAllErrors();
        ObservableList<MediaItem> currentList = (mediaType.get() == MediaType.IMAGE) ? imageMediaFiles : videoMediaFiles;
        for (File file : files) {
            boolean isSupported = (mediaType.get() == MediaType.IMAGE) ? isSupportedImageFile(file) : isSupportedVideoFile(file);
            if (isSupported) {
                NewMediaItem mediaItem = new NewMediaItem(file);
                currentList.add(mediaItem);
                generateThumbnailAsync(mediaItem);
            } else {
                generalError.set("Unsupported file type: " + file.getName());
            }
        }
    }

    private boolean isSupportedImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    private boolean isSupportedVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4");
    }

    public void removeMedia(String uniqueId) {
        imageMediaFiles.removeIf(mf -> mf.uniqueId.equals(uniqueId));
        videoMediaFiles.removeIf(mf -> mf.uniqueId.equals(uniqueId));
        uploadedFileNames.remove(uniqueId);
        thumbnailCache.remove(uniqueId);
        thumbnailProperties.remove(uniqueId);
    }

    public ObjectProperty<Image> getThumbnailProperty(String uniqueId) {
        return thumbnailProperties.computeIfAbsent(uniqueId, k -> {
            ObjectProperty<Image> prop = new SimpleObjectProperty<>(thumbnailCache.get(uniqueId));
            logger.debug("Created thumbnail property for: {}", uniqueId);
            return prop;
        });
    }

    private void generateThumbnailAsync(NewMediaItem mediaItem) {
        addTask(mediaItem.uniqueId);
        logger.info("Starting thumbnail generation for: {}", mediaItem.displayName);

        CompletableFuture.runAsync(() -> {
            try {
                BufferedImage bufferedImage = generateThumbnail(mediaItem.file);
                if (bufferedImage != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "jpg", baos);
                    Image thumbnail = new Image(new ByteArrayInputStream(baos.toByteArray()));

                    if (thumbnail.isError()) {
                        logger.error("Error creating JavaFX Image: {}", thumbnail.getException());
                    } else {
                        logger.info("Successfully generated thumbnail for: {} ({}x{})",
                                mediaItem.displayName, thumbnail.getWidth(), thumbnail.getHeight());
                        updateThumbnailCacheAndProperty(mediaItem.uniqueId, thumbnail);
                    }
                } else {
                    logger.warn("Failed to generate BufferedImage for: {}", mediaItem.displayName);
                }
            } catch (Exception e) {
                logger.error("Failed to generate thumbnail for {}", mediaItem.displayName, e);
            } finally {
                removeTask(mediaItem.uniqueId);
            }
        });
    }

    private void loadExistingThumbnailAsync(ExistingMediaItem mediaItem) {
        addTask(mediaItem.uniqueId);
        logger.info("Starting thumbnail load for existing media: {}", mediaItem.serverFilename);

        CompletableFuture.runAsync(() -> {
            try {
                if (editingPost.get() == null) {
                    logger.warn("No editing post available for thumbnail load");
                    return;
                }

                String thumbFilename = getThumbnailFileName(mediaItem.serverFilename);
                logger.debug("Generated thumbnail filename: {}", thumbFilename);

                Map<String, String> urls = postApiRepository.generateDownloadUrls(
                        editingPost.get().getUuid(),
                        List.of(thumbFilename, mediaItem.serverFilename)
                ).get();

                logger.debug("Received URLs from API: {}", urls);

                String thumbnailUrl = urls.get(thumbFilename);
                if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    logger.info("Loading thumbnail from URL: {}", thumbnailUrl);

                    try (InputStream in = new URL(thumbnailUrl).openStream()) {
                        Image thumbnail = new Image(in);

                        if (thumbnail.isError()) {
                            logger.error("Image loading error for thumbnail: {}", thumbFilename);
                            logger.error("Error: {}", thumbnail.getException());
                            loadOriginalAsThumb(mediaItem, urls);
                        } else {
                            logger.info("Successfully loaded thumbnail: {}x{}",
                                    thumbnail.getWidth(), thumbnail.getHeight());
                            updateThumbnailCacheAndProperty(mediaItem.uniqueId, thumbnail);
                        }
                    }
                } else {
                    logger.warn("Thumbnail not found in response: {}", thumbFilename);
                    logger.debug("Available URLs: {}", urls.keySet());
                    loadOriginalAsThumb(mediaItem, urls);
                }
            } catch (Exception e) {
                logger.error("Failed to load existing thumbnail for {}", mediaItem.displayName, e);
            } finally {
                removeTask(mediaItem.uniqueId);
            }
        });
    }

    private void loadOriginalAsThumb(ExistingMediaItem mediaItem, Map<String, String> urls) {
        String originalUrl = urls.get(mediaItem.serverFilename);
        if (originalUrl != null && !originalUrl.isEmpty()) {
            logger.info("Falling back to original file as thumbnail for: {}", mediaItem.serverFilename);
            try (InputStream in = new URL(originalUrl).openStream()) {
                Image thumbnail = new Image(in, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);

                if (thumbnail.isError()) {
                    logger.error("Error loading original file as thumbnail: {}", thumbnail.getException());
                } else {
                    logger.info("Successfully loaded original as thumbnail: {}x{}",
                            thumbnail.getWidth(), thumbnail.getHeight());
                    updateThumbnailCacheAndProperty(mediaItem.uniqueId, thumbnail);
                }
            } catch (Exception e) {
                logger.error("Failed to load original file as thumbnail", e);
            }
        } else {
            logger.error("No original file URL available for: {}", mediaItem.serverFilename);
        }
    }

    private String getThumbnailFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            logger.warn("Original filename is null or empty");
            return null;
        }

        int lastDotIndex = originalFileName.lastIndexOf('.');
        String result;

        if (lastDotIndex == -1) {
            result = originalFileName + "_thumb.jpg";
        } else {
            String nameWithoutExt = originalFileName.substring(0, lastDotIndex);
            result = nameWithoutExt + "_thumb.jpg";
        }

        logger.debug("Thumbnail filename: {} -> {}", originalFileName, result);
        return result;
    }

    private BufferedImage generateThumbnail(File file) throws Exception {
        logger.debug("Generating thumbnail for file: {}", file.getName());

        if (isSupportedImageFile(file)) {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                logger.warn("ImageIO.read returned null for: {}", file.getName());
            } else {
                logger.debug("Loaded image: {}x{}", image.getWidth(), image.getHeight());
            }
            return image;
        } else if (isSupportedVideoFile(file)) {
            logger.debug("Generating video thumbnail for: {}", file.getName());
            try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
                FrameGrab grab = FrameGrab.createFrameGrab(channel);
                grab.seekToFramePrecise(50);
                Picture picture = grab.getNativeFrame();
                BufferedImage result = picture != null ? AWTUtil.toBufferedImage(picture) : null;

                if (result == null) {
                    logger.warn("Failed to extract video frame for: {}", file.getName());
                } else {
                    logger.debug("Extracted video frame: {}x{}", result.getWidth(), result.getHeight());
                }

                return result;
            }
        }

        logger.warn("Unsupported file type for thumbnail generation: {}", file.getName());
        return null;
    }

    private void updateThumbnailCacheAndProperty(String uniqueId, Image thumbnail) {
        if (thumbnail == null) {
            logger.warn("Attempting to update with null thumbnail for: {}", uniqueId);
            return;
        }

        javafx.application.Platform.runLater(() -> {
            try {
                thumbnailCache.put(uniqueId, thumbnail);
                ObjectProperty<Image> property = thumbnailProperties.computeIfAbsent(
                        uniqueId,
                        k -> new SimpleObjectProperty<>()
                );
                property.set(thumbnail);

                logger.info("Successfully updated thumbnail property for: {}", uniqueId);

                if (property.get() != null) {
                    logger.debug("Property verification passed for: {}", uniqueId);
                } else {
                    logger.error("Property verification failed for: {}", uniqueId);
                }

            } catch (Exception e) {
                logger.error("Error updating thumbnail property for: {}", uniqueId, e);
            }
        });
    }

    private CompletableFuture<Void> uploadFileWithThumbnail(NewMediaItem mediaItem) {
        addTask("upload-" + mediaItem.uniqueId);
        return CompletableFuture.runAsync(() -> {
            try {
                String extension = getFileExtension(mediaItem.file.getName());
                String baseName = UUID.randomUUID().toString();
                String uploadedFileName = baseName + extension;
                String thumbnailFileName = baseName + "_thumb.jpg";

                uploadFile(mediaItem.file, uploadedFileName).join();

                BufferedImage thumbnailImage = generateThumbnail(mediaItem.file);
                if (thumbnailImage != null) {
                    Path tempThumbPath = Files.createTempFile("thumb_", ".jpg");
                    ImageIO.write(thumbnailImage, "jpg", tempThumbPath.toFile());
                    uploadFile(tempThumbPath.toFile(), thumbnailFileName).join();
                    Files.delete(tempThumbPath);
                }
                uploadedFileNames.put(mediaItem.uniqueId, uploadedFileName);
            } catch (Exception e) {
                throw new RuntimeException("Upload failed for " + mediaItem.displayName, e);
            }
        }).whenComplete((res, ex) -> removeTask("upload-" + mediaItem.uniqueId));
    }

    private CompletableFuture<Void> uploadFile(File file, String fileName) {
        return postApiRepository.generateUploadUrl(fileName)
                .thenCompose(response -> {
                    String uploadUrl = response.get("uploadUrl");
                    if (uploadUrl == null) throw new RuntimeException("Received null upload URL for " + fileName);
                    String contentType = URLConnection.guessContentTypeFromName(file.getName());
                    return postApiRepository.uploadFile(uploadUrl, file.toPath(), contentType);
                });
    }

    private String getFileExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? "" : name.substring(dotIndex);
    }

    private void addTask(String taskId) {
        activeTasks.add(taskId);
        updateActiveTaskStatus();
        logger.debug("Added task: {} (total: {})", taskId, activeTasks.size());
    }

    private void removeTask(String taskId) {
        activeTasks.remove(taskId);
        updateActiveTaskStatus();
        logger.debug("Removed task: {} (total: {})", taskId, activeTasks.size());
    }

    private void updateActiveTaskStatus() {
        javafx.application.Platform.runLater(() -> hasActiveTasks.set(!activeTasks.isEmpty()));
    }

    public void loadPost(Post post, Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
        if (post != null) initializeForEdit(post);
        else initializeForCreate();
    }

    private void initializeForCreate() {
        editingPost.set(null);
        resetFields();
    }

    public void initializeForEdit(Post post) {
        logger.info("Initializing edit mode for post: {}", post.getTitle());
        resetFields();
        editingPost.set(post);
        title.set(post.getTitle());
        content.set(post.getContent());
        publishDate.set(post.getDate());
        status.set(post.getStatus());
        selectedPlatforms.setAll(post.getPlatforms());
        mediaType.set(post.getMediaType());

        if (post.getMediaType() == MediaType.LINK && post.getMediaUris() != null && !post.getMediaUris().isEmpty()) {
            linkUrl.set(post.getMediaUris().get(0));
        } else if (post.getMediaUris() != null && !post.getMediaUris().isEmpty()) {
            ObservableList<MediaItem> currentList = (post.getMediaType() == MediaType.IMAGE) ? imageMediaFiles : videoMediaFiles;

            logger.info("Loading {} media items for editing", post.getMediaUris().size());
            for (String serverFilename : post.getMediaUris()) {
                ExistingMediaItem item = new ExistingMediaItem(serverFilename);
                currentList.add(item);
                logger.debug("Added existing media item: {}", serverFilename);
                loadExistingThumbnailAsync(item);
            }
        }
    }

    public void resetFields() {
        title.set("");
        content.set("");
        publishDate.set(null);
        status.set(Status.IN_PROGRESS);
        mediaType.set(MediaType.NONE);
        selectedPlatforms.setAll(userPreferencesService.getSelectedPlatforms());
        clearAllMediaInputs();
        clearAllErrors();
        activeTasks.clear();
        updateActiveTaskStatus();
    }

    private void clearAllMediaInputs() {
        linkUrl.set("");
        imageMediaFiles.clear();
        videoMediaFiles.clear();
        thumbnailCache.clear();
        thumbnailProperties.clear();
    }

    private void clearAllErrors() {
        titleError.set(null);
        contentError.set(null);
        dateError.set(null);
        platformsError.set(null);
        linkUrlError.set(null);
        generalError.set(null);
    }

    private boolean validate() {
        clearAllErrors();
        boolean isValid = true;
        if (title.get() == null || title.get().trim().isEmpty()) {
            titleError.set("Title is required.");
            isValid = false;
        }
        if (content.get() == null || content.get().trim().isEmpty()) {
            contentError.set("Content is required.");
            isValid = false;
        }
        if (publishDate.get() == null) {
            dateError.set("A valid publish date is required.");
            isValid = false;
        }
        if (selectedPlatforms.isEmpty()) {
            platformsError.set("At least one platform must be selected.");
            isValid = false;
        }
        if (mediaType.get() == MediaType.LINK && (linkUrl.get() == null || linkUrl.get().trim().isEmpty())) {
            linkUrlError.set("Link URL is required for the Link media type.");
            isValid = false;
        }
        if (mediaType.get() == MediaType.IMAGE && imageMediaFiles.isEmpty()) {
            platformsError.set("At least one image must be added.");
            isValid = false;
        }
        if (mediaType.get() == MediaType.VIDEO && videoMediaFiles.isEmpty()) {
            platformsError.set("At least one video must be added.");
            isValid = false;
        }
        return isValid;
    }

    public void save() {
        if (isSaving.get() || hasActiveTasks.get() || !validate()) return;
        isSaving.set(true);

        ObservableList<MediaItem> filesToUpload = (mediaType.get() == MediaType.IMAGE) ? imageMediaFiles : videoMediaFiles;
        List<CompletableFuture<Void>> uploadFutures = filesToUpload.stream()
                .filter(item -> item instanceof NewMediaItem)
                .map(item -> (NewMediaItem) item)
                .map(this::uploadFileWithThumbnail)
                .collect(Collectors.toList());

        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            Post post = createPostFromFields();
            if (editingPost.get() != null) updatePost(post);
            else createPost(post);
        }).exceptionally(this::handleSaveFailure);
    }

    private Post createPostFromFields() {
        Post post = (editingPost.get() != null) ? editingPost.get() : new Post();
        post.setUuid(editingPost.get() != null ? editingPost.get().getUuid() : null);
        post.setTitle(title.get());
        post.setContent(content.get());
        post.setDate(publishDate.get());
        post.setStatus(status.get());
        post.setPlatforms(new ArrayList<>(selectedPlatforms));
        post.setMediaType(mediaType.get());

        List<String> uris = new ArrayList<>();
        if (mediaType.get() == MediaType.LINK) {
            uris.add(linkUrl.get());
        } else {
            ObservableList<MediaItem> activeMedia = (mediaType.get() == MediaType.IMAGE) ? imageMediaFiles : videoMediaFiles;
            for (MediaItem item : activeMedia) {
                if (item instanceof NewMediaItem) {
                    uris.add(uploadedFileNames.get(item.uniqueId));
                } else if (item instanceof ExistingMediaItem) {
                    uris.add(((ExistingMediaItem) item).serverFilename);
                }
            }
        }
        post.setMediaUris(uris.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return post;
    }

    private void createPost(Post post) {
        postApiRepository.createPost(PostMapper.toRequestDto(post))
                .thenAccept(this::handleSaveSuccess)
                .exceptionally(this::handleSaveFailure);
    }

    private void updatePost(Post post) {
        postApiRepository.updatePost(post.getUuid(), PostMapper.toRequestDto(post))
                .thenAccept(this::handleSaveSuccess)
                .exceptionally(this::handleSaveFailure);
    }

    private Void handleSaveSuccess(Object result) {
        javafx.application.Platform.runLater(() -> {
            isSaving.set(false);
            notificationService.showSuccess("Post saved successfully!");
            if (onSaveCallback != null) onSaveCallback.run();
            close();
        });
        return null;
    }

    private Void handleSaveFailure(Throwable ex) {
        javafx.application.Platform.runLater(() -> {
            isSaving.set(false);
            generalError.set("Failed to save post: " + ex.getCause().getMessage());
            notificationService.showError("Failed to save post.");
        });
        logger.error("Failed to save post", ex);
        return null;
    }

    public void close() {
        if (onCloseCallback != null) onCloseCallback.run();
    }
}