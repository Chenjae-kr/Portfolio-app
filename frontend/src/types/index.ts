// API Response Types
export interface ApiResponse<T> {
  data: T;
  meta: {
    timestamp: string;
    [key: string]: unknown;
  };
  error: ApiError | null;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

// Auth Types
export interface User {
  id: string;
  email: string;
  displayName: string;
  locale: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

// Portfolio Types
export type PortfolioType = 'REAL' | 'HYPOTHETICAL' | 'BACKTEST_ONLY';
export type AssetClass = 'EQUITY' | 'BOND' | 'COMMODITY' | 'CASH' | 'ALT';

export interface Portfolio {
  id: string;
  workspaceId: string;
  groupId?: string;
  name: string;
  description?: string;
  baseCurrency: string;
  type: PortfolioType;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  archivedAt?: string;
}

export interface PortfolioTarget {
  id: string;
  instrumentId?: string;
  assetClass: AssetClass;
  currency?: string;
  targetWeight: number;
  minWeight?: number;
  maxWeight?: number;
}

export interface PortfolioWithTargets extends Portfolio {
  targets: PortfolioTarget[];
}

// Instrument Types
export type InstrumentType = 'STOCK' | 'ETF' | 'ETN' | 'BOND' | 'COMMODITY_INDEX' | 'CASH_PROXY';
export type InstrumentStatus = 'ACTIVE' | 'DELISTED';

export interface Instrument {
  id: string;
  instrumentType: InstrumentType;
  name: string;
  ticker?: string;
  exchangeId?: string;
  currency: string;
  country?: string;
  assetClass: AssetClass;
  sector?: string;
  industry?: string;
  provider?: string;
  expenseRatio?: number;
  status: InstrumentStatus;
}

// Transaction Types
export type TransactionType =
  | 'BUY' | 'SELL' | 'DIVIDEND' | 'INTEREST'
  | 'FEE' | 'TAX' | 'DEPOSIT' | 'WITHDRAW'
  | 'FX_CONVERT' | 'SPLIT' | 'MERGER' | 'TRANSFER';

export type TransactionStatus = 'POSTED' | 'VOID' | 'PENDING';
export type LegType = 'ASSET' | 'CASH' | 'FEE' | 'TAX' | 'INCOME' | 'FX';

export interface TransactionLeg {
  id?: string;
  legType: LegType;
  instrumentId?: string;
  currency: string;
  quantity?: number;
  price?: number;
  amount: number;
  fxRateToBase?: number;
}

export interface Transaction {
  id: string;
  portfolioId: string;
  occurredAt: string;
  settleDate?: string;
  type: TransactionType;
  status: TransactionStatus;
  note?: string;
  tags: string[];
  legs: TransactionLeg[];
}

// Valuation Types
export type ValuationMode = 'REALTIME' | 'EOD';

export interface ValuationPosition {
  instrumentId: string;
  instrumentName?: string;
  ticker?: string;
  assetClass?: AssetClass;
  quantity: number;
  avgCost?: number;
  marketPrice: number;
  marketValueBase: number;
  unrealizedPnlBase: number;
  realizedPnlBase: number;
  weight: number;
  dayPnlBase?: number;
}

export interface Valuation {
  portfolioId: string;
  asOf: string;
  mode: ValuationMode;
  totalValueBase: number;
  cashValueBase: number;
  dayPnlBase: number;
  totalPnlBase: number;
  positions: ValuationPosition[];
  fxUsed: Record<string, number>;
  priceTimestamp: Record<string, string>;
}

// Performance Types
export type MetricType = 'TWR' | 'MWR' | 'SIMPLE';
export type FrequencyType = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface PerformanceDataPoint {
  date: string;
  value: number;
}

export interface RiskMetrics {
  totalReturn: number;
  cagr: number;
  volatility: number;
  mdd: number;
  sharpe: number;
}

export interface BenchmarkSeries {
  id: string;
  label: string;
  dataPoints: PerformanceDataPoint[];
  stats: RiskMetrics;
}

export interface PerformanceData {
  portfolioId: string;
  from: string;
  to: string;
  metric: string;
  frequency: string;
  dataPoints: PerformanceDataPoint[];
  stats: RiskMetrics;
  benchmarks?: BenchmarkSeries[];
}

export interface PerformancePoint {
  ts: string;
  value: number;
}

export interface PerformanceSeries {
  id: string;
  label: string;
  metric: MetricType;
  currencyMode: 'BASE' | 'NATIVE';
  points: PerformancePoint[];
}

export interface PerformanceStats {
  id: string;
  label: string;
  cagr?: number;
  vol?: number;
  mdd?: number;
  sharpe?: number;
  beta?: number;
  trackingError?: number;
  totalInvested?: number;
}

export interface CompareCurvePoint {
  date: string;
  value: number;
}

export interface CompareCurve {
  id: string;
  label: string;
  metric: string;
  points: CompareCurvePoint[];
}

export interface CompareStatRow {
  id: string;
  label: string;
  totalReturn?: number;
  cagr?: number;
  vol?: number;
  mdd?: number;
  sharpe?: number;
}

export interface CompareResponse {
  curves: CompareCurve[];
  statsTable: CompareStatRow[];
}

// Backtest Types
export type RebalanceType = 'NONE' | 'PERIODIC' | 'BAND';
export type RebalancePeriod = 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL';
export type PriceMode = 'ADJ_CLOSE' | 'CLOSE';
export type BacktestStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED';
export type InvestmentType = 'LUMP_SUM' | 'DCA';
export type DcaFrequency = 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL';

export interface BacktestConfig {
  id?: string;
  name: string;
  startDate: string;
  endDate: string;
  initialCapitalBase: number;
  rebalanceType: RebalanceType;
  rebalancePeriod?: RebalancePeriod;
  bandThreshold?: number;
  dividendReinvest: boolean;
  priceMode: PriceMode;
  feeModel?: Record<string, unknown>;
  investmentType?: InvestmentType;
  dcaAmount?: number;
  dcaFrequency?: DcaFrequency;
  targets: PortfolioTarget[];
}

export interface BacktestRun {
  id: string;
  configId: string;
  status: BacktestStatus;
  startedAt: string;
  finishedAt?: string;
  errorMessage?: string;
}

export interface BacktestResultPoint {
  ts: string;
  equityCurveBase: number;
  drawdown: number;
  cashBase: number;
  totalInvested?: number;
}

export interface BacktestTradeLog {
  ts: string;
  instrumentId: string;
  action: string;
  quantity: number;
  price: number;
  fee: number;
  amount?: number;
}

export interface BacktestResult {
  run: BacktestRun;
  series: BacktestResultPoint[];
  stats: PerformanceStats;
  tradeLogs: BacktestTradeLog[];
}

// Rebalance Types
export interface WeightComparison {
  instrumentId: string;
  instrumentName: string;
  currentWeight: number;
  targetWeight: number;
  difference: number;
  currentValue: number;
  targetValue: number;
  diffValue: number;
}

export interface TradeRecommendation {
  instrumentId: string;
  instrumentName: string;
  action: 'BUY' | 'SELL';
  amount: number;
  estimatedFee: number;
}

export interface RebalanceAnalysis {
  portfolioId: string;
  totalValue: number;
  cashBalance: number;
  cashWeight: number;
  comparisons: WeightComparison[];
  trades: TradeRecommendation[];
  totalEstimatedFee: number;
  needsRebalancing: boolean;
  maxDrift: number;
}
