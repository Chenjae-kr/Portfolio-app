import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { guest: true },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/portfolio/:id',
    name: 'PortfolioDetail',
    component: () => import('@/views/portfolio/PortfolioDetailView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/portfolio/new',
    name: 'NewPortfolio',
    component: () => import('@/views/portfolio/NewPortfolioView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/compare',
    name: 'Compare',
    component: () => import('@/views/compare/CompareView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/backtest',
    name: 'Backtest',
    component: () => import('@/views/backtest/BacktestView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/backtest/:id',
    name: 'BacktestResult',
    component: () => import('@/views/backtest/BacktestResultView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Navigation guard
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore();

  // Try to fetch user if we have a token but no user
  if (!authStore.user && localStorage.getItem('accessToken')) {
    try {
      await authStore.fetchUser();
    } catch {
      // Token invalid, continue as guest
    }
  }

  const isAuthenticated = authStore.isAuthenticated;

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } else if (to.meta.guest && isAuthenticated) {
    next({ name: 'Dashboard' });
  } else {
    next();
  }
});

export default router;
