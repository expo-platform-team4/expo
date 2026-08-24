'use client'

import { ANONYMOUS, loadTossPayments } from '@tosspayments/tosspayments-sdk'
import type { TossPaymentsPayment } from '@tosspayments/tosspayments-sdk'
import { useEffect, useRef, useState } from 'react'

import { Button, Card, CardTitle, ErrorState, LoadingBlock } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useInitiateBoothPayment } from '../hooks'
import type { BoothPaymentInitiation } from '../api'

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY

/**
 * 부스 상품 주문의 토스 결제창. `features/payment/components/TossCheckout.tsx`(티켓 결제,
 * 다른 담당자 소유)와 같은 결제창(구버전 `tossPayments.payment()`) 패턴을 따르지만,
 * booth 도메인 안에 독립적으로 둔다 — 도메인 경계를 넘어 그 파일을 고치지 않는다.
 *
 * 흐름은 티켓 결제와 같다: `initiate` 로 결제 시도를 만들고 → 결제창 객체를 준비 →
 * "결제하기" 클릭 시 `requestPayment`(redirect 방식)로 토스 결제창으로 이동한다. 승인
 * 확정(`confirm`)은 토스가 되돌려주는 `/client/booth-orders/payments/success` 페이지가
 * 맡는다 — 여기서 하지 않는다.
 */
export const BoothCheckout = ({
  orderId,
  onError,
}: {
  orderId: number
  onError?: (error: unknown) => void
}) => {
  const paymentRef = useRef<TossPaymentsPayment | null>(null)
  const initiationRef = useRef<BoothPaymentInitiation | null>(null)
  /** StrictMode(dev)의 이중 effect 실행에도 결제 시도를 한 번만 만들기 위한 가드. */
  const setupStartedRef = useRef(false)

  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  const initiate = useInitiateBoothPayment()

  useEffect(() => {
    if (setupStartedRef.current) return
    setupStartedRef.current = true

    const setup = async () => {
      if (!TOSS_CLIENT_KEY) {
        const missingKeyError = new Error('NEXT_PUBLIC_TOSS_CLIENT_KEY 가 설정되지 않았습니다.')
        setError(missingKeyError)
        setStatus('error')
        onError?.(missingKeyError)
        return
      }

      try {
        const initiation = await initiate.mutateAsync(orderId)
        initiationRef.current = initiation

        const tossPayments = await loadTossPayments(TOSS_CLIENT_KEY)
        paymentRef.current = tossPayments.payment({ customerKey: ANONYMOUS })

        setStatus('ready')
      } catch (err) {
        setError(err)
        setStatus('error')
        onError?.(err)
      }
    }

    setup()
    // orderId 가 바뀌는 경우는 없다(주문 상세 화면에 고정 진입) — 최초 1회만 실행한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId])

  const handlePay = async () => {
    const payment = paymentRef.current
    const initiation = initiationRef.current
    if (!payment || !initiation) return

    setSubmitting(true)
    try {
      // Redirect 방식 — 성공하면 브라우저가 successUrl 로 이동하므로 이 아래는 실행되지 않는다.
      await payment.requestPayment({
        method: 'CARD',
        amount: { currency: 'KRW', value: initiation.amount },
        orderId: initiation.pgOrderId,
        orderName: initiation.orderName,
        successUrl: `${window.location.origin}/client/booth-orders/payments/success`,
        failUrl: `${window.location.origin}/client/booth-orders/payments/fail`,
      })
    } catch (err) {
      setError(err)
      setSubmitting(false)
    }
  }

  if (status === 'error') {
    return <ErrorState error={error} />
  }

  return (
    <Card className="flex flex-col gap-4">
      <CardTitle>결제</CardTitle>
      {status === 'loading' ? (
        <LoadingBlock />
      ) : (
        <>
          <p className="text-body-md text-on-surface-variant">
            &ldquo;결제하기&rdquo;를 누르면 토스 결제창(카드)이 열립니다. 테스트 환경이라 실제 카드
            없이 테스트 카드 정보로 진행할 수 있습니다.
          </p>
          <Button onClick={handlePay} loading={submitting} size="lg">
            결제하기
          </Button>
        </>
      )}
      {error != null && status === 'ready' && (
        <p className="text-error text-label-sm">
          {getErrorMessage(error, '결제 요청에 실패했습니다.')}
        </p>
      )}
    </Card>
  )
}
