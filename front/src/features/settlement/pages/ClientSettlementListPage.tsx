'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
  Select,
} from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { SETTLEMENT_STATUS_LABEL, type ClientSettlement } from '@/features/client/api'
import { formatCurrency } from '@/lib/currency'
import { formatDate } from '@/lib/date'

import { useClientSettlements } from '../hooks'

const PAGE_SIZE = 10

const STATUS_BADGE: Record<string, BadgeVariant> = {
  WAITING: 'neutral',
  CALCULATED: 'info',
  UNDER_REVIEW: 'info',
  CONFIRMED: 'success',
  REMITTANCE_PENDING: 'info',
  REMITTED: 'success',
  ON_HOLD: 'error',
}

/**
 * `/client/settlements`. Function.md 3절 — "정산 리포트" 목록.
 *
 * 목록 API 는 상태 필터 파라미터가 없다(`ClientSettlementController` 에는 page·size 뿐이다) —
 * 상태 드롭다운은 현재 페이지에 불러온 항목만 걸러낸다.
 */
const ClientSettlementListPage = () => {
  const router = useRouter()
  const [page, setPage] = useState(0) // 0-based. Spec.md 2절 — 서버로 넘기는 값은 항상 0-based
  const [statusFilter, setStatusFilter] = useState<string>('ALL')

  const { data, isPending, isError, error, refetch } = useClientSettlements(page, PAGE_SIZE)

  const header = (
    <PageHeader title="정산 리포트" description="박람회별 정산 현황과 받을 금액을 확인합니다." />
  )

  if (isError) {
    return (
      <div>
        {header}
        <ErrorState error={error} onRetry={() => refetch()} />
      </div>
    )
  }

  if (isPending) {
    return (
      <div>
        {header}
        <LoadingBlock label="정산 목록을 불러오는 중입니다" />
      </div>
    )
  }

  const items = data.items.filter(
    (settlement) => statusFilter === 'ALL' || settlement.status === statusFilter
  )
  const totalPages = Math.max(1, Math.ceil(data.totalCount / data.size))

  return (
    <div>
      {header}

      <div className="mb-4 max-w-xs">
        <Select
          label="상태"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="ALL">전체</option>
          {Object.entries(SETTLEMENT_STATUS_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </div>

      {items.length === 0 ? (
        <EmptyState
          title="정산 내역이 없습니다"
          description="조건에 맞는 정산 건이 아직 없습니다."
        />
      ) : (
        <div className="flex flex-col gap-4">
          {items.map((settlement: ClientSettlement) => (
            <Card
              key={settlement.settlementId}
              className="hover:border-secondary cursor-pointer border border-transparent transition-colors"
              onClick={() => router.push(`/client/settlements/${settlement.settlementId}`)}
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-title-lg text-on-surface font-semibold">
                      {settlement.expoTitle}
                    </h3>
                    <Badge variant={STATUS_BADGE[settlement.status] ?? 'neutral'}>
                      {SETTLEMENT_STATUS_LABEL[settlement.status] ?? settlement.status}
                    </Badge>
                  </div>
                  <p className="text-body-sm text-on-surface-variant mt-2">
                    행사 종료 {formatDate(settlement.eventEndAt)} · 정산 기한{' '}
                    {formatDate(settlement.settlementDueAt)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-label-sm text-on-surface-variant font-semibold">받을 금액</p>
                  <p className="text-title-lg text-on-surface font-semibold">
                    {formatCurrency(settlement.remittanceDueAmount)}
                  </p>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-3">
          <Button
            variant="secondary"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
          >
            이전
          </Button>
          <span className="text-body-sm text-on-surface-variant">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((prev) => prev + 1)}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  )
}

export default ClientSettlementListPage
