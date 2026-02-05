import { defineStore } from 'pinia';
import { ref } from 'vue';
import { valuationApi, type PerformanceParams, type CompareRequest } from '@/api';
import type { Valuation, PerformanceSeries, CompareResponse, ValuationMode } from '@/types';

export const useValuationStore = defineStore('valuation', () => {
  const valuations = ref<Record<string, Valuation>>({});
  const performance = ref<Record<string, PerformanceSeries>>({});
  const compareResult = ref<CompareResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchValuation(portfolioId: string, mode: ValuationMode = 'REALTIME') {
    loading.value = true;
    error.value = null;
    try {
      const valuation = await valuationApi.getValuation(portfolioId, mode);
      valuations.value[portfolioId] = valuation;
      return valuation;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch valuation';
      throw e;
    } finally {
      loading.value = false;
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

  function getPerformance(portfolioId: string) {
    return performance.value[portfolioId];
  }

  function clearCompare() {
    compareResult.value = null;
  }

  return {
    valuations,
    performance,
    compareResult,
    loading,
    error,
    fetchValuation,
    fetchPerformance,
    compare,
    getValuation,
    getPerformance,
    clearCompare,
  };
});
