<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { usePortfolioStore, useValuationStore } from '@/stores';
import type { MetricType } from '@/types';
import { formatPercent, formatNumber, getChangeClass } from '@/utils/format';
import { useI18n } from 'vue-i18n';

// ECharts
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import {
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';

use([
  CanvasRenderer,
  LineChart,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
]);

const portfolioStore = usePortfolioStore();
const valuationStore = useValuationStore();
const { t } = useI18n();

const selectedPortfolios = ref<string[]>([]);
const dateRange = ref({ from: '', to: '' });
const metric = ref<MetricType>('TWR');
const loading = ref(false);
const compared = ref(false);

// Set default date range (6 months)
const today = new Date();
const sixMonthsAgo = new Date(today);
sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
dateRange.value = {
  from: sixMonthsAgo.toISOString().split('T')[0],
  to: today.toISOString().split('T')[0],
};

const metrics: { value: MetricType; label: string }[] = [
  { value: 'TWR', label: 'Time-Weighted (TWR)' },
  { value: 'SIMPLE', label: 'Simple Return' },
];

// 차트 색상 팔레트
const COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6'];

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
  compared.value = false;
  try {
    await valuationStore.compare({
      portfolioIds: selectedPortfolios.value,
      from: dateRange.value.from,
      to: dateRange.value.to,
      metric: metric.value,
      currencyMode: 'BASE',
    });
    compared.value = true;
  } catch (e) {
    console.error('Comparison failed:', e);
  } finally {
    loading.value = false;
  }
}

const chartOption = computed(() => {
  if (!valuationStore.compareResult) return null;

  const curves = valuationStore.compareResult.curves;
  if (!curves || curves.length === 0) return null;

  // 모든 곡선에서 날짜 합집합
  const allDates = new Set<string>();
  curves.forEach((curve: any) => {
    if (curve.points) {
      curve.points.forEach((p: any) => allDates.add(p.date));
    }
  });
  const sortedDates = Array.from(allDates).sort();

  const series = curves.map((curve: any, idx: number) => {
    const pointMap = new Map<string, number>();
    if (curve.points) {
      curve.points.forEach((p: any) => pointMap.set(p.date, p.value));
    }

    const data = sortedDates.map(d => {
      const val = pointMap.get(d);
      return val !== undefined ? +(val * 100).toFixed(2) : null;
    });

    return {
      name: curve.label,
      type: 'line' as const,
      data,
      smooth: true,
      symbol: 'none',
      lineStyle: { width: 2.5, color: COLORS[idx % COLORS.length] },
      itemStyle: { color: COLORS[idx % COLORS.length] },
    };
  });

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        let html = `<div style="font-size:12px;color:#888;margin-bottom:6px">${params[0]?.axisValue}</div>`;
        params.forEach((p: any) => {
          if (p.value != null) {
            const sign = p.value >= 0 ? '+' : '';
            const color = p.value >= 0 ? '#22c55e' : '#ef4444';
            html += `<div style="display:flex;align-items:center;gap:8px;margin:3px 0">
              <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color}"></span>
              <span style="flex:1">${p.seriesName}</span>
              <span style="font-weight:600;color:${color}">${sign}${p.value}%</span>
            </div>`;
          }
        });
        return html;
      },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#1f2937' },
    },
    legend: {
      data: curves.map((c: any) => c.label),
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 12 },
    },
    grid: {
      left: 60,
      right: 20,
      top: 20,
      bottom: 50,
    },
    xAxis: {
      type: 'category',
      data: sortedDates,
      axisLabel: {
        formatter: (val: string) => {
          const d = new Date(val);
          return `${d.getMonth() + 1}/${d.getDate()}`;
        },
        color: '#9ca3af',
        fontSize: 11,
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (val: number) => `${val}%`,
        color: '#9ca3af',
        fontSize: 11,
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
    },
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
    series,
  };
});

const statsTable = computed(() => {
  if (!valuationStore.compareResult) return [];
  return valuationStore.compareResult.statsTable || [];
});
</script>

