
---

## (A) OpenAPI(Swagger) v1 초안 (핵심 엔드포인트 + 스키마)

> 아래 YAML은 “최소하지만 실전형” 구조입니다. 실제로는 파일 분리 권장(components/schemas, paths 등).  
> 백테스트 실행은 **비동기(run_id 반환)** 방식으로 정의했습니다.

```yaml
openapi: 3.0.3
info:
  title: Portfolio Manager API
  version: 1.0.0
servers:
  - url: /api/v1

security:
  - bearerAuth: []

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    ApiResponse:
      type: object
      properties:
        data: {}
        meta:
          type: object
          additionalProperties: true
        error:
          $ref: '#/components/schemas/ApiError'
      required: [data, meta, error]

    ApiError:
      type: object
      properties:
        code:
          type: string
          example: VALIDATION_ERROR
        message:
          type: string
        details:
          type: object
          additionalProperties: true

    CurrencyCode:
      type: string
      example: KRW

    Portfolio:
      type: object
      properties:
        id: { type: string, example: "p_123" }
        workspace_id: { type: string, example: "w_1" }
        group_id: { type: string, nullable: true }
        name: { type: string }
        description: { type: string, nullable: true }
        base_currency: { $ref: '#/components/schemas/CurrencyCode' }
        type:
          type: string
          enum: [REAL, HYPOTHETICAL, BACKTEST_ONLY]
        tags:
          type: array
          items: { type: string }
        archived_at:
          type: string
          format: date-time
          nullable: true
        created_at: { type: string, format: date-time }
        updated_at: { type: string, format: date-time }
      required: [id, workspace_id, name, base_currency, type, created_at, updated_at]

    Instrument:
      type: object
      properties:
        id: { type: string, example: "ins_AAPL" }
        name: { type: string, example: "Apple Inc." }
        ticker: { type: string, nullable: true, example: "AAPL" }
        instrument_type:
          type: string
          enum: [STOCK, ETF, ETN, BOND, COMMODITY_INDEX, CASH_PROXY]
        asset_class:
          type: string
          enum: [EQUITY, BOND, COMMODITY, CASH, ALT]
        exchange: { type: string, nullable: true, example: "NASDAQ" }
        currency: { $ref: '#/components/schemas/CurrencyCode' }
        country: { type: string, example: "US" }
        sector: { type: string, nullable: true }
        provider: { type: string, nullable: true }
        expense_ratio: { type: number, nullable: true }
        status:
          type: string
          enum: [ACTIVE, DELISTED]
      required: [id, name, instrument_type, asset_class, currency, status]

    Target:
      type: object
      properties:
        instrument_id: { type: string, nullable: true, description: "null이면 현금 타겟" }
        currency: { type: string, nullable: true, description: "현금 타겟일 때 필수" }
        asset_class:
          type: string
          enum: [EQUITY, BOND, COMMODITY, CASH, ALT]
        target_weight:
          type: number
          format: double
          description: "0~1"
        min_weight: { type: number, nullable: true }
        max_weight: { type: number, nullable: true }
      required: [asset_class, target_weight]

    Transaction:
      type: object
      properties:
        id: { type: string, example: "tx_1" }
        portfolio_id: { type: string, example: "p_123" }
        occurred_at: { type: string, format: date-time }
        settle_date: { type: string, format: date, nullable: true }
        type:
          type: string
          enum: [BUY, SELL, DIVIDEND, INTEREST, FEE, TAX, DEPOSIT, WITHDRAW, FX_CONVERT, SPLIT, MERGER, TRANSFER]
        status:
          type: string
          enum: [POSTED, VOID, PENDING]
        note: { type: string, nullable: true }
        tags:
          type: array
          items: { type: string }
        legs:
          type: array
          items: { $ref: '#/components/schemas/TransactionLeg' }
      required: [id, portfolio_id, occurred_at, type, status, legs]

    TransactionLeg:
      type: object
      properties:
        leg_type:
          type: string
          enum: [ASSET, CASH, FEE, TAX, INCOME, FX]
        instrument_id: { type: string, nullable: true }
        currency: { $ref: '#/components/schemas/CurrencyCode' }
        quantity: { type: number, nullable: true }
        price: { type: number, nullable: true }
        amount:
          type: number
          description: "통화 기준 금액(현금/수수료/세금은 -)"
        fx_rate_to_base:
          type: number
          nullable: true
          description: "발생 시점 환율 고정(옵션)"
      required: [leg_type, currency, amount]

    Valuation:
      type: object
      properties:
        portfolio_id: { type: string }
        as_of: { type: string, format: date-time }
        mode:
          type: string
          enum: [REALTIME, EOD]
        total_value_base: { type: number }
        cash_value_base: { type: number }
        day_pnl_base: { type: number }
        total_pnl_base: { type: number }
        positions:
          type: array
          items: { $ref: '#/components/schemas/ValuationPosition' }
        fx_used:
          type: object
          additionalProperties: { type: number }
        price_timestamp:
          type: object
          additionalProperties: { type: string, format: date-time }
      required: [portfolio_id, as_of, mode, total_value_base, cash_value_base, positions]

    ValuationPosition:
      type: object
      properties:
        instrument_id: { type: string }
        quantity: { type: number }
        avg_cost: { type: number, nullable: true }
        market_price: { type: number }
        market_value_base: { type: number }
        unrealized_pnl_base: { type: number }
        realized_pnl_base: { type: number }
        weight: { type: number }
        day_pnl_base: { type: number, nullable: true }
      required: [instrument_id, quantity, market_price, market_value_base, weight]

    PerformancePoint:
      type: object
      properties:
        ts: { type: string, format: date }
        value: { type: number }
      required: [ts, value]

    PerformanceSeries:
      type: object
      properties:
        id: { type: string, example: "p_123" }
        label: { type: string, example: "My Portfolio" }
        metric:
          type: string
          enum: [TWR, MWR, SIMPLE]
        currency_mode:
          type: string
          enum: [BASE, NATIVE]
        points:
          type: array
          items: { $ref: '#/components/schemas/PerformancePoint' }
      required: [id, label, metric, currency_mode, points]

    CompareRequest:
      type: object
      properties:
        portfolio_ids:
          type: array
          items: { type: string }
          maxItems: 5
        benchmarks:
          type: array
          items: { type: string }
          maxItems: 3
        from: { type: string, format: date }
        to: { type: string, format: date }
        metric:
          type: string
          enum: [TWR, MWR, SIMPLE]
        currency_mode:
          type: string
          enum: [BASE, NATIVE]
      required: [portfolio_ids, from, to, metric, currency_mode]

    CompareResponse:
      type: object
      properties:
        curves:
          type: array
          items: { $ref: '#/components/schemas/PerformanceSeries' }
        stats_table:
          type: array
          items:
            type: object
            properties:
              id: { type: string }
              label: { type: string }
              cagr: { type: number, nullable: true }
              vol: { type: number, nullable: true }
              mdd: { type: number, nullable: true }
              sharpe: { type: number, nullable: true }
              beta: { type: number, nullable: true }
              tracking_error: { type: number, nullable: true }

    BacktestConfig:
      type: object
      properties:
        id: { type: string }
        name: { type: string }
        start_date: { type: string, format: date }
        end_date: { type: string, format: date }
        initial_capital_base: { type: number }
        rebalance_type:
          type: string
          enum: [NONE, PERIODIC, BAND]
        rebalance_period:
          type: string
          nullable: true
          enum: [MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL]
        band_threshold: { type: number, nullable: true }
        dividend_reinvest: { type: boolean }
        price_mode:
          type: string
          enum: [ADJ_CLOSE, CLOSE]
        fee_model:
          type: object
          additionalProperties: true
        targets:
          type: array
          items: { $ref: '#/components/schemas/Target' }
      required: [name, start_date, end_date, initial_capital_base, rebalance_type, dividend_reinvest, price_mode, targets]

    BacktestRun:
      type: object
      properties:
        id: { type: string, example: "br_1" }
        config_id: { type: string }
        status:
          type: string
          enum: [RUNNING, SUCCEEDED, FAILED]
        started_at: { type: string, format: date-time }
        finished_at: { type: string, format: date-time, nullable: true }
        error_message: { type: string, nullable: true }

paths:
  /portfolios:
    get:
      summary: List portfolios
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse'
    post:
      summary: Create portfolio
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                name: { type: string }
                base_currency: { $ref: '#/components/schemas/CurrencyCode' }
                type: { type: string, enum: [REAL, HYPOTHETICAL, BACKTEST_ONLY] }
                group_id: { type: string, nullable: true }
                tags:
                  type: array
                  items: { type: string }
              required: [name, base_currency, type]
      responses:
        '201':
          description: Created

  /portfolios/{portfolioId}:
    get:
      summary: Get portfolio detail
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }
    patch:
      summary: Update portfolio
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }

  /portfolios/{portfolioId}/targets:
    get:
      summary: Get targets
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }
    put:
      summary: Replace targets (must sum to 1.0)
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
        - name: normalize
          in: query
          required: false
          schema: { type: boolean, default: false }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                targets:
                  type: array
                  items: { $ref: '#/components/schemas/Target' }
              required: [targets]
      responses:
        '200': { description: OK }

  /portfolios/{portfolioId}/transactions:
    get:
      summary: List transactions
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
        - name: from
          in: query
          schema: { type: string, format: date }
        - name: to
          in: query
          schema: { type: string, format: date }
        - name: type
          in: query
          schema: { type: string }
      responses:
        '200': { description: OK }
    post:
      summary: Create transaction
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                occurred_at: { type: string, format: date-time }
                type:
                  type: string
                  enum: [BUY, SELL, DIVIDEND, INTEREST, FEE, TAX, DEPOSIT, WITHDRAW, FX_CONVERT, SPLIT, MERGER, TRANSFER]
                note: { type: string, nullable: true }
                tags:
                  type: array
                  items: { type: string }
                legs:
                  type: array
                  items: { $ref: '#/components/schemas/TransactionLeg' }
              required: [occurred_at, type, legs]
      responses:
        '201': { description: Created }

  /transactions/{transactionId}/void:
    post:
      summary: Void a posted transaction (soft cancel)
      parameters:
        - name: transactionId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }

  /portfolios/{portfolioId}/valuation:
    get:
      summary: Get portfolio valuation (realtime or eod)
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
        - name: mode
          in: query
          required: false
          schema:
            type: string
            enum: [REALTIME, EOD]
            default: REALTIME
        - name: as_of
          in: query
          required: false
          schema:
            type: string
            description: "now or ISO datetime"
      responses:
        '200':
          description: OK

  /portfolios/{portfolioId}/performance:
    get:
      summary: Get performance time series
      parameters:
        - name: portfolioId
          in: path
          required: true
          schema: { type: string }
        - name: from
          in: query
          required: true
          schema: { type: string, format: date }
        - name: to
          in: query
          required: true
          schema: { type: string, format: date }
        - name: metric
          in: query
          schema:
            type: string
            enum: [TWR, MWR, SIMPLE]
            default: TWR
        - name: frequency
          in: query
          schema:
            type: string
            enum: [DAILY, WEEKLY, MONTHLY]
            default: DAILY
      responses:
        '200': { description: OK }

  /compare/performance:
    post:
      summary: Compare portfolios and benchmarks
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/CompareRequest' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ApiResponse' }

  /instruments/search:
    get:
      summary: Search instruments
      parameters:
        - name: q
          in: query
          required: true
          schema: { type: string }
        - name: asset_class
          in: query
          schema: { type: string }
        - name: country
          in: query
          schema: { type: string }
      responses:
        '200': { description: OK }

  /instruments/{instrumentId}:
    get:
      summary: Instrument detail
      parameters:
        - name: instrumentId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }

  /prices/{instrumentId}:
    get:
      summary: Get price series
      parameters:
        - name: instrumentId
          in: path
          required: true
          schema: { type: string }
        - name: timeframe
          in: query
          schema:
            type: string
            enum: [1D, 1W, 1M]
            default: 1D
        - name: from
          in: query
          schema: { type: string, format: date }
        - name: to
          in: query
          schema: { type: string, format: date }
        - name: adjusted
          in: query
          schema: { type: boolean, default: true }
      responses:
        '200': { description: OK }

  /fx:
    get:
      summary: Get FX series
      parameters:
        - name: base
          in: query
          required: true
          schema: { type: string, example: "KRW" }
        - name: quote
          in: query
          required: true
          schema: { type: string, example: "USD" }
        - name: from
          in: query
          schema: { type: string, format: date }
        - name: to
          in: query
          schema: { type: string, format: date }
      responses:
        '200': { description: OK }

  /backtests/configs:
    post:
      summary: Create backtest config
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/BacktestConfig' }
      responses:
        '201': { description: Created }

  /backtests/runs:
    post:
      summary: Start backtest run (async)
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                config_id: { type: string }
                inline_config: { $ref: '#/components/schemas/BacktestConfig' }
              oneOf:
                - required: [config_id]
                - required: [inline_config]
      responses:
        '202':
          description: Accepted
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse'

  /backtests/runs/{runId}:
    get:
      summary: Get backtest run status
      parameters:
        - name: runId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }

  /backtests/runs/{runId}/results:
    get:
      summary: Get backtest results
      parameters:
        - name: runId
          in: path
          required: true
          schema: { type: string }
      responses:
        '200': { description: OK }
```

