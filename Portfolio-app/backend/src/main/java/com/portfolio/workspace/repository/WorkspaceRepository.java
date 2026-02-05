package com.portfolio.workspace.repository;

import com.portfolio.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {

    List<Workspace> findByOwnerUserId(String ownerUserId);

    Optional<Workspace> findByOwnerUserIdAndName(String ownerUserId, String name);

    boolean existsByOwnerUserIdAndName(String ownerUserId, String name);
}
