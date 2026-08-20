import { useMutation, useQuery } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import * as authApi from './api'
import { authKeys } from './queryKeys'

/**
 * 로그인 응답에 `role`·`nickname` 이 이미 포함되어 있다 — 별도 프로필 조회 없이
 * 응답 하나로 store 를 완성한다.
 */
export const useLogin = () =>
  useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      authApi.login(email, password),
    onSuccess: (result) => {
      useAuthStore
        .getState()
        .setTokens(result.accessToken, result.refreshToken, result.role, result.nickname)
    },
  })

export const useSignupMember = () => useMutation({ mutationFn: authApi.signupMember })

export const useSignupClient = () => useMutation({ mutationFn: authApi.signupClient })

export const useEmailAvailability = () =>
  useMutation({ mutationFn: authApi.checkEmailAvailability })

export const useNicknameAvailability = () =>
  useMutation({ mutationFn: authApi.checkNicknameAvailability })

export const useBusinessNumberAvailability = () =>
  useMutation({ mutationFn: authApi.checkBusinessNumberAvailability })

export const useRequestPhoneVerification = () =>
  useMutation({ mutationFn: authApi.requestPhoneVerification })

export const useConfirmPhoneVerification = () =>
  useMutation({
    mutationFn: ({
      verificationId,
      verificationCode,
    }: {
      verificationId: number
      verificationCode: string
    }) => authApi.confirmPhoneVerification(verificationId, verificationCode),
  })

/**
 * 로그아웃. Spec.md 3-4 절 — 액세스 토큰은 stateless 라 서버가 즉시 무효화하지 않는다.
 * API 성공/실패와 무관하게 `onSettled` 에서 항상 store 를 비운다.
 */
export const useLogout = () =>
  useMutation({
    mutationFn: authApi.logout,
    onSettled: () => {
      useAuthStore.getState().clear()
    },
  })

export const useMyProfile = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: authKeys.profile,
    queryFn: authApi.getMyProfile,
    enabled: Boolean(accessToken),
  })
}
