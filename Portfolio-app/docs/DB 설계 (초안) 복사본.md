
---

## 1) 도메인 모델 & DB 설계(ERD 수준)

### 1.1 핵심 설계 원칙

- **거래(Transaction) = 단일 진실원(Single Source of Truth)**  
    포지션/평단/현금/성과는 거래를 기반으로 **파생(계산/캐시)**.
    
- **가격(Price)/환율(FX)은 “시점”이 핵심**  
    실시간 평가(마지막 틱) + 백테스트(EOD) 분리.
    
- **멀티 포트폴리오 + 멀티 통화**를 1차부터 자연스럽게 지원  
    Portfolio.base_currency, CashAccount(통화별), FXRate.
    

---

### 1.2 엔티티 목록(요약)

- User, Workspace(확장용), Portfolio, PortfolioGroup
    
- Instrument(상품 마스터), Exchange, InstrumentIdentifier
    
- HoldingLot(선택), PositionSnapshot(캐시), PortfolioValuationSnapshot(캐시)
    
- Transaction, TransactionLeg(복식부기 스타일 권장), CorporateAction
    
- PriceBar(EOD/OHLCV), PriceTick(실시간), FXRate
    
- Benchmark, BenchmarkConstituent(선택), BacktestRun, BacktestConfig, BacktestResultSeries, BacktestTradeLog
    
- AlertRule, NotificationEvent
    
- DataSource, DataIngestionLog
    

---

### 1.3 테이블 상세(핵심 위주)

#### A) 사용자/권한

**users**

- id (PK, UUID)
    
- email (unique)
    
- password_hash (nullable if SSO)
    
- display_name
    
- locale (ko-KR)
    
- created_at, updated_at, deleted_at
    

**workspaces** (v2 공유 대비, MVP는 user=workspace 1:1로 써도 됨)

- id (PK)
    
- owner_user_id (FK users.id)
    
- name
    
- created_at
    

**workspace_members**

- workspace_id (FK)
    
- user_id (FK)
    
- role (OWNER/EDITOR/VIEWER)
    
- PK(workspace_id, user_id)
    

---

#### B) 포트폴리오

**portfolio_groups**

- id (PK)
    
- workspace_id (FK)
    
- name
    
- sort_order
    
- created_at
    

**portfolios**

- id (PK)
    
- workspace_id (FK)
    
- group_id (FK, nullable)
    
- name
    
- description
    
- base_currency (KRW 기본)
    
- type (REAL/HYPOTHETICAL/BACKTEST_ONLY)
    
- tags (jsonb)
    
- created_at, updated_at
    
- archived_at (nullable)
    

**portfolio_targets** (목표비중)

- id (PK)
    
- portfolio_id (FK)
    
- instrument_id (FK, nullable) // 현금 목표는 null + currency로 표현 가능
    
- asset_class (EQUITY/BOND/COMMODITY/CASH/ALT)
    
- currency (nullable)
    
- target_weight (decimal(7,4)) // 0~1
    
- min_weight, max_weight (nullable)
    
- updated_at
    
- unique(portfolio_id, instrument_id, currency)
    

**portfolio_constraints**

- id (PK)
    
- portfolio_id (FK)
    
- constraint_type (MAX_SINGLE_NAME, MAX_SECTOR, MIN_CASH, REGION_CAP 등)
    
- params (jsonb)
    
- enabled (bool)
    

---

#### C) 상품/시세/환율

**exchanges**

- id (PK)
    
- code (KRX, NYSE, NASDAQ, …)
    
- timezone (IANA)
    
- currency (KRW/USD…)
    
- trading_hours (jsonb) // 장시간/휴장 정책은 별도 서비스가 담당해도 됨
    

**instruments**

- id (PK)
    
- instrument_type (STOCK/ETF/ETN/BOND/COMMODITY_INDEX/CASH_PROXY)
    
- name
    
- ticker (nullable) // 일부는 ISIN 기반
    
- exchange_id (FK, nullable)
    
- currency (KRW/USD…)
    
- country (KR/US/…)
    
- asset_class (EQUITY/BOND/COMMODITY/CASH/ALT)
    
- sector, industry (nullable)
    
- provider (ETF 운용사 등, nullable)
    
- expense_ratio (nullable)
    
- benchmark_index (nullable)
    
- status (ACTIVE/DELISTED)
    
- first_listed_at, delisted_at (nullable)
    

**instrument_identifiers**

- instrument_id (FK)
    
- id_type (ISIN/CUSIP/FIGI/SEDOL)
    
- value
    
- unique(id_type, value)
    

**price_bars** (백테스트/차트 기본)

