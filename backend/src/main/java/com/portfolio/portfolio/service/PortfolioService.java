package com.portfolio.portfolio.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.entity.PortfolioTarget;
import com.portfolio.portfolio.repository.PortfolioRepository;
import com.portfolio.portfolio.repository.PortfolioTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioTargetRepository targetRepository;

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
    
    // ===== Target Management =====
    
    @Transactional(readOnly = true)
    public List<PortfolioTarget> getTargets(String portfolioId, String workspaceId) {
        // 권한 확인
        findById(portfolioId, workspaceId);
        return targetRepository.findByPortfolioId(portfolioId);
    }
    
    @Transactional
    public List<PortfolioTarget> updateTargets(String portfolioId, String workspaceId, 
                                               List<PortfolioTarget> targets, boolean normalize) {
        Portfolio portfolio = findById(portfolioId, workspaceId);
        
        // 기존 타겟 삭제
        targetRepository.deleteByPortfolioId(portfolioId);
        
        // 비중 합계 검증
        BigDecimal totalWeight = targets.stream()
                .map(PortfolioTarget::getTargetWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Normalize 옵션이 true면 자동으로 비율 조정
        if (normalize && totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal finalTotalWeight = totalWeight;
            targets.forEach(target -> {
                BigDecimal normalizedWeight = target.getTargetWeight()
                        .divide(finalTotalWeight, 4, BigDecimal.ROUND_HALF_UP);
                target.setTargetWeight(normalizedWeight);
            });
            totalWeight = BigDecimal.ONE;
        }
        
        // 비중 합계가 1.0이 아니면 에러
        BigDecimal tolerance = new BigDecimal("0.0005"); // ±0.05% 허용
        if (totalWeight.subtract(BigDecimal.ONE).abs().compareTo(tolerance) > 0) {
            throw new BusinessException(ErrorCode.INVALID_TARGET_WEIGHTS);
        }
        
        // 새 타겟 저장
        targets.forEach(target -> {
            target.setPortfolio(portfolio);
            targetRepository.save(target);
        });
        
        return targetRepository.findByPortfolioId(portfolioId);
    }
}
