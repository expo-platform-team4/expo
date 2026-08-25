import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/** 백엔드 Role enum 과 값이 같아야 한다 (com.expo.auth.Role). */
export type Role = 'MEMBER' | 'CLIENT' | 'ADMIN'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  role: Role | null
  nickname: string | null
  /** localStorage 에서 rehydrate 가 끝났는지. `RequireAuth` 가 이 값으로 첫 틱의 오판정을 막는다. */
  hasHydrated: boolean
  setTokens: (accessToken: string, refreshToken: string, role: Role, nickname: string) => void
  setAccessToken: (accessToken: string) => void
  clear: () => void
  setHasHydrated: (hasHydrated: boolean) => void
}

/**
 * 인증 상태. Spec.md 3-2 절의 결정 — Zustand persist(localStorage).
 *
 * 역할(role)은 JWT 를 프론트에서 디코드하지 않는다. 로그인 응답에 없으므로
 * 로그인 성공 직후 별도로 받아 함께 저장한다 (features/auth/api.ts 참고).
 *
 * `hasHydrated` 를 컴포넌트의 `useEffect(() => setState(true), [])` 로 흉내 내지 않는다 —
 * 그 패턴은 렌더 중 setState 를 한 번 더 유발해 `react-hooks/set-state-in-effect` 에 걸린다.
 * `onRehydrateStorage` 는 persist 미들웨어가 rehydrate 를 끝낸 시점에 콜백하는 훅이라
 * 같은 정보를 부작용 없이 얻는다.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      role: null,
      nickname: null,
      hasHydrated: false,
      setTokens: (accessToken, refreshToken, role, nickname) =>
        set({ accessToken, refreshToken, role, nickname }),
      setAccessToken: (accessToken) => set({ accessToken }),
      clear: () => set({ accessToken: null, refreshToken: null, role: null, nickname: null }),
      setHasHydrated: (hasHydrated) => set({ hasHydrated }),
    }),
    {
      name: 'expo-auth',
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true)
      },
    }
  )
)

/** 역할별 기본 홈. Spec.md 4절. */
export const homeRouteFor = (role: Role): string => {
  switch (role) {
    case 'CLIENT':
      return '/client'
    case 'ADMIN':
      return '/admin'
    case 'MEMBER':
    default:
      return '/'
  }
}

/**
 * 로그인 직후 보낼 경로. `redirect` 쿼리가 있어도 **역할의 자기 구역이 아니면 무시한다.**
 *
 * 이걸 두는 이유 — 예전에는 `redirectTo || homeRouteFor(role)` 이었는데, 이러면
 * **관리자가 로그인했는데 클라이언트 대시보드로 들어갔다.**
 *
 * ```
 * 1. 로그아웃 상태로 /client/dashboard 접근
 * 2. RequireAuth 가 /login?redirect=%2Fclient%2Fdashboard 로 보낸다
 * 3. 관리자로 로그인 → redirect 가 우선이라 /client/dashboard 로 간다
 * 4. /client 레이아웃이 ADMIN 도 허용하므로 튕기지도 않는다   ← 그대로 눌러앉는다
 * ```
 *
 * 4번이 이 버그가 조용한 이유다. 권한 오류가 아니라 **의도한 화면이 아닐 뿐**이라
 * 아무 경고도 안 뜬다.
 *
 * 그래서 `redirect` 를 "접근 가능한가" 가 아니라 **"그 역할의 구역인가"** 로 판단한다.
 * 관리자는 `/client/**` 에 들어갈 수 있지만 로그인 착지점은 아니다.
 *
 * ```
 * ADMIN   /admin/settlements 로 딥링크 → 그대로 간다
 *         /client/dashboard 로 딥링크 → /admin 으로 간다
 * CLIENT  /client/expos 로 딥링크     → 그대로 간다
 * MEMBER  /mypage/tickets 로 딥링크   → 그대로 간다
 * ```
 */
export const resolvePostLoginRoute = (role: Role, redirectTo: string | null): string => {
  if (!redirectTo || !isOwnAreaOf(role, redirectTo)) {
    return homeRouteFor(role)
  }
  return redirectTo
}

/**
 * 그 경로가 이 역할의 <b>자기 구역</b>인가.
 *
 * "들어갈 수 있는가"(`RequireAuth` 의 `roles`)와 다르다. 관리자는 `/client/**` 에 들어갈 수
 * 있지만 그곳이 관리자의 구역은 아니다.
 *
 * 외부에서 온 값을 경로로 쓰므로 `//evil.com` 같은 스킴 없는 절대 URL 도 걸러야 한다 —
 * `/` 로 시작하고 `//` 로 시작하지 않는 것만 통과시킨다.
 */
const isOwnAreaOf = (role: Role, path: string): boolean => {
  if (!path.startsWith('/') || path.startsWith('//')) {
    return false
  }
  const isAdminArea = path.startsWith('/admin')
  const isClientArea = path.startsWith('/client')

  switch (role) {
    case 'ADMIN':
      return isAdminArea
    case 'CLIENT':
      return isClientArea
    case 'MEMBER':
    default:
      // 회원은 전용 구역이 좁고(/mypage) 공개 화면도 착지점이 될 수 있다.
      // 남의 구역만 아니면 된다.
      return !isAdminArea && !isClientArea
  }
}

/**
 * 헤더의 "Profile" 클릭 시 이동할 경로. `homeRouteFor` 와 다른 이유 —
 * MEMBER 는 역할 홈이 `/`(공개 홈)이라 프로필 진입점이 따로 필요하다.
 * CLIENT 는 `/client`(주최사 포털 홈, `ClientHomePage`)가 `/mypage` 와 같은 "바로가기 카드"
 * 랜딩이라 `homeRouteFor` 와 같은 경로를 쓴다. ADMIN 은 대시보드 자체가 곧 프로필
 * 진입점이라 여전히 겹친다 — 어드민 전용 랜딩 페이지가 따로 없다.
 */
export const profileRouteFor = (role: Role): string => {
  switch (role) {
    case 'CLIENT':
      return '/client'
    case 'ADMIN':
      return '/admin'
    case 'MEMBER':
    default:
      return '/mypage'
  }
}
