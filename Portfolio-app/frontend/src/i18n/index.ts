import { createI18n } from 'vue-i18n';
import ko from '@/locales/ko';
import en from '@/locales/en';

const messages = {
  ko,
  en,
};

// 로컬 스토리지에서 저장된 언어 가져오기, 없으면 한국어 기본값
const savedLocale = localStorage.getItem('locale') || 'ko';

const i18n = createI18n({
  legacy: false, // Composition API 모드
  locale: savedLocale,
  fallbackLocale: 'ko',
  messages,
  globalInjection: true, // $t를 전역으로 사용
});

export default i18n;
