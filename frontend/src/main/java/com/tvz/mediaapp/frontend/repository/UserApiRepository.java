package com.tvz.mediaapp.frontend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.tvz.mediaapp.dto.AuthResponseDto;
import com.tvz.mediaapp.dto.LoginRequestDto;
import com.tvz.mediaapp.dto.RefreshTokenRequestDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class UserApiRepository {
    private static final String API_BASE_URL = "http://localhost:8080/api";

    @Inject
    private HttpClient httpClient;
    @Inject
    private ObjectMapper objectMapper;

    public CompletableFuture<AuthResponseDto> login(String username, String password) throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setLogin(username);
        loginRequest.setPassword(password);
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(this::handleAuthResponse);
    }

    public CompletableFuture<AuthResponseDto> refreshToken(String refreshToken) throws Exception {
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken(refreshToken);
        String requestBody = objectMapper.writeValueAsString(refreshRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/auth/refresh-token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(this::handleAuthResponse);
    }

    private AuthResponseDto handleAuthResponse(HttpResponse<String> response) {
        try {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), AuthResponseDto.class);
            } else {
                throw new RuntimeException("API call failed with status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse API response", e);
        }
    }
}