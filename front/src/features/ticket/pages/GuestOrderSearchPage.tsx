'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Card, CardTitle, Input } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useGuestOrderSearch } from '../hooks'
import { guestOrderSearchSchema, type GuestOrderSearchFormValues } from '../schemas'
import { useGuestOrderLookupStore } from '../store'

/**
 * `/orders/guest/search` — 비회원 주문 조회. Function.md 2절.
 *
 * `POST /api/orders/search/guest` 는 실제로 동작하는 엔드포인트다. 화면 지시문은 "주문번호 +
 * 비밀번호" 라고 적었지만, 실제 `GuestTicketSearchRequest` 는 `orderNumber`·`password`·
 * `phoneNumber` 셋을 요구한다(`backend/.../ticket/dto/GuestTicketSearchRequest.java`) — 서버가
 * 최종 권위이므로(Spec.md 7절) 폼도 셋 다 받는다.
 *
 * 조회에 성공하면 결과를 `useGuestOrderLookupStore` 에 담아 `/orders/guest/{orderNumber}` 로
 * 이동한다. 그 화면이 스토어를 못 찾으면(새로고침·직접 링크) 같은 조회 API 로 다시 인증받는
 * 폼을 보여준다 — GET-by-id 가 없어서 나온 선택이다(`GuestOrderDetailPage.tsx` 참고).
 */
const GuestOrderSearchPage = () => {
  const router = useRouter()
  const setResult = useGuestOrderLookupStore((state) => state.setResult)
  const [formError, setFormError] = useState<string | null>(null)

  const search = useGuestOrderSearch()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<GuestOrderSearchFormValues>({
    resolver: zodResolver(guestOrderSearchSchema),
    defaultValues: { orderNumber: '', password: '', phoneNumber: '' },
  })

  const onSubmit = (values: GuestOrderSearchFormValues) => {
    setFormError(null)
    search.mutate(values, {
      onSuccess: (result) => {
        setResult(result)
        router.push(`/orders/guest/${encodeURIComponent(result.orderNumber)}`)
      },
      onError: (err) => setFormError(getErrorMessage(err)),
    })
  }

  return (
    <div className="flex justify-center py-8">
      <Card className="w-full max-w-md">
        <CardTitle>비회원 주문 조회</CardTitle>
        <p className="text-body-md text-on-surface-variant mb-4">
          예매 시 입력한 주문번호·연락처·비밀번호로 조회합니다.
        </p>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input
            label="주문번호"
            error={errors.orderNumber?.message}
            {...register('orderNumber')}
          />
          <Input
            label="연락처"
            placeholder="01012345678"
            error={errors.phoneNumber?.message}
            {...register('phoneNumber')}
          />
          <Input
            label="비밀번호"
            type="password"
            error={errors.password?.message}
            {...register('password')}
          />

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={search.isPending}>
            조회하기
          </Button>
        </form>
      </Card>
    </div>
  )
}

export default GuestOrderSearchPage
