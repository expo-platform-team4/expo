'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useParams } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button, Card, Input, PageHeader } from '@/components/ui'
import { TossCheckout } from '@/features/payment/components/TossCheckout'
import { GuestRefundPanel } from '@/features/refund/components/GuestRefundPanel'
import { formatCurrency } from '@/lib/currency'
import { getErrorMessage } from '@/lib/errorMessage'

import type { GuestOrderSearchResult } from '../api'
import { OrderStatusBadge } from '../components/OrderStatusBadge'
import { useGuestOrderSearch } from '../hooks'
import { useGuestOrderLookupStore } from '../store'

/**
 * `/orders/guest/{orderNumber}` — 비회원 주문 상세. Function.md 2절.
 *
 * **GET-by-id 엔드포인트가 없다** — 있는 건 `POST /api/orders/search/guest` 뿐이고, 그건
 * 주문번호·연락처·비밀번호 조합을 매번 요구한다(조회 자체가 인증이다). 그래서:
 *
 *   1. 검색 화면(`GuestOrderSearchPage`)에서 막 넘어온 경우 — 조회 결과를
 *      `useGuestOrderLookupStore` 에 이미 들고 있으니 바로 보여준다.
 *   2. 이 주소로 직접 들어온 경우(새로고침·북마크·공유 링크) — 스토어가 비어 있다.
 *      주문번호는 URL 에 있지만 연락처·비밀번호는 없으므로, 그 둘만 다시 받는 축소판
 *      조회 폼을 보여주고 같은 API 로 재인증한다.
 *
 * 가짜 GET 상세 API 를 만들지 않고 실제로 있는 조회 API 를 재사용하는 쪽을 골랐다 — 이
 * 화면만 다른 인증 규칙을 흉내 내면 "링크만 알면 남의 주문도 보인다" 는 구멍이 생긴다.
 *
 * "환불 신청" 은 `GuestRefundPanel` 이 처리한다 — 연락처·비밀번호를 다시 입력받아
 * `POST /api/orders/guest/refund-eligibility` 로 가능 여부를 먼저 확인하고,
 * `POST /api/orders/guest/refunds` 로 실제 요청을 보낸다(둘 다 위 재조회 폼과 같은
 * "조합 자체가 인증" 규칙을 쓴다).
 */
const GuestOrderDetailPage = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const storedResult = useGuestOrderLookupStore((state) => state.result)
  const setResult = useGuestOrderLookupStore((state) => state.setResult)

  const result = storedResult?.orderNumber === orderNumber ? storedResult : null

  return (
    <div>
      <PageHeader title="주문 상세" description={orderNumber} />
      {result ? (
        <OrderDetail result={result} />
      ) : (
        <ReLookupForm orderNumber={orderNumber} onFound={setResult} />
      )}
    </div>
  )
}

/** 스토어에 없을 때 보여주는 축소판 재조회 폼. 주문번호는 URL 값을 그대로 쓰고 바꿀 수 없다. */
const reLookupSchema = z.object({
  password: z
    .string()
    .min(1, '비밀번호는 필수입니다.')
    .max(100, '비밀번호는 100자 이하여야 합니다.'),
  phoneNumber: z.string().min(1, '연락처는 필수입니다.').max(20, '연락처는 20자 이하여야 합니다.'),
})
type ReLookupFormValues = z.infer<typeof reLookupSchema>

const ReLookupForm = ({
  orderNumber,
  onFound,
}: {
  orderNumber: string
  onFound: (result: GuestOrderSearchResult) => void
}) => {
  const search = useGuestOrderSearch()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ReLookupFormValues>({
    resolver: zodResolver(reLookupSchema),
    defaultValues: { password: '', phoneNumber: '' },
  })

  const onSubmit = (values: ReLookupFormValues) => {
    setFormError(null)
    search.mutate(
      { orderNumber, ...values },
      {
        onSuccess: onFound,
        onError: (err) => setFormError(getErrorMessage(err)),
      }
    )
  }

  return (
    <Card className="max-w-md">
      <p className="text-body-md text-on-surface-variant mb-4">
        이 주문을 다시 확인하려면 연락처와 비밀번호를 입력해 주세요.
      </p>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Input label="주문번호" value={orderNumber} disabled readOnly />
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
          확인하기
        </Button>
      </form>
    </Card>
  )
}

const OrderDetail = ({ result }: { result: GuestOrderSearchResult }) => {
  const [checkoutOpen, setCheckoutOpen] = useState(false)

  return (
    <div className="flex flex-col gap-4">
      <Card className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-2">
          <p className="text-label-md text-on-surface-variant font-mono">{result.orderNumber}</p>
          <OrderStatusBadge status={result.status} />
        </div>

        <ul className="divide-outline-variant divide-y">
          {result.items.map((item) => (
            <li key={item.ticketProductId} className="flex items-center justify-between py-2">
              <div>
                <p className="text-body-md text-on-surface">{item.ticketName}</p>
                <p className="text-label-sm text-on-surface-variant">
                  {formatCurrency(item.unitPrice)} × {item.quantity}매
                </p>
              </div>
              <p className="text-body-md text-on-surface font-medium">
                {formatCurrency(item.itemSubtotalAmount)}
              </p>
            </li>
          ))}
        </ul>

        <div className="border-outline-variant flex flex-col gap-1 border-t pt-3">
          <div className="text-label-md text-on-surface-variant flex justify-between">
            <span>티켓 소계</span>
            <span>{formatCurrency(result.ticketSubtotalAmount)}</span>
          </div>
          <div className="text-label-md text-on-surface-variant flex justify-between">
            <span>예약 수수료</span>
            <span>{formatCurrency(result.bookingFeeAmount)}</span>
          </div>
          <div className="text-title-lg text-on-surface flex justify-between font-semibold">
            <span>총 결제 금액</span>
            <span>{formatCurrency(result.totalAmount)}</span>
          </div>
        </div>
      </Card>

      {/* PENDING(결제 대기)이면 재조회로 다시 들어왔을 때도 만료 전까지는 이어서 결제할 수
          있어야 한다 — 주문 생성 직후 화면(OrderConfirmation)에만 있으면 안 된다. */}
      {result.status === 'PENDING' && !checkoutOpen && (
        <Button onClick={() => setCheckoutOpen(true)}>다시 결제하기</Button>
      )}
      {result.status === 'PENDING' && checkoutOpen && (
        <TossCheckout orderNumber={result.orderNumber} />
      )}

      {result.status === 'PAID' && <GuestRefundPanel orderNumber={result.orderNumber} />}
    </div>
  )
}

export default GuestOrderDetailPage
