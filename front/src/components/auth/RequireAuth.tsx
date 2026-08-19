'use client'

import { useRouter, usePathname } from 'next/navigation'
import { useEffect } from 'react'

import { LoadingBlock } from '@/components/ui'
import { homeRouteFor, type Role, useAuthStore } from '@/lib/auth'

/**
 * 라우트 보호. Spec.md 4절.
 *
 *   토큰 없음                 → /login?redirect={현재 경로}
 *   토큰은 있는데 역할 불일치   → 자기 역할의 홈으로 되돌린다 (403 화면 대신)
 *
 * `roles` 는 선택이다. 안 주면 "로그인만 되어 있으면 통과" — `app/client/layout.tsx` ·
 * `app/mypage/layout.tsx` 처럼 로그인 여부만 보면 되는 자리에서 쓴다. 특정 역할만
 * 들여보내야 하는 자리(예: 관리자 전용 라우트)에만 `roles={['ADMIN']}` 을 넘긴다.
 *
 * Zustand persist 는 localStorage 에서 하이드레이션되기까지 한 틱이 걸린다. 그 틱 동안
 * accessToken 이 null 로 보여서, 하이드레이션 전에 바로 리다이렉트하면 로그인 상태인
 * 사용자도 /login 으로 튕겨 나간다. store 의 `hasHydrated` 로 그 틱을 기다린다
 * (`lib/auth.ts` 의 `onRehydrateStorage` 참고 — effect 로 흉내 내지 않는다).
 */
const RequireAuth = ({ roles, children }: { roles?: Role[]; children: React.ReactNode }) => {
  const router = useRouter()
  const pathname = usePathname()
  const { accessToken, role, hasHydrated } = useAuthStore()

  const roleMismatch = Boolean(roles && role && !roles.includes(role))

  useEffect(() => {
    if (!hasHydrated) return

    if (!accessToken) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname)}`)
      return
    }
    if (roleMismatch && role) {
      router.replace(homeRouteFor(role))
    }
  }, [hasHydrated, accessToken, role, roleMismatch, router, pathname])

  if (!hasHydrated || !accessToken || roleMismatch) {
    return <LoadingBlock label="확인하는 중입니다" />
  }

  return children
}

export default RequireAuth
