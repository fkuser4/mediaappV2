package com.tvz.mediaapp.frontend.model;

import com.google.inject.Singleton;
import com.tvz.mediaapp.dto.UserDto;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Singleton
public class SessionManager {
    private final StringProperty accessToken = new SimpleStringProperty();
    private final ObjectProperty<UserDto> currentUser = new SimpleObjectProperty<>();

    public String getAccessToken() { return accessToken.get(); }
    public void setAccessToken(String token) { this.accessToken.set(token); }
    public StringProperty accessTokenProperty() { return accessToken; }

    public UserDto getCurrentUser() { return currentUser.get(); }
    public void setCurrentUser(UserDto user) { this.currentUser.set(user); }
    public ObjectProperty<UserDto> currentUserProperty() { return currentUser; }

    public void clear() {
        setAccessToken(null);
        setCurrentUser(null);
    }
}