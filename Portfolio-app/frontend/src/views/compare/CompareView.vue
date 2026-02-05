<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { usePortfolioStore, useValuationStore } from '@/stores';
import type { MetricType } from '@/types';
import { formatPercent, formatNumber } from '@/utils/format';

const portfolioStore = usePortfolioStore();
const valuationStore = useValuationStore();

const selectedPortfolios = ref<string[]>([]);
const dateRange = ref({ from: '', to: '' });
const metric = ref<MetricType>('TWR');
const loading = ref(false);

// Set default date range (1 year)
const today = new Date();
const oneYearAgo = new Date(today);
oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
dateRange.value = {
  from: oneYearAgo.toISOString().split('T')[0],
  to: today.toISOString().split('T')[0],
};

const metrics: { value: MetricType; label: string }[] = [
  { value: 'TWR', label: 'Time-Weighted (TWR)' },
  { value: 'MWR', label: 'Money-Weighted (MWR)' },
  { value: 'SIMPLE', label: 'Simple Return' },
];

onMounted(() => {
  portfolioStore.fetchPortfolios();
});

function togglePortfolio(id: string) {
  const index = selectedPortfolios.value.indexOf(id);
  if (index === -1) {
    if (selectedPortfolios.value.length < 5) {
      selectedPortfolios.value.push(id);
    }
  } else {
    selectedPortfolios.value.splice(index, 1);
  }
}

async function runComparison() {
  if (selectedPortfolios.value.length < 2) return;

  loading.value = true;
  try {
    await valuationStore.compare({
      portfolioIds: selectedPortfolios.value,
      from: dateRange.value.from,
      to: dateRange.value.to,
      metric: metric.value,
      currencyMode: 'BASE',
    });
  } catch (e) {
    console.error('Comparison failed:', e);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="compare-view">
    <header class="page-header">
      <h1>Compare Portfolios</h1>
      <p>Analyze and compare performance across multiple portfolios</p>
    </header>

    <div class="compare-layout">
      <!-- Selection Panel -->
      <aside class="selection-panel card">
        <h3>Select Portfolios</h3>
        <p class="hint">Select 2-5 portfolios to compare</p>

        <div class="portfolio-list">
          <label
            v-for="portfolio in portfolioStore.portfolios"
            :key="portfolio.id"
            class="portfolio-checkbox"
            :class="{ selected: selectedPortfolios.includes(portfolio.id) }"
          >
            <input
              type="checkbox"
              :checked="selectedPortfolios.includes(portfolio.id)"
              :disabled="!selectedPortfolios.includes(portfolio.id) && selectedPortfolios.length >= 5"
              @change="togglePortfolio(portfolio.id)"
            />
            <span class="portfolio-name">{{ portfolio.name }}</span>
            <span class="portfolio-currency">{{ portfolio.baseCurrency }}</span>
          </label>
        </div>

        <div class="controls">
          <div class="form-group">
            <label class="form-label">Date Range</label>
            <div class="date-inputs">
              <input type="date" v-model="dateRange.from" class="form-input" />
              <span>to</span>
              <input type="date" v-model="dateRange.to" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Return Metric</label>
            <select v-model="metric" class="form-input">
              <option v-for="m in metrics" :key="m.value" :value="m.value">
                {{ m.label }}
              </option>
            </select>
          </div>

          <button
            class="btn btn-primary btn-full"
            :disabled="selectedPortfolios.length < 2 || loading"
            @click="runComparison"
          >
            <span v-if="loading" class="spinner"></span>
            <span v-else>Compare</span>
          </button>
        </div>
      </aside>

      <!-- Results Panel -->
      <main class="results-panel">
        <div v-if="!valuationStore.compareResult" class="placeholder card">
          <div class="placeholder-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="18" y1="20" x2="18" y2="10" />
              <line x1="12" y1="20" x2="12" y2="4" />
              <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
          </div>
          <h3>Select portfolios to compare</h3>
          <p>Choose at least 2 portfolios from the left panel</p>
        </div>

        <template v-else>
          <!-- Chart placeholder -->
          <div class="chart-card card">
            <h3>Cumulative Returns</h3>
            <div class="chart-placeholder">
              <p>Chart visualization coming soon...</p>
              <p class="hint">ECharts integration pending</p>
            </div>
          </div>

          <!-- Stats Table -->
          <div class="stats-card card">
            <h3>Performance Metrics</h3>
            <table class="table">
              <thead>
                <tr>
                  <th>Portfolio</th>
                  <th class="text-right">CAGR</th>
                  <th class="text-right">Volatility</th>
                  <th class="text-right">MDD</th>
                  <th class="text-right">Sharpe</th>
                  <th class="text-right">Beta</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="stat in valuationStore.compareResult.statsTable" :key="stat.id">
                  <td>{{ stat.label }}</td>
                  <td class="text-right">{{ stat.cagr ? formatPercent(stat.cagr) : '-' }}</td>
                  <td class="text-right">{{ stat.vol ? formatPercent(stat.vol) : '-' }}</td>
                  <td class="text-right">{{ stat.mdd ? formatPercent(stat.mdd) : '-' }}</td>
                  <td class="text-right">{{ stat.sharpe ? formatNumber(stat.sharpe, 2) : '-' }}</td>
                  <td class="text-right">{{ stat.beta ? formatNumber(stat.beta, 2) : '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.compare-view {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin-bottom: 8px;
}

.page-header p {
  color: var(--text-secondary);
}

.compare-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
}

.selection-panel h3 {
  margin-bottom: 4px;
}

.hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.portfolio-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
  max-height: 300px;
  overflow-y: auto;
}

.portfolio-checkbox {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.portfolio-checkbox:hover {
  border-color: var(--primary-color);
}

.portfolio-checkbox.selected {
  border-color: var(--primary-color);
  background-color: var(--primary-light);
}

.portfolio-checkbox input {
  accent-color: var(--primary-color);
}

.portfolio-name {
  flex: 1;
  font-weight: 500;
}

.portfolio-currency {
  font-size: 12px;
  color: var(--text-muted);
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-inputs input {
  flex: 1;
}

.date-inputs span {
  color: var(--text-muted);
}

.btn-full {
  width: 100%;
}

.results-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;
}

.placeholder-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.placeholder h3 {
  margin-bottom: 8px;
}

.placeholder p {
  color: var(--text-secondary);
}

.chart-card h3,
.stats-card h3 {
  margin-bottom: 16px;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-hover);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
}

.text-right {
  text-align: right;
}

@media (max-width: 900px) {
  .compare-layout {
    grid-template-columns: 1fr;
  }
}
</style>
