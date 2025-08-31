package com.tvz.mediaapp.frontend.view;

import com.google.inject.Inject;
import com.tvz.mediaapp.frontend.components.CustomLineChart;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.utils.ButtonStyler;
import com.tvz.mediaapp.frontend.viewmodel.HomeViewModel;
import javafx.animation.*;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeView implements View {
    private static final Logger logger = LoggerFactory.getLogger(HomeView.class);

    @FXML private HBox statusBoxContainer;
    @FXML private HBox periodButtonContainer;
    @FXML private StackPane chartContainer;
    @FXML private GridPane calendarGrid;
    @FXML private Label calendarMonthYear;
    @FXML private Button calendarPrevBtn;
    @FXML private Button calendarNextBtn;
    @FXML private VBox todayPostsContainer;
    @FXML private VBox upcomingEventsContainer;
    @FXML private Label todayDateLabel;
    @FXML private ListView<Post> todayPostsList;
    @FXML private ListView<HomeViewModel.HolidayEvent> upcomingEventsList;

    private final HomeViewModel viewModel;
    private Parent root;

    private ToggleButton weekBtn;
    private ToggleButton monthBtn;
    private ToggleButton yearBtn;

    private CustomLineChart lineChart;
    private YearMonth currentMonth;
    private LocalDate selectedDate;


    @Inject
    public HomeView(HomeViewModel viewModel) {
        this.viewModel = viewModel;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home-view.fxml"));
            loader.setController(this);
            root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/home-view.css")).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for HomeView", e);
        }
    }

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        setupStatusBoxes();
        setupLineChart();
        setupPeriodButtons();
        setupCalendar();
        setupTodayPosts();
        setupUpcomingEvents();

        viewModel.initialize();

        viewModel.selectedPeriodProperty().addListener((obs, old, period) -> {
            lineChart.setPeriod(period);
            updateChartData();
        });

        viewModel.allPostsProperty().addListener((ListChangeListener<Post>) change -> {
            logger.info("Posts updated in view, refreshing chart");
            updateChartData();
            updateCalendar();
        });
    }

    private void setupStatusBoxes() {
        StatusDisplayBox doneBox = new StatusDisplayBox(
                "Done",
                "check.svg",
                "#1ED760",
                "#1A5A3A"
        );

        StatusDisplayBox inProgressBox = new StatusDisplayBox(
                "In Progress",
                "hourglass.svg",
                "#FBC02D",
                "#5A4A1A"
        );

        StatusDisplayBox canceledBox = new StatusDisplayBox(
                "Canceled",
                "x.svg",
                "#FF6B6B",
                "#5A1A1A"
        );

        statusBoxContainer.getChildren().addAll(doneBox, inProgressBox, canceledBox);

        for (Node statusBox : statusBoxContainer.getChildren()) {
            HBox.setHgrow(statusBox, Priority.ALWAYS);
        }

        viewModel.doneCountProperty().addListener((obs, old, count) ->
                doneBox.setCount(count.intValue()));
        viewModel.inProgressCountProperty().addListener((obs, old, count) ->
                inProgressBox.setCount(count.intValue()));
        viewModel.canceledCountProperty().addListener((obs, old, count) ->
                canceledBox.setCount(count.intValue()));
    }

    private void setupPeriodButtons() {
        ToggleGroup periodGroup = new ToggleGroup();

        weekBtn = createPeriodButton("W", periodGroup);
        monthBtn = createPeriodButton("M", periodGroup);
        yearBtn = createPeriodButton("Y", periodGroup);

        periodButtonContainer.getChildren().addAll(weekBtn, monthBtn, yearBtn);
        periodButtonContainer.setSpacing(8);

        weekBtn.setOnAction(e -> {
            viewModel.setSelectedPeriod(HomeViewModel.Period.WEEKLY);
            lineChart.setPeriod(HomeViewModel.Period.WEEKLY);
        });
        monthBtn.setOnAction(e -> {
            viewModel.setSelectedPeriod(HomeViewModel.Period.MONTHLY);
            lineChart.setPeriod(HomeViewModel.Period.MONTHLY);
        });
        yearBtn.setOnAction(e -> {
            viewModel.setSelectedPeriod(HomeViewModel.Period.YEARLY);
            lineChart.setPeriod(HomeViewModel.Period.YEARLY);
        });

        weekBtn.setSelected(true);
        viewModel.setSelectedPeriod(HomeViewModel.Period.WEEKLY);
        lineChart.setPeriod(HomeViewModel.Period.WEEKLY);
    }

    private ToggleButton createPeriodButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.getStyleClass().add("period-button");
        btn.setToggleGroup(group);
        return btn;
    }

    private void setupLineChart() {
        lineChart = new CustomLineChart();

        Pane canvasBuffer = new Pane();
        canvasBuffer.getChildren().add(lineChart);

        chartContainer.getChildren().add(canvasBuffer);

        lineChart.widthProperty().bind(canvasBuffer.widthProperty());
        lineChart.heightProperty().bind(canvasBuffer.heightProperty());

        updateChartData();
    }

    private void updateChartData() {

        List<Integer> data = viewModel.getChartData();

        logger.info("Requesting chart update with data: {}", data);
        lineChart.setData(data);
    }


    private void setupCalendar() {
        ButtonStyler.with(calendarPrevBtn)
                .svgPath("src/main/resources/svg/chevron-left.svg")
                .iconSize(12)
                .normalColors("#716996", "transparent")
                .hoverColors("#ffffff", "transparent")
                .apply();

        ButtonStyler.with(calendarNextBtn)
                .svgPath("src/main/resources/svg/chevron-right.svg")
                .iconSize(12)
                .normalColors("#716996", "transparent")
                .hoverColors("#ffffff", "transparent")
                .apply();

        calendarPrevBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        calendarNextBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        updateCalendar();
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();

        calendarMonthYear.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) +
                " " + currentMonth.getYear());

        String[] dayHeaders = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(dayHeaders[i]);
            dayHeader.getStyleClass().add("calendar-day-header");
            calendarGrid.add(dayHeader, i, 0);
            GridPane.setHalignment(dayHeader, javafx.geometry.HPos.CENTER);
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;

        int row = 1;
        int col = firstDayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.getStyleClass().add("calendar-day");

            if (date.equals(LocalDate.now())) dayLabel.getStyleClass().add("calendar-today");
            if (date.equals(selectedDate)) dayLabel.getStyleClass().add("calendar-selected");

            StackPane dayCellContainer;
            if (viewModel.hasPostsOnDate(date)) {
                Circle indicator = new Circle(2, Color.web("#56448e"));
                dayCellContainer = new StackPane(dayLabel, indicator);
                StackPane.setAlignment(indicator, Pos.BOTTOM_CENTER);
                StackPane.setMargin(indicator, new Insets(0, 0, 2, 0));
            } else {
                dayCellContainer = new StackPane(dayLabel);
            }

            calendarGrid.add(dayCellContainer, col, row);
            GridPane.setHalignment(dayCellContainer, javafx.geometry.HPos.CENTER);
            dayCellContainer.setOnMouseClicked(e -> selectDate(date));

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        updateCalendar();
        updateTodayPosts(date);

        viewModel.setCurrentlyViewedDate(date);
    }

    private void setupTodayPosts() {
        todayDateLabel.setText("Today's Posts");
        todayPostsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);
                if (empty || post == null) {
                    setGraphic(null);
                } else {
                    VBox postCell = new VBox(4);
                    postCell.getStyleClass().add("today-post-cell");
                    Label titleLabel = new Label(post.getTitle());
                    titleLabel.getStyleClass().add("today-post-title");
                    Label statusLabel = new Label(post.getStatus().getDisplayName());
                    statusLabel.getStyleClass().addAll("today-post-status", "status-" + post.getStatus().name().toLowerCase());
                    postCell.getChildren().addAll(titleLabel, statusLabel);
                    setGraphic(postCell);
                }
            }
        });
        todayPostsList.setPlaceholder(new Label("No posts for selected date"));
        todayPostsList.setItems(viewModel.getTodayPosts());

        todayPostsList.setSelectionModel(null);
        todayPostsList.setFocusTraversable(false);
    }

    private void updateTodayPosts(LocalDate date) {
        if (date.equals(LocalDate.now())) {
            todayDateLabel.setText("Today's Posts");
        } else {
            todayDateLabel.setText("Posts for " + date.format(DateTimeFormatter.ofPattern("MMM d")));
        }
    }

    private void setupUpcomingEvents() {
        upcomingEventsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HomeViewModel.HolidayEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setGraphic(null);
                } else {
                    HBox eventCell = new HBox(10);
                    eventCell.getStyleClass().add("event-cell");
                    eventCell.setAlignment(Pos.CENTER_LEFT);
                    VBox dateBox = new VBox(2);
                    dateBox.setAlignment(Pos.CENTER);
                    dateBox.getStyleClass().add("event-date-box");
                    Label dayLabel = new Label(String.valueOf(event.getDate().getDayOfMonth()));
                    dayLabel.getStyleClass().add("event-day");
                    Label monthLabel = new Label(event.getDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase());
                    monthLabel.getStyleClass().add("event-month");
                    dateBox.getChildren().addAll(dayLabel, monthLabel);
                    VBox infoBox = new VBox(2);
                    Label nameLabel = new Label(event.getName());
                    nameLabel.getStyleClass().add("event-name");
                    Label hashtagLabel = new Label(event.getHashtag());
                    hashtagLabel.getStyleClass().add("event-hashtag");
                    infoBox.getChildren().addAll(nameLabel, hashtagLabel);
                    infoBox.setAlignment(Pos.CENTER_LEFT);
                    eventCell.getChildren().addAll(dateBox, infoBox);
                    setGraphic(eventCell);
                }
            }
        });
        upcomingEventsList.setPlaceholder(new Label("No upcoming events"));
        upcomingEventsList.setItems(viewModel.getUpcomingEvents());

        upcomingEventsList.setSelectionModel(null);
        upcomingEventsList.setFocusTraversable(false);
    }

    @Override
    public Parent getView() { return root; }

    private static class StatusDisplayBox extends HBox {
        private final Label countLabel;

        public StatusDisplayBox(String title, String iconPath, String color, String bgColor) {
            getStyleClass().add("status-box");
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(15);
            setPadding(new Insets(15));

            StackPane iconContainer = new StackPane();
            iconContainer.getStyleClass().add("status-icon-container");
            iconContainer.setStyle("-fx-background-color: " + bgColor + ";");

            SVGPath icon = new SVGPath();
            try (InputStream is = getClass().getResourceAsStream("/svg/" + iconPath)) {
                if (is == null) {
                    logger.error("Cannot find SVG resource: /svg/{}", iconPath);
                } else {
                    String svgContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    StringBuilder combinedPath = new StringBuilder();
                    Matcher matcher = Pattern.compile("d=\"([^\"]*)\"").matcher(svgContent);
                    while (matcher.find()) {
                        if (combinedPath.length() > 0) {
                            combinedPath.append(" ");
                        }
                        String pathData = matcher.group(1);

                        if (iconPath.contains("check")) {
                            pathData = pathData.replace("M20 6L9 17l-5-5", "M20 6L9 17L4 12");
                        }
                        if (iconPath.contains("x")) {
                            pathData = pathData.replace("M18 6 6 18", "M18 6L6 18")
                                    .replace("m6 6 12 12", "M6 6L18 18");
                        }

                        combinedPath.append(pathData);
                    }

                    if (combinedPath.length() > 0) {
                        icon.setContent(combinedPath.toString());
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to read SVG resource", e);
            }


            if (iconPath.contains("check") || iconPath.contains("x")) {
                icon.setFill(Color.TRANSPARENT);
                icon.setStroke(Color.WHITE);
                icon.setStrokeWidth(2.0);
            } else if (iconPath.contains("hourglass")) {
                icon.setFill(Color.TRANSPARENT);
                icon.setStroke(Color.WHITE);
                icon.setStrokeWidth(1.5);
                icon.setStyle("-fx-scale-x: 1.1; -fx-scale-y: 1.1;");

            }
            icon.getStyleClass().add("status-icon");
            iconContainer.getChildren().add(icon);

            VBox textBox = new VBox(2);
            textBox.setAlignment(Pos.CENTER_LEFT);

            countLabel = new Label("0");
            countLabel.getStyleClass().add("status-count");

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("status-title");

            textBox.getChildren().addAll(countLabel, titleLabel);
            getChildren().addAll(iconContainer, textBox);
            setStyle("-fx-background-color: " + color + "20;");
        }

        public void setCount(int count) {
            countLabel.setText(String.valueOf(count));
        }
    }
}