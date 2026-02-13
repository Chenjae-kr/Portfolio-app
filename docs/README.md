# Portfolio Manager 문서 인덱스

최종 업데이트: 2026-02-13

## 문서 목록

- [프로젝트 개요](./01_PROJECT_OVERVIEW.md)
- [기술 스택](./02_TECH_STACK.md)
- [진척도](./PROGRESS.md)
- [API 레퍼런스](./API.md)
- [데이터베이스 설계](./DATABASE.md)
- [개발 워크플로우](./DEVELOPMENT_WORKFLOW.md)
- [다음 단계](./NEXT_STEPS.md)

## 권장 읽기 순서

1. `01_PROJECT_OVERVIEW.md`
2. `02_TECH_STACK.md`
3. `DEVELOPMENT_WORKFLOW.md`
4. `API.md`
5. `DATABASE.md`
6. `PROGRESS.md`
7. `NEXT_STEPS.md`

## 현재 상태 요약

- 백엔드: Spring Boot 3.3.0 + Java 21
- 프론트엔드: Vue 3.5 + TypeScript + Vite 7
- DB 스키마: Flyway `V1__init_schema.sql` 기준 24개 테이블
- API: `/api/v1` 하위 컨트롤러 기준 34개 엔드포인트
- 테스트 실행 상태 (로컬 확인 기준)
  - Frontend Vitest: 28개 중 24개 통과, 4개 실패
  - Backend Gradle Test: 실행 환경에 Java Runtime이 없어 미실행

## 주의 사항

- 본 문서는 코드 기준으로 정리되어 있으며, 계획성 문구보다 "현재 구현 상태"를 우선합니다.
- 상세 명세가 필요한 경우 `backend/src/main/java/com/portfolio/api`와 `backend/src/main/resources/db/migration/V1__init_schema.sql`을 기준으로 확인합니다.
