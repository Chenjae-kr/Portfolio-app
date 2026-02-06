<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';
import { useValuationStore } from '@/stores';
import { formatPercent } from '@/utils/format';
import { useI18n } from 'vue-i18n';
import type { PerformanceData } from '@/types';

use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
]);

interface Props {
  portfolioId: string;
  baseCurrency: string;
}

const props = defineProps<Props>();
const valuationStore = useValuationStore();
const { t } = useI18n();

type PeriodKey = '1M' | '3M' | '6M' | 'YTD' | '1Y' | 'ALL';

const selectedPeriod = ref<PeriodKey>('3M');
const periods: { key: PeriodKey; label: string }[] = [
  { key: '1M', label: '1M' },
  { key: '3M', label: '3M' },
  { key: '6M', label: '6M' },
  { key: 'YTD', label: 'YTD' },
  { key: '1Y', label: '1Y' },
  { key: 'ALL', label: 'ALL' },
];

function getDateRange(period: PeriodKey): { from: string; to: string } {
  const today = new Date();
  const to = today.toISOString().split('T')[0];
  let fromDate = new Date(today);

  switch (period) {
    case '1M':
      fromDate.setMonth(fromDate.getMonth() - 1);
      break;
    case '3M':
      fromDate.setMonth(fromDate.getMonth() - 3);
      break;
    case '6M':
      fromDate.setMonth(fromDate.getMonth() - 6);
      break;
    case 'YTD':
      fromDate = new Date(today.getFullYear(), 0, 1);
      break;
    case '1Y':
      fromDate.setFullYear(fromDate.getFullYear() - 1);
      break;
    case 'ALL':
      fromDate.setFullYear(fromDate.getFullYear() - 5);
      break;
  }

  const from = fromDate.toISOString().split('T')[0];
  return { from, to };
}

async function loadPerformance() {
  const { from, to } = getDateRange(selectedPeriod.value);
  try {
    await valuationStore.fetchPerformanceData(props.portfolioId, {
      from,
      to,
      metric: 'TWR',
      frequency: 'DAILY',
    });
  } catch {
    // Error handled in store
  }
}

const perfData = computed<PerformanceData | undefined>(
  () => valuationStore.getPerformanceData(props.portfolioId)
);

const chartOption = computed(() => {
  if (!perfData.value || perfData.value.dataPoints.length === 0) {
    return null;
  }

  const dates = perfData.value.dataPoints.map(dp => dp.date);
  const values = perfData.value.dataPoints.map(dp => +(dp.value * 100).toFixed(2));

  // 색상: 최종 수익률이 양수면 초록, 음수면 빨강
  const lastValue = values[values.length - 1];
  const lineColor = lastValue >= 0 ? '#22c55e' : '#ef4444';
  const areaColorStart = lastValue >= 0 ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)';
  const areaColorEnd = 'rgba(255,255,255,0)';

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0];
        const val = p.value >= 0 ? `+${p.value}%` : `${p.value}%`;
        const color = p.value >= 0 ? '#22c55e' : '#ef4444';
        return `<div style="font-size:13px">
          <div style="color:#888">${p.axisValue}</div>
          <div style="color:${color};font-weight:600;font-size:15px;margin-top:4px">${val}</div>
        </div>`;
      },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#1f2937' },
    },
    grid: {
      left: 60,
      right: 20,
      top: 20,
      bottom: 60,
    },
    xAxis: {
      type: 'category',
      data: dates,
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
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100,
      },
    ],
    series: [
      {
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: lineColor, width: 2 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: areaColorStart },
              { offset: 1, color: areaColorEnd },
            ],
          },
        },
        markLine: {
          silent: true,
          symbol: 'none',
          label: { show: false },
          lineStyle: { color: '#d1d5db', type: 'dashed', width: 1 },
          data: [{ yAxis: 0 }],
        },
      },
    ],
  };
});

const stats = computed(() => perfData.value?.stats);

onMounted(() => {
  loadPerformance();
});

watch(selectedPeriod, () => {
  loadPerformance();
});
</script>

<template>
  <div class="performance-panel">
    <!-- Period Selector -->
    <div class="period-selector">
      <button
        v-for="period in periods"
        :key="period.key"
        class="period-btn"
        :class="{ active: selectedPeriod === period.key }"
        @click="selectedPeriod = period.key"
      >
        {{ period.label }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="valuationStore.performanceLoading" class="chart-loading">
      <div class="spinner"></div>
      <span>{{ t('common.loading') }}</span>
    </div>

    <!-- Chart -->
    <template v-else>
      <div v-if="chartOption" class="chart-container">
        <VChart :option="chartOption" autoresize style="height: 350px" />
      </div>
      <div v-else class="empty-chart">
        <p>{{ t('performance.noData') }}</p>
      </div>

      <!-- Stats Cards -->
      <div v-if="stats" class="stats-grid">
        <div class="stat-card">
          <span class="stat-label">{{ t('performance.totalReturn') }}</span>
          <span
            class="stat-value"
            :class="{ 'number-positive': stats.totalReturn > 0, 'number-negative': stats.totalReturn < 0 }"
          >
            {{ formatPercent(stats.totalReturn, 2, true) }}
          </span>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ t('performance.cagr') }}</span>
          <span
            class="stat-value"
            :class="{ 'number-positive': stats.cagr > 0, 'number-negative': stats.cagr < 0 }"
          >
            {{ formatPercent(stats.cagr, 2, true) }}
          </span>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ t('performance.volatility') }}</span>
          <span class="stat-value">{{ formatPercent(stats.volatility, 2) }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ t('performance.mdd') }}</span>
          <span class="stat-value number-negative" v-if="stats.mdd > 0">
            -{{ formatPercent(stats.mdd, 2) }}
          </span>
          <span class="stat-value" v-else>0.00%</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ t('performance.sharpe') }}</span>
          <span class="stat-value">{{ stats.sharpe?.toFixed(2) ?? '-' }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.performance-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.period-selector {
  display: flex;
  gap: 4px;
  background-color: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: 4px;
  width: fit-content;
}

.period-btn {
  padding: 8px 16px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.period-btn:hover {
  color: var(--text-primary);
}

.period-btn.active {
  background-color: var(--bg-primary);
  color: var(--primary-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chart-container {
  border-radius: var(--radius-md);
}

.chart-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 350px;
  color: var(--text-secondary);
}

.empty-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--text-muted);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  background-color: var(--bg-hover);
  border-radius: var(--radius-md);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.stat-value {
  font-size: 1.125rem;
  font-weight: 600;
}
</style>
