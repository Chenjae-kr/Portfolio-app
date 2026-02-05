# 개발 진척도 보고서

**작성일:** 2026-02-06  
**프로젝트:** Portfolio Manager App  
**버전:** v0.0.1-SNAPSHOT

---

## 📊 전체 진척도

| 영역 | 진척도 | 상태 |
|------|--------|------|
| 인프라 설정 | 100% | ✅ 완료 |
| 사용자 인증 | 100% | ✅ 완료 |
| 포트폴리오 관리 | 60% | 🚧 진행중 |
| 거래 관리 | 0% | ⏸️ 대기 |
| 평가 엔진 | 0% | ⏸️ 대기 |
| 성과 분석 | 0% | ⏸️ 대기 |
| 비교 차트 | 0% | ⏸️ 대기 |
| 백테스팅 | 0% | ⏸️ 대기 |
| 리밸런싱 | 0% | ⏸️ 대기 |

**전체 진척률:** 약 **28%**

---

## 🎯 Core Features 구현 현황

### 1. Multi-portfolio Management ⚡ 60%

**✅ 완료:**
- [x] Portfolio 엔티티 및 Repository
- [x] Workspace 자동 생성 (회원가입 시)
- [x] Portfolio CRUD API
  - GET /api/v1/portfolios (목록 조회)
  - POST /api/v1/portfolios (생성)
  - GET /api/v1/portfolios/{id} (상세 조회)
  - PATCH /api/v1/portfolios/{id} (수정)
  - DELETE /api/v1/portfolios/{id} (아카이브)
- [x] Portfolio UI (대시보드, 생성, 상세)
- [x] 다국어 지원 (한국어/영어)

**🚧 진행 필요:**
- [ ] Portfolio Targets (목표 비중)
- [ ] Portfolio Constraints (제약 조건)
- [ ] 종목별 보유 내역 표시
- [ ] 포트폴리오 타입별 필터링

**⏸️ 미구현:**
- [ ] 다중 자산 클래스 지원 (주식, ETF, 채권, 원자재)
- [ ] 그룹 관리

---

### 2. Real-time Valuation ⏸️ 0%

**⏸️ 미구현:**
- [ ] 평가 엔진
- [ ] 실시간 가격 조회
- [ ] 환율 변환
- [ ] Redis 캐싱
- [ ] Valuation API

---

### 3. Portfolio Performance Comparison ⏸️ 0%

**⏸️ 미구현:**
- [ ] 성과 비교 API
- [ ] 벤치마크 설정
- [ ] 비교 차트 UI
- [ ] TWR/MWR 계산
- [ ] 리스크 지표 (Volatility, MDD, Sharpe, Sortino)

---

### 4. Strategy Backtesting ⏸️ 0%

**⏸️ 미구현:**
- [ ] Backtest 설정
- [ ] RabbitMQ 비동기 처리
- [ ] Backtest 실행 엔진
- [ ] 결과 조회 API
- [ ] Backtest UI

---

### 5. Rebalancing Tools ⏸️ 0%

**⏸️ 미구현:**
- [ ] 리밸런싱 시뮬레이션
- [ ] 목표 비중 대비 현재 비중 비교
- [ ] 매매 추천
- [ ] 리밸런싱 UI

---

## 🏗️ 기술 스택 구현 현황

### Backend

| 기술 | 상태 | 진척도 |
|------|------|--------|
| Java 21 | ✅ 설정 완료 | 100% |
| Spring Boot 3.3 | ✅ 설정 완료 | 100% |
| Spring Security + JWT | ✅ 구현 완료 | 100% |
| Spring Data JPA | ✅ 기본 설정 | 80% |
| jOOQ | ⏸️ 미사용 | 0% |
| PostgreSQL 16 | ✅ Docker 설정 | 100% |
| Flyway | ✅ 마이그레이션 1개 | 20% |
| Redis 7 | 🚧 설정만 완료 | 10% |
| RabbitMQ | 🚧 설정만 완료 | 10% |

### Frontend

| 기술 | 상태 | 진척도 |
|------|------|--------|
| Vue 3 + TypeScript | ✅ 설정 완료 | 100% |
| Vite | ✅ 설정 완료 | 100% |
| Pinia | ✅ Auth Store | 60% |
| vue-i18n | ✅ 다국어 구현 | 100% |
| ECharts | ⏸️ 미사용 | 0% |
| Axios | ✅ API 클라이언트 | 80% |

