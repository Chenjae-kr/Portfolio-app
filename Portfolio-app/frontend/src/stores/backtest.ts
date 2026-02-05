import { defineStore } from 'pinia';
import { ref } from 'vue';
import { backtestApi, type RunBacktestRequest } from '@/api';
import type { BacktestConfig, BacktestRun, BacktestResult } from '@/types';

export const useBacktestStore = defineStore('backtest', () => {
  const configs = ref<BacktestConfig[]>([]);
  const runs = ref<BacktestRun[]>([]);
  const currentRun = ref<BacktestRun | null>(null);
  const currentResult = ref<BacktestResult | null>(null);
  const loading = ref(false);
  const polling = ref(false);
  const error = ref<string | null>(null);

  async function fetchConfigs() {
    loading.value = true;
    error.value = null;
    try {
      configs.value = await backtestApi.listConfigs();
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch configs';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function createConfig(config: BacktestConfig) {
    loading.value = true;
    error.value = null;
    try {
      const created = await backtestApi.createConfig(config);
      configs.value.unshift(created);
      return created;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to create config';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function runBacktest(request: RunBacktestRequest) {
    loading.value = true;
    error.value = null;
    try {
      const run = await backtestApi.run(request);
      currentRun.value = run;
      runs.value.unshift(run);
      return run;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to start backtest';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function pollRunStatus(runId: string, intervalMs = 2000): Promise<BacktestRun> {
    polling.value = true;
    error.value = null;

    return new Promise((resolve, reject) => {
      const poll = async () => {
        try {
          const run = await backtestApi.getRun(runId);
          currentRun.value = run;

          // Update in runs list
          const index = runs.value.findIndex((r) => r.id === runId);
          if (index !== -1) runs.value[index] = run;

          if (run.status === 'SUCCEEDED' || run.status === 'FAILED') {
            polling.value = false;
            resolve(run);
          } else {
            setTimeout(poll, intervalMs);
          }
        } catch (e: unknown) {
          polling.value = false;
          error.value = (e as Error).message || 'Failed to poll backtest status';
          reject(e);
        }
      };
      poll();
    });
  }

  async function fetchResults(runId: string) {
    loading.value = true;
    error.value = null;
    try {
      currentResult.value = await backtestApi.getResults(runId);
      return currentResult.value;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch results';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchRuns(configId?: string) {
    loading.value = true;
    error.value = null;
    try {
      runs.value = await backtestApi.listRuns(configId);
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch runs';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  function clearCurrent() {
    currentRun.value = null;
    currentResult.value = null;
  }

  return {
    configs,
    runs,
    currentRun,
    currentResult,
    loading,
    polling,
    error,
    fetchConfigs,
    createConfig,
    runBacktest,
    pollRunStatus,
    fetchResults,
    fetchRuns,
    clearCurrent,
  };
});
