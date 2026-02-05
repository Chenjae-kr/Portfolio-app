import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { authApi, type LoginRequest, type RegisterRequest } from '@/api';
import type { User } from '@/types';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null);
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
    if (!token) return;

    loading.value = true;
    try {
      user.value = await authApi.me();
    } catch {
      logout();
    } finally {
      loading.value = false;
    }
  }

  function logout() {
    user.value = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
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
