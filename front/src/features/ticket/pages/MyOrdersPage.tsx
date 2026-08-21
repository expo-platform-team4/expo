'use client'

import { Badge, Card, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
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

const OrderCard = ({ order }: { order: MemberOrder }) => (
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
        {order.paymentStatus && <Badge variant="neutral">결제 {order.paymentStatus}</Badge>}
        {order.refundStatus && <Badge variant="neutral">환불 {order.refundStatus}</Badge>}
        {order.refundable && <Badge variant="info">환불 가능</Badge>}
      </div>
    )}
  </Card>
)

export default MyOrdersPage
