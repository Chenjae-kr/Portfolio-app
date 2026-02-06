<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useBacktestStore } from '@/stores';
import type { RebalanceType, RebalancePeriod, PriceMode, AssetClass, InvestmentType, DcaFrequency } from '@/types';
import { useI18n } from 'vue-i18n';

const router = useRouter();
const backtestStore = useBacktestStore();
const { t } = useI18n();

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
const investmentType = ref<InvestmentType>('LUMP_SUM');
const dcaAmount = ref(1000000);
const dcaFrequency = ref<DcaFrequency>('MONTHLY');

interface TargetRow {
  id: string;
  instrumentId: string;
  name: string;
  assetClass: AssetClass;
  targetWeight: number;
}

// Default targets: 60/40 portfolio
const targets = ref<TargetRow[]>([
  { id: '1', instrumentId: 'SPY', name: 'S&P 500 ETF', assetClass: 'EQUITY', targetWeight: 0.6 },
  { id: '2', instrumentId: 'BND', name: 'US Total Bond', assetClass: 'BOND', targetWeight: 0.4 },
]);

const error = ref('');

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

const dcaFrequencies: { value: DcaFrequency; label: string }[] = [
  { value: 'MONTHLY', label: t('backtest.frequencyMonthly') },
  { value: 'QUARTERLY', label: t('backtest.frequencyQuarterly') },
  { value: 'SEMI_ANNUAL', label: t('backtest.frequencySemiAnnual') },
  { value: 'ANNUAL', label: t('backtest.frequencyAnnual') },
];

const initialCapitalLabel = computed(() =>
  investmentType.value === 'DCA'
    ? t('backtest.initialCapitalDCA')
    : t('backtest.initialCapital')
);

// 종목 프리셋
const presets = [
  { instrumentId: 'SPY', name: 'S&P 500 ETF', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'QQQ', name: 'Nasdaq 100 ETF', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'VOO', name: 'Vanguard S&P 500', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'VTI', name: 'US Total Market', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'BND', name: 'US Total Bond', assetClass: 'BOND' as AssetClass },
  { instrumentId: 'TLT', name: '20+ Year Treasury', assetClass: 'BOND' as AssetClass },
  { instrumentId: '005930', name: 'Samsung Electronics', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'AAPL', name: 'Apple Inc.', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'MSFT', name: 'Microsoft Corp.', assetClass: 'EQUITY' as AssetClass },
  { instrumentId: 'NVDA', name: 'NVIDIA Corp.', assetClass: 'EQUITY' as AssetClass },
];

function addTarget() {
  targets.value.push({
    id: Date.now().toString(),
    instrumentId: '',
    name: '',
    assetClass: 'EQUITY',
    targetWeight: 0,
  });
}

function removeTarget(index: number) {
  targets.value.splice(index, 1);
}

function onInstrumentSelect(target: TargetRow) {
  const preset = presets.find(p => p.instrumentId === target.instrumentId);
  if (preset) {
    target.name = preset.name;
    target.assetClass = preset.assetClass;
  }
}

async function runBacktest() {
  if (!isWeightValid.value) {
    error.value = t('backtest.weightError');
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
        investmentType: investmentType.value,
        dcaAmount: investmentType.value === 'DCA' ? dcaAmount.value : undefined,
        dcaFrequency: investmentType.value === 'DCA' ? dcaFrequency.value : undefined,
        targets: targets.value.map(t => ({
          instrumentId: t.instrumentId,
          assetClass: t.assetClass,
          targetWeight: t.targetWeight,
        })),
      },
    });

    // 동기 실행이므로 이미 결과가 있음
    if (run.status === 'SUCCEEDED') {
      router.push(`/backtest/${run.id}`);
    } else if (run.status === 'RUNNING') {
      // 비동기인 경우 polling
      const completedRun = await backtestStore.pollRunStatus(run.id);
      if (completedRun.status === 'SUCCEEDED') {
        router.push(`/backtest/${run.id}`);
      } else {
        error.value = completedRun.errorMessage || t('backtest.failed');
      }
    } else {
      error.value = run.errorMessage || t('backtest.failed');
    }
  } catch (e: unknown) {
    error.value = (e as Error).message || t('backtest.failed');
  }
}
</script>