### A-1) 표준 에러코드(권장)

- `AUTH_REQUIRED` (401)
    
- `FORBIDDEN` (403)
    
- `NOT_FOUND` (404)
    
- `VALIDATION_ERROR` (422)
    
    - 예: 타겟 비중 합이 1.0이 아님, 거래 legs가 복식부기 불일치
        
- `DATA_UNAVAILABLE` (503)
    
    - 예: 시세/환율 소스 장애
        
- `BACKTEST_FAILED` (500)
    
    - error.details에 run_id, reason, instrument_id 포함
        

### A-2) 검증 규칙(서버)

- 거래 생성 시:
    
    - legs 합계 검증(통화별 혹은 base 환산 후 합계=0 형태를 권장)
        
    - BUY/SELL은 ASSET leg가 정확히 1개(혹은 분할 매수면 복수 허용)
        
- targets 저장 시:
    
    - CASH 타겟은 `instrument_id=null` + `currency!=null`
        
    - sum(target_weight)=1.0(±0.0005 허용) + normalize 옵션 제공
        

---

## (B) 데이터 파이프라인 / 배치 / 캐시(REDIS) 설계

### B.1 컴포넌트 구성(권장 아키텍처)

- **API Server**: CRUD, 조회, 인증
    
- **Pricing Service**: 가격/환율 수집(실시간/지연/EOD)
    
