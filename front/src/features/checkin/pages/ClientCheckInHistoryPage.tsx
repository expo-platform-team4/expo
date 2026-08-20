'use client'

import { useSearchParams } from 'next/navigation'
import { useState } from 'react'

import { Badge, Button, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import type { CheckInHistoryRow } from '../api'
import { ExpoSwitcher } from '../components/ExpoSwitcher'
import { useCheckInHistory } from '../hooks'

const parseExpoId = (raw: string | null): number | null => {
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

type Filter = 'ALL' | 'SUCCESS' | 'FAILED'

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'SUCCESS', label: '성공' },
  { value: 'FAILED', label: '실패' },
]

const RESULT_BADGE: Record<CheckInHistoryRow['result'], { label: string; variant: BadgeVariant }> =
  {
    SUCCESS: { label: '성공', variant: 'success' },
    ALREADY_USED: { label: '이미 사용됨', variant: 'error' },
    CANCELED_TICKET: { label: '취소된 티켓', variant: 'error' },
    WRONG_EXPO: { label: '다른 박람회', variant: 'error' },
  }

const METHOD_LABEL: Record<CheckInHistoryRow['method'], string> = {
  QR: 'QR 스캔',
  MANUAL_CODE: '코드 직접 입력',
}

/**
 * `/client/check-in/history` — 체크인 이력. Function.md 5절, 디자인 없이 자유 생성한 화면.
 *
 * "체크인 이력 목록. 필터(성공/실패) 지원". 서버(`ClientCheckInController.history`)는
 * `page`/`size` 만 받고 결과별 필터 파라미터가 없어서, 최근 것부터 서버 최대치(`size=100`)를
 * 받아 **화면에서 필터링한다** — `features/client` 의 정산 목록과 같은 판단이다(박람회당
 * 이력이 아직 그 정도 규모다). `totalCount` 가 100 을 넘으면 그 사실을 명시한다
 * (Function.md 7절 — 근사치임을 감춘 빈 목록으로 두지 않는다).
 */
const ClientCheckInHistoryPage = () => {
  const expoId = parseExpoId(useSearchParams().get('expoId'))
  const [filter, setFilter] = useState<Filter>('ALL')
  const historyQuery = useCheckInHistory(expoId)

  const filteredItems = historyQuery.data?.items.filter((item) => {
    if (filter === 'ALL') return true
    if (filter === 'SUCCESS') return item.result === 'SUCCESS'
    return item.result !== 'SUCCESS'
  })

  return (
    <div>
      <PageHeader
        title="체크인 이력"
        description="입장 처리 이력입니다. 거절된 시도도 함께 나옵니다."
      />

      <div className="mb-6">
        <ExpoSwitcher expoId={expoId} basePath="/client/check-in/history" />
      </div>

      {expoId === null ? (
        <EmptyState
          title="박람회를 선택해 주세요"
          description="위에서 이력을 확인할 박람회를 골라 주세요."
        />
      ) : historyQuery.isPending ? (
        <LoadingBlock label="체크인 이력을 불러오는 중입니다" />
      ) : historyQuery.isError ? (
        <ErrorState error={historyQuery.error} onRetry={() => historyQuery.refetch()} />
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between gap-4">
            <div className="flex gap-2">
              {FILTERS.map((item) => (
                <Button
                  key={item.value}
                  type="button"
                  size="sm"
                  variant={filter === item.value ? 'primary' : 'secondary'}
                  onClick={() => setFilter(item.value)}
                >
                  {item.label}
                </Button>
              ))}
            </div>
            <p className="text-label-sm text-on-surface-variant">
              전체 {historyQuery.data.totalCount.toLocaleString('ko-KR')}건 중 최근{' '}
              {historyQuery.data.items.length.toLocaleString('ko-KR')}건
              {historyQuery.data.totalCount > historyQuery.data.items.length &&
                ' (오래된 이력은 표시되지 않습니다)'}
            </p>
          </div>

          {filteredItems && filteredItems.length === 0 ? (
            <EmptyState title="조건에 맞는 이력이 없습니다" />
          ) : (
            <div className="divide-outline-variant border-outline-variant divide-y rounded-md border">
              {filteredItems?.map((item) => (
                <HistoryRow key={item.id} item={item} />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const HistoryRow = ({ item }: { item: CheckInHistoryRow }) => {
  const badge = RESULT_BADGE[item.result]
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
      <div>
        <div className="flex items-center gap-2">
          <span className="text-body-md text-on-surface font-mono">{item.ticketCode}</span>
          <Badge variant={badge.variant}>{badge.label}</Badge>
        </div>
        <p className="text-label-sm text-on-surface-variant mt-1">
          {METHOD_LABEL[item.method]} · {formatDateTime(item.checkedAt)}
          {item.detail && ` · ${item.detail}`}
        </p>
      </div>
    </div>
  )
}

export default ClientCheckInHistoryPage
