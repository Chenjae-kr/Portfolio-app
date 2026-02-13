# 기술 스택

최종 업데이트: 2026-02-13

## Backend

- Java: 21
- Spring Boot: 3.3.0
- Build: Gradle
- 주요 의존성
  - `spring-boot-starter-web`
  - `spring-boot-starter-security`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-data-redis`
  - `spring-boot-starter-amqp`
  - `spring-boot-starter-jooq`
  - `flyway-core`, `flyway-database-postgresql`
  - `jjwt 0.12.5`
  - `springdoc-openapi-starter-webmvc-ui 2.5.0`
  - `resilience4j-spring-boot3 2.2.0`
  - `micrometer-registry-prometheus`

## Frontend

- Vue: 3.5.24
- TypeScript: 5.9.x
- Vite: 7.2.x
- Pinia: 3.0.x
- Vue Router: 4.6.x
- vue-i18n: 9.14.x
- Axios: 1.13.x
- ECharts: 6.0.0
- Test: Vitest 4.x + Vue Test Utils

## Database / Infra

- PostgreSQL 16 (`postgres:16-alpine`)
- Redis 7 (`redis:7-alpine`)
- RabbitMQ 3 Management (`rabbitmq:3-management-alpine`)
- Docker Compose 로컬 인프라 구성

## 실행 프로파일

### `application.yml` (기본)

- PostgreSQL + Flyway 활성화
- Redis/RabbitMQ 연결 설정 활성화
- context-path: `/api`
- Swagger UI: `/api/swagger-ui.html`

### `application-dev.yml`

- H2 in-memory 사용
- Redis/RabbitMQ/Cache/JOOQ AutoConfig 비활성화
- Flyway 비활성화

### `application-local.yml`

- 로컬 PostgreSQL/Redis/RabbitMQ 직접 연결
- JPA `ddl-auto: update`

## 보안 설정

- JWT 필터 기반 인증
- 공개 엔드포인트
  - `/v1/auth/login`
  - `/v1/auth/register`
  - `/v1/auth/refresh`
  - Swagger/Actuator
- CSRF 비활성화
- CORS: localhost/127.0.0.1 허용 패턴

## 모듈 구성 (백엔드)

- `api`: REST controller
- `auth`: JWT/보안/유저 인증
- `portfolio`: 포트폴리오/그룹/타겟
- `ledger`: 거래/거래레그
- `pricing`: 종목/가격 서비스
- `valuation`: 평가 계산
- `analytics`: 성과 분석
- `backtest`: 백테스트 실행
- `rebalance`: 리밸런싱 분석
- `infra`: 초기 데이터/인프라 설정

## 기술 부채

- Redis/RabbitMQ 의존성은 포함되어 있으나 일부 도메인에서 실사용 연결이 제한적
- 개발 프로파일(H2)과 운영 스키마(PostgreSQL/Flyway) 간 동작 차이를 주기적으로 검증 필요