- **Valuation Engine**: 포트 평가/스냅샷 생성(캐시/DB)
    
- **Backtest Worker**: 큐 기반 비동기 실행
    
- **Cache Layer (Redis)**: 최신 시세/환율/포트 평가 캐시
    
- **Job Queue**: BullMQ/Celery/SQS 등(백테스트/재계산)
    

---

### B.2 가격/환율 수집 전략

#### 1) Price (EOD)

- 스케줄: 매 영업일 장 마감 후 +30~60분
    
- 적재: `price_bars(timeframe=1D)` upsert
    
- 보정: 가능한 경우 `adj_close`도 적재(배당/분할 반영)
    

**리트라이**

- 3회 지수 백오프(1m, 5m, 15m)
    
- 실패 시 `data_ingestion_log`에 기록 + 알림(운영)
    

#### 2) Price (실시간/지연)

- 폴링 주기: 5~15초(유료) / 60초(무료)
    
- 저장:
    
    - 원칙: DB 저장은 비용 큼 → **Redis에 최신 틱만 저장**이 기본
        
    - 필요 시 1분 OHLC로 집계해 DB에 저장(차트용)
        

**Redis 키 예시**

- `tick:{instrument_id}` → `{ts,last,bid,ask,change,change_pct}`
    
- TTL: 60~180초
    

