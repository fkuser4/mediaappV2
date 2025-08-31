package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.viewmodel.PreviewViewModel;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PreviewView implements View {
    private static final Logger logger = LoggerFactory.getLogger(PreviewView.class);

    private final Parent root;
    private final PreviewViewModel viewModel;

    private final CompletableFuture<Boolean> webViewReadyFuture = new CompletableFuture<>();

    @FXML private TextField searchField;
    @FXML private ListView<Post> postListView;
    @FXML private WebView webView;
    private WebEngine webEngine;
    private boolean isPageReady = false;

    @Inject
    public PreviewView(PreviewViewModel viewModel) {
        this.viewModel = viewModel;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/preview-view.fxml"));
            loader.setController(this);
            root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/preview-view.css")).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for PreviewView", e);
        }
    }

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        setupPostPicker();
        setupWebView();
    }

    public CompletableFuture<Boolean> getWebViewReadyFuture() {
        return webViewReadyFuture;
    }

    public void prepareForDisplay() {
        viewModel.initialize();
        if (isPageReady) {
            Platform.runLater(() -> webEngine.executeScript("showInitialState();"));
        }
    }

    private void setupPostPicker() {
        postListView.setItems(viewModel.getFilteredPostList());
        postListView.setCellFactory(lv -> new ListCell<>() {
            private final VBox container = new VBox();
            private final Label titleLabel = new Label();
            {
                container.getStyleClass().add("list-item-container");
                titleLabel.getStyleClass().add("list-item-label");
                container.getChildren().add(titleLabel);
            }
            @Override
            protected void updateItem(Post item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    titleLabel.setText(item.getTitle()); setGraphic(container); setText(null);
                }
            }
        });
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());
        postListView.getSelectionModel().selectedItemProperty().addListener((obs, oldPost, newPost) -> viewModel.selectedPostProperty().set(newPost));
    }

    private void setupWebView() {
        webEngine.setOnError(event -> logger.error("WebView error: {}", event.getMessage()));

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                logger.info("WebView page loaded successfully.");
                isPageReady = true;
                webViewReadyFuture.complete(true);

                if (viewModel.selectedPostProperty().get() != null) {
                    injectData();
                } else {
                    Platform.runLater(() -> webEngine.executeScript("showInitialState();"));
                }
            } else if (newState == Worker.State.FAILED) {
                isPageReady = false;
                webViewReadyFuture.complete(false);
                logger.error("WebView failed to load content.", webEngine.getLoadWorker().getException());
            }
        });

        viewModel.selectedPostProperty().addListener((obs, oldPost, newPost) -> {
            if (isPageReady) {
                if (newPost != null) {
                    loadPostIntoWebView();
                } else {
                    logger.info("No post selected, showing initial state.");
                    Platform.runLater(() -> webEngine.executeScript("showInitialState();"));
                }
            }
        });

        viewModel.postToRefreshProperty().addListener((obs, oldVal, updatedPost) -> {
            if (updatedPost != null && isPageReady) {
                logger.info("Refresh signal received. Injecting updated data.");
                injectData();
            }
        });

        loadWebViewContent();
    }

    private void loadWebViewContent() {
        try {
            URL htmlUrl = getClass().getResource("/webview/index.html");
            if (htmlUrl != null) {
                webEngine.load(htmlUrl.toExternalForm());
            } else {
                throw new IOException("Resource '/webview/index.html' not found.");
            }
        } catch (Exception e) {
            logger.error("Failed to load WebView content", e);
            webEngine.loadContent("<html><body><h1>Error loading content</h1></body></html>");
        }
    }

    private void loadPostIntoWebView() {
        if (isPageReady) {
            injectData();
        } else {
            logger.info("Post selected, but WebView not ready. Data will be injected on page load.");
        }
    }

    private void injectData() {
        viewModel.preparePreviewDataAsJson().thenAccept(jsonPayload -> Platform.runLater(() -> {
            try {
                String script = String.format("loadPreviewFromJava(%s);", jsonPayload);
                webEngine.executeScript(script);
            } catch (JSException e) {
                logger.error("JavaScript error during injection: {}", e.getMessage(), e);
            }
        }));
    }

    @Override
    public Parent getView() {
        return root;
    }
}