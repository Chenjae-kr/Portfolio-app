<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

interface Props {
  open: boolean;
}

defineProps<Props>();
defineEmits<{
  (e: 'toggle'): void;
}>();

const route = useRoute();
const { t } = useI18n();

const navItems = computed(() => [
  { name: t('nav.dashboard'), path: '/dashboard', icon: 'dashboard' },
  { name: t('nav.compare'), path: '/compare', icon: 'compare' },
  { name: t('nav.backtest'), path: '/backtest', icon: 'backtest' },
]);

const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/');
};
</script>

<template>
  <aside class="app-sidebar" :class="{ collapsed: !open }">
    <div class="sidebar-header">
      <RouterLink to="/dashboard" class="logo">
        <span class="logo-icon">P</span>
        <span v-if="open" class="logo-text">Portfolio</span>
      </RouterLink>
    </div>

    <nav class="sidebar-nav">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="nav-icon">
          <template v-if="item.icon === 'dashboard'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="9" rx="1" />
              <rect x="14" y="3" width="7" height="5" rx="1" />
              <rect x="14" y="12" width="7" height="9" rx="1" />
              <rect x="3" y="16" width="7" height="5" rx="1" />
            </svg>
          </template>
          <template v-else-if="item.icon === 'compare'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="20" x2="18" y2="10" />
              <line x1="12" y1="20" x2="12" y2="4" />
              <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
          </template>
          <template v-else-if="item.icon === 'backtest'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="22,12 18,12 15,21 9,3 6,12 2,12" />
            </svg>
          </template>
        </span>
        <span v-if="open" class="nav-text">{{ item.name }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-footer">
      <RouterLink to="/portfolio/new" class="new-portfolio-btn" :class="{ collapsed: !open }">
        <span class="btn-icon">+</span>
        <span v-if="open" class="btn-text">{{ t('nav.newPortfolio') }}</span>
      </RouterLink>
    </div>
  </aside>
</template>

<style scoped>
.app-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 240px;
  height: 100vh;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: 100;
}

.app-sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--text-primary);
  font-weight: 600;
  font-size: 18px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  text-decoration: none;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.nav-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background-color: var(--primary-light);
  color: var(--primary-color);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.new-portfolio-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.2s;
}

.new-portfolio-btn:hover {
  background: var(--primary-dark);
}

.new-portfolio-btn.collapsed {
  padding: 12px;
}

.btn-icon {
  font-size: 18px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .app-sidebar {
    transform: translateX(-100%);
  }

  .app-sidebar:not(.collapsed) {
    transform: translateX(0);
  }
}
</style>
