import { get, post } from './client';
import type { BacktestConfig, BacktestRun, BacktestResult } from '@/types';

export interface RunBacktestRequest {
  configId?: string;
  inlineConfig?: BacktestConfig;
}

export const backtestApi = {
  // Create backtest config
  createConfig: (config: BacktestConfig) =>
    post<BacktestConfig>('/v1/backtests/configs', config),

  // Get config by ID
  getConfig: (id: string) =>
    get<BacktestConfig>(`/v1/backtests/configs/${id}`),

  // List configs
  listConfigs: () =>
    get<BacktestConfig[]>('/v1/backtests/configs'),

  // Run backtest
  run: (request: RunBacktestRequest) =>
    post<BacktestRun>('/v1/backtests/runs', request),

  // Get run status
  getRun: (runId: string) =>
    get<BacktestRun>(`/v1/backtests/runs/${runId}`),

  // Get run results
  getResults: (runId: string) =>
    get<BacktestResult>(`/v1/backtests/runs/${runId}/results`),

  // List runs
  listRuns: (configId?: string) =>
    get<BacktestRun[]>('/v1/backtests/runs', { configId }),
};
