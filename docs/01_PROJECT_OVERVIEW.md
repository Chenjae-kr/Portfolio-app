# 프로젝트 개요

최종 업데이트: 2026-02-13

## 목표

Portfolio Manager App은 다중 포트폴리오를 생성/관리하고 아래 기능을 제공합니다.

- 거래 원장 기반 포지션/평가
- 포트폴리오 성과 분석
- 포트폴리오 간 비교
- 전략 백테스트
- 리밸런싱 분석

## 현재 제공 범위

### 백엔드

- 인증: 회원가입/로그인/내정보/토큰 갱신/로그아웃
- 포트폴리오: CRUD + 목표비중(Targets)
- 포트폴리오 그룹: CRUD
- 금융상품: 검색/목록/상세
- 거래: 생성/목록/상세/VOID
- 평가/성과: valuation/performance
- 비교: 다중 포트폴리오 성과 비교
- 백테스트: config/run/result 조회
- 리밸런싱: 편차 분석 및 추천

### 프론트엔드

- 인증 화면 (`/login`, `/register`)
- 대시보드 (`/dashboard`)
- 포트폴리오 생성/상세 (`/portfolio/new`, `/portfolio/:id`)
- 비교 (`/compare`)
- 백테스트 (`/backtest`, `/backtest/history`, `/backtest/:id`)

## 아키텍처

```text
Vue 3 SPA
  -> REST API (/api/v1)
Spring Boot (domain services)
  -> PostgreSQL (source of truth)
  -> Redis (cache)
  -> RabbitMQ (async infra, 일부 미활용)
```

## 핵심 설계 원칙

- 거래 원장(transaction + legs)을 단일 진실 공급원으로 사용
- 평가/성과는 거래와 가격 데이터를 기반으로 계산
- JWT 기반 stateless 인증
- API 응답은 `data`, `meta`, `error` 공통 래퍼 사용

## MVP 기준 완료도

- 인증/포트폴리오/거래/평가/성과/비교/백테스트/리밸런싱: 구현됨
- 외부 실시간 가격 연동, 백테스트 비동기 큐 처리, 고급 리포팅: 미완료

## 관련 문서

- [기술 스택](./02_TECH_STACK.md)
- [API 레퍼런스](./API.md)
- [데이터베이스 설계](./DATABASE.md)
- [진척도](./PROGRESS.md)
