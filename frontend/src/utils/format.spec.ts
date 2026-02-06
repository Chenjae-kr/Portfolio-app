import { describe, it, expect } from 'vitest';
import { formatCurrency, formatPercent, formatQuantity, getChangeClass } from './format';

describe('format utilities', () => {
  describe('formatCurrency', () => {
    it('KRW 통화를 올바르게 포맷팅', () => {
      expect(formatCurrency(1000000, 'KRW')).toBe('₩1,000,000');
      expect(formatCurrency(0, 'KRW')).toBe('₩0');
      expect(formatCurrency(-5000, 'KRW')).toBe('₩-5,000');
    });

    it('USD 통화를 올바르게 포맷팅', () => {
      expect(formatCurrency(1234.56, 'USD')).toBe('$1,234.56');
      expect(formatCurrency(0, 'USD')).toBe('$0.00');
    });

    it('부호 표시 옵션 적용', () => {
      expect(formatCurrency(1000, 'KRW', true)).toBe('+₩1,000');
      expect(formatCurrency(-1000, 'KRW', true)).toBe('-₩1,000');
      expect(formatCurrency(0, 'KRW', true)).toBe('₩0');
    });
  });

  describe('formatPercent', () => {
    it('퍼센트를 올바르게 포맷팅', () => {
      expect(formatPercent(0.1234)).toBe('12.34%');
      expect(formatPercent(0.5)).toBe('50.00%');
      expect(formatPercent(-0.075)).toBe('-7.50%');
    });

    it('소수점 자릿수 조정', () => {
      expect(formatPercent(0.123456, 3)).toBe('12.346%');
      expect(formatPercent(0.123456, 1)).toBe('12.3%');
    });

    it('부호 표시 옵션 적용', () => {
      expect(formatPercent(0.05, 2, true)).toBe('+5.00%');
      expect(formatPercent(-0.05, 2, true)).toBe('-5.00%');
      expect(formatPercent(0, 2, true)).toBe('0.00%');
    });
  });

  describe('formatQuantity', () => {
    it('수량을 올바르게 포맷팅', () => {
      expect(formatQuantity(1234.5678)).toBe('1,234.5678');
      expect(formatQuantity(1000000)).toBe('1,000,000');
      expect(formatQuantity(0.00001234, 8)).toBe('0.00001234');
    });
  });

  describe('getChangeClass', () => {
    it('양수에 대해 positive 클래스 반환', () => {
      expect(getChangeClass(100)).toBe('number-positive');
      expect(getChangeClass(0.01)).toBe('number-positive');
    });

    it('음수에 대해 negative 클래스 반환', () => {
      expect(getChangeClass(-100)).toBe('number-negative');
      expect(getChangeClass(-0.01)).toBe('number-negative');
    });

    it('0에 대해 빈 문자열 반환', () => {
      expect(getChangeClass(0)).toBe('');
    });
  });
});