- instrument_id (FK)
    
- timeframe (1D/1W/1M)
    
- ts (date or datetime, timeframe 기준)
    
- open, high, low, close (decimal)
    
- volume (bigint)
    
- adj_close (decimal, optional but 강력 추천)
    
- data_vendor
    
- PK(instrument_id, timeframe, ts)
    
- index(instrument_id, ts)
    

**price_ticks** (실시간/지연 시세)

- instrument_id (FK)
    
- ts (datetime)
    
- last
    
- bid, ask (nullable)
    
- change, change_pct (nullable)
    
- vendor
    
- PK(instrument_id, ts)
    

**fx_rates**

- base_currency (KRW)
    
- quote_currency (USD 등) // “1 USD = ? KRW”처럼 정의를 고정
    
- ts (datetime or date)
    
- rate
    
- source
    
- PK(base_currency, quote_currency, ts)
    
- index(quote_currency, ts)
    

> 권장: FX는 “일자별(EOD)”와 “실시간”을 분리하거나, ts로 통일하되 조회 정책을 명확히.

---

#### D) 거래(원장) — 가장 중요

**transactions**

- id (PK)
    
- portfolio_id (FK)
    
- occurred_at (datetime) // 체결시각/기준시각
    
- settle_date (date, nullable) // 결제일(채권/해외 등)
    
- type (BUY/SELL/DIVIDEND/INTEREST/FEE/TAX/DEPOSIT/WITHDRAW/FX_CONVERT/SPLIT/MERGER/TRANSFER)
    
- status (POSTED/VOID/PENDING)
    
- note (text)
    
- tags (jsonb)
    
- created_at, updated_at
    

**transaction_legs** (복식부기 스타일: 자산/현금/수수료/세금 분리)

- id (PK)
    
- transaction_id (FK)
    
- leg_type (ASSET/CASH/FEE/TAX/INCOME/FX)
    
- instrument_id (FK, nullable) // 자산 leg만
    
- currency (KRW/USD…)
    
- quantity (decimal, nullable) // 자산 leg에서 +
    
- price (decimal, nullable) // 체결단가
    
- amount (decimal) // 통화 기준 금액(현금/수수료/세금은 -)
    
- fx_rate_to_base (decimal, nullable) // 발생시점 환율 고정 저장 가능(권장)
    
- account (string, optional) // 현금계정 구분
    
- index(transaction_id)
    

**corporate_actions** (가능하면 별도)

- id (PK)
    
- instrument_id (FK)
    
- action_type (SPLIT/REVERSE_SPLIT/DIVIDEND/CASH_MERGER/SPINOFF)
    
- ex_date, pay_date (nullable)
    
- ratio (decimal, nullable)
    
- cash_amount (decimal, nullable)
    
- currency
    
- metadata (jsonb)
    

> MVP에서는 corporate action을 전부 자동화하지 말고, **가격이 adj_close를 제공하면 “성과 계산”은 해결**되고, “보유수량”만 사용자가 거래로 보정 가능하게 설계하면 안전합니다.

---

#### E) 포지션/평가 캐시(조회 성능)

**position_snapshots**

- portfolio_id
    
- as_of (datetime)
    
- instrument_id
    
- quantity
    
- avg_cost (portfolio base currency 기준 또는 instrument currency 기준 중 택1)
    
- market_value_base
    
- unrealized_pnl_base
    
- realized_pnl_base
    
- PK(portfolio_id, as_of, instrument_id)
    
- index(portfolio_id, as_of)
    

**portfolio_valuation_snapshots**

- portfolio_id
    
- as_of
    
- total_market_value_base
    
- cash_value_base
    
- day_pnl_base
    
- total_pnl_base
    
- twr_to_date (nullable)
    
- mwr_to_date (nullable)
    
- PK(portfolio_id, as_of)
    

> 실시간 평가용은 as_of를 “분 단위”로 저장하거나, 캐시 서버(REDIS)로만 처리해도 됨.

---

#### F) 백테스트

**benchmarks**

- id (PK)
    
- name
    
- type (INDEX/ETF_PROXY)
    
- base_currency
    
- instrument_id (nullable) // ETF proxy로 쓸 경우
    

**backtest_configs**

- id (PK)
    
- workspace_id
    
- name
    
- start_date, end_date
    
- initial_capital_base
    
- rebalance_type (NONE/PERIODIC/BAND)
    
- rebalance_period (MONTHLY/QUARTERLY… nullable)
    
- band_threshold (decimal nullable)
    
- fee_model (jsonb) // 수수료/슬리피지/세금
    
- dividend_reinvest (bool)
    
