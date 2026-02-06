import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import i18n from './i18n';
import { useAuthStore } from './stores';
import './assets/styles/main.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(i18n);

// 앱 시작 시 토큰이 있으면 사용자 정보 자동 로드
const authStore = useAuthStore();
const token = localStorage.getItem('accessToken');
if (token) {
  authStore.fetchUser().catch(() => {
    // 토큰이 유효하지 않으면 무시 (로그아웃 상태 유지)
  });
}

app.mount('#app');
