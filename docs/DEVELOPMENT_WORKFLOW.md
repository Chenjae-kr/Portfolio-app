# 개발 워크플로우

최종 업데이트: 2026-02-13

## 기본 흐름

1. 작업 범위 확인 (`docs/NEXT_STEPS.md`, 이슈/요구사항)
2. 구현
3. 테스트
4. 문서 업데이트
5. 커밋

## 로컬 실행

### 인프라

```bash
cd /Users/chenjae/dev/project/Portfolio-app
docker-compose up -d
```

### 백엔드

```bash
cd /Users/chenjae/dev/project/Portfolio-app/backend
./gradlew bootRun
```

### 프론트엔드

```bash
cd /Users/chenjae/dev/project/Portfolio-app/frontend
npm install
npm run dev
```

## 테스트

### 백엔드

```bash
cd /Users/chenjae/dev/project/Portfolio-app/backend
./gradlew test
```

### 프론트엔드

```bash
cd /Users/chenjae/dev/project/Portfolio-app/frontend
npm test -- --run
```

## 문서 업데이트 규칙

아래 변경이 있으면 docs를 같이 갱신합니다.

- API 변경: `docs/API.md`
- DB 스키마 변경: `docs/DATABASE.md`
- 구현 상태 변경: `docs/PROGRESS.md`, `docs/NEXT_STEPS.md`
- 스택/설정 변경: `docs/02_TECH_STACK.md`

문서 갱신 시 반드시 포함:

- 최종 업데이트 날짜
- 실제 코드 기준 상태(완료/미완료 분리)
- 추정 표현 대신 확인 가능한 사실 우선

## 커밋 컨벤션

형식:

```text
<type>: <subject>
```

예시 type:

- `feat`
- `fix`
- `docs`
- `test`
- `refactor`
- `chore`

## PR/리뷰 체크리스트

- 변경 사항에 대한 테스트 실행 여부 기록
- API/DB 변경 시 문서 동기화 확인
- 기존 동작 회귀 여부 확인
- 하드코딩된 환경값/키 누락 여부 확인

## 현재 확인된 테스트 상태 참고

- Frontend Vitest: 4개 실패 (`src/api/instrument.spec.ts` 경로 기대값 불일치)
- Backend Test: 현재 실행 환경 Java Runtime 부재로 미실행
