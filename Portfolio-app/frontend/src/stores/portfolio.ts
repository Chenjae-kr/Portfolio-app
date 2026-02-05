import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { portfolioApi, type CreatePortfolioRequest, type UpdatePortfolioRequest } from '@/api';
import type { Portfolio, PortfolioWithTargets, PortfolioTarget } from '@/types';

export const usePortfolioStore = defineStore('portfolio', () => {
  const portfolios = ref<Portfolio[]>([]);
  const currentPortfolio = ref<PortfolioWithTargets | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const portfolioCount = computed(() => portfolios.value.length);

  const portfoliosByGroup = computed(() => {
    const groups: Record<string, Portfolio[]> = { ungrouped: [] };
    portfolios.value.forEach((p) => {
      const key = p.groupId || 'ungrouped';
      if (!groups[key]) groups[key] = [];
      groups[key].push(p);
    });
    return groups;
  });

  async function fetchPortfolios(groupId?: string) {
    loading.value = true;
    error.value = null;
    try {
      portfolios.value = await portfolioApi.list({ groupId, archived: false });
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch portfolios';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchPortfolio(id: string) {
    loading.value = true;
    error.value = null;
    try {
      currentPortfolio.value = await portfolioApi.getById(id);
      return currentPortfolio.value;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to fetch portfolio';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function createPortfolio(data: CreatePortfolioRequest) {
    loading.value = true;
    error.value = null;
    try {
      const portfolio = await portfolioApi.create(data);
      portfolios.value.unshift(portfolio);
      return portfolio;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to create portfolio';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function updatePortfolio(id: string, data: UpdatePortfolioRequest) {
    loading.value = true;
    error.value = null;
    try {
      const updated = await portfolioApi.update(id, data);
      const index = portfolios.value.findIndex((p) => p.id === id);
      if (index !== -1) portfolios.value[index] = updated;
      if (currentPortfolio.value?.id === id) {
        currentPortfolio.value = { ...currentPortfolio.value, ...updated };
      }
      return updated;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to update portfolio';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function deletePortfolio(id: string) {
    loading.value = true;
    error.value = null;
    try {
      await portfolioApi.delete(id);
      portfolios.value = portfolios.value.filter((p) => p.id !== id);
      if (currentPortfolio.value?.id === id) {
        currentPortfolio.value = null;
      }
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to delete portfolio';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function updateTargets(portfolioId: string, targets: PortfolioTarget[], normalize = false) {
    loading.value = true;
    error.value = null;
    try {
      const updated = await portfolioApi.updateTargets(portfolioId, targets, normalize);
      if (currentPortfolio.value?.id === portfolioId) {
        currentPortfolio.value.targets = updated;
      }
      return updated;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Failed to update targets';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  function clearCurrent() {
    currentPortfolio.value = null;
  }

  return {
    portfolios,
    currentPortfolio,
    loading,
    error,
    portfolioCount,
    portfoliosByGroup,
    fetchPortfolios,
    fetchPortfolio,
    createPortfolio,
    updatePortfolio,
    deletePortfolio,
    updateTargets,
    clearCurrent,
  };
});
