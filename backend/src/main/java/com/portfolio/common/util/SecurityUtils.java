package com.portfolio.common.util;

import com.portfolio.auth.entity.User;
import com.portfolio.auth.repository.UserRepository;
import com.portfolio.workspace.entity.Workspace;
import com.portfolio.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 인증된 사용자 정보 및 workspace 조회 유틸리티
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;

    /**
     * 현재 인증된 사용자의 이메일을 가져옵니다.
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * 현재 인증된 사용자의 User 엔티티를 가져옵니다.
     */
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    /**
     * 현재 인증된 사용자의 기본 workspace ID를 가져옵니다.
     */
    public String getCurrentWorkspaceId() {
        User user = getCurrentUser();
        Workspace workspace = workspaceService.getDefaultWorkspace(user.getId());
        return workspace.getId();
    }
}
