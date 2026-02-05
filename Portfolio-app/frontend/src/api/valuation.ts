import { get, post } from './client';
import type {
  Valuation,
  ValuationMode,
  PerformanceSeries,
  MetricType,
  FrequencyType,
  CompareResponse,
} from '@/types';

export interface PerformanceParams {
  from: string;
  to: string;
  metric?: MetricType;
  frequency?: FrequencyType;
}

export interface CompareRequest {
  portfolioIds: string[];
  benchmarks?: string[];
  from: string;
  to: string;
  metric: MetricType;
  currencyMode: 'BASE' | 'NATIVE';
}

export const valuationApi = {
  // Get portfolio valuation
  getValuation: (portfolioId: string, mode: ValuationMode = 'REALTIME', asOf?: string) =>
    get<Valuation>(`/v1/portfolios/${portfolioId}/valuation`, { mode, as_of: asOf }),

  // Get performance series
  getPerformance: (portfolioId: string, params: PerformanceParams) =>
    get<PerformanceSeries>(`/v1/portfolios/${portfolioId}/performance`, params),

  // Compare portfolios
  compare: (request: CompareRequest) =>
    post<CompareResponse>('/v1/compare/performance', request),
};
