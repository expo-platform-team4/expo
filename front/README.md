# Expo Frontend

박람회 예약·티켓 판매 플랫폼의 프론트엔드입니다. Next.js(App Router)로 만들었고,
백엔드(Spring Boot)와 한 저장소 안에서 같이 관리됩니다(모노레포).

이 문서는 "처음 이 프로젝트를 열었을 때 뭘 어떻게 하면 되는지"를 안내합니다.
순서대로 따라오시면 됩니다.

---

## 1. 처음 시작할 때 (한 번만 하면 됨)

### 1-1. Node·Bun 버전 맞추기

이 프로젝트는 **Node 24 LTS**, **Bun**을 씁니다. Node 26에서는 Next 16의 `rewrites()`가
깨져서 `/api` 프록시가 전부 실패합니다. 반드시 24를 씁니다.

```bash
nvm use   # .nvmrc 보고 24로 자동 전환
bun install
```

nvm이 없으면 [nvm 설치](https://github.com/nvm-sh/nvm) 하거나 직접 Node 24를 설치합니다.

### 1-2. 환경변수 파일 만들기

```bash
cp env.sample .env.local
```

`.env.local`은 커밋 금지 파일입니다(`env.sample`만 커밋).

---

## 2. 개발할 때마다 (매번 해야 함)

프론트만 켜서는 실제 데이터가 안 뜹니다. **백엔드도 같이 켜져 있어야** 합니다.

1. **백엔드 실행** — `backend/`에서 `./gradlew bootRun --args='--spring.profiles.active=local'` (포트 8080)
2. **DB/S3 실행** — 저장소 루트에서 `docker compose up -d`
3. **프론트 실행** — `front/`에서:

```bash
bun run dev
```

`http://localhost:3000` 열면 됩니다. `/api/...` 요청은 `next.config.ts`의 rewrites가
백엔드(8080)로 그대로 넘겨줍니다. 백엔드가 꺼져 있으면 이 프록시가 실패합니다.

---

## 3. 폴더 구조 — 어디에 뭘 만들어야 하나요

가장 중요한 규칙 한 줄: **"페이지 껍데기는 `app/`에, 실제 코드는 `features/`에"**

```text
src/
├── app/            # 라우팅 전용. 각 page.tsx는 한 줄짜리 "연결" 파일
├── features/       # 도메인별 진짜 코드가 여기 다 있음 (여러분이 작업할 곳!)
│   └── <도메인>/
│       ├── api.ts        # 백엔드 API 호출 함수들
│       ├── queryKeys.ts  # React Query 캐시 키
│       ├── hooks.ts      # api.ts를 React Query로 감싼 훅
│       └── pages/
│           └── XxxPage.tsx  # 실제 화면 컴포넌트
├── components/
│   ├── ui/         # 여러 페이지에서 재사용하는 공통 부품 (버튼, 로딩 등)
│   ├── layout/     # 헤더·사이드바 등 레이아웃
│   └── auth/       # RequireAuth 등 인증 가드
└── lib/            # axios 설정, 상수, 날짜 유틸 등 전역 설정
```

`app/expos/page.tsx` 파일을 열어보면 내용이 한 줄입니다:

```tsx
export { default } from '@/features/expo/pages/ExpoListPage'
```

"이 주소(`/expos`)로 오면 `features/expo/pages/ExpoListPage`를 보여줘라"는 뜻입니다.
**실제 화면을 만들 때는 `app/` 폴더는 건드릴 일이 거의 없고, `features/도메인이름/` 폴더
안에서만 작업하면 됩니다.**

이렇게 나눈 이유: Next.js 라우팅 규칙(폴더 구조 = URL 주소)과 실제 로직을 분리해서,
나중에 URL 구조가 바뀌어도 실제 코드(`features/`)는 그대로 두고 `app/`의 연결 파일만
고치면 되게 하기 위해서입니다. ([DeunDeunn/Proovy](https://github.com/DeunDeunn/Proovy)
프론트 구조를 참고했습니다.)

### 3-1. 도메인 담당 매핑

| 도메인 폴더   | 담당 범위                              | 담당자 |
| ------------- | -------------------------------------- | ------ |
| `auth`        | 인증, 일반 회원 마이페이지             | A      |
| `expo`        | 박람회 관리, 검색·화면 노출, 광고 배너 | B      |
| `ticket`      | 티켓·재고, 주문·결제                   | C      |
| `checkin`     | 발권·QR 체크인, 카카오 알림, 정산      | D      |
| `client`      | 클라이언트 마이페이지, 관리자 대시보드 | E      |
| `recruitment` | 기업 모집                              | F      |
| `venue`       | 가상 장소                              | F      |
| `booth`       | 부스 판매·배정                         | F      |

`venue`는 아직 대표 예시 페이지가 없습니다(전부 관리자 전용 화면이라 admin 작업 재개할 때
같이 채우면 됩니다).

### 3-2. 지금 있는 예시, 그대로 안 써도 됩니다

각 도메인 `pages/`에 이미 파일이 하나씩 있는데(`LoginPage.tsx`, `ExpoListPage.tsx`
같은), 이건 **"이런 식으로 구조 잡으면 된다"는 예시**일 뿐입니다. 이름도 내용도
실제 화면 기획에 맞게 자유롭게 바꾸거나 지우고 새로 만드셔도 됩니다. 다만 `app/`의
위임 파일과 이름이 안 맞게 되면 같이 고쳐주세요.

---

## 4. 인증이 필요한 화면 — `layout.tsx` + `RequireAuth`

`app/mypage/layout.tsx`, `app/client/layout.tsx`처럼 로그인이 필요한 섹션은
`components/auth/RequireAuth.tsx`로 감싸져 있습니다.

```tsx
import RequireAuth from '@/components/auth/RequireAuth'

const MyPageLayout = ({ children }: { children: React.ReactNode }) => (
  <RequireAuth>{children}</RequireAuth>
)

export default MyPageLayout
```

**지금 `RequireAuth`는 로그인 여부를 실제로 확인하지 않고 그냥 통과시키는
placeholder입니다.** 로그인 상태 확인 로직(`useMe` 같은 훅으로 401 체크)은
`auth` 담당자가 붙여야 합니다. 관리자 전용 화면이 생기면 같은 패턴으로
`RequireAdmin.tsx`를 추가하면 됩니다.

---

## 5. API 연동하는 법

`features/도메인/api.ts` → `queryKeys.ts` → `hooks.ts` → `pages/XxxPage.tsx` 순서로
채우면 됩니다.

### 5-1. `api.ts` — 백엔드에 요청 보내는 함수

```ts
import { api } from '@/lib/api'

export const getExpos = (params: ExpoSearchParams) =>
  api.get('/expos', { params }).then((res) => res.data)
```

`api`는 `lib/api.ts`에 설정된 axios 인스턴스입니다. **새로 axios 인스턴스를 만들거나
fetch를 직접 쓰지 마세요** — 인증 헤더·에러 처리가 빠집니다.

### 5-2. `queryKeys.ts` — 캐시 이름표 관리

```ts
export const expoKeys = {
  all: ['expos'] as const,
  lists: () => [...expoKeys.all, 'list'] as const,
  list: (params: ExpoSearchParams) => [...expoKeys.lists(), params] as const,
}
```

### 5-3. `hooks.ts` — 화면에서 실제로 쓰는 훅

```ts
'use client'

import { useQuery } from '@tanstack/react-query'
import { getExpos } from './api'
import { expoKeys } from './queryKeys'

export const useExpos = (params: ExpoSearchParams) =>
  useQuery({ queryKey: expoKeys.list(params), queryFn: () => getExpos(params) })
```

### 5-4. 페이지 컴포넌트에서 사용

```tsx
'use client'

import { useExpos } from '../hooks'

const ExpoListPage = () => {
  const { data, isLoading, error } = useExpos({})

  if (isLoading) return <div>로딩 중...</div>
  if (error) return <div>에러가 발생했습니다.</div>

  return <div>{/* data로 화면 그리기 */}</div>
}

export default ExpoListPage
```

---

## 6. 코드 작성 규칙

- 함수형 컴포넌트는 `function` 대신 `const XXX = () => {}` 화살표 함수로 씁니다.
- 저장하면 Prettier가 자동으로 정리합니다 (VSCode 확장 설치 필요, 아래 참고).
- 커밋 전에 아래가 통과하는지 확인합니다:

```bash
bun run format   # Prettier 자동 정리
bun run check     # format:check + lint + typecheck 한 번에
bun run build
```

자세한 컨벤션(Tailwind v4, `src/lib/api.ts` 재사용 등)은 [`AGENTS.md`](AGENTS.md)를,
CI·PR 규칙은 저장소 루트 [`CONTRIBUTING.md`](../CONTRIBUTING.md)를 참고하세요.

VSCode로 `front/` 폴더만 열어서 작업하는 경우, `.vscode/extensions.json`에 있는
권장 확장(EditorConfig, Prettier, ESLint)을 설치해야 저장 시 자동 포맷이 걸립니다.

---

## 7. 막힐 때

- `/api/...` 요청이 계속 실패한다 → 백엔드(8080)가 켜져 있는지, Docker(Postgres/S3)가
  떠 있는지 확인
- 화면에 아무것도 안 뜬다 → 브라우저 개발자도구(F12) 콘솔 확인
- 새 라우트 만들었는데 404가 뜬다 → 개발 서버 재시작(`Ctrl+C` → `bun run dev`).
  라우트 폴더가 한꺼번에 많이 생기면 Turbopack이 못 따라갈 때가 있습니다.
- 어떻게 짜야 할지 감이 안 잡힌다 → 다른 팀원 도메인 예시 코드나
  [Proovy](https://github.com/DeunDeunn/Proovy) 프론트를 참고하세요.
- 그래도 모르겠다 → 팀 채널에 편하게 물어보세요.
