'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'

import { Button, EmptyState, ErrorState, Input, LoadingBlock, PageHeader } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import type { GuestTicketOrder } from '../api'
import { OrderConfirmation } from '../components/OrderConfirmation'
import { TicketProductSelector } from '../components/TicketProductSelector'
import { useCreateGuestOrder, usePurchasableTicketProducts } from '../hooks'
import { guestOrderSchema, type GuestOrderFormValues } from '../schemas'

/**
 * `/orders/guest` — 티켓 예매·결제(비회원). Function.md 2절.
 *
 * `POST /api/orders/guest` 는 인증이 필요 없다. 이름·연락처·나이·비밀번호는 백엔드
 * `GuestTicketOrderRequest` 와 정확히 같은 필드다(`schemas.ts` 참고). 결제 연동이 없는 건
 * 회원 화면과 같다 — `OrderConfirmation` 이 공통으로 처리한다. 여기서는 그 위에 예약자
 * 이름·나이와, 나중에 조회할 때 필요한 비밀번호를 기억해 두라는 안내를 얹는다.
 */
const GuestOrderPage = () => {
  const expoIdParam = useSearchParams().get('expoId')
  const parsedExpoId = expoIdParam ? Number(expoIdParam) : null
  const expoId =
    parsedExpoId && Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  const [createdOrder, setCreatedOrder] = useState<GuestTicketOrder | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const { data: products, isPending, error, refetch } = usePurchasableTicketProducts(expoId)
  const createOrder = useCreateGuestOrder()

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<GuestOrderFormValues>({
    resolver: zodResolver(guestOrderSchema),
    defaultValues: { items: [], name: '', phoneNumber: '', age: 0, password: '' },
  })
  const items = useWatch({ control, name: 'items' })

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="비회원 티켓 예매" />
        <EmptyState
          title="박람회를 먼저 선택해 주세요"
          description="예매하려는 박람회 상세 화면에서 '예매하기' 로 들어와야 합니다."
          action={
            <Link href="/expos">
              <Button variant="secondary">박람회 목록으로</Button>
            </Link>
          }
        />
      </div>
    )
  }

  if (createdOrder) {
    return (
      <div>
        <PageHeader title="비회원 티켓 예매" description="주문이 접수되었습니다." />
        <OrderConfirmation
          order={createdOrder.ticketOrderResponse}
          extra={
            <div className="bg-surface-container-low text-label-md text-on-surface-variant rounded p-3">
              <p>
                예약자 {createdOrder.guestName} ({createdOrder.guestAge}세)
              </p>
              <p className="mt-1">
                주문번호와 방금 입력한 연락처·비밀번호를 기억해 주세요. 이후{' '}
                <Link href="/orders/guest/search" className="text-secondary underline">
                  비회원 주문 조회
                </Link>
                에서 다시 확인할 수 있습니다.
              </p>
            </div>
          }
        />
      </div>
    )
  }

  const onSubmit = (values: GuestOrderFormValues) => {
    setFormError(null)
    createOrder.mutate(values, {
      onSuccess: (order) => setCreatedOrder(order),
      onError: (err) => setFormError(getErrorMessage(err)),
    })
  }

  return (
    <div>
      <PageHeader
        title="비회원 티켓 예매"
        description={`박람회 #${expoId} 의 구매 가능한 티켓입니다. 로그인 없이 예매할 수 있습니다.`}
      />

      {isPending ? (
        <LoadingBlock label="티켓 상품을 불러오는 중입니다" />
      ) : error ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : products.length === 0 ? (
        <EmptyState
          title="구매 가능한 티켓이 없습니다"
          description="현재 판매 중인 티켓 상품이 없습니다."
        />
      ) : (
        <form className="flex flex-col gap-6" onSubmit={handleSubmit(onSubmit)} noValidate>
          <TicketProductSelector
            products={products}
            value={items}
            onChange={(next) => setValue('items', next, { shouldValidate: true })}
            error={errors.items?.message ?? errors.items?.root?.message}
          />

          <div className="flex flex-col gap-4 sm:grid sm:grid-cols-2">
            <Input label="이름" error={errors.name?.message} {...register('name')} />
            <Input
              label="연락처"
              placeholder="01012345678"
              error={errors.phoneNumber?.message}
              {...register('phoneNumber')}
            />
            <Input
              label="나이"
              type="number"
              inputMode="numeric"
              error={errors.age?.message}
              {...register('age', { valueAsNumber: true })}
            />
            <Input
              label="비밀번호"
              type="password"
              hint="주문 조회 시 필요합니다. 잊지 않도록 기억해 주세요."
              error={errors.password?.message}
              {...register('password')}
            />
          </div>

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={createOrder.isPending}>
            주문하기
          </Button>
        </form>
      )}
    </div>
  )
}

export default GuestOrderPage