- price_mode (ADJ_CLOSE/CLOSE)
    
- params (jsonb) // 룰 기반 확장
    

**backtest_runs**

- id (PK)
    
- config_id (FK)
    
- created_by (user_id)
    
- status (RUNNING/SUCCEEDED/FAILED)
    
- started_at, finished_at
    
- error_message (nullable)
    

**backtest_result_series**

- run_id (FK)
    
- ts (date)
    
- equity_curve_base
    
- drawdown
    
- cash_base
    
- PK(run_id, ts)
    

**backtest_trade_logs**

- run_id (FK)
    
- ts
    
- instrument_id
    
- action (BUY/SELL/REBALANCE)
    
- quantity
    
- price
    
- fee
    
- reason (jsonb)
    
- index(run_id, ts)
    

---

### 1.4 인덱스/성능 포인트

- price_bars(instrument_id, ts) 필수
    
- transactions(portfolio_id, occurred_at) 필수
    
- transaction_legs(transaction_id), position_snapshots(portfolio_id, as_of)
    
- 실시간 화면은 “포트폴리오 합산”이 잦으므로 portfolio_valuation_snapshots 최신 1건 빠르게 조회
    

---

## 2) API 명세(REST 기준, v1)

### 2.1 공통

- Base: `/api/v1`
    
- Auth: Bearer JWT (또는 세션)
    
- 응답 포맷:
    

```json
{ "data": ..., "meta": {...}, "error": null }
```

- 페이지네이션: `?limit=50&cursor=...`
    

---

### 2.2 포트폴리오

**GET /portfolios**

- 설명: 포트 목록
    
- Query: group_id, archived=false
    
- Response: id, name, base_currency, total_value, day_pnl, updated_at
    

**POST /portfolios**

- Body: name, base_currency, type, group_id, tags
    
- Response: portfolio
    

**GET /portfolios/{id}**

- 상세 + 요약 지표
    

**PATCH /portfolios/{id}**

- 이름/설명/태그/그룹/아카이브
    

**DELETE /portfolios/{id}**

- 정책: soft delete 권장(archived_at)
    

---

### 2.3 목표비중/제약

**GET /portfolios/{id}/targets**  
**PUT /portfolios/{id}/targets**

- Body: target list (instrument_id or cash currency), 합 1.0 검증
    
- 서버 옵션: normalize=true
    

**GET /portfolios/{id}/constraints**  
**PUT /portfolios/{id}/constraints**

---

### 2.4 거래

**GET /portfolios/{id}/transactions**

- Query: from, to, type, tag
    

**POST /portfolios/{id}/transactions**

- Body: type, occurred_at, note, legs[]
    
- Legs 예시(매수):
    

```json
{
  "type": "BUY",
  "occurred_at": "2026-02-06T10:31:00+09:00",
  "legs": [
    { "leg_type": "ASSET", "instrument_id": "AAPL", "quantity": 2, "price": 190.5, "currency": "USD", "amount": 381.0 },
    { "leg_type": "CASH", "currency": "USD", "amount": -381.0 },
    { "leg_type": "FEE", "currency": "USD", "amount": -1.0 }
  ]
}
```

**POST /transactions/{id}/void**

- 거래 취소(VOID) + 스냅샷 재계산 트리거
    

---

### 2.5 평가/실시간

**GET /portfolios/{id}/valuation**

- Query: as_of=now|datetime, mode=realtime|eod
    
- Response:
    
    - total_value_base, cash_base, day_pnl_base, total_pnl_base
        
    - positions[]: instrument_id, qty, mv_base, pnl_base, weight
        
    - fx_used: { USDKRW: 1320.5, ... }
        
    - price_timestamp (각 종목 최신 시세 시각)
        

**GET /portfolios/{id}/performance**

- Query: from/to, metric=TWR|MWR|SIMPLE, frequency=DAILY|MONTHLY
    
- Response: series[], summary stats
    

---

### 2.6 비교

**POST /compare/performance**

- Body:
    

```json
{
  "portfolio_ids": ["p1","p2"],
  "benchmarks": ["KOSPI","SPY_PROXY"],
  "from": "2024-01-01",
  "to": "2026-02-06",
  "metric": "TWR",
  "currency_mode": "BASE"
}
```

- Response:
    
    - curves[] (id, label, series)
        
    - stats_table (vol, mdd, sharpe, beta…)
        

---

### 2.7 백테스트

**POST /backtests/configs**

- config 저장
    

**POST /backtests/runs**

- Body: config_id OR inline config
    
- Response: run_id, status
    

**GET /backtests/runs/{run_id}**

- 상태/에러
    