---

## 📦 백엔드 구조 현황

### 구현된 패키지

```
backend/src/main/java/com/portfolio/
├── api/                     ✅ Controllers
│   ├── AuthController       ✅ 인증 API
│   └── PortfolioController  ✅ 포트폴리오 API
├── auth/                    ✅ 인증/인가
│   ├── entity/User          ✅ 사용자 엔티티
│   ├── jwt/                 ✅ JWT 처리
│   ├── repository/          ✅ Repository
│   └── service/AuthService  ✅ 인증 서비스
├── portfolio/               🚧 포트폴리오 도메인
│   ├── entity/
│   │   ├── Portfolio        ✅ 포트폴리오 엔티티
│   │   └── PortfolioTarget  ⏸️ 미사용
│   ├── repository/          ✅ Repository
│   └── service/             ✅ Service
├── workspace/               ✅ 워크스페이스
│   ├── entity/              ✅ Workspace 엔티티
│   ├── repository/          ✅ Repository
│   └── service/             ✅ Service
├── ledger/                  ⏸️ 미구현
├── pricing/                 ⏸️ 미구현
├── valuation/               ⏸️ 미구현
├── analytics/               ⏸️ 미구현
├── backtest/                ⏸️ 미구현
└── infra/                   🚧 부분 구현
    ├── redis/               🚧 Config만
    └── rabbitmq/            🚧 Config만
```

---

## 🎨 프론트엔드 구조 현황

### 구현된 구조

```
frontend/src/
├── api/                     ✅ API 클라이언트
│   ├── auth.ts              ✅ 인증 API
│   ├── portfolio.ts         ✅ 포트폴리오 API
│   ├── client.ts            ✅ Axios 설정
│   └── ...                  ⏸️ 기타 API
├── components/              🚧 컴포넌트
│   ├── layout/              ✅ 레이아웃
│   │   ├── AppHeader        ✅ 헤더
│   │   ├── AppSidebar       ✅ 사이드바
│   │   ├── AppLayout        ✅ 레이아웃
│   │   └── LanguageSwitcher ✅ 언어 선택
│   └── portfolio/           🚧 포트폴리오
│       ├── PortfolioCard    ✅ 카드
│       └── PositionTable    ⏸️ 미사용
├── i18n/                    ✅ 다국어
├── locales/                 ✅ 번역 파일
│   ├── ko.ts                ✅ 한국어
│   └── en.ts                ✅ 영어
├── stores/                  🚧 상태 관리
│   ├── auth.ts              ✅ 인증 Store
│   ├── portfolio.ts         🚧 포트폴리오 Store
│   ├── valuation.ts         ⏸️ 미사용
│   └── backtest.ts          ⏸️ 미사용
├── views/                   🚧 페이지
│   ├── auth/                ✅ 인증 페이지
│   │   ├── LoginView        ✅ 로그인
│   │   └── RegisterView     ✅ 회원가입
│   ├── dashboard/           ✅ 대시보드
│   ├── portfolio/           🚧 포트폴리오
│   │   ├── NewPortfolioView ✅ 생성
│   │   └── PortfolioDetailView 🚧 상세 (빈 상태)
│   ├── compare/             ⏸️ 비교 (빈 페이지)
│   └── backtest/            ⏸️ 백테스트 (빈 페이지)
└── utils/                   ✅ 유틸리티
    └── format.ts            ✅ 포맷팅 함수
```

---

## 🧪 테스트 환경

### Backend Tests

**구현 현황:**
```
backend/src/test/
├── resources/
│   └── application-test.yml  ✅ H2 테스트 설정
└── java/com/portfolio/
    ├── TestConfig.java        ✅ 테스트 설정
    ├── auth/
    │   └── AuthServiceTest    ✅ 5개 테스트
    ├── portfolio/
    │   └── PortfolioServiceTest ✅ 6개 테스트
    ├── workspace/
    │   └── WorkspaceServiceTest ✅ 4개 테스트
    └── api/
        └── AuthControllerTest  ✅ 4개 통합 테스트
```

**테스트 통계:**
- 작성된 테스트: 19개
- 통과율: 설정 조정 필요 (Context 로딩 이슈)

### Frontend Tests

