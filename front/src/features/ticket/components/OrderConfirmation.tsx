import type { ReactNode } from 'react'

import { Card, CardTitle, EmptyState } from '@/components/ui'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime } from '@/lib/date'

import type { TicketOrder } from '../api'
import { OrderStatusBadge } from './OrderStatusBadge'

/**
 * 주문 생성 직후 화면. 화면 1·2(회원·비회원 예매)가 공유한다.
 *
 * 결제 연동이 없다(작업 지시의 "Critical scope context" — `backend/.../payment`,
 * `backend/.../refund` 는 `package-info.java` 뿐이고 `TicketOrderStatus` 를 `PAID` 로
 * 옮기는 엔드포인트가 어디에도 없다, GitHub 이슈 #107). 그래서 여기서 끝을 "결제하기"
 * 버튼이 아니라 `EmptyState notReady` 로 정직하게 닫는다 — 가짜 결제 위젯을 만들면 검증하는
 * 사람이 실제로 붙은 줄 오해한다.
 */
export const OrderConfirmation = ({
  order,
  extra,
}: {
  order: TicketOrder
  /** 비회원 화면의 예약자 이름·나이처럼, 주문 정보 위에 덧붙일 내용. */
  extra?: ReactNode
}) => (
  <div className="flex flex-col gap-4">
    <Card className="flex flex-col gap-4">
      <div className="flex items-start justify-between gap-2">
        <div>
          <CardTitle className="mb-1">주문이 생성되었습니다</CardTitle>
          <p className="text-label-md text-on-surface-variant font-mono">{order.orderNumber}</p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      {extra}

      <ul className="divide-outline-variant divide-y">
        {order.items.map((item) => (
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
          <span>{formatCurrency(order.ticketSubtotalAmount)}</span>
        </div>
        <div className="text-label-md text-on-surface-variant flex justify-between">
          <span>예약 수수료</span>
          <span>{formatCurrency(order.bookingFeeAmount)}</span>
        </div>
        <div className="text-title-lg text-on-surface flex justify-between font-semibold">
          <span>총 결제 금액</span>
          <span>{formatCurrency(order.totalAmount)}</span>
        </div>
      </div>

      {order.status === 'PENDING' && (
        <p className="text-label-sm text-on-surface-variant">
          {formatDateTime(order.expiresAt)} 까지 결제를 완료하지 않으면 예약이 자동으로 취소됩니다.
        </p>
      )}
    </Card>

    <EmptyState
      notReady
      title="결제"
      description="결제 연동 준비 중입니다. 지금은 주문만 생성되고 결제는 진행할 수 없습니다."
    />
  </div>
)
