<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { usePortfolioStore, useValuationStore } from '@/stores';
import PortfolioCard from '@/components/portfolio/PortfolioCard.vue';
import { formatCurrency, formatPercent } from '@/utils/format';
import { useI18n } from 'vue-i18n';

const router = useRouter();
const portfolioStore = usePortfolioStore();
const valuationStore = useValuationStore();
const { t } = useI18n();

const totalValue = computed(() => {
  return Object.values(valuationStore.valuations).reduce(
    (sum, v) => sum + v.totalValueBase,
    0
  );
});

const totalDayPnl = computed(() => {
  return Object.values(valuationStore.valuations).reduce(
    (sum, v) => sum + v.dayPnlBase,
    0
  );
});

const totalPnl = computed(() => {
  return Object.values(valuationStore.valuations).reduce(
    (sum, v) => sum + v.totalPnlBase,
    0
  );
});

onMounted(async () => {
  await portfolioStore.fetchPortfolios();

  // Fetch valuations for all portfolios
  for (const portfolio of portfolioStore.portfolios) {
    try {
      await valuationStore.fetchValuation(portfolio.id);
    } catch {
      // Ignore errors for individual portfolios
    }
  }
});

function navigateToPortfolio(id: string) {
  router.push(`/portfolio/${id}`);
}
</script>

<template>
  <div class="dashboard">
    <!-- Summary Section -->
    <section class="summary-section">
      <div class="summary-cards">
        <div class="summary-card">
          <span class="summary-label">{{ t('dashboard.totalAssets') }}</span>
          <span class="summary-value">{{ formatCurrency(totalValue, 'KRW') }}</span>
        </div>
        <div class="summary-card">
          <span class="summary-label">{{ t('dashboard.todayPnl') }}</span>
          <span
            class="summary-value"
            :class="{ 'number-positive': totalDayPnl > 0, 'number-negative': totalDayPnl < 0 }"
          >
            {{ formatCurrency(totalDayPnl, 'KRW', true) }}
          </span>
        </div>
        <div class="summary-card">
          <span class="summary-label">{{ t('dashboard.totalPnl') }}</span>
          <span
            class="summary-value"
            :class="{ 'number-positive': totalPnl > 0, 'number-negative': totalPnl < 0 }"
          >
            {{ formatCurrency(totalPnl, 'KRW', true) }}
          </span>
        </div>
      </div>
    </section>

    <!-- Portfolios Section -->
    <section class="portfolios-section">
      <div class="section-header">
        <h2>{{ t('dashboard.portfolios') }}</h2>
        <RouterLink to="/portfolio/new" class="btn btn-primary">
          + {{ t('dashboard.newPortfolio') }}
        </RouterLink>
      </div>

      <div v-if="portfolioStore.loading" class="loading-state">
        <div class="spinner"></div>
        <span>{{ t('dashboard.loadingPortfolios') }}</span>
      </div>

      <div v-else-if="portfolioStore.portfolios.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
            <polyline points="3.27,6.96 12,12.01 20.73,6.96" />
            <line x1="12" y1="22.08" x2="12" y2="12" />
          </svg>
        </div>
        <h3>{{ t('dashboard.noPortfoliosYet') }}</h3>
        <p>{{ t('dashboard.createFirstPortfolio') }}</p>
        <RouterLink to="/portfolio/new" class="btn btn-primary">
          {{ t('dashboard.createPortfolio') }}
        </RouterLink>
      </div>

      <div v-else class="portfolios-grid">
        <PortfolioCard
          v-for="portfolio in portfolioStore.portfolios"
          :key="portfolio.id"
          :portfolio="portfolio"
          :valuation="valuationStore.getValuation(portfolio.id)"
          @click="navigateToPortfolio(portfolio.id)"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.summary-section {
  margin-bottom: 32px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.summary-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  font-size: 13px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-value {
  font-size: 1.5rem;
  font-weight: 600;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 1.25rem;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  text-align: center;
}

.loading-state {
  gap: 16px;
  color: var(--text-secondary);
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 1.125rem;
  margin-bottom: 8px;
}

.empty-state p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.portfolios-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
