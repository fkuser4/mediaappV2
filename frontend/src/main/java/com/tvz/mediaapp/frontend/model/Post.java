package com.tvz.mediaapp.frontend.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private String uuid;
    private final StringProperty title;
    private final StringProperty content = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> date;
    private final ListProperty<Platform> platforms;
    private final ObjectProperty<Status> status;
    private final ObjectProperty<MediaType> mediaType = new SimpleObjectProperty<>(MediaType.NONE);
    private final ListProperty<String> mediaUris = new SimpleListProperty<>(FXCollections.observableArrayList());

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {
        this.title = new SimpleStringProperty();
        this.date = new SimpleObjectProperty<>();
        this.status = new SimpleObjectProperty<>();
        this.platforms = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    public Post(String title, LocalDate date, Status status, Platform... platforms) {
        this.title = new SimpleStringProperty(title);
        this.date = new SimpleObjectProperty<>(date);
        this.status = new SimpleObjectProperty<>(status);
        this.platforms = new SimpleListProperty<>(FXCollections.observableArrayList(platforms));
        this.mediaUris.set(FXCollections.observableArrayList(new ArrayList<>()));
    }

    public Post(Post other) {
        this.id.set(other.getId());
        this.uuid = other.getUuid();
        this.title = new SimpleStringProperty(other.getTitle());
        this.content.set(other.getContent());
        this.date = new SimpleObjectProperty<>(other.getDate());
        this.platforms = new SimpleListProperty<>(FXCollections.observableArrayList(other.getPlatforms()));
        this.status = new SimpleObjectProperty<>(other.getStatus());
        this.mediaType.set(other.getMediaType());
        this.mediaUris.set(FXCollections.observableArrayList(other.getMediaUris()));
        this.createdAt = other.getCreatedAt();
        this.updatedAt = other.getUpdatedAt();
    }

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getTitle() { return title.get(); }
    public void setTitle(String title) { this.title.set(title); }
    public StringProperty titleProperty() { return title; }

    public String getContent() { return content.get(); }
    public void setContent(String content) { this.content.set(content); }
    public StringProperty contentProperty() { return content; }

    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate date) { this.date.set(date); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public ObservableList<Platform> getPlatforms() { return platforms.get(); }

    public void setPlatforms(List<Platform> platformList) {
        this.platforms.set(FXCollections.observableArrayList(platformList));
    }

    public ListProperty<Platform> platformsProperty() { return platforms; }

    public Status getStatus() { return status.get(); }
    public void setStatus(Status status) { this.status.set(status); }
    public ObjectProperty<Status> statusProperty() { return status; }

    public MediaType getMediaType() { return mediaType.get(); }
    public void setMediaType(MediaType type) { this.mediaType.set(type); }
    public ObjectProperty<MediaType> mediaTypeProperty() { return mediaType; }

    public ObservableList<String> getMediaUris() { return mediaUris.get(); }
    public void setMediaUris(List<String> uris) { this.mediaUris.set(FXCollections.observableArrayList(uris)); }
    public ListProperty<String> mediaUrisProperty() { return mediaUris; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}