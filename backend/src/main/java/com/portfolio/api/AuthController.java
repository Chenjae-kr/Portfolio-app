package com.portfolio.api;

import com.portfolio.auth.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", "mock-access-token-" + System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
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
