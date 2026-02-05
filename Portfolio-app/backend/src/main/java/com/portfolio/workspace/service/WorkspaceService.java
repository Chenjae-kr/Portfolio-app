package com.portfolio.workspace.service;

import com.portfolio.workspace.entity.Workspace;
import com.portfolio.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public Workspace createDefaultWorkspace(String userId, String userEmail) {
        // 기본 workspace 이름 생성
        String workspaceName = userEmail + "'s Workspace";
        
        // 중복 체크
        if (workspaceRepository.existsByOwnerUserIdAndName(userId, workspaceName)) {
            // 이미 존재하면 조회해서 반환
            return workspaceRepository.findByOwnerUserIdAndName(userId, workspaceName)
                    .orElseThrow(() -> new IllegalStateException("Workspace state inconsistent"));
        }

        // 새 workspace 생성
        Workspace workspace = Workspace.builder()
                .ownerUserId(userId)
                .name(workspaceName)
                .build();

        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace getDefaultWorkspace(String userId) {
        return workspaceRepository.findByOwnerUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No workspace found for user: " + userId));
    }
}
