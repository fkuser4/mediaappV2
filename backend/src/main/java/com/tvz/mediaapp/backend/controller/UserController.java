package com.tvz.mediaapp.backend.controller;

import com.tvz.mediaapp.backend.model.User;
import com.tvz.mediaapp.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserDto> getAuthenticatedUser(@AuthenticationPrincipal User user) {
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
        return ResponseEntity.ok(userDto);
    }
}
