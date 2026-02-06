<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useBacktestStore } from '@/stores';
import type { RebalanceType, RebalancePeriod, PriceMode, PortfolioTarget, AssetClass } from '@/types';
import { formatCurrency } from '@/utils/format';

const router = useRouter();
const backtestStore = useBacktestStore();

// Form state
const name = ref('My Backtest');
const startDate = ref('2020-01-01');
const endDate = ref(new Date().toISOString().split('T')[0]);
const initialCapital = ref(100000000);
const rebalanceType = ref<RebalanceType>('PERIODIC');
const rebalancePeriod = ref<RebalancePeriod>('QUARTERLY');
const bandThreshold = ref(0.05);
const dividendReinvest = ref(true);
const priceMode = ref<PriceMode>('ADJ_CLOSE');

// Targets (simplified for now)
const targets = ref<(PortfolioTarget & { name: string })[]>([
  { id: '1', instrumentId: 'SPY', name: 'S&P 500 ETF', assetClass: 'EQUITY', targetWeight: 0.6 },
  { id: '2', instrumentId: 'AGG', name: 'US Bonds ETF', assetClass: 'BOND', targetWeight: 0.4 },
]);

const error = ref('');
const step = ref(1);

const totalWeight = computed(() =>
  targets.value.reduce((sum, t) => sum + t.targetWeight, 0)
);

const isWeightValid = computed(() =>
  Math.abs(totalWeight.value - 1) < 0.001
);

const rebalanceTypes: { value: RebalanceType; label: string }[] = [
  { value: 'NONE', label: 'No Rebalancing' },
  { value: 'PERIODIC', label: 'Periodic Rebalancing' },
  { value: 'BAND', label: 'Band Rebalancing' },
];

const rebalancePeriods: { value: RebalancePeriod; label: string }[] = [
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'QUARTERLY', label: 'Quarterly' },
  { value: 'SEMI_ANNUAL', label: 'Semi-Annual' },
  { value: 'ANNUAL', label: 'Annual' },
];

function addTarget() {
  targets.value.push({
    id: Date.now().toString(),
    instrumentId: '',
    name: '',
    assetClass: 'EQUITY' as AssetClass,
    targetWeight: 0,
  });
}

function removeTarget(index: number) {
  targets.value.splice(index, 1);
}

async function runBacktest() {
  if (!isWeightValid.value) {
    error.value = 'Target weights must sum to 100%';
    return;
  }

  error.value = '';

  try {
    const run = await backtestStore.runBacktest({
      inlineConfig: {
        name: name.value,
        startDate: startDate.value,
        endDate: endDate.value,
        initialCapitalBase: initialCapital.value,
        rebalanceType: rebalanceType.value,
        rebalancePeriod: rebalanceType.value === 'PERIODIC' ? rebalancePeriod.value : undefined,
        bandThreshold: rebalanceType.value === 'BAND' ? bandThreshold.value : undefined,
        dividendReinvest: dividendReinvest.value,
        priceMode: priceMode.value,
        targets: targets.value.map(t => ({
          instrumentId: t.instrumentId,
          assetClass: t.assetClass,
          targetWeight: t.targetWeight,
        })),
      },
    });

    // Poll for completion
    const completedRun = await backtestStore.pollRunStatus(run.id);

    if (completedRun.status === 'SUCCEEDED') {
      router.push(`/backtest/${run.id}`);
    } else {
      error.value = completedRun.errorMessage || 'Backtest failed';
    }
  } catch (e: unknown) {
    error.value = (e as Error).message || 'Failed to run backtest';
  }
}
</script>

