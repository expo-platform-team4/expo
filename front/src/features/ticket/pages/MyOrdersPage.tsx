'use client'

import { useState } from 'react'

import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { TossCheckout } from '@/features/payment/components/TossCheckout'
import { MemberRefundPanel } from '@/features/refund/components/MemberRefundPanel'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime } from '@/lib/date'

import type { MemberOrder, TicketOrderStatus } from '../api'
import { useMyOrders } from '../hooks'

const STATUS_LABEL: Record<TicketOrderStatus, string> = {
  PENDING: '결제 대기',
  PAID: '결제 완료',
  CANCELED: '취소됨',
  PAYMENT_FAILED: '결제 실패',
  EXPIRED: '만료됨',
}

const STATUS_VARIANT: Record<TicketOrderStatus, BadgeVariant> = {
  PENDING: 'info',
  PAID: 'success',
  CANCELED: 'neutral',
  PAYMENT_FAILED: 'error',
  EXPIRED: 'neutral',
}

/** 결제 상태(`ticket_payments.status`) 한글 라벨. 명세에 없는 값이 오면 원문을 그대로 보여준다. */
const PAYMENT_STATUS_LABEL: Record<string, string> = {
  READY: '결제 준비중',
  IN_PROGRESS: '결제 진행중',
  DONE: '결제 완료',
  FAILED: '결제 실패',
  CANCELED: '결제 취소됨',
}

/** 환불 상태(`ticket_refunds.status`) 한글 라벨. 명세에 없는 값이 오면 원문을 그대로 보여준다. */
const REFUND_STATUS_LABEL: Record<string, string> = {
  REQUESTED: '환불 요청됨',
  PROCESSING: '환불 처리중',
  COMPLETED: '환불 완료',
  FAILED: '환불 실패',
}

/** `/mypage/orders`. A-API-019, A-API-020 — `GET /api/users/me/orders`. */
const MyOrdersPage = () => {
  const { data, isPending, error, refetch } = useMyOrders()

  return (
    <div>
      <PageHeader title="예매 내역" description="주문·결제 내역을 확인합니다." />

      {isPending && <LoadingBlock label="예매 내역을 불러오는 중입니다" />}
      {error && <ErrorState error={error} onRetry={refetch} />}
      {!isPending && !error && data && data.length === 0 && (
        <EmptyState title="예매 내역이 없습니다" description="아직 구매한 티켓이 없습니다." />
      )}
      {!isPending && !error && data && data.length > 0 && (
        <ul className="space-y-4">
          {data.map((order) => (
            <li key={order.orderId}>
              <OrderCard order={order} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

const OrderCard = ({ order }: { order: MemberOrder }) => {
  const [checkoutOpen, setCheckoutOpen] = useState(false)

  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-title-lg text-on-surface font-semibold">
            {order.expoTitle ?? '박람회 정보 없음'}
          </p>
          <p className="text-body-sm text-on-surface-variant mt-1">
            주문번호 {order.orderNumber} · {formatDateTime(order.createdAt)}
          </p>
        </div>
        <Badge variant={STATUS_VARIANT[order.orderStatus]}>{STATUS_LABEL[order.orderStatus]}</Badge>
      </div>

      <dl className="text-body-sm text-on-surface-variant mt-4 grid grid-cols-2 gap-y-1 sm:grid-cols-4">
        <div>
          <dt className="text-label-sm">수량</dt>
          <dd className="text-on-surface">{order.totalQuantity}매</dd>
        </div>
        <div>
          <dt className="text-label-sm">티켓 금액</dt>
          <dd className="text-on-surface">{formatCurrency(order.ticketSubtotalAmount)}</dd>
        </div>
        <div>
          <dt className="text-label-sm">예매 수수료</dt>
          <dd className="text-on-surface">{formatCurrency(order.bookingFeeAmount)}</dd>
        </div>
        <div>
          <dt className="text-label-sm">총 결제 금액</dt>
          <dd className="text-on-surface font-semibold">{formatCurrency(order.totalAmount)}</dd>
        </div>
      </dl>

      {(order.paymentStatus || order.refundStatus || order.refundable) && (
        <div className="mt-3 flex flex-wrap gap-2">
          {order.paymentStatus && (
            <Badge variant="neutral">
              {PAYMENT_STATUS_LABEL[order.paymentStatus] ?? order.paymentStatus}
            </Badge>
          )}
          {order.refundStatus && (
            <Badge variant="neutral">
              {REFUND_STATUS_LABEL[order.refundStatus] ?? order.refundStatus}
            </Badge>
          )}
          {order.refundable && <Badge variant="info">환불 가능</Badge>}
        </div>
      )}

      {/* PENDING(결제 대기)은 주문 생성 직후에만 결제할 수 있는 게 아니다 — 예매 내역으로
          다시 들어왔을 때도 만료 전까지는 이어서 결제할 수 있어야 한다. */}
      {order.orderStatus === 'PENDING' && !checkoutOpen && (
        <Button className="mt-4" onClick={() => setCheckoutOpen(true)}>
          다시 결제하기
        </Button>
      )}
      {order.orderStatus === 'PENDING' && checkoutOpen && (
        <div className="mt-4">
          <TossCheckout orderNumber={order.orderNumber} />
        </div>
      )}

      {order.refundable && <MemberRefundPanel orderId={order.orderId} />}
    </Card>
  )
}

export default MyOrdersPage
