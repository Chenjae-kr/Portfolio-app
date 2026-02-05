package com.portfolio.portfolio.service;

import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    @Transactional(readOnly = true)
    public List<Portfolio> findAll(String workspaceId) {
        return portfolioRepository.findByWorkspaceIdAndArchivedAtIsNullOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public Portfolio findById(String id, String workspaceId) {
        return portfolioRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + id));
    }

    @Transactional
    public Portfolio create(String workspaceId, String name, String description, 
                           String baseCurrency, Portfolio.PortfolioType type) {
        // 이름 중복 체크
        if (portfolioRepository.existsByNameAndWorkspaceIdAndArchivedAtIsNull(name, workspaceId)) {
            throw new IllegalArgumentException("Portfolio with name '" + name + "' already exists");
        }

        Portfolio portfolio = Portfolio.builder()
                .workspaceId(workspaceId)
                .name(name)
                .description(description)
                .baseCurrency(baseCurrency)
                .type(type)
                .build();

        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public Portfolio update(String id, String workspaceId, String name, String description) {
        Portfolio portfolio = findById(id, workspaceId);
        
        if (name != null) {
            portfolio.setName(name);
        }
        if (description != null) {
            portfolio.setDescription(description);
        }
        
        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public void delete(String id, String workspaceId) {
        Portfolio portfolio = findById(id, workspaceId);
        portfolio.setArchivedAt(java.time.LocalDateTime.now());
        portfolioRepository.save(portfolio);
    }
}
