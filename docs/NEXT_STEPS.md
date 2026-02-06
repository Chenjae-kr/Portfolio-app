# 다음 개발 단계 및 개선 사항

> **작성일:** 2026-02-06  
> **현재 버전:** v0.0.1-SNAPSHOT  
> **전체 진척률:** 75%

---

## 📊 현재 완료 상태

### ✅ 완성된 기능 (75%)

```
✅ 인프라 설정      ████████████████████ 100%
✅ 사용자 인증      ████████████████████ 100%
✅ 포트폴리오 관리  ███████████████████░  95%
✅ 금융상품 관리    ████████████████░░░░  80%
✅ 거래 관리        ██████████████████░░  90%
✅ 평가 엔진        █████████████████░░░  85%
✅ 가격 데이터      ████████░░░░░░░░░░░░  40%
✅ 성과 분석        ████████████████░░░░  80%
✅ 비교 차트        ████████████████░░░░  80%
✅ 백테스팅         ████████████████░░░░  80%
⏸️ 리밸런싱         ░░░░░░░░░░░░░░░░░░░░   0%
```

**완성된 주요 기능:**
- ✅ JWT 기반 인증 시스템
- ✅ 포트폴리오 CRUD (생성, 조회, 수정, 삭제)
- ✅ 목표 비중 설정 (자동 검증/정규화)
- ✅ 금융상품 검색 및 필터링
- ✅ 포트폴리오 그룹 관리
- ✅ 다국어 지원 (한국어/영어)
- ✅ 테스트 환경 (60개 테스트)
- ✅ 거래 원장 (복식부기, 매수/매도/입금/출금/배당)
- ✅ 평가 엔진 (포지션/현금/시가평가/손익 계산)
- ✅ PriceService 아키텍처 (Mock 가격 + 히스토리컬)
- ✅ 성과 분석 (TWR, CAGR, Volatility, MDD, Sharpe)
- ✅ ECharts 수익률 차트 + 기간 선택기
- ✅ 포트폴리오 비교 분석 (Compare API + ECharts 비교 차트)
- ✅ 백테스트 엔진 (정적배분 + 리밸런싱 + ECharts 결과 차트)

---

## 🎯 다음 우선순위 (Sprint 별)

### Sprint 1: Transaction Ledger (거래 원장) ✅ 완료

**목표:** 거래 입력 및 포지션 계산 기반 구축

**✅ 완료된 항목:**
- [x] TransactionService (복식부기 밸런싱 검증, 생성/조회/취소)
- [x] TransactionController (REST API 4개 엔드포인트)
- [x] TransactionForm.vue (BUY/SELL/DEPOSIT/WITHDRAW/DIVIDEND)
- [x] TransactionList.vue (거래 목록, 상태 배지, 취소 기능)
- [x] PortfolioDetailView에 Transactions 탭 통합
- [x] i18n 번역 (한국어/영어)

**🚧 남은 항목:**
- [ ] 거래 필터링 (날짜, 유형별)
- [ ] CSV/Excel 임포트

---

### Sprint 2: Valuation Engine (평가 엔진) ✅ 완료

**목표:** 실시간 포트폴리오 평가액 계산

**✅ 완료된 항목:**
- [x] ValuationService (포지션/현금/시가평가/손익 계산)
- [x] PriceService 인터페이스 + MockPriceService (20+ 종목)
- [x] 평균단가, 미실현 손익, 실현 손익, 포지션 비중 계산
- [x] ValuationController (실제 데이터 반환)
- [x] 대시보드/포트폴리오 카드 실제 평가 데이터 표시
- [x] PositionTable 컴포넌트 실제 포지션 표시
- [x] 거래 후 평가 데이터 자동 갱신

**🚧 남은 항목:**
- [ ] 환율 변환 (multi-currency 평가)
- [ ] Redis 캐싱
- [ ] 외부 가격 API 연동

---

### Sprint 3: Price Data Integration (가격 데이터 연동) 🟡 부분 완료

**목표:** 실제 시세 데이터 연동

