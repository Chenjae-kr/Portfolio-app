-- Users & Auth
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(100),
    locale VARCHAR(10) DEFAULT 'ko-KR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Workspaces (for future multi-user support)
CREATE TABLE workspaces (
    id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workspace_members (
    workspace_id VARCHAR(36) NOT NULL REFERENCES workspaces(id),
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    PRIMARY KEY (workspace_id, user_id)
);

-- Portfolio Groups
CREATE TABLE portfolio_groups (
    id VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL REFERENCES workspaces(id),
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Portfolios
CREATE TABLE portfolios (
    id VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL REFERENCES workspaces(id),
    group_id VARCHAR(36) REFERENCES portfolio_groups(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    base_currency CHAR(3) NOT NULL DEFAULT 'KRW',
    type VARCHAR(20) NOT NULL CHECK (type IN ('REAL', 'HYPOTHETICAL', 'BACKTEST_ONLY')),
    tags JSONB DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP
);

CREATE INDEX idx_portfolios_workspace ON portfolios(workspace_id);
CREATE INDEX idx_portfolios_group ON portfolios(group_id);

-- Portfolio Targets
CREATE TABLE portfolio_targets (
    id VARCHAR(36) PRIMARY KEY,
    portfolio_id VARCHAR(36) NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    instrument_id VARCHAR(36),
    asset_class VARCHAR(20) NOT NULL CHECK (asset_class IN ('EQUITY', 'BOND', 'COMMODITY', 'CASH', 'ALT')),
    currency CHAR(3),
    target_weight DECIMAL(7, 4) NOT NULL CHECK (target_weight >= 0 AND target_weight <= 1),
    min_weight DECIMAL(7, 4),
    max_weight DECIMAL(7, 4),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (portfolio_id, instrument_id, currency)
);

CREATE INDEX idx_portfolio_targets_portfolio ON portfolio_targets(portfolio_id);

-- Portfolio Constraints
CREATE TABLE portfolio_constraints (
    id VARCHAR(36) PRIMARY KEY,
    portfolio_id VARCHAR(36) NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    constraint_type VARCHAR(50) NOT NULL,
    params JSONB NOT NULL DEFAULT '{}',
    enabled BOOLEAN DEFAULT TRUE
);

-- Exchanges
CREATE TABLE exchanges (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100),
    timezone VARCHAR(50) NOT NULL,
    currency CHAR(3) NOT NULL,
    trading_hours JSONB
);

-- Instruments
CREATE TABLE instruments (
    id VARCHAR(36) PRIMARY KEY,
    instrument_type VARCHAR(20) NOT NULL CHECK (instrument_type IN ('STOCK', 'ETF', 'ETN', 'BOND', 'COMMODITY_INDEX', 'CASH_PROXY')),
    name VARCHAR(200) NOT NULL,
    ticker VARCHAR(20),
    exchange_id VARCHAR(36) REFERENCES exchanges(id),
    currency CHAR(3) NOT NULL,
    country CHAR(2),
    asset_class VARCHAR(20) NOT NULL CHECK (asset_class IN ('EQUITY', 'BOND', 'COMMODITY', 'CASH', 'ALT')),
    sector VARCHAR(100),
    industry VARCHAR(100),
    provider VARCHAR(100),
    expense_ratio DECIMAL(6, 4),
    benchmark_index VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DELISTED')),
    first_listed_at DATE,
    delisted_at DATE
);

CREATE INDEX idx_instruments_ticker ON instruments(ticker);
CREATE INDEX idx_instruments_asset_class ON instruments(asset_class);
CREATE INDEX idx_instruments_status ON instruments(status);

-- Instrument Identifiers (ISIN, CUSIP, etc.)
CREATE TABLE instrument_identifiers (
    instrument_id VARCHAR(36) NOT NULL REFERENCES instruments(id),
    id_type VARCHAR(20) NOT NULL CHECK (id_type IN ('ISIN', 'CUSIP', 'FIGI', 'SEDOL')),
    value VARCHAR(50) NOT NULL,
    PRIMARY KEY (instrument_id, id_type),
    UNIQUE (id_type, value)
);

-- Price Bars (EOD data)
CREATE TABLE price_bars (
    instrument_id VARCHAR(36) NOT NULL REFERENCES instruments(id),
    timeframe VARCHAR(5) NOT NULL CHECK (timeframe IN ('D1', 'W1', 'M1')),
    ts DATE NOT NULL,
    open DECIMAL(18, 6) NOT NULL,
    high DECIMAL(18, 6) NOT NULL,
    low DECIMAL(18, 6) NOT NULL,
    close DECIMAL(18, 6) NOT NULL,
    adj_close DECIMAL(18, 6),
    volume BIGINT,
    data_vendor VARCHAR(50),
    PRIMARY KEY (instrument_id, timeframe, ts)
);

CREATE INDEX idx_price_bars_ts ON price_bars(instrument_id, ts);

-- FX Rates
CREATE TABLE fx_rates (
    base_currency CHAR(3) NOT NULL,
    quote_currency CHAR(3) NOT NULL,
    ts TIMESTAMP NOT NULL,
    rate DECIMAL(18, 8) NOT NULL,
    source VARCHAR(50),
    PRIMARY KEY (base_currency, quote_currency, ts)
);

CREATE INDEX idx_fx_rates_quote_ts ON fx_rates(quote_currency, ts);

-- Transactions
CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    portfolio_id VARCHAR(36) NOT NULL REFERENCES portfolios(id),
    occurred_at TIMESTAMP NOT NULL,
    settle_date DATE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('BUY', 'SELL', 'DIVIDEND', 'INTEREST', 'FEE', 'TAX', 'DEPOSIT', 'WITHDRAW', 'FX_CONVERT', 'SPLIT', 'MERGER', 'TRANSFER')),
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED', 'VOID', 'PENDING')),
    note TEXT,
    tags JSONB DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_portfolio_occurred ON transactions(portfolio_id, occurred_at);
CREATE INDEX idx_transactions_status ON transactions(status);

-- Transaction Legs (double-entry style)
CREATE TABLE transaction_legs (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    leg_type VARCHAR(20) NOT NULL CHECK (leg_type IN ('ASSET', 'CASH', 'FEE', 'TAX', 'INCOME', 'FX')),
    instrument_id VARCHAR(36) REFERENCES instruments(id),
    currency CHAR(3) NOT NULL,
    quantity DECIMAL(18, 8),
    price DECIMAL(18, 6),
    amount DECIMAL(18, 4) NOT NULL,
    fx_rate_to_base DECIMAL(18, 8),
    account VARCHAR(50)
);

CREATE INDEX idx_transaction_legs_transaction ON transaction_legs(transaction_id);

-- Position Snapshots (for fast queries)
CREATE TABLE position_snapshots (
    portfolio_id VARCHAR(36) NOT NULL REFERENCES portfolios(id),
    as_of TIMESTAMP NOT NULL,
    instrument_id VARCHAR(36) NOT NULL REFERENCES instruments(id),
    quantity DECIMAL(18, 8) NOT NULL,
    avg_cost DECIMAL(18, 6),
    market_value_base DECIMAL(18, 4),
    unrealized_pnl_base DECIMAL(18, 4),
    realized_pnl_base DECIMAL(18, 4),
    PRIMARY KEY (portfolio_id, as_of, instrument_id)
);

CREATE INDEX idx_position_snapshots_as_of ON position_snapshots(portfolio_id, as_of);

-- Portfolio Valuation Snapshots
CREATE TABLE portfolio_valuation_snapshots (
    portfolio_id VARCHAR(36) NOT NULL REFERENCES portfolios(id),
    as_of TIMESTAMP NOT NULL,
    total_market_value_base DECIMAL(18, 4) NOT NULL,
    cash_value_base DECIMAL(18, 4) NOT NULL,
    day_pnl_base DECIMAL(18, 4),
    total_pnl_base DECIMAL(18, 4),
    twr_to_date DECIMAL(10, 6),
    mwr_to_date DECIMAL(10, 6),
    PRIMARY KEY (portfolio_id, as_of)
);

-- Benchmarks
CREATE TABLE benchmarks (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INDEX', 'ETF_PROXY')),
    base_currency CHAR(3) NOT NULL,
    instrument_id VARCHAR(36) REFERENCES instruments(id)
);

-- Backtest Configs
CREATE TABLE backtest_configs (
    id VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL REFERENCES workspaces(id),
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    initial_capital_base DECIMAL(18, 4) NOT NULL,
    rebalance_type VARCHAR(20) NOT NULL CHECK (rebalance_type IN ('NONE', 'PERIODIC', 'BAND')),
    rebalance_period VARCHAR(20) CHECK (rebalance_period IN ('MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL')),
    band_threshold DECIMAL(5, 4),
    fee_model JSONB DEFAULT '{}',
    dividend_reinvest BOOLEAN DEFAULT FALSE,
    price_mode VARCHAR(20) NOT NULL DEFAULT 'ADJ_CLOSE' CHECK (price_mode IN ('ADJ_CLOSE', 'CLOSE')),
    params JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Backtest Runs
CREATE TABLE backtest_runs (
    id VARCHAR(36) PRIMARY KEY,
    config_id VARCHAR(36) NOT NULL REFERENCES backtest_configs(id),
    created_by VARCHAR(36) NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX idx_backtest_runs_config ON backtest_runs(config_id);
CREATE INDEX idx_backtest_runs_status ON backtest_runs(status);

-- Backtest Result Series
CREATE TABLE backtest_result_series (
    run_id VARCHAR(36) NOT NULL REFERENCES backtest_runs(id) ON DELETE CASCADE,
    ts DATE NOT NULL,
    equity_curve_base DECIMAL(18, 4) NOT NULL,
    drawdown DECIMAL(10, 6),
    cash_base DECIMAL(18, 4),
    PRIMARY KEY (run_id, ts)
);

-- Backtest Trade Logs
CREATE TABLE backtest_trade_logs (
    id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL REFERENCES backtest_runs(id) ON DELETE CASCADE,
    ts DATE NOT NULL,
    instrument_id VARCHAR(36) NOT NULL REFERENCES instruments(id),
    action VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 8) NOT NULL,
    price DECIMAL(18, 6) NOT NULL,
    fee DECIMAL(18, 4),
    reason JSONB
);

CREATE INDEX idx_backtest_trade_logs_run ON backtest_trade_logs(run_id, ts);

-- Alert Rules
CREATE TABLE alert_rules (
    id VARCHAR(36) PRIMARY KEY,
    portfolio_id VARCHAR(36) NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    params JSONB NOT NULL DEFAULT '{}',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notification Events
CREATE TABLE notification_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    alert_rule_id VARCHAR(36) REFERENCES alert_rules(id),
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_events_user ON notification_events(user_id, created_at);
