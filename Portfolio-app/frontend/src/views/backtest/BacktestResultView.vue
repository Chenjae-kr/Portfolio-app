<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useBacktestStore } from '@/stores';
import { formatCurrency, formatPercent, formatNumber, formatDate } from '@/utils/format';

const route = useRoute();
const router = useRouter();
const backtestStore = useBacktestStore();

const runId = computed(() => route.params.id as string);

onMounted(async () => {
  await backtestStore.fetchResults(runId.value);
});

const result = computed(() => backtestStore.currentResult);
</script>

<template>
  <div class="backtest-result">
    <header class="page-header">
      <button class="btn btn-secondary" @click="router.push('/backtest')">
        &larr; Back to Backtest
      </button>
      <h1>Backtest Results</h1>
    </header>

    <div v-if="backtestStore.loading" class="loading-state">
      <div class="spinner"></div>
      <span>Loading results...</span>
    </div>

    <template v-else-if="result">
      <!-- Summary Cards -->
      <section class="summary-section">
        <div class="summary-cards">
          <div class="summary-card">
            <span class="label">Final Value</span>
            <span class="value">
              {{ result.series.length > 0
                ? formatCurrency(result.series[result.series.length - 1].equityCurveBase, 'KRW')
                : '-' }}
            </span>
          </div>
          <div class="summary-card">
            <span class="label">CAGR</span>
            <span class="value">{{ result.stats.cagr ? formatPercent(result.stats.cagr) : '-' }}</span>
          </div>
          <div class="summary-card">
            <span class="label">Volatility</span>
            <span class="value">{{ result.stats.vol ? formatPercent(result.stats.vol) : '-' }}</span>
          </div>
          <div class="summary-card">
            <span class="label">Max Drawdown</span>
            <span class="value number-negative">{{ result.stats.mdd ? formatPercent(result.stats.mdd) : '-' }}</span>
          </div>
          <div class="summary-card">
            <span class="label">Sharpe Ratio</span>
            <span class="value">{{ result.stats.sharpe ? formatNumber(result.stats.sharpe, 2) : '-' }}</span>
          </div>
        </div>
      </section>

      <!-- Equity Curve Chart Placeholder -->
      <section class="chart-section card">
        <h3>Equity Curve</h3>
        <div class="chart-placeholder">
          <p>Chart visualization coming soon...</p>
          <p class="hint">ECharts integration pending</p>
        </div>
      </section>

      <!-- Trade Logs -->
      <section class="trades-section card">
        <h3>Trade Log</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Instrument</th>
                <th>Action</th>
                <th class="text-right">Quantity</th>
                <th class="text-right">Price</th>
                <th class="text-right">Fee</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(trade, index) in result.tradeLogs.slice(0, 50)" :key="index">
                <td>{{ formatDate(trade.ts) }}</td>
                <td>{{ trade.instrumentId }}</td>
                <td>
                  <span class="badge" :class="trade.action === 'BUY' ? 'badge-success' : 'badge-danger'">
                    {{ trade.action }}
                  </span>
                </td>
                <td class="text-right font-mono">{{ formatNumber(trade.quantity, 4) }}</td>
                <td class="text-right font-mono">{{ formatCurrency(trade.price, 'KRW') }}</td>
                <td class="text-right font-mono">{{ formatCurrency(trade.fee, 'KRW') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="result.tradeLogs.length > 50" class="more-hint">
          Showing 50 of {{ result.tradeLogs.length }} trades
        </p>
      </section>
    </template>

    <div v-else class="error-state card">
      <p>Failed to load backtest results</p>
      <button class="btn btn-primary" @click="router.push('/backtest')">
        Run New Backtest
      </button>
    </div>
  </div>
</template>

<style scoped>
.backtest-result {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  gap: 16px;
  color: var(--text-secondary);
}

.summary-section {
  margin-bottom: 24px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.summary-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
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

.chart-section,
.trades-section {
  margin-bottom: 24px;
}

.chart-section h3,
.trades-section h3 {
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

.hint {
  font-size: 13px;
  color: var(--text-muted);
}

.table-wrapper {
  overflow-x: auto;
}

.text-right {
  text-align: right;
}

.font-mono {
  font-family: var(--font-mono);
  font-size: 13px;
}

.more-hint {
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
}
</style>
