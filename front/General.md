# General — 변하지 않는 규칙

이 문서는 **화면이 바뀌어도 바뀌지 않는 것**을 담는다. 프로젝트 목적, 기술 스택, 폴더 구조,
네이밍, 커밋 규칙. "무엇을 만드는가" 는 [Function.md](Function.md), "어떻게 동작하는가" 는
[Spec.md](Spec.md), "어떻게 보이는가" 는 [Style.md](Style.md) 를 읽는다.

이 넷을 만든 이유 — 한 파일(`AGENTS.md`)에 전부 담으면 규칙이 묻힌다. 이미 겪었다.
백엔드 `AGENTS.md` 가 기술 스택·마이그레이션·로깅·테스트 규칙을 다 담고 있었는데, H2 관련
서술이 낡은 채로 세 파일에 흩어져 있었고 필요한 규칙을 찾으려면 매번 `grep` 해야 했다.
그래서 주제별로 쪼갠다. **암묵지 없이, 읽는 사람(에이전트 포함)이 당연하다고 짐작하지
않아도 되게** 쓴다.

---

## 1. 프로젝트가 무엇인가

박람회 예약·티켓 판매 플랫폼. 세 역할이 쓴다.

```
MEMBER   일반 회원. 박람회를 구경하고 티켓을 산다
CLIENT   주최사(업체). 박람회를 열고, 부스·모집공고를 관리하고, 정산을 받는다
ADMIN    관리자. 심사·승인·정산 확정을 한다
```

비회원(로그인하지 않은 방문자)도 티켓을 살 수 있다 — 주문번호와 비밀번호로 나중에 조회한다.

백엔드는 완성돼 있다. Spring Boot 4 · Java 21 · PostgreSQL 18, 엔드포인트 133개.
프론트는 **뼈대만 있고 화면이 거의 없다.** 이 문서들은 그 화면을 채우는 작업의 규칙이다.

---

## 2. 기술 스택

| 용도            | 쓰는 것                           | 버전                                      |
| --------------- | --------------------------------- | ----------------------------------------- |
| 프레임워크      | Next.js (App Router)              | 16.2.12                                   |
| 런타임          | React                             | 19.2.4                                    |
| 스타일          | Tailwind CSS                      | v4 (CSS-first, `tailwind.config.js` 없음) |
| 서버 상태·캐싱  | TanStack Query                    | 5.101.4                                   |
| 클라이언트 상태 | Zustand                           | 5.0.14                                    |
| 폼              | React Hook Form + Zod             | 7.84 / 4.4                                |
| HTTP            | axios (`src/lib/api.ts` 인스턴스) | 1.19                                      |
| 결제            | Toss Payments SDK                 | 2.7.1                                     |
| QR              | qrcode.react                      | 4.2.0                                     |
| 패키지 매니저   | Bun                               | —                                         |
| Node            | **24 고정**                       | `.nvmrc`                                  |

**Node 26 을 쓰면 안 된다.** Next 16 의 `rewrites()` 가 전부 500 으로 깨져 `/api` 프록시가
통째로 죽는다(`front/AGENTS.md` 에 재현 방법이 있다).

**Tailwind v4 는 `tailwind.config.js` 를 쓰지 않는다.** `src/app/globals.css` 의 `@theme`
블록이 설정 파일을 대신한다. content 경로 지정도 필요 없다(자동 탐지). v3 튜토리얼을 그대로
따라가면 이 지점에서 어긋난다. 실제 토큰은 [Style.md](Style.md) 에 있다.

**Next 16 App Router 만 쓴다.** `react-router` 를 도입하지 않는다.

---

## 3. 폴더 구조와 모듈 규칙

```
src/
  app/                    라우트. 폴더 구조가 곧 URL 이다
    {segment}/page.tsx    화면 자체는 두지 않는다. features/ 에서 export 만 가져온다
  features/
    {module}/
      api.ts              axios 호출 함수. src/lib/api.ts 의 인스턴스를 재사용한다
      queryKeys.ts         TanStack Query 캐시 키
      hooks.ts             useQuery/useMutation 훅. api.ts + queryKeys.ts 를 조합한다
      pages/
        {Name}Page.tsx     실제 화면 컴포넌트
  components/
    {domain}/               여러 feature 가 공유하는 컴포넌트
    layout/                 공통 셸 3종 (Style.md 참고)
  lib/
    api.ts                  axios 인스턴스. 새로 만들지 않는다
    auth.ts                 토큰 저장·인증 상태 (신설 예정, Spec.md 참고)
    date.ts                 날짜 포맷 유틸
```

