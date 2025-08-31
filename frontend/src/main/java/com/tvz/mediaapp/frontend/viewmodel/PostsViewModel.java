package com.tvz.mediaapp.frontend.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.repository.PostApiRepository;
import com.tvz.mediaapp.frontend.service.NavigationManager;
import com.tvz.mediaapp.frontend.service.NotificationService;
import com.tvz.mediaapp.frontend.utils.PostMapper;
import com.tvz.mediaapp.frontend.view.DeleteConfirmationModalView;
import com.tvz.mediaapp.frontend.view.PostCreateEditModalView;
import com.tvz.mediaapp.frontend.view.PostViewModalView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Singleton
public class PostsViewModel {
    private static final Logger logger = LoggerFactory.getLogger(PostsViewModel.class);

    private final ObservableList<Post> masterPostList = FXCollections.observableArrayList();
    private ScheduledExecutorService pollingExecutor;
    private final StringProperty searchText = new SimpleStringProperty("");
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    @Inject private PostApiRepository postApiRepository;
    @Inject private NavigationManager navigationManager;
    @Inject private NotificationService notificationService;
    @Inject private Injector injector;

    public void initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            logger.info("Initializing PostsViewModel for the first time in this session.");
            fetchPosts();
            startPolling();
        } else {
            logger.warn("PostsViewModel already initialized. Skipping poller setup, just refreshing data.");
            fetchPosts();
        }
    }

    public void reset() {
        logger.info("Resetting PostsViewModel state for logout.");
        stopPolling();
        Platform.runLater(masterPostList::clear);
        isInitialized.set(false);
    }

    public void createNewPost() {
        PostCreateEditModalViewModel viewModel = injector.getInstance(PostCreateEditModalViewModel.class);
        viewModel.setOnCloseCallback(navigationManager::hideModal);
        viewModel.loadPost(null, this::refreshPosts);

        PostCreateEditModalView view = injector.getInstance(PostCreateEditModalView.class);
        view.setViewModel(viewModel);

        view.prepareForDisplay();
        navigationManager.showModal(view.getView());
    }

    public void editPost(Post post) {
        if (post == null) return;

        PostCreateEditModalViewModel viewModel = injector.getInstance(PostCreateEditModalViewModel.class);
        viewModel.setOnCloseCallback(navigationManager::hideModal);
        viewModel.loadPost(new Post(post), this::refreshPosts);

        PostCreateEditModalView view = injector.getInstance(PostCreateEditModalView.class);
        view.setViewModel(viewModel);

        view.prepareForDisplay();
        navigationManager.showModal(view.getView());
    }

    public void viewPost(Post post) {
        if (post == null) return;

        PostViewModalViewModel viewModel = injector.getInstance(PostViewModalViewModel.class);
        viewModel.setOnCloseCallback(navigationManager::hideModal);
        viewModel.loadPost(post, this::refreshPosts);

        PostViewModalView view = injector.getInstance(PostViewModalView.class);
        view.setViewModel(viewModel);

        navigationManager.showModal(view.getView());
    }

    public void deletePost(Post post) {
        if (post == null) return;
        DeleteConfirmationModalView deleteModal = injector.getInstance(DeleteConfirmationModalView.class);
        deleteModal.showDeleteConfirmation(post, this::performDeletePost);
    }

    public void stopPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            logger.info("Stopping API polling service.");
            pollingExecutor.shutdownNow();
            pollingExecutor = null;
        }
    }

    public void fetchPosts() {
        postApiRepository.getPosts().thenAccept(postDtos -> {
            List<Post> serverPosts = postDtos.stream().map(PostMapper::fromDto).collect(Collectors.toList());
            Platform.runLater(() -> updateObservableList(serverPosts));

        }).exceptionally(ex -> {
            logger.error("Failed to fetch posts", ex);
            Platform.runLater(() -> {
                if (ex.getCause() instanceof ConnectException) {
                    notificationService.showError("Cannot connect to server. Please check your connection.");
                } else {
                    notificationService.showError("Failed to fetch posts. Please try again.");
                }
            });
            return null;
        });
    }

    public void refreshPosts() {
        fetchPosts();
    }

    private void updateObservableList(List<Post> serverPosts) {
        masterPostList.removeIf(localPost ->
                serverPosts.stream().noneMatch(serverPost -> serverPost.getUuid().equals(localPost.getUuid()))
        );

        for (Post serverPost : serverPosts) {
            Optional<Post> localOpt = masterPostList.stream()
                    .filter(p -> p.getUuid().equals(serverPost.getUuid()))
                    .findFirst();

            if (localOpt.isPresent()) {
                Post localPost = localOpt.get();
                if (serverPost.getUpdatedAt().isAfter(localPost.getUpdatedAt())) {
                    int index = masterPostList.indexOf(localPost);
                    masterPostList.set(index, serverPost);
                }
            } else {
                masterPostList.add(0, serverPost);
            }
        }
    }

    private void startPolling() {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ApiPollingThread");
            t.setDaemon(true);
            return t;
        });
        pollingExecutor.scheduleAtFixedRate(this::fetchPosts, 30, 30, TimeUnit.SECONDS);
        logger.info("Started API polling every 30 seconds.");
    }

    private CompletableFuture<Void> performDeletePost(Post post) {
        return postApiRepository.deletePost(post.getUuid())
                .orTimeout(8, TimeUnit.SECONDS)
                .thenRun(() -> Platform.runLater(() -> {
                    masterPostList.remove(post);
                    notificationService.showSuccess("Post deleted successfully!");
                    logger.info("Post deleted: {}", post.getTitle());
                })).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (ex.getCause() instanceof TimeoutException) {
                            notificationService.showError("Delete request timed out. Please check your connection.");
                        } else if (ex.getCause() instanceof ConnectException) {
                            notificationService.showError("Cannot connect to server. Please check your connection.");
                        } else {
                            notificationService.showError("Failed to delete post. Please try again.");
                        }
                    });
                    logger.error("Failed to delete post with UUID: {}", post.getUuid(), ex);
                    return null;
                });
    }

    public ObservableList<Post> getPostList() {
        return masterPostList;
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }
}