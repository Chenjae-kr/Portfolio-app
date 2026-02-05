# 기술 스택

> Portfolio Manager App의 전체 기술 스택 및 아키텍처 구성

---

## 📦 Technology Stack Overview

```
Frontend  : Vue 3 + TypeScript + Vite + Pinia + ECharts
Backend   : Java 21 + Spring Boot 3.3+ + Spring Security (JWT)
Database  : PostgreSQL 16+ (Flyway migration)
ORM       : Spring Data JPA + Hibernate
Query     : jOOQ (집계/분석 쿼리)
Cache     : Redis 7
Queue     : RabbitMQ 3
Docs/Obs  : Swagger (springdoc) + Actuator + Micrometer
```

---

## 🔧 Backend Stack

### Core Framework

| 항목 | 기술 | 버전 | 용도 |
|------|------|------|------|
| Language | Java | 21 (LTS) | 성능/GC 개선, 최신 LTS |
| Framework | Spring Boot | 3.3+ | REST API, DI, AOP |
| Build Tool | Gradle | 8.x | 의존성 관리, 멀티모듈 |

### Spring Modules

```groovy
dependencies {
    // Web & API
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Security
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    
    // Data Access
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    
    // Cache & Queue
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    
    // Monitoring & Docs
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui'
    
    // Resilience & HTTP Client
    implementation 'io.github.resilience4j:resilience4j-spring-boot3'
    implementation 'org.springframework.boot:spring-boot-starter-webflux' // for WebClient
    
    // Query Builder (Optional)
    implementation 'org.jooq:jooq'
}
```

### 데이터 접근 전략

| Layer | Technology | 사용 시나리오 |
|-------|------------|--------------|
| **ORM** | Spring Data JPA + Hibernate | CRUD, 관계 매핑, 트랜잭션 관리 |
| **Query** | jOOQ | 복잡한 집계/분석 쿼리 (성과, 리스크) |
| **Cache** | Redis + Spring Cache | 실시간 시세, 평가액, FX |

### 인증/보안

```java
// JWT Configuration
@Configuration
public class SecurityConfig {
    - JWT Access Token (15분 ~ 1시간)
    - JWT Refresh Token (7일 ~ 30일)
    - BCryptPasswordEncoder (strength: 12)
    - CORS: localhost:5173 (dev), 도메인 (prod)
}
```

**권한 모델**
- Workspace Role: `OWNER` / `EDITOR` / `VIEWER`
- Spring Security Method Security (`@PreAuthorize`)

### 비동기/배치

| 항목 | 기술 | 용도 |
|------|------|------|
| Scheduler | Spring `@Scheduled` | 가격 EOD 수집, 스냅샷 생성 |
| Batch | Spring Batch (선택) | 대량 데이터 적재, 리포트 생성 |
| Queue | RabbitMQ | 백테스트 실행, 재계산 요청 |
| Lock | ShedLock + PostgreSQL | 배치 중복 실행 방지 |

### 외부 호출/Resilience

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pricingApi:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        
  retry:
    instances:
      pricingApi:
        max-attempts: 3
        wait-duration: 1s
```

### 로깅/관측

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  level:
    root: INFO
    com.portfolio: DEBUG
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 🗄️ Database Stack

### PostgreSQL 16+

**핵심 기능**
- JSON/JSONB: 설정, 메타데이터 저장
- Partitioning: `price_bars` 월별 파티셔닝
- Full-text Search: `pg_trgm` (종목명 검색)
- UUID: 분산 ID 생성

**마이그레이션**

```sql
-- Flyway 버전 관리
V1__init_schema.sql
V2__add_workspaces.sql
V3__add_portfolio_constraints.sql
...
```

**필수 인덱스**

```sql
-- 거래 조회
CREATE INDEX idx_transactions_portfolio_occurred 
  ON transactions(portfolio_id, occurred_at DESC);

-- 시세 조회  
CREATE INDEX idx_price_bars_instrument_ts 
  ON price_bars(instrument_id, ts DESC);

-- 환율 조회
CREATE INDEX idx_fx_rates_pair_ts 
  ON fx_rates(base_currency, quote_currency, ts DESC);
```

**파티셔닝 전략**

```sql
-- price_bars 월별 파티셔닝 (대용량 시계열 대비)
CREATE TABLE price_bars_2026_01 PARTITION OF price_bars
  FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

---

## 🚀 Cache & Queue

### Redis 7

**캐시 키 설계**

```
# 실시간 시세 (TTL: 60~180s)
tick:{instrument_id} → {ts, last, bid, ask, change_pct}

# 환율 (TTL: 300s)
fx:{base}:{quote} → {ts, rate}

# 포트폴리오 평가 (TTL: 10~30s)
val:{portfolio_id}:realtime → Valuation JSON
val:{portfolio_id}:eod:{date} → Valuation JSON

# 백테스트 결과 캐시 (TTL: 1h)
backtest:{config_hash}:{date_range} → Result JSON
```

**Spring Cache 설정**

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000 # 5분
      cache-null-values: false
```

### RabbitMQ

**Queue 구조**

```
Exchange: portfolio.topic (Topic Exchange)

Queue: backtest.run.queue
  - Routing Key: backtest.run.*
  - Consumer: BacktestWorker
  
Queue: valuation.recompute.queue
  - Routing Key: valuation.recompute.*
  - Consumer: ValuationWorker
```

---

## 🎨 Frontend Stack

### Core Framework

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0",
    "vue-i18n": "^9.9.0"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "@vitejs/plugin-vue": "^5.0.0",
    "vitest": "^1.1.0",
    "@vue/test-utils": "^2.4.0"
  }
}
```

