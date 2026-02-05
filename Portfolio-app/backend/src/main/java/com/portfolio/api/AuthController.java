package com.portfolio.api;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 개발 모드: 간단한 mock 응답
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "mock-access-token-" + System.currentTimeMillis());
        tokens.put("refreshToken", "mock-refresh-token-" + System.currentTimeMillis());
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", "test-user");
        user.put("email", request.getEmail());
        user.put("displayName", "Test User");
        user.put("locale", "ko");
        
        Map<String, Object> loginData = new HashMap<>();
        loginData.put("tokens", tokens);
        loginData.put("user", user);
        
        // API 응답 래퍼
        Map<String, Object> response = new HashMap<>();
        response.put("data", loginData);
        response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
        response.put("error", null);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // 개발 모드: 간단한 mock 응답
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "mock-access-token-" + System.currentTimeMillis());
        tokens.put("refreshToken", "mock-refresh-token-" + System.currentTimeMillis());
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", "new-user-" + System.currentTimeMillis());
        user.put("email", request.getEmail());
        user.put("displayName", request.getDisplayName());
        user.put("locale", "ko");
        
        Map<String, Object> registerData = new HashMap<>();
        registerData.put("tokens", tokens);
        registerData.put("user", user);
        
        // API 응답 래퍼
        Map<String, Object> response = new HashMap<>();
        response.put("data", registerData);
        response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
        response.put("error", null);
        
        return ResponseEntity.ok(response);
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