#### 3) FX (환율)

- 실시간: 10~60초 폴링
    
- EOD: 1일 1회(장마감 이후)
    
- Redis:
    
    - `fx:{base}:{quote}` → `{ts,rate}`
        
    - TTL: 300초
        

---

### B.3 평가(Valuation) 캐시 전략

#### 실시간 평가 요청 흐름

1. API가 포트 보유/현금 상태를 읽음
    
    - 빠르게: `position_snapshots` 최신(또는 Redis 캐시)
        
2. 최신 시세/환율을 Redis에서 조회
    
    - 없으면 fallback: DB의 마지막 close + “STALE” 플래그
        
3. 계산 후 응답
    
4. **계산 결과를 Redis에 캐시**
    
    - `val:{portfolio_id}` → valuation JSON
        
    - TTL: 10~30초(실시간), 5분(eod)
        

**Redis 키**

- `val:{portfolio_id}:realtime`
    
- `val:{portfolio_id}:eod:{date}`
    

#### 스냅샷 생성 배치(조회속도 개선)

- 스케줄: 매 1~5분(유료) / 15분(무료)
    
- 대상: “최근 조회된 포트” 또는 “즐겨찾기 포트”
    
- 저장: `portfolio_valuation_snapshots` (분 단위)
    
- 장점: 홈 로딩이 매우 빨라짐
    

