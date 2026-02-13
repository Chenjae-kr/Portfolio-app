# 개발 진척도

최종 업데이트: 2026-02-13

## 전체 요약

- 현재 버전: `v0.0.1-SNAPSHOT`
- 백엔드 API: 컨트롤러 기준 34개 엔드포인트 구현
- DB: Flyway V1 기준 24개 테이블 정의
- 프론트엔드: 인증/대시보드/포트폴리오/비교/백테스트 주요 화면 구현

## 기능별 상태

| 영역 | 상태 | 비고 |
|---|---|---|
| 인증 (JWT) | 완료 | login/register/me/refresh/logout |
| 포트폴리오 CRUD | 완료 | targets 포함 |
| 포트폴리오 그룹 | 완료 | CRUD |
| 금융상품 조회 | 완료 | 검색/목록/상세 |
| 거래 원장 | 완료 | 생성/목록/상세/void |
| 평가 엔진 | 완료 | valuation API |
| 성과 분석 | 완료 | performance API |
| 포트폴리오 비교 | 완료 | compare/performance |
| 백테스트 | 완료(기본) | config/run/result API |
| 리밸런싱 분석 | 완료 | portfolio rebalance API |
| 외부 가격 연동 | 부분 | Mock + ExternalPriceService 구조 |
| Redis 캐시 실사용 | 부분 | 설정/의존성은 존재, 도메인별 적용 확장 필요 |
| RabbitMQ 비동기 백테스트 | 미완료 | infra 존재, 실제 백테스트 처리 연동 필요 |

## 코드 규모 지표(파일 수)

- `backend/src/main/java`: 55 files
- `backend/src/test/java`: 10 files
- `frontend/src`: 52 files
- `frontend` 테스트 파일(`*.spec.*`): 5 files

## 테스트 상태

### Frontend (실행됨)

- 명령: `npm test -- --run`
- 결과: 28 tests 중 24 passed, 4 failed
- 실패 파일: `frontend/src/api/instrument.spec.ts`
- 실패 원인: 테스트 기대 URL(`/instruments/...`)과 실제 코드 URL(`/v1/instruments/...`) 불일치

### Backend (미실행)

- 명령: `./gradlew test`
- 상태: 실행 환경 Java Runtime 부재로 실행 불가

## 최근 반영 사항

- 문서와 실제 코드 불일치 항목 정리
- API 엔드포인트/스키마/인프라 구성 최신화
- 구형 링크 및 존재하지 않는 문서 참조 제거
