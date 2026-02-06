<script setup lang="ts">
import { onMounted, computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { usePortfolioStore, useValuationStore } from '@/stores';
import { valuationApi, transactionApi } from '@/api';
import type { Transaction, RebalanceAnalysis } from '@/types';
import PortfolioCard from '@/components/portfolio/PortfolioCard.vue';
import { formatCurrency, formatPercent, formatDate, getChangeClass } from '@/utils/format';
import { useI18n } from 'vue-i18n';

const router = useRouter();
const portfolioStore = usePortfolioStore();
const valuationStore = useValuationStore();
const { t } = useI18n();

const totalValue = computed(() => {
  return Object.values(valuationStore.valuations).reduce(
    (sum, v) => sum + v.totalValueBase,
    0
  );
});

const totalDayPnl = computed(() => {
  return Object.values(valuationStore.valuations).reduce(
    (sum, v) => sum + v.dayPnlBase,
    0
  );
});

const totalPnl = computed(() => {
  return Object.values(valuationStore.valuations).reduce(
    (sum, v) => sum + v.totalPnlBase,
    0
  );
});

// Rebalancing alerts
const rebalanceAlerts = ref<Array<{ portfolioId: string; name: string; maxDrift: number }>>([]);
const rebalanceLoading = ref(false);

// Recent transactions
const recentTransactions = ref<Array<Transaction & { portfolioName: string }>>([]);
const transactionsLoading = ref(false);

// Top/Worst performers
const sortedByReturn = computed(() => {
  return portfolioStore.portfolios
    .map(p => {
      const v = valuationStore.getValuation(p.id);
      if (!v) return { portfolio: p, returnPct: 0, hasValuation: false };
      const costBasis = v.totalValueBase - v.totalPnlBase;
      const returnPct = costBasis > 0 ? v.totalPnlBase / costBasis : 0;
      return { portfolio: p, returnPct, hasValuation: true };
    })
    .filter(item => item.hasValuation)
    .sort((a, b) => b.returnPct - a.returnPct);
});

const topPerformers = computed(() => sortedByReturn.value.slice(0, 3));
const worstPerformers = computed(() => {
  const sorted = [...sortedByReturn.value].reverse();
  return sorted.slice(0, 3);
});

const typeLabels: Record<string, string> = {
  BUY: 'BUY', SELL: 'SELL', DEPOSIT: 'DEPOSIT', WITHDRAW: 'WITHDRAW',
  DIVIDEND: 'DIVIDEND', INTEREST: 'INTEREST', FEE: 'FEE', TAX: 'TAX',
};

function getTypeBadgeClass(type: string): string {
  if (type === 'BUY') return 'badge-buy';
  if (type === 'SELL') return 'badge-sell';
  if (type === 'DEPOSIT') return 'badge-deposit';
  if (type === 'WITHDRAW') return 'badge-withdraw';
  if (type === 'DIVIDEND') return 'badge-dividend';
  return '';
}

function getMainLeg(tx: Transaction) {
  return tx.legs.find(l => l.legType === 'ASSET') || tx.legs.find(l => l.legType === 'CASH') || tx.legs[0];
}

onMounted(async () => {
  await portfolioStore.fetchPortfolios();

  // Fetch valuations for all portfolios
  for (const portfolio of portfolioStore.portfolios) {
    try {
      await valuationStore.fetchValuation(portfolio.id);
    } catch {
      // Ignore errors for individual portfolios
    }
  }

  // Fetch rebalance data for all portfolios (parallel, non-blocking)
  rebalanceLoading.value = true;
  const rebalancePromises = portfolioStore.portfolios.map(async (portfolio) => {
    try {
      const analysis: RebalanceAnalysis = await valuationApi.getRebalance(portfolio.id);
      if (analysis.needsRebalancing && analysis.maxDrift > 0.05) {
        rebalanceAlerts.value.push({
          portfolioId: portfolio.id,
          name: portfolio.name,
          maxDrift: analysis.maxDrift,
        });
      }
    } catch { /* ignore */ }
  });
  Promise.all(rebalancePromises).finally(() => { rebalanceLoading.value = false; });

  // Fetch recent transactions from all portfolios (parallel, non-blocking)
  transactionsLoading.value = true;
  const allTransactions: Array<Transaction & { portfolioName: string }> = [];
  const txPromises = portfolioStore.portfolios.map(async (portfolio) => {
    try {
      const txs = await transactionApi.list(portfolio.id, { size: 5 });
      txs.forEach(tx => allTransactions.push({ ...tx, portfolioName: portfolio.name }));
    } catch { /* ignore */ }
  });
  Promise.all(txPromises).then(() => {
    recentTransactions.value = allTransactions
      .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())
      .slice(0, 5);
  }).finally(() => { transactionsLoading.value = false; });
});

