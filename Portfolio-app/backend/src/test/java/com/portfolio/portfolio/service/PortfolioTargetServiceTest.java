package com.portfolio.portfolio.service;

import com.portfolio.TestConfig;
import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.util.AssetClass;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.entity.PortfolioTarget;
import com.portfolio.portfolio.repository.PortfolioRepository;
import com.portfolio.portfolio.repository.PortfolioTargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class PortfolioTargetServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioTargetRepository targetRepository;

    private String workspaceId;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        workspaceId = "test-workspace";
        portfolio = Portfolio.builder()
                .workspaceId(workspaceId)
                .name("Test Portfolio")
                .baseCurrency("KRW")
                .type(Portfolio.PortfolioType.REAL)
                .build();
        portfolio = portfolioRepository.save(portfolio);
    }

    @Test
    @DisplayName("목표 비중 설정 - 합계 100%")
    void updateTargetsValid() {
        // given
        List<PortfolioTarget> targets = new ArrayList<>();
        
        PortfolioTarget target1 = new PortfolioTarget();
        target1.setInstrumentId("inst-1");
        target1.setAssetClass(AssetClass.EQUITY);
        target1.setCurrency("USD");
        target1.setTargetWeight(new BigDecimal("0.6"));
        targets.add(target1);

        PortfolioTarget target2 = new PortfolioTarget();
        target2.setInstrumentId("inst-2");
        target2.setAssetClass(AssetClass.BOND);
        target2.setCurrency("KRW");
        target2.setTargetWeight(new BigDecimal("0.4"));
        targets.add(target2);

        // when
        List<PortfolioTarget> saved = portfolioService.updateTargets(
                portfolio.getId(), workspaceId, targets, false);

        // then
        assertThat(saved).hasSize(2);
        
        BigDecimal totalWeight = saved.stream()
                .map(PortfolioTarget::getTargetWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalWeight).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("목표 비중 설정 - 합계가 100%가 아니면 예외")
    void updateTargetsInvalidSum() {
        // given
        List<PortfolioTarget> targets = new ArrayList<>();
        
        PortfolioTarget target1 = new PortfolioTarget();
        target1.setInstrumentId("inst-1");
        target1.setAssetClass(AssetClass.EQUITY);
        target1.setCurrency("USD");
        target1.setTargetWeight(new BigDecimal("0.6"));
        targets.add(target1);

        PortfolioTarget target2 = new PortfolioTarget();
        target2.setInstrumentId("inst-2");
        target2.setAssetClass(AssetClass.BOND);
        target2.setCurrency("KRW");
        target2.setTargetWeight(new BigDecimal("0.3")); // 합계 0.9

        targets.add(target2);

        // when & then
        assertThatThrownBy(() -> portfolioService.updateTargets(
                portfolio.getId(), workspaceId, targets, false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("목표 비중 설정 - 정규화 옵션")
    void updateTargetsWithNormalize() {
        // given
        List<PortfolioTarget> targets = new ArrayList<>();
        
        PortfolioTarget target1 = new PortfolioTarget();
        target1.setInstrumentId("inst-1");
        target1.setAssetClass(AssetClass.EQUITY);
        target1.setCurrency("USD");
        target1.setTargetWeight(new BigDecimal("60")); // 정규화 전
        targets.add(target1);

        PortfolioTarget target2 = new PortfolioTarget();
        target2.setInstrumentId("inst-2");
        target2.setAssetClass(AssetClass.BOND);
        target2.setCurrency("KRW");
        target2.setTargetWeight(new BigDecimal("40")); // 정규화 전
        targets.add(target2);

        // when
        List<PortfolioTarget> saved = portfolioService.updateTargets(
                portfolio.getId(), workspaceId, targets, true); // normalize=true

        // then
        assertThat(saved).hasSize(2);
        
        BigDecimal totalWeight = saved.stream()
                .map(PortfolioTarget::getTargetWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 정규화 후 합계가 1.0이 되어야 함
        assertThat(totalWeight).isEqualByComparingTo(BigDecimal.ONE);
        
        // 비율이 유지되어야 함 (60:40)
        assertThat(saved.get(0).getTargetWeight()).isEqualByComparingTo(new BigDecimal("0.6"));
        assertThat(saved.get(1).getTargetWeight()).isEqualByComparingTo(new BigDecimal("0.4"));
    }

    @Test
    @DisplayName("목표 비중 조회")
    void getTargets() {
        // given
        List<PortfolioTarget> targets = new ArrayList<>();
        
        PortfolioTarget target = new PortfolioTarget();
        target.setInstrumentId("inst-1");
        target.setAssetClass(AssetClass.EQUITY);
        target.setCurrency("USD");
        target.setTargetWeight(BigDecimal.ONE);
        targets.add(target);

        portfolioService.updateTargets(portfolio.getId(), workspaceId, targets, false);

        // when
        List<PortfolioTarget> retrieved = portfolioService.getTargets(portfolio.getId(), workspaceId);

        // then
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).getInstrumentId()).isEqualTo("inst-1");
    }

    @Test
    @DisplayName("목표 비중 업데이트 - 기존 삭제 후 재생성")
    void updateTargetsReplacesOld() {
        // given - 초기 타겟 설정
        List<PortfolioTarget> initialTargets = new ArrayList<>();
        PortfolioTarget target1 = new PortfolioTarget();
        target1.setInstrumentId("inst-1");
        target1.setAssetClass(AssetClass.EQUITY);
        target1.setCurrency("USD");
        target1.setTargetWeight(BigDecimal.ONE);
        initialTargets.add(target1);
        
        portfolioService.updateTargets(portfolio.getId(), workspaceId, initialTargets, false);

        // when - 새로운 타겟으로 교체
        List<PortfolioTarget> newTargets = new ArrayList<>();
        PortfolioTarget target2 = new PortfolioTarget();
        target2.setInstrumentId("inst-2");
        target2.setAssetClass(AssetClass.BOND);
        target2.setCurrency("KRW");
        target2.setTargetWeight(BigDecimal.ONE);
        newTargets.add(target2);
        
        List<PortfolioTarget> updated = portfolioService.updateTargets(
                portfolio.getId(), workspaceId, newTargets, false);

        // then
        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getInstrumentId()).isEqualTo("inst-2");
        
        // 기존 타겟은 삭제되어야 함
        List<PortfolioTarget> all = targetRepository.findByPortfolioId(portfolio.getId());
        assertThat(all).hasSize(1);
    }
}
