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
      return '/client/dashboard'
    case 'ADMIN':
      return '/admin'
    case 'MEMBER':
    default:
      return '/'
  }
}

/**
 * 헤더의 "Profile" 클릭 시 이동할 경로. `homeRouteFor` 와 다른 이유 —
 * MEMBER 는 역할 홈이 `/`(공개 홈)이라 프로필 진입점이 따로 필요하다.
 * CLIENT·ADMIN 은 대시보드 자체가 곧 프로필 진입점이라 겹친다.
 */
export const profileRouteFor = (role: Role): string => {
  switch (role) {
    case 'CLIENT':
      return '/client/dashboard'
    case 'ADMIN':
      return '/admin'
    case 'MEMBER':
    default:
      return '/mypage'
  }
}