**라우트 파일은 한 줄로 끝난다.**

```ts
// src/app/tickets/page.tsx 의 실제 패턴
export { default } from '@/features/checkin/pages/TicketViewPage'
```

로직을 `app/` 에 두지 않는다. 라우트가 늘어도 `features/` 구조는 그대로 유지된다.

**네 파일 규칙은 예외 없이 지킨다.** `features/checkin` 이 유일한 실구현이라 그 모양을
그대로 따른다 — `api.ts`(94줄) 에 fetch 함수와 에러 변환, `hooks.ts` 에 `useQuery` 래핑,
`queryKeys.ts` 에 키 팩토리, `pages/TicketViewPage.tsx` 에 UI. 이 경계를 섞지 않는다.
컴포넌트 안에서 axios 를 직접 부르지 않는다.

### 모듈 ↔ 화면 대응

`docs/screen-api-map.md` 7절이 확정한 목록이다. 새 모듈 셋을 만든다.

```
settlement   정산 리포트(주최사) · 정산 관리(관리자)
admin        관리자 대시보드 · 카테고리 관리 · 알림 이력
```

`booth` 모듈은 배너 화면을 담지 않는다 — 배너는 이번 범위에서 **제외**다
([이슈 #103](https://github.com/expo-platform-team4/expo/issues/103)).

---

## 4. 네이밍

```
컴포넌트 파일        PascalCase.tsx           TicketViewPage.tsx
훅                  camelCase, use 접두어      useTicketView
쿼리 키 팩토리 함수   camelCase                 checkinKeys.ticketView(token)
라우트 세그먼트      kebab-case                 check-in, recruitment-notices
```

백엔드 응답 필드는 camelCase 로 온다(`Jackson` 기본값). 그대로 쓴다 — 프론트에서
snake_case 로 바꾸지 않는다.

---

## 5. 커밋·PR 규칙

**커밋 메시지는 제목 한 줄만 쓴다.** 본문·트레일러(`Co-Authored-By` 등)를 붙이지 않는다.
저장소 전체가 이 관례를 따른다 — `git log --oneline` 을 보면 확인된다.

브랜치는 `dev` 에서 딴다. PR 은 `dev` 를 대상으로 연다. 백엔드 쪽에서 이미 반복된 패턴 —
화면 단위로 나누되, **한 셸(레이아웃)이나 한 흐름(예: 체크인 3화면)은 한 PR 로 묶는다.**
너무 잘게 쪼개면 리뷰가 반쪽짜리를 보게 된다.

---

## 6. 환경변수

`.env*` 는 커밋하지 않는다. `env.sample` 만 커밋한다.

```
NEXT_PUBLIC_API_BASE_URL   브라우저용. 기본값 '/api' (rewrites 를 탄다)
BACKEND_ORIGIN              서버 컴포넌트·서버 액션용. 기본값 'http://localhost:8080'
```

서버 컴포넌트는 "현재 오리진" 이라는 개념이 없어 상대 경로(`/api/...`)로 요청할 수 없다.
그래서 실행 위치에 따라 `src/lib/api.ts` 가 baseURL 을 다르게 잡는다 — 이 분기를 건드리지
않는다.

---

## 7. 다 됐다고 말하기 전에

```bash
cd front && bun run check && bun run build
```

`check` 는 `format:check` + `lint` + `typecheck` 를 한 번에 돌린다. CI 와 같은 조합이다.

- 포맷이 어긋나면 `bun run format`
- ESLint 위반은 `bun run lint:fix` 로 일부 자동 수정
- `any` 로 타입 오류를 덮지 않는다

화면을 다 만들었다고 끝이 아니다. **[preview_tools 워크플로](../AGENTS.md)** 대로 실제
브라우저에서 열어 확인한다. 특히 인증이 걸린 화면은 로그인 상태와 비로그인 상태 둘 다 본다.
