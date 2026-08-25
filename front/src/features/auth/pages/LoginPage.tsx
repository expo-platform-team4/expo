'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { Badge, Button, Card, CardTitle, Input } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'
import { resolvePostLoginRoute } from '@/lib/auth'

import { useLogin } from '../hooks'
import { loginSchema, type LoginFormValues } from '../schemas'

/**
 * `/login`. Function.md 2절 — "이메일·비밀번호. 성공 시 토큰 저장 후 역할별 홈으로 이동".
 * `redirect` 쿼리(예: `/login?redirect=/mypage/orders`)가 있으면 그리로, 없으면
 * `resolvePostLoginRoute(role, redirect)` 로 보낸다. `RequireAuth` 가 만드는 redirect 쿼리를
 * 받되, **그 경로가 로그인한 역할의 구역일 때만** 따른다 — 관리자가 클라이언트 화면으로
 * 들어가 버리던 문제 때문이다. 자세한 이유는 `lib/auth.ts` 주석에 있다.
 */
const LoginPage = () => {
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirectTo = searchParams.get('redirect')
  const justSignedUp = searchParams.get('signup') === 'success'
  const loginMutation = useLogin()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  const onSubmit = (values: LoginFormValues) => {
    setFormError(null)
    loginMutation.mutate(values, {
      onSuccess: (result) => {
        router.replace(resolvePostLoginRoute(result.role, redirectTo))
      },
      onError: (error) => setFormError(getErrorMessage(error)),
    })
  }

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <Card className="w-full max-w-md">
        <CardTitle>로그인</CardTitle>
        {justSignedUp && (
          <Badge variant="success" className="mb-2">
            가입이 완료되었습니다. 로그인해 주세요.
          </Badge>
        )}
        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input
            label="이메일"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register('email')}
          />
          <Input
            label="비밀번호"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
          {formError && <p className="text-label-sm text-error">{formError}</p>}
          <Button type="submit" size="lg" loading={loginMutation.isPending}>
            로그인
          </Button>
        </form>
        <p className="text-label-md text-on-surface-variant mt-6 text-center">
          아직 계정이 없으신가요?{' '}
          <Link href="/signup" className="text-secondary font-semibold">
            회원가입
          </Link>
        </p>
      </Card>
    </div>
  )
}

export default LoginPage
