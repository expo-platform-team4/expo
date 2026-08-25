import { z } from 'zod'

/**
 * 백엔드 `SignupRequest` / `ClientSignupRequest` 의 `@Pattern` 과 정확히 같은 정규식이어야
 * 한다 (Spec.md 7절 — "규칙이 두 곳에 있으면 어긋나는 날이 온다"). 백엔드 소스를 그대로 옮겼다.
 */
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/
const PHONE_PATTERN = /^01[016789]\d{7,8}$/
const BUSINESS_NUMBER_PATTERN = /^\d{3}-?\d{2}-?\d{5}$/

const baseSignupShape = {
  email: z
    .string()
    .min(1, '이메일은 필수입니다.')
    .max(255, '이메일은 255자 이하여야 합니다.')
    .email('올바른 이메일 형식이 아닙니다.'),
  password: z
    .string()
    .regex(PASSWORD_PATTERN, '비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.'),
  passwordConfirm: z.string().min(1, '비밀번호 확인은 필수입니다.'),
  nickname: z
    .string()
    .min(2, '닉네임은 2자 이상 50자 이하여야 합니다.')
    .max(50, '닉네임은 2자 이상 50자 이하여야 합니다.'),
  phoneNumber: z.string().regex(PHONE_PATTERN, '휴대폰 번호 형식이 올바르지 않습니다.'),
  emailVerificationToken: z.string().min(1, '이메일 인증을 완료해 주세요.'),
  phoneVerificationToken: z.string().min(1, '휴대폰 본인인증을 완료해 주세요.'),
  serviceTermsAgreed: z.boolean().refine((agreed) => agreed, '서비스 이용약관에 동의해 주세요.'),
  privacyPolicyAgreed: z
    .boolean()
    .refine((agreed) => agreed, '개인정보 수집 및 이용에 동의해 주세요.'),
  marketingAgreed: z.boolean(),
}

/** 두 가입 폼이 공유하는 비밀번호 일치 검증. `refine` 은 스키마 합성 뒤에만 걸 수 있어 각자 붙인다. */
const passwordsMatch = { message: '비밀번호가 일치하지 않습니다.', path: ['passwordConfirm'] }

export const memberSignupSchema = z
  .object(baseSignupShape)
  .refine((data) => data.password === data.passwordConfirm, passwordsMatch)
export type MemberSignupFormValues = z.infer<typeof memberSignupSchema>

export const clientSignupSchema = z
  .object({
    ...baseSignupShape,
    companyName: z
      .string()
      .min(1, '기업명은 필수입니다.')
      .max(150, '기업명은 150자 이하여야 합니다.'),
    businessNumber: z
      .string()
      .regex(BUSINESS_NUMBER_PATTERN, '사업자등록번호 형식이 올바르지 않습니다.'),
  })
  .refine((data) => data.password === data.passwordConfirm, passwordsMatch)
export type ClientSignupFormValues = z.infer<typeof clientSignupSchema>

export const loginSchema = z.object({
  email: z.string().min(1, '이메일은 필수입니다.').email('올바른 이메일 형식이 아닙니다.'),
  password: z.string().min(1, '비밀번호는 필수입니다.'),
})
export type LoginFormValues = z.infer<typeof loginSchema>

/** 마이페이지 "프로필 수정" — 닉네임 변경. 백엔드 `NicknameChangeRequest`. */
export const nicknameChangeSchema = z.object({
  nickname: baseSignupShape.nickname,
})
export type NicknameChangeFormValues = z.infer<typeof nicknameChangeSchema>

/** 재설정 토큰으로 새 비밀번호 저장. 백엔드 `PasswordResetConfirmRequest`. */
export const passwordResetConfirmSchema = z
  .object({
    resetToken: z.string().min(1, '재설정 토큰은 필수입니다.'),
    newPassword: z
      .string()
      .regex(PASSWORD_PATTERN, '비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.'),
    newPasswordConfirm: z.string().min(1, '새 비밀번호 확인은 필수입니다.'),
  })
  .refine((data) => data.newPassword === data.newPasswordConfirm, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['newPasswordConfirm'],
  })
export type PasswordResetConfirmFormValues = z.infer<typeof passwordResetConfirmSchema>

/** 로그인 상태에서의 비밀번호 변경. 백엔드 `ChangePasswordRequest`. */
export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, '현재 비밀번호는 필수입니다.'),
    newPassword: z
      .string()
      .regex(PASSWORD_PATTERN, '비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.'),
    newPasswordConfirm: z.string().min(1, '새 비밀번호 확인은 필수입니다.'),
  })
  .refine((data) => data.newPassword === data.newPasswordConfirm, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['newPasswordConfirm'],
  })
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>

/** 회원 탈퇴 — 본인 확인용 현재 비밀번호. 백엔드 `MemberWithdrawalRequest`. */
export const withdrawalSchema = z.object({
  password: z.string().min(1, '비밀번호는 필수입니다.'),
})
export type WithdrawalFormValues = z.infer<typeof withdrawalSchema>
