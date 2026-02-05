package com.portfolio.portfolio.repository;

import com.portfolio.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByWorkspaceIdAndArchivedAtIsNullOrderByCreatedAtDesc(String workspaceId);

    List<Portfolio> findByWorkspaceIdAndGroupIdAndArchivedAtIsNull(String workspaceId, String groupId);

    Optional<Portfolio> findByIdAndWorkspaceId(String id, String workspaceId);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.targets WHERE p.id = :id")
    Optional<Portfolio> findByIdWithTargets(@Param("id") String id);

    boolean existsByNameAndWorkspaceIdAndArchivedAtIsNull(String name, String workspaceId);
}