**✅ 완료된 항목:**
- [x] PriceService 인터페이스 (현재가/히스토리컬/환율 표준화)
- [x] MockPriceService (결정론적 히스토리컬 가격 시뮬레이션)
- [x] ValuationService 리팩토링 (PriceService 의존성 주입)
- [x] Mock 환율 데이터 (USD/KRW, EUR/KRW, JPY/KRW)

**🚧 남은 항목 (외부 API 연동 시 구현):**
- [ ] 외부 API 연동 (Alpha Vantage / Yahoo Finance / KRX)
- [ ] HTTP Client 설정 (WebClient + Resilience4j)
- [ ] Redis 캐싱 (실시간 시세 TTL: 60-180s)
- [ ] EOD 가격 수집 스케줄러
- [ ] PriceBar DB 저장
- [ ] FX Rate 수집

**예상 기간:** 2-3일 (외부 API 연동)  
**우선순위:** 🟡 High  
**의존성:** API 키 설정 필요

---

### Sprint 4: Performance Analytics (성과 분석) ✅ 완료

**목표:** 수익률 계산 및 차트

**✅ 완료된 항목:**
- [x] PerformanceService (TWR 수익률 계산, 현금흐름 조정)
- [x] 리스크 지표 (CAGR, Volatility, MDD, Sharpe Ratio)
- [x] Performance API 엔드포인트 (실제 데이터 연동)
- [x] ECharts 기반 누적 수익률 차트 (PerformanceChart.vue)
- [x] 기간 선택기 (1M/3M/6M/YTD/1Y/ALL)
- [x] 리스크 지표 카드 UI (5개 지표)
- [x] 수익/손실 색상 자동 변경 (초록/빨강)
- [x] i18n 번역 (한국어/영어)

**🚧 남은 항목:**
- [ ] MWR (Money-Weighted Return / IRR) 계산
- [ ] 벤치마크 비교 (KOSPI, S&P500)
- [ ] 월별 수익률 히트맵

---

### Sprint 5: Portfolio Comparison (포트폴리오 비교) ✅ 완료

**목표:** 다중 포트폴리오 성과 비교 분석

**✅ 완료된 항목:**
- [x] CompareController (POST /v1/compare/performance)
- [x] 다중 포트폴리오 성과 동시 조회 (2~5개)
- [x] CompareView.vue (ECharts 누적 수익률 비교 차트)
- [x] 성과 지표 비교 테이블 (총수익률/CAGR/Volatility/MDD/Sharpe)
- [x] 포트폴리오 선택 UI (체크박스, 최대 5개)
- [x] 기간 설정 + 수익률 메트릭 선택
- [x] 차트 색상 팔레트 (5색 구분)
- [x] CompareResponse/CompareCurve/CompareStatRow 타입
- [x] i18n 번역 (한국어/영어 12개 키)

**🚧 남은 항목:**
- [ ] 벤치마크 오버레이 (KOSPI, S&P500)
- [ ] 비교 결과 PDF 내보내기

---

### Sprint 6: Backtesting Engine (백테스트) ✅ 완료

**목표:** 전략 백테스트 기본 구현

**✅ 완료된 항목:**
- [x] BacktestService (정적 배분 + 리밸런싱 + 거래비용 0.1% 엔진)
- [x] BacktestController (REST API 7개 엔드포인트)
- [x] 주기적 리밸런싱 (Monthly/Quarterly/Semi-Annual/Annual)
- [x] 밴드 리밸런싱 (목표 비중 ± threshold 이탈 시)
- [x] In-memory Config/Run/Result 저장소
- [x] 일별 Equity Curve, Drawdown, Trade Log 생성
- [x] 성과 통계 (CAGR, Volatility, MDD, Sharpe Ratio)
- [x] BacktestView.vue (종목 프리셋 10개, 비중 설정, 리밸런싱 전략)
- [x] BacktestResultView.vue (ECharts Equity Curve + Drawdown 차트)
- [x] Trade Log 테이블 (50건 표시)
- [x] i18n 번역 (한국어/영어 30+ 키)

