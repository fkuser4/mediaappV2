package com.tvz.mediaapp.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.model.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class UserPreferencesService {
    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);
    private static final String PREFERENCES_FILE = "user-preferences.json";

    private final BooleanProperty useAmericanDateFormat = new SimpleBooleanProperty(false);

    private final ObservableSet<Platform> selectedPlatforms = FXCollections.observableSet(new HashSet<>());

    private final BooleanProperty darkMode = new SimpleBooleanProperty(true);
    private final IntegerProperty fontSize = new SimpleIntegerProperty(14);

    private final ObjectMapper objectMapper;
    private final Path preferencesPath;

    @Inject
    public UserPreferencesService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.preferencesPath = getPreferencesPath();

        selectedPlatforms.addAll(Set.of(Platform.values()));

        loadPreferences();

        setupAutoSave();
    }

    private Path getPreferencesPath() {
        String userHome = System.getProperty("user.home");
        Path appDataDir = Paths.get(userHome, ".mediaapp");

        try {
            Files.createDirectories(appDataDir);
        } catch (IOException e) {
            logger.warn("Could not create app data directory, using current directory", e);
            return Paths.get(PREFERENCES_FILE);
        }

        return appDataDir.resolve(PREFERENCES_FILE);
    }

    private void setupAutoSave() {
        useAmericanDateFormat.addListener((obs, oldVal, newVal) -> savePreferences());
        darkMode.addListener((obs, oldVal, newVal) -> savePreferences());
        fontSize.addListener((obs, oldVal, newVal) -> savePreferences());

        selectedPlatforms.addListener((javafx.collections.SetChangeListener<Platform>) change -> {
            if (selectedPlatforms.isEmpty()) {
                selectedPlatforms.add(Platform.FACEBOOK);
            }
            savePreferences();
        });
    }

    public void loadPreferences() {
        try {
            if (Files.exists(preferencesPath)) {
                UserPreferencesData data = objectMapper.readValue(preferencesPath.toFile(), UserPreferencesData.class);

                useAmericanDateFormat.set(data.useAmericanDateFormat);
                darkMode.set(data.darkMode);
                fontSize.set(data.fontSize);

                if (data.selectedPlatforms != null && !data.selectedPlatforms.isEmpty()) {
                    selectedPlatforms.clear();
                    selectedPlatforms.addAll(data.selectedPlatforms);
                }

                logger.info("User preferences loaded successfully");
            } else {
                logger.info("No preferences file found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load user preferences, using defaults", e);
        }
    }

    public void savePreferences() {
        try {
            UserPreferencesData data = new UserPreferencesData();
            data.useAmericanDateFormat = useAmericanDateFormat.get();
            data.darkMode = darkMode.get();
            data.fontSize = fontSize.get();
            data.selectedPlatforms = new HashSet<>(selectedPlatforms);

            objectMapper.writeValue(preferencesPath.toFile(), data);
            logger.debug("User preferences saved successfully");
        } catch (IOException e) {
            logger.error("Failed to save user preferences", e);
        }
    }

    public DateTimeFormatter getDateFormatter() {
        return useAmericanDateFormat.get() ?
                DateTimeFormatter.ofPattern("MM/dd/yyyy") :
                DateTimeFormatter.ofPattern("dd/MM/yyyy");
    }

    public String formatDateForDisplay(java.time.LocalDate date) {
        return date.format(getDateFormatter());
    }

    public boolean isPlatformSelected(Platform platform) {
        return selectedPlatforms.contains(platform);
    }

    public void selectPlatform(Platform platform) {
        selectedPlatforms.add(platform);
    }

    public void deselectPlatform(Platform platform) {
        if (selectedPlatforms.size() > 1) {
            selectedPlatforms.remove(platform);
        } else {
            logger.warn("Cannot deselect last platform. At least one must remain selected.");
        }
    }

    public boolean canDeselectPlatform(Platform platform) {
        return selectedPlatforms.size() > 1 && selectedPlatforms.contains(platform);
    }

    public BooleanProperty useAmericanDateFormatProperty() {
        return useAmericanDateFormat;
    }

    public ObservableSet<Platform> getSelectedPlatforms() {
        return selectedPlatforms;
    }

    public boolean isUseAmericanDateFormat() {
        return useAmericanDateFormat.get();
    }

    public void setUseAmericanDateFormat(boolean useAmericanFormat) {
        this.useAmericanDateFormat.set(useAmericanFormat);
    }

    public static class UserPreferencesData {
        public boolean useAmericanDateFormat = false;
        public boolean darkMode = true;
        public int fontSize = 14;
        public Set<Platform> selectedPlatforms = new HashSet<>();

        public UserPreferencesData() {}
    }
}