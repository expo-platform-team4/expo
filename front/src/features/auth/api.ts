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
