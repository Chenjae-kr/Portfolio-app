<script setup lang="ts">
import type { Portfolio, Valuation } from '@/types';
import { formatCurrency, formatPercent, getChangeClass } from '@/utils/format';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

interface Props {
  portfolio: Portfolio;
  valuation?: Valuation;
}

const props = defineProps<Props>();
const { t } = useI18n();

const returnPercent = computed(() => {
  if (!props.valuation) return 0;
  const { totalValueBase, totalPnlBase } = props.valuation;
  const costBasis = totalValueBase - totalPnlBase;
  return costBasis > 0 ? totalPnlBase / costBasis : 0;
});

const dayReturnPercent = computed(() => {
  if (!props.valuation) return 0;
  const { totalValueBase, dayPnlBase } = props.valuation;
  const previousValue = totalValueBase - dayPnlBase;
  return previousValue > 0 ? dayPnlBase / previousValue : 0;
});
</script>

<template>
  <div class="portfolio-card" @click="$emit('click')">
    <div class="card-header">
      <div class="portfolio-info">
        <h3 class="portfolio-name">{{ portfolio.name }}</h3>
        <span class="portfolio-type badge">{{ portfolio.type }}</span>
      </div>
      <span class="currency-badge">{{ portfolio.baseCurrency }}</span>
    </div>

    <div v-if="valuation" class="card-body">
      <div class="total-value">
        <span class="label">{{ t('portfolio.totalValue') }}</span>
        <span class="value">{{ formatCurrency(valuation.totalValueBase, portfolio.baseCurrency) }}</span>
      </div>

      <div class="metrics">
        <div class="metric">
          <span class="metric-label">{{ t('portfolio.today') }}</span>
          <span class="metric-value" :class="getChangeClass(valuation.dayPnlBase)">
            {{ formatCurrency(valuation.dayPnlBase, portfolio.baseCurrency, true) }}
            <small>({{ formatPercent(dayReturnPercent, 2, true) }})</small>
          </span>
        </div>
        <div class="metric">
          <span class="metric-label">{{ t('dashboard.totalPnl') }}</span>
          <span class="metric-value" :class="getChangeClass(valuation.totalPnlBase)">
            {{ formatCurrency(valuation.totalPnlBase, portfolio.baseCurrency, true) }}
            <small>({{ formatPercent(returnPercent, 2, true) }})</small>
          </span>
        </div>
      </div>

      <div class="positions-count">
        {{ t('portfolio.positionsCount', { count: valuation.positions.length }) }}
      </div>
    </div>

    <div v-else class="card-body loading">
      <div class="skeleton skeleton-lg"></div>
      <div class="skeleton skeleton-md"></div>
    </div>
  </div>
</template>

<style scoped>
.portfolio-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.portfolio-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.portfolio-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.portfolio-name {
  font-size: 1rem;
  font-weight: 600;
}

.portfolio-type {
  font-size: 11px;
  padding: 2px 8px;
  background-color: var(--bg-hover);
  color: var(--text-secondary);
}

.currency-badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-body.loading {
  min-height: 120px;
}

.total-value {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.total-value .label {
  font-size: 12px;
  color: var(--text-secondary);
}

.total-value .value {
  font-size: 1.5rem;
  font-weight: 600;
}

.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.metric-value {
  font-size: 14px;
  font-weight: 500;
}

.metric-value small {
  font-weight: 400;
  opacity: 0.8;
}

.positions-count {
  font-size: 13px;
  color: var(--text-muted);
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.skeleton {
  background: linear-gradient(90deg, var(--bg-hover) 25%, var(--border-color) 50%, var(--bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}

.skeleton-lg {
  height: 32px;
  width: 60%;
}

.skeleton-md {
  height: 20px;
  width: 80%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
