<script setup lang="ts">
import { ref, computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import type { ValuationPosition } from '@/types';
import { useI18n } from 'vue-i18n';

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent]);

interface Props {
  positions: ValuationPosition[];
  cashWeight?: number;
}

const props = defineProps<Props>();
const { t } = useI18n();

type ViewMode = 'assetClass' | 'instrument';
const viewMode = ref<ViewMode>('assetClass');

const COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16', '#64748b', '#14b8a6'];

const ASSET_CLASS_LABELS: Record<string, string> = {
  EQUITY: 'Equity',
  BOND: 'Bond',
  COMMODITY: 'Commodity',
  CASH: 'Cash',
  ALT: 'Alternative',
};

const chartOption = computed(() => {
  let data: Array<{ name: string; value: number }> = [];

  if (viewMode.value === 'assetClass') {
    const grouped: Record<string, number> = {};
    for (const pos of props.positions) {
      const cls = pos.assetClass || 'EQUITY';
      grouped[cls] = (grouped[cls] || 0) + pos.weight;
    }
    if (props.cashWeight && props.cashWeight > 0.001) {
      grouped['CASH'] = (grouped['CASH'] || 0) + props.cashWeight;
    }
    data = Object.entries(grouped).map(([cls, weight]) => ({
      name: ASSET_CLASS_LABELS[cls] || cls,
      value: +(weight * 100).toFixed(1),
    }));
  } else {
    data = props.positions
      .filter(p => p.weight > 0.001)
      .map(p => ({
        name: p.ticker || p.instrumentName || p.instrumentId,
        value: +(p.weight * 100).toFixed(1),
      }));
    if (props.cashWeight && props.cashWeight > 0.001) {
      data.push({ name: t('portfolio.cash'), value: +(props.cashWeight * 100).toFixed(1) });
    }
  }

  return {
    tooltip: {
      formatter: (p: { name: string; value: number }) => `${p.name}: ${p.value}%`,
    },
    legend: {
      bottom: 0,
      type: 'scroll',
      textStyle: { fontSize: 12, color: '#64748b' },
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' },
      },
      data: data.map((d, i) => ({
        ...d,
        itemStyle: { color: COLORS[i % COLORS.length] },
      })),
    }],
  };
});
</script>

<template>
  <div class="allocation-chart">
    <div class="chart-header">
      <h3>{{ t('portfolio.allocation') }}</h3>
      <div class="view-toggle">
        <button
          class="toggle-btn"
          :class="{ active: viewMode === 'assetClass' }"
          @click="viewMode = 'assetClass'"
        >
          {{ t('portfolio.byAssetClass') }}
        </button>
        <button
          class="toggle-btn"
          :class="{ active: viewMode === 'instrument' }"
          @click="viewMode = 'instrument'"
        >
          {{ t('portfolio.byInstrument') }}
        </button>
      </div>
    </div>
    <VChart :option="chartOption" autoresize style="height: 300px" />
  </div>
</template>

<style scoped>
.allocation-chart {
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chart-header h3 {
  font-size: 1rem;
}

.view-toggle {
  display: flex;
  gap: 4px;
  background-color: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: 4px;
}

.toggle-btn {
  padding: 6px 14px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn:hover {
  color: var(--text-primary);
}

.toggle-btn.active {
  background-color: var(--bg-primary);
  color: var(--primary-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
