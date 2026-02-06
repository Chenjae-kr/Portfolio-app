import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { authApi, type LoginRequest, type RegisterRequest } from '@/api';
import type { User } from '@/types';

const USER_STORAGE_KEY = 'user';

function saveUserToStorage(userData: User | null) {
  if (userData) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData));
  } else {
    localStorage.removeItem(USER_STORAGE_KEY);
  }
}

function loadUserFromStorage(): User | null {
  try {
    const stored = localStorage.getItem(USER_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore('auth', () => {
  // 초기화 시 localStorage에서 user 정보를 복구
  const user = ref<User | null>(loadUserFromStorage());
  const loading = ref(false);
  const error = ref<string | null>(null);

  const isAuthenticated = computed(() => !!user.value);

  async function login(credentials: LoginRequest) {
    loading.value = true;
    error.value = null;
    try {
      const response = await authApi.login(credentials);
      user.value = response.user;
      localStorage.setItem('accessToken', response.tokens.accessToken);
      localStorage.setItem('refreshToken', response.tokens.refreshToken);
      saveUserToStorage(response.user);
      return response;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Login failed';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function register(data: RegisterRequest) {
    loading.value = true;
    error.value = null;
    try {
      const response = await authApi.register(data);
      user.value = response.user;
      localStorage.setItem('accessToken', response.tokens.accessToken);
      localStorage.setItem('refreshToken', response.tokens.refreshToken);
      saveUserToStorage(response.user);
      return response;
    } catch (e: unknown) {
      error.value = (e as Error).message || 'Registration failed';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchUser() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      // 토큰 없으면 저장된 user도 지움
      user.value = null;
      saveUserToStorage(null);
      return;
    }

    loading.value = true;
    try {
      const userData = await authApi.me();
      user.value = userData;
      saveUserToStorage(userData);
    } catch {
      // /me 실패 시, localStorage에 user 정보가 있으면 유지 (토큰 갱신 시도)
      const storedUser = loadUserFromStorage();
      if (storedUser && localStorage.getItem('refreshToken')) {
        // refresh token이 있으면 user 정보는 유지하고,
        // 다음 API 호출에서 interceptor가 토큰을 갱신할 것
        user.value = storedUser;
      } else {
        logout();
      }
    } finally {
      loading.value = false;
    }
  }

  function logout() {
    user.value = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    saveUserToStorage(null);
  }

  return {
    user,
    loading,
    error,
    isAuthenticated,
    login,
    register,
    fetchUser,
    logout,
  };
});
