package com.corc.backend.service;

import com.corc.backend.dto.request.LoginRequest;
import com.corc.backend.dto.request.RegisterRequest;
import com.corc.backend.dto.response.AuthResponse;
import com.corc.backend.dto.response.UserResponse;
import com.corc.backend.entity.RefreshToken;
import com.corc.backend.entity.Role;
import com.corc.backend.entity.User;
import com.corc.backend.entity.enums.RoleName;
import com.corc.backend.exception.DuplicateResourceException;
import com.corc.backend.exception.InvalidTokenException;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.RefreshTokenRepository;
import com.corc.backend.repository.RoleRepository;
import com.corc.backend.repository.UserRepository;
import com.corc.backend.security.JwtTokenProvider;
import com.corc.backend.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final MailService mailService;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public record AuthResult(AuthResponse authResponse, String refreshToken) {}

    @Transactional
    public AuthResult registerWithToken(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        String avatarUrl = "https://ui-avatars.com/api/?name="
                + request.getName().replace(" ", "+")
                + "&background=c6a87c&color=000";

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone("")
                .avatar(avatarUrl)
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);
        mailService.sendWelcomeEmail(user.getEmail(), user.getName());

        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        RefreshToken rt = createRefreshToken(user);

        return new AuthResult(buildAuthResponse(user, accessToken), rt.getToken());
    }

    @Transactional
    public AuthResult loginWithToken(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String accessToken = tokenProvider.generateAccessToken(authentication);

        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken rt = createRefreshToken(user);

        return new AuthResult(buildAuthResponse(user, accessToken), rt.getToken());
    }

    @Transactional
    public AuthResult refreshWithToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);
        RefreshToken newToken = createRefreshToken(user);
        String accessToken = tokenProvider.generateAccessToken(user.getEmail());

        return new AuthResult(buildAuthResponse(user, accessToken), newToken.getToken());
    }

    @Transactional
    public void logout(String accessToken, String refreshTokenStr) {
        if (accessToken != null && tokenProvider.validateToken(accessToken)) {
            long remaining = tokenProvider.getRemainingMillis(accessToken);
            tokenBlacklist.blacklist(accessToken, remaining);
        }

        if (refreshTokenStr != null) {
            refreshTokenRepository.findByToken(refreshTokenStr)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .isAdmin(isAdmin)
                .accessToken(accessToken)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .isAdmin(isAdmin)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}