---

### B.4 거래 변경 시 재계산 트리거(정합성 핵심)

#### 이벤트 기반

- Transaction POST/VOID 발생 → 이벤트 발행
    
    - `portfolio.recompute.requested` (portfolio_id, from_occurred_at)
        

#### 재계산 범위 최적화

- 원장 전체 재계산은 비싸므로:
    
    - **from_occurred_at 이후 구간만** 재계산(TWR/MWR 구간 포함)
        
    - 포지션은 “누적”이므로 checkpoint(스냅샷) 기준으로 재생성
        

#### 추천 방식

- 매일 1회 “EOD checkpoint snapshot” 생성
    
- 거래가 들어오면 최근 checkpoint부터 replay
    

---

### B.5 백테스트 워커/큐 설계

#### 실행 프로토콜

1. `/backtests/runs` 호출 → run 레코드 생성(status=RUNNING)
    
2. 큐에 job enqueue(run_id)
    
3. 워커가 데이터 준비(가격/FX 시계열) → 실행
    
4. 결과 저장 + status=SUCCEEDED/FAILED
    

**중요 정책**

- 동일 config + 동일 기간 + 동일 fee_model이면 결과 캐싱 가능(해시 키)
    
- 가격 데이터 누락:
    
    - 기본: 해당 종목이 상장 전이면 시작일을 상장일로 당겨 경고
        
    - 중간 결측이면 forward-fill(옵션) or 실패(엄격모드)
        

---

### B.6 운영/관측(Observability)

- 로그: ingestion latency, valuation latency, cache hit ratio
    
- 메트릭:
    
    - p95 valuation 응답시간
        
    - FX/price freshness(현재시각 - tick ts)
        
- 알림:
    
    - 데이터 소스 장애 > 5분 지속
        
    - 백테스트 실패율 급증
        

---

## (C) 화면별 상세 UI 스펙(컴포넌트/상태/마이크로카피)

아래는 모바일 기준(웹도 동일한 컴포넌트 재활용 가능)으로, “각 화면의 상태 머신”까지 명시합니다.

---

### C.1 홈(대시보드)

**상단 KPI 컴포넌트**

- Total Assets (KRW)
    
- Today PnL
    
- Total PnL
    
- 토글: `기준통화(KRW)` / `원자산 통화` (옵션)
    

**리스트(포트 카드)**

- 포트명, 평가액, 오늘손익, 최신갱신시각(“방금/1분 전”)
    
- 배지:
    
    - “리밸런싱 필요”
        
    - “데이터 지연”
        
    - “현금 부족”(리밸런싱 시뮬레이션 결과 기반)
        

**상태**

- Loading: 스켈레톤(포트 카드 3~5개)
    
- Empty: “아직 포트폴리오가 없어요. 1분만에 만들 수 있어요.”
    
    - CTA: `+ 첫 포트 만들기`
        
- Error(Data): “시세 정보를 불러오지 못했어요. 마지막 값을 표시합니다.”
    
    - 버튼: `재시도`, `상세(상태 페이지)`
        

**마이크로카피**

- 실시간 갱신 실패: “현재 시세가 지연되고 있어요. 마지막 시세로 계산했어요.”
    

---

### C.2 포트폴리오 상세

**헤더**

- 포트명 + (⋮) 메뉴: 편집/복제/스냅샷/아카이브
    

**탭 구조**

1. 구성
    
2. 성과
    
3. 리스크
    
