package com.portfolio.portfolio.repository;

import com.portfolio.portfolio.entity.PortfolioTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioTargetRepository extends JpaRepository<PortfolioTarget, String> {
    
    List<PortfolioTarget> findByPortfolioId(String portfolioId);
    
    void deleteByPortfolioId(String portfolioId);
    
    boolean existsByPortfolioId(String portfolioId);
}
