'use client'

import { ANONYMOUS, loadTossPayments } from '@tosspayments/tosspayments-sdk'
import type { TossPaymentsPayment } from '@tosspayments/tosspayments-sdk'
import { useEffect, useRef, useState } from 'react'

import { Button, Card, CardTitle, ErrorState, LoadingBlock } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useInitiateTicketPayment } from '../hooks'
import type { TicketPaymentInitiation } from '../api'

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY

/**
 * 토스 결제창. 회원·비회원 주문 화면(`OrderConfirmation`)이 공유한다.
 *
 * **결제위젯(`tossPayments.widgets()`)이 아니라 결제창(구버전, `tossPayments.payment()`)을
 * 쓴다.** 지금 발급된 테스트 클라이언트 키가 "API 개별 연동" 키라 위젯 연동을 지원하지
 * 않는다 — `widgets()`를 호출하면 "결제위젯 연동 키의 클라이언트 키로 SDK를 연동해주세요"
 * 에러가 난다. 결제창 방식은 카드/간편결제 통합 UI를 토스가 직접 새 창(iframe)으로 띄워주므로
 * 우리 쪽에서 결제수단·약관 위젯을 렌더링할 selector가 따로 필요 없다.
 *
 * 인증 방식이 갈리는 건 이 컴포넌트가 아니라 백엔드다 — 회원 주문이면 `api.ts` 인터셉터가
 * 붙인 JWT 로, 비회원 주문이면 예측 불가능한 `orderNumber` 자체로 소유자를 확인한다.
 *
 * 흐름: `initiate` 로 결제 시도를 만들고 → 결제창 객체를 준비 → "결제하기" 클릭 시
 * `requestPayment`(redirect 방식)를 불러 토스 결제창으로 이동한다. 승인 확정(`confirm`)은
 * 토스가 되돌려주는 `/orders/payments/success` 페이지에서 처리한다 — 여기서 하지 않는다.
 */
export const TossCheckout = ({
  orderNumber,
  onError,
}: {
  orderNumber: string
  /** 초기화 실패(결제 시도 생성 실패 등) 상위에 알림. */
  onError?: (error: unknown) => void
}) => {
  const paymentRef = useRef<TossPaymentsPayment | null>(null)
  const initiationRef = useRef<TicketPaymentInitiation | null>(null)
  /**
   * React 18 Strict Mode(dev)는 마운트 시 effect를 "실행 → cleanup → 실행"으로 일부러
   * 두 번 돌린다. `setupStartedRef`는 컴포넌트 인스턴스 생애 동안 값이 유지되므로, 이 ref
   * 하나로 "이미 시작했으면 다시 시작하지 않는다"를 보장한다 — `cancelled` 플래그로 도중에
   * 끊는 방식은 첫 번째 실행의 cleanup이 (StrictMode가 흉내내는 가짜 언마운트로) 호출되며
   * `initiate` 완료 직후 조용히 중단시키는 문제가 있었다(에러 없이 로딩에서 멈춘 것처럼 보임).
   */
  const setupStartedRef = useRef(false)

  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  const initiate = useInitiateTicketPayment()

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
        const initiation = await initiate.mutateAsync(orderNumber)
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
    // orderNumber 가 바뀌는 경우는 없다(주문 확인 화면에 고정 진입) — 최초 1회만 실행한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNumber])

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
        orderId: initiation.orderId,
        orderName: initiation.orderName,
        successUrl: `${window.location.origin}/orders/payments/success`,
        failUrl: `${window.location.origin}/orders/payments/fail`,
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