**🚧 남은 항목:**
- [ ] RabbitMQ 비동기 처리 (대규모 백테스트)
- [ ] Backtest 이력 목록 UI
- [ ] 배당 재투자 시뮬레이션
- [ ] DB 영속화 (현재 in-memory)

---

## 💡 추가 개발하면 좋을 점

### UX 개선

**1. 대시보드 고도화**
- [ ] 전체 자산 합산 표시 (모든 포트폴리오)
- [ ] 리밸런싱 필요 알림 카드
- [ ] 최근 거래 요약
- [ ] 이번 달 배당 캘린더
- [ ] 성과 TOP3 / WORST3 포트폴리오

**2. 포트폴리오 상세 개선**
- [ ] 자산 배분 도넛 차트 (자산 클래스별)
- [ ] 국가/섹터별 분류
- [ ] 드래그 앤 드롭 정렬
- [ ] 종목별 상세 정보 모달

**3. 검색 및 필터 강화**
- [ ] 전역 검색 (포트폴리오, 종목, 거래)
- [ ] 고급 필터 (날짜, 금액, 자산군)
- [ ] 저장된 필터 프리셋

**4. 모바일 반응형**
- [ ] 모바일 최적화 레이아웃
- [ ] 터치 제스처 지원
- [ ] 하단 네비게이션 바

### 기능 추가

**1. 거래 관련**
- [ ] 거래 템플릿 (자주 하는 거래 저장)
- [ ] CSV/Excel 임포트
- [ ] 증권사 계좌 연동 (추후)
- [ ] 거래 승인 워크플로우

**2. 알림 시스템**
- [ ] 목표 비중 이탈 알림 (±5%)
- [ ] 평가액 급변 알림 (-3%, -5%)
- [ ] 배당 지급 예정 알림
- [ ] 리밸런싱 추천 알림

**3. 리포트 기능**
- [ ] 월간/분기/연간 리포트 생성
- [ ] PDF 내보내기
- [ ] 이메일 발송
- [ ] 성과 대시보드

**4. 고급 분석**
- [ ] 상관 관계 분석 (포트폴리오 간)
- [ ] 섹터/국가 노출도 분석
- [ ] 효율적 프론티어 (Efficient Frontier)
- [ ] 몬테카를로 시뮬레이션

**5. 리밸런싱 도구**
- [ ] 현재 vs 목표 비중 비교 시각화
- [ ] 매매 추천 (최소 거래)
- [ ] 현금 제약 조건 반영
- [ ] 세금 최적화 (손익 실현 시뮬레이션)

---

## 🔧 기술 부채 및 개선 사항

### 코드 품질

**1. 테스트 안정화** 🔴 High
- [ ] Backend 테스트 Context 로딩 이슈 해결
- [ ] Frontend Mock 설정 완성
- [ ] 테스트 커버리지 80% 목표
- [ ] E2E 테스트 추가 (Playwright)

**2. 에러 처리 표준화** 🟡 Medium
- [ ] 전역 예외 처리 개선
- [ ] 사용자 친화적 에러 메시지
- [ ] 에러 로깅 및 추적 (Sentry)
- [ ] Retry 로직 표준화

**3. 코드 리팩토링** 🟢 Low
- [ ] Controller에서 Service로 비즈니스 로직 이동
- [ ] DTO 클래스 분리
- [ ] Validator 클래스 추가
- [ ] 공통 유틸리티 정리

### 성능 최적화

**1. Database** 🟡 Medium
- [ ] 인덱스 최적화
  - `transactions(portfolio_id, occurred_at)`
  - `price_bars(instrument_id, ts)`
  - `portfolio_targets(portfolio_id)`
- [ ] N+1 쿼리 문제 확인 및 해결
- [ ] Connection Pool 튜닝 (HikariCP)
- [ ] 시계열 테이블 파티셔닝 (price_bars)

**2. Caching** 🟡 Medium
- [ ] Redis 캐싱 전략 구체화
  - 포트폴리오 목록 캐시
  - 종목 검색 결과 캐시
  - 평가액 캐시 (TTL: 30s)
- [ ] Cache Invalidation 전략
- [ ] Cache Warming (앱 시작 시)

