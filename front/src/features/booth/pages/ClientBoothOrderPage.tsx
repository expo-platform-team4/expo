'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { BoothCheckout } from '../components/BoothCheckout'
import { useCancelBoothOrder, useMyBoothOrder } from '../hooks'
import type { BoothOrderStatus } from '../api'

const statusLabel = (status: BoothOrderStatus): string => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return '결제 대기'
    case 'PAYMENT_COMPLETED':
      return '결제 완료'
    case 'FAILED':
      return '결제 실패'
    case 'CANCELED':
      return '취소됨'
    case 'EXPIRED':
      return '만료됨'
    default:
      return status
  }
}

const statusVariant = (status: BoothOrderStatus): 'success' | 'neutral' | 'error' | 'info' => {
  switch (status) {
    case 'PAYMENT_COMPLETED':
      return 'success'
    case 'PENDING_PAYMENT':
      return 'info'
    case 'FAILED':
    case 'CANCELED':
    case 'EXPIRED':
      return 'error'
    default:
      return 'neutral'
  }
}

/**
 * `/client/booth-orders/[orderId]`. CLIENT 전용.
 *
 * 참여 신청 상세(`ClientParticipationDetailPage`)에서 "주문하고 결제하기"를 누르면 이
 * 화면으로 온다. 주문이 `PENDING_PAYMENT` 상태일 때만 결제창(`BoothCheckout`)을 보여준다 —
 * 이미 결제됐거나 만료·취소된 주문은 상태만 보여주고 되돌아갈 링크를 준다.
 *
 * 주문은 15분 뒤 만료된다(백엔드 배치가 정리한다). 화면을 띄워둔 채 시간이 지나는 경우를
 * 대비해 15초마다 다시 불러 만료 여부를 반영한다.
 */
const ClientBoothOrderPage = () => {
  const params = useParams<{ orderId: string }>()
  const orderId = Number(params.orderId)
  const { data: order, isPending, isError, error, refetch } = useMyBoothOrder(orderId)
  const cancelMutation = useCancelBoothOrder()

  if (isPending) {
    return <LoadingBlock label="주문을 불러오는 중입니다" />
  }
  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  return (
    <div>
      <PageHeader title="부스 상품 주문" description={`주문번호 ${order.orderNumber}`} />

      <div className="flex flex-col gap-6">
        <Card className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <CardTitle className="mb-0">주문 정보</CardTitle>
            <Badge variant={statusVariant(order.status)}>{statusLabel(order.status)}</Badge>
          </div>
          <dl className="text-body-md text-on-surface grid grid-cols-1 gap-2 sm:grid-cols-2">
            <div>
              <dt className="text-label-sm text-on-surface-variant">결제 금액</dt>
              <dd className="text-title-lg font-semibold">
                {formatCurrency(Number(order.totalAmount))}
              </dd>
            </div>
            <div>
              <dt className="text-label-sm text-on-surface-variant">
                {order.status === 'PENDING_PAYMENT' ? '결제 마감' : '결제 일시'}
              </dt>
              <dd>
                {order.paidAt ? formatDateTime(order.paidAt) : formatDateTime(order.expiresAt)}
              </dd>
            </div>
          </dl>

          {order.status === 'PENDING_PAYMENT' && (
            <div>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                loading={cancelMutation.isPending}
                onClick={() => {
                  if (window.confirm('주문을 취소할까요? 임시 확보한 부스 상품이 풀립니다.')) {
                    cancelMutation.mutate(orderId)
                  }
                }}
              >
                주문 취소
              </Button>
              {cancelMutation.isError && (
                <p className="text-label-sm text-error mt-1">
                  {getErrorMessage(cancelMutation.error)}
                </p>
              )}
            </div>
          )}
        </Card>

        {order.status === 'PENDING_PAYMENT' ? (
          <BoothCheckout orderId={order.id} />
        ) : (
          <Link href="/client/participations" className="text-secondary text-label-md underline">
            참여 신청 내역으로 이동
          </Link>
        )}
      </div>
    </div>
  )
}

export default ClientBoothOrderPage
