'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Card, CardTitle, Checkbox, Input } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { EmailVerificationField } from '../components/EmailVerificationField'
import { PhoneVerificationField } from '../components/PhoneVerificationField'
import {
  useBusinessNumberAvailability,
  useEmailAvailability,
  useNicknameAvailability,
  useSignupClient,
} from '../hooks'
import { clientSignupSchema, type ClientSignupFormValues } from '../schemas'
import { useAvailabilityHint } from '../useAvailabilityHint'

/** `/signup/client`. Function.md 2절 — "사업자등록번호 검증 포함". 이메일 인증도 가입을 막는다. */
const ClientSignupPage = () => {
  const router = useRouter()
  const signupMutation = useSignupClient()
  const [formError, setFormError] = useState<string | null>(null)
  const [phoneVerified, setPhoneVerified] = useState(false)
  const [emailVerified, setEmailVerified] = useState(false)

  const emailAvailability = useAvailabilityHint(useEmailAvailability())
  const nicknameAvailability = useAvailabilityHint(useNicknameAvailability())
  const businessNumberAvailability = useAvailabilityHint(useBusinessNumberAvailability())

  const {
    register,
    handleSubmit,
    control,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ClientSignupFormValues>({
    resolver: zodResolver(clientSignupSchema),
    defaultValues: {
      email: '',
      password: '',
      passwordConfirm: '',
      nickname: '',
      phoneNumber: '',
      emailVerificationToken: '',
      phoneVerificationToken: '',
      companyName: '',
      businessNumber: '',
      serviceTermsAgreed: false,
      privacyPolicyAgreed: false,
      marketingAgreed: false,
    },
  })

  const emailValue = watch('email')

  const onSubmit = (values: ClientSignupFormValues) => {
    setFormError(null)
    signupMutation.mutate(values, {
      onSuccess: () => router.push('/login?signup=success'),
      onError: (error) => setFormError(getErrorMessage(error)),
    })
  }

  return (
    <div className="flex justify-center py-8">
      <Card className="w-full max-w-lg">
        <CardTitle>기업 회원가입</CardTitle>
        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <p className="text-label-sm text-on-surface-variant -mt-2">담당자 정보</p>
          <Input
            label="이메일"
            type="email"
            autoComplete="email"
            error={
              errors.email?.message ??
              (emailAvailability.hint?.ok === false ? emailAvailability.hint.message : undefined)
            }
            hint={emailAvailability.hint?.ok ? emailAvailability.hint.message : undefined}
            {...register('email', {
              onBlur: (event) => emailAvailability.check(event.target.value),
              onChange: () => {
                // 이메일이 바뀌면 이전 인증 토큰은 그 즉시 무효 — 아래 key={emailValue} 로
                // EmailVerificationField 도 함께 리마운트되어 내부 UI 상태가 리셋된다.
                setValue('emailVerificationToken', '', { shouldValidate: true })
                setEmailVerified(false)
              },
            })}
          />
          <EmailVerificationField
            key={emailValue}
            email={emailValue}
            emailHasError={Boolean(errors.email)}
            onVerified={(token) => {
              setValue('emailVerificationToken', token ?? '', { shouldValidate: true })
              setEmailVerified(Boolean(token))
            }}
          />
          <Input
            label="비밀번호"
            type="password"
            autoComplete="new-password"
            hint="영문·숫자·특수문자를 포함해 8자 이상"
            error={errors.password?.message}
            {...register('password')}
          />
          <Input
            label="비밀번호 확인"
            type="password"
            autoComplete="new-password"
            error={errors.passwordConfirm?.message}
            {...register('passwordConfirm')}
          />
          <Input
            label="담당자 닉네임"
            autoComplete="nickname"
            error={
              errors.nickname?.message ??
              (nicknameAvailability.hint?.ok === false
                ? nicknameAvailability.hint.message
                : undefined)
            }
            hint={nicknameAvailability.hint?.ok ? nicknameAvailability.hint.message : undefined}
            {...register('nickname', {
              onBlur: (event) => nicknameAvailability.check(event.target.value),
            })}
          />
          <PhoneVerificationField
            control={control}
            name="phoneNumber"
            verified={phoneVerified}
            onVerified={(token) => {
              setValue('phoneVerificationToken', token ?? '', { shouldValidate: true })
              setPhoneVerified(Boolean(token))
            }}
          />

          <p className="text-label-sm text-on-surface-variant mt-2">기업 정보</p>
          <Input label="기업명" error={errors.companyName?.message} {...register('companyName')} />
          <Input
            label="사업자등록번호"
            placeholder="123-45-67890"
            hint="테스트 번호: 1234567890 · 1111111111 · 2222222222"
            error={
              errors.businessNumber?.message ??
              (businessNumberAvailability.hint?.ok === false
                ? businessNumberAvailability.hint.message
                : undefined)
            }
            {...register('businessNumber', {
              onBlur: (event) => businessNumberAvailability.check(event.target.value),
            })}
          />

          <div className="flex flex-col gap-2 pt-2">
            <Checkbox
              label="[필수] 서비스 이용약관에 동의합니다"
              error={errors.serviceTermsAgreed?.message}
              {...register('serviceTermsAgreed')}
            />
            <Checkbox
              label="[필수] 개인정보 수집 및 이용에 동의합니다"
              error={errors.privacyPolicyAgreed?.message}
              {...register('privacyPolicyAgreed')}
            />
            <Checkbox
              label="[선택] 마케팅 정보 수신에 동의합니다"
              {...register('marketingAgreed')}
            />
          </div>

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button
            type="submit"
            size="lg"
            loading={signupMutation.isPending}
            disabled={!phoneVerified || !emailVerified}
          >
            가입하기
          </Button>
          {(!phoneVerified || !emailVerified) && (
            <p className="text-label-sm text-on-surface-variant text-center">
              {!emailVerified && !phoneVerified
                ? '이메일과 휴대폰 인증을 완료해 주세요.'
                : !emailVerified
                  ? '이메일 인증을 완료해 주세요.'
                  : '휴대폰 인증을 완료해 주세요.'}
            </p>
          )}
        </form>
      </Card>
    </div>
  )
}

export default ClientSignupPage
