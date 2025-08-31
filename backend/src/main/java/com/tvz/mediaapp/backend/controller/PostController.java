package com.tvz.mediaapp.backend.controller;

import com.tvz.mediaapp.backend.model.User;
import com.tvz.mediaapp.backend.service.PostService;
import com.tvz.mediaapp.dto.PostDto;
import com.tvz.mediaapp.dto.PostRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(postService.getAllPostsForUser(user));
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostRequestDto postDto, @AuthenticationPrincipal User user) {
        return new ResponseEntity<>(postService.createPost(postDto, user), HttpStatus.CREATED);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<PostDto> updatePost(@PathVariable UUID uuid, @RequestBody PostRequestDto postDto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(postService.updatePost(uuid, postDto, user));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID uuid, @AuthenticationPrincipal User user) {
        postService.deletePost(uuid, user);
        return ResponseEntity.noContent().build();
    }
}