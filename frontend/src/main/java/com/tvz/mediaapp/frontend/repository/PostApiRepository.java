package com.tvz.mediaapp.frontend.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tvz.mediaapp.dto.DownloadRequestDto;
import com.tvz.mediaapp.dto.PostDto;
import com.tvz.mediaapp.dto.PostRequestDto;
import com.tvz.mediaapp.dto.UploadRequestDto;
import com.tvz.mediaapp.frontend.model.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PostApiRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostApiRepository.class);
    private static final String API_BASE_URL = "http://localhost:8080/api";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;

    @Inject
    public PostApiRepository(HttpClient httpClient, ObjectMapper objectMapper, SessionManager sessionManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
    }

    private HttpRequest.Builder createAuthenticatedRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + sessionManager.getAccessToken());
    }

    public CompletableFuture<List<PostDto>> getPosts() {
        HttpRequest request = createAuthenticatedRequest(URI.create(API_BASE_URL + "/posts")).GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) throw new RuntimeException("Failed to fetch posts");
                    try {
                        return objectMapper.readValue(response.body(), new TypeReference<>() {});
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse posts", e);
                    }
                });
    }

    public CompletableFuture<PostDto> createPost(PostRequestDto newPost) {
        try {
            String requestBody = objectMapper.writeValueAsString(newPost);
            logger.info("Creating post with media URIs: {}", newPost.getMediaUris());

            HttpRequest request = createAuthenticatedRequest(URI.create(API_BASE_URL + "/posts"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        logger.debug("Create post response status: {}", response.statusCode());
                        if (response.statusCode() != 201) {
                            logger.error("Failed to create post. Status: {}, Response: {}", response.statusCode(), response.body());
                            throw new RuntimeException("Failed to create post (HTTP " + response.statusCode() + ")");
                        }
                        try {
                            PostDto result = objectMapper.readValue(response.body(), PostDto.class);
                            logger.info("Successfully created post with UUID: {}", result.getUuid());
                            return result;
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse created post", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<PostDto> updatePost(String uuid, PostRequestDto updatedPost) {
        try {
            String requestBody = objectMapper.writeValueAsString(updatedPost);
            HttpRequest request = createAuthenticatedRequest(URI.create(API_BASE_URL + "/posts/" + uuid))
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) throw new RuntimeException("Failed to update post");
                        try {
                            return objectMapper.readValue(response.body(), PostDto.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse updated post", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> deletePost(String uuid) {
        HttpRequest request = createAuthenticatedRequest(URI.create(API_BASE_URL + "/posts/" + uuid))
                .DELETE()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 204) throw new RuntimeException("Failed to delete post");
                });
    }

    public CompletableFuture<Map<String, String>> generateUploadUrl(String originalFilename) {
        try {
            UploadRequestDto dto = new UploadRequestDto();
            dto.setFilename(originalFilename);
            String requestBody = objectMapper.writeValueAsString(dto);

            logger.debug("Requesting upload URL for filename: {}", originalFilename);

            HttpRequest request = createAuthenticatedRequest(URI.create(API_BASE_URL + "/media/generate-upload-url"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            logger.error("Failed to get upload URL. Status: {}, Response: {}", response.statusCode(), response.body());
                            throw new RuntimeException("Failed to get upload URL (HTTP " + response.statusCode() + ")");
                        }
                        try {
                            Map<String, String> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
                            logger.debug("Received upload URL for filename: {}", originalFilename);
                            return result;
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse upload URL response", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Map<String, String>> generateDownloadUrls(String postUuid, List<String> filenames) {
        try {
            DownloadRequestDto dto = new DownloadRequestDto();
            dto.setPostUuid(postUuid);
            dto.setFilenames(filenames);
            String requestBody = objectMapper.writeValueAsString(dto);

            HttpRequest request = createAuthenticatedRequest(URI.create(API_BASE_URL + "/media/generate-download-urls"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) throw new RuntimeException("Failed to get download URLs");
                        try {
                            return objectMapper.readValue(response.body(), new TypeReference<>() {});
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse download URLs response", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> uploadFile(String preSignedUrl, Path filePath, String contentType) {
        try {
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File does not exist: " + filePath);
            }

            long fileSize = Files.size(filePath);
            logger.debug("Uploading file: {} (size: {} bytes) to S3", filePath.getFileName(), fileSize);

            HttpRequest request = HttpRequest.newBuilder(URI.create(preSignedUrl))
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofFile(filePath))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        logger.debug("Upload response for {}: status={}, body length={}",
                                filePath.getFileName(), response.statusCode(),
                                response.body() != null ? response.body().length() : 0);

                        if (response.statusCode() != 200) {
                            logger.error("File upload failed for {}. Status: {}, Response: {}",
                                    filePath.getFileName(), response.statusCode(),
                                    response.body() != null ? response.body().substring(0, Math.min(response.body().length(), 500)) : "null");
                            throw new RuntimeException("File upload failed with status: " + response.statusCode());
                        }
                        logger.info("Successfully uploaded file: {} ({} bytes)", filePath.getFileName(), fileSize);
                    });
        } catch (IOException e) {
            logger.error("Could not read file for upload: {}", filePath, e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            logger.error("Upload failed for file: {}", filePath, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}