<template>
  <div class="compare-view">
    <header class="page-header">
      <h1>{{ t('compare.title') }}</h1>
      <p>{{ t('compare.subtitle') }}</p>
    </header>

    <div class="compare-layout">
      <!-- Selection Panel -->
      <aside class="selection-panel card">
        <h3>{{ t('compare.selectPortfolios') }}</h3>
        <p class="hint">{{ t('compare.selectHint') }}</p>

        <div v-if="portfolioStore.loading" class="loading-small">
          <div class="spinner"></div>
        </div>

        <div v-else class="portfolio-list">
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

          <p v-if="portfolioStore.portfolios.length === 0" class="empty-hint">
            {{ t('compare.noPortfolios') }}
          </p>
        </div>

        <div class="controls">
          <div class="form-group">
            <label class="form-label">{{ t('compare.dateRange') }}</label>
            <div class="date-inputs">
              <input type="date" v-model="dateRange.from" class="form-input" />
              <span>~</span>
              <input type="date" v-model="dateRange.to" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">{{ t('compare.metric') }}</label>
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
            <span v-else>{{ t('compare.run') }}</span>
          </button>
        </div>
      </aside>

      <!-- Results Panel -->
      <main class="results-panel">
        <!-- Placeholder -->
        <div v-if="!compared" class="placeholder card">
          <div class="placeholder-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="18" y1="20" x2="18" y2="10" />
              <line x1="12" y1="20" x2="12" y2="4" />
              <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
          </div>
          <h3>{{ t('compare.selectPrompt') }}</h3>
          <p>{{ t('compare.selectDescription') }}</p>
        </div>

        <template v-else>
          <!-- Chart -->
          <div class="chart-card card">
            <h3>{{ t('compare.cumulativeReturns') }}</h3>
            <div v-if="chartOption" class="chart-container">
              <VChart :option="chartOption" autoresize style="height: 400px" />
            </div>
            <div v-else class="chart-empty">
              <p>{{ t('compare.noData') }}</p>
            </div>
          </div>

          <!-- Stats Table -->
          <div v-if="statsTable.length > 0" class="stats-card card">
            <h3>{{ t('compare.performanceMetrics') }}</h3>
            <div class="table-wrapper">
              <table class="table compare-table">
                <thead>
                  <tr>
                    <th>{{ t('compare.portfolio') }}</th>
                    <th class="text-right">{{ t('performance.totalReturn') }}</th>
                    <th class="text-right">{{ t('performance.cagr') }}</th>
                    <th class="text-right">{{ t('performance.volatility') }}</th>
                    <th class="text-right">{{ t('performance.mdd') }}</th>
                    <th class="text-right">{{ t('performance.sharpe') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(stat, idx) in statsTable" :key="stat.id">
                    <td>
                      <div class="portfolio-label">
                        <span class="color-dot" :style="{ background: COLORS[idx % COLORS.length] }"></span>
                        {{ stat.label }}
                      </div>
                    </td>
                    <td class="text-right font-mono" :class="getChangeClass(stat.totalReturn || 0)">
                      {{ stat.totalReturn != null ? formatPercent(stat.totalReturn, 2, true) : '-' }}
                    </td>
                    <td class="text-right font-mono" :class="getChangeClass(stat.cagr || 0)">
                      {{ stat.cagr != null ? formatPercent(stat.cagr, 2, true) : '-' }}
                    </td>
                    <td class="text-right font-mono">
                      {{ stat.vol != null ? formatPercent(stat.vol, 2) : '-' }}
                    </td>
                    <td class="text-right font-mono number-negative">
                      {{ stat.mdd != null && stat.mdd > 0 ? '-' + formatPercent(stat.mdd, 2) : '-' }}
                    </td>
                    <td class="text-right font-mono">
                      {{ stat.sharpe != null ? formatNumber(stat.sharpe, 2) : '-' }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
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

.loading-small {
  display: flex;
  justify-content: center;
  padding: 20px;
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

.empty-hint {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 20px;
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

.chart-container {
  border-radius: var(--radius-md);
}

.chart-empty {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.table-wrapper {
  overflow-x: auto;
}

.compare-table th,
.compare-table td {
  padding: 14px 16px;
}

.text-right {
  text-align: right;
}

.font-mono {
  font-family: var(--font-mono);
  font-size: 13px;
}

.portfolio-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .compare-layout {
    grid-template-columns: 1fr;
  }
}
</style>
