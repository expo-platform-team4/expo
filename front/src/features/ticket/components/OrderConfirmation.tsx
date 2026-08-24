import type { ReactNode } from 'react'

import { Card, CardTitle } from '@/components/ui'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime } from '@/lib/date'
import { TossCheckout } from '@/features/payment/components/TossCheckout'

import type { TicketOrder } from '../api'
import { OrderStatusBadge } from './OrderStatusBadge'

/**
 * 주문 생성 직후 화면. 화면 1·2(회원·비회원 예매)가 공유한다.
 *
 * `status === 'PENDING'`일 때만 결제위젯(`TossCheckout`)을 띄운다 — 회원·비회원 인증
 * 방식이 다른 건 `TossCheckout`이 아니라 백엔드가 처리하므로 여기서 분기하지 않는다.
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

    {order.status === 'PENDING' && <TossCheckout orderNumber={order.orderNumber} />}
  </div>
)
