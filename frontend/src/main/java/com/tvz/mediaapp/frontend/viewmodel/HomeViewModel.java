package com.tvz.mediaapp.frontend.viewmodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.model.Status;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class HomeViewModel {
    private static final Logger logger = LoggerFactory.getLogger(HomeViewModel.class);


    @Inject private PostsViewModel postsViewModel;
    @Inject private ObjectMapper objectMapper;

    private final ObservableList<Post> allPosts = FXCollections.observableArrayList();
    private final ObservableList<Post> todayPosts = FXCollections.observableArrayList();
    private final ObservableList<HolidayEvent> upcomingEvents = FXCollections.observableArrayList();

    private final ObjectProperty<Period> selectedPeriod = new SimpleObjectProperty<>(Period.WEEKLY);

    private final IntegerProperty doneCount = new SimpleIntegerProperty(0);
    private final IntegerProperty inProgressCount = new SimpleIntegerProperty(0);
    private final IntegerProperty canceledCount = new SimpleIntegerProperty(0);

    private Map<String, Map<String, List<Holiday>>> calendarData;

    private LocalDate currentlyViewedDate = LocalDate.now();


    public enum Period {
        WEEKLY, MONTHLY, YEARLY
    }

    public void initialize() {
        ObservableList<Post> masterPostList = postsViewModel.getPostList();

        syncPosts(masterPostList);

        masterPostList.addListener((ListChangeListener<Post>) change -> {
            logger.info("Detected change in master post list, syncing HomeViewModel...");
            javafx.application.Platform.runLater(() -> syncPosts(change.getList()));
        });

        loadCalendarData();
        loadUpcomingEvents();
    }

    private void syncPosts(List<? extends Post> masterList) {
        allPosts.setAll(masterList);
        updateCounts();
        loadPostsForDate(currentlyViewedDate);
    }

    public void setCurrentlyViewedDate(LocalDate date) {
        this.currentlyViewedDate = date;
        loadPostsForDate(date);
    }

    public void loadPostsForDate(LocalDate date) {
        List<Post> postsForDate = allPosts.stream()
                .filter(post -> post.getDate() != null && post.getDate().equals(date))
                .collect(Collectors.toList());
        todayPosts.setAll(postsForDate);
    }

    private void loadCalendarData() {
        try (InputStream inputStream = getClass().getResourceAsStream("/data/calendar_data.json")) {
            if (inputStream == null) {
                logger.warn("Calendar data file not found in resources: /data/calendar_data.json");
                calendarData = new HashMap<>();
                return;
            }
            JsonNode root = objectMapper.readTree(inputStream);
            calendarData = new HashMap<>();

            JsonNode months = root.get("months");
            if (months != null) {
                months.fieldNames().forEachRemaining(month -> {
                    Map<String, List<Holiday>> monthData = new HashMap<>();
                    JsonNode monthNode = months.get(month);

                    monthNode.fieldNames().forEachRemaining(day -> {
                        List<Holiday> holidays = new ArrayList<>();
                        JsonNode dayNode = monthNode.get(day);

                        if (dayNode.isArray()) {
                            dayNode.forEach(holiday -> {
                                String name = holiday.get("name").asText();
                                String hashtag = holiday.get("hashtag").asText();
                                holidays.add(new Holiday(name, hashtag));
                            });
                        }
                        monthData.put(day, holidays);
                    });
                    calendarData.put(month, monthData);
                });
            }
            logger.info("Loaded calendar data successfully");

        } catch (IOException e) {
            logger.error("Failed to load or parse calendar data", e);
            calendarData = new HashMap<>();
        }
    }


    private void loadUpcomingEvents() {
        if (calendarData == null || calendarData.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);
        List<HolidayEvent> events = new ArrayList<>();

        LocalDate current = today;
        while (!current.isAfter(endDate)) {
            String monthName = current.getMonth().toString();
            monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase();
            String dayStr = String.valueOf(current.getDayOfMonth());

            Map<String, List<Holiday>> monthData = calendarData.get(monthName);
            if (monthData != null) {
                List<Holiday> holidays = monthData.get(dayStr);
                if (holidays != null) {
                    for (Holiday holiday : holidays) {
                        events.add(new HolidayEvent(current, holiday.name, holiday.hashtag));
                    }
                }
            }
            current = current.plusDays(1);
        }

        events.sort(Comparator.comparing(HolidayEvent::getDate));
        upcomingEvents.setAll(events.stream().limit(10).collect(Collectors.toList()));
    }

    public List<Integer> getChartData() {
        Period period = selectedPeriod.get();
        if (period == null) {
            return Collections.emptyList();
        }

        switch (period) {
            case WEEKLY:
                return getWeeklyData(allPosts);
            case MONTHLY:
                return getMonthlyData(allPosts);
            case YEARLY:
                return getYearlyData(allPosts);
            default:
                return Collections.emptyList();
        }
    }

    private List<Integer> getWeeklyData(List<Post> posts) {
        List<Integer> data = new ArrayList<>(Collections.nCopies(7, 0));
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        for (Post post : posts) {
            if (post.getCreatedAt() != null) {
                LocalDate postDate = post.getCreatedAt().toLocalDate();
                if (!postDate.isBefore(startOfWeek) && !postDate.isAfter(endOfWeek)) {
                    int dayOfWeek = postDate.getDayOfWeek().getValue();
                    data.set(dayOfWeek - 1, data.get(dayOfWeek - 1) + 1);
                }
            }
        }
        logger.debug("Weekly (Days of Week) data: {}", data);
        return data;
    }


    private List<Integer> getMonthlyData(List<Post> posts) {
        List<Integer> data = new ArrayList<>(Collections.nCopies(12, 0));
        int currentYear = LocalDate.now().getYear();
        for (Post post : posts) {
            if (post.getCreatedAt() != null && post.getCreatedAt().toLocalDate().getYear() == currentYear) {
                int month = post.getCreatedAt().toLocalDate().getMonthValue() - 1;
                data.set(month, data.get(month) + 1);
            }
        }
        logger.debug("Monthly data: {}", data);
        return data;
    }

    private List<Integer> getYearlyData(List<Post> posts) {
        Map<Integer, Integer> yearlyCounts = new HashMap<>();
        int currentYear = LocalDate.now().getYear();

        for (Post post : posts) {
            if (post.getCreatedAt() != null) {
                int year = post.getCreatedAt().toLocalDate().getYear();
                yearlyCounts.put(year, yearlyCounts.getOrDefault(year, 0) + 1);
            }
        }

        List<Integer> data = new ArrayList<>(Collections.nCopies(5, 0));
        for(int i=0; i < 5; i++){
            int year = currentYear - (4-i);
            data.set(i, yearlyCounts.getOrDefault(year, 0));
        }

        logger.debug("Yearly data: {}", data);
        return data;
    }

    public boolean hasPostsOnDate(LocalDate date) {
        return allPosts.stream()
                .anyMatch(post -> post.getDate() != null && post.getDate().equals(date));
    }

    public IntegerProperty doneCountProperty() { return doneCount; }
    public IntegerProperty inProgressCountProperty() { return inProgressCount; }
    public IntegerProperty canceledCountProperty() { return canceledCount; }

    private void updateCounts() {
        int done = (int) allPosts.stream().filter(p -> p.getStatus() == Status.DONE).count();
        int progress = (int) allPosts.stream().filter(p -> p.getStatus() == Status.IN_PROGRESS).count();
        int canceled = (int) allPosts.stream().filter(p -> p.getStatus() == Status.CANCELED).count();

        doneCount.set(done);
        inProgressCount.set(progress);
        canceledCount.set(canceled);

        logger.info("Updated counts - Done: {}, In Progress: {}, Canceled: {}", done, progress, canceled);
    }

    public ObjectProperty<Period> selectedPeriodProperty() { return selectedPeriod; }
    public Period getSelectedPeriod() { return selectedPeriod.get(); }
    public void setSelectedPeriod(Period period) { selectedPeriod.set(period); }
    public ObservableList<Post> getTodayPosts() { return todayPosts; }
    public ObservableList<HolidayEvent> getUpcomingEvents() { return upcomingEvents; }
    public ObservableList<Post> allPostsProperty() { return allPosts; }

    private static class Holiday {
        String name;
        String hashtag;

        Holiday(String name, String hashtag) {
            this.name = name;
            this.hashtag = hashtag;
        }
    }

    public static class HolidayEvent {
        private final LocalDate date;
        private final String name;
        private final String hashtag;

        public HolidayEvent(LocalDate date, String name, String hashtag) {
            this.date = date;
            this.name = name;
            this.hashtag = hashtag;
        }

        public LocalDate getDate() { return date; }
        public String getName() { return name; }
        public String getHashtag() { return hashtag; }
    }
}