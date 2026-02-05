<script setup lang="ts">
import AppHeader from './AppHeader.vue';
import AppSidebar from './AppSidebar.vue';
import { ref } from 'vue';

const sidebarOpen = ref(true);

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value;
}
</script>

<template>
  <div class="app-layout">
    <AppSidebar :open="sidebarOpen" @toggle="toggleSidebar" />
    <div class="main-content" :class="{ 'sidebar-collapsed': !sidebarOpen }">
      <AppHeader @toggle-sidebar="toggleSidebar" />
      <main class="page-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--bg-primary);
}

.main-content {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.3s ease;
}

.main-content.sidebar-collapsed {
  margin-left: 64px;
}

.page-content {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
  }
}
</style>
