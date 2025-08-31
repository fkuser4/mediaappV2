package com.tvz.mediaapp.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostDto {
    private String uuid;
    private String title;
    private String content;
    private String publishDate;
    private String status;
    private List<String> platforms;
    private String mediaType;
    private List<String> mediaUris;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, String> uploadUrls;
}