<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useBacktestStore } from '@/stores';
import { formatCurrency, formatPercent, formatNumber, formatDate } from '@/utils/format';
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

const route = useRoute();
const router = useRouter();
const backtestStore = useBacktestStore();
const { t } = useI18n();

const runId = computed(() => route.params.id as string);

onMounted(async () => {
  await backtestStore.fetchResults(runId.value);
});

const result = computed(() => backtestStore.currentResult);

const totalReturnPercent = computed(() => {
  const stats = result.value?.stats;
  const series = result.value?.series;
  if (!stats?.totalInvested || !series?.length) return null;
  const finalValue = series[series.length - 1].equityCurveBase;
  const invested = stats.totalInvested;
  if (invested === 0) return null;
  return (finalValue - invested) / invested;
});

const equityChartOption = computed(() => {
  if (!result.value?.series?.length) return null;

  const dates = result.value.series.map((p: any) => p.ts);
  const equity = result.value.series.map((p: any) => p.equityCurveBase);
  const cash = result.value.series.map((p: any) => p.cashBase);
  const invested = result.value.series.map((p: any) => p.totalInvested);
  const hasDCA = invested.some((v: any, i: number) => i > 0 && v !== invested[0]);

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        let html = `<div style="font-size:12px;color:#888;margin-bottom:4px">${params[0]?.axisValue}</div>`;
        params.forEach((p: any) => {
          if (p.value != null) {
            html += `<div style="display:flex;align-items:center;gap:8px;margin:2px 0">
              <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color}"></span>
              <span style="flex:1">${p.seriesName}</span>
              <span style="font-weight:600">${formatCurrency(p.value, 'KRW')}</span>
            </div>`;
          }
        });
        return html;
      },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
    },
    legend: {
      data: [t('backtest.equityCurve'), ...(hasDCA ? [t('backtest.totalInvestedLine')] : []), t('backtest.cashBalance')],
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 12 },
    },
    grid: { left: 80, right: 20, top: 20, bottom: 50 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        formatter: (val: string) => {
          const d = new Date(val);
          return `${d.getFullYear()}.${d.getMonth() + 1}`;
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
        formatter: (val: number) => {
          if (val >= 1e8) return `${(val / 1e8).toFixed(0)}억`;
          if (val >= 1e4) return `${(val / 1e4).toFixed(0)}만`;
          return val.toString();
        },
        color: '#9ca3af',
        fontSize: 11,
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
    },
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
    series: [
      {
        name: t('backtest.equityCurve'),
        type: 'line',
        data: equity,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2.5, color: '#6366f1' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(99,102,241,0.15)' },
              { offset: 1, color: 'rgba(99,102,241,0)' },
            ],
          },
        },
        itemStyle: { color: '#6366f1' },
      },
      ...(hasDCA ? [{
        name: t('backtest.totalInvestedLine'),
        type: 'line',
        data: invested,
        smooth: false,
        symbol: 'none',
        lineStyle: { width: 1.5, color: '#f59e0b', type: 'dashed' },
        itemStyle: { color: '#f59e0b' },
      }] : []),
      {
        name: t('backtest.cashBalance'),
        type: 'line',
        data: cash,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5, color: '#94a3b8', type: 'dashed' },
        itemStyle: { color: '#94a3b8' },
      },
    ],
  };
});

const drawdownChartOption = computed(() => {
  if (!result.value?.series?.length) return null;

  const dates = result.value.series.map((p: any) => p.ts);
  const drawdowns = result.value.series.map((p: any) =>
    p.drawdown ? +(Number(p.drawdown) * -100).toFixed(2) : 0
  );

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0];
        return `<div style="font-size:12px;color:#888">${p.axisValue}</div>
                <div style="color:#ef4444;font-weight:600">${p.value}%</div>`;
      },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
    },
    grid: { left: 60, right: 20, top: 10, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        formatter: (val: string) => {
          const d = new Date(val);
          return `${d.getFullYear()}.${d.getMonth() + 1}`;
        },
        color: '#9ca3af',
        fontSize: 11,
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      max: 0,
      axisLabel: {
        formatter: (val: number) => `${val}%`,
        color: '#9ca3af',
        fontSize: 11,
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
    },
    series: [
      {
        type: 'line',
        data: drawdowns,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5, color: '#ef4444' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(239,68,68,0)' },
              { offset: 1, color: 'rgba(239,68,68,0.15)' },
            ],
          },
        },
        itemStyle: { color: '#ef4444' },
      },
    ],
  };
});
</script>

