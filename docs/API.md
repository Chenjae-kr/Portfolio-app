# API Reference

> **Portfolio Manager App** REST API 명세서  
> Base URL: `http://localhost:8080/api`

---

## 📋 목차

1. [개요](#-개요)
2. [인증](#-인증)
3. [공통 응답 구조](#-공통-응답-구조)
4. [에러 코드](#-에러-코드)
5. [인증 API](#-인증-api-auth)
6. [포트폴리오 API](#-포트폴리오-api-portfolios)
7. [포트폴리오 목표 비중 API](#-포트폴리오-목표-비중-api-portfolio-targets)
8. [금융상품 API](#-금융상품-api-instruments)
9. [포트폴리오 그룹 API](#-포트폴리오-그룹-api-portfolio-groups)
10. [구현된 추가 API](#-구현된-추가-api-상세는-progressmd-참고)
11. [향후 추가 예정 API](#-향후-추가-예정-api)

---

## 📌 개요

### Base URL

```
Development: http://localhost:8080/api
Production: https://api.portfolio-manager.com (TBD)
```

### API 버전

현재 버전: **v1**

모든 엔드포인트는 `/v1/` 접두사를 사용합니다.

### Content-Type

```
Content-Type: application/json
Accept: application/json
```

---

## 🔐 인증

### JWT Bearer Token

대부분의 API는 JWT 토큰을 요구합니다.

**헤더 형식:**
```http
Authorization: Bearer <access_token>
```

**토큰 획득:**
1. `/v1/auth/login` 또는 `/v1/auth/register`로 로그인/회원가입
2. 응답에서 `accessToken` 획득
3. 이후 요청에 Bearer 토큰으로 포함

**토큰 갱신:**
- `/v1/auth/refresh` 엔드포인트 사용 (구현 예정)

---

## 📦 공통 응답 구조

### 성공 응답

```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": null
}
```

### 에러 응답

```json
{
  "data": null,
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": {
    "code": "PORTFOLIO_NOT_FOUND",
    "message": "Portfolio not found: portfolio-123"
  }
}
```

### 페이징 응답

```json
{
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20
  },
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z",
    "totalElements": 100,
    "totalPages": 5,
    "currentPage": 0,
    "size": 20
  },
  "error": null
}
```

---

## ⚠️ 에러 코드

### 공통 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |
| `INVALID_INPUT` | 400 | 잘못된 입력 |
| `VALIDATION_ERROR` | 422 | 유효성 검증 실패 |

### 인증 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `AUTH_REQUIRED` | 401 | 인증 필요 |
| `INVALID_TOKEN` | 401 | 유효하지 않거나 만료된 토큰 |
| `FORBIDDEN` | 403 | 접근 권한 없음 |
| `AUTH_FAILED` | 401 | 로그인 실패 |
| `REGISTRATION_FAILED` | 400 | 회원가입 실패 |

### 포트폴리오 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `PORTFOLIO_NOT_FOUND` | 404 | 포트폴리오를 찾을 수 없음 |
| `INVALID_TARGET_WEIGHTS` | 422 | 목표 비중 합계가 1.0이 아님 |

### 거래 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `TRANSACTION_NOT_FOUND` | 404 | 거래를 찾을 수 없음 |
| `INVALID_TRANSACTION_LEGS` | 422 | 거래 Legs 검증 실패 |
| `TRANSACTION_ALREADY_VOID` | 409 | 이미 취소된 거래 |

### 금융상품 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `INSTRUMENT_NOT_FOUND` | 404 | 금융상품을 찾을 수 없음 |

### 가격 데이터 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `PRICE_DATA_UNAVAILABLE` | 503 | 가격 데이터를 사용할 수 없음 |
| `FX_DATA_UNAVAILABLE` | 503 | 환율 데이터를 사용할 수 없음 |

### 백테스트 에러

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `BACKTEST_NOT_FOUND` | 404 | 백테스트를 찾을 수 없음 |
| `BACKTEST_FAILED` | 500 | 백테스트 실행 실패 |

---

## 🔑 인증 API (Auth)

### 회원가입

**POST** `/v1/auth/register`

사용자 계정을 생성하고 자동으로 Workspace를 생성합니다.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!",
  "displayName": "홍길동"
}
```

**Response:** `201 Created`
```json
{
  "data": {
    "user": {
      "id": "user-uuid",
      "email": "user@example.com",
      "displayName": "홍길동"
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh-token-uuid"
  },
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": null
}
```

**에러:**
- `400 REGISTRATION_FAILED` - 이미 존재하는 이메일

---

### 로그인

**POST** `/v1/auth/login`

이메일과 비밀번호로 로그인합니다.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!"
}
```

**Response:** `200 OK`
```json
{
  "data": {
    "user": {
      "id": "user-uuid",
      "email": "user@example.com",
      "displayName": "홍길동"
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh-token-uuid"
  },
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": null
}
```

**에러:**
- `401 AUTH_FAILED` - 잘못된 이메일 또는 비밀번호

---

### 토큰 갱신

**POST** `/v1/auth/refresh`

Refresh Token으로 새로운 Access Token을 발급받습니다.

**Request Body:**
```json
{
  "refreshToken": "refresh-token-uuid"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "mock-access-token-1234567890"
}
```

**Status:** 🚧 Mock 구현 (실제 로직 구현 필요)

---

### 로그아웃

**POST** `/v1/auth/logout`

현재 세션을 종료합니다.

**Response:** `200 OK`
```json
{
  "message": "Logged out successfully"
}
```

---

## 📂 포트폴리오 API (Portfolios)

### 포트폴리오 목록 조회

**GET** `/v1/portfolios`

현재 워크스페이스의 모든 포트폴리오를 조회합니다.

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": "portfolio-uuid-1",
      "name": "성장 포트폴리오",
      "description": "공격적 성장 전략",
      "baseCurrency": "KRW",
      "type": "REAL",
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-02-01T14:30:00"
    },
    {
      "id": "portfolio-uuid-2",
      "name": "안정 포트폴리오",
      "description": "채권 중심 안정 전략",
      "baseCurrency": "USD",
      "type": "REAL",
      "createdAt": "2026-01-20T09:00:00",
      "updatedAt": "2026-01-20T09:00:00"
    }
  ],
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": null
}
```

---

### 포트폴리오 상세 조회

**GET** `/v1/portfolios/{id}`

특정 포트폴리오의 상세 정보를 조회합니다.

**Path Parameters:**
- `id` (string, required) - 포트폴리오 ID

**Response:** `200 OK`
```json
{
  "data": {
    "id": "portfolio-uuid-1",
    "name": "성장 포트폴리오",
    "description": "공격적 성장 전략",
    "baseCurrency": "KRW",
    "type": "REAL",
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": "2026-02-01T14:30:00"
  },
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": null
}
```

**에러:**
- `404 NOT_FOUND` - 포트폴리오를 찾을 수 없음

---

### 포트폴리오 생성

**POST** `/v1/portfolios`

새로운 포트폴리오를 생성합니다.

**Request Body:**
```json
{
  "name": "성장 포트폴리오",
  "description": "공격적 성장 전략",
  "baseCurrency": "KRW",
  "type": "REAL"
}
```

**필드 설명:**
- `name` (string, required) - 포트폴리오 이름
- `description` (string, optional) - 설명
- `baseCurrency` (string, required) - 기준 통화 (ISO 4217: KRW, USD, EUR 등)
- `type` (string, required) - 포트폴리오 유형
  - `REAL` - 실제 계좌
  - `HYPOTHETICAL` - 가상 포트폴리오
  - `BACKTEST_ONLY` - 백테스트 전용

**Response:** `201 Created`
```json
{
  "data": {
    "id": "portfolio-uuid-new",
    "name": "성장 포트폴리오",
    "description": "공격적 성장 전략",
    "baseCurrency": "KRW",
    "type": "REAL",
    "createdAt": "2026-02-06T10:30:00",
    "updatedAt": "2026-02-06T10:30:00"
  },
  "meta": {
    "timestamp": "2026-02-06T10:30:00.000Z"
  },
  "error": null
}
```

---

### 포트폴리오 수정

**PATCH** `/v1/portfolios/{id}`

포트폴리오의 이름과 설명을 수정합니다.

**Path Parameters:**
- `id` (string, required) - 포트폴리오 ID

**Request Body:**
```json
{
  "name": "수정된 포트폴리오 이름",
  "description": "수정된 설명"
}
```

**Response:** `200 OK`
```json
{
  "data": {
    "id": "portfolio-uuid-1",
    "name": "수정된 포트폴리오 이름",
    "description": "수정된 설명",
    "baseCurrency": "KRW",
    "type": "REAL",
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": "2026-02-06T10:35:00"
  },
  "meta": {
    "timestamp": "2026-02-06T10:35:00.000Z"
  },
  "error": null
}
```

**에러:**
- `404 NOT_FOUND` - 포트폴리오를 찾을 수 없음

---

### 포트폴리오 삭제 (아카이브)

**DELETE** `/v1/portfolios/{id}`

포트폴리오를 아카이브합니다 (소프트 삭제).

**Path Parameters:**
- `id` (string, required) - 포트폴리오 ID

**Response:** `200 OK`
```json
{
  "data": {
    "message": "Portfolio archived successfully"
  },
  "meta": {
    "timestamp": "2026-02-06T10:40:00.000Z"
  },
  "error": null
}
```

**에러:**
- `404 NOT_FOUND` - 포트폴리오를 찾을 수 없음

---

## 🎯 포트폴리오 목표 비중 API (Portfolio Targets)

### 목표 비중 조회

**GET** `/v1/portfolios/{id}/targets`

포트폴리오의 목표 비중 설정을 조회합니다.

**Path Parameters:**
- `id` (string, required) - 포트폴리오 ID

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": "target-uuid-1",
      "instrumentId": "inst-aapl",
      "assetClass": "EQUITY",
      "currency": "USD",
      "targetWeight": 0.35,
      "minWeight": 0.30,
      "maxWeight": 0.40,
      "updatedAt": "2026-02-06T10:00:00"
    },
    {
      "id": "target-uuid-2",
      "instrumentId": "inst-voo",
      "assetClass": "EQUITY",
      "currency": "USD",
      "targetWeight": 0.25,
      "minWeight": null,
      "maxWeight": null,
      "updatedAt": "2026-02-06T10:00:00"
    },
    {
      "id": "target-uuid-3",
      "instrumentId": null,
      "assetClass": "CASH",
      "currency": "KRW",
      "targetWeight": 0.40,
      "minWeight": 0.20,
      "maxWeight": 0.50,
      "updatedAt": "2026-02-06T10:00:00"
    }
  ],
  "meta": {
    "timestamp": "2026-02-06T10:45:00.000Z"
  },
  "error": null
}
```

**필드 설명:**
- `instrumentId` - 금융상품 ID (`null`이면 현금)
- `assetClass` - 자산 클래스 (`EQUITY`, `BOND`, `COMMODITY`, `CASH`, `ALT`)
- `currency` - 통화 코드
- `targetWeight` - 목표 비중 (0.0 ~ 1.0, 합계는 1.0이어야 함)
- `minWeight` / `maxWeight` - 최소/최대 비중 (리밸런싱 밴드용, optional)

---

### 목표 비중 설정/수정

**PUT** `/v1/portfolios/{id}/targets`

포트폴리오의 목표 비중을 설정하거나 수정합니다.

**Path Parameters:**
- `id` (string, required) - 포트폴리오 ID

**Query Parameters:**
- `normalize` (boolean, optional, default: `false`) - 비중 합계를 자동으로 1.0으로 정규화

**Request Body:**
```json
{
  "targets": [
    {
      "instrumentId": "inst-aapl",
      "assetClass": "EQUITY",
      "currency": "USD",
      "targetWeight": 0.35,
      "minWeight": 0.30,
      "maxWeight": 0.40
    },
    {
      "instrumentId": "inst-voo",
      "assetClass": "EQUITY",
      "currency": "USD",
      "targetWeight": 0.25
    },
    {
      "instrumentId": null,
      "assetClass": "CASH",
      "currency": "KRW",
      "targetWeight": 0.40
    }
  ]
}
```

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": "target-uuid-new-1",
      "instrumentId": "inst-aapl",
      "assetClass": "EQUITY",
      "currency": "USD",
      "targetWeight": 0.35,
      "minWeight": 0.30,
      "maxWeight": 0.40,
      "updatedAt": "2026-02-06T10:50:00"
    },
    {
      "id": "target-uuid-new-2",
      "instrumentId": "inst-voo",
      "assetClass": "EQUITY",
      "currency": "USD",
      "targetWeight": 0.25,
      "minWeight": null,
      "maxWeight": null,
      "updatedAt": "2026-02-06T10:50:00"
    },
    {
      "id": "target-uuid-new-3",
      "instrumentId": null,
      "assetClass": "CASH",
      "currency": "KRW",
      "targetWeight": 0.40,
      "minWeight": null,
      "maxWeight": null,
      "updatedAt": "2026-02-06T10:50:00"
    }
  ],
  "meta": {
    "timestamp": "2026-02-06T10:50:00.000Z"
  },
  "error": null
}
```

**검증 규칙:**
- ✅ `targetWeight` 합계가 1.0이어야 함 (±0.0005 허용)
- ✅ `normalize=true`이면 자동으로 비율에 맞춰 정규화
- ✅ 현금 타겟: `instrumentId=null`, `currency` 필수
- ✅ 종목 타겟: `instrumentId` 필수

**에러:**
- `404 NOT_FOUND` - 포트폴리오를 찾을 수 없음
- `422 INVALID_TARGET_WEIGHTS` - 비중 합계가 1.0이 아님

---

## 🏢 금융상품 API (Instruments)

### 금융상품 검색

**GET** `/v1/instruments/search`

금융상품을 검색합니다 (이름 또는 티커).

**Query Parameters:**
- `q` (string, optional) - 검색어 (이름 또는 티커)
- `assetClass` (string, optional) - 자산 클래스 필터 (`EQUITY`, `BOND`, `COMMODITY`, `CASH`, `ALT`)
- `page` (integer, optional, default: 0) - 페이지 번호 (0-based)
- `size` (integer, optional, default: 20) - 페이지 크기

**Response:** `200 OK`
```json
{
  "data": {
    "content": [
      {
        "id": "inst-aapl",
        "instrumentType": "STOCK",
        "name": "Apple Inc.",
        "ticker": "AAPL",
        "currency": "USD",
        "country": "US",
        "assetClass": "EQUITY",
        "sector": "Technology",
        "industry": "Consumer Electronics",
        "provider": null,
        "status": "ACTIVE"
      },
      {
        "id": "inst-voo",
        "instrumentType": "ETF",
        "name": "Vanguard S&P 500 ETF",
        "ticker": "VOO",
        "currency": "USD",
        "country": "US",
        "assetClass": "EQUITY",
        "sector": null,
        "industry": null,
        "provider": "Vanguard",
        "status": "ACTIVE"
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "number": 0,
    "size": 20
  },
  "meta": {
    "timestamp": "2026-02-06T11:00:00.000Z",
    "totalElements": 2,
    "totalPages": 1,
    "currentPage": 0,
    "size": 20
  },
  "error": null
}
```

**예제:**
```bash
# 이름 또는 티커로 검색
GET /v1/instruments/search?q=Apple

# 자산 클래스별 필터링
GET /v1/instruments/search?assetClass=EQUITY

# 검색어 + 자산 클래스
GET /v1/instruments/search?q=Vanguard&assetClass=EQUITY&page=0&size=10
```

---

### 금융상품 상세 조회

**GET** `/v1/instruments/{id}`

특정 금융상품의 상세 정보를 조회합니다.

**Path Parameters:**
- `id` (string, required) - 금융상품 ID

**Response:** `200 OK`
```json
{
  "data": {
    "id": "inst-aapl",
    "instrumentType": "STOCK",
    "name": "Apple Inc.",
    "ticker": "AAPL",
    "currency": "USD",
    "country": "US",
    "assetClass": "EQUITY",
    "sector": "Technology",
    "industry": "Consumer Electronics",
    "provider": null,
    "status": "ACTIVE"
  },
  "meta": {
    "timestamp": "2026-02-06T11:05:00.000Z"
  },
  "error": null
}
```

**에러:**
- `404 INSTRUMENT_NOT_FOUND` - 금융상품을 찾을 수 없음

---

### 금융상품 목록 조회

**GET** `/v1/instruments`

금융상품 목록을 조회합니다.

**Query Parameters:**
- `assetClass` (string, optional) - 자산 클래스 필터

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": "inst-aapl",
      "instrumentType": "STOCK",
      "name": "Apple Inc.",
      "ticker": "AAPL",
      "currency": "USD",
      "country": "US",
      "assetClass": "EQUITY",
      "sector": "Technology",
      "industry": "Consumer Electronics",
      "provider": null,
      "status": "ACTIVE"
    }
  ],
  "meta": {
    "timestamp": "2026-02-06T11:10:00.000Z",
    "count": 1
  },
  "error": null
}
```

---

## 📁 포트폴리오 그룹 API (Portfolio Groups)

### 그룹 목록 조회

**GET** `/v1/portfolio-groups`

워크스페이스의 모든 포트폴리오 그룹을 조회합니다 (정렬 순서대로).

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": "group-uuid-1",
      "name": "주식 포트폴리오",
      "sortOrder": 0,
      "createdAt": "2026-01-10T09:00:00"
    },
    {
      "id": "group-uuid-2",
      "name": "채권 포트폴리오",
      "sortOrder": 1,
      "createdAt": "2026-01-11T10:00:00"
    }
  ],
  "meta": {
    "timestamp": "2026-02-06T11:15:00.000Z"
  },
  "error": null
}
```

---

### 그룹 생성

**POST** `/v1/portfolio-groups`

새로운 포트폴리오 그룹을 생성합니다.

**Request Body:**
```json
{
  "name": "신규 그룹",
  "sortOrder": 0
}
```

**필드 설명:**
- `name` (string, required) - 그룹 이름
- `sortOrder` (integer, optional, default: 0) - 정렬 순서

**Response:** `201 Created`
```json
{
  "data": {
    "id": "group-uuid-new",
    "name": "신규 그룹",
    "sortOrder": 0,
    "createdAt": "2026-02-06T11:20:00"
  },
  "meta": {
    "timestamp": "2026-02-06T11:20:00.000Z"
  },
  "error": null
}
```

**에러:**
- `400 BAD_REQUEST` - 동일한 이름의 그룹이 이미 존재

---

### 그룹 수정

**PATCH** `/v1/portfolio-groups/{id}`

그룹의 이름 또는 정렬 순서를 수정합니다.

**Path Parameters:**
- `id` (string, required) - 그룹 ID

**Request Body:**
```json
{
  "name": "수정된 그룹 이름",
  "sortOrder": 2
}
```

**Response:** `200 OK`
```json
{
  "data": {
    "id": "group-uuid-1",
    "name": "수정된 그룹 이름",
    "sortOrder": 2,
    "createdAt": "2026-01-10T09:00:00"
  },
  "meta": {
    "timestamp": "2026-02-06T11:25:00.000Z"
  },
  "error": null
}
```

**에러:**
- `404 NOT_FOUND` - 그룹을 찾을 수 없음

---

### 그룹 삭제

**DELETE** `/v1/portfolio-groups/{id}`

포트폴리오 그룹을 삭제합니다.

**Path Parameters:**
- `id` (string, required) - 그룹 ID

**Response:** `200 OK`
```json
{
  "data": {
    "message": "Group deleted successfully"
  },
  "meta": {
    "timestamp": "2026-02-06T11:30:00.000Z"
  },
  "error": null
}
```

**에러:**
- `404 NOT_FOUND` - 그룹을 찾을 수 없음

---

## ✅ 구현된 추가 API (상세는 PROGRESS.md 참고)

다음 API들은 이미 구현되어 있습니다. 요청/응답 상세는 Swagger UI (`/api/swagger-ui.html`) 또는 [PROGRESS.md](PROGRESS.md) 엔드포인트 현황을 참고하세요.

### Transaction (거래) ✅

- `POST /v1/portfolios/{id}/transactions` - 거래 생성
- `GET /v1/portfolios/{id}/transactions` - 거래 내역 조회
- `GET /v1/transactions/{id}` - 거래 상세 조회
- `POST /v1/transactions/{id}/void` - 거래 취소

### Valuation & Performance (평가·성과) ✅

- `GET /v1/portfolios/{id}/valuation` - 포트폴리오 평가액 조회
- `GET /v1/portfolios/{id}/performance` - 성과 지표 조회 (TWR, CAGR, Volatility, MDD, Sharpe)

### Compare (포트폴리오 비교) ✅

- `POST /v1/compare/performance` - 다중 포트폴리오 성과 비교

### Backtest (백테스트) ✅

- `POST /v1/backtests/runs` - 백테스트 실행
- `GET /v1/backtests/runs` - 백테스트 목록 조회
- `GET /v1/backtests/runs/{id}` - 백테스트 상태 조회
- `GET /v1/backtests/runs/{id}/results` - 백테스트 결과 조회
- `DELETE /v1/backtests/runs/{id}` - 백테스트 삭제

### Rebalance (리밸런싱) ✅

- `GET /v1/portfolios/{id}/rebalance` - 현재 vs 목표 비중 및 매매 추천

---

## 🚧 향후 추가 예정 API

### Price Data (가격 데이터)

```
GET    /v1/prices/{instrumentId}/latest       # 최신 가격 조회
GET    /v1/prices/{instrumentId}/history      # 가격 히스토리 조회
GET    /v1/fx-rates/{base}/{quote}            # 환율 조회
```

---

## 📖 사용 예제

### 1. 회원가입 및 로그인

```bash
# 회원가입
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123!",
    "displayName": "홍길동"
  }'

# 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123!"
  }'

# 응답에서 accessToken 획득
# 이후 요청에 Authorization: Bearer <token> 헤더 포함
```

---

### 2. 포트폴리오 생성 및 조회

```bash
# 포트폴리오 생성
curl -X POST http://localhost:8080/api/v1/portfolios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "성장 포트폴리오",
    "description": "공격적 성장 전략",
    "baseCurrency": "KRW",
    "type": "REAL"
  }'

# 포트폴리오 목록 조회
curl -X GET http://localhost:8080/api/v1/portfolios \
  -H "Authorization: Bearer <token>"

# 포트폴리오 상세 조회
curl -X GET http://localhost:8080/api/v1/portfolios/{portfolio-id} \
  -H "Authorization: Bearer <token>"
```

---

### 3. 금융상품 검색 및 목표 비중 설정

```bash
# 금융상품 검색
curl -X GET "http://localhost:8080/api/v1/instruments/search?q=Apple&assetClass=EQUITY" \
  -H "Authorization: Bearer <token>"

# 목표 비중 설정
curl -X PUT "http://localhost:8080/api/v1/portfolios/{portfolio-id}/targets?normalize=false" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "targets": [
      {
        "instrumentId": "inst-aapl",
        "assetClass": "EQUITY",
        "currency": "USD",
        "targetWeight": 0.35
      },
      {
        "instrumentId": "inst-voo",
        "assetClass": "EQUITY",
        "currency": "USD",
        "targetWeight": 0.25
      },
      {
        "instrumentId": null,
        "assetClass": "CASH",
        "currency": "KRW",
        "targetWeight": 0.40
      }
    ]
  }'
