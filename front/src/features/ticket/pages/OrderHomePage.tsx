'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'

import RequireAuth from '@/components/auth/RequireAuth'
import { Button, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import type { TicketOrder } from '../api'
import { OrderConfirmation } from '../components/OrderConfirmation'
import { TicketProductSelector } from '../components/TicketProductSelector'
import { useCreateMemberOrder, usePurchasableTicketProducts } from '../hooks'
import { memberOrderSchema, type MemberOrderFormValues } from '../schemas'

/**
 * `/orders` — 티켓 예매·결제(회원). Function.md 2절.
 *
 * 결제 연동이 없어 실제로 하는 일은 "주문 생성" 까지다(`OrderConfirmation` 참고). `/api/orders/member`
 * 가 `@AuthenticationPrincipal` 을 요구해 로그인이 필요하다 — Spec.md 4절의 매핑표에는 없는
 * 경로지만(`/api/member/**` 접두어가 아니다) 실제로는 인증이 필수라 `RequireAuth` 로 감싼다.
 * `/orders/guest`·`/orders/guest/search` 는 반대로 인증이 필요 없어 감싸지 않는다.
 */
const OrderHomePage = () => (
  <RequireAuth roles={['MEMBER']}>
    <OrderHomeContent />
  </RequireAuth>
)

const OrderHomeContent = () => {
  const expoIdParam = useSearchParams().get('expoId')
  const parsedExpoId = expoIdParam ? Number(expoIdParam) : null
  const expoId =
    parsedExpoId && Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  const [createdOrder, setCreatedOrder] = useState<TicketOrder | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const { data: products, isPending, error, refetch } = usePurchasableTicketProducts(expoId)
  const createOrder = useCreateMemberOrder()

  const {
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<MemberOrderFormValues>({
    resolver: zodResolver(memberOrderSchema),
    defaultValues: { items: [] },
  })
  const items = useWatch({ control, name: 'items' })

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="티켓 예매" />
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
        <PageHeader title="티켓 예매" description="주문이 접수되었습니다." />
        <OrderConfirmation order={createdOrder} />
      </div>
    )
  }

  const onSubmit = (values: MemberOrderFormValues) => {
    setFormError(null)
    createOrder.mutate(values.items, {
      onSuccess: (order) => setCreatedOrder(order),
      onError: (err) => setFormError(getErrorMessage(err)),
    })
  }

  return (
    <div>
      <PageHeader title="티켓 예매" description={`박람회 #${expoId} 의 구매 가능한 티켓입니다.`} />

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

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={createOrder.isPending}>
            주문하기
          </Button>
        </form>
      )}
    </div>
  )
}

export default OrderHomePage
