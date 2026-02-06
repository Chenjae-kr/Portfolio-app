<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { valuationApi } from '@/api';
import type { RebalanceAnalysis } from '@/types';
import { formatCurrency, formatPercent, getChangeClass } from '@/utils/format';
import { useI18n } from 'vue-i18n';

// ECharts
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import {
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent, TitleComponent]);

const props = defineProps<{
  portfolioId: string;
  baseCurrency: string;
}>();

const { t } = useI18n();
const loading = ref(false);
const error = ref('');
const analysis = ref<RebalanceAnalysis | null>(null);

const COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16'];

async function fetchAnalysis() {
  loading.value = true;
  error.value = '';
  try {
    analysis.value = await valuationApi.getRebalance(props.portfolioId);
  } catch (e: unknown) {
    error.value = (e as Error).message || 'Failed to load rebalance data';
  } finally {
    loading.value = false;
  }
}

onMounted(fetchAnalysis);
watch(() => props.portfolioId, fetchAnalysis);

const currentPieOption = computed(() => {
  if (!analysis.value?.comparisons?.length) return null;

  const data = analysis.value.comparisons
    .filter(c => c.currentWeight > 0.001)
    .map((c, i) => ({
      name: c.instrumentName || c.instrumentId,
      value: +(c.currentWeight * 100).toFixed(1),
      itemStyle: { color: COLORS[i % COLORS.length] },
    }));

  // Add cash if significant
  if (analysis.value.cashWeight > 0.01) {
    data.push({
      name: t('portfolio.cash'),
      value: +(analysis.value.cashWeight * 100).toFixed(1),
      itemStyle: { color: '#d1d5db' },
    });
  }

  return {
    title: {
      text: t('rebalance.currentAllocation'),
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' },
    },
    tooltip: {
      formatter: (p: any) => `${p.name}: ${p.value}%`,
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      label: { show: false },
      data,
    }],
  };
});

const targetPieOption = computed(() => {
  if (!analysis.value?.comparisons?.length) return null;

  const data = analysis.value.comparisons
    .filter(c => c.targetWeight > 0.001)
    .map((c, i) => ({
      name: c.instrumentName || c.instrumentId,
      value: +(c.targetWeight * 100).toFixed(1),
      itemStyle: { color: COLORS[i % COLORS.length] },
    }));

  return {
    title: {
      text: t('rebalance.targetAllocation'),
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' },
    },
    tooltip: {
      formatter: (p: any) => `${p.name}: ${p.value}%`,
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      label: { show: false },
      data,
    }],
  };
});

const driftStatus = computed(() => {
  if (!analysis.value) return 'none';
  const drift = analysis.value.maxDrift;
  if (drift < 0.02) return 'good';
  if (drift < 0.05) return 'warning';
  return 'danger';
});
</script>

