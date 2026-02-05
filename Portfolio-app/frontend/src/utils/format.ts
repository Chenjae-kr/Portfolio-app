import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

export function formatCurrency(
  value: number,
  currency = 'KRW',
  showSign = false
): string {
  const formatter = new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency,
    minimumFractionDigits: currency === 'KRW' ? 0 : 2,
    maximumFractionDigits: currency === 'KRW' ? 0 : 2,
  });

  const formatted = formatter.format(Math.abs(value));

  if (showSign && value !== 0) {
    return value > 0 ? `+${formatted}` : `-${formatted}`;
  }

  return value < 0 ? `-${formatted}` : formatted;
}

export function formatNumber(
  value: number,
  decimals = 2,
  showSign = false
): string {
  const formatter = new Intl.NumberFormat('ko-KR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });

  const formatted = formatter.format(Math.abs(value));

  if (showSign && value !== 0) {
    return value > 0 ? `+${formatted}` : `-${formatted}`;
  }

  return value < 0 ? `-${formatted}` : formatted;
}

export function formatPercent(
  value: number,
  decimals = 2,
  showSign = false
): string {
  const formatted = formatNumber(value * 100, decimals);

  if (showSign && value !== 0) {
    return value > 0 ? `+${formatted}%` : `${formatted}%`;
  }

  return `${formatted}%`;
}

export function formatDate(date: string | Date, format = 'YYYY-MM-DD'): string {
  return dayjs(date).format(format);
}

export function formatDateTime(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD HH:mm');
}

export function formatRelativeTime(date: string | Date): string {
  return dayjs(date).fromNow();
}

export function formatCompactNumber(value: number): string {
  if (Math.abs(value) >= 1e12) {
    return `${(value / 1e12).toFixed(1)}T`;
  }
  if (Math.abs(value) >= 1e8) {
    return `${(value / 1e8).toFixed(1)}억`;
  }
  if (Math.abs(value) >= 1e4) {
    return `${(value / 1e4).toFixed(1)}만`;
  }
  return formatNumber(value, 0);
}

export function formatQuantity(value: number): string {
  if (Number.isInteger(value)) {
    return formatNumber(value, 0);
  }
  return formatNumber(value, 4);
}

export function getChangeClass(value: number): string {
  if (value > 0) return 'number-positive';
  if (value < 0) return 'number-negative';
  return '';
}
