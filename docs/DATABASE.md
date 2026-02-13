# Database Schema

최종 업데이트: 2026-02-13

## 개요

- DBMS: PostgreSQL 16
- 마이그레이션: Flyway
- 현재 마이그레이션 파일: `V1__init_schema.sql` (1개)
- V1 기준 테이블 수: 24개

## 도메인별 테이블

### 사용자/워크스페이스

- `users`
- `workspaces`
- `workspace_members`

### 포트폴리오

- `portfolio_groups`
- `portfolios`
- `portfolio_targets`
- `portfolio_constraints`

### 금융상품/가격

- `exchanges`
- `instruments`
- `instrument_identifiers`
- `price_bars`
- `fx_rates`
- `benchmarks`

### 거래/평가

- `transactions`
- `transaction_legs`
- `position_snapshots`
- `portfolio_valuation_snapshots`

### 백테스트

- `backtest_configs`
- `backtest_runs`
- `backtest_result_series`
- `backtest_trade_logs`

### 알림

- `alert_rules`
- `notification_events`

## 핵심 관계

- `workspaces.owner_user_id -> users.id`
- `portfolios.workspace_id -> workspaces.id`
- `portfolio_targets.portfolio_id -> portfolios.id`
- `transactions.portfolio_id -> portfolios.id`
- `transaction_legs.transaction_id -> transactions.id`
- `price_bars.instrument_id -> instruments.id`
- `backtest_runs.config_id -> backtest_configs.id`

## 인덱스(핵심)

- `idx_users_email`
- `idx_portfolios_workspace`, `idx_portfolios_group`
- `idx_portfolio_targets_portfolio`
- `idx_instruments_ticker`, `idx_instruments_asset_class`, `idx_instruments_status`
- `idx_transactions_portfolio_occurred`, `idx_transactions_status`
- `idx_transaction_legs_transaction`
- `idx_price_bars_ts`
- `idx_fx_rates_quote_ts`
- `idx_position_snapshots_as_of`
- `idx_backtest_runs_config`, `idx_backtest_runs_status`
- `idx_backtest_trade_logs_run`
- `idx_notification_events_user`

## 스키마 운영 메모

- 현재는 V1에 대부분의 테이블이 한 번에 정의되어 있음
- 후속 변경은 반드시 `V2+` 신규 마이그레이션으로 추가해야 함
- `application-dev.yml`은 H2 `create-drop` 모드라 PostgreSQL 제약/타입과 차이가 날 수 있음

## 참조 파일

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/application.yml`
