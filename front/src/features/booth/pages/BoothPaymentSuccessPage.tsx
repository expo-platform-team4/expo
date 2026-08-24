'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { useEffect, useRef } from 'react'

import { Card, CardTitle, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'
import { formatCurrency } from '@/lib/currency'

import { useConfirmBoothPayment } from '../hooks'

/**
 * `/client/booth-orders/payments/success` — 토스 결제창이 승인 성공 후 돌려보내는 콜백.
 *
 * 쿼리파라미터(`paymentKey`·`orderId`·`amount`)는 토스가 그대로 실어 보낸 값이다. **여기서
 * 결제가 끝난 게 아니다** — 이 페이지가 `POST /api/client/booth-payments/confirm` 을 직접
 * 호출해야 실제로 승인이 확정된다(`BoothCheckout.tsx` 상단 주석 참고).
 *
 * 사용자가 버튼을 누르는 시점이 없는 리다이렉트 전용 페이지라, 마운트되자마자 자동으로
 * 확정을 시도한다.
 */
const BoothPaymentSuccessPage = () => {
  const searchParams = useSearchParams()
  const confirm = useConfirmBoothPayment()
  const requestedRef = useRef(false)

  const paymentKey = searchParams.get('paymentKey')
  const orderId = searchParams.get('orderId')
  const amount = searchParams.get('amount')

  useEffect(() => {
    if (requestedRef.current) return
    if (!paymentKey || !orderId || !amount) return
    requestedRef.current = true
    confirm.mutate({ paymentKey, orderId, amount: Number(amount) })
    // 마운트 시 한 번만 — 쿼리파라미터는 이 페이지 생애 동안 바뀌지 않는다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!paymentKey || !orderId || !amount) {
    return (
      <div>
        <PageHeader title="결제 확인" />
        <ErrorState error={new Error('결제 정보가 올바르지 않습니다.')} />
      </div>
    )
  }

  return (
    <div>
      <PageHeader title="결제 확인" />
      {confirm.isPending || confirm.isIdle ? (
        <LoadingBlock label="결제를 확정하는 중입니다" />
      ) : confirm.isError ? (
        <ErrorState
          error={confirm.error}
          onRetry={() => confirm.mutate({ paymentKey, orderId, amount: Number(amount) })}
        />
      ) : (
        <Card className="flex flex-col gap-4">
          <CardTitle>결제가 완료되었습니다</CardTitle>
          {confirm.data.approvedAmount != null && (
            <p className="text-title-lg text-on-surface font-semibold">
              {formatCurrency(Number(confirm.data.approvedAmount))}
            </p>
          )}
          <Link href="/client/participations" className="text-secondary text-label-md underline">
            참여 신청 내역으로 이동
          </Link>
        </Card>
      )}
    </div>
  )
}

export default BoothPaymentSuccessPage
