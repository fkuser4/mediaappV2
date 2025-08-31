package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.model.Platform;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.model.Status;
import com.tvz.mediaapp.frontend.utils.ButtonStyler;
import com.tvz.mediaapp.frontend.viewmodel.PostsViewModel;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class PostsView implements View {
    @FXML private FlowPane activeFiltersPane;
    @FXML private Button addButton;
    @FXML private Button historyButton;
    @FXML private TextField searchField;
    @FXML private TableView<Post> postsTableView;
    @FXML private TableColumn<Post, String> titleColumn;
    @FXML private TableColumn<Post, LocalDate> dateColumn;
    @FXML private TableColumn<Post, List<Platform>> platformColumn;
    @FXML private TableColumn<Post, Status> statusColumn;
    @FXML private TableColumn<Post, Post> actionsColumn;

    private final PostsViewModel viewModel;
    private Parent root;
    private Button filterButton = new Button();
    private FilteredList<Post> filteredData;
    private final List<Predicate<Post>> activeFilters = new ArrayList<>();
    private final List<Platform> platformList = new ArrayList<>(List.of(Platform.values()));
    private final List<Status> statusList = new ArrayList<>(List.of(Status.values()));
    public int numberOfFilters = 0;

    @Inject
    public PostsView(PostsViewModel viewModel) {
        this.viewModel = viewModel;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/posts-view.fxml"));
            loader.setController(this);
            root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/posts-view.css")).toExternalForm());
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/button-styler.css")).toExternalForm());
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/notification.css")).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for PostsView", e);
        }
    }

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(viewModel.getPostList(), p -> true);
        SortedList<Post> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(postsTableView.comparatorProperty());
        postsTableView.setItems(sortedData);

        setupFilterButton();
        setupActionButtons();
        setupSearchField();
        setupTable();

        viewModel.initialize();
    }

    private void setupSearchField() {
        searchField.setPromptText("Search posts...");
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());
        viewModel.searchTextProperty().addListener((obs, oldV, newV) -> applyFiltersAndSearch());
    }

    private void setupTable() {
        postsTableView.setSelectionModel(null);
        postsTableView.setPlaceholder(new Label("No posts available."));

        postsTableView.setRowFactory(tv -> {
            TableRow<Post> row = new TableRow<>();
            row.setMinHeight(50);
            row.setMaxHeight(50);
            row.setPrefHeight(50);
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    Post clickedPost = row.getItem();
                    viewModel.viewPost(clickedPost);
                }
            });
            return row;
        });

        titleColumn.prefWidthProperty().bind(postsTableView.widthProperty().multiply(0.38));
        platformColumn.prefWidthProperty().bind(postsTableView.widthProperty().multiply(0.20));
        statusColumn.prefWidthProperty().bind(postsTableView.widthProperty().multiply(0.15));
        dateColumn.prefWidthProperty().bind(postsTableView.widthProperty().multiply(0.15));
        actionsColumn.prefWidthProperty().bind(postsTableView.widthProperty().multiply(0.12));

        titleColumn.setReorderable(false);
        platformColumn.setReorderable(false);
        statusColumn.setReorderable(false);
        dateColumn.setReorderable(false);
        actionsColumn.setReorderable(false);

        titleColumn.setSortable(false);
        platformColumn.setSortable(false);
        statusColumn.setSortable(false);
        actionsColumn.setSortable(false);
        dateColumn.setSortable(true);

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        platformColumn.setCellValueFactory(new PropertyValueFactory<>("platforms"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        dateColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormatter.format(item));
            }
        });

        platformColumn.setCellFactory(tc -> new TableCell<>() {
            private final HBox platformIconBox = new HBox(5) {{ setAlignment(Pos.CENTER_LEFT); }};
            @Override
            protected void updateItem(List<Platform> platforms, boolean empty) {
                super.updateItem(platforms, empty);
                if (empty || platforms == null || platforms.isEmpty()) {
                    setGraphic(null);
                } else {
                    platformIconBox.getChildren().clear();
                    platforms.forEach(p -> platformIconBox.getChildren().add(createPlatformIcon(p)));
                    setGraphic(platformIconBox);
                }
            }
        });

        statusColumn.setCellFactory(tc -> new TableCell<>() {
            private final Label statusLabel = new Label();
            @Override
            protected void updateItem(Status item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    statusLabel.setText(item.getDisplayName());
                    statusLabel.getStyleClass().setAll("status-label", item.getStyleClass());
                    setGraphic(statusLabel);
                }
            }
        });

        actionsColumn.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox pane = new HBox(8, editBtn, deleteBtn);
            {
                pane.setAlignment(Pos.CENTER_RIGHT);
                pane.setPadding(new Insets(0, 25, 0, 0));

                ButtonStyler.with(editBtn).svgPath("src/main/resources/svg/edit.svg").iconSize(16)
                        .normalColors("#a1a1aa", "transparent").hoverColors("#e4e4e7", "transparent").apply();
                ButtonStyler.with(deleteBtn).svgPath("src/main/resources/svg/delete.svg").iconSize(16)
                        .normalColors("#f87171", "transparent").hoverColors("#ef4444", "transparent").apply();
                editBtn.getStyleClass().add("editTask-button");
                deleteBtn.getStyleClass().add("deleteTask-button");

                pane.setOnMouseClicked(Event::consume);

                editBtn.setOnAction(event -> {
                    Post post = getTableRow().getItem();
                    if (post != null) {
                        viewModel.editPost(post);
                    }
                });

                deleteBtn.setOnAction(event -> {
                    Post post = getTableRow().getItem();
                    if (post != null) {
                        viewModel.deletePost(post);
                    }
                });
            }
            @Override
            protected void updateItem(Post item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void applyFiltersAndSearch() {
        Predicate<Post> combinedPredicate = post -> {
            String searchText = viewModel.searchTextProperty().get();
            boolean searchMatch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                searchMatch = post.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                        (post.getContent() != null && post.getContent().toLowerCase().contains(lowerCaseFilter));
            }
            if (!searchMatch) return false;
            return activeFilters.stream().allMatch(predicate -> predicate.test(post));
        };
        filteredData.setPredicate(combinedPredicate);
    }

    private Node createPlatformIcon(Platform platform) {
        SVGPath svg = new SVGPath();
        String path = com.tvz.mediaapp.frontend.utils.SVGPathExtractor.extractSVGPath("src/main/resources/icons/" + platform.getIconName());
        svg.setContent(path);
        svg.getStyleClass().add("platform-icon");
        return svg;
    }

    private void createFinalChip(HBox container, String category, Object value) {
        Predicate<Post> predicate = post -> {
            if (value instanceof Platform) return post.getPlatforms().contains(value);
            if (value instanceof Status) return post.getStatus() == value;
            return false;
        };
        activeFilters.add(predicate);
        applyFiltersAndSearch();
        container.getChildren().clear();
        container.setSpacing(2);
        HBox categoryChip = new HBox(new Label(category));
        categoryChip.getStyleClass().addAll("builder-chip", "builder-chip-left");
        categoryChip.setAlignment(Pos.CENTER);
        String valueText = (value instanceof Status) ? ((Status) value).getDisplayName() : value.toString();
        Label valueLabel = new Label(valueText);
        Button closeButton = new Button();
        ButtonStyler.with(closeButton).svgPath("src/main/resources/svg/close.svg").iconSize(6)
                .normalColors("#a09ec0", "transparent").hoverColors("white", "rgba(255, 255, 255, 0.1)").apply();
        closeButton.getStyleClass().add("filter-chip-close-button-inside");
        HBox valueChip = new HBox(6, valueLabel, closeButton);
        valueChip.getStyleClass().addAll("builder-chip", "builder-chip-right");
        valueChip.setAlignment(Pos.CENTER);
        container.getChildren().addAll(categoryChip, valueChip);
        closeButton.setOnAction(e -> {
            activeFiltersPane.getChildren().remove(container);
            activeFilters.remove(predicate);
            applyFiltersAndSearch();
            numberOfFilters--;
            if (value instanceof Platform) platformList.add((Platform) value);
            if (value instanceof Status) statusList.add((Status) value);
            updateFilterButtonState();
        });
    }

    private void setupFilterButton() {
        activeFiltersPane.setHgap(9); activeFiltersPane.setVgap(9);
        Label plusIcon = new Label("+"); plusIcon.getStyleClass().add("plus-symbol");
        Label filterTextLabel = new Label("Add a filter"); filterTextLabel.getStyleClass().add("filter-button-text-label");
        HBox content = new HBox(4, plusIcon, filterTextLabel); content.setAlignment(Pos.CENTER);
        filterButton.setGraphic(content); filterButton.getStyleClass().add("add-filter-button");
        filterButton.setOnAction(event -> {
            if (numberOfFilters < 12 && hasAvailableFilters()) {
                filterFactory();
                numberOfFilters++;
                updateFilterButtonState();
            }
        });
        updateFilterButtonState(); activeFiltersPane.getChildren().add(0, filterButton);
    }

    private void filterFactory() {
        HBox hbox = new HBox(0); hbox.setAlignment(Pos.CENTER);
        ComboBox<String> comboBox1 = new ComboBox<>(); comboBox1.getStyleClass().addAll("filter-combo-box", "filter-combo-box-initial");
        if (!platformList.isEmpty()) comboBox1.getItems().add("Platform");
        if (!statusList.isEmpty()) comboBox1.getItems().add("Status");
        comboBox1.setOnAction(e -> {
            String selectedItem = comboBox1.getSelectionModel().getSelectedItem();
            HBox categoryChip = new HBox(new Label(selectedItem)); categoryChip.getStyleClass().addAll("builder-chip", "builder-chip-left"); categoryChip.setAlignment(Pos.CENTER);
            ComboBox<Object> comboBox2 = new ComboBox<>(); comboBox2.getStyleClass().add("filter-combo-box");
            if ("Platform".equals(selectedItem)) comboBox2.getItems().addAll(platformList);
            if ("Status".equals(selectedItem)) comboBox2.getItems().addAll(statusList);
            comboBox2.setConverter(new StringConverter<>() {
                @Override public String toString(Object object) { if (object instanceof Status) return ((Status) object).getDisplayName(); return object != null ? object.toString() : ""; }
                @Override public Object fromString(String string) { return null; }
            });
            hbox.getChildren().clear(); hbox.getChildren().addAll(categoryChip, comboBox2);
            comboBox2.setOnAction(ev -> {
                Object selectedItem2 = comboBox2.getSelectionModel().getSelectedItem();
                createFinalChip(hbox, selectedItem, selectedItem2);
                if (selectedItem2 instanceof Platform) platformList.remove((Platform) selectedItem2);
                if (selectedItem2 instanceof Status) statusList.remove((Status) selectedItem2);
                updateFilterButtonState();
            });
            javafx.application.Platform.runLater(comboBox2::show);
        });
        hbox.getChildren().add(comboBox1); activeFiltersPane.getChildren().add(hbox); javafx.application.Platform.runLater(comboBox1::show);
    }

    private boolean hasAvailableFilters() { return !platformList.isEmpty() || !statusList.isEmpty(); }

    private void updateFilterButtonState() { filterButton.setDisable(!hasAvailableFilters() || numberOfFilters >= 12); }

    private void setupActionButtons() {
        ButtonStyler.with(addButton).svgPath("src/main/resources/svg/plus.svg").iconSize(16).normalColors("#605e81", "#323144").hoverColors("#dcd8d8", "#353554").apply();
        addButton.getStyleClass().add("right-action-button");
        ButtonStyler.with(historyButton).svgPath("src/main/resources/svg/history.svg").iconSize(16).normalColors("#605e81", "#323144").hoverColors("#dcd8d8", "#353554").apply();
        historyButton.getStyleClass().add("right-action-button");
    }

    @FXML private void handleAddNew() { viewModel.createNewPost(); }
    @FXML private void handleHistory() {  }
    @Override public Parent getView() { return root; }
}