package com.tvz.mediaapp.backend.repository;

import com.tvz.mediaapp.backend.model.Post;
import com.tvz.mediaapp.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findByUuidAndUser(UUID uuid, User user);

    List<Post> findAllByUserOrderByPublishDateDesc(User user);
}