```

---

### 4. 비중 자동 정규화

```bash
# 비중 합계가 1.0이 아닌 경우 normalize=true로 자동 조정
curl -X PUT "http://localhost:8080/api/v1/portfolios/{portfolio-id}/targets?normalize=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "targets": [
      {
        "instrumentId": "inst-aapl",
        "assetClass": "EQUITY",
        "currency": "USD",
        "targetWeight": 35
      },
      {
        "instrumentId": "inst-voo",
        "assetClass": "EQUITY",
        "currency": "USD",
        "targetWeight": 25
      },
      {
        "instrumentId": null,
        "assetClass": "CASH",
        "currency": "KRW",
        "targetWeight": 40
      }
    ]
  }'

# 자동으로 35, 25, 40 → 0.35, 0.25, 0.40으로 정규화됨
```

---

## 📝 참고사항

### 데이터 형식

**날짜/시간:**
- ISO 8601 형식 사용
- 예: `2026-02-06T10:30:00.000Z`

**통화 코드:**
- ISO 4217 (3글자)
- 예: `KRW`, `USD`, `EUR`, `JPY`

**국가 코드:**
- ISO 3166-1 alpha-2 (2글자)
- 예: `KR`, `US`, `CN`, `JP`

**Decimal 값:**
- 비중: 최대 소수점 4자리 (예: `0.3500`)
- 가격: 최대 소수점 6자리
- 금액: 최대 소수점 4자리

---

## 🔄 버전 변경 이력

### v1.1.0 (Current)
- 거래 API (생성/조회/취소)
- 평가·성과 API (valuation, performance)
- 포트폴리오 비교 API (compare/performance)
- 백테스트 API (runs, results)
- 리밸런싱 API (portfolios/{id}/rebalance)

### v1.0.0
- 인증 API (Login, Register, Refresh, Logout)
- 포트폴리오 CRUD 및 목표 비중 설정
- 금융상품 검색 및 조회
- 포트폴리오 그룹 관리

---

**문서 버전:** 1.1.0  
**최종 업데이트:** 2026-02-06  
**API 버전:** v1  

**관련 문서:**
- [데이터베이스 설계](DATABASE.md)
- [개발 워크플로우](DEVELOPMENT_WORKFLOW.md)
- [프로젝트 가이드](../CLAUDE.md)

---

<div align="center">

**API 사용 중 문제가 있으시면 이슈를 등록해주세요** 💬

**[⬆️ 맨 위로](#api-reference)**

</div>