**GET /backtests/runs/{run_id}/results**

- equity curve + stats + trade logs 요약
    

---

### 2.8 상품/시세/환율

**GET /instruments/search?q=**

- Response: candidates
    

**GET /instruments/{id}**

- 상세
    

**GET /prices/{instrument_id}**

- Query: timeframe, from, to, adjusted=true
    

**GET /fx?base=KRW&quote=USD&from&to**

---

## 3) 계산 스펙(정확도가 제품 신뢰를 좌우)

### 3.1 실시간 평가액(Valuation)

**정의**

- 각 포지션의 시장가치:
    
    - `MV_i = qty_i * last_price_i` (instrument currency)
        
- 기준통화 환산:
    
    - `MV_i_base = MV_i * FX(instrument_currency → base_currency)`
        
- 포트 총액:
    
    - `Total = Σ MV_i_base + Cash_base`
        

**가격 선택 정책**

- realtime 모드:
    
    - last_tick(가능하면) else latest close
        
- eod 모드:
    
    - close 또는 adj_close(설정)
        

**환율 선택 정책**

- 기본: 최근 FX (realtime)
    
- 옵션: “거래 환율 고정(legs.fx_rate_to_base)” 기반 손익 분해(고급)
    

---

### 3.2 손익(PnL) — 실현/미실현

**평단 방식(기본: 평균단가)**

- 매수:
    
    - new_avg_cost = (old_qty_old_avg + buy_qty_buy_price + fee_alloc) / (old_qty + buy_qty)
        
- 매도:
    
    - realized_pnl = (sell_price - avg_cost) * sell_qty - sell_fee - sell_tax
        
    - qty 감소, avg_cost는 유지
        

> 통화가 다른 경우: **instrument currency 기준으로 avg_cost를 유지**하고, 평가 시점에 base 환산이 계산적으로 안정적입니다.

---

### 3.3 수익률: TWR vs MWR

#### (1) TWR(Time-Weighted Return) — “전략 성과”

현금 유입/유출 영향 제거.

- 평가시점을 현금흐름 시점으로 구간 분할
    
- 각 구간 수익률:
    
    - `r_k = (V_end - CF_k) / V_start - 1`
        
    - CF_k: 구간 말 직전에 발생한 순현금흐름(입출금/배당/수수료 등 정책에 따라 포함)
        
- 전체 TWR:
    
    - `TWR = Π(1 + r_k) - 1`
        

**현금흐름 포함 정책(권장 기본)**

- DEPOSIT/WITHDRAW/FX_CONVERT: CF로 처리
    
- FEE/TAX: CF로 처리(실제 성과에 반영)
    
- DIVIDEND/INTEREST: CF로 처리하되, “재투자”면 같은 시점에 매수거래가 추가되어 효과 반영
    

#### (2) MWR(Money-Weighted Return) / IRR — “내 체감 수익”

현금흐름을 포함한 내부수익률.

- 현금흐름 리스트 구성:
    
    - 투자(입금/매수로 인한 현금 유출) = 음수
        
    - 회수(출금/매도로 인한 현금 유입) = 양수
        
    - 종료 시점 포트 평가액을 최종 양수 CF로 추가
        
- IRR은 NPV=0이 되는 할인율 r:
    
    - `Σ CF_t / (1+r)^(Δt/365) = 0`
        

**실무 팁**

- MWR은 사용자 입장에 직관적이지만 변동성이 커서, 기본 표시는 TWR 추천 + 상세에서 MWR 병기.
    

---

### 3.4 벤치마크 비교(시장수익률)

- 벤치마크는 2가지 방식 지원:
    
    1. **지수 시계열 제공**(이상적)
        
    2. **ETF Proxy**(SPY, QQQ, AGG, GLD 같은 대체재)
        

비교 지표:

- 초과수익(Alpha): `Portfolio - Benchmark`
    
- 베타/알파(회귀):
    
    - `R_p = α + β R_b + ε`
        
- 트래킹에러(TE): 초과수익의 표준편차(연율)
    

---

### 3.5 리스크 지표(기본 세트)

- 변동성(연율): `std(daily_returns) * sqrt(252)`
    
- MDD: 누적곡선의 최고점 대비 최저점 낙폭
    
- Sharpe: `(mean(R) - Rf) / std(R)` (Rf는 설정값 기본 0 또는 단기금리)
    
- Sortino: 하방 변동성 사용
    
- Calmar: `CAGR / MDD`
    

---

### 3.6 백테스트 계산 정책(현실 반영 옵션)

- 가격: adj_close 우선(배당/분할 반영)
    
