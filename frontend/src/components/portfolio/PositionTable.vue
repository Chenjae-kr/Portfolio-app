<script setup lang="ts">
import type { ValuationPosition } from '@/types';
import { formatCurrency, formatPercent, formatQuantity, getChangeClass } from '@/utils/format';

interface Props {
  positions: ValuationPosition[];
  currency: string;
}

defineProps<Props>();
</script>

<template>
  <div class="position-table-wrapper">
    <table class="table position-table">
      <thead>
        <tr>
          <th>Instrument</th>
          <th class="text-right">Quantity</th>
          <th class="text-right">Price</th>
          <th class="text-right">Value</th>
          <th class="text-right">Weight</th>
          <th class="text-right">P&L</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="position in positions" :key="position.instrumentId">
          <td>
            <div class="instrument-cell">
              <span class="ticker">{{ position.ticker || position.instrumentId }}</span>
              <span v-if="position.instrumentName" class="name">{{ position.instrumentName }}</span>
            </div>
          </td>
          <td class="text-right font-mono">{{ formatQuantity(position.quantity) }}</td>
          <td class="text-right font-mono">{{ formatCurrency(position.marketPrice, currency) }}</td>
          <td class="text-right font-mono">{{ formatCurrency(position.marketValueBase, currency) }}</td>
          <td class="text-right">{{ formatPercent(position.weight) }}</td>
          <td class="text-right">
            <div class="pnl-cell" :class="getChangeClass(position.unrealizedPnlBase)">
              <span class="pnl-value">{{ formatCurrency(position.unrealizedPnlBase, currency, true) }}</span>
              <span v-if="position.avgCost" class="pnl-percent">
                ({{ formatPercent((position.marketPrice - position.avgCost) / position.avgCost, 2, true) }})
              </span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="positions.length === 0" class="empty-state">
      <p>No positions in this portfolio</p>
    </div>
  </div>
</template>

<style scoped>
.position-table-wrapper {
  overflow-x: auto;
}

.position-table th,
.position-table td {
  padding: 14px 16px;
}

.text-right {
  text-align: right;
}

.font-mono {
  font-family: var(--font-mono);
  font-size: 13px;
}

.instrument-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ticker {
  font-weight: 600;
}

.name {
  font-size: 12px;
  color: var(--text-muted);
}

.pnl-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.pnl-value {
  font-weight: 500;
}

.pnl-percent {
  font-size: 12px;
  opacity: 0.8;
}

.empty-state {
  padding: 40px;
  text-align: center;
  color: var(--text-secondary);
}
</style>
