package com.portfolio.auth.service;

import com.portfolio.TestConfig;
import com.portfolio.auth.entity.User;
import com.portfolio.auth.jwt.JwtTokenProvider;
import com.portfolio.auth.repository.UserRepository;
import com.portfolio.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @BeforeEach
    void setUp() {
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공")
    void register_Success() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String displayName = "Test User";

        // When
        Map<String, Object> result = authService.register(email, password, displayName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("tokens", "user");
        
        Map<String, Object> tokens = (Map<String, Object>) result.get("tokens");
        assertThat(tokens).containsKeys("accessToken", "refreshToken");
        
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertThat(user.get("email")).isEqualTo(email);
        assertThat(user.get("displayName")).isEqualTo(displayName);

        // 데이터베이스 확인
        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getDisplayName()).isEqualTo(displayName);
        assertThat(passwordEncoder.matches(password, savedUser.getPasswordHash())).isTrue();

        // Workspace 자동 생성 확인
        assertThat(workspaceRepository.findByOwnerUserId(savedUser.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 시도 시 예외 발생")
    void register_DuplicateEmail_ThrowsException() {
        // Given
        String email = "test@example.com";
        authService.register(email, "password123", "User 1");

        // When & Then
        assertThatThrownBy(() -> 
            authService.register(email, "password456", "User 2")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이미 사용 중인 이메일입니다");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        authService.register(email, password, "Test User");

        // When
        Map<String, Object> result = authService.login(email, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("tokens", "user");
        
        Map<String, Object> tokens = (Map<String, Object>) result.get("tokens");
        assertThat(tokens).containsKeys("accessToken", "refreshToken");
        
        String accessToken = (String) tokens.get("accessToken");
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.getUsername(accessToken)).isEqualTo(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
    void login_NonExistentEmail_ThrowsException() {
        // Given
        String email = "nonexistent@example.com";
        String password = "password123";

        // When & Then
        assertThatThrownBy(() -> 
            authService.login(email, password)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
    void login_WrongPassword_ThrowsException() {
        // Given
        String email = "test@example.com";
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";
        authService.register(email, correctPassword, "Test User");

        // When & Then
        assertThatThrownBy(() -> 
            authService.login(email, wrongPassword)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
    }
}