<template>
  <div class="backtest-view">
    <header class="page-header">
      <h1>Backtest Studio</h1>
      <p>Test your investment strategies with historical data</p>
    </header>

    <div class="backtest-form card">
      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <!-- Step 1: Basic Settings -->
      <section class="form-section">
        <h3>1. Basic Settings</h3>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Backtest Name</label>
            <input v-model="name" type="text" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">Initial Capital</label>
            <input v-model.number="initialCapital" type="number" class="form-input" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Start Date</label>
            <input v-model="startDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">End Date</label>
            <input v-model="endDate" type="date" class="form-input" />
          </div>
        </div>
      </section>

      <!-- Step 2: Asset Allocation -->
      <section class="form-section">
        <h3>2. Asset Allocation</h3>
        <p class="section-hint">Define your target portfolio weights (must sum to 100%)</p>

        <div class="targets-list">
          <div v-for="(target, index) in targets" :key="target.id" class="target-row">
            <input
              v-model="target.instrumentId"
              type="text"
              class="form-input"
              placeholder="Ticker (e.g., SPY)"
            />
            <input
              v-model="target.name"
              type="text"
              class="form-input"
              placeholder="Name"
            />
            <div class="weight-input">
              <input
                v-model.number="target.targetWeight"
                type="number"
                class="form-input"
                step="0.01"
                min="0"
                max="1"
              />
              <span class="weight-percent">{{ (target.targetWeight * 100).toFixed(0) }}%</span>
            </div>
            <button class="btn-icon" @click="removeTarget(index)" title="Remove">
              &times;
            </button>
          </div>
        </div>

        <div class="targets-footer">
          <button class="btn btn-secondary" @click="addTarget">+ Add Asset</button>
          <span class="weight-total" :class="{ invalid: !isWeightValid }">
            Total: {{ (totalWeight * 100).toFixed(1) }}%
          </span>
        </div>
      </section>

      <!-- Step 3: Rebalancing -->
      <section class="form-section">
        <h3>3. Rebalancing Strategy</h3>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Rebalancing Type</label>
            <select v-model="rebalanceType" class="form-input">
              <option v-for="r in rebalanceTypes" :key="r.value" :value="r.value">
                {{ r.label }}
              </option>
            </select>
          </div>

          <div class="form-group" v-if="rebalanceType === 'PERIODIC'">
            <label class="form-label">Rebalancing Period</label>
            <select v-model="rebalancePeriod" class="form-input">
              <option v-for="p in rebalancePeriods" :key="p.value" :value="p.value">
                {{ p.label }}
              </option>
            </select>
          </div>

          <div class="form-group" v-if="rebalanceType === 'BAND'">
            <label class="form-label">Band Threshold</label>
            <input
              v-model.number="bandThreshold"
              type="number"
              class="form-input"
              step="0.01"
              min="0.01"
              max="0.5"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="dividendReinvest" />
              <span>Reinvest Dividends</span>
            </label>
          </div>
        </div>
      </section>

      <!-- Actions -->
      <div class="form-actions">
        <button
          class="btn btn-primary btn-lg"
          :disabled="!isWeightValid || backtestStore.loading || backtestStore.polling"
          @click="runBacktest"
        >
          <span v-if="backtestStore.loading || backtestStore.polling" class="spinner"></span>
          <span v-else>Run Backtest</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.backtest-view {
  max-width: 800px;
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

.backtest-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.error-message {
  padding: 12px;
  background-color: var(--danger-light);
  color: var(--danger-color);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.form-section h3 {
  margin-bottom: 8px;
}

.section-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.targets-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.target-row {
  display: grid;
  grid-template-columns: 120px 1fr 140px 40px;
  gap: 12px;
  align-items: center;
}

.weight-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weight-input input {
  width: 80px;
}

.weight-percent {
  font-size: 13px;
  color: var(--text-muted);
  min-width: 40px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border: 1px solid var(--border-color);
  background: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 18px;
  color: var(--text-muted);
  transition: all 0.2s;
}

.btn-icon:hover {
  border-color: var(--danger-color);
  color: var(--danger-color);
}

.targets-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.weight-total {
  font-weight: 500;
}

.weight-total.invalid {
  color: var(--danger-color);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  accent-color: var(--primary-color);
}

.form-actions {
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: center;
}

.btn-lg {
  padding: 14px 32px;
  font-size: 16px;
}
</style>
