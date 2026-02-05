package com.portfolio.portfolio.service;

import com.portfolio.TestConfig;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
@DisplayName("PortfolioService 테스트")
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private static final String TEST_WORKSPACE_ID = "test-workspace";

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();
    }

    @Test
    @DisplayName("포트폴리오 생성 성공")
    void create_Success() {
        // Given
        String name = "Test Portfolio";
        String description = "Test Description";
        String baseCurrency = "KRW";
        Portfolio.PortfolioType type = Portfolio.PortfolioType.REAL;

        // When
        Portfolio portfolio = portfolioService.create(
            TEST_WORKSPACE_ID, name, description, baseCurrency, type
        );

        // Then
        assertThat(portfolio).isNotNull();
        assertThat(portfolio.getId()).isNotNull();
        assertThat(portfolio.getName()).isEqualTo(name);
        assertThat(portfolio.getDescription()).isEqualTo(description);
        assertThat(portfolio.getBaseCurrency()).isEqualTo(baseCurrency);
        assertThat(portfolio.getType()).isEqualTo(type);
        assertThat(portfolio.getWorkspaceId()).isEqualTo(TEST_WORKSPACE_ID);
        assertThat(portfolio.getCreatedAt()).isNotNull();
        assertThat(portfolio.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("중복된 이름으로 포트폴리오 생성 시 예외 발생")
    void create_DuplicateName_ThrowsException() {
        // Given
        String name = "Test Portfolio";
        portfolioService.create(TEST_WORKSPACE_ID, name, "Desc 1", "KRW", Portfolio.PortfolioType.REAL);

        // When & Then
        assertThatThrownBy(() -> 
            portfolioService.create(TEST_WORKSPACE_ID, name, "Desc 2", "USD", Portfolio.PortfolioType.REAL)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("포트폴리오 목록 조회")
    void findAll_Success() {
        // Given
        portfolioService.create(TEST_WORKSPACE_ID, "Portfolio 1", "Desc 1", "KRW", Portfolio.PortfolioType.REAL);
        portfolioService.create(TEST_WORKSPACE_ID, "Portfolio 2", "Desc 2", "USD", Portfolio.PortfolioType.HYPOTHETICAL);

        // When
        List<Portfolio> portfolios = portfolioService.findAll(TEST_WORKSPACE_ID);

        // Then
        assertThat(portfolios).hasSize(2);
        assertThat(portfolios).extracting("name")
            .containsExactlyInAnyOrder("Portfolio 1", "Portfolio 2");
    }

    @Test
    @DisplayName("포트폴리오 상세 조회 성공")
    void findById_Success() {
        // Given
        Portfolio created = portfolioService.create(
            TEST_WORKSPACE_ID, "Test Portfolio", "Desc", "KRW", Portfolio.PortfolioType.REAL
        );

        // When
        Portfolio found = portfolioService.findById(created.getId(), TEST_WORKSPACE_ID);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Test Portfolio");
    }

    @Test
    @DisplayName("존재하지 않는 포트폴리오 조회 시 예외 발생")
    void findById_NotFound_ThrowsException() {
        // Given
        String nonExistentId = "non-existent-id";

        // When & Then
        assertThatThrownBy(() -> 
            portfolioService.findById(nonExistentId, TEST_WORKSPACE_ID)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Portfolio not found");
    }

    @Test
    @DisplayName("포트폴리오 업데이트 성공")
    void update_Success() {
        // Given
        Portfolio portfolio = portfolioService.create(
            TEST_WORKSPACE_ID, "Original Name", "Original Desc", "KRW", Portfolio.PortfolioType.REAL
        );
        String newName = "Updated Name";
        String newDescription = "Updated Description";

        // When
        Portfolio updated = portfolioService.update(portfolio.getId(), TEST_WORKSPACE_ID, newName, newDescription);

        // Then
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getDescription()).isEqualTo(newDescription);
        assertThat(updated.getUpdatedAt()).isAfter(portfolio.getUpdatedAt());
    }

    @Test
    @DisplayName("포트폴리오 삭제(아카이브) 성공")
    void delete_Success() {
        // Given
        Portfolio portfolio = portfolioService.create(
            TEST_WORKSPACE_ID, "Test Portfolio", "Desc", "KRW", Portfolio.PortfolioType.REAL
        );

        // When
        portfolioService.delete(portfolio.getId(), TEST_WORKSPACE_ID);

        // Then
        Portfolio archived = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(archived.getArchivedAt()).isNotNull();
    }
}
