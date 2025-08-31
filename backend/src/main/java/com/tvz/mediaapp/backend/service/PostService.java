package com.tvz.mediaapp.backend.service;

import com.tvz.mediaapp.backend.model.Post;
import com.tvz.mediaapp.backend.model.User;
import com.tvz.mediaapp.backend.repository.PostRepository;
import com.tvz.mediaapp.dto.PostDto;
import com.tvz.mediaapp.dto.PostRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<PostDto> getAllPostsForUser(User user) {
        return postRepository.findAllByUserOrderByPublishDateDesc(user).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional
    public PostDto createPost(PostRequestDto dto, User user) {
        Post newPost = Post.fromDto(dto, user);

        if ("IMAGE".equalsIgnoreCase(dto.getMediaType()) || "VIDEO".equalsIgnoreCase(dto.getMediaType())) {
            if (dto.getMediaUris() != null && !dto.getMediaUris().isEmpty()) {
                log.info("Media type is IMAGE or VIDEO. Proceeding to move files.");
                storageService.movePendingFilesToPermanentLocation(newPost, dto.getMediaUris());
            }
        }

        Post savedPost = postRepository.save(newPost);
        log.info("Created new post with UUID: {} for user: {}", savedPost.getUuid(), user.getUsername());
        return convertToDto(savedPost);
    }

    @Transactional
    public PostDto updatePost(UUID uuid, PostRequestDto dto, User user) {
        Post post = postRepository.findByUuidAndUser(uuid, user)
                .orElseThrow(() -> new NoSuchElementException("Post not found with UUID: " + uuid));

        String oldMediaType = post.getMediaType();
        List<String> oldMediaUris = new ArrayList<>(post.getMediaUris());

        if ("IMAGE".equalsIgnoreCase(oldMediaType) || "VIDEO".equalsIgnoreCase(oldMediaType)) {

            if (!"IMAGE".equalsIgnoreCase(dto.getMediaType()) && !"VIDEO".equalsIgnoreCase(dto.getMediaType())) {
                if (!oldMediaUris.isEmpty()) {
                    log.info("Media type changed from {} to {}. Deleting all old media files.", oldMediaType, dto.getMediaType());
                    storageService.deleteMediaObjects(post, oldMediaUris);
                }
            } else {
                Set<String> newFiles = new HashSet<>(dto.getMediaUris());
                List<String> filesToRemove = oldMediaUris.stream()
                        .filter(f -> !newFiles.contains(f))
                        .toList();

                if (!filesToRemove.isEmpty()) {
                    log.info("Deleting {} removed media files.", filesToRemove.size());
                    storageService.deleteMediaObjects(post, filesToRemove);
                }
            }
        }

        if ("IMAGE".equalsIgnoreCase(dto.getMediaType()) || "VIDEO".equalsIgnoreCase(dto.getMediaType())) {
            Set<String> existingFiles = new HashSet<>(oldMediaUris);
            List<String> filesToAdd = dto.getMediaUris().stream()
                    .filter(f -> !existingFiles.contains(f))
                    .toList();

            if (!filesToAdd.isEmpty()) {
                log.info("Moving {} new media files to permanent storage.", filesToAdd.size());
                storageService.movePendingFilesToPermanentLocation(post, filesToAdd);
            }
        }

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setPublishDate(LocalDate.parse(dto.getPublishDate()));
        post.setStatus(dto.getStatus());
        post.setPlatforms(dto.getPlatforms());
        post.setMediaType(dto.getMediaType());
        post.setMediaUris(dto.getMediaUris());

        Post updatedPost = postRepository.save(post);
        log.info("Updated post with UUID: {}", updatedPost.getUuid());
        return convertToDto(updatedPost);
    }

    @Transactional
    public void deletePost(UUID uuid, User user) {
        Post post = postRepository.findByUuidAndUser(uuid, user)
                .orElseThrow(() -> new NoSuchElementException("Post not found with UUID: " + uuid));

        if ("IMAGE".equalsIgnoreCase(post.getMediaType()) || "VIDEO".equalsIgnoreCase(post.getMediaType())) {
            storageService.deleteMediaForPost(post);
        }

        postRepository.delete(post);
        log.info("Deleted post with UUID: {}", uuid);
    }

    private PostDto convertToDto(Post post) {
        PostDto dto = new PostDto();
        dto.setUuid(post.getUuid().toString());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setPublishDate(post.getPublishDate().toString());
        dto.setStatus(post.getStatus());
        dto.setPlatforms(post.getPlatforms());
        dto.setMediaType(post.getMediaType());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setMediaUris(post.getMediaUris());
        return dto;
    }
}