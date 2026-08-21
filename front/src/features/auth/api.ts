import { api } from '@/lib/api'
import type { Role } from '@/lib/auth'

/** 백엔드 공통 응답 봉투. `com.expo.common.response.ApiResponse`. */
type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

export type LoginResult = {
  accessToken: string
  accessTokenExpiresInMinutes: number
  refreshToken: string
  refreshTokenExpiresAt: string
  userId: number
  email: string
  nickname: string
  role: Role
}

/** 이메일·비밀번호 로그인. `POST /api/auth/login`. */
export const login = async (email: string, password: string): Promise<LoginResult> => {
  const { data } = await api.post<ApiEnvelope<LoginResult>>('/auth/login', { email, password })
  return data.data
}

/** `POST /api/auth/logout` — 서버에 저장된 refresh token 전체를 폐기한다. */
export const logout = async (): Promise<void> => {
  await api.post('/auth/logout')
}

export type SignupMemberPayload = {
  email: string
  password: string
  passwordConfirm: string
  nickname: string
  phoneNumber: string
  /** `POST /api/auth/email-verifications/confirm` 응답의 `signupVerificationToken`. */
  emailVerificationToken: string
  serviceTermsAgreed: boolean
  privacyPolicyAgreed: boolean
  marketingAgreed: boolean
}

export type SignupMemberResult = { userId: number; email: string; nickname: string; role: Role }

/** 일반 회원가입. `POST /api/auth/signup`. `SignupRequest` 와 필드가 정확히 대응한다. */
export const signupMember = async (payload: SignupMemberPayload): Promise<SignupMemberResult> => {
  const { data } = await api.post<ApiEnvelope<SignupMemberResult>>('/auth/signup', payload)
  return data.data
}

export type SignupClientPayload = SignupMemberPayload & {
  companyName: string
  businessNumber: string
}

export type SignupClientResult = {
  userId: number
  role: Role
  email: string
  companyName: string
  message: string
}

/** 클라이언트(주최사) 회원가입. `POST /api/auth/client-signup`. `ClientSignupRequest` 와 대응한다. */
export const signupClient = async (payload: SignupClientPayload): Promise<SignupClientResult> => {
  const { data } = await api.post<ApiEnvelope<SignupClientResult>>('/auth/client-signup', payload)
  return data.data
}

export type AvailabilityResult = {
  valid: boolean
  duplicate: boolean
  available: boolean
  message: string
}

/** `GET /api/auth/email-availability`. */
export const checkEmailAvailability = async (email: string): Promise<AvailabilityResult> => {
  const { data } = await api.get<ApiEnvelope<AvailabilityResult>>('/auth/email-availability', {
    params: { email },
  })
  return data.data
}

/** `GET /api/auth/nickname-availability`. */
export const checkNicknameAvailability = async (nickname: string): Promise<AvailabilityResult> => {
  const { data } = await api.get<ApiEnvelope<AvailabilityResult>>('/auth/nickname-availability', {
    params: { nickname },
  })
  return data.data
}

/** `GET /api/auth/business-number-availability`. 테스트 번호: 1234567890 / 1111111111 / 2222222222. */
export const checkBusinessNumberAvailability = async (
  businessNumber: string
): Promise<AvailabilityResult> => {
  const { data } = await api.get<ApiEnvelope<AvailabilityResult>>(
    '/auth/business-number-availability',
    { params: { businessNumber } }
  )
  return data.data
}

export type PhoneVerificationRequestResult = {
  verificationId: number
  phoneNumber: string
  expiresAt: string
  message: string
}

/** `POST /api/auth/phone-verifications`. MVP: 테스트 인증번호 `123456` 고정. */
export const requestPhoneVerification = async (
  phoneNumber: string
): Promise<PhoneVerificationRequestResult> => {
  const { data } = await api.post<ApiEnvelope<PhoneVerificationRequestResult>>(
    '/auth/phone-verifications',
    { phoneNumber }
  )
  return data.data
}

export type PhoneVerificationConfirmResult = {
  verificationId: number
  phoneNumber: string
  verifiedAt: string
  signupVerificationToken: string
  message: string
}

/** `POST /api/auth/phone-verifications/confirm`. */
export const confirmPhoneVerification = async (
  verificationId: number,
  verificationCode: string
): Promise<PhoneVerificationConfirmResult> => {
  const { data } = await api.post<ApiEnvelope<PhoneVerificationConfirmResult>>(
    '/auth/phone-verifications/confirm',
    { verificationId, verificationCode }
  )
  return data.data
}

