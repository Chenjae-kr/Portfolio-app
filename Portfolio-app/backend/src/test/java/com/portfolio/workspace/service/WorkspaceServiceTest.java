package com.portfolio.workspace.service;

import com.portfolio.TestConfig;
import com.portfolio.workspace.entity.Workspace;
import com.portfolio.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("WorkspaceService 테스트")
class WorkspaceServiceTest {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        workspaceRepository.deleteAll();
    }

    @Test
    @DisplayName("기본 workspace 생성 성공")
    void createDefaultWorkspace_Success() {
        // When
        Workspace workspace = workspaceService.createDefaultWorkspace(TEST_USER_ID, TEST_EMAIL);

        // Then
        assertThat(workspace).isNotNull();
        assertThat(workspace.getId()).isNotNull();
        assertThat(workspace.getOwnerUserId()).isEqualTo(TEST_USER_ID);
        assertThat(workspace.getName()).isEqualTo(TEST_EMAIL + "'s Workspace");
        assertThat(workspace.getCreatedAt()).isNotNull();

        // 데이터베이스 확인
        Workspace saved = workspaceRepository.findById(workspace.getId()).orElseThrow();
        assertThat(saved.getOwnerUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("중복 workspace 생성 시도 시 기존 workspace 반환")
    void createDefaultWorkspace_AlreadyExists_ReturnsExisting() {
        // Given
        Workspace existing = workspaceService.createDefaultWorkspace(TEST_USER_ID, TEST_EMAIL);

        // When
        Workspace result = workspaceService.createDefaultWorkspace(TEST_USER_ID, TEST_EMAIL);

        // Then
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(workspaceRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자의 기본 workspace 조회 성공")
    void getDefaultWorkspace_Success() {
        // Given
        Workspace created = workspaceService.createDefaultWorkspace(TEST_USER_ID, TEST_EMAIL);

        // When
        Workspace found = workspaceService.getDefaultWorkspace(TEST_USER_ID);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getOwnerUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("workspace가 없는 사용자 조회 시 예외 발생")
    void getDefaultWorkspace_NotFound_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> 
            workspaceService.getDefaultWorkspace("non-existent-user")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No workspace found");
    }
}
