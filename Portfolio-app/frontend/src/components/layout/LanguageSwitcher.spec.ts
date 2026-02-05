import { describe, it, expect, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';
import ko from '@/locales/ko';
import en from '@/locales/en';

const i18n = createI18n({
  legacy: false,
  locale: 'ko',
  fallbackLocale: 'ko',
  messages: { ko, en },
});

describe('LanguageSwitcher 컴포넌트', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('컴포넌트가 정상적으로 마운트됨', () => {
    const wrapper = mount(LanguageSwitcher, {
      global: {
        plugins: [i18n],
      },
    });

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('.language-button').exists()).toBe(true);
  });

  it('초기 언어가 한국어로 표시됨', () => {
    const wrapper = mount(LanguageSwitcher, {
      global: {
        plugins: [i18n],
      },
    });

    expect(wrapper.find('.language-text').text()).toBe('한국어');
  });

  it('버튼 클릭 시 드롭다운이 표시됨', async () => {
    const wrapper = mount(LanguageSwitcher, {
      global: {
        plugins: [i18n],
      },
    });

    expect(wrapper.find('.language-dropdown').exists()).toBe(false);

    await wrapper.find('.language-button').trigger('click');

    expect(wrapper.find('.language-dropdown').exists()).toBe(true);
    expect(wrapper.findAll('.language-option')).toHaveLength(2);
  });

  it('언어 선택 시 언어가 변경됨', async () => {
    const wrapper = mount(LanguageSwitcher, {
      global: {
        plugins: [i18n],
      },
    });

    await wrapper.find('.language-button').trigger('click');

    const options = wrapper.findAll('.language-option');
    await options[1].trigger('click'); // 영어 선택

    expect(i18n.global.locale.value).toBe('en');
    expect(localStorage.setItem).toHaveBeenCalledWith('locale', 'en');
  });

  it('현재 선택된 언어에 active 클래스가 적용됨', async () => {
    const wrapper = mount(LanguageSwitcher, {
      global: {
        plugins: [i18n],
      },
    });

    await wrapper.find('.language-button').trigger('click');

    const options = wrapper.findAll('.language-option');
    expect(options[0].classes()).toContain('active'); // 한국어
    expect(options[1].classes()).not.toContain('active'); // 영어
  });
});
