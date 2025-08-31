package com.tvz.mediaapp.frontend.viewmodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.repository.PostApiRepository;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PreviewViewModel {
    private static final Logger logger = LoggerFactory.getLogger(PreviewViewModel.class);

    private final PostApiRepository postApiRepository;
    private final PostsViewModel postsViewModel;
    private final ObjectMapper objectMapper;

    private final FilteredList<Post> filteredPostList;
    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObjectProperty<Post> selectedPost = new SimpleObjectProperty<>();

    private final ObjectProperty<Post> postToRefresh = new SimpleObjectProperty<>();

    @Inject
    public PreviewViewModel(PostApiRepository postApiRepository, PostsViewModel postsViewModel, ObjectMapper objectMapper) {
        this.postApiRepository = postApiRepository;
        this.postsViewModel = postsViewModel;
        this.objectMapper = objectMapper;
        this.filteredPostList = new FilteredList<>(this.postsViewModel.getPostList());
        this.searchText.addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        this.postsViewModel.getPostList().addListener((ListChangeListener<Post>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Post removedPost : c.getRemoved()) {
                        if (removedPost.equals(selectedPost.get())) {
                            selectedPost.set(null);
                        }
                    }
                } else if (c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); ++i) {
                        Post updatedPost = c.getList().get(i);
                        if (updatedPost.equals(selectedPost.get())) {
                            logger.info("Selected post was updated. Signaling view to refresh.");
                            postToRefresh.set(updatedPost);
                        }
                    }
                }
            }
        });
    }

    public void initialize() {
        postsViewModel.refreshPosts();
        searchText.set("");
        selectedPost.set(null);
    }

    private void applyFilter(String filterText) {
        Post currentSelection = selectedPost.get();

        filteredPostList.setPredicate(post -> {
            if (post.equals(currentSelection)) {
                return true;
            }
            if (filterText == null || filterText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = filterText.toLowerCase();
            return post.getTitle().toLowerCase().contains(lowerCaseFilter);
        });
    }


    public CompletableFuture<String> preparePreviewDataAsJson() {
        Post post = selectedPost.get();
        if (post == null) {
            return CompletableFuture.completedFuture("{}");
        }

        Map<String, Object> previewData = new HashMap<>();
        previewData.put("content", post.getContent());
        previewData.put("mediaType", post.getMediaType().name());
        previewData.put("platforms", post.getPlatforms());

        previewData.put("user", Map.of(
                "username", "johndoe_official",
                "displayName", "John Doe",
                "avatarUrl", "https://pbs.twimg.com/profile_images/1683325380441128960/yRsRRjGO_400x400.jpg",
                "verified", true
        ));
        previewData.put("engagement", Map.of(
                "likes", "1,234",
                "comments", "56",
                "shares", "12",
                "views", "10,000"
        ));

        if (post.getMediaUris() != null && !post.getMediaUris().isEmpty()) {
            return postApiRepository.generateDownloadUrls(post.getUuid(), post.getMediaUris())
                    .thenApply(urlMap -> {
                        previewData.put("mediaUris", urlMap.values());
                        try {
                            return objectMapper.writeValueAsString(previewData);
                        } catch (Exception e) {
                            logger.error("Failed to serialize post data to JSON", e);
                            return "{}";
                        }
                    });
        } else {
            previewData.put("mediaUris", Collections.emptyList());
            try {
                return CompletableFuture.completedFuture(objectMapper.writeValueAsString(previewData));
            } catch (Exception e) {
                logger.error("Failed to serialize post data to JSON", e);
                return CompletableFuture.completedFuture("{}");
            }
        }
    }

    public FilteredList<Post> getFilteredPostList() { return filteredPostList; }
    public StringProperty searchTextProperty() { return searchText; }
    public ObjectProperty<Post> selectedPostProperty() { return selectedPost; }
    public ObjectProperty<Post> postToRefreshProperty() { return postToRefresh; }
}