# Spec — 어떻게 동작하는가

API 계약, 인증, 에러 처리, 라우팅 규칙. **암묵지를 남기지 않는 것이 이 문서의 목적이다** —
백엔드를 만든 사람과 프론트를 만드는 사람(또는 세션)이 다를 수 있으므로, "당연히 이렇게
하겠지" 로 남겨 둔 것들을 전부 여기 적는다.

---

## 1. 응답 봉투 — 모든 API 가 같은 모양이다

```json
성공  { "success": true,  "data": { ... }, "message": null }
실패  { "success": false, "data": null,    "message": "이미 사용 중인 이메일입니다." }
```

`data` 가 `null` 이면 그 필드는 응답 JSON 에서 **아예 빠진다**(`success`/`message` 만 온다).
`data === undefined` 체크가 아니라 **`'data' in response` 체크가 필요할 수 있다**는 뜻이다.

**`message` 는 이미 사용자에게 보여줄 수 있는 한국어 문장이다.** 백엔드 `ErrorCode` 가
그렇게 설계돼 있다. 실패 시 이 문자열을 그대로 토스트·인라인 에러에 쓴다 — 프론트에서
에러 코드를 다시 한국어로 매핑하지 않는다.

```ts
type ApiResponse<T> = {
  success: boolean
  data?: T
  message?: string
}
```

---

## 2. 페이지네이션 — 응답 모양이 통일돼 있다

목록 API(정산·알림 이력·감사 로그 등)는 전부 같은 봉투를 쓴다.

```json
{
  "totalCount": 42,
  "page": 0,
  "size": 20,
  "items": [ ... ]
}
```

`page` 는 **0부터**다. 요청 파라미터도 `page`(기본 0)·`size`(기본 20, 서버가 최대 100 으로
자른다) 그대로다. 프론트에서 1부터 세는 UI 를 만들려면 화면 레이어에서만 +1 표시하고,
API 호출 시점에는 다시 -1 한다 — 서버로 넘기는 값은 항상 0-based.

---

## 3. 인증 — 토큰 저장과 재발급

### 3-1. 로그인

```
POST /api/auth/login
Body: { "email": string, "password": string }
Response.data: {
  accessToken: string
  accessTokenExpiresInMinutes: number
  refreshToken: string
  refreshTokenExpiresAt: string (ISO Instant)
}
```

### 3-2. 저장 방식 — Zustand persist (localStorage)

**결정.** 액세스·리프레시 토큰을 Zustand store 에 두고 `persist` 미들웨어로 localStorage 에
저장한다. `src/lib/auth.ts` 에 둔다(신설).

```ts
// src/lib/auth.ts 의 모양
type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  role: 'MEMBER' | 'CLIENT' | 'ADMIN' | null
  setTokens: (accessToken: string, refreshToken: string, role: Role) => void
  clear: () => void
}
```

역할(`role`)은 JWT 를 프론트에서 디코드하지 않는다 — **로그인 응답에 없으므로, 로그인 성공
직후 `/api/users/me` 또는 동등한 프로필 조회로 받아 함께 저장한다.** (프로필 조회 엔드포인트가
아직 없다면 이슈로 요청한다 — Function.md 7절 원칙.)

### 3-3. axios 인터셉터 — `src/lib/api.ts` 에 추가한다

지금 `api.ts` 에는 인터셉터가 없다(TODO 로 남겨져 있다). 이렇게 채운다.

```
요청 인터셉터   accessToken 이 있으면 Authorization: Bearer {accessToken} 을 붙인다

응답 인터셉터   401 을 받으면:
  1. refreshToken 으로 POST /api/auth/reissue 를 부른다
  2. 성공하면 새 accessToken 을 저장하고 원래 요청을 한 번만 재시도한다
  3. reissue 자체가 실패하면 store 를 비우고 /login 으로 보낸다
  4. 재시도한 요청이 또 401 이면 더 반복하지 않는다 (무한 루프 방지)
```

```
POST /api/auth/reissue
Body: { "refreshToken": string }
Response.data: { accessToken, accessTokenExpiresInMinutes, ... }
```

### 3-4. 로그아웃

```
POST /api/auth/logout       Authorization 헤더 필요. 서버가 refresh_tokens 를 전부 폐기한다
```

**액세스 토큰은 stateless 라 서버가 즉시 무효화하지 않는다.** 로그아웃 시 클라이언트가
반드시 store 를 비워야 한다 — API 호출만 하고 로컬 상태를 안 지우면 로그아웃이 안 된 것처럼
보인다.

---

## 4. 인가 — 라우트 보호 규칙

백엔드 권한 경계와 프론트 라우트 보호가 같은 모양이어야 한다.

```
/api/auth/**       인증 불필요                    → 공개 라우트
/api/public/**     인증 불필요                    → 공개 라우트
/api/member/**     MEMBER 만                      → RequireAuth(role: 'MEMBER')
/api/client/**     CLIENT 또는 ADMIN               → RequireAuth(role: ['CLIENT','ADMIN'])
/api/admin/**      ADMIN 만                        → RequireAuth(role: 'ADMIN')
/api/users/**      MEMBER 또는 CLIENT              → RequireAuth(role: ['MEMBER','CLIENT'])
```

`src/components/auth/RequireAuth.tsx` 는 현재 아무 검사도 안 하는 스텁이다(그대로 통과).
이걸 채운다.

```
토큰 없음           → /login?redirect={현재 경로} 로 이동
토큰은 있는데 역할 불일치 → 403 화면 또는 자기 역할의 홈으로 이동 (화면 설계 시 정할 것)
```