4. 거래
    
5. 인사이트
    

#### 구성 탭 컴포넌트

- 도넛 차트(자산군/종목 토글)
    
- 테이블:
    
    - 종목명(티커), 비중, 평가액, 손익, 금일변동
        
    - 정렬: 비중/손익/변동성(추후)
        
- 퀵액션:
    
    - `목표비중 수정`
        
    - `리밸런싱 시뮬레이션`
        

**상태**

- 가격 stale: 테이블에 “(지연)” 라벨 + 상단 배너 1회
    
- 종목이 있지만 가격 누락: “가격 데이터가 없는 종목이 있어요(2개).”
    
    - CTA: “대체 종목 지정” or “가격 모드 변경(EOD)”
        

#### 거래 탭

- 상단 필터: 전체/매수/매도/배당/수수료 + 기간
    
- 리스트: 날짜, 타입, 금액, 메모
    
- FAB: `+ 거래 추가`
    
- Empty: “기록이 없어요. 첫 거래를 추가하면 성과가 계산돼요.”
    

---

### C.3 목표비중 편집(중요 UX)

**기본**

- 총합 100% 상단 고정 표시
    
- 입력 방식:
    
    - 슬라이더 + 숫자입력(0.1% 단위)
        
    - 드래그로 우선순위/정렬
        

**검증 UX**

- 합이 100%가 아닐 때:
    
    - 하단 고정 바: “현재 98.7%예요. 자동으로 맞출까요?”
        
    - 버튼: `자동 정규화` / `직접 수정`
        

**제약 입력**

- 종목별 min/max
    
- 자산군 cap
    
- 현금 최소비중
    

---

### C.4 비교 화면

**컨트롤**

- 기간: 1M/3M/YTD/1Y/전체/커스텀
    
- 수익률: TWR(기본)/MWR
    
- 벤치마크 추가: + 버튼(최대 3)
    

**차트**

- 누적수익률 라인
    
- 드로다운(토글)
    
- 탭하면 해당 날짜 값 툴팁(포트별)
    

**지표 테이블**

- 기본 6개: CAGR, Vol, MDD, Sharpe, Beta, TE
    
- 각 지표 옆 `i`:
    
    - “MDD: 최고점 대비 최대 낙폭이에요.”
        

**상태**

- 기간이 너무 짧아 지표 계산 불가:
    
    - “해당 기간은 데이터가 부족해 일부 지표가 숨겨졌어요.”
        

---

### C.5 백테스트 스튜디오

**Step UI**

1. 구성(타겟)
    
2. 리밸런싱
    
3. 비용/배당
    
4. 벤치마크
    
5. 실행
    

**실행 중**

- Progress UI(단계 표시):
    
    - 데이터 준비 → 시뮬레이션 → 지표 계산 → 저장
        
- 취소 버튼(가능하면)
    
- 결과 페이지로 자동 전환
    

**결과**

- 요약 카드: 누적수익률, CAGR, MDD, Sharpe, 승률(옵션)
    
- 연도별 테이블
    
- 리밸런싱 로그(접기/펼치기)
    

**마이크로카피(과최적화 경고)**

- “파라미터를 자주 조정하면 과거에만 잘 맞는 전략이 될 수 있어요.”
    

---

## 다음 이어서(선택 없이 바로 진행 가능)

원하면 다음 산출물도 바로 이어서 더 “개발 문서화” 해드릴게요.

1. **Swagger를 더 확장**: 응답 스키마를 실제 형태로 고정(PortfolioListResponse, ValuationResponse 등) + 에러 응답 표준화(HTTP별)
    
2. **계산 엔진 상세 설계**:
    
    - 포지션 재생성 알고리즘(Checkpoint + Replay)
        
    - TWR 구간 분할 규칙(현금흐름 타입별 포함/제외)
        
3. **QA 테스트 케이스**:
    
    - 거래 입력 엣지(부분매도, 수수료만, 환전 후 매수, 배당 재투자)
        
    - 백테스트 결측/상장 전/폐지 처리
        

원하는 방향이 “바로 개발 착수”면, 다음은 **(1) 계산 엔진 상세 설계 + (2) QA 테스트 케이스**를 먼저 내는 게 가장 효율적입니다.