function navigateToPortfolio(id: string) {
  router.push(`/portfolio/${id}`);
}
</script>

<template>
  <div class="dashboard">
    <!-- Summary Section -->
    <section class="summary-section">
      <div class="summary-cards">
        <div class="summary-card">
          <span class="summary-label">{{ t('dashboard.totalAssets') }}</span>
          <span class="summary-value">{{ formatCurrency(totalValue, 'KRW') }}</span>
        </div>
        <div class="summary-card">
          <span class="summary-label">{{ t('dashboard.todayPnl') }}</span>
          <span
            class="summary-value"
            :class="{ 'number-positive': totalDayPnl > 0, 'number-negative': totalDayPnl < 0 }"
          >
            {{ formatCurrency(totalDayPnl, 'KRW', true) }}
          </span>
        </div>
        <div class="summary-card">
          <span class="summary-label">{{ t('dashboard.totalPnl') }}</span>
          <span
            class="summary-value"
            :class="{ 'number-positive': totalPnl > 0, 'number-negative': totalPnl < 0 }"
          >
            {{ formatCurrency(totalPnl, 'KRW', true) }}
          </span>
        </div>
      </div>
    </section>

    <!-- Rebalancing Alerts -->
    <section class="alerts-section" v-if="!rebalanceLoading && rebalanceAlerts.length > 0">
      <div class="section-header">
        <h2>{{ t('dashboard.rebalanceAlerts') }}</h2>
      </div>
      <div class="alert-cards">
        <div
          v-for="alert in rebalanceAlerts"
          :key="alert.portfolioId"
          class="alert-card"
          @click="navigateToPortfolio(alert.portfolioId)"
        >
          <div class="alert-icon">&#9888;</div>
          <div class="alert-content">
            <span class="alert-name">{{ alert.name }}</span>
            <span class="alert-drift">
              {{ t('rebalance.maxDrift') }}: {{ formatPercent(alert.maxDrift) }}
            </span>
          </div>
          <span class="alert-action">{{ t('dashboard.viewRebalance') }} &rarr;</span>
        </div>
      </div>
    </section>

    <!-- Top / Worst Performers -->
    <section class="performers-section" v-if="sortedByReturn.length > 0">
      <div class="performers-grid">
        <div class="performers-card">
          <h3>{{ t('dashboard.topPerformers') }}</h3>
          <div class="performer-list">
            <div
              v-for="item in topPerformers"
              :key="item.portfolio.id"
              class="performer-item"
              @click="navigateToPortfolio(item.portfolio.id)"
            >
              <span class="performer-name">{{ item.portfolio.name }}</span>
              <span class="performer-return number-positive">
                {{ formatPercent(item.returnPct, 2, true) }}
              </span>
            </div>
          </div>
        </div>
        <div class="performers-card">
          <h3>{{ t('dashboard.worstPerformers') }}</h3>
          <div class="performer-list">
            <div
              v-for="item in worstPerformers"
              :key="item.portfolio.id"
              class="performer-item"
              @click="navigateToPortfolio(item.portfolio.id)"
            >
              <span class="performer-name">{{ item.portfolio.name }}</span>
              <span class="performer-return" :class="getChangeClass(item.returnPct)">
                {{ formatPercent(item.returnPct, 2, true) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Recent Transactions -->
    <section class="transactions-section" v-if="portfolioStore.portfolios.length > 0">
      <div class="section-header">
        <h2>{{ t('dashboard.recentTransactions') }}</h2>
      </div>
      <div v-if="transactionsLoading" class="loading-state mini">
        <div class="spinner"></div>
      </div>
      <div v-else-if="recentTransactions.length === 0" class="empty-mini">
        <p>{{ t('dashboard.noRecentTransactions') }}</p>
      </div>
      <div v-else class="recent-tx-container">
        <table class="table recent-tx-table">
          <thead>
            <tr>
              <th>{{ t('transaction.date') }}</th>
              <th>{{ t('dashboard.portfolios') }}</th>
              <th>{{ t('transaction.type') }}</th>
              <th>{{ t('transaction.instrument') }}</th>
              <th class="text-right">{{ t('transaction.amount') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tx in recentTransactions" :key="tx.id">
              <td class="text-muted">{{ formatDate(tx.occurredAt) }}</td>
              <td>{{ tx.portfolioName }}</td>
              <td><span class="type-badge" :class="getTypeBadgeClass(tx.type)">{{ typeLabels[tx.type] || tx.type }}</span></td>
              <td>{{ getMainLeg(tx)?.instrumentId || '-' }}</td>
              <td class="text-right font-mono">
                {{ formatCurrency(Math.abs(getMainLeg(tx)?.amount || 0), 'KRW') }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Portfolios Section -->
    <section class="portfolios-section">
      <div class="section-header">
        <h2>{{ t('dashboard.portfolios') }}</h2>
        <RouterLink to="/portfolio/new" class="btn btn-primary">
          + {{ t('dashboard.newPortfolio') }}
        </RouterLink>
      </div>

      <div v-if="portfolioStore.loading" class="loading-state">
        <div class="spinner"></div>
        <span>{{ t('dashboard.loadingPortfolios') }}</span>
      </div>

      <div v-else-if="portfolioStore.portfolios.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
            <polyline points="3.27,6.96 12,12.01 20.73,6.96" />
            <line x1="12" y1="22.08" x2="12" y2="12" />
          </svg>
        </div>
        <h3>{{ t('dashboard.noPortfoliosYet') }}</h3>
        <p>{{ t('dashboard.createFirstPortfolio') }}</p>
        <RouterLink to="/portfolio/new" class="btn btn-primary">
          {{ t('dashboard.createPortfolio') }}
        </RouterLink>
      </div>

      <div v-else class="portfolios-grid">
        <PortfolioCard
          v-for="portfolio in portfolioStore.portfolios"
          :key="portfolio.id"
          :portfolio="portfolio"
          :valuation="valuationStore.getValuation(portfolio.id)"
          @click="navigateToPortfolio(portfolio.id)"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.summary-section {
  margin-bottom: 32px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.summary-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  font-size: 13px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-value {
  font-size: 1.5rem;
  font-weight: 600;
}

/* Rebalancing Alerts */
.alerts-section { margin-bottom: 32px; }
.alert-cards { display: flex; flex-direction: column; gap: 8px; }
.alert-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px;
  background-color: #fffbeb; border: 1px solid #fde68a;
  border-radius: var(--radius-lg); cursor: pointer; transition: all 0.2s;
}
.alert-card:hover { border-color: #f59e0b; }
.alert-icon { font-size: 20px; color: #f59e0b; }
.alert-content { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.alert-name { font-weight: 600; font-size: 14px; color: #92400e; }
.alert-drift { font-size: 13px; color: #b45309; }
.alert-action { font-size: 13px; color: #d97706; font-weight: 500; }

/* Top/Worst Performers */
.performers-section { margin-bottom: 32px; }
.performers-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.performers-card {
  background-color: var(--bg-secondary); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 20px;
}
.performers-card h3 {
  font-size: 14px; color: var(--text-secondary); margin-bottom: 16px;
  text-transform: uppercase; letter-spacing: 0.05em;
}
.performer-list { display: flex; flex-direction: column; gap: 12px; }
.performer-item {
  display: flex; justify-content: space-between; align-items: center;
  cursor: pointer; padding: 4px 0;
}
.performer-item:hover { opacity: 0.8; }
.performer-name { font-weight: 500; }
.performer-return { font-weight: 600; font-family: var(--font-mono, monospace); }

/* Recent Transactions */
.transactions-section { margin-bottom: 32px; }
.recent-tx-container {
  background-color: var(--bg-secondary); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); overflow: hidden;
}
.recent-tx-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.recent-tx-table th {
  padding: 12px 16px; text-align: left; font-size: 12px;
  color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.05em;
  border-bottom: 1px solid var(--border-color);
}
.recent-tx-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-color); }
.recent-tx-table tr:last-child td { border-bottom: none; }
.text-muted { color: var(--text-secondary); }
.text-right { text-align: right; }
.font-mono { font-family: var(--font-mono, monospace); }
.type-badge {
  font-weight: 600; font-size: 11px; text-transform: uppercase;
  padding: 2px 8px; border-radius: 4px;
}
.badge-buy { background-color: #dcfce7; color: #166534; }
.badge-sell { background-color: #fee2e2; color: #991b1b; }
.badge-deposit { background-color: #dbeafe; color: #1e40af; }
.badge-withdraw { background-color: #fef3c7; color: #92400e; }
.badge-dividend { background-color: #f3e8ff; color: #6b21a8; }

.empty-mini {
  padding: 24px; text-align: center; color: var(--text-muted);
  background-color: var(--bg-secondary); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}
.loading-state.mini { padding: 24px; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 1.25rem;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  text-align: center;
}

.loading-state {
  gap: 16px;
  color: var(--text-secondary);
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 1.125rem;
  margin-bottom: 8px;
}

.empty-state p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.portfolios-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
