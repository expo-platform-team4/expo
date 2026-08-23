import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

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

export const useRequestEmailVerification = () =>
  useMutation({ mutationFn: authApi.requestEmailVerification })

export const useConfirmEmailVerification = () =>
  useMutation({
    mutationFn: ({
      verificationId,
      verificationCode,
    }: {
      verificationId: number
      verificationCode: string
    }) => authApi.confirmEmailVerification(verificationId, verificationCode),
  })

/**
 * 로그아웃. Spec.md 3-4 절 — 액세스 토큰은 stateless 라 서버가 즉시 무효화하지 않는다.
 * API 성공/실패와 무관하게 `onSettled` 에서 항상 store 를 비운다.
 *
 * `queryClient.clear()` 도 같이 호출한다 — 주문·티켓처럼 회원 ID 없이 고정된 키를 쓰는
 * 쿼리 캐시가 그대로 남아 있으면, 같은 브라우저에서 다른 계정으로 다시 로그인했을 때
 * 이전 사용자의 데이터가 잠깐 보일 수 있다.
 */
export const useLogout = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.logout,
    onSettled: () => {
      useAuthStore.getState().clear()
      queryClient.clear()
    },
  })
}

export const useMyProfile = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: authKeys.profile,
    queryFn: authApi.getMyProfile,
    enabled: Boolean(accessToken),
  })
}

/** 닉네임 변경 (A-API-016). 성공하면 사이드바·프로필 화면이 같이 쓰는 profile 캐시를 갱신한다. */
export const useChangeNickname = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.changeNickname,
    onSuccess: (profile) => {
      queryClient.setQueryData(authKeys.profile, profile)
    },
  })
}

/** 프로필 이미지 등록·교체 (A-API-017). 닉네임 변경과 같은 캐시 갱신 패턴이다. */
export const useChangeProfileImage = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.changeProfileImage,
    onSuccess: (profile) => {
      queryClient.setQueryData(authKeys.profile, profile)
    },
  })
}

/** 프로필 이미지 삭제 (A-API-017). */
export const useRemoveProfileImage = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.removeProfileImage,
    onSuccess: (profile) => {
      queryClient.setQueryData(authKeys.profile, profile)
    },
  })
}

/** 비밀번호 재설정 요청 (A-API-013). 로그인 화면의 "비밀번호 찾기"에서 쓴다. */
export const useRequestPasswordReset = () =>
  useMutation({ mutationFn: authApi.requestPasswordReset })

/** 재설정 토큰으로 새 비밀번호 저장 (A-API-014). */
export const useConfirmPasswordReset = () =>
  useMutation({ mutationFn: authApi.confirmPasswordReset })

/** 로그인 상태에서의 비밀번호 변경. */
export const useChangePassword = () => useMutation({ mutationFn: authApi.changePassword })

/**
 * 회원 탈퇴 (A-API-018). 성공하면 서버 Refresh Token 이 이미 폐기됐으므로, 로컬 인증
 * 상태도 즉시 지운다 — `useLogout` 과 같은 `onSettled` 패턴이다.
 */
export const useWithdraw = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: authApi.withdrawMember,
    onSuccess: () => {
      useAuthStore.getState().clear()
      queryClient.clear()
    },
  })
}
