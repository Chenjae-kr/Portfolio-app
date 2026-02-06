<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from '@/components/layout/LanguageSwitcher.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { t } = useI18n();

const email = ref('');
const password = ref('');
const error = ref('');

async function handleSubmit() {
  error.value = '';
  try {
    await authStore.login({ email: email.value, password: password.value });
    const redirect = route.query.redirect as string || '/dashboard';
    router.push(redirect);
  } catch (e: unknown) {
    error.value = (e as Error).message || t('auth.login.loginFailed');
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="language-switcher-container">
      <LanguageSwitcher />
    </div>
    <div class="auth-card">
      <div class="auth-header">
        <h1>{{ t('auth.login.title') }}</h1>
        <p>{{ t('auth.login.subtitle') }}</p>
      </div>

      <form @submit.prevent="handleSubmit" class="auth-form">
        <div v-if="error" class="error-message">
          {{ error }}
        </div>

        <div class="form-group">
          <label class="form-label" for="email">{{ t('common.email') }}</label>
          <input
            id="email"
            v-model="email"
            type="email"
            class="form-input"
            :placeholder="t('auth.login.emailPlaceholder')"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="password">{{ t('common.password') }}</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="form-input"
            :placeholder="t('auth.login.passwordPlaceholder')"
            required
          />
        </div>

        <button type="submit" class="btn btn-primary btn-full" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="spinner"></span>
          <span v-else>{{ t('auth.login.signInButton') }}</span>
        </button>
      </form>

      <div class="auth-footer">
        <p>{{ t('auth.login.noAccount') }} <RouterLink to="/register">{{ t('auth.login.signUp') }}</RouterLink></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: var(--bg-primary);
  position: relative;
}

.language-switcher-container {
  position: absolute;
  top: 24px;
  right: 24px;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 32px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header h1 {
  font-size: 1.5rem;
  margin-bottom: 8px;
}

.auth-header p {
  color: var(--text-secondary);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-message {
  padding: 12px;
  background-color: var(--danger-light);
  color: var(--danger-color);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.btn-full {
  width: 100%;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