**3. Frontend** 🟢 Low
- [ ] 코드 스플리팅 (Route-based)
- [ ] 이미지 최적화
- [ ] Virtual Scrolling (거래 내역)
- [ ] Lazy Loading (차트 라이브러리)

### 보안 강화

**1. 인증/인가** 🟡 Medium
- [ ] Refresh Token 갱신 로직 구현
- [ ] 토큰 만료 자동 처리
- [ ] RBAC (Role-Based Access Control)
- [ ] API Rate Limiting

**2. 데이터 보호** 🟢 Low
- [ ] 민감 정보 암호화 (KMS)
- [ ] SQL Injection 방어 확인
- [ ] XSS 방어 확인
- [ ] CSRF Token 구현

---

## 🚀 기능별 상세 계획

### 📝 Transaction Ledger (최우선)

**구현 세부사항:**

```java
// Transaction 생성 로직
@Transactional
public Transaction createTransaction(String portfolioId, TransactionRequest request) {
    // 1. Portfolio 검증
    // 2. Legs 검증 (합계 = 0)
    // 3. BUY/SELL은 ASSET leg 필수
    // 4. Transaction 저장
    // 5. Position 재계산 트리거
    // 6. Valuation 캐시 무효화
}
```

**검증 규칙:**
- ✅ Legs 합계 = 0 (통화별 또는 base 환산)
- ✅ BUY/SELL은 정확히 1개의 ASSET leg 필요
- ✅ 수수료/세금은 별도 leg
- ✅ 수량/금액 음수 체크

**UI Flow:**
```
1. 거래 유형 선택 (매수/매도/입금/배당)
2. 종목 선택 (검색)
3. 수량/가격 입력
4. 수수료/세금 자동 계산 (옵션)
5. 미리보기
6. 저장
```

**테스트 케이스:**
- [ ] 매수 거래 생성
- [ ] 매도 거래 (부분/전체)
- [ ] 배당 수령
- [ ] 입출금
- [ ] 환전 (FX_CONVERT)
- [ ] Legs 검증 실패
- [ ] 거래 취소 (VOID)

---

### 📊 Valuation Engine

**계산 로직:**

```typescript
// 포지션별 평가
position.marketValue = position.quantity × latestPrice
position.marketValueBase = position.marketValue × fxRate

// 손익 계산
position.unrealizedPnL = (latestPrice - avgCost) × quantity
position.realizedPnL = // 거래 이력 기반 계산

// 포트폴리오 합계
portfolio.totalValue = Σ(positions.marketValueBase) + cashBase
portfolio.totalPnL = Σ(unrealizedPnL + realizedPnL)
```

**가격 소스 우선순위:**
1. Redis 캐시 (실시간)
2. DB 최신 PriceBar (EOD)
3. 마지막 거래 가격 (fallback)

**UI 표시:**
- 평가액 (큰 글씨)
- 오늘 손익 (색상 표시)
- 전체 손익 (%, 금액)
- 포지션별 상세

---

### 📈 Performance Analytics

**구현 순서:**

1. **TWR 계산**
   ```
   현금흐름 시점으로 구간 분할
   각 구간 수익률 계산
   구간 수익률 곱하기
   ```

2. **리스크 지표**
   ```
   Volatility = std(returns) × √252
   MDD = max(peak - trough) / peak
   Sharpe = (return - rf) / volatility
   ```

3. **차트 구현 (ECharts)**
   - 누적 수익률 라인 차트
   - Drawdown 차트
   - 월별 수익률 히트맵

**필요한 데이터:**
- 일별 포트폴리오 가치 시계열
- 현금흐름 이벤트
- 무위험 수익률 (설정값)

---

## 🎨 UX/UI 개선 아이디어

### Dashboard 개선

**1. 요약 카드 추가**
```
┌─────────────┬─────────────┬─────────────┐
│ 총 자산      │ 오늘 손익    │ 전체 수익률  │
│ ₩50,000,000 │ +₩125,000   │ +12.5%      │
└─────────────┴─────────────┴─────────────┘

┌────────────────────────────────────────┐
│ 🎯 리밸런싱 필요                        │
│ "성장 포트폴리오" 비중 이탈 +5.2%       │
│ [시뮬레이션 보기]                       │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│ 📅 이번 달 배당 예정                    │
│ AAPL: ₩15,000 (2/15)                  │
│ VOO: ₩8,500 (2/22)                    │
└────────────────────────────────────────┘
```

