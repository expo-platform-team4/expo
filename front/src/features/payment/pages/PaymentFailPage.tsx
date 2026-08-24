'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { useEffect, useRef } from 'react'

import { Card, CardTitle, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'

import { useFailTicketPayment } from '../hooks'

/**
 * `/orders/payments/fail` — 토스 결제창이 승인 실패·취소 시 돌려보내는 콜백 페이지.
 *
 * 쿼리파라미터(`code`·`message`·`orderId`)는 토스가 실어 보낸 값이다. 사용자에게 실패
 * 사유를 보여주는 것과 별개로, `POST /api/payments/tickets/fail` 을 불러 임시확보해둔
 * 재고를 반환해야 한다 — 안 부르면 결제창까지만 갔다가 포기한 재고가 10분 만료될 때까지
 * 계속 묶여있는다.
 */
const PaymentFailPage = () => {
  const searchParams = useSearchParams()
  const fail = useFailTicketPayment()
  const requestedRef = useRef(false)

  const code = searchParams.get('code')
  const message = searchParams.get('message')
  const orderId = searchParams.get('orderId')

  useEffect(() => {
    if (requestedRef.current) return
    if (!orderId || !code) return
    requestedRef.current = true
    fail.mutate({ orderId, failureCode: code })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div>
      <PageHeader title="결제 실패" />
      {!orderId || !code ? (
        <ErrorState error={new Error('결제 정보가 올바르지 않습니다.')} />
      ) : fail.isPending || fail.isIdle ? (
        <LoadingBlock label="처리하는 중입니다" />
      ) : (
        <Card className="flex flex-col gap-3">
          <CardTitle>결제가 완료되지 않았습니다</CardTitle>
          <p className="text-body-md text-on-surface-variant">
            {message ?? '결제 진행 중 문제가 발생했습니다.'}
          </p>
          <p className="text-label-sm text-on-surface-variant">
            임시 확보했던 재고는 반환되었습니다. 다시 시도하려면 주문을 처음부터 진행해 주세요.
          </p>
          <Link href="/orders" className="text-secondary text-label-md underline">
            주문 화면으로 돌아가기
          </Link>
        </Card>
      )}
    </div>
  )
}

export default PaymentFailPage
