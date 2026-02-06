<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { transactionApi } from '@/api';
import type { Transaction, TransactionType } from '@/types';
import { formatCurrency } from '@/utils/format';
import { useI18n } from 'vue-i18n';
import dayjs from 'dayjs';

const props = defineProps<{
  portfolioId: string;
  baseCurrency: string;
}>();

const emit = defineEmits<{
  (e: 'add'): void;
}>();

const { t } = useI18n();
const transactions = ref<Transaction[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

// 필터 상태
const filterFromDate = ref<string>('');
const filterToDate = ref<string>('');
const filterType = ref<TransactionType | ''>('');

const TX_TYPES = ['BUY', 'SELL', 'DEPOSIT', 'WITHDRAW', 'DIVIDEND', 'INTEREST', 'FEE', 'TAX', 'FX_CONVERT', 'SPLIT', 'MERGER', 'TRANSFER'] as const;

function getTypeLabel(type: string) {
  return t(`transaction.types.${type}`) || type;
}

const typeColors: Record<string, string> = {
  BUY: '#2563eb',
  SELL: '#dc2626',
  DEPOSIT: '#16a34a',
  WITHDRAW: '#ea580c',
  DIVIDEND: '#7c3aed',
  INTEREST: '#0891b2',
  FEE: '#64748b',
  TAX: '#64748b',
  FX_CONVERT: '#d97706',
};

function getMainLeg(tx: Transaction) {
  const assetLeg = tx.legs.find(l => l.legType === 'ASSET');
  if (assetLeg) return assetLeg;
  const cashLeg = tx.legs.find(l => l.legType === 'CASH');
  if (cashLeg) return cashLeg;
  return tx.legs[0];
}

function getDisplayAmount(tx: Transaction): number {
  const leg = getMainLeg(tx);
  if (!leg) return 0;
  if (leg.legType === 'ASSET') {
    return Math.abs(leg.amount);
  }
  // DEPOSIT/WITHDRAW: show positive amount
  return Math.abs(leg.amount);
}

async function fetchTransactions() {
  loading.value = true;
  error.value = null;
  try {
    const params: { fromDate?: string; toDate?: string; type?: TransactionType } = {};
    if (filterFromDate.value) params.fromDate = filterFromDate.value;
    if (filterToDate.value) params.toDate = filterToDate.value;
    if (filterType.value) params.type = filterType.value as TransactionType;

    transactions.value = await transactionApi.list(props.portfolioId, params);
  } catch (e: unknown) {
    error.value = (e as Error).message || 'Failed to load transactions';
  } finally {
    loading.value = false;
  }
}

function applyFilter() {
  fetchTransactions();
}

function resetFilter() {
  filterFromDate.value = '';
  filterToDate.value = '';
  filterType.value = '';
  fetchTransactions();
}

async function handleVoid(txId: string) {
  if (!confirm(t('transaction.confirmVoid'))) return;
  try {
    await transactionApi.void(txId);
    await fetchTransactions();
  } catch (e: unknown) {
    alert((e as Error).message || 'Failed to void transaction');
  }
}

onMounted(fetchTransactions);

defineExpose({ refresh: fetchTransactions });
</script>

<template>
  <div class="transaction-list">
    <div class="list-header">
      <h3>{{ t('transaction.title') }}</h3>
      <button class="btn btn-primary btn-sm" @click="$emit('add')">
        + {{ t('transaction.addTransaction') }}
      </button>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label>{{ t('transaction.filterFromDate') }}</label>
        <input v-model="filterFromDate" type="date" class="filter-input" />
      </div>
      <div class="filter-group">
        <label>{{ t('transaction.filterToDate') }}</label>
        <input v-model="filterToDate" type="date" class="filter-input" />
      </div>
      <div class="filter-group">
        <label>{{ t('transaction.filterType') }}</label>
        <select v-model="filterType" class="filter-select">
          <option value="">{{ t('transaction.filterAllTypes') }}</option>
          <option v-for="txType in TX_TYPES" :key="txType" :value="txType">{{ getTypeLabel(txType) }}</option>
        </select>
      </div>
      <div class="filter-actions">
        <button class="btn btn-secondary btn-sm" @click="applyFilter">{{ t('transaction.filterApply') }}</button>
        <button class="btn btn-outline btn-sm" @click="resetFilter">{{ t('transaction.filterReset') }}</button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn btn-secondary btn-sm" @click="fetchTransactions">{{ t('common.retry') }}</button>
    </div>

    <div v-else-if="transactions.length === 0" class="empty-state">
      <p>{{ t('transaction.noTransactions') }}</p>
      <button class="btn btn-primary btn-sm" @click="$emit('add')">
        {{ t('transaction.addFirst') }}
      </button>
    </div>

    <table v-else class="tx-table">
      <thead>
        <tr>
          <th>{{ t('transaction.date') }}</th>
          <th>{{ t('transaction.type') }}</th>
          <th>{{ t('transaction.instrument') }}</th>
          <th class="text-right">{{ t('transaction.quantity') }}</th>
          <th class="text-right">{{ t('transaction.price') }}</th>
          <th class="text-right">{{ t('transaction.amount') }}</th>
          <th>{{ t('transaction.status') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tx in transactions" :key="tx.id" :class="{ voided: tx.status === 'VOID' }">
          <td class="date-cell">{{ dayjs(tx.occurredAt).format('YYYY-MM-DD') }}</td>
          <td>
            <span class="type-badge" :style="{ color: typeColors[tx.type] || '#64748b' }">
              {{ getTypeLabel(tx.type) }}
            </span>
          </td>
          <td>
            <template v-if="getMainLeg(tx)?.instrumentId">
              {{ getMainLeg(tx)?.instrumentId }}
            </template>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="text-right">
            <template v-if="getMainLeg(tx)?.quantity">
              {{ getMainLeg(tx)?.quantity }}
            </template>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="text-right">
            <template v-if="getMainLeg(tx)?.price">
              {{ formatCurrency(getMainLeg(tx)!.price!, getMainLeg(tx)!.currency) }}
            </template>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="text-right amount-cell">
            {{ formatCurrency(getDisplayAmount(tx), baseCurrency) }}
          </td>
          <td>
            <span class="status-badge" :class="tx.status.toLowerCase()">
              {{ tx.status }}
            </span>
          </td>
          <td>
            <button
              v-if="tx.status === 'POSTED'"
              class="btn-void"
              @click="handleVoid(tx.id)"
              :title="t('transaction.void')"
            >
              ✕
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.transaction-list {
  width: 100%;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0;
  font-size: 1.1rem;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-secondary, #f8fafc);
  border-radius: 8px;
  border: 1px solid var(--border-color, #e2e8f0);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-secondary, #64748b);
}

.filter-input,
.filter-select {
  padding: 6px 10px;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 6px;
  font-size: 14px;
  min-width: 120px;
}

.filter-select {
  min-width: 100px;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: 8px;
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--border-color, #e2e8f0);
  color: var(--text-secondary, #64748b);
}

.btn-outline:hover {
  background: var(--bg-hover, rgba(0,0,0,0.04));
}

.tx-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.tx-table th {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 2px solid var(--border-color);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
}

.tx-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
}

.tx-table tr:hover {
  background-color: var(--bg-hover, rgba(0,0,0,0.02));
}

.tx-table tr.voided {
  opacity: 0.5;
  text-decoration: line-through;
}

.text-right {
  text-align: right;
}

.text-muted {
  color: var(--text-muted, #94a3b8);
}

.date-cell {
  font-variant-numeric: tabular-nums;
  color: var(--text-secondary);
}

.amount-cell {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.type-badge {
  font-weight: 600;
  font-size: 13px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.posted {
  background-color: #dcfce7;
  color: #166534;
}

.status-badge.void {
  background-color: #fef2f2;
  color: #991b1b;
}

.status-badge.pending {
  background-color: #fef9c3;
  color: #854d0e;
}

.btn-void {
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 2px 6px;
  font-size: 12px;
}

.btn-void:hover {
  background-color: #fef2f2;
  color: #dc2626;
  border-color: #dc2626;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 13px;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
}
</style>