export type EmailVerificationCreateResult = {
  verificationId: number
  email: string
  expiresAt: string
  message: string
}

/** `POST /api/auth/email-verifications` — 회원가입 이메일 인증 요청. */
export const requestEmailVerification = async (
  email: string
): Promise<EmailVerificationCreateResult> => {
  const { data } = await api.post<ApiEnvelope<EmailVerificationCreateResult>>(
    '/auth/email-verifications',
    { email }
  )
  return data.data
}

export type EmailVerificationConfirmResult = {
  verificationId: number
  email: string
  verifiedAt: string
  signupVerificationToken: string
  message: string
}

/** `POST /api/auth/email-verifications/confirm`. */
export const confirmEmailVerification = async (
  verificationId: number,
  verificationCode: string
): Promise<EmailVerificationConfirmResult> => {
  const { data } = await api.post<ApiEnvelope<EmailVerificationConfirmResult>>(
    '/auth/email-verifications/confirm',
    { verificationId, verificationCode }
  )
  return data.data
}

export type MemberProfile = {
  userId: number
  email: string
  nickname: string
  role: Role
  phoneNumber: string | null
  profileImageFileId: number | null
  profileImageUpdatedAt: string | null
  createdAt: string
}

/** `GET /api/users/me/profile` — 로그인 사용자 공통 프로필(마이페이지 사이드바용). */
export const getMyProfile = async (): Promise<MemberProfile> => {
  const { data } = await api.get<ApiEnvelope<MemberProfile>>('/users/me/profile')
  return data.data
}

/** `PATCH /api/users/me/nickname` — A-API-016. 기존과 같은 닉네임을 다시 보내도 에러 없이 처리된다. */
export const changeNickname = async (nickname: string): Promise<MemberProfile> => {
  const { data } = await api.patch<ApiEnvelope<MemberProfile>>('/users/me/nickname', { nickname })
  return data.data
}

export type ChangePasswordPayload = {
  currentPassword: string
  newPassword: string
  newPasswordConfirm: string
}

/**
 * `PATCH /api/users/me/password` — 로그인 상태에서의 비밀번호 변경.
 *
 * 이메일 토큰 기반 재설정(`requestPasswordReset`/`confirmPasswordReset`, "비밀번호를 잊어버렸을 때"용,
 * 로그인 화면 쪽에서 쓴다)과는 다른 흐름이다. 마이페이지에서는 이미 로그인돼 있으므로 현재
 * 비밀번호만 확인하고 바로 바꾼다.
 */
export const changePassword = async (payload: ChangePasswordPayload): Promise<void> => {
  await api.patch('/users/me/password', payload)
}

export type PasswordResetRequestResult = {
  email: string
  expiresAt: string
  message: string
}

/**
 * `POST /api/auth/password-reset-requests` — A-API-013.
 *
 * MVP: 이메일 발송이 아직 연동돼 있지 않다. 재설정 토큰은 로그에도 남기지 않으므로,
 * 실제 이메일 발송이 붙기 전까지는 이 API 단독으로 재설정 흐름을 끝까지 테스트할 수 없다
 * (백엔드 `PasswordResetController` 설명 참고). 토큰을 손에 넣는 방법이 생기면
 * 아래 `confirmPasswordReset` 으로 이어간다.
 */
export const requestPasswordReset = async (email: string): Promise<PasswordResetRequestResult> => {
  const { data } = await api.post<ApiEnvelope<PasswordResetRequestResult>>(
    '/auth/password-reset-requests',
    { email }
  )
  return data.data
}

export type PasswordResetConfirmPayload = {
  resetToken: string
  newPassword: string
  newPasswordConfirm: string
}

/** `POST /api/auth/password-resets/confirm` — A-API-014. 토큰은 1회용, 30분 유효. */
export const confirmPasswordReset = async (
  payload: PasswordResetConfirmPayload
): Promise<{ message: string }> => {
  const { data } = await api.post<ApiEnvelope<{ message: string }>>(
    '/auth/password-resets/confirm',
    payload
  )
  return data.data
}

/**
 * `POST /api/users/me/withdrawal` — A-API-018.
 *
 * 현재 비밀번호로 본인 확인 후 소프트 삭제(`account_status = WITHDRAWN`) 처리한다. 성공하면
 * 서버의 모든 Refresh Token 이 폐기된다 — 화면은 이 호출 뒤에 반드시 로컬 인증 상태도 지워야 한다
 * (`useWithdraw` 훅 참고).
 */
export const withdrawMember = async (password: string): Promise<void> => {
  await api.post('/users/me/withdrawal', { password })
}
