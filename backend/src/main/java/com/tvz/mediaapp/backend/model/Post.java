package com.tvz.mediaapp.backend.model;

import com.tvz.mediaapp.dto.PostRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {

    @Id
    private UUID uuid;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDate publishDate;

    @Column(nullable = false)
    private String status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_platforms", joinColumns = @JoinColumn(name = "post_uuid"))
    @Column(name = "platform")
    private List<String> platforms;

    @Column(nullable = false)
    private String mediaType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_media_uris", joinColumns = @JoinColumn(name = "post_uuid"))
    @Column(name = "media_uri")
    private List<String> mediaUris;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Post fromDto(PostRequestDto dto, User user) {
        return Post.builder()
                .uuid(dto.getUuid() != null ? UUID.fromString(dto.getUuid()) : UUID.randomUUID())
                .title(dto.getTitle())
                .content(dto.getContent())
                .publishDate(LocalDate.parse(dto.getPublishDate()))
                .status(dto.getStatus())
                .platforms(dto.getPlatforms())
                .mediaType(dto.getMediaType())
                .mediaUris(dto.getMediaUris())
                .user(user)
                .build();
    }
}