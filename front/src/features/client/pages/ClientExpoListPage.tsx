'use client'

import { useRouter } from 'next/navigation'

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
import { formatCurrency } from '@/lib/currency'
import { formatDate } from '@/lib/date'

import {
  type ClientDashboardExpo,
  type ClientSettlement,
  EXPO_EVENT_STATUS_LABEL,
  EXPO_REVIEW_STATUS_LABEL,
  SETTLEMENT_STATUS_LABEL,
} from '../api'
import { useClientMyExpos, useClientSettlements } from '../hooks'

const EVENT_STATUS_BADGE: Record<ClientDashboardExpo['eventStatus'], BadgeVariant> = {
  SCHEDULED: 'info',
  ONGOING: 'success',
  ENDED: 'neutral',
  CANCELED: 'error',
}

const REVIEW_STATUS_BADGE: Record<ClientDashboardExpo['reviewStatus'], BadgeVariant> = {
  DRAFT: 'neutral',
  UNDER_REVIEW: 'info',
  REJECTED: 'error',
  APPROVED: 'success',
}

/** `/client/expos`. Function.md 3절 — "내가 연 박람회 목록 + 매출 요약". */
const ClientExpoListPage = () => {
  const router = useRouter()
  const exposQuery = useClientMyExpos()
  const settlementsQuery = useClientSettlements()

  const isPending = exposQuery.isPending || settlementsQuery.isPending
  const isError = exposQuery.isError || settlementsQuery.isError
  const firstError = exposQuery.error ?? settlementsQuery.error

  const refetchAll = () => {
    exposQuery.refetch()
    settlementsQuery.refetch()
  }

  const header = (
    <PageHeader
      title="내 박람회"
      description="내가 연 박람회 목록과 박람회별 매출 요약입니다."
      action={<Button onClick={() => router.push('/client/expos/new')}>박람회 개최 신청</Button>}
    />
  )

  if (isError) {
    return (
      <div>
        {header}
        <ErrorState error={firstError} onRetry={refetchAll} />
      </div>
    )
  }

  if (isPending) {
    return (
      <div>
        {header}
        <LoadingBlock label="내 박람회를 불러오는 중입니다" />
      </div>
    )
  }

  const expos = exposQuery.data
  const settlementByExpoId = new Map<number, ClientSettlement>(
    settlementsQuery.data.items.map((settlement) => [settlement.expoId, settlement])
  )

  if (expos.length === 0) {
    return (
      <div>
        {header}
        <EmptyState
          title="등록한 박람회가 없습니다"
          description="박람회 개최 신청으로 첫 박람회를 등록해 보세요."
          action={
            <Button onClick={() => router.push('/client/expos/new')}>박람회 개최 신청</Button>
          }
        />
      </div>
    )
  }

  return (
    <div>
      {header}
      <div className="flex flex-col gap-4">
        {expos.map((expo) => {
          const settlement = settlementByExpoId.get(expo.expoId)
          return (
            <Card key={expo.expoId}>
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-title-lg text-on-surface font-semibold">{expo.title}</h3>
                    <Badge variant={EVENT_STATUS_BADGE[expo.eventStatus]}>
                      {EXPO_EVENT_STATUS_LABEL[expo.eventStatus]}
                    </Badge>
                    <Badge variant={REVIEW_STATUS_BADGE[expo.reviewStatus]}>
                      {EXPO_REVIEW_STATUS_LABEL[expo.reviewStatus]}
                    </Badge>
                  </div>
                  <p className="text-body-sm text-on-surface-variant mt-2">
                    행사 기간 {formatDate(expo.eventStartAt)} ~ {formatDate(expo.eventEndAt)}
                  </p>
                  <p className="text-body-sm text-on-surface-variant">
                    판매 기간 {formatDate(expo.salesStartAt)} ~ {formatDate(expo.salesEndAt)} · 등록
                    티켓 상품 {expo.ticketProductCount.toLocaleString('ko-KR')}종
                  </p>
                </div>

                <div className="border-outline-variant min-w-[220px] rounded-md border px-4 py-3">
                  <p className="text-label-sm text-on-surface-variant font-semibold">매출 요약</p>
                  {settlement ? (
                    <div className="mt-2 space-y-1">
                      <p className="text-body-sm text-on-surface flex justify-between gap-4">
                        <span>티켓 순매출</span>
                        <span>{formatCurrency(settlement.netTicketSalesAmount)}</span>
                      </p>
                      <p className="text-body-sm text-on-surface flex justify-between gap-4">
                        <span>부스 매출</span>
                        <span>{formatCurrency(settlement.grossBoothSalesAmount)}</span>
                      </p>
                      <p className="text-body-sm text-on-surface flex justify-between gap-4 font-medium">
                        <span>받을 금액</span>
                        <span>{formatCurrency(settlement.remittanceDueAmount)}</span>
                      </p>
                      <Badge variant="info" className="mt-1">
                        {SETTLEMENT_STATUS_LABEL[settlement.status] ?? settlement.status}
                      </Badge>
                    </div>
                  ) : (
                    <p className="text-body-sm text-on-surface-variant mt-2">
                      아직 정산이 집계되지 않았습니다.
                    </p>
                  )}
                </div>
              </div>
            </Card>
          )
        })}
      </div>
    </div>
  )
}

export default ClientExpoListPage