<template>
  <div class="backtest-result">
    <header class="page-header">
      <button class="btn btn-secondary" @click="router.push('/backtest')">
        &larr; {{ t('backtest.backToStudio') }}
      </button>
      <h1>{{ t('backtest.results') }}</h1>
    </header>

    <div v-if="backtestStore.loading" class="loading-state">
      <div class="spinner"></div>
      <span>{{ t('common.loading') }}</span>
    </div>

    <template v-else-if="result">
      <!-- Summary Cards -->
      <section class="summary-section">
        <div class="summary-cards">
          <div class="summary-card">
            <span class="label">{{ t('backtest.finalValue') }}</span>
            <span class="value">
              {{ result.series?.length
                ? formatCurrency(result.series[result.series.length - 1].equityCurveBase, 'KRW')
                : '-' }}
            </span>
          </div>
          <div class="summary-card" v-if="result.stats?.totalInvested">
            <span class="label">{{ t('backtest.totalInvested') }}</span>
            <span class="value">
              {{ formatCurrency(result.stats.totalInvested, 'KRW') }}
            </span>
          </div>
          <div class="summary-card" v-if="totalReturnPercent != null">
            <span class="label">{{ t('backtest.totalReturn') }}</span>
            <span class="value" :class="totalReturnPercent >= 0 ? 'number-positive' : 'number-negative'">
              {{ formatPercent(totalReturnPercent, 2, true) }}
            </span>
          </div>
          <div class="summary-card">
            <span class="label">{{ t('performance.cagr') }}</span>
            <span class="value" :class="result.stats?.cagr && result.stats.cagr > 0 ? 'number-positive' : 'number-negative'">
              {{ result.stats?.cagr != null ? formatPercent(result.stats.cagr, 2, true) : '-' }}
            </span>
          </div>
          <div class="summary-card">
            <span class="label">{{ t('performance.volatility') }}</span>
            <span class="value">{{ result.stats?.vol != null ? formatPercent(result.stats.vol) : '-' }}</span>
          </div>
          <div class="summary-card">
            <span class="label">{{ t('performance.mdd') }}</span>
            <span class="value number-negative">
              {{ result.stats?.mdd != null ? '-' + formatPercent(result.stats.mdd) : '-' }}
            </span>
          </div>
          <div class="summary-card">
            <span class="label">{{ t('performance.sharpe') }}</span>
            <span class="value">{{ result.stats?.sharpe != null ? formatNumber(result.stats.sharpe, 2) : '-' }}</span>
          </div>
        </div>
      </section>

      <!-- Equity Curve Chart -->
      <section class="chart-section card">
        <h3>{{ t('backtest.equityCurve') }}</h3>
        <div v-if="equityChartOption" class="chart-container">
          <VChart :option="equityChartOption" autoresize style="height: 400px" />
        </div>
      </section>

      <!-- Drawdown Chart -->
      <section class="chart-section card">
        <h3>{{ t('backtest.drawdown') }}</h3>
        <div v-if="drawdownChartOption" class="chart-container">
          <VChart :option="drawdownChartOption" autoresize style="height: 200px" />
        </div>
      </section>

      <!-- Trade Logs -->
      <section class="trades-section card">
        <h3>{{ t('backtest.tradeLog') }} ({{ result.tradeLogs?.length || 0 }})</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t('transaction.date') }}</th>
                <th>{{ t('transaction.instrument') }}</th>
                <th>{{ t('backtest.action') }}</th>
                <th class="text-right">{{ t('transaction.quantity') }}</th>
                <th class="text-right">{{ t('transaction.price') }}</th>
                <th class="text-right">{{ t('transaction.fee') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(trade, index) in (result.tradeLogs || []).slice(0, 50)" :key="index">
                <td>{{ formatDate(trade.ts) }}</td>
                <td class="font-mono">{{ trade.instrumentId || '-' }}</td>
                <td>
                  <span class="badge" :class="{
                    'badge-success': trade.action === 'BUY',
                    'badge-danger': trade.action === 'SELL',
                    'badge-info': trade.action === 'DEPOSIT',
                  }">
                    {{ trade.action === 'DEPOSIT' ? t('backtest.deposit') : trade.action }}
                  </span>
                </td>
                <td class="text-right font-mono">
                  {{ trade.action === 'DEPOSIT' ? '-' : formatNumber(trade.quantity, 4) }}
                </td>
                <td class="text-right font-mono">
                  {{ trade.action === 'DEPOSIT' ? '-' : formatCurrency(trade.price, 'KRW') }}
                </td>
                <td class="text-right font-mono">
                  {{ trade.action === 'DEPOSIT'
                    ? formatCurrency(trade.amount ?? 0, 'KRW')
                    : formatCurrency(trade.fee, 'KRW') }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="result.tradeLogs && result.tradeLogs.length > 50" class="more-hint">
          50 / {{ result.tradeLogs.length }} {{ t('backtest.tradesShown') }}
        </p>
      </section>
    </template>

    <div v-else class="error-state card">
      <p>{{ t('backtest.loadFailed') }}</p>
      <button class="btn btn-primary" @click="router.push('/backtest')">
        {{ t('backtest.newBacktest') }}
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

.chart-container {
  border-radius: var(--radius-md);
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

.badge-info {
  background-color: #dbeafe;
  color: #2563eb;
}

.more-hint {
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
}
</style>