<template>
  <div class="rebalance-panel">
    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>{{ t('common.loading') }}</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-state card">
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="fetchAnalysis">{{ t('common.retry') }}</button>
    </div>

    <!-- No targets set -->
    <div v-else-if="analysis && analysis.comparisons.length === 0" class="empty-state card">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 8v4M12 16h.01" />
        </svg>
      </div>
      <h3>{{ t('rebalance.noTargets') }}</h3>
      <p>{{ t('rebalance.noTargetsHint') }}</p>
    </div>

    <!-- Analysis results -->
    <template v-else-if="analysis">
      <!-- Status Banner -->
      <div class="status-banner" :class="'status-' + driftStatus">
        <div class="status-content">
          <span class="status-icon" v-if="driftStatus === 'good'">&#10003;</span>
          <span class="status-icon" v-else-if="driftStatus === 'warning'">&#9888;</span>
          <span class="status-icon" v-else>&#10007;</span>
          <div>
            <strong>
              {{ analysis.needsRebalancing ? t('rebalance.needsRebalancing') : t('rebalance.balanced') }}
            </strong>
            <span class="status-detail">
              {{ t('rebalance.maxDrift') }}: {{ formatPercent(analysis.maxDrift) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Donut Charts -->
      <div class="charts-row">
        <div class="chart-card card">
          <VChart v-if="currentPieOption" :option="currentPieOption" autoresize style="height: 260px" />
        </div>
        <div class="chart-card card">
          <VChart v-if="targetPieOption" :option="targetPieOption" autoresize style="height: 260px" />
        </div>
      </div>

      <!-- Weight Comparison Table -->
      <div class="comparison-card card">
        <h3>{{ t('rebalance.weightComparison') }}</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t('rebalance.instrument') }}</th>
                <th class="text-right">{{ t('rebalance.currentWeight') }}</th>
                <th class="text-right">{{ t('rebalance.targetWeight') }}</th>
                <th class="text-right">{{ t('rebalance.drift') }}</th>
                <th class="text-right">{{ t('rebalance.currentValue') }}</th>
                <th class="text-right">{{ t('rebalance.targetValue') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in analysis.comparisons" :key="c.instrumentId">
                <td class="instrument-name">{{ c.instrumentName || c.instrumentId }}</td>
                <td class="text-right font-mono">{{ formatPercent(c.currentWeight) }}</td>
                <td class="text-right font-mono">{{ formatPercent(c.targetWeight) }}</td>
                <td class="text-right font-mono" :class="getChangeClass(c.difference)">
                  {{ formatPercent(c.difference, 2, true) }}
                </td>
                <td class="text-right font-mono">{{ formatCurrency(c.currentValue, baseCurrency) }}</td>
                <td class="text-right font-mono">{{ formatCurrency(c.targetValue, baseCurrency) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Trade Recommendations -->
      <div v-if="analysis.trades.length > 0" class="trades-card card">
        <h3>{{ t('rebalance.tradeRecommendations') }}</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t('rebalance.instrument') }}</th>
                <th>{{ t('rebalance.action') }}</th>
                <th class="text-right">{{ t('rebalance.tradeAmount') }}</th>
                <th class="text-right">{{ t('rebalance.estimatedFee') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="trade in analysis.trades" :key="trade.instrumentId">
                <td class="instrument-name">{{ trade.instrumentName || trade.instrumentId }}</td>
                <td>
                  <span class="badge" :class="trade.action === 'BUY' ? 'badge-success' : 'badge-danger'">
                    {{ trade.action }}
                  </span>
                </td>
                <td class="text-right font-mono">{{ formatCurrency(trade.amount, baseCurrency) }}</td>
                <td class="text-right font-mono text-muted">{{ formatCurrency(trade.estimatedFee, baseCurrency) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="2"><strong>{{ t('rebalance.totalFee') }}</strong></td>
                <td></td>
                <td class="text-right font-mono">
                  <strong>{{ formatCurrency(analysis.totalEstimatedFee, baseCurrency) }}</strong>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.rebalance-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  gap: 12px;
  color: var(--text-secondary);
  text-align: center;
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 8px;
}

.status-banner {
  padding: 14px 20px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
}

.status-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  font-size: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.status-detail {
  display: block;
  font-size: 13px;
  opacity: 0.8;
  margin-top: 2px;
}

.status-good {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.status-good .status-icon {
  background-color: #dcfce7;
  color: #16a34a;
}

.status-warning {
  background-color: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.status-warning .status-icon {
  background-color: #fef3c7;
  color: #d97706;
}

.status-danger {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.status-danger .status-icon {
  background-color: #fee2e2;
  color: #dc2626;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  padding: 16px;
}

.comparison-card h3,
.trades-card h3 {
  margin-bottom: 16px;
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

.text-muted {
  color: var(--text-muted);
}

.instrument-name {
  font-weight: 500;
}

tfoot td {
  border-top: 2px solid var(--border-color);
}

@media (max-width: 700px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
