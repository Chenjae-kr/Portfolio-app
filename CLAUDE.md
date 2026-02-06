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

### Infrastructure

**Option 1: Docker (Full Stack)**
```bash
docker-compose up -d          # Start PostgreSQL, Redis, RabbitMQ
docker-compose down           # Stop all services
```

**Option 2: Local Development (PostgreSQL only)**
```bash
# macOS with Homebrew
brew services start postgresql@14

# Create database (first time only)
createdb portfolio
```

### Backend
```bash
cd backend

# Build
./gradlew build

# Run (local profile - uses local PostgreSQL)
./gradlew bootRun --args='--spring.profiles.active=local'

# Run with Docker infrastructure
./gradlew bootRun

# Run tests (H2 in-memory DB 사용)
./gradlew test

# Run single test
./gradlew test --tests "com.portfolio.auth.service.AuthServiceTest"

# Test reports: backend/build/reports/tests/test/index.html
```

Backend runs on: http://localhost:8080/api
Swagger UI: http://localhost:8080/api/swagger-ui.html

### Frontend
```bash
cd frontend

npm install         # Install dependencies
npm run dev         # Run dev server (http://localhost:5173)
npm run build       # Build for production
npm test            # Run tests
npm run test:ui     # Run tests with UI (interactive)
npm run test:coverage  # Run tests with coverage
```

## Technology Stack

### Backend
- Java 21 (LTS)
- Spring Boot 3.3+
- Spring Security (JWT authentication)
- Spring Data JPA + Hibernate (ORM)
- jOOQ (aggregation/analytics queries)
- PostgreSQL 16+ with Flyway migrations (H2 for tests)
- Redis 7 (real-time price/FX cache) - optional for local dev
- RabbitMQ (async job queue for backtests) - optional for local dev

### Frontend
- Vue 3 + TypeScript
- Vite 7+
- Pinia 3 (state management)
- Vue Router 4
- Vue I18n 9 (다국어 지원: ko/en)
- ECharts 6 / vue-echarts (financial charts)
- VueUse (composables)
- Axios
- Day.js
- Vitest (testing)

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
[ Redis ] -- real-time quotes/valuation cache (optional in dev)
[ RabbitMQ ] -- backtest/recompute queue (optional in dev)
```

### Backend Package Structure
```
src/main/java/com/portfolio/
├── api/              # REST Controllers (AuthController, PortfolioController, etc.)
├── auth/             # Security, JWT, User entity
│   ├── config/       # SecurityConfig
│   ├── jwt/          # JwtTokenProvider, JwtAuthenticationFilter
│   ├── entity/       # User
│   ├── repository/   # UserRepository
│   └── service/      # AuthService, CustomUserDetailsService
├── portfolio/        # Portfolio domain
│   ├── entity/       # Portfolio, PortfolioTarget, PortfolioGroup
│   ├── repository/
│   └── service/      # PortfolioService
├── ledger/           # Transaction domain
│   ├── entity/       # Transaction, TransactionLeg
│   └── repository/
├── pricing/          # Instrument, Price, FX
│   ├── entity/       # Instrument, PriceBar, FxRate
│   ├── repository/
│   └── service/      # InstrumentService
├── workspace/        # Workspace (multi-tenant)
│   ├── entity/
│   ├── repository/
│   └── service/
├── common/           # Shared utilities
│   ├── exception/    # GlobalExceptionHandler, ErrorCode
│   ├── response/     # ApiResponse
│   └── util/         # AssetClass enum
└── infra/            # Infrastructure configs
    ├── redis/        # RedisConfig
    └── rabbitmq/     # RabbitMQConfig
```

### Frontend Structure
```
src/
├── api/           # API client modules (auth, portfolio, valuation, etc.)
├── components/    # Reusable Vue components
│   ├── layout/    # AppLayout, AppHeader, AppSidebar
│   └── portfolio/ # PortfolioCard, PositionTable, TargetWeights
├── composables/   # Vue composables for shared logic
├── i18n/          # Internationalization setup
├── locales/       # Translation files (ko.ts, en.ts)
├── router/        # Vue Router configuration with auth guards
├── stores/        # Pinia stores (auth, portfolio, valuation, backtest)
├── types/         # TypeScript type definitions
├── utils/         # Utility functions (format.ts)
└── views/         # Page components
    ├── auth/      # LoginView, RegisterView
    ├── portfolio/ # PortfolioDetailView, NewPortfolioView
    ├── compare/   # CompareView
    └── backtest/  # BacktestView, BacktestResultView
```

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

## Testing

### Backend Tests
- Tests use H2 in-memory database (application-test.yml)
- Redis/RabbitMQ are excluded in test profile
- Run: `./gradlew test`

### Frontend Tests
- Vitest + Vue Test Utils + jsdom
- Run: `npm test` or `npm run test:ui`

## Environment Profiles

### Backend Profiles
- **default**: Full stack with Redis/RabbitMQ
- **local**: Local PostgreSQL, Redis/RabbitMQ optional
- **test**: H2 in-memory DB, no external services

### Configuration Files
```
backend/src/main/resources/
├── application.yml          # Default config
├── application-local.yml    # Local dev overrides
└── application-test.yml     # Test config (H2)
```

## Internationalization (i18n)

Frontend supports Korean (ko) and English (en):
- Default language: Korean
- Language files: `frontend/src/locales/{ko,en}.ts`
- Usage: `useI18n()` composable with `t('key')` function
- Language preference saved to localStorage