**2. 포트폴리오 카드 개선**
- 미니 차트 (최근 30일)
- 비중 프로그레스 바
- 빠른 액션 (거래 추가, 리밸런싱)

### Portfolio Detail 개선

**1. Positions 탭**
```
[도넛 차트] ← 자산 클래스별 / 종목별 토글

┌──────────────────────────────────────────────┐
│ 종목      비중    평가액      손익       변동  │
├──────────────────────────────────────────────┤
│ 🇺🇸 AAPL  35%   ₩17,500,000  +15.2%   +2.1% │
│ 🇺🇸 VOO   25%   ₩12,500,000   +8.5%   +1.3% │
│ 🇰🇷 005930 20%  ₩10,000,000   +5.0%   -0.5% │
│ 💰 Cash   20%   ₩10,000,000     -       -   │
└──────────────────────────────────────────────┘

[정렬: 비중 ▼] [필터: 전체 ▼]
```

**2. Performance 탭**
- 기간별 수익률 차트
- 벤치마크 비교 (KOSPI, S&P500)
- 월별 수익률 테이블

**3. Transactions 탭**
- 타임라인 뷰
- 거래 유형별 필터
- CSV 내보내기

---

## 🛠️ 기술 개선

### 1. API 문서화

**Swagger UI 활성화**
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

**문서화 개선:**
- [ ] 모든 엔드포인트에 @Operation 추가
- [ ] 예제 Request/Response
- [ ] 에러 코드 문서화

### 2. 로깅 전략

**구조화 로깅 (JSON)**
```json
{
  "timestamp": "2026-02-06T10:30:00Z",
  "level": "INFO",
  "service": "portfolio-api",
  "traceId": "abc123",
  "userId": "user-1",
  "action": "CREATE_TRANSACTION",
  "portfolioId": "p-123",
  "duration": 45
}
```

**로깅 포인트:**
- [ ] API 요청/응답 (duration)
- [ ] 거래 생성/취소
- [ ] 평가 계산
- [ ] 외부 API 호출
- [ ] 에러 및 예외

### 3. 모니터링

**Actuator 엔드포인트 활성화**
- [ ] `/actuator/health` - 헬스 체크
- [ ] `/actuator/metrics` - 메트릭
- [ ] `/actuator/prometheus` - Prometheus 연동

**주요 메트릭:**
- API 응답 시간 (p50, p95, p99)
- 거래 생성 수
- 평가 계산 빈도
- 캐시 히트율
- 에러율

### 4. CI/CD

**GitHub Actions 파이프라인**
```yaml
name: CI/CD
on: [push, pull_request]

jobs:
  backend-test:
    - Setup Java 21
    - Run tests
    - Code coverage (Jacoco)
    
  frontend-test:
    - Setup Node 20
    - Run tests
    - Coverage report
    
  build:
    - Build Docker image
    - Push to registry
    
  deploy:
    - Deploy to staging (auto)
    - Deploy to prod (manual)
```

---

## 📚 개발 프로세스 개선

### 1. Git Workflow

**브랜치 전략:**
```
main (protected)
  ├─ develop
  │   ├─ feature/transaction-ledger
  │   ├─ feature/valuation-engine
  │   └─ feature/price-integration
  └─ hotfix/critical-bug
```

**Commit Convention:**
```
feat: 새로운 기능
fix: 버그 수정
docs: 문서 업데이트
test: 테스트 추가
refactor: 코드 리팩토링
perf: 성능 개선
style: 코드 포맷팅
chore: 빌드/설정 변경
```

### 2. Code Review

**리뷰 체크리스트:**
- [ ] 비즈니스 로직 정확성
- [ ] 테스트 커버리지
- [ ] 에러 처리
- [ ] 성능 고려사항
- [ ] 보안 이슈
- [ ] 코드 스타일

