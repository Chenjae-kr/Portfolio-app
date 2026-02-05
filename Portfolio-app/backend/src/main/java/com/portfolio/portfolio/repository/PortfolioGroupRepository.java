package com.portfolio.portfolio.repository;

import com.portfolio.portfolio.entity.PortfolioGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioGroupRepository extends JpaRepository<PortfolioGroup, String> {
    
    List<PortfolioGroup> findByWorkspaceIdOrderBySortOrder(String workspaceId);
    
    Optional<PortfolioGroup> findByIdAndWorkspaceId(String id, String workspaceId);
    
    boolean existsByNameAndWorkspaceId(String name, String workspaceId);
}
