'use client'

import { useParams, useRouter } from 'next/navigation'

import { Badge, Button, Card, CardTitle, ErrorState, LoadingBlock } from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { SETTLEMENT_STATUS_LABEL } from '@/features/client/api'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime } from '@/lib/date'

import { REMITTANCE_STATUS_LABEL, SETTLEMENT_ITEM_TYPE_LABEL, type SettlementItem } from '../api'
import { useClientSettlementDetail } from '../hooks'

const STATUS_BADGE: Record<string, BadgeVariant> = {
  WAITING: 'neutral',
  CALCULATED: 'info',
  UNDER_REVIEW: 'info',
  CONFIRMED: 'success',
  REMITTANCE_PENDING: 'info',
  REMITTED: 'success',
  ON_HOLD: 'error',
}

/** `/client/settlements/{id}`. Function.md 3절 — 정산 상세, 티켓/부스 구분 리포트. */
const ClientSettlementDetailPage = () => {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const settlementId = Number(params.id)
  const isValidId = Number.isFinite(settlementId)

  const {
    data: detail,
    isPending,
    isError,
    error,
    refetch,
  } = useClientSettlementDetail(isValidId ? settlementId : null)

  if (!isValidId) {
    return <ErrorState error={new Error('잘못된 정산 주소입니다.')} />
  }

  if (isPending) {
    return <LoadingBlock label="정산 상세를 불러오는 중입니다" />
  }

  if (isError) {
    // Spec.md 5절 — 내 정산이 아니면 404. "권한 없음" 이 아니라 서버 message 를 그대로 보여준다.
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  const { settlement, items } = detail
  const includedItems = items.filter((item) => item.includedInRemittance)
  const referenceItems = items.filter((item) => !item.includedInRemittance)

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <Button variant="ghost" size="sm" onClick={() => router.push('/client/settlements')}>
          ← 목록으로
        </Button>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <h1 className="text-headline-sm text-on-background font-semibold">
            {settlement.expoTitle}
          </h1>
          <Badge variant={STATUS_BADGE[settlement.status] ?? 'neutral'}>
            {SETTLEMENT_STATUS_LABEL[settlement.status] ?? settlement.status}
          </Badge>
        </div>
        <p className="text-body-sm text-on-surface-variant mt-1">
          행사 종료 {formatDateTime(settlement.eventEndAt)} · 정산 기한{' '}
          {formatDateTime(settlement.settlementDueAt)}
        </p>
      </div>

      <Card>
        <CardTitle>정산 요약</CardTitle>
        <dl className="text-body-md text-on-surface grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-label-sm text-on-surface-variant">티켓 판매원금</dt>
            <dd>{formatCurrency(settlement.grossTicketSalesAmount)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">티켓 환불원금</dt>
            <dd>{formatCurrency(settlement.ticketRefundAmount)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">티켓 순매출</dt>
            <dd>{formatCurrency(settlement.netTicketSalesAmount)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">부스 매출</dt>
            <dd>{formatCurrency(settlement.grossBoothSalesAmount)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">조정 합계</dt>
            <dd>{formatCurrency(settlement.adjustmentAmount)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant font-semibold">받을 금액</dt>
            <dd className="font-semibold">{formatCurrency(settlement.remittanceDueAmount)}</dd>
          </div>
        </dl>
        <p className="text-label-sm text-on-surface-variant mt-4">
          예매 수수료 {formatCurrency(settlement.bookingFeeNetAmount)}는 구매자가 판매원금 위에
          추가로 낸 플랫폼 몫이라 받을 금액에 포함되지 않습니다.
        </p>
      </Card>

      <Card>
        <CardTitle>송금 현황</CardTitle>
        <dl className="text-body-md text-on-surface grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-label-sm text-on-surface-variant">송금 상태</dt>
            <dd>
              {REMITTANCE_STATUS_LABEL[settlement.remittanceStatus] ?? settlement.remittanceStatus}
            </dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">실제 송금액</dt>
            <dd>
              {settlement.remittedAmount !== null
                ? formatCurrency(settlement.remittedAmount)
                : '아직 송금되지 않았습니다'}
            </dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">송금 시각</dt>
            <dd>{settlement.remittedAt ? formatDateTime(settlement.remittedAt) : '-'}</dd>
          </div>
        </dl>
      </Card>

      <Card>
        <CardTitle>정산금 포함 항목</CardTitle>
        <SettlementItemTable items={includedItems} emptyLabel="정산금에 포함된 항목이 없습니다." />
      </Card>

      <Card>
        <CardTitle>참고 항목 (정산금 미포함)</CardTitle>
        <SettlementItemTable items={referenceItems} emptyLabel="참고 항목이 없습니다." />
      </Card>
    </div>
  )
}

const SettlementItemTable = ({
  items,
  emptyLabel,
}: {
  items: SettlementItem[]
  emptyLabel: string
}) => {
  if (items.length === 0) {
    return <p className="text-body-sm text-on-surface-variant">{emptyLabel}</p>
  }

  return (
    <div className="divide-y divide-[#f1f5f9]">
      {items.map((item, index) => (
        <div key={index} className="flex items-center justify-between gap-4 py-3">
          <div>
            <p className="text-body-sm text-on-surface font-medium">
              {SETTLEMENT_ITEM_TYPE_LABEL[item.itemType]}
            </p>
            <p className="text-label-sm text-on-surface-variant">
              {formatDateTime(item.occurredAt)}
            </p>
          </div>
          <p
            className={
              item.amount < 0
                ? 'text-body-md text-error font-semibold'
                : 'text-body-md text-on-surface font-semibold'
            }
          >
            {formatCurrency(item.amount)}
          </p>
        </div>
      ))}
    </div>
  )
}

export default ClientSettlementDetailPage
