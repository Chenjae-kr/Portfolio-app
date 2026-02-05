# 개발 워크플로우 및 문서 관리 규칙

> **Portfolio Manager App 개발 가이드**  
> Claude AI를 포함한 모든 개발자가 따라야 할 워크플로우와 규칙

---

## 📋 목차

1. [개발 워크플로우](#-개발-워크플로우)
2. [문서 관리 규칙](#-문서-관리-규칙)
3. [커밋 컨벤션](#-커밋-컨벤션)
4. [테스트 규칙](#-테스트-규칙)
5. [코드 리뷰 체크리스트](#-코드-리뷰-체크리스트)
6. [문서 구조](#-문서-구조)

---

## 🔄 개발 워크플로우

### 기본 원칙

```
계획 → 구현 → 테스트 → 문서 업데이트 → 커밋/푸시
```

### 상세 프로세스

#### 1️⃣ 개발 시작 전

**필수 확인 사항:**
- [ ] `docs/NEXT_STEPS.md` 열어서 다음 우선순위 확인
- [ ] 해당 기능이 Sprint 계획에 있는지 확인
- [ ] 의존성 기능이 완료되었는지 확인

**작업 준비:**
```bash
# 최신 코드 pull
cd /Users/chenjae/project/Portfolio-app
git pull origin main

# 브랜치 생성 (선택사항)
git checkout -b feature/transaction-ledger

# 서버 실행 확인
docker-compose up -d  # 인프라
cd backend && ./gradlew bootRun  # 백엔드
cd frontend && npm run dev  # 프론트엔드
```

**문서 확인:**
- `CLAUDE.md` - 프로젝트 개요, 기술 스택, API 설계
- `docs/NEXT_STEPS.md` - 다음 할 일, 우선순위
- `docs/PROGRESS.md` - 현재 진척도

---

#### 2️⃣ 구현 단계

**Backend 개발 순서:**
1. **Entity 작성/수정** (필요시)
   - `@Entity`, `@Table` 정의
   - 관계 설정 (`@ManyToOne`, `@OneToMany`)
   - 생성일시, 수정일시 (`@PrePersist`, `@PreUpdate`)

2. **Repository 작성**
   - `JpaRepository` 상속
   - 커스텀 쿼리 메서드 추가
   - `@Query` 어노테이션 (복잡한 쿼리)

3. **Service 작성**
   - `@Service`, `@RequiredArgsConstructor`
   - `@Transactional` (쓰기 작업)
   - `@Transactional(readOnly = true)` (읽기 작업)
   - 비즈니스 로직 구현
   - 예외 처리 (`BusinessException`)

4. **Controller 작성**
   - `@RestController`, `@RequestMapping`
   - DTO 정의 (Request, Response)
   - 유효성 검증 (`@Valid`, `@NotNull`)
   - 에러 응답 처리

**Frontend 개발 순서:**
1. **API Client 작성** (`src/api/`)
   - TypeScript 인터페이스 정의
   - axios 호출 함수 작성

2. **Store 작성** (필요시) (`src/stores/`)
   - Pinia store 정의
   - 상태, 액션, 게터 구현

3. **Component 작성** (`src/components/`)
   - Vue 3 Composition API 사용
   - TypeScript 타입 정의
   - Props, Emits 명시

4. **View 작성** (`src/views/`)
   - 라우팅 설정
   - 컴포넌트 조합

5. **다국어 추가** (`src/locales/`)
   - `ko.ts`에 한국어 키 추가
   - `en.ts`에 영어 키 추가

**코딩 중 주의사항:**
- ✅ 기존 코드 스타일 유지
- ✅ 주석은 필요한 경우만 (복잡한 로직)
- ✅ 변수/함수명은 명확하고 의미있게
- ✅ 매직 넘버 대신 상수 사용
- ✅ 중복 코드 최소화 (DRY 원칙)

---

#### 3️⃣ 테스트 단계

**Backend 테스트 작성:**

```bash
cd backend
./gradlew test
```

**테스트 파일 위치:**
- Service Test: `src/test/java/com/portfolio/{domain}/service/`
- Controller Test: `src/test/java/com/portfolio/api/`

**테스트 작성 규칙:**
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class PortfolioServiceTest {
    
    @Test
    @DisplayName("포트폴리오 생성")
    void createPortfolio() {
        // Given
        // When
        // Then
    }
}
```

**필수 테스트:**
- [ ] 정상 케이스 (Happy Path)
- [ ] 예외 케이스 (에러 처리)
- [ ] 경계값 테스트
- [ ] 유효성 검증 테스트

**Frontend 테스트 작성:**

```bash
cd frontend
npm test
```

**테스트 파일 위치:**
- API Test: `src/api/*.spec.ts`
- Component Test: `src/components/**/*.spec.ts`
- Store Test: `src/stores/*.spec.ts`

**테스트 작성 규칙:**
```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('PortfolioService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  
  it('포트폴리오 목록 조회', async () => {
    // Arrange
    // Act
    // Assert
  });
});
```

**테스트 통과 기준:**
- ✅ 모든 테스트 통과 (0 failed)
- ✅ 새로운 테스트 추가 (최소 3개 이상)
- ✅ 기존 테스트 깨지지 않음 (회귀 테스트)

---

#### 4️⃣ 문서 업데이트 단계 ⭐ 중요!

**개발 완료 후 반드시 업데이트해야 할 문서:**

### A. `docs/PROGRESS.md` 업데이트

**업데이트 항목:**
1. **전체 진척률** (%)
   - 기능 완성도 재평가
   - 숫자 증가 (예: 38% → 45%)

2. **Core Features 구현 현황**
   ```markdown
   | 기능 | 진척률 | 상태 | 비고 |
   |------|--------|------|------|
   | Transaction Ledger | 80% | 🚧 진행중 | 거래 생성 완료, 조회 API 남음 |
   ```

3. **완료된 주요 기능**
   ```markdown
   - ✅ 거래 생성 API (2026-02-06)
   - ✅ 거래 내역 조회 (2026-02-06)
   ```

4. **API 엔드포인트 현황**
   ```markdown
   ### Transaction
   - [x] `POST /api/v1/portfolios/{id}/transactions` ✅
   - [x] `GET /api/v1/portfolios/{id}/transactions` ✅
   - [ ] `POST /api/v1/transactions/{id}/void`
   ```

5. **테스트 현황**
   ```markdown
   - **Backend**: 75개 (+ 15개)
   - **Frontend**: 24개 (+ 8개)
   ```

6. **다음 단계**
   ```markdown
   ### 우선순위 1: Valuation Engine 🔴
   - [ ] Valuation Service 구현
   - [ ] 평가액 조회 API
   ```

7. **최근 업데이트 섹션**
   ```markdown
   ## 최근 업데이트
   
   ### 2026-02-06
   - ✅ Transaction Ledger 구현 완료
   - ✅ 거래 생성/조회 API 추가
   - ✅ 복식부기 검증 로직
   - ✅ 테스트 15개 추가
   ```

8. **문서 버전**
   ```markdown
   **문서 버전:** 1.2.0 (이전: 1.1.0)
   ```

---

### B. `docs/NEXT_STEPS.md` 업데이트

**업데이트 항목:**

1. **현재 완료 상태**
   - 진척률 바 업데이트
   ```markdown
   ✅ 거래 관리        ████████████████░░░░  80%
   ```

2. **Sprint 체크리스트**
   ```markdown
   ### Sprint 1: Transaction Ledger ✅ 완료
   
   #### Backend
   - [x] Transaction Service 구현 ✅
   - [x] Position Calculation ✅
   - [x] API 엔드포인트 ✅
   ```

3. **다음 우선순위 재정렬**
   - 완료된 Sprint는 아래로 이동
   - 다음 Sprint를 최상단으로

4. **알려진 이슈 업데이트**
   - 해결된 이슈: ~~취소선~~
   - 새로운 이슈: 추가

5. **릴리스 계획 업데이트**
   ```markdown
   ### v0.1.0 (MVP) - 진행중
   
   **포함 기능:**
   - ✅ 인증 시스템
   - ✅ 포트폴리오 CRUD
   - ✅ 거래 입력
   - 🚧 평가액 계산 (진행중)
   ```

---

### C. `CHANGELOG.md` 작성 (선택사항)

릴리스 시점에 작성:

```markdown
## [0.1.0] - 2026-02-06

### Added
- Transaction Ledger 기능 추가
- 거래 생성/조회 API
- 복식부기 검증 로직

### Changed
- 포트폴리오 상세 페이지 UI 개선

### Fixed
- 회원가입 시 Workspace 자동 생성 버그 수정

### Tests
- Backend 테스트 15개 추가
- Frontend 테스트 8개 추가
```

---

### D. `API.md` 업데이트 (API 변경 시 필수)

**API 추가 시:**

```markdown
## 📝 거래 API (Transactions)

### 거래 생성

**POST** `/v1/portfolios/{id}/transactions`

포트폴리오에 새로운 거래를 생성합니다.

**Request Body:**
```json
{
  "occurredAt": "2026-02-06T10:30:00Z",
  "type": "BUY",
  "legs": [
    {
      "legType": "ASSET",
      "instrumentId": "inst-aapl",
      "quantity": 10,
      "price": 150.00,
      "amount": 1500.00
    },
    {
      "legType": "CASH",
      "currency": "USD",
      "amount": -1500.00
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "data": {
    "id": "txn-uuid",
    "type": "BUY",
    "status": "POSTED",
    "occurredAt": "2026-02-06T10:30:00Z"
  }
}
```
```

**업데이트 내용:**
- [ ] 새로운 엔드포인트 추가
- [ ] Request/Response 예제
- [ ] 에러 코드 (필요시)
- [ ] 사용 예제 (curl)

---

### E. `DATABASE.md` 업데이트 (DB 변경 시 필수)

**테이블 추가 시:**

```markdown
### 11. dividend_schedules (배당 일정)

**목적:** 배당 지급 일정 추적

```sql
CREATE TABLE dividend_schedules (
    id VARCHAR(36) PRIMARY KEY,
    instrument_id VARCHAR(36) NOT NULL REFERENCES instruments(id),
    ex_date DATE NOT NULL,
    pay_date DATE NOT NULL,
    amount_per_share DECIMAL(18, 6) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED'
);

CREATE INDEX idx_dividend_schedules_instrument ON dividend_schedules(instrument_id, ex_date);
```

**주요 필드:**
- `ex_date` - 배당락일
- `pay_date` - 배당 지급일
- `amount_per_share` - 주당 배당금

**특징:**
- 배당 캘린더 기능용
- 자동 배당 거래 생성에 활용
```

**업데이트 내용:**
- [ ] ERD 다이어그램 업데이트 (Mermaid)
- [ ] 테이블 상세 설명 추가
- [ ] 인덱스 추가 (필요시)
- [ ] Flyway 마이그레이션 작성

---

#### 5️⃣ 커밋 및 푸시

**Git 작업 순서:**

```bash
# 1. 상태 확인
git status

# 2. 파일 추가
git add .

# 3. 커밋 (컨벤션 준수)
git commit -m "feat: 거래 생성 및 조회 API 구현

- Transaction Service 추가
- 복식부기 검증 로직
- 거래 내역 페이지
- 테스트 15개 추가
"

# 4. 푸시
git push origin main
```

**커밋 전 체크리스트:**
- [ ] 모든 테스트 통과
- [ ] Linter 에러 없음
- [ ] `PROGRESS.md` 업데이트 완료
- [ ] `NEXT_STEPS.md` 업데이트 완료
- [ ] 불필요한 파일 제외 (`.DS_Store`, `node_modules/`, 등)

---

## 📚 문서 관리 규칙

### 문서 업데이트 타이밍

| 상황 | 업데이트 문서 | 필수 여부 |
|------|--------------|----------|
| 기능 개발 완료 | `PROGRESS.md` + `NEXT_STEPS.md` | ✅ 필수 |
| API 추가/변경 | `PROGRESS.md` + `API.md` | ✅ 필수 |
| DB 스키마 변경 | `DATABASE.md` + Flyway 마이그레이션 | ✅ 필수 |
| 버그 수정 | `PROGRESS.md` (알려진 이슈) | 🟡 권장 |
| 테스트 추가 | `PROGRESS.md` (테스트 수) | ✅ 필수 |
| 릴리스 | `CHANGELOG.md` | ✅ 필수 |
| 아키텍처 변경 | `docs/02_TECH_STACK.md` | ✅ 필수 |

### 문서 작성 원칙

**1. 명확성**
- ✅ 구체적인 숫자와 날짜 사용
- ✅ 모호한 표현 지양 ("조만간" ❌ → "2026-02-10까지" ✅)
- ✅ 체크박스로 진행 상태 표시

**2. 일관성**
- ✅ 동일한 용어 사용 (Portfolio, Transaction)
- ✅ 날짜 형식 통일 (YYYY-MM-DD)
- ✅ 이모지 일관되게 사용

**3. 최신성**
- ✅ 개발 완료 시 즉시 업데이트
- ✅ 오래된 정보 삭제 또는 아카이브
- ✅ 버전 번호 증가

**4. 구조화**
- ✅ 마크다운 헤딩 체계적 사용 (H1, H2, H3)
- ✅ 표, 리스트 활용
- ✅ 코드 블록으로 예제 제공

---

## 📝 커밋 컨벤션

### 커밋 메시지 형식

```
<type>: <subject>

<body>
```

### Type 종류

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat: 거래 생성 API 구현` |
| `fix` | 버그 수정 | `fix: 로그인 시 500 에러 수정` |
| `docs` | 문서 변경 | `docs: PROGRESS.md 업데이트` |
| `test` | 테스트 추가/수정 | `test: Transaction Service 테스트 추가` |
| `refactor` | 코드 리팩토링 | `refactor: Service 로직 분리` |
| `style` | 코드 포맷팅 | `style: Prettier 적용` |
| `chore` | 빌드/설정 변경 | `chore: Gradle 의존성 업데이트` |
| `perf` | 성능 개선 | `perf: 쿼리 최적화` |

### Subject 작성 규칙

- ✅ 50자 이내
- ✅ 명령형 사용 ("추가하다" ❌ → "추가" ✅)
- ✅ 마침표 없음
- ✅ 한글 사용

### Body 작성 규칙

- ✅ 72자마다 줄바꿈
- ✅ 무엇을, 왜 변경했는지 설명
- ✅ 세부 항목은 `-` 로 나열

### 좋은 커밋 메시지 예시

```bash
# 1. 단일 기능
git commit -m "feat: 포트폴리오 목표 비중 설정 API 추가"

# 2. 상세 설명 포함
git commit -m "$(cat <<'EOF'
feat: 거래 생성 및 조회 API 구현

- Transaction Service 추가
- 복식부기 검증 로직 (Legs 합계 = 0)
- 거래 내역 조회 필터링
- Position 재계산 트리거
- 테스트 15개 추가
EOF
)"

# 3. 버그 수정
git commit -m "fix: 회원가입 시 Workspace 자동 생성 누락 수정

- User 생성 후 Workspace 자동 생성
- workspaceId 외래키 제약 조건 만족
- 테스트 케이스 추가
"

# 4. 문서 업데이트
git commit -m "docs: 개발 진척도 업데이트 (v1.2.0)

- Transaction Ledger 완료 반영
- 진척률 38% → 45%
- API 엔드포인트 2개 추가
- 테스트 15개 증가
"
```

### 나쁜 커밋 메시지 예시 ❌

```bash
# 너무 모호함
git commit -m "수정"

# 너무 길고 구조화되지 않음
git commit -m "포트폴리오 관련 기능 추가하고 테스트도 작성했고 문서도 업데이트했음"

# 여러 type 혼합 (분리해야 함)
git commit -m "feat+fix: 기능 추가 및 버그 수정"
```

---

## 🧪 테스트 규칙

### 테스트 작성 원칙

**1. AAA 패턴 (Arrange-Act-Assert)**
```java
@Test
void createPortfolio() {
    // Arrange (준비)
    String name = "성장 포트폴리오";
    
    // Act (실행)
    Portfolio portfolio = portfolioService.create(name);
    
    // Assert (검증)
    assertThat(portfolio.getName()).isEqualTo(name);
}
```

**2. 테스트 네이밍**
- ✅ 메서드명: 동사_상태_기대결과
  - `createPortfolio_validData_success()`
  - `updateTargets_invalidSum_throwsException()`
- ✅ `@DisplayName`: 한글로 명확히
  - `@DisplayName("유효한 데이터로 포트폴리오 생성 성공")`

**3. 테스트 독립성**
- ✅ 각 테스트는 독립적으로 실행 가능
- ✅ `@Transactional`로 DB 롤백
- ✅ `@BeforeEach`로 초기화

**4. 테스트 커버리지 목표**
- Service: 80% 이상
- Controller: 70% 이상
- Repository: 기본 CRUD는 스킵, 커스텀 쿼리만 테스트

### 테스트 실행 명령어

**Backend:**
```bash
# 전체 테스트
./gradlew test

# 특정 클래스
./gradlew test --tests "PortfolioServiceTest"

# 특정 메서드
./gradlew test --tests "PortfolioServiceTest.createPortfolio"

# 커버리지 리포트
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

**Frontend:**
```bash
# 전체 테스트
npm test

# Watch 모드
npm test -- --watch

# 커버리지
npm test -- --coverage

# UI 모드
npm test -- --ui
```

---

## ✅ 코드 리뷰 체크리스트

### 기능 구현

- [ ] 요구사항을 모두 충족하는가?
- [ ] 예외 상황을 처리하는가?
- [ ] 에러 메시지가 사용자 친화적인가?
- [ ] 로깅이 적절한가?

### 코드 품질

- [ ] 변수/함수명이 명확한가?
- [ ] 함수는 한 가지 일만 하는가? (Single Responsibility)
- [ ] 중복 코드가 없는가?
- [ ] 매직 넘버 대신 상수를 사용하는가?
- [ ] 주석이 필요한 곳에만 있는가?

### 성능

- [ ] N+1 쿼리 문제가 없는가?
- [ ] 불필요한 DB 호출이 없는가?
- [ ] 캐싱을 고려했는가?
- [ ] 페이징을 적용했는가? (대량 데이터)

### 보안

- [ ] SQL Injection 취약점이 없는가?
- [ ] XSS 방어가 되어있는가?
- [ ] 인증/인가 처리가 적절한가?
- [ ] 민감 정보가 로그에 노출되지 않는가?

### 테스트

- [ ] 단위 테스트가 작성되었는가?
- [ ] 통합 테스트가 필요한가?
- [ ] 테스트가 모두 통과하는가?
- [ ] 엣지 케이스를 테스트하는가?

### 문서

- [ ] API 문서가 업데이트되었는가?
- [ ] `PROGRESS.md`가 업데이트되었는가?
- [ ] `NEXT_STEPS.md`가 업데이트되었는가?
- [ ] 복잡한 로직에 주석이 있는가?

---

## 📁 문서 구조

### 문서 위치 및 역할

```
Portfolio-app/
├── README.md                          # 프로젝트 소개 (한국어)
├── CLAUDE.md                          # Claude용 프로젝트 가이드
├── docs/
│   ├── README.md                      # 문서 인덱스
│   ├── 01_PROJECT_OVERVIEW.md         # 프로젝트 개요
│   ├── 02_TECH_STACK.md               # 기술 스택
│   ├── API.md                         # ⭐ API 레퍼런스 (API 변경 시)
│   ├── DATABASE.md                    # ⭐ DB 설계 (스키마 변경 시)
│   ├── PROGRESS.md                    # ⭐ 개발 진척도 (자주 업데이트)
│   ├── NEXT_STEPS.md                  # ⭐ 다음 단계 (자주 업데이트)
│   ├── DEVELOPMENT_WORKFLOW.md        # ⭐ 이 문서 (워크플로우 규칙)
│   └── CHANGELOG.md                   # 변경 이력 (릴리스 시)
└── ...
```

### 문서별 업데이트 빈도

| 문서 | 업데이트 빈도 | 업데이트 트리거 |
|------|--------------|----------------|
| `PROGRESS.md` | 높음 (매 기능 완료) | 기능 개발, 테스트 추가 |
| `NEXT_STEPS.md` | 높음 (매 기능 완료) | Sprint 진행, 우선순위 변경 |
| `API.md` | 중간 (API 변경 시) | 엔드포인트 추가/수정, 응답 구조 변경 |
| `DATABASE.md` | 중간 (스키마 변경 시) | 테이블 추가/수정, 인덱스 변경 |
| `DEVELOPMENT_WORKFLOW.md` | 낮음 | 개발 프로세스 변경 |
| `CHANGELOG.md` | 중간 (릴리스 시) | 버전 릴리스 |
| `01_PROJECT_OVERVIEW.md` | 낮음 | 프로젝트 방향 변경 |
| `02_TECH_STACK.md` | 낮음 | 기술 스택 변경 |
| `CLAUDE.md` | 낮음 | API 설계 원칙 변경 |

---

## 🤖 Claude AI를 위한 특별 규칙

### 개발 시작 시

1. **항상 먼저 확인:**
   ```
   - docs/NEXT_STEPS.md의 "다음 우선순위" 확인
   - docs/PROGRESS.md의 "다음 단계" 확인
   - 현재 Sprint가 무엇인지 파악
   ```

2. **질문 받았을 때:**
   - 기능 개발 요청 → NEXT_STEPS.md 참고
   - 버그 수정 요청 → PROGRESS.md의 "알려진 이슈" 확인
   - 현재 상태 질문 → PROGRESS.md 참조

### 개발 중

1. **코드 작성 시:**
   - CLAUDE.md의 "Domain Model Principles" 준수
   - 기존 코드 스타일 유지
   - 중복 코드 피하기

2. **API 추가 시:**
   - CLAUDE.md의 "API Design" 섹션 참조
   - RESTful 원칙 준수
   - DTO 검증 추가

3. **DB 변경 시:**
   - Flyway 마이그레이션 작성
   - 외래 키 제약 조건 확인

### 개발 완료 시 (매우 중요!)

**필수 작업 순서:**

1. **테스트 실행 및 확인**
   ```bash
   cd backend && ./gradlew test
   cd frontend && npm test
   ```

2. **문서 업데이트 (순서대로)**
   
   a) `docs/PROGRESS.md` 업데이트:
   - [ ] 전체 진척률 증가
   - [ ] Core Features 표 업데이트
   - [ ] 완료된 주요 기능 추가
   - [ ] API 엔드포인트 체크
   - [ ] 테스트 수 증가
   - [ ] "최근 업데이트" 섹션 추가
   - [ ] 문서 버전 증가
   
   b) `docs/NEXT_STEPS.md` 업데이트:
   - [ ] 완료된 Sprint 체크
   - [ ] 다음 Sprint 우선순위 재정렬
   - [ ] 알려진 이슈 업데이트
   - [ ] 릴리스 계획 업데이트

3. **Git 커밋**
   ```bash
   # 기능 커밋
   git add <feature-files>
   git commit -m "feat: <기능 요약>"
   
   # 문서 커밋 (별도로)
   git add docs/PROGRESS.md docs/NEXT_STEPS.md
   git commit -m "docs: 개발 진척도 업데이트 (v1.x.0)"
   
   # 푸시
   git push origin main
   ```

4. **사용자에게 보고**
   - 완료된 기능 요약
   - 추가된 테스트 수
   - 업데이트된 문서 목록
   - 다음 우선순위 안내

### 사용자 요청 해석

**"다음 개발해줘" → NEXT_STEPS.md의 최상위 우선순위 개발**

**"테스트 작성해줘" → 해당 기능의 Service + Controller 테스트 모두 작성**

**"문서 업데이트해줘" → PROGRESS.md + NEXT_STEPS.md 모두 업데이트**

**"커밋해줘" → 테스트 확인 후 문서 업데이트 후 커밋**

---

## 🎯 체크리스트 템플릿

### 새 기능 개발 완료 체크리스트

**코드 구현:**
- [ ] Backend Entity/Repository/Service/Controller 구현
- [ ] Frontend API/Store/Component/View 구현
- [ ] 다국어 번역 추가 (ko.ts, en.ts)
- [ ] 에러 처리 구현

**테스트:**
- [ ] Backend 테스트 작성 (Service + Controller)
- [ ] Frontend 테스트 작성 (API + Component)
- [ ] 모든 테스트 통과 확인
- [ ] 기존 테스트 회귀 없음 확인

**문서:**
- [ ] `docs/PROGRESS.md` 업데이트
  - [ ] 전체 진척률
  - [ ] Core Features 표
  - [ ] 완료된 주요 기능
  - [ ] API 엔드포인트
  - [ ] 테스트 수
  - [ ] 최근 업데이트
  - [ ] 문서 버전
- [ ] `docs/NEXT_STEPS.md` 업데이트
  - [ ] 완료 Sprint 체크
  - [ ] 다음 우선순위 정렬
  - [ ] 알려진 이슈
  - [ ] 릴리스 계획
- [ ] `docs/API.md` 업데이트 (API 변경 시)
  - [ ] 새 엔드포인트 추가
  - [ ] Request/Response 예제
  - [ ] 에러 코드 업데이트
- [ ] `docs/DATABASE.md` 업데이트 (스키마 변경 시)
  - [ ] ERD 다이어그램 수정
  - [ ] 테이블 설명 추가
  - [ ] 인덱스 전략 업데이트

**Git:**
- [ ] 기능 커밋 (feat:)
- [ ] 문서 커밋 (docs:)
- [ ] Push to origin/main
- [ ] 커밋 메시지 컨벤션 준수

**완료 보고:**
- [ ] 완료 기능 요약
- [ ] 테스트 결과 공유
- [ ] 다음 우선순위 안내

---

## 📖 참고 자료

### 내부 문서
- `CLAUDE.md` - 프로젝트 가이드
- `docs/PROGRESS.md` - 진척도
- `docs/NEXT_STEPS.md` - 다음 단계
- `docs/01_PROJECT_OVERVIEW.md` - 프로젝트 개요
- `docs/02_TECH_STACK.md` - 기술 스택

### 외부 참고
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
- [Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)

---

## 🔄 이 문서의 업데이트

**업데이트 시점:**
- 개발 프로세스가 변경될 때
- 새로운 규칙이 추가될 때
- 기존 규칙이 비효율적으로 판명될 때

**업데이트 방법:**
1. 변경 사항 논의
2. 문서 수정
3. 팀원(Claude 포함) 공유
4. Git 커밋: `docs: 워크플로우 규칙 업데이트`

---

**문서 버전:** 1.0.0  
**최초 작성:** 2026-02-06  
**최종 업데이트:** 2026-02-06  
**작성자:** Portfolio Manager Team  

---

<div align="center">

**이 문서를 준수하여 일관된 개발 프로세스를 유지합니다** 🚀

**[⬆️ 맨 위로](#개발-워크플로우-및-문서-관리-규칙)**

</div>
