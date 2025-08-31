package com.tvz.mediaapp.backend.service;

import com.tvz.mediaapp.backend.exception.UserAlreadyExistsException;
import com.tvz.mediaapp.backend.model.Role;
import com.tvz.mediaapp.backend.model.User;
import com.tvz.mediaapp.backend.repository.UserRepository;
import com.tvz.mediaapp.dto.AuthResponseDto;
import com.tvz.mediaapp.dto.LoginRequestDto;
import com.tvz.mediaapp.dto.RegisterRequestDto;
import com.tvz.mediaapp.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        log.info("Attempting to register user with username: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Attempting login for: {}", request.getLogin());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );

        User user = userRepository.findByUsernameOrEmail(request.getLogin(), request.getLogin())
                .orElseThrow(() -> new UsernameNotFoundException("User not found during login"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in successfully: {}", user.getUsername());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponseDto refreshToken(String refreshToken) {
        final String username = jwtService.extractUsername(refreshToken);
        if (username == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found from refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        String newAccessToken = jwtService.generateToken(user);
        return buildAuthResponse(user, newAccessToken, refreshToken);
    }


    private AuthResponseDto buildAuthResponse(User user, String accessToken, String refreshToken) {
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDto)
                .build();
    }
}
