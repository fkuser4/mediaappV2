package com.tvz.mediaapp.frontend.utils;

import com.tvz.mediaapp.dto.PostDto;
import com.tvz.mediaapp.dto.PostRequestDto;
import com.tvz.mediaapp.frontend.model.MediaType;
import com.tvz.mediaapp.frontend.model.Platform;
import com.tvz.mediaapp.frontend.model.Post;
import com.tvz.mediaapp.frontend.model.Status;

import java.time.LocalDate;
import java.util.ArrayList;

public class PostMapper {

    public static Post fromDto(PostDto dto) {
        Post post = new Post(
                dto.getTitle(),
                LocalDate.parse(dto.getPublishDate()),
                Status.valueOf(dto.getStatus())
        );
        post.setUuid(dto.getUuid());
        post.setContent(dto.getContent());
        post.setCreatedAt(dto.getCreatedAt());
        post.setUpdatedAt(dto.getUpdatedAt());

        if (dto.getPlatforms() != null) {
            post.getPlatforms().setAll(dto.getPlatforms().stream()
                    .map(Platform::valueOf)
                    .toList());
        }

        if (dto.getMediaType() != null) {
            post.setMediaType(MediaType.valueOf(dto.getMediaType()));
        }

        post.setMediaUris(dto.getMediaUris() != null ? dto.getMediaUris() : new ArrayList<>());
        return post;
    }

    public static PostRequestDto toRequestDto(Post post) {
        PostRequestDto dto = new PostRequestDto();
        dto.setUuid(post.getUuid());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setPublishDate(post.getDate().toString());
        dto.setStatus(post.getStatus().name());
        dto.setPlatforms(post.getPlatforms().stream()
                .map(Enum::name)
                .toList());
        dto.setMediaType(post.getMediaType().name());
        dto.setMediaUris(new ArrayList<>(post.getMediaUris()));
        return dto;
    }
}