<template>
  <div class="backtest-view">
    <header class="page-header">
      <h1>{{ t('backtest.title') }}</h1>
      <p>{{ t('backtest.subtitle') }}</p>
    </header>

    <div class="backtest-form card">
      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <!-- Step 1: Basic Settings -->
      <section class="form-section">
        <h3>{{ t('backtest.basicSettings') }}</h3>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ t('backtest.backtestName') }}</label>
            <input v-model="name" type="text" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">{{ initialCapitalLabel }}</label>
            <input v-model.number="initialCapital" type="number" class="form-input" min="0" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ t('backtest.startDate') }}</label>
            <input v-model="startDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('backtest.endDate') }}</label>
            <input v-model="endDate" type="date" class="form-input" />
          </div>
        </div>

        <!-- 투자 방식: 일시불 / 적립식 (기본 설정 카드 안, 종료일 아래) -->
        <h4 class="sub-section-title">{{ t('backtest.investmentStrategy') }}</h4>
        <div class="form-row investment-type-row">
          <div class="form-group">
            <label class="form-label">{{ t('backtest.investmentTypeLabel') }}</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" v-model="investmentType" value="LUMP_SUM" />
                <span>{{ t('backtest.lumpSum') }}</span>
              </label>
              <label class="radio-label">
                <input type="radio" v-model="investmentType" value="DCA" />
                <span>{{ t('backtest.dca') }}</span>
              </label>
            </div>
          </div>
        </div>

        <!-- 적립 금액·적립 주기 (적립식 선택 시에만 표시) -->
        <div v-if="investmentType === 'DCA'" class="form-row">
          <div class="form-group">
            <label class="form-label">{{ t('backtest.dcaAmount') }}</label>
            <input
              v-model.number="dcaAmount"
              type="number"
              class="form-input"
              min="0"
              :placeholder="t('backtest.dcaAmount')"
            />
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('backtest.dcaFrequencyLabel') }}</label>
            <select v-model="dcaFrequency" class="form-input">
              <option v-for="f in dcaFrequencies" :key="f.value" :value="f.value">
                {{ f.label }}
              </option>
            </select>
          </div>
        </div>

        <p v-if="investmentType === 'DCA'" class="section-hint">
          {{ t('backtest.dcaHint') }}
        </p>
      </section>

      <!-- Step 2 라벨: 투자 방식 (기존 Step 2 내용은 Step 1로 통합됨) -->
      <!-- Step 3: Asset Allocation -->
      <section class="form-section">
        <h3>{{ t('backtest.assetAllocation') }}</h3>
        <p class="section-hint">{{ t('backtest.allocationHint') }}</p>

        <div class="targets-list">
          <div v-for="(target, index) in targets" :key="target.id" class="target-row">
            <select
              v-model="target.instrumentId"
              class="form-input ticker-select"
              @change="onInstrumentSelect(target)"
            >
              <option value="" disabled>{{ t('backtest.selectInstrument') }}</option>
              <option v-for="p in presets" :key="p.instrumentId" :value="p.instrumentId">
                {{ p.instrumentId }} - {{ p.name }}
              </option>
            </select>
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
          <button class="btn btn-secondary" @click="addTarget">+ {{ t('backtest.addAsset') }}</button>
          <span class="weight-total" :class="{ invalid: !isWeightValid }">
            {{ t('backtest.total') }}: {{ (totalWeight * 100).toFixed(1) }}%
          </span>
        </div>
      </section>

      <!-- Step 4: Rebalancing -->
      <section class="form-section">
        <h3>{{ t('backtest.rebalancing') }}</h3>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ t('backtest.rebalanceType') }}</label>
            <select v-model="rebalanceType" class="form-input">
              <option v-for="r in rebalanceTypes" :key="r.value" :value="r.value">
                {{ r.label }}
              </option>
            </select>
          </div>

          <div class="form-group" v-if="rebalanceType === 'PERIODIC'">
            <label class="form-label">{{ t('backtest.rebalancePeriod') }}</label>
            <select v-model="rebalancePeriod" class="form-input">
              <option v-for="p in rebalancePeriods" :key="p.value" :value="p.value">
                {{ p.label }}
              </option>
            </select>
          </div>

          <div class="form-group" v-if="rebalanceType === 'BAND'">
            <label class="form-label">{{ t('backtest.bandThreshold') }}</label>
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
              <span>{{ t('backtest.reinvestDividends') }}</span>
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
          <span v-else>{{ t('backtest.run') }}</span>
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

.sub-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 24px 0 12px;
  padding-bottom: 4px;
}

.section-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.field-hint {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
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
  grid-template-columns: 1fr 140px 40px;
  gap: 12px;
  align-items: center;
}

.ticker-select {
  min-width: 0;
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

.radio-group {
  display: flex;
  gap: 24px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-label input {
  accent-color: var(--primary-color);
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
