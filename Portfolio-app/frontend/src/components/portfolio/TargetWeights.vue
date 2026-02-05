<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { portfolioApi } from '@/api/portfolio';
import { instrumentApi, type Instrument } from '@/api/instrument';
import type { PortfolioTarget, AssetClass } from '@/types';
import { useI18n } from 'vue-i18n';
import { formatPercent } from '@/utils/format';

const props = defineProps<{
  portfolioId: string;
  baseCurrency: string;
}>();

const { t } = useI18n();

const targets = ref<PortfolioTarget[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const searchQuery = ref('');
const searchResults = ref<Instrument[]>([]);
const showSearch = ref(false);

const assetClasses: AssetClass[] = ['EQUITY', 'BOND', 'COMMODITY', 'CASH', 'ALT'];

const totalWeight = computed(() => {
  return targets.value.reduce((sum, t) => sum + Number(t.targetWeight), 0);
});

const isValid = computed(() => {
  const total = totalWeight.value;
  return Math.abs(total - 1.0) < 0.001 && targets.value.every(t => t.targetWeight > 0);
});

async function fetchTargets() {
  loading.value = true;
  error.value = null;
  try {
    const response = await portfolioApi.getTargets(props.portfolioId);
    targets.value = response || [];
  } catch (err: any) {
    error.value = err.message || 'Failed to load targets';
  } finally {
    loading.value = false;
  }
}

async function searchInstruments() {
  if (searchQuery.value.length < 2) {
    searchResults.value = [];
    return;
  }
  
  try {
    const response = await instrumentApi.search({
      q: searchQuery.value,
      size: 10,
    });
    searchResults.value = response.data.content || [];
  } catch (err) {
    console.error('Failed to search instruments:', err);
  }
}

function addTarget(instrument?: Instrument) {
  const newTarget: Partial<PortfolioTarget> = {
    instrumentId: instrument?.id,
    assetClass: instrument?.assetClass as AssetClass || 'EQUITY',
    currency: instrument?.currency || props.baseCurrency,
    targetWeight: 0,
  };
  targets.value.push(newTarget as PortfolioTarget);
  searchQuery.value = '';
  searchResults.value = [];
  showSearch.value = false;
}

function removeTarget(index: number) {
  targets.value.splice(index, 1);
}

function normalize() {
  const total = totalWeight.value;
  if (total > 0) {
    targets.value.forEach(target => {
      target.targetWeight = Number((target.targetWeight / total).toFixed(4));
    });
  }
}

async function saveTargets() {
  saving.value = true;
  error.value = null;
  try {
    const normalize = Math.abs(totalWeight.value - 1.0) >= 0.001;
    await portfolioApi.updateTargets(props.portfolioId, targets.value, normalize);
    alert(t('portfolio.targetsSaved'));
  } catch (err: any) {
    error.value = err.message || 'Failed to save targets';
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  fetchTargets();
});
</script>

<template>
  <div class="target-weights">
    <div class="header">
      <h3>{{ t('portfolio.targetWeights') }}</h3>
      <div class="actions">
        <button class="btn btn-secondary" @click="showSearch = !showSearch">
          + {{ t('portfolio.addTarget') }}
        </button>
        <button 
          class="btn btn-primary" 
          @click="saveTargets"
          :disabled="!isValid || saving"
        >
          {{ saving ? t('common.saving') : t('common.save') }}
        </button>
      </div>
    </div>

    <!-- Search Modal -->
    <div v-if="showSearch" class="search-modal">
      <div class="search-box">
        <input
          v-model="searchQuery"
          type="text"
          :placeholder="t('portfolio.searchInstrument')"
          @input="searchInstruments"
        />
        <button @click="showSearch = false">{{ t('common.cancel') }}</button>
      </div>
      <div class="search-results" v-if="searchResults.length">
        <div 
          v-for="instrument in searchResults"
          :key="instrument.id"
          class="search-result-item"
          @click="addTarget(instrument)"
        >
          <span class="name">{{ instrument.name }}</span>
          <span class="ticker">{{ instrument.ticker }}</span>
          <span class="badge">{{ instrument.assetClass }}</span>
        </div>
      </div>
      <div class="search-empty" v-else-if="searchQuery.length >= 2">
        {{ t('portfolio.noInstrumentsFound') }}
      </div>
    </div>

    <div v-if="loading" class="loading">{{ t('common.loading') }}</div>

    <div v-else-if="error" class="error">{{ error }}</div>

    <div v-else class="targets-list">
      <!-- Total Weight Indicator -->
      <div class="total-weight" :class="{ invalid: !isValid }">
        <span>{{ t('portfolio.totalWeight') }}:</span>
        <strong>{{ formatPercent(totalWeight, 2) }}</strong>
        <button 
          v-if="Math.abs(totalWeight - 1.0) >= 0.001" 
          class="btn btn-sm btn-secondary"
          @click="normalize"
        >
          {{ t('portfolio.normalize') }}
        </button>
      </div>

      <!-- Targets Table -->
      <div class="table-container">
        <table class="targets-table">
          <thead>
            <tr>
              <th>{{ t('portfolio.instrument') }}</th>
              <th>{{ t('portfolio.assetClass') }}</th>
              <th>{{ t('portfolio.currency') }}</th>
              <th>{{ t('portfolio.targetWeight') }}</th>
              <th>{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(target, index) in targets" :key="index">
              <td>
                <input 
                  v-if="!target.instrumentId"
                  type="text"
                  v-model="target.currency"
                  placeholder="Cash"
                  class="input-sm"
                />
                <span v-else>{{ target.instrumentId }}</span>
              </td>
              <td>
                <select v-model="target.assetClass" class="select-sm">
                  <option v-for="ac in assetClasses" :key="ac" :value="ac">
                    {{ ac }}
                  </option>
                </select>
              </td>
              <td>
                <input 
                  v-model="target.currency"
                  type="text"
                  maxlength="3"
                  class="input-sm"
                  style="width: 60px;"
                />
              </td>
              <td>
                <input
                  v-model.number="target.targetWeight"
                  type="number"
                  step="0.01"
                  min="0"
                  max="1"
                  class="input-sm"
                  style="width: 100px;"
                />
              </td>
              <td>
                <button 
                  class="btn btn-sm btn-danger"
                  @click="removeTarget(index)"
                >
                  {{ t('common.delete') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="targets.length === 0" class="empty-state">
          <p>{{ t('portfolio.noTargetsYet') }}</p>
          <button class="btn btn-primary" @click="showSearch = true">
            {{ t('portfolio.addFirstTarget') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.target-weights {
  padding: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header h3 {
  font-size: 1.25rem;
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
}

.search-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  z-index: 1000;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-box input {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.search-results {
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-result-item:hover {
  background: var(--bg-secondary);
}

.search-result-item .name {
  flex: 1;
  font-weight: 500;
}

.search-result-item .ticker {
  color: var(--text-secondary);
  font-size: 13px;
}

.search-empty {
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
}

.total-weight {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-secondary);
  border: 2px solid var(--success-color);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  font-size: 1.125rem;
}

.total-weight.invalid {
  border-color: var(--danger-color);
  background: rgba(var(--danger-rgb), 0.05);
}

.total-weight strong {
  font-size: 1.25rem;
  color: var(--success-color);
}

.total-weight.invalid strong {
  color: var(--danger-color);
}

.table-container {
  overflow-x: auto;
}

.targets-table {
  width: 100%;
  border-collapse: collapse;
}

.targets-table th,
.targets-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.targets-table th {
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-secondary);
}

.input-sm,
.select-sm {
  padding: 6px 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 14px;
}

.empty-state {
  padding: 60px 24px;
  text-align: center;
  color: var(--text-secondary);
}

.empty-state p {
  margin-bottom: 16px;
}

.loading,
.error {
  padding: 40px;
  text-align: center;
}

.error {
  color: var(--danger-color);
}
</style>