### 상태 관리 (Pinia)

```typescript
stores/
├── auth.ts          // 인증, 사용자 정보
├── portfolio.ts     // 포트폴리오 CRUD
├── valuation.ts     // 실시간 평가
├── compare.ts       // 비교/분석
└── backtest.ts      // 백테스트
```

### 차트 라이브러리

**ECharts 5.x**
- 금융 차트 (라인, 캔들, 드로다운)
- 대규모 데이터 렌더링 최적화
- 다크 모드 지원

```typescript
// ECharts 설정 예시
const option = {
  xAxis: { type: 'time' },
  yAxis: { type: 'value' },
  series: [{
    type: 'line',
    data: performanceData,
    smooth: true
  }]
};
```

### API 통신

```typescript
// axios interceptor
axios.interceptors.request.use(config => {
  const token = authStore.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      await authStore.refreshToken();
      return axios.request(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 국제화 (i18n)

```typescript
// locales/ko.ts
export default {
  dashboard: {
    title: '대시보드',
    totalAssets: '총 자산',
    todayPnL: '오늘 손익'
  },
  portfolio: {
    create: '포트폴리오 생성',
    valuation: '평가액'
  }
};
```

---

## 🔄 DevOps & Infrastructure

### Docker Compose (개발 환경)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    
  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports: ["5672:5672", "15672:15672"]
```

### CI/CD Pipeline

```yaml
# GitHub Actions
name: CI/CD
on: [push, pull_request]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Run tests
        run: ./gradlew test
        
  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Run tests
        run: npm test
```

---

## 📦 모듈 구조

### Backend Package Structure

```
com.portfolio/
├── api/                    # Controllers, DTOs, OpenAPI
│   ├── AuthController
│   ├── PortfolioController
│   └── dto/
├── auth/                   # Security, JWT
│   ├── jwt/
│   └── service/
├── portfolio/              # Portfolio Domain
│   ├── entity/
│   ├── repository/
│   └── service/
├── ledger/                 # Transaction, Legs
│   ├── entity/
│   └── service/
├── pricing/                # Price/FX Clients
│   ├── client/
│   └── cache/
├── valuation/              # Valuation Engine
│   └── service/
├── analytics/              # Performance, Risk
│   └── service/
├── backtest/               # Backtest Engine
│   ├── config/
│   └── worker/
└── infra/                  # Redis, MQ, Config
    ├── cache/
    └── queue/
```

### Frontend Structure

```
src/
├── api/                    # API clients
│   ├── auth.ts
│   ├── portfolio.ts
│   └── valuation.ts
├── components/             # 재사용 컴포넌트
│   ├── layout/
│   ├── portfolio/
│   └── chart/
├── composables/            # Vue Composables
│   ├── useAuth.ts
│   └── usePortfolio.ts
├── router/                 # 라우팅
├── stores/                 # Pinia Stores
├── types/                  # TypeScript 타입
├── utils/                  # 유틸리티
└── views/                  # 페이지 컴포넌트
    ├── auth/
    ├── dashboard/
    └── portfolio/
```

---

## 🎯 기술 선택 이유

### 왜 Java 21?
- ✅ LTS 최신 버전 (장기 지원)
- ✅ Virtual Threads (경량 동시성)
- ✅ Record Pattern Matching (코드 간결성)
- ✅ 성능 개선 (GC, JIT)

### 왜 PostgreSQL?
- ✅ ACID 보장 (금융 데이터 정합성)
- ✅ JSON/JSONB 지원 (유연한 스키마)
- ✅ Partitioning (시계열 데이터 최적화)
- ✅ 무료 오픈소스, 커뮤니티 활발

### 왜 Vue 3?
- ✅ Composition API (로직 재사용)
- ✅ TypeScript 지원 우수
- ✅ 빠른 렌더링 (Virtual DOM 최적화)
- ✅ Pinia (직관적 상태 관리)

### 왜 Redis?
- ✅ 초고속 캐시 (실시간 시세)
- ✅ TTL 자동 만료
- ✅ Pub/Sub (실시간 알림 확장 가능)
- ✅ Spring 통합 우수

---

## 📊 성능 최적화 전략

### Backend
1. **N+1 문제 방지**: `@EntityGraph`, Fetch Join
2. **캐시 전략**: Redis L1, DB Query L2
3. **Connection Pool**: HikariCP (default)
4. **비동기 처리**: `@Async`, RabbitMQ

### Frontend
1. **코드 분할**: Vue Router Lazy Loading
2. **차트 최적화**: Downsampling, Virtual Scroll
3. **API 호출**: Debounce, Request Caching
4. **번들 최적화**: Tree Shaking, Minify

---

## 🔐 보안 체크리스트

- [x] JWT 토큰 HttpOnly Cookie (XSS 방어)
- [x] CSRF Token (POST/PUT/DELETE)
- [x] SQL Injection 방어 (PreparedStatement)
- [x] Rate Limiting (API 호출 제한)
- [x] HTTPS Only (TLS 1.3)
- [x] 비밀번호 암호화 (BCrypt)
- [ ] 2FA/OTP (Phase 2)
- [ ] API Key Rotation (Phase 2)

---

**문서 버전:** 1.0.0  
**작성일:** 2026-02-06  
**관련 문서:** [프로젝트 개요](01_PROJECT_OVERVIEW.md), [데이터베이스 설계](03_DATABASE_DESIGN.md)