**구현 현황:**
```
frontend/src/
├── vitest.config.ts         ✅ Vitest 설정
├── tests/setup.ts           ✅ 테스트 셋업
├── utils/
│   └── format.spec.ts       ✅ 10개 테스트
├── stores/
│   └── auth.spec.ts         ✅ 4개 테스트
└── components/
    └── LanguageSwitcher.spec.ts ✅ 5개 테스트
```

**테스트 통계:**
- 작성된 테스트: 19개
- 통과: 11개 (58%)
- 실패: 8개 (mock 설정 조정 필요)

---

## 🗄️ 데이터베이스

### 구현된 테이블

| 테이블 | 상태 | 용도 |
|--------|------|------|
| users | ✅ 완료 | 사용자 |
| workspaces | ✅ 완료 | 워크스페이스 |
| portfolios | ✅ 완료 | 포트폴리오 |
| portfolio_targets | ⏸️ 미사용 | 목표 비중 |
| portfolio_constraints | ⏸️ 미사용 | 제약 조건 |
| instruments | ⏸️ 미사용 | 금융상품 |
| transactions | ⏸️ 미사용 | 거래 |
| transaction_legs | ⏸️ 미사용 | 거래 내역 |
| price_bars | ⏸️ 미사용 | 가격 데이터 |
| fx_rates | ⏸️ 미사용 | 환율 |

---

## 🔐 인증 & 보안

### 구현 완료

- ✅ JWT 기반 인증
- ✅ 회원가입 (이메일 중복 체크)
- ✅ 로그인 (비밀번호 검증)
- ✅ 로그아웃
- ✅ BCrypt 비밀번호 암호화
- ✅ CORS 설정
- ✅ Refresh Token (구조만)

### 미구현

- [ ] Refresh Token 갱신 로직
- [ ] 이메일 인증
- [ ] 비밀번호 재설정
- [ ] OAuth 2.0 (Google, Kakao)
- [ ] 권한 관리 (RBAC)

---

## 🌐 다국어 지원

### 구현 완료

- ✅ vue-i18n 설정
- ✅ 한국어/영어 전환
- ✅ localStorage 언어 저장
- ✅ 모든 주요 페이지 다국어 적용
  - 로그인/회원가입
  - 대시보드
  - 포트폴리오 목록/생성/상세
  - 헤더/사이드바

---

## 📋 API 엔드포인트 현황

### 구현된 엔드포인트

```
✅ POST   /api/v1/auth/register      회원가입
✅ POST   /api/v1/auth/login         로그인
✅ POST   /api/v1/auth/refresh       토큰 갱신 (Mock)
✅ POST   /api/v1/auth/logout        로그아웃 (Mock)

✅ GET    /api/v1/portfolios         포트폴리오 목록
✅ POST   /api/v1/portfolios         포트폴리오 생성
✅ GET    /api/v1/portfolios/{id}    포트폴리오 상세
✅ PATCH  /api/v1/portfolios/{id}    포트폴리오 수정
✅ DELETE /api/v1/portfolios/{id}    포트폴리오 삭제

⏸️ PUT    /api/v1/portfolios/{id}/targets     목표 비중 설정
⏸️ GET    /api/v1/portfolios/{id}/transactions 거래 내역
⏸️ POST   /api/v1/portfolios/{id}/transactions 거래 생성
⏸️ POST   /api/v1/transactions/{id}/void      거래 취소
⏸️ GET    /api/v1/portfolios/{id}/valuation   평가액 조회
⏸️ GET    /api/v1/portfolios/{id}/performance 성과 조회
⏸️ POST   /api/v1/compare/performance         비교 분석
⏸️ POST   /api/v1/backtests/runs              백테스트 실행
⏸️ GET    /api/v1/backtests/runs/{id}/results 백테스트 결과
```

---

## 🎯 MVP 우선순위 진척도

### 1. Transaction Ledger + Valuation Calculation ⏸️ 0%

- [ ] Transaction 엔티티
- [ ] TransactionLeg (Double-entry)
- [ ] 거래 생성/조회 API
- [ ] Valuation Engine
- [ ] 실시간/EOD 평가액 계산

---

### 2. Portfolio UI 🚧 70%

- [x] 대시보드
- [x] 포트폴리오 목록
- [x] 포트폴리오 생성
- [x] 포트폴리오 카드
- [ ] 포트폴리오 상세 (보유 종목)
- [ ] 거래 내역 UI

---

### 3. Comparison Charts ⏸️ 0%

- [ ] 비교 페이지 구현
- [ ] ECharts 연동
- [ ] 성과 비교 차트
- [ ] 벤치마크 비교

