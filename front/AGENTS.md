# front/AGENTS.md

프론트엔드 작업 규칙. 공통 규칙과 금지 사항은 [루트 AGENTS.md](../AGENTS.md) 를 먼저 읽는다.

## 화면을 구현하기 전에 — 주제별 명세 넷

이 파일 하나에 규칙을 다 담지 않는다. **디자인·기능·계약을 필요할 때 필요한 것만 읽는다.**

| 상황                                             | 읽는 문서                                                        |
| ------------------------------------------------ | ---------------------------------------------------------------- |
| 새 화면을 만들거나 라우트를 정할 때              | [Function.md](Function.md) — 화면 목록, 역할별 권한, 사용자 흐름 |
| API 를 붙이거나 인증·에러·페이지네이션을 다룰 때 | [Spec.md](Spec.md) — 요청·응답 계약, 토큰 저장, 라우트 보호 규칙 |
| 색·타이포·간격·레이아웃 셸을 쓸 때               | [Style.md](Style.md) — 디자인 토큰, 컴포넌트 규칙, 공통 셸 3종   |
| 폴더 구조·네이밍·커밋이 헷갈릴 때                | [General.md](General.md) — 이 넷 중 가장 먼저 읽어도 되는 것     |

넷 다 `docs/screen-api-map.md`(저장소 루트)의 결정을 근거로 한다. 그 문서의 7절이
"무엇을 만들지 말지" 를 확정한 원본이고, 이 넷은 그걸 구현 가능한 형태로 풀어 쓴 것이다.
Stitch 디자인 원본은 `projects/9433856227347358698`(MCP `mcp__stitch__*` 도구로 조회)다.

## Node 24 로 고정이다

**Node 26 을 쓰지 않는다.** Next 16 의 `rewrites()` 가 전부 500 으로 깨진다.
내부·외부 rewrite 구분 없이, dev·production 구분 없이 아래가 뜬다.

```txt
ERR_INVALID_ARG_TYPE: The "path" argument must be of type string. Received null
```

`/api` 프록시가 rewrites 로 동작하므로 **백엔드 호출이 전부 실패한다.**
`.nvmrc` 에 `24` 가 박혀 있다. 패키지 매니저는 Bun 이다.

## 검색 결과가 통하지 않는 지점

| 하려는 것     | 흔한 예제 (틀림)          | 여기서 (맞음)                         |
| ------------- | ------------------------- | ------------------------------------- |
| 라우팅        | `react-router` 도입       | **App Router 만 쓴다**                |
| Tailwind 설정 | `tailwind.config.js` 생성 | **`src/app/globals.css` 의 `@theme`** |

Tailwind v4 는 `tailwind.config.js` 를 쓰지 않는다. content 경로 지정도 필요 없다(자동 탐지).
v3 튜토리얼을 그대로 따라가면 안 된다.

## 무엇을 쓰는가

| 용도            | 쓰는 것                                             |
| --------------- | --------------------------------------------------- |
| 서버 상태·캐싱  | TanStack Query                                      |
| 클라이언트 상태 | Zustand                                             |
| 폼              | React Hook Form + Zod (`@hookform/resolvers`)       |
| HTTP            | **`src/lib/api.ts` 의 axios 인스턴스를 재사용한다** |

**API 호출을 새로 만들지 않는다.** `src/lib/api.ts` 에 baseURL·인터셉터가 이미 설정돼 있다.
`fetch` 를 직접 쓰거나 axios 인스턴스를 새로 만들면 인증 헤더와 에러 처리가 빠진다.

## 환경변수

`.env*` 는 커밋하지 않는다. `env.sample` 만 커밋한다.
브라우저에 노출되는 값과 서버 전용 값이 나뉘어 있으니, 새 변수를 추가하기 전에
[README](../README.md) 의 "프론트 환경변수 — 두 개로 나뉜 이유" 절을 읽는다.

## 다 됐다고 말하기 전에

```bash
cd front && bun run check && bun run build
```

`check` 는 `format:check` + `lint` + `typecheck` 를 한 번에 돌린다. CI 와 같은 조합이다.

- 포맷이 어긋나면 `bun run format` 으로 고친다
- ESLint 위반은 `bun run lint:fix` 로 일부 자동 수정된다
- `any` 로 타입 오류를 덮지 않는다
