<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { transactionApi, type CreateTransactionRequest } from '@/api';
import type { TransactionType, LegType } from '@/types';
import { useI18n } from 'vue-i18n';
import dayjs from 'dayjs';

const props = defineProps<{
  portfolioId: string;
  baseCurrency: string;
}>();

const emit = defineEmits<{
  (e: 'saved'): void;
  (e: 'cancel'): void;
}>();

const { t } = useI18n();
const loading = ref(false);
const error = ref<string | null>(null);

// Form state
const txType = ref<TransactionType>('BUY');
const occurredAt = ref(dayjs().format('YYYY-MM-DDTHH:mm'));
const note = ref('');

// Simple form fields for BUY/SELL
const instrumentId = ref('');
const quantity = ref<number | null>(null);
const price = ref<number | null>(null);
const fee = ref<number | null>(null);
const currency = ref('');

// For DEPOSIT/WITHDRAW
const cashAmount = ref<number | null>(null);

const transactionTypes: { value: TransactionType; label: string }[] = [
  { value: 'BUY', label: '매수' },
  { value: 'SELL', label: '매도' },
  { value: 'DEPOSIT', label: '입금' },
  { value: 'WITHDRAW', label: '출금' },
  { value: 'DIVIDEND', label: '배당' },
];

const isAssetTransaction = computed(() => txType.value === 'BUY' || txType.value === 'SELL');
const isCashTransaction = computed(() => txType.value === 'DEPOSIT' || txType.value === 'WITHDRAW');
const isDividend = computed(() => txType.value === 'DIVIDEND');

const totalAmount = computed(() => {
  if (!quantity.value || !price.value) return 0;
  return quantity.value * price.value;
});

watch(txType, () => {
  // Reset currency to baseCurrency
  currency.value = props.baseCurrency;
});

// Initialize currency
currency.value = props.baseCurrency;

function buildLegs(): CreateTransactionRequest['legs'] {
  const legs: CreateTransactionRequest['legs'] = [];

  if (isAssetTransaction.value) {
    const isBuy = txType.value === 'BUY';
    const amt = totalAmount.value;

    // ASSET leg: 매수=+qty/+amount, 매도=-qty/-amount
    legs.push({
      legType: 'ASSET' as LegType,
      instrumentId: instrumentId.value,
      currency: currency.value,
      quantity: isBuy ? quantity.value! : -quantity.value!,
      price: price.value!,
      amount: isBuy ? amt : -amt,
    });

    // CASH leg: 매수=-amount, 매도=+amount (복식부기 밸런싱)
    legs.push({
      legType: 'CASH' as LegType,
      currency: currency.value,
      amount: isBuy ? -amt : amt,
    });

    // FEE leg (optional)
    if (fee.value && fee.value > 0) {
      legs.push({
        legType: 'FEE' as LegType,
        currency: currency.value,
        amount: fee.value,
      });
      // Adjust CASH leg to include fee
      legs[1].amount = isBuy ? -(amt + fee.value) : (amt - fee.value);
      legs[0].amount = isBuy ? (amt + fee.value) : -(amt + fee.value);
      // Recalculate: ASSET + CASH + FEE = 0
      // BUY:  ASSET(+total) + CASH(-total-fee) + FEE(+fee) = 0 => ASSET = total, CASH = -(total+fee), FEE = fee ... doesn't balance
      // Let's fix: 
      // BUY:  ASSET(+total) + CASH(-(total+fee)) + FEE(+fee) = total - total - fee + fee = 0 ✓
      // SELL: ASSET(-total) + CASH(+(total-fee)) + FEE(+fee) = -total + total - fee + fee = 0 ✓
      legs[0].amount = isBuy ? amt : -amt;
      legs[1].amount = isBuy ? -(amt + fee.value) : (amt - fee.value);
      legs[2].amount = fee.value;
    }
  } else if (isCashTransaction.value) {
    const isDeposit = txType.value === 'DEPOSIT';
    const amt = cashAmount.value!;

    // For deposit/withdraw, we need legs to balance to 0
    // CASH leg: 입금=+amount, 출금=-amount
    legs.push({
      legType: 'CASH' as LegType,
      currency: currency.value,
      amount: isDeposit ? amt : -amt,
    });

    // Balancing leg (external)
    legs.push({
      legType: 'CASH' as LegType,
      currency: currency.value,
      amount: isDeposit ? -amt : amt,
      account: 'EXTERNAL',
    });
  } else if (isDividend.value) {
    const amt = cashAmount.value!;

    // INCOME leg (+amount)
    legs.push({
      legType: 'INCOME' as LegType,
      instrumentId: instrumentId.value || undefined,
      currency: currency.value,
      amount: amt,
    });

    // CASH leg (-amount to balance)
    legs.push({
      legType: 'CASH' as LegType,
      currency: currency.value,
      amount: -amt,
    });
  }

  return legs;
}