로그인 성공 후에는 `redirect` 쿼리가 있으면 그 경로로, 없으면 역할별 기본 홈으로 보낸다.

```
MEMBER  →  /
CLIENT  →  /client/dashboard
ADMIN   →  /admin
```

---

## 5. 에러 처리 — HTTP 상태별로 다르게 다룬다

백엔드가 실제로 쓰는 상태 코드는 이 일곱이다. 전부 `{ success: false, message }` 봉투로 온다.

| 상태                                                                                      | 의미                                    | 화면에서                                                                                      |
| ----------------------------------------------------------------------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------- |
| 400                                                                                       | 입력값 오류, 검증 실패                  | 폼 인라인 에러 (message 를 그대로)                                                            |
| 401                                                                                       | 인증 필요·토큰 만료                     | 3-3 절 인터셉터가 처리. 화면 레벨에서 따로 잡지 않는다                                        |
| 403                                                                                       | 권한 없음                               | 전용 안내 화면 또는 토스트 + 이전 화면으로                                                    |
| 404                                                                                       | 대상 없음                               | "존재하지 않습니다" 안내. **정산 API 는 남의 것도 404 로 준다** (403 이 아니다 —              |
| 존재 여부를 숨기려는 의도된 설계다. 프론트가 이걸 "권한 없음" 으로 바꿔 보여주면 안 된다) |
| 409                                                                                       | 상태 충돌 (예: 이미 확정된 정산 재계산) | 토스트로 message 노출. 재시도 버튼을 달 이유가 없다 — 같은 요청을 다시 보내도 똑같이 실패한다 |
| 410                                                                                       | 링크 만료 (QR 접근 토큰)                | 재발급 안내 문구 (Function.md 의 `/tickets` 화면 참고)                                        |
| 500/502                                                                                   | 서버·PG 오류                            | 일반 오류 화면. 재시도 버튼 유효                                                              |

**TanStack Query 에서 재시도 정책.** `providers.tsx` 가 이미 전역으로 `retry: 1` 을 걸어
뒀다. 401/403/404/409 처럼 **다시 불러도 결과가 같은 에러**는 컴포넌트 단에서
`retry: false` 로 덮어써야 한다 — 그렇지 않으면 실패가 뻔한 요청을 자동으로 한 번 더 보낸다
(체크인의 `useTicketView` 가 이미 이 패턴이다. `Reference: front/src/features/checkin/hooks.ts`).

---

## 6. TanStack Query 관례

```
staleTime            60 * 1000 (기본, providers.tsx)
refetchOnWindowFocus  false
쿼리 키               {module}Keys.{자원}(...params) 형태의 팩토리 함수. 문자열 나열 금지
```

목록·상세를 나누는 키는 이렇게 짓는다 (`checkinKeys` 패턴 재사용).

```ts
export const settlementKeys = {
  all: ['settlement'] as const,
  list: (page: number) => [...settlementKeys.all, 'list', page] as const,
  detail: (id: number) => [...settlementKeys.all, 'detail', id] as const,
}
```

**뮤테이션 후 무효화.** 정산 재계산·확정·송금 기록처럼 서버 상태를 바꾸는 액션은 성공 시
관련 쿼리를 `invalidateQueries({ queryKey: settlementKeys.all })` 로 무효화한다. 낙관적
업데이트는 기본으로 쓰지 않는다 — 정산·체크인처럼 상태 전이가 서버에서 엄격히 막히는
도메인이 많아, 성공 여부를 서버 응답으로 확정한 뒤 반영하는 편이 안전하다.

---

## 7. 폼 검증

React Hook Form + Zod. **백엔드 검증 메시지와 프론트 Zod 메시지가 다를 수 있다** — 서버가
최종 권위다. Zod 는 "제출 전에 빤히 보이는 실수를 막는 것" 이 목적이고, 서버가 400 을 주면
그 `message` 로 덮어써서 보여준다(Zod 메시지를 우선하지 않는다).

예: 회원가입의 비밀번호 규칙(영문·숫자·특수문자 조합 8자 이상)은 백엔드 `SignupRequest` 의
`@Pattern` 과 **정확히 같은 정규식**을 Zod 스키마에도 넣는다. 규칙이 두 곳에 있으면
어긋나는 날이 온다 — 백엔드 소스(`SignupRequest.java`)를 확인하고 그대로 옮긴다.

---

## 8. 실시간성이 필요한 화면

체크인 현황(`/client/check-in`)은 스캔 직후 숫자가 바뀌어야 자연스럽다. **폴링으로
충분하다** — WebSocket 을 새로 얹지 않는다. `useQuery` 의 `refetchInterval` 을 현황
화면에서만 짧게(예: 5초) 건다. 스캔 화면(`/scan`) 자체는 폴링하지 않는다 — 스캔 성공
뮤테이션이 끝나면 현황 쿼리를 무효화하는 것으로 충분하다.

---

## 9. 날짜·시간

백엔드는 `Instant`(UTC, ISO-8601)로 준다. `src/lib/date.ts`(현재 스텁)에 포맷 함수를 채운다.
브라우저 로컬 타임존으로 표시한다 — 서버 타임존을 가정하지 않는다.

```ts
new Date(isoInstant).toLocaleString('ko-KR', { ... })
```

`checkin` 모듈의 `TicketViewPage.tsx` 가 이미 이 패턴을 쓰고 있다. 참고한다.

---

## 다음

화면 목록은 [Function.md](Function.md), 디자인 토큰·컴포넌트·레이아웃 셸은
[Style.md](Style.md) 를 본다.
