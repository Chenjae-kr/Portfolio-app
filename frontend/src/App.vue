<script setup lang="ts">
import { RouterView } from 'vue-router';
import AppLayout from '@/components/layout/AppLayout.vue';
import { useAuthStore } from '@/stores';
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const authStore = useAuthStore();
const route = useRoute();

// 앱 시작 시 토큰이 있으면 사용자 정보 자동 로드
onMounted(async () => {
  const token = localStorage.getItem('accessToken');
  if (token && !authStore.user) {
    try {
      await authStore.fetchUser();
    } catch {
      // 토큰이 유효하지 않으면 무시 (로그아웃 상태 유지)
    }
  }
});

const showLayout = computed(() => {
  return authStore.isAuthenticated && !route.meta.guest;
});
</script>

<template>
  <AppLayout v-if="showLayout">
    <RouterView />
  </AppLayout>
  <RouterView v-else />
</template>

<style>
#app {
  min-height: 100vh;
}
</style>
