package com.tvz.mediaapp.dto;

import lombok.Data;
import java.util.List;

@Data
public class PostRequestDto {
    private String uuid;
    private String title;
    private String content;
    private String publishDate;
    private String status;
    private List<String> platforms;
    private String mediaType;
    private List<String> mediaUris;
}