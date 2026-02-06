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
import { formatPercent, getChangeClass } from '@/utils/format';
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

// Benchmark visibility toggles
const benchmarkVisibility = ref<Record<string, boolean>>({});

const BENCHMARK_COLORS: Record<string, string> = {
  KOSPI: '#f59e0b',
  SP500: '#8b5cf6',
};

const monthLabels = computed(() => [
  t('performance.jan'), t('performance.feb'), t('performance.mar'), t('performance.apr'),
  t('performance.may'), t('performance.jun'), t('performance.jul'), t('performance.aug'),
  t('performance.sep'), t('performance.oct'), t('performance.nov'), t('performance.dec'),
]);

function getDateRange(period: PeriodKey): { from: string; to: string } {
  const today = new Date();
  const to = today.toISOString().split('T')[0] as string;
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

  const from = fromDate.toISOString().split('T')[0] as string;
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

const benchmarks = computed(() => perfData.value?.benchmarks || []);

const chartOption = computed(() => {
  if (!perfData.value || perfData.value.dataPoints.length === 0) {
    return null;
  }

  const dates = perfData.value.dataPoints.map(dp => dp.date);
  const values = perfData.value.dataPoints.map(dp => +(dp.value * 100).toFixed(2));

  const lastValue = values[values.length - 1] ?? 0;
  const lineColor = lastValue >= 0 ? '#22c55e' : '#ef4444';
  const areaColorStart = lastValue >= 0 ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)';
  const areaColorEnd = 'rgba(255,255,255,0)';

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const allSeries: any[] = [
    {
      name: t('portfolio.performance'),
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
  ];

  // Add benchmark series
  for (const bm of benchmarks.value) {
    if (!benchmarkVisibility.value[bm.id]) continue;

    const bmMap = new Map(bm.dataPoints.map(dp => [dp.date, dp.value]));
    const bmValues = dates.map(d => {
      const v = bmMap.get(d);
      return v !== undefined ? +(v * 100).toFixed(2) : null;
    });

    allSeries.push({
      name: bm.label,
      type: 'line',
      data: bmValues,
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: BENCHMARK_COLORS[bm.id] || '#94a3b8',
        width: 1.5,
        type: 'dashed',
      },
      connectNulls: true,
    });
  }

  const showLegend = benchmarks.value.some(bm => benchmarkVisibility.value[bm.id]);

  return {
    tooltip: {
      trigger: 'axis',
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      formatter: (params: any) => {
        if (!Array.isArray(params)) params = [params];
        let html = `<div style="font-size:13px"><div style="color:#888">${params[0].axisValue}</div>`;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        for (const p of params) {
          if (p.value === null || p.value === undefined) continue;
          const val = p.value >= 0 ? `+${p.value}%` : `${p.value}%`;
          const color = p.value >= 0 ? '#22c55e' : '#ef4444';
          html += `<div style="margin-top:4px;display:flex;align-items:center;gap:6px">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
            <span style="color:#666">${p.seriesName}:</span>
            <span style="color:${color};font-weight:600">${val}</span>
          </div>`;
        }
        html += '</div>';
        return html;
      },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#1f2937' },
    },
    legend: showLegend ? {
      bottom: 0,
      textStyle: { fontSize: 12, color: '#64748b' },
    } : undefined,
    grid: {
      left: 60,
      right: 20,
      top: 20,
      bottom: showLegend ? 80 : 60,
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
    series: allSeries,
  };
});

const stats = computed(() => perfData.value?.stats);

// Monthly Return Heatmap
interface MonthlyReturn {
  year: number;
  month: number;
  value: number;
}

const monthlyReturns = computed<MonthlyReturn[]>(() => {
  if (!perfData.value || perfData.value.dataPoints.length < 2) return [];

  const points = perfData.value.dataPoints;
  const monthEndMap = new Map<string, number>();

  for (const dp of points) {
    const d = new Date(dp.date);
    const key = `${d.getFullYear()}-${String(d.getMonth()).padStart(2, '0')}`;
    monthEndMap.set(key, dp.value);
  }

  const sortedKeys = Array.from(monthEndMap.keys()).sort();
  const result: MonthlyReturn[] = [];

  for (let i = 0; i < sortedKeys.length; i++) {
    const key = sortedKeys[i]!;
    const parts = key.split('-');
    const yearStr = parts[0] ?? '0';
    const monthStr = parts[1] ?? '0';
    const cumEnd = monthEndMap.get(key)!;
    const cumPrev = i > 0 ? monthEndMap.get(sortedKeys[i - 1]!)! : 0;

    const monthReturn = (1 + cumEnd) / (1 + cumPrev) - 1;

    result.push({
      year: parseInt(yearStr),
      month: parseInt(monthStr),
      value: monthReturn,
    });
  }

  return result;
});

const heatmapGrid = computed(() => {
  if (monthlyReturns.value.length === 0) return [];

  const yearMap = new Map<number, (number | null)[]>();

  for (const mr of monthlyReturns.value) {
    if (!yearMap.has(mr.year)) {
      yearMap.set(mr.year, new Array(12).fill(null));
    }
    yearMap.get(mr.year)![mr.month] = mr.value;
  }

  const years = Array.from(yearMap.keys()).sort();
  return years.map(year => {
    const months = yearMap.get(year)!;
    const validMonths = months.filter((v): v is number => v !== null);
    const yearlyReturn = validMonths.length > 0
      ? validMonths.reduce((acc, v) => acc * (1 + v), 1) - 1
      : 0;
    return { year, months, yearlyReturn };
  });
});

function getHeatmapColor(value: number | null): string {
  if (value === null) return 'transparent';
  if (value === 0) return 'var(--bg-hover)';
  const absVal = Math.abs(value);
  const intensity = Math.min(absVal / 0.10, 1);
  if (value > 0) {
    const alpha = 0.15 + intensity * 0.55;
    return `rgba(34, 197, 94, ${alpha})`;
  } else {
    const alpha = 0.15 + intensity * 0.55;
    return `rgba(239, 68, 68, ${alpha})`;
  }
}

function getHeatmapTextColor(value: number | null): string {
  if (value === null) return 'transparent';
  const absVal = Math.abs(value);
  if (absVal > 0.05) return '#fff';
  return value > 0 ? '#166534' : '#991b1b';
}

onMounted(() => {
  loadPerformance();
});

watch(selectedPeriod, () => {
  loadPerformance();
});
</script>

<template>
  <div class="performance-panel">
    <!-- Period Selector + Benchmark Toggles -->
    <div class="controls-row">
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

      <!-- Benchmark Toggles -->
      <div v-if="benchmarks.length > 0" class="benchmark-toggles">
        <span class="toggle-label">{{ t('performance.benchmark') }}:</span>
        <label
          v-for="bm in benchmarks"
          :key="bm.id"
          class="benchmark-checkbox"
        >
          <input
            type="checkbox"
            v-model="benchmarkVisibility[bm.id]"
          />
          <span
            class="checkbox-label"
            :style="{ color: BENCHMARK_COLORS[bm.id] || '#94a3b8' }"
          >{{ bm.label }}</span>
        </label>
      </div>
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

      <!-- Benchmark Stats Comparison -->
      <div v-if="benchmarks.some(bm => benchmarkVisibility[bm.id])" class="benchmark-stats">
        <h4>{{ t('performance.benchmarkComparison') }}</h4>
        <div class="benchmark-table-container">
          <table class="benchmark-table">
            <thead>
              <tr>
                <th></th>
                <th class="text-right">{{ t('performance.totalReturn') }}</th>
                <th class="text-right">{{ t('performance.cagr') }}</th>
                <th class="text-right">{{ t('performance.volatility') }}</th>
                <th class="text-right">{{ t('performance.mdd') }}</th>
                <th class="text-right">{{ t('performance.sharpe') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="stats">
                <td class="font-weight-600">{{ t('portfolio.performance') }}</td>
                <td class="text-right" :class="getChangeClass(stats.totalReturn)">
                  {{ formatPercent(stats.totalReturn, 2, true) }}
                </td>
                <td class="text-right" :class="getChangeClass(stats.cagr)">
                  {{ formatPercent(stats.cagr, 2, true) }}
                </td>
                <td class="text-right">{{ formatPercent(stats.volatility, 2) }}</td>
                <td class="text-right number-negative" v-if="stats.mdd > 0">
                  -{{ formatPercent(stats.mdd, 2) }}
                </td>
                <td class="text-right" v-else>0.00%</td>
                <td class="text-right">{{ stats.sharpe?.toFixed(2) ?? '-' }}</td>
              </tr>
              <tr v-for="bm in benchmarks.filter(b => benchmarkVisibility[b.id])" :key="bm.id">
                <td>
                  <span
                    class="bm-dot"
                    :style="{ backgroundColor: BENCHMARK_COLORS[bm.id] || '#94a3b8' }"
                  ></span>
                  {{ bm.label }}
                </td>
                <td class="text-right" :class="getChangeClass(bm.stats.totalReturn)">
                  {{ formatPercent(bm.stats.totalReturn, 2, true) }}
                </td>
                <td class="text-right" :class="getChangeClass(bm.stats.cagr)">
                  {{ formatPercent(bm.stats.cagr, 2, true) }}
                </td>
                <td class="text-right">{{ formatPercent(bm.stats.volatility, 2) }}</td>
                <td class="text-right number-negative" v-if="bm.stats.mdd > 0">
                  -{{ formatPercent(bm.stats.mdd, 2) }}
                </td>
                <td class="text-right" v-else>0.00%</td>
                <td class="text-right">{{ bm.stats.sharpe?.toFixed(2) ?? '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Monthly Return Heatmap -->
      <div v-if="heatmapGrid.length > 0" class="monthly-heatmap">
        <h3>{{ t('performance.monthlyReturns') }}</h3>
        <div class="heatmap-container">
          <table class="heatmap-table">
            <thead>
              <tr>
                <th class="year-col"></th>
                <th v-for="(label, idx) in monthLabels" :key="idx" class="month-col">
                  {{ label }}
                </th>
                <th class="yearly-col">{{ t('performance.yearlyTotal') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in heatmapGrid" :key="row.year">
                <td class="year-label">{{ row.year }}</td>
                <td
                  v-for="(val, mIdx) in row.months"
                  :key="mIdx"
                  class="heatmap-cell"
                  :style="{
                    backgroundColor: getHeatmapColor(val),
                    color: getHeatmapTextColor(val),
                  }"
                >
                  <template v-if="val !== null">
                    {{ (val * 100).toFixed(1) }}%
                  </template>
                </td>
                <td
                  class="heatmap-cell yearly-cell"
                  :style="{
                    backgroundColor: getHeatmapColor(row.yearlyReturn),
                    color: getHeatmapTextColor(row.yearlyReturn),
                  }"
                >
                  {{ (row.yearlyReturn * 100).toFixed(1) }}%
                </td>
              </tr>
            </tbody>
          </table>
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

.controls-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
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

/* Benchmark Toggles */
.benchmark-toggles {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.benchmark-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
}

.benchmark-checkbox input[type="checkbox"] {
  accent-color: var(--primary-color);
}

.checkbox-label {
  font-weight: 500;
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

/* Benchmark Stats */
.benchmark-stats {
  margin-top: 4px;
}

.benchmark-stats h4 {
  font-size: 14px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin-bottom: 12px;
}

.benchmark-table-container {
  overflow-x: auto;
}

.benchmark-table {
  width: 100%;
  font-size: 13px;
  border-collapse: collapse;
}

.benchmark-table th,
.benchmark-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
}

.benchmark-table th {
  font-size: 11px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  font-weight: 500;
}

.text-right { text-align: right; }
.font-weight-600 { font-weight: 600; }

.bm-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

/* Monthly Return Heatmap */
.monthly-heatmap {
  margin-top: 4px;
}

.monthly-heatmap h3 {
  font-size: 14px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin-bottom: 12px;
}

.heatmap-container {
  overflow-x: auto;
}

.heatmap-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 3px;
  font-size: 12px;
}

.heatmap-table th {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-muted);
  text-align: center;
  padding: 4px 2px;
}

.year-label {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-secondary);
  padding-right: 8px;
  text-align: right;
}

.heatmap-cell {
  text-align: center;
  padding: 8px 4px;
  border-radius: 4px;
  font-weight: 500;
  font-family: var(--font-mono, monospace);
  font-size: 11px;
  min-width: 52px;
}

.yearly-cell {
  font-weight: 700;
  border-left: 2px solid var(--border-color);
}

.month-col { min-width: 52px; }
.yearly-col { min-width: 60px; }
</style>