- 리밸런싱:
    
    - 목표비중 W, 현재가치 V → 목표수량 계산
        
    - 거래비용 적용 후 현금 부족 시:
        
        - (기본) 비중을 proportionally scale-down
            
        - (옵션) 우선순위(이탈 큰 종목부터)만 체결
            
- 비용 모델:
    
    - 수수료: `max(min_fee, notional * fee_rate)`
        
    - 슬리피지: `price * (1 ± slip_rate)` (매수/매도 방향 반영)
        
- 세금(간단):
    
    - (옵션) 매도 시 notional * tax_rate
        
    - 국가별 정교화는 v2
        

---

## 4) 모바일 UI/UX 흐름 & 와이어프레임(텍스트)

### 4.1 온보딩(3분 내 “가치 체감” 목표)

**Step 1**: “무엇을 할 수 있나요?”

- 카드 3개: 실시간 평가 / 비교 / 백테스트  
    **Step 2**: “첫 포트 만들기”
    
- 템플릿: 60/40, 올웨더, S&P+채권, 배당, 현금비중 높은 방어형  
    **Step 3**: “자산 추가 방식”
    
- (A) 지금 보유 입력(수량/평단)
    
- (B) 목표비중만 설정(가상/전략)
    

---

### 4.2 홈(대시보드) 와이어

- [상단] 전체자산(합산) / 오늘손익 / 누적손익
    
- [필터칩] 전체 | 장기 | 단기 | 연금 | 즐겨찾기
    
- [카드1] 리밸런싱 필요 TOP3
    
- [카드2] 이번 달 배당(예상/확정)
    
- [리스트] 포트폴리오 목록(평가액/일손익/알림 배지)
    

**핵심 UX**

- 숫자 탭하면 “원 단위/축약 단위” 토글
    
- 길게 누르면 “포트 고정/그룹 이동/복제”
    

---

### 4.3 포트폴리오 상세 와이어

- 헤더: 포트명 + 설정(⋮)
    
- 큰 숫자: 평가액 / 오늘손익
    
- [탭]
    
    1. 구성: 도넛(자산군↔종목) + 테이블
        
    2. 성과: 기간칩(1M/3M/YTD/1Y/전체) + 누적곡선
        
    3. 리스크: MDD/Vol/Sharpe + 드로다운 그래프
        
    4. 거래: 거래 리스트 + “+거래”
        
    5. 인사이트: 편중/상관/환율기여
        

**구성 탭 테이블 기본 정렬**

- 비중 DESC
    
- 스와이프 액션: (왼) 목표비중 수정, (오) 거래 추가
    

---

### 4.4 비교 화면 와이어

- 상단: [기간] [수익률: TWR/MWR] [통화: KRW/USD]
    
- 포트 선택 chips (최대 5)
    
- 차트:
    
    - 누적수익률(라인)
        
    - 드로다운(토글)
        
- 지표표(스크롤):
    
    - CAGR / MDD / Vol / Sharpe / Beta / TE
        
- 하단 인사이트:
    
    - “A는 수익이 높지만 변동성↑”
        
    - “B는 벤치마크 대비 알파 +”
        

---

### 4.5 백테스트 스튜디오 와이어

- 입력(상단 스텝)
    
    1. 전략 선택(정적/주기/밴드)
        
    2. 자산 구성(목표비중)
        
    3. 비용/배당/세금(토글)
        
    4. 벤치마크 선택
        
- 결과
    
    - 성과 요약 카드
        
    - 연도별 테이블
        
    - 리밸런싱 로그(언제 무엇을 얼마나)
        

---

## 5) 개발 우선순위(추천) — “정확도 → 속도 → 고급”

1. 거래/평가/환산(정확한 원장 + valuation)
    
2. 포트 비교(TWR 기반)
    
3. 백테스트(정적+주기 리밸런싱)
    
4. 리스크/회귀(베타/알파)
    
5. 알림/인사이트
    

---

## 6) 다음 산출물(바로 이어서 작성 가능)

원하면 내가 **바로 이어서** 아래까지 만들어줄게요(문서 수준으로):

- **(A) OpenAPI(Swagger) YAML 초안** (엔드포인트/스키마/에러코드)
    
- **(B) 이벤트/배치 설계**: 가격 수집, FX 수집, 스냅샷 생성 주기, 캐시 전략(REDIS)
    
- **(C) 화면별 상세 UI 스펙**: 컴포넌트, 상태(loading/empty/error), 마이크로카피, 접근성
    

지금 단계에서 가장 도움이 되는 걸로는 **A(스웨거) + B(데이터 파이프라인/캐시)** 조합이 보통 개발이 빨라집니다.