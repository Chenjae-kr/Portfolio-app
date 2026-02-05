<script setup lang="ts">
import { onMounted, computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { usePortfolioStore, useValuationStore } from '@/stores';
import { formatCurrency, formatPercent, getChangeClass, formatQuantity } from '@/utils/format';
import PositionTable from '@/components/portfolio/PositionTable.vue';

const route = useRoute();
const portfolioStore = usePortfolioStore();
const valuationStore = useValuationStore();

const portfolioId = computed(() => route.params.id as string);
const activeTab = ref('positions');

const tabs = [
  { id: 'positions', label: 'Positions' },
  { id: 'performance', label: 'Performance' },
  { id: 'transactions', label: 'Transactions' },
];

const portfolio = computed(() => portfolioStore.currentPortfolio);
const valuation = computed(() => valuationStore.getValuation(portfolioId.value));

const returnPercent = computed(() => {
  if (!valuation.value) return 0;
  const { totalValueBase, totalPnlBase } = valuation.value;
  const costBasis = totalValueBase - totalPnlBase;
  return costBasis > 0 ? totalPnlBase / costBasis : 0;
});

onMounted(async () => {
  await Promise.all([
    portfolioStore.fetchPortfolio(portfolioId.value),
    valuationStore.fetchValuation(portfolioId.value),
  ]);
});
</script>

<template>
  <div class="portfolio-detail">
    <div v-if="portfolioStore.loading" class="loading-state">
      <div class="spinner"></div>
      <span>Loading portfolio...</span>
    </div>

    <template v-else-if="portfolio">
      <!-- Header -->
      <header class="detail-header">
        <div class="header-left">
          <h1>{{ portfolio.name }}</h1>
          <p v-if="portfolio.description" class="description">{{ portfolio.description }}</p>
          <div class="meta">
            <span class="badge">{{ portfolio.type }}</span>
            <span class="currency">{{ portfolio.baseCurrency }}</span>
          </div>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary">Edit</button>
        </div>
      </header>

      <!-- Summary Cards -->
      <section class="summary-section" v-if="valuation">
        <div class="summary-cards">
          <div class="summary-card primary">
            <span class="label">Total Value</span>
            <span class="value">{{ formatCurrency(valuation.totalValueBase, portfolio.baseCurrency) }}</span>
          </div>
          <div class="summary-card">
            <span class="label">Today's P&L</span>
            <span class="value" :class="getChangeClass(valuation.dayPnlBase)">
              {{ formatCurrency(valuation.dayPnlBase, portfolio.baseCurrency, true) }}
            </span>
          </div>
          <div class="summary-card">
            <span class="label">Total P&L</span>
            <span class="value" :class="getChangeClass(valuation.totalPnlBase)">
              {{ formatCurrency(valuation.totalPnlBase, portfolio.baseCurrency, true) }}
              <small>({{ formatPercent(returnPercent, 2, true) }})</small>
            </span>
          </div>
          <div class="summary-card">
            <span class="label">Cash</span>
            <span class="value">{{ formatCurrency(valuation.cashValueBase, portfolio.baseCurrency) }}</span>
          </div>
        </div>
      </section>

      <!-- Tabs -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-button"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Tab Content -->
      <div class="tab-content">
        <template v-if="activeTab === 'positions'">
          <PositionTable
            v-if="valuation"
            :positions="valuation.positions"
            :currency="portfolio.baseCurrency"
          />
        </template>

        <template v-else-if="activeTab === 'performance'">
          <div class="placeholder-content">
            <p>Performance chart coming soon...</p>
          </div>
        </template>

        <template v-else-if="activeTab === 'transactions'">
          <div class="placeholder-content">
            <p>Transactions list coming soon...</p>
          </div>
        </template>
      </div>
    </template>
  </div>
</template>

<style scoped>
.portfolio-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  gap: 16px;
  color: var(--text-secondary);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.detail-header h1 {
  font-size: 1.75rem;
  margin-bottom: 8px;
}

.description {
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.currency {
  font-size: 14px;
  color: var(--text-muted);
}

.summary-section {
  margin-bottom: 32px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.summary-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-card.primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  color: white;
}

.summary-card.primary .label {
  color: rgba(255, 255, 255, 0.8);
}

.summary-card .label {
  font-size: 12px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-card .value {
  font-size: 1.25rem;
  font-weight: 600;
}

.summary-card .value small {
  font-size: 0.875rem;
  font-weight: 400;
  opacity: 0.8;
}

.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.tab-button {
  padding: 12px 20px;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
}

.tab-button:hover {
  color: var(--text-primary);
}

.tab-button.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.tab-content {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.placeholder-content {
  padding: 40px;
  text-align: center;
  color: var(--text-secondary);
}
</style>
