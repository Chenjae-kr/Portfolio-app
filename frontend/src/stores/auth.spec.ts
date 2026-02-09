import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from './auth';
import { authApi } from '@/api';

vi.mock('@/api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    me: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  },
}));

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('초기 상태 확인', () => {
    const store = useAuthStore();

    expect(store.user).toBeNull();
    expect(store.isAuthenticated).toBe(false);
    expect(store.loading).toBe(false);
  });

  it('로그인 성공', async () => {
    const store = useAuthStore();
    const mockResponse = {
      user: {
        id: '1',
        email: 'test@example.com',
        displayName: 'Test User',
        locale: 'ko',
      },
      tokens: {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
      },
    };

    vi.mocked(authApi.login).mockResolvedValue(mockResponse);

    await store.login({
      email: 'test@example.com',
      password: 'password123',
    });

    expect(store.user).toEqual(mockResponse.user);
    expect(store.isAuthenticated).toBe(true);
    expect(localStorage.setItem).toHaveBeenCalledWith('accessToken', 'mock-access-token');
    expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'mock-refresh-token');
  });

  it('회원가입 성공', async () => {
    const store = useAuthStore();
    const mockResponse = {
      user: {
        id: '1',
        email: 'newuser@example.com',
        displayName: 'New User',
        locale: 'ko',
      },
      tokens: {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
      },
    };

    vi.mocked(authApi.register).mockResolvedValue(mockResponse);

    await store.register({
      email: 'newuser@example.com',
      password: 'password123',
      displayName: 'New User',
    });

    expect(store.user).toEqual(mockResponse.user);
    expect(store.isAuthenticated).toBe(true);
  });

  it('로그아웃', () => {
    const store = useAuthStore();

    // 로그인 상태 설정
    store.user = {
      id: '1',
      email: 'test@example.com',
      displayName: 'Test User',
      locale: 'ko',
    };

    store.logout();

    expect(store.user).toBeNull();
    expect(store.isAuthenticated).toBe(false);
    expect(localStorage.removeItem).toHaveBeenCalledWith('accessToken');
    expect(localStorage.removeItem).toHaveBeenCalledWith('refreshToken');
  });
});
