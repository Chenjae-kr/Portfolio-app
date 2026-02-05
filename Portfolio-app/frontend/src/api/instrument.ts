import { get } from './client';
import type { Instrument, AssetClass } from '@/types';

export interface SearchInstrumentParams {
  q: string;
  assetClass?: AssetClass;
  country?: string;
  limit?: number;
}

export interface PriceBar {
  ts: string;
  open: number;
  high: number;
  low: number;
  close: number;
  adjClose?: number;
  volume?: number;
}

export interface PriceParams {
  timeframe?: 'D1' | 'W1' | 'M1';
  from?: string;
  to?: string;
  adjusted?: boolean;
}

export interface FxRate {
  ts: string;
  rate: number;
}

export const instrumentApi = {
  // Search instruments
  search: (params: SearchInstrumentParams) =>
    get<Instrument[]>('/v1/instruments/search', params),

  // Get instrument by ID
  getById: (id: string) =>
    get<Instrument>(`/v1/instruments/${id}`),

  // Get price series
  getPrices: (instrumentId: string, params?: PriceParams) =>
    get<PriceBar[]>(`/v1/prices/${instrumentId}`, params),

  // Get FX rates
  getFxRates: (base: string, quote: string, from?: string, to?: string) =>
    get<FxRate[]>('/v1/fx', { base, quote, from, to }),
};
