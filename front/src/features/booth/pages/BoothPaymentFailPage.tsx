'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'

import { Card, CardTitle, PageHeader } from '@/components/ui'

/**
 * `/client/booth-orders/payments/fail` — 토스 결제창이 승인 실패·취소 시 돌려보내는 콜백.
 *
 * 티켓 결제(`PaymentFailPage`)와 달리 별도로 부를 백엔드 "실패" 엔드포인트가 없다 —
 * `ClientBoothPaymentController` 에 `/fail` 이 없다. 결제창까지 갔다가 포기해도 부스 상품
 * 임시 확보는 주문 만료(15분)나 참여 신청 상세 화면에서의 "주문 취소" 로 풀리므로, 여기서는
 * 실패 사유만 보여주고 돌아갈 링크를 준다.
 */
const BoothPaymentFailPage = () => {
  const searchParams = useSearchParams()
  const message = searchParams.get('message')

  return (
    <div>
      <PageHeader title="결제 실패" />
      <Card className="flex flex-col gap-3">
        <CardTitle>결제가 완료되지 않았습니다</CardTitle>
        <p className="text-body-md text-on-surface-variant">
          {message ?? '결제 진행 중 문제가 발생했습니다.'}
        </p>
        <p className="text-label-sm text-on-surface-variant">
          주문은 아직 결제 대기 상태입니다. 참여 신청 내역에서 다시 시도하거나 주문을 취소할 수
          있습니다.
        </p>
        <Link href="/client/participations" className="text-secondary text-label-md underline">
          참여 신청 내역으로 이동
        </Link>
      </Card>
    </div>
  )
}

export default BoothPaymentFailPage
