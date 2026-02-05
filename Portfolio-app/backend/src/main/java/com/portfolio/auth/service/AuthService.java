package com.portfolio.auth.service;

import com.portfolio.auth.entity.User;
import com.portfolio.auth.jwt.JwtTokenProvider;
import com.portfolio.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Map<String, Object> register(String email, String password, String displayName) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .locale("ko")
                .build();

        user = userRepository.save(user);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 응답 데이터 구성
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("locale", user.getLocale());

        Map<String, Object> result = new HashMap<>();
        result.put("tokens", tokens);
        result.put("user", userData);

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(String email, String password) {
        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 삭제된 사용자 체크
        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("삭제된 계정입니다.");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 응답 데이터 구성
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("locale", user.getLocale());

        Map<String, Object> result = new HashMap<>();
        result.put("tokens", tokens);
        result.put("user", userData);

        return result;
    }
}
