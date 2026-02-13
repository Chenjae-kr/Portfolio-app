# API Reference

최종 업데이트: 2026-02-13

Base URL: `http://localhost:8080/api`

버전 Prefix: `/v1`

예시: `GET http://localhost:8080/api/v1/portfolios`

## 공통

### 인증

- 인증 방식: JWT Bearer
- 헤더: `Authorization: Bearer <accessToken>`
- 무인증 허용: `POST /v1/auth/login`, `POST /v1/auth/register`, `POST /v1/auth/refresh`

### 응답 형식

```json
{
  "data": {},
  "meta": {
    "timestamp": "2026-02-13T00:00:00Z"
  },
  "error": null
}
```

에러 시:

```json
{
  "data": null,
  "meta": {
    "timestamp": "2026-02-13T00:00:00Z"
  },
  "error": {
    "code": "BAD_REQUEST",
    "message": "..."
  }
}
```

## 인증 API

- `POST /v1/auth/login`
- `POST /v1/auth/register`
- `GET /v1/auth/me`
- `POST /v1/auth/refresh`
- `POST /v1/auth/logout`

## 포트폴리오 API

- `GET /v1/portfolios`
- `GET /v1/portfolios/{id}`
- `POST /v1/portfolios`
- `PATCH /v1/portfolios/{id}`
- `DELETE /v1/portfolios/{id}`
- `GET /v1/portfolios/{id}/targets`
- `PUT /v1/portfolios/{id}/targets?normalize={boolean}`

## 포트폴리오 그룹 API

- `GET /v1/portfolio-groups`
- `POST /v1/portfolio-groups`
- `PATCH /v1/portfolio-groups/{id}`
- `DELETE /v1/portfolio-groups/{id}`

## 금융상품 API

- `GET /v1/instruments/search?q={keyword}&assetClass={ASSET_CLASS}&page=0&size=20`
- `GET /v1/instruments/{id}`
- `GET /v1/instruments?assetClass={ASSET_CLASS}`

## 거래 API

- `GET /v1/portfolios/{portfolioId}/transactions?type={TYPE}&from={ISO_LOCAL_DATE_TIME}&to={ISO_LOCAL_DATE_TIME}`
- `POST /v1/portfolios/{portfolioId}/transactions`
- `GET /v1/transactions/{id}`
- `POST /v1/transactions/{id}/void`

`POST /v1/portfolios/{portfolioId}/transactions` 요청 예시:

```json
{
  "type": "BUY",
  "occurredAt": "2026-02-13T09:00:00",
  "note": "AAPL 매수",
  "legs": [
    {
      "legType": "ASSET",
      "instrumentId": "inst-aapl",
      "currency": "USD",
      "quantity": 10,
      "price": 200,
      "amount": 2000
    },
    {
      "legType": "CASH",
      "currency": "USD",
      "amount": -2000
    }
  ]
}
```

## 평가/성과/비교 API

- `GET /v1/portfolios/{id}/valuation?mode=REALTIME`
- `GET /v1/portfolios/{id}/performance?from=YYYY-MM-DD&to=YYYY-MM-DD&metric=TWR&frequency=DAILY`
- `POST /v1/compare/performance`
- `GET /v1/portfolios/{id}/rebalance`

## 백테스트 API

- `POST /v1/backtests/configs`
- `GET /v1/backtests/configs`
- `GET /v1/backtests/configs/{id}`
- `POST /v1/backtests/runs`
- `GET /v1/backtests/runs`
- `GET /v1/backtests/runs/{id}`
- `GET /v1/backtests/runs/{id}/results`

## 현재 기준 주의사항

- 문서 기준 집계 엔드포인트 수: 34개 (`backend/src/main/java/com/portfolio/api` 기준)
- 일부 프론트엔드 API 타입 정의는 서버 응답 스키마와 1:1 일치하지 않는 항목이 있어, 실제 통신은 런타임 데이터 기준으로 점검 필요