### 3. 문서화

**자동 업데이트 대상:**
- [x] PROGRESS.md (진척도)
- [ ] API.md (엔드포인트 목록)
- [ ] CHANGELOG.md (변경 이력)
- [ ] 이 문서 (NEXT_STEPS.md)

---

## 🎯 단기 목표 (1-2주)

### Week 1 ✅ 완료
- [x] ~~포트폴리오 관리 완성~~ ✅
- [x] ~~Transaction Ledger 구현~~ ✅
- [x] ~~Valuation Engine 기본 구현~~ ✅
- [ ] 테스트 안정화

### Week 2 (현재)
- [x] ~~Price Data 연동 (Mock)~~ ✅
- [x] ~~Performance 차트~~ ✅
- [ ] 대시보드 고도화
- [ ] 문서화 완성
- [ ] 비교 분석 / 백테스트 시작

---

## 🎓 학습 및 도입 검토

### 새로운 기술 도입

**1. jOOQ (복잡한 쿼리)**
- 성과 분석 쿼리
- 집계 및 리포트
- Type-safe SQL

**2. WebSocket (실시간 업데이트)**
- 가격 실시간 Push
- 포트폴리오 평가 업데이트
- 알림 실시간 전송

**3. GraphQL (고려 사항)**
- 프론트엔드 유연한 쿼리
- N+1 문제 해결
- 개발 생산성

**4. Redis Pub/Sub**
- 포트폴리오 갱신 이벤트
- 다중 클라이언트 동기화

---

## 📊 성능 목표

### 응답 시간

| API | 목표 | 현재 | 개선 필요 |
|-----|------|------|----------|
| 포트폴리오 목록 | < 200ms | - | 측정 필요 |
| 포트폴리오 상세 | < 300ms | - | 측정 필요 |
| 평가액 조회 | < 500ms | - | 구현 필요 |
| 거래 생성 | < 1s | - | 구현 필요 |
| 백테스트 실행 | < 30s | - | 구현 필요 |

### 동시 사용자

- 목표: 100명 동시 접속
- 평가 요청: 초당 500회
- 캐시 활용으로 DB 부하 최소화

---

## 🐛 알려진 이슈 및 해결 계획

### 1. Backend 테스트 Context 로딩 실패 🔴

**원인:**
- Spring Security 설정 관련 Bean 의존성 이슈
- H2 환경에서 Auto-configuration 충돌

**해결 방안:**
- [ ] @SpringBootTest(webEnvironment = MOCK) 사용
- [ ] Test-specific Security Config
- [ ] @MockBean으로 외부 의존성 제거

### 2. Frontend 테스트 Mock 불안정 🟡

**원인:**
- API mock 설정 미흡
- Store 의존성 모킹 부족

**해결 방안:**
- [ ] MSW (Mock Service Worker) 도입
- [ ] Test Utils 개선
- [ ] Component 격리 테스트

### 3. Workspace 외래 키 ✅ 해결됨

**해결 방법:**
- ✅ 회원가입 시 자동 Workspace 생성

---

## 💼 비즈니스 로직 개선

### 1. 거래 원가 계산

**현재 계획:** 평균 단가  
**향후 확장:**
- [ ] FIFO (First-In-First-Out)
- [ ] LIFO (Last-In-First-Out)
- [ ] 특정 로트 지정

### 2. 배당 처리

**구현 계획:**
- [ ] 배당 이벤트 자동 기록
- [ ] 배당 캘린더
- [ ] 재투자 옵션 (DRIP)
- [ ] 원천징수 처리

### 3. 리밸런싱 알고리즘

**Phase 1:**
- 현재 vs 목표 비중 계산
- 매매 추천 (간단)

**Phase 2:**
- 최소 거래 최적화
- 거래 비용 고려
- 세금 최적화

---

## 🔄 반복 개선 사항

### 매 Sprint 마다

**개발 완료 시:**
- [x] PROGRESS.md 업데이트 ✅
- [x] NEXT_STEPS.md 업데이트 (이 문서)
- [ ] CHANGELOG.md 작성
- [ ] API 문서 갱신

