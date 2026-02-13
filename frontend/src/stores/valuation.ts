import { defineStore } from 'pinia';
import { ref } from 'vue';
import { valuationApi, type PerformanceParams, type CompareRequest } from '@/api';
import type { Valuation, PerformanceData, PerformanceSeries, CompareResponse, ValuationMode } from '@/types';

export const useValuationStore = defineStore('valuation', () => {
  const valuations = ref<Record<string, Valuation>>({});
  const performanceData = ref<Record<string, PerformanceData>>({});
  const performance = ref<Record<string, PerformanceSeries>>({});
  const compareResult = ref<CompareResponse | null>(null);
  const loading = ref(false);
  const performanceLoading = ref(false);
  const error = ref<string | null>(null);
  const realtimeActive = ref(false);
  const lastUpdatedAt = ref<Record<string, string>>({});
  const realtimeIntervalMs = ref(30000);
  const realtimeTimer = ref<number | null>(null);
  const realtimePortfolioIds = ref<string[]>([]);

  async function fetchValuation(portfolioId: string, mode: ValuationMode = 'REALTIME') {
    loading.value = true;
    error.value = null;
    try {
      const valuation = await valuationApi.getValuation(portfolioId, mode);
      valuations.value[portfolioId] = valuation;
      lastUpdatedAt.value[portfolioId] = new Date().toISOString();
      return valuation;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch valuation';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchPerformanceData(portfolioId: string, params: PerformanceParams) {
    performanceLoading.value = true;
    error.value = null;
    try {
      const data = await valuationApi.getPerformanceData(portfolioId, params);
      performanceData.value[portfolioId] = data;
      return data;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch performance';
      throw e;
    } finally {
      performanceLoading.value = false;
    }
  }

  async function fetchPerformance(portfolioId: string, params: PerformanceParams) {
    loading.value = true;
    error.value = null;
    try {
      const series = await valuationApi.getPerformance(portfolioId, params);
      performance.value[portfolioId] = series;
      return series;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch performance';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function compare(request: CompareRequest) {
    loading.value = true;
    error.value = null;
    try {
      compareResult.value = await valuationApi.compare(request);
      return compareResult.value;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to compare portfolios';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  function getValuation(portfolioId: string) {
    return valuations.value[portfolioId];
  }

  function getPerformanceData(portfolioId: string) {
    return performanceData.value[portfolioId];
  }

  function getPerformance(portfolioId: string) {
    return performance.value[portfolioId];
  }

  function clearCompare() {
    compareResult.value = null;
  }

  function setRealtimeTargets(portfolioIds: string[]) {
    realtimePortfolioIds.value = [...new Set(portfolioIds)];
  }

  async function refreshRealtimeValuations() {
    if (realtimePortfolioIds.value.length === 0) return;

    await Promise.all(
      realtimePortfolioIds.value.map(async (portfolioId) => {
        try {
          await fetchValuation(portfolioId);
        } catch {
          // Ignore per-portfolio realtime fetch failures
        }
      })
    );
  }

  function startRealtimeUpdates(portfolioIds: string[], intervalMs = 30000) {
    setRealtimeTargets(portfolioIds);
    realtimeIntervalMs.value = intervalMs;

    if (realtimeTimer.value) {
      window.clearInterval(realtimeTimer.value);
      realtimeTimer.value = null;
    }

    realtimeActive.value = true;
    realtimeTimer.value = window.setInterval(() => {
      void refreshRealtimeValuations();
    }, intervalMs);
  }

  function stopRealtimeUpdates() {
    if (realtimeTimer.value) {
      window.clearInterval(realtimeTimer.value);
      realtimeTimer.value = null;
    }
    realtimeActive.value = false;
    realtimePortfolioIds.value = [];
  }

  function getLastUpdatedAt(portfolioId: string) {
    return lastUpdatedAt.value[portfolioId] ?? null;
  }

  return {
    valuations,
    performanceData,
    performance,
    compareResult,
    loading,
    performanceLoading,
    error,
    realtimeActive,
    realtimeIntervalMs,
    lastUpdatedAt,
    fetchValuation,
    fetchPerformanceData,
    fetchPerformance,
    compare,
    getValuation,
    getPerformanceData,
    getPerformance,
    clearCompare,
    startRealtimeUpdates,
    stopRealtimeUpdates,
    setRealtimeTargets,
    refreshRealtimeValuations,
    getLastUpdatedAt,
  };
});
