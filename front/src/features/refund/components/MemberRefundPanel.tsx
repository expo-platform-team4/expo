'use client'

import { useState } from 'react'

import { Button, Textarea } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useRequestMemberRefund } from '../hooks'

/**
 * 회원 예매 내역(`MyOrdersPage`) 카드에 붙는 전체 환불 신청 패널.
 *
 * 환불 가능 여부는 이미 목록 응답의 `refundable` 로 판단됐다(호출부에서 그 값이 참일
 * 때만 렌더링한다) — 여기서 다시 확인하지 않는다.
 *
 * **`refundable` 은 신청 이후에도 계속 true 다** — `v_member_mypage_orders` 뷰가 이 값을
 * `ticket_refunds` 존재 여부가 아니라 주문 상태·행사 임박 여부·체크인 여부로만 계산한다
 * (`R__03_v_member_mypage_orders.sql`). 그래서 신청 성공 여부를 이 컴포넌트가 직접
 * 기억해 뒀다가(`requestRefund.isSuccess`) 폼을 감춘다 — 안 그러면 캐시가 갱신돼도
 * 버튼이 남아 있어 중복 신청을 시도하게 된다(백엔드는 `REFUND_ALREADY_REQUESTED` 로
 * 막지만, 애초에 누를 수 없게 하는 게 낫다).
 */
export const MemberRefundPanel = ({ orderId }: { orderId: number }) => {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const requestRefund = useRequestMemberRefund()

  if (requestRefund.isSuccess) {
    return <p className="text-body-md text-on-surface mt-4">환불 신청이 접수되었습니다.</p>
  }

  if (!open) {
    return (
      <Button variant="secondary" className="mt-4" onClick={() => setOpen(true)}>
        환불 신청
      </Button>
    )
  }

  return (
    <div className="mt-4 flex flex-col gap-3">
      <Textarea
        label="환불 사유 (선택)"
        placeholder="환불 사유를 입력해 주세요."
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        maxLength={1000}
      />
      {requestRefund.isError && (
        <p className="text-error text-label-sm">
          {getErrorMessage(requestRefund.error, '환불 신청에 실패했습니다.')}
        </p>
      )}
      <div className="flex gap-2">
        <Button
          variant="danger"
          loading={requestRefund.isPending}
          onClick={() => requestRefund.mutate({ orderId, reason })}
        >
          환불 신청 확정
        </Button>
        <Button variant="ghost" disabled={requestRefund.isPending} onClick={() => setOpen(false)}>
          취소
        </Button>
      </div>
    </div>
  )
}
