<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useBacktestStore } from '@/stores';
import type { BacktestRun } from '@/types';
import { useI18n } from 'vue-i18n';
import dayjs from 'dayjs';

const router = useRouter();
const backtestStore = useBacktestStore();
const { t } = useI18n();

const runs = ref<BacktestRun[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

onMounted(async () => {
  await fetchRuns();
});

async function fetchRuns() {
  loading.value = true;
  error.value = null;
  try {
    await backtestStore.fetchRuns();
    runs.value = backtestStore.runs;
  } catch (e: unknown) {
    error.value = (e as Error).message || t('backtest.history.loadFailed');
  } finally {
    loading.value = false;
  }
}

function getStatusClass(status: string) {
  switch (status) {
    case 'SUCCEEDED':
      return 'status-success';
    case 'FAILED':
      return 'status-failed';
    case 'RUNNING':
      return 'status-running';
    default:
      return '';
  }
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'SUCCEEDED':
      return t('backtest.history.statusSucceeded');
    case 'FAILED':
      return t('backtest.history.statusFailed');
    case 'RUNNING':
      return t('backtest.history.statusRunning');
    default:
      return status;
  }
}

function formatDate(dateStr: string) {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss');
}

function viewResult(runId: string) {
  router.push(`/backtest/${runId}`);
}
</script>

<template>
  <div class="backtest-history-view">
    <header class="page-header">
      <div class="header-content">
        <div>
          <h1>{{ t('backtest.history.title') }}</h1>
          <p>{{ t('backtest.history.subtitle') }}</p>
        </div>
        <button class="btn btn-primary" @click="router.push('/backtest')">
          {{ t('backtest.history.newBacktest') }}
        </button>
      </div>
    </header>

    <div class="history-content card">
      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>{{ t('backtest.history.loading') }}</p>
      </div>

      <div v-else-if="runs.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <line x1="9" y1="3" x2="9" y2="21" />
            <line x1="3" y1="9" x2="21" y2="9" />
          </svg>
        </div>
        <h3>{{ t('backtest.history.noHistory') }}</h3>
        <p>{{ t('backtest.history.noHistoryDesc') }}</p>
        <button class="btn btn-primary" @click="router.push('/backtest')">
          {{ t('backtest.history.createFirst') }}
        </button>
      </div>

      <div v-else class="runs-table">
        <table>
          <thead>
            <tr>
              <th>{{ t('backtest.history.name') }}</th>
              <th>{{ t('backtest.history.status') }}</th>
              <th>{{ t('backtest.history.startedAt') }}</th>
              <th>{{ t('backtest.history.finishedAt') }}</th>
              <th>{{ t('backtest.history.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in runs" :key="run.id" class="run-row">
              <td>
                <div class="run-name">
                  <span class="run-id">#{{ run.id.substring(0, 8) }}</span>
                  <span v-if="run.configId" class="config-id">{{ run.configId.substring(0, 8) }}</span>
                </div>
              </td>
              <td>
                <span :class="['status-badge', getStatusClass(run.status)]">
                  {{ getStatusLabel(run.status) }}
                </span>
              </td>
              <td class="date-cell">{{ formatDate(run.startedAt) }}</td>
              <td class="date-cell">
                {{ run.finishedAt ? formatDate(run.finishedAt) : '-' }}
              </td>
              <td>
                <div class="actions">
                  <button
                    v-if="run.status === 'SUCCEEDED'"
                    class="btn btn-sm btn-primary"
                    @click="viewResult(run.id)"
                  >
                    {{ t('backtest.history.viewResult') }}
                  </button>
                  <button
                    v-else-if="run.status === 'FAILED'"
                    class="btn btn-sm btn-secondary"
                    @click="viewResult(run.id)"
                  >
                    {{ t('backtest.history.viewError') }}
                  </button>
                  <span v-else class="text-muted">{{ t('backtest.history.running') }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.backtest-history-view {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h1 {
  margin-bottom: 8px;
}

.page-header p {
  color: var(--text-secondary);
}

.history-content {
  padding: 24px;
}

.error-message {
  padding: 12px;
  background-color: var(--danger-light);
  color: var(--danger-color);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  gap: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-state h3 {
  margin-bottom: 8px;
}

.empty-state p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.runs-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background-color: var(--bg-secondary);
  border-bottom: 2px solid var(--border-color);
}

th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

td {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.run-row:hover {
  background-color: var(--bg-secondary);
}

.run-name {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.run-id {
  font-weight: 500;
  font-family: var(--font-mono);
  font-size: 14px;
}

.config-id {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-success {
  background-color: var(--success-light);
  color: var(--success-color);
}

.status-failed {
  background-color: var(--danger-light);
  color: var(--danger-color);
}

.status-running {
  background-color: var(--info-light);
  color: var(--info-color);
}

.date-cell {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-secondary);
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
  }

  .runs-table {
    font-size: 14px;
  }

  th,
  td {
    padding: 10px 8px;
  }

  .run-name {
    min-width: 120px;
  }
}
</style>