**코드 리뷰 시:**
- [ ] 테스트 커버리지 확인
- [ ] 성능 프로파일링
- [ ] 보안 체크리스트
- [ ] 코드 품질 점검

**배포 전:**
- [ ] 통합 테스트 실행
- [ ] 마이그레이션 스크립트 확인
- [ ] 롤백 계획 수립
- [ ] 릴리스 노트 작성

---

## 📞 의사 결정 필요 사항

### 1. 가격 데이터 소스 선택

**옵션:**
- Alpha Vantage (무료: 5 req/min, 500 req/day)
- Yahoo Finance (비공식 API)
- KRX API (국내 주식)
- IEX Cloud (유료, 안정적)

**결정 필요:**
- 예산
- 데이터 품질 요구사항
- 실시간 vs 지연 데이터

### 2. 배포 전략

**옵션:**
- VM (단순, 저비용)
- Docker Compose (개발/소규모)
- Kubernetes (확장성)
- Serverless (트래픽 변동 큰 경우)

### 3. 프론트엔드 호스팅

**옵션:**
- Vercel (무료, CDN)
- Netlify (무료, CDN)
- S3 + CloudFront
- 자체 Nginx

---

## 📅 릴리스 계획

### v0.1.0 (MVP) - 목표: 2주 후

**포함 기능:**
- ✅ 인증 시스템
- ✅ 포트폴리오 CRUD
- ✅ 목표 비중 설정
- ✅ 거래 입력 (복식부기)
- ✅ 평가액 계산 (포지션/현금/손익)
- ✅ 성과 분석 차트 (ECharts)

### v0.2.0 - 목표: 4주 후

**포함 기능:**
- 실시간 가격 데이터
- 성과 분석 차트
- 포트폴리오 비교
- 알림 기본

### v0.3.0 - 목표: 6주 후

**포함 기능:**
- 백테스트 엔진
- 리밸런싱 도구
- 고급 분석
- 모바일 최적화

### v1.0.0 (Production Ready) - 목표: 8주 후

**요구사항:**
- 모든 MVP 기능 완성
- 테스트 커버리지 > 80%
- 성능 목표 달성
- 보안 감사 통과
- 문서화 완료

---

## 📝 체크리스트 템플릿

### 새 기능 개발 시

- [ ] 기능 요구사항 정리
- [ ] DB 스키마 설계/마이그레이션
- [ ] Backend Service 구현
- [ ] Backend API Controller
- [ ] Backend 테스트 작성 (단위 + 통합)
- [ ] Frontend API 클라이언트
- [ ] Frontend 컴포넌트/페이지
- [ ] Frontend 테스트 작성
- [ ] 다국어 번역 추가
- [ ] PROGRESS.md 업데이트
- [ ] NEXT_STEPS.md 업데이트
- [ ] Git Commit & Push

### 버그 수정 시

- [ ] 이슈 재현
- [ ] 테스트 케이스 작성 (실패 확인)
- [ ] 코드 수정
- [ ] 테스트 통과 확인
- [ ] 회귀 테스트
- [ ] Commit & Push

---

## 🎯 성공 지표

### 개발 지표

- 주간 커밋 수: 15-20개
- 테스트 커버리지: 현재 ~30% → 목표 80%
- API 응답 시간: < 500ms
- 빌드 시간: < 3분

### 코드 품질

- SonarQube Grade: A
- 복잡도: < 10 (함수당)
- 중복 코드: < 3%
- 기술 부채: < 5%

### 사용성

- 페이지 로딩: < 2초
- 첫 화면 렌더링: < 1초
- 에러율: < 1%
- 사용자 만족도: 4/5 이상

---

**문서 버전:** 1.0.0  
**작성일:** 2026-02-06  
**다음 업데이트:** 다음 Sprint 완료 시  

**관련 문서:**
- [개발 진척도](PROGRESS.md)
- [프로젝트 개요](01_PROJECT_OVERVIEW.md)
- [기술 스택](02_TECH_STACK.md)

---

<div align="center">

**[⬆️ 맨 위로](#다음-개발-단계-및-개선-사항)**

</div>
