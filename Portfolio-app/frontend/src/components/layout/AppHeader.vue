<script setup lang="ts">
import { useAuthStore } from '@/stores';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';

const emit = defineEmits<{
  (e: 'toggle-sidebar'): void;
}>();

const authStore = useAuthStore();
const router = useRouter();
const { t } = useI18n();

async function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <button class="menu-button" @click="emit('toggle-sidebar')">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="3" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </svg>
      </button>
    </div>

    <div class="header-right">
      <LanguageSwitcher />
      <div class="user-menu">
        <span class="user-name">{{ authStore.user?.displayName || authStore.user?.email }}</span>
        <button class="logout-button" @click="handleLogout">
          {{ t('nav.logout') }}
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 8px;
  color: var(--text-primary);
  transition: background-color 0.2s;
}

.menu-button:hover {
  background-color: var(--bg-hover);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: var(--text-secondary);
}

.logout-button {
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  background: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-primary);
  transition: all 0.2s;
}

.logout-button:hover {
  background-color: var(--bg-hover);
}
</style>
