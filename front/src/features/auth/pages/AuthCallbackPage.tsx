'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import { useEffect } from 'react'

import { ErrorState, LoadingBlock } from '@/components/ui'
import { homeRouteFor, useAuthStore, type Role } from '@/lib/auth'

/**
 * `/auth/callback`. Function.md 2절 — "소셜 로그인 콜백. OAuth2 리다이렉트 처리".
 *
 * 백엔드 OAuth2 는 `application-oauth2.yml` 로 켤 수 있는 선택 프로필이고, 로그인 성공 후
 * 프론트로 무엇을 실어 돌려보내는지 정하는 `SuccessHandler` 코드가 저장소에 아직 없다
 * (검색해도 안 나온다 — `AuthService`/`LoginService` 는 로컬 로그인만 다룬다).
 *
 * 그래서 이 화면은 **가장 흔한 Spring Security OAuth2 관례**를 가정해 만든다 —
 * `accessToken`·`refreshToken`·`role`·`nickname` 을 쿼리 파라미터로 실어 리다이렉트하는
 * 방식. 백엔드에 SuccessHandler 가 실제로 붙으면 이 계약이 맞는지 다시 확인해야 한다.
 */
const AuthCallbackPage = () => {
  const router = useRouter()
  const searchParams = useSearchParams()

  const accessToken = searchParams.get('accessToken')
  const refreshToken = searchParams.get('refreshToken')
  const role = searchParams.get('role') as Role | null
  const nickname = searchParams.get('nickname')

  useEffect(() => {
    if (!accessToken || !refreshToken || !role || !nickname) return
    useAuthStore.getState().setTokens(accessToken, refreshToken, role, nickname)
    router.replace(homeRouteFor(role))
  }, [accessToken, refreshToken, role, nickname, router])

  if (!accessToken || !refreshToken || !role || !nickname) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <ErrorState
          error={new Error('소셜 로그인 응답에 필요한 정보가 없습니다.')}
          onRetry={() => router.replace('/login')}
        />
      </div>
    )
  }

  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <LoadingBlock label="로그인 처리 중입니다" />
    </div>
  )
}

export default AuthCallbackPage
