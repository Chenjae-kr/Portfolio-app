import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import TargetWeights from './TargetWeights.vue';
import { createI18n } from 'vue-i18n';
import ko from '@/locales/ko';
import en from '@/locales/en';
import { portfolioApi } from '@/api/portfolio';

const i18n = createI18n({
  legacy: false,
  locale: 'ko',
  messages: { ko, en },
});

vi.mock('@/api/portfolio', () => ({
  portfolioApi: {
    getTargets: vi.fn(),
    updateTargets: vi.fn(),
  },
}));

vi.mock('@/api/instrument', () => ({
  instrumentApi: {
    search: vi.fn().mockResolvedValue({ data: { content: [] } }),
  },
}));

function mountComponent() {
  vi.mocked(portfolioApi.getTargets).mockResolvedValue([]);
  return mount(TargetWeights, {
    props: {
      portfolioId: 'test-portfolio',
      baseCurrency: 'KRW',
    },
    global: {
      plugins: [i18n],
    },
  });
}

describe('TargetWeights', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('컴포넌트가 렌더링됨', () => {
    const wrapper = mountComponent();
    expect(wrapper.exists()).toBe(true);
  });

  it('목표 비중이 없을 때 빈 상태 표시', async () => {
    const wrapper = mountComponent();
    await flushPromises();

    expect(wrapper.find('.empty-state').exists()).toBe(true);
  });

  it('총 비중 계산', async () => {
    const wrapper = mountComponent();
    await flushPromises();

    // targets 배열 초기화 후 추가
    wrapper.vm.targets.length = 0;
    wrapper.vm.targets.push(
      {
        id: '1',
        instrumentId: 'inst-1',
        assetClass: 'EQUITY',
        currency: 'USD',
        targetWeight: 0.6,
      },
      {
        id: '2',
        instrumentId: 'inst-2',
        assetClass: 'BOND',
        currency: 'KRW',
        targetWeight: 0.4,
      },
    );

    await wrapper.vm.$nextTick();

    expect(wrapper.vm.totalWeight).toBe(1.0);
    expect(wrapper.vm.isValid).toBe(true);
  });

  it('비중 합계가 100%가 아니면 유효하지 않음', async () => {
    const wrapper = mountComponent();
    await flushPromises();

    wrapper.vm.targets.length = 0;
    wrapper.vm.targets.push(
      {
        id: '1',
        instrumentId: 'inst-1',
        assetClass: 'EQUITY',
        currency: 'USD',
        targetWeight: 0.6,
      },
      {
        id: '2',
        instrumentId: 'inst-2',
        assetClass: 'BOND',
        currency: 'KRW',
        targetWeight: 0.3, // 합계 0.9
      },
    );

    await wrapper.vm.$nextTick();

    expect(wrapper.vm.totalWeight).toBeCloseTo(0.9, 10);
    expect(wrapper.vm.isValid).toBe(false);
  });

  it('정규화 기능', async () => {
    const wrapper = mountComponent();
    await flushPromises();

    wrapper.vm.targets.length = 0;
    wrapper.vm.targets.push(
      {
        id: '1',
        instrumentId: 'inst-1',
        assetClass: 'EQUITY',
        currency: 'USD',
        targetWeight: 60,
      },
      {
        id: '2',
        instrumentId: 'inst-2',
        assetClass: 'BOND',
        currency: 'KRW',
        targetWeight: 40,
      },
    );

    await wrapper.vm.$nextTick();

    // 정규화 실행
    wrapper.vm.normalize();

    await wrapper.vm.$nextTick();

    expect(wrapper.vm.targets[0].targetWeight).toBeCloseTo(0.6, 4);
    expect(wrapper.vm.targets[1].targetWeight).toBeCloseTo(0.4, 4);
    expect(wrapper.vm.totalWeight).toBeCloseTo(1.0, 4);
  });
});
