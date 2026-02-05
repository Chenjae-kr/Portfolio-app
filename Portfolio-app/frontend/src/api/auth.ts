import { post, get } from './client';
import type { User, AuthTokens } from '@/types';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginResponse {
  user: User;
  tokens: AuthTokens;
}

export const authApi = {
  // Login
  login: (data: LoginRequest) =>
    post<LoginResponse>('/v1/auth/login', data),

  // Register
  register: (data: RegisterRequest) =>
    post<LoginResponse>('/v1/auth/register', data),

  // Refresh token
  refresh: (refreshToken: string) =>
    post<AuthTokens>('/v1/auth/refresh', { refreshToken }),

  // Get current user
  me: () =>
    get<User>('/v1/auth/me'),

  // Logout
  logout: () =>
    post<void>('/v1/auth/logout'),
};
