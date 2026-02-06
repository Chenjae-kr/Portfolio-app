<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { usePortfolioStore } from '@/stores';
import type { PortfolioType } from '@/types';
import { useI18n } from 'vue-i18n';

const router = useRouter();
const portfolioStore = usePortfolioStore();
const { t } = useI18n();

const name = ref('');
const description = ref('');
const baseCurrency = ref('KRW');
const type = ref<PortfolioType>('REAL');
const error = ref('');

const currencies = ['KRW', 'USD', 'EUR', 'JPY'];
const portfolioTypes = computed(() => [
  { value: 'REAL' as PortfolioType, label: t('portfolio.types.real'), description: t('portfolio.types.realDesc') },
  { value: 'HYPOTHETICAL' as PortfolioType, label: t('portfolio.types.hypothetical'), description: t('portfolio.types.hypotheticalDesc') },
  { value: 'BACKTEST_ONLY' as PortfolioType, label: t('portfolio.types.backtest'), description: t('portfolio.types.backtestDesc') },
]);

async function handleSubmit() {
  error.value = '';
  try {
    const portfolio = await portfolioStore.createPortfolio({
      name: name.value,
      description: description.value || undefined,
      baseCurrency: baseCurrency.value,
      type: type.value,
    });
    router.push(`/portfolio/${portfolio.id}`);
  } catch (e: unknown) {
    error.value = (e as Error).message || t('portfolio.failedToCreate');
  }
}
</script>

<template>
  <div class="new-portfolio">
    <header class="page-header">
      <h1>{{ t('portfolio.createNew') }}</h1>
      <p>{{ t('portfolio.createNewSubtitle') }}</p>
    </header>

    <form @submit.prevent="handleSubmit" class="portfolio-form card">
      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <div class="form-group">
        <label class="form-label" for="name">{{ t('portfolio.name') }}</label>
        <input
          id="name"
          v-model="name"
          type="text"
          class="form-input"
          :placeholder="t('portfolio.namePlaceholder')"
          required
        />
      </div>

      <div class="form-group">
        <label class="form-label" for="description">{{ t('portfolio.descriptionLabel') }}</label>
        <textarea
          id="description"
          v-model="description"
          class="form-input"
          rows="3"
          :placeholder="t('portfolio.descriptionPlaceholder')"
        ></textarea>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label class="form-label" for="currency">{{ t('portfolio.baseCurrency') }}</label>
          <select id="currency" v-model="baseCurrency" class="form-input">
            <option v-for="c in currencies" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">{{ t('portfolio.type') }}</label>
          <div class="type-options">
            <label
              v-for="pt in portfolioTypes"
              :key="pt.value"
              class="type-option"
              :class="{ selected: type === pt.value }"
            >
              <input type="radio" v-model="type" :value="pt.value" class="sr-only" />
              <span class="type-label">{{ pt.label }}</span>
              <span class="type-description">{{ pt.description }}</span>
            </label>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" @click="router.back()">
          {{ t('common.cancel') }}
        </button>
        <button type="submit" class="btn btn-primary" :disabled="portfolioStore.loading">
          <span v-if="portfolioStore.loading" class="spinner"></span>
          <span v-else>{{ t('portfolio.createButton') }}</span>
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.new-portfolio {
  max-width: 640px;
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

.portfolio-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.error-message {
  padding: 12px;
  background-color: var(--danger-light);
  color: var(--danger-color);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}

textarea.form-input {
  resize: vertical;
  min-height: 80px;
}

.type-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.type-option:hover {
  border-color: var(--primary-color);
}

.type-option.selected {
  border-color: var(--primary-color);
  background-color: var(--primary-light);
}

.type-label {
  font-weight: 500;
}

.type-description {
  font-size: 13px;
  color: var(--text-secondary);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
