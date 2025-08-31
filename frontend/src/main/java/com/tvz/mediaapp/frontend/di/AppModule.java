package com.tvz.mediaapp.frontend.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.tvz.mediaapp.frontend.model.SessionManager;
import com.tvz.mediaapp.frontend.repository.AuthRepository;
import com.tvz.mediaapp.frontend.repository.PostApiRepository;
import com.tvz.mediaapp.frontend.repository.UserApiRepository;
import com.tvz.mediaapp.frontend.service.*;
import com.tvz.mediaapp.frontend.view.*;
import com.tvz.mediaapp.frontend.viewmodel.*;

import java.net.http.HttpClient;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {

        bind(HttpClient.class).toInstance(HttpClient.newHttpClient());
        bind(SessionManager.class).in(Singleton.class);
        bind(AuthRepository.class).in(Singleton.class);
        bind(PostApiRepository.class).in(Singleton.class);
        bind(UserApiRepository.class).in(Singleton.class);
        bind(AuthService.class).in(Singleton.class);
        bind(NotificationService.class).in(Singleton.class);
        bind(UserPreferencesService.class).in(Singleton.class);
        bind(NavigationManager.class).in(Singleton.class);
        bind(ViewCache.class).in(Singleton.class);

        bind(PostsViewModel.class).in(Singleton.class);
        bind(HomeViewModel.class).in(Singleton.class);

        bind(ContentNavigationManager.class);

        bind(LoginView.class);
        bind(MainView.class);
        bind(LoadingView.class);
        bind(GlobalTopBarView.class);
        bind(HomeView.class);
        bind(PostsView.class);
        bind(SettingsView.class);
        bind(PreviewView.class);

        bind(PostCreateEditModalView.class);
        bind(PostViewModalView.class);
        bind(DeleteConfirmationModalView.class);
        bind(PostCreateEditModalViewModel.class);
        bind(PostViewModalViewModel.class);

        bind(LoginViewModel.class);

        try {
            bind(Keyring.class).toInstance(Keyring.create());
        } catch (BackendNotSupportedException e) {
            throw new RuntimeException("Keychain backend not supported on this OS.", e);
        }
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}