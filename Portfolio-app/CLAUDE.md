# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Portfolio Manager App - A multi-asset portfolio management service supporting real-time valuation, portfolio comparison, backtesting, and rebalancing simulation.

**Core Features:**
- Multi-portfolio management (stocks, ETFs, bonds, commodities)
- Real-time valuation with currency conversion
- Portfolio performance comparison & benchmarking
- Strategy backtesting engine
- Rebalancing tools & risk analysis

## Development Commands

### Infrastructure (Docker)
```bash
# Start PostgreSQL, Redis, RabbitMQ
docker-compose up -d

# Stop all services
docker-compose down
```

### Backend
```bash
cd backend

# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests (H2 in-memory DB 사용)
./gradlew test

# Run single test
./gradlew test --tests "com.portfolio.auth.service.AuthServiceTest"

# Test reports
# - HTML: backend/build/reports/tests/test/index.html
```

Backend runs on: http://localhost:8080/api
Swagger UI: http://localhost:8080/api/swagger-ui.html

### Frontend
```bash
cd frontend

# Install dependencies
npm install

# Run dev server
npm run dev

# Build for production
npm run build

# Type check
npm run type-check

# Run tests
npm test

# Run tests with UI (interactive)
npm run test:ui

# Run tests with coverage
npm run test:coverage
```

Frontend runs on: http://localhost:5173

## Technology Stack

### Backend
- Java 21 (LTS)
- Spring Boot 3.3+
- Spring Security (JWT authentication)
- Spring Data JPA + Hibernate (ORM)
- jOOQ (aggregation/analytics queries)
- PostgreSQL 16+ with Flyway migrations
- Redis 7 (real-time price/FX cache)
- RabbitMQ (async job queue for backtests)

### Frontend
- Vue 3 + TypeScript
- Vite
- Pinia (state management)
- ECharts (financial charts)
- Axios

## Architecture

```
[ Vue.js SPA ]
      | REST
[ Spring Boot API ]
      |
 +-- Portfolio/Transaction Domain
 +-- Valuation Engine
 +-- Analytics Engine
 +-- Backtest Engine (Async via RabbitMQ)
 +-- Pricing/FX Client

[ PostgreSQL ] -- ledger/timeseries/EOD data
[ Redis ] -- real-time quotes/valuation cache
[ RabbitMQ ] -- backtest/recompute queue
```

### Backend Package Structure (Monolith)
- `api` - Controllers, DTOs, OpenAPI
- `auth` - Security, JWT
- `portfolio` - Portfolio, Targets, Constraints
- `ledger` - Transaction, Legs, Posting rules
- `pricing` - Price/FX clients, cache
- `valuation` - Valuation engine, snapshots
- `analytics` - Performance, Risk, Compare
- `backtest` - Configs, Runs, Worker interface
- `infra` - Redis, MQ, DB, Observability

### Frontend Structure
- `api/` - API client and endpoint modules
- `components/` - Reusable Vue components (layout, portfolio, chart)
- `composables/` - Vue composables for shared logic
- `router/` - Vue Router configuration
- `stores/` - Pinia stores (auth, portfolio, valuation, backtest)
- `types/` - TypeScript type definitions
- `utils/` - Utility functions (format, etc.)
- `views/` - Page components (dashboard, portfolio, compare, backtest)

## Domain Model Principles

- **Transaction ledger is the single source of truth** - positions/valuations are derived
- **Double-entry style legs** for transactions (ASSET/CASH/FEE/TAX)
- Prices and FX rates are time-point based
- Multi-portfolio + multi-currency support from day one

### Key Entities
- `Portfolio` - base_currency, type (REAL/STRATEGY/BACKTEST)
- `Instrument` - ticker, type, asset_class, currency, exchange
- `Transaction` + `TransactionLeg` - double-entry style
- `PriceBar` / `FXRate` - time-series data

## API Design

Base: `/api/v1`

### Core Endpoints
```
GET/POST  /portfolios
GET/PATCH /portfolios/{id}
PUT       /portfolios/{id}/targets
GET/POST  /portfolios/{id}/transactions
POST      /transactions/{id}/void
GET       /portfolios/{id}/valuation?mode=REALTIME|EOD
GET       /portfolios/{id}/performance
POST      /compare/performance
POST      /backtests/runs
GET       /backtests/runs/{id}/results
```

### Transaction Legs Validation
- Legs must balance (currency-wise or base-converted sum = 0)
- BUY/SELL requires exactly one ASSET leg
- Fees/taxes are separate legs

### Target Weights Validation
- Sum must equal 1.0 (with normalize option)
- Cash targets: `instrument_id=null` + `currency!=null`

## Redis Cache Keys

```
tick:{instrument_id}     TTL: 60-180s
fx:{base}:{quote}        TTL: 300s
val:{portfolio_id}:realtime   TTL: 10-30s
val:{portfolio_id}:eod:{date} TTL: 5min
```

## Calculation Specs

### Valuation
```
MV_i = qty_i * last_price_i
MV_i_base = MV_i * FX(instrument_currency -> base_currency)
Total = Sum(MV_i_base) + Cash_base
```

### Returns
- **TWR (Time-Weighted)**: Default for strategy performance comparison
- **MWR (Money-Weighted/IRR)**: User's actual experience with cash flows

### Risk Metrics
- Volatility (annualized)
- MDD (Maximum Drawdown)
- Sharpe/Sortino ratios
- Beta, Tracking Error

## MVP Priority

1. Transaction ledger + Valuation calculation
2. Portfolio UI
3. Comparison charts
4. Basic backtesting (static + periodic rebalancing)
5. Rebalancing tools
6. Alerts/Insights
