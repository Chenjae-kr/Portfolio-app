package com.portfolio.api;

import com.portfolio.auth.entity.User;
import com.portfolio.auth.jwt.JwtTokenProvider;
import com.portfolio.auth.repository.UserRepository;
import com.portfolio.auth.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> loginData = authService.login(request.getEmail(), request.getPassword());
            
            // API 응답 래퍼
            Map<String, Object> response = new HashMap<>();
            response.put("data", loginData);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", "AUTH_FAILED");
            error.put("message", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", null);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Map<String, Object> registerData = authService.register(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName()
            );
            
            // API 응답 래퍼
            Map<String, Object> response = new HashMap<>();
            response.put("data", registerData);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", "REGISTRATION_FAILED");
            error.put("message", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", null);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return createAuthErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
            }

            String email;
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("displayName", user.getDisplayName());
            userData.put("locale", user.getLocale());

            Map<String, Object> response = new HashMap<>();
            response.put("data", userData);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createAuthErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
                return createAuthErrorResponse("Invalid refresh token", HttpStatus.UNAUTHORIZED);
            }

            String email = jwtTokenProvider.getUsername(refreshToken);
            // 사용자가 존재하는지 확인
            userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String newAccessToken = jwtTokenProvider.generateAccessToken(email);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("accessToken", newAccessToken);
            tokenData.put("refreshToken", newRefreshToken);

            Map<String, Object> response = new HashMap<>();
            response.put("data", tokenData);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createAuthErrorResponse("Failed to refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("data", Map.of("message", "Logged out successfully"));
        response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
        response.put("error", null);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> createAuthErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "AUTH_FAILED");
        error.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String email;
        private String password;
        private String displayName;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}
