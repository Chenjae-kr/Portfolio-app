<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ref } from 'vue';

const { locale, t } = useI18n();
const showDropdown = ref(false);

function changeLanguage(newLocale: string) {
  locale.value = newLocale;
  localStorage.setItem('locale', newLocale);
  showDropdown.value = false;
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value;
}

// 현재 언어 표시
const currentLanguage = () => {
  return locale.value === 'ko' ? '한국어' : 'English';
};
</script>

<template>
  <div class="language-switcher">
    <button class="language-button" @click="toggleDropdown">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10" />
        <line x1="2" y1="12" x2="22" y2="12" />
        <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
      </svg>
      <span class="language-text">{{ currentLanguage() }}</span>
    </button>

    <div v-if="showDropdown" class="language-dropdown">
      <button
        class="language-option"
        :class="{ active: locale === 'ko' }"
        @click="changeLanguage('ko')"
      >
        {{ t('language.korean') }}
      </button>
      <button
        class="language-option"
        :class="{ active: locale === 'en' }"
        @click="changeLanguage('en')"
      >
        {{ t('language.english') }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.language-switcher {
  position: relative;
}

.language-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-primary);
  transition: all 0.2s;
}

.language-button:hover {
  background-color: var(--bg-hover);
}

.language-text {
  font-weight: 500;
}

.language-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  min-width: 140px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 1000;
}

.language-option {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-primary);
  transition: background-color 0.2s;
}

.language-option:hover {
  background-color: var(--bg-hover);
}

.language-option.active {
  background-color: var(--primary-light);
  color: var(--primary-color);
  font-weight: 600;
}
</style>