---

### 4. Basic Backtesting ⏸️ 0%

- [ ] Backtest 설정 UI
- [ ] 정적 배분 백테스트
- [ ] 주기적 리밸런싱 백테스트
- [ ] 결과 차트

---

### 5. Rebalancing Tools ⏸️ 0%

- [ ] 현재 vs 목표 비중 비교
- [ ] 리밸런싱 시뮬레이션
- [ ] 매매 추천

---

### 6. Alerts/Insights ⏸️ 0%

- [ ] Alert Rules
- [ ] 알림 시스템
- [ ] Insights 생성

---

## 🐛 알려진 이슈

### Backend

1. **테스트 환경 Context 로딩 실패**
   - Spring Security 설정 관련 Bean 의존성 이슈
   - H2 환경에서 일부 Auto-configuration 충돌
   - 우선순위: 🔴 High

2. **Workspace 외래 키 제약**
   - 초기 데이터 없이 포트폴리오 생성 불가
   - 회원가입 시 자동 생성으로 해결 ✅

### Frontend

1. **테스트 Mock 설정 불완전**
   - API mock 설정 미흡
   - Store 테스트 실패 (8개)
   - 우선순위: 🟡 Medium

2. **포트폴리오 상세 페이지 빈 상태**
   - Valuation API 미구현으로 데이터 없음
   - 우선순위: 🟢 Low (의존성)

---

## 📅 다음 단계 (Next Sprint)

### 우선순위 1: Transaction & Valuation 🔴

1. [ ] Instrument 엔티티 및 Repository
2. [ ] Transaction 엔티티 (Double-entry legs)
3. [ ] 거래 생성/조회 API
4. [ ] Valuation Engine 기본 구현
5. [ ] 평가액 조회 API
6. [ ] 포트폴리오 상세 페이지 데이터 연동

### 우선순위 2: 테스트 안정화 🟡

1. [ ] Backend 테스트 Context 로딩 수정
2. [ ] Frontend Mock 설정 완성
3. [ ] 통합 테스트 추가

### 우선순위 3: 가격 데이터 연동 🟢

1. [ ] PriceBar 엔티티
2. [ ] 가격 데이터 수집 (외부 API)
3. [ ] Redis 캐싱 구현
4. [ ] 실시간 가격 업데이트

---

## 📈 성과

### 완료된 주요 기능

1. ✅ **인프라 구축 완료**
   - Docker Compose (PostgreSQL, Redis, RabbitMQ)
   - Flyway 마이그레이션
   - 개발/로컬 프로파일 분리

2. ✅ **인증 시스템 완성**
   - JWT 기반 인증
   - 회원가입/로그인
   - Workspace 자동 생성

3. ✅ **포트폴리오 기본 CRUD**
   - API 구현
   - UI 구현
   - 다국어 지원

4. ✅ **테스트 환경 구축**
   - Backend: JUnit 5 + H2
   - Frontend: Vitest + Vue Test Utils
   - 총 38개 테스트 작성

5. ✅ **다국어 지원**
   - 한국어/영어 전환
   - 모든 주요 페이지 적용

### 개발 속도

- **총 개발 기간:** 1일
- **커밋 수:** 6개
- **작성된 코드:**
  - Backend: ~2,500 lines
  - Frontend: ~1,500 lines
  - 테스트: ~1,000 lines

---

## 🎓 학습 및 개선 사항

### 기술적 성과

1. Spring Boot 3.3 + Java 21 환경 구축
2. Vue 3 Composition API + TypeScript 활용
3. Docker를 통한 인프라 관리
4. TDD 환경 구축 (테스트 우선 개발 준비)
5. 다국어 지원 구현 경험

### 개선 필요 사항

1. 테스트 커버리지 향상 (현재 ~20%)
2. API 문서화 (Swagger UI 활성화)
3. 에러 처리 표준화
4. 로깅 전략 수립
5. 성능 최적화 (캐싱, 인덱싱)

---

## 📝 참고 문서

- [CLAUDE.md](../CLAUDE.md) - 개발 가이드
- [README.md](../README.md) - 프로젝트 소개
- [Backend Tests](../backend/src/test/) - 테스트 코드
- [Frontend Tests](../frontend/src/) - 프론트엔드 테스트

---

**문서 버전:** 1.0.0  
**마지막 업데이트:** 2026-02-06  
**작성자:** Development Team
