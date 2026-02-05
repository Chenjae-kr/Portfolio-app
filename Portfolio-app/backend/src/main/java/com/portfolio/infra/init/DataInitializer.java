package com.portfolio.infra.init;

import com.portfolio.auth.entity.User;
import com.portfolio.auth.repository.UserRepository;
import com.portfolio.workspace.entity.Workspace;
import com.portfolio.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 필수 초기 데이터를 생성합니다.
 * - 개발 모드용 시스템 사용자
 * - 개발 모드용 기본 워크스페이스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    // PortfolioController에서 사용하는 기본 워크스페이스 ID와 동일해야 함
    public static final String DEFAULT_WORKSPACE_ID = "default-workspace";
    public static final String SYSTEM_USER_ID = "system";
    private static final String SYSTEM_USER_EMAIL = "system@portfolio.local";
    private static final String DEFAULT_WORKSPACE_NAME = "Default Workspace";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createSystemUserIfNotExists();
        createDefaultWorkspaceIfNotExists();
    }

    private void createSystemUserIfNotExists() {
        if (userRepository.existsById(SYSTEM_USER_ID)) {
            log.debug("System user already exists");
            return;
        }

        User systemUser = User.builder()
                .id(SYSTEM_USER_ID)
                .email(SYSTEM_USER_EMAIL)
                .displayName("System")
                .build();

        userRepository.save(systemUser);
        log.info("Created system user: {}", SYSTEM_USER_ID);
    }

    private void createDefaultWorkspaceIfNotExists() {
        if (workspaceRepository.existsById(DEFAULT_WORKSPACE_ID)) {
            log.debug("Default workspace already exists");
            return;
        }

        Workspace workspace = Workspace.builder()
                .id(DEFAULT_WORKSPACE_ID)
                .ownerUserId(SYSTEM_USER_ID)
                .name(DEFAULT_WORKSPACE_NAME)
                .build();

        workspaceRepository.save(workspace);
        log.info("Created default workspace: {}", DEFAULT_WORKSPACE_ID);
    }
}