async function handleSubmit() {
  error.value = null;

  // Validation
  if (isAssetTransaction.value) {
    if (!instrumentId.value.trim()) {
      error.value = t('transaction.instrumentRequired');
      return;
    }
    if (!quantity.value || quantity.value <= 0) {
      error.value = t('transaction.quantityRequired');
      return;
    }
    if (!price.value || price.value <= 0) {
      error.value = t('transaction.priceRequired');
      return;
    }
  } else if (isCashTransaction.value || isDividend.value) {
    if (!cashAmount.value || cashAmount.value <= 0) {
      error.value = t('transaction.amountRequired');
      return;
    }
  }

  loading.value = true;
  try {
    const request: CreateTransactionRequest = {
      type: txType.value,
      occurredAt: dayjs(occurredAt.value).format('YYYY-MM-DDTHH:mm:ss'),
      note: note.value || undefined,
      legs: buildLegs(),
    };

    await transactionApi.create(props.portfolioId, request);
    emit('saved');
  } catch (e: unknown) {
    error.value = (e as Error).message || t('transaction.createFailed');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="transaction-form">
    <h3>{{ t('transaction.addTransaction') }}</h3>

    <div v-if="error" class="error-message">{{ error }}</div>

    <form @submit.prevent="handleSubmit">
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">{{ t('transaction.type') }}</label>
          <select v-model="txType" class="form-input">
            <option v-for="tt in transactionTypes" :key="tt.value" :value="tt.value">
              {{ tt.label }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">{{ t('transaction.date') }}</label>
          <input v-model="occurredAt" type="datetime-local" class="form-input" />
        </div>
      </div>

      <!-- BUY / SELL -->
      <template v-if="isAssetTransaction">
        <div class="form-row">
          <div class="form-group flex-2">
            <label class="form-label">{{ t('transaction.instrument') }}</label>
            <input
              v-model="instrumentId"
              type="text"
              class="form-input"
              :placeholder="t('transaction.instrumentPlaceholder')"
            />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('transaction.currency') }}</label>
            <input v-model="currency" type="text" class="form-input" maxlength="3" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ t('transaction.quantity') }}</label>
            <input v-model.number="quantity" type="number" step="any" min="0" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('transaction.price') }}</label>
            <input v-model.number="price" type="number" step="any" min="0" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('transaction.fee') }}</label>
            <input v-model.number="fee" type="number" step="any" min="0" class="form-input" placeholder="0" />
          </div>
        </div>

        <div v-if="totalAmount > 0" class="total-row">
          <span>{{ t('transaction.totalAmount') }}:</span>
          <strong>{{ totalAmount.toLocaleString() }} {{ currency }}</strong>
          <template v-if="fee && fee > 0">
            <span class="fee-info">(+ {{ t('transaction.fee') }} {{ fee.toLocaleString() }})</span>
          </template>
        </div>
      </template>

      <!-- DEPOSIT / WITHDRAW / DIVIDEND -->
      <template v-if="isCashTransaction || isDividend">
        <div class="form-row">
          <div v-if="isDividend" class="form-group">
            <label class="form-label">{{ t('transaction.instrument') }}</label>
            <input
              v-model="instrumentId"
              type="text"
              class="form-input"
              :placeholder="t('transaction.instrumentPlaceholder')"
            />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('transaction.amount') }}</label>
            <input v-model.number="cashAmount" type="number" step="any" min="0" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('transaction.currency') }}</label>
            <input v-model="currency" type="text" class="form-input" maxlength="3" />
          </div>
        </div>
      </template>

      <div class="form-group">
        <label class="form-label">{{ t('transaction.note') }}</label>
        <input v-model="note" type="text" class="form-input" :placeholder="t('transaction.notePlaceholder')" />
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" @click="$emit('cancel')">
          {{ t('common.cancel') }}
        </button>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? '...' : t('transaction.save') }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.transaction-form {
  width: 100%;
}

.transaction-form h3 {
  margin: 0 0 20px 0;
  font-size: 1.1rem;
}

.form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.form-group.flex-2 {
  flex: 2;
}

.form-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.form-input {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md, 6px);
  font-size: 14px;
  background-color: var(--bg-primary, #fff);
  color: var(--text-primary);
}

.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.total-row {
  padding: 12px 16px;
  background-color: var(--bg-hover, #f8fafc);
  border-radius: var(--radius-md, 6px);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.total-row strong {
  font-size: 1.1rem;
}

.fee-info {
  color: var(--text-muted, #94a3b8);
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.error-message {
  padding: 10px 14px;
  background-color: #fef2f2;
  color: #dc2626;
  border-radius: var(--radius-md, 6px);
  font-size: 14px;
  margin-bottom: 16px;
}
</style>
