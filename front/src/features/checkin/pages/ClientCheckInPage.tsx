'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'

import { Button, Card, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'

import type { CheckInSummary } from '../api'
import { ExpoSwitcher } from '../components/ExpoSwitcher'
import { useCheckInSummary } from '../hooks'

/** `?expoId=` 를 정수로 읽는다. 잘못된 값(음수·NaN·소수)이면 "선택 안 함" 취급한다. */
const parseExpoId = (raw: string | null): number | null => {
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

/**
 * `/client/check-in` — 체크인 현황. Function.md 5절, 디자인 없이 자유 생성한 화면.
 *
 * "오늘 체크인 현황 (입장 완료 수 / 전체)". Spec.md 8절 — 스캔 직후 숫자가 바뀌어야
 * 자연스러워서 **폴링**한다(`useCheckInSummary` 의 `refetchInterval` 5초). WebSocket 은
 * 얹지 않는다.
 */
const ClientCheckInPage = () => {
  const expoId = parseExpoId(useSearchParams().get('expoId'))
  const summaryQuery = useCheckInSummary(expoId)

  return (
    <div>
      <PageHeader
        title="체크인 현황"
        description="오늘 체크인 현황입니다. 5초마다 자동으로 새로고침됩니다."
        action={
          expoId ? (
            <div className="flex gap-2">
              <Link href={`/client/check-in/scan?expoId=${expoId}`}>
                <Button>QR 스캔</Button>
              </Link>
              <Link href={`/client/check-in/history?expoId=${expoId}`}>
                <Button variant="secondary">체크인 이력</Button>
              </Link>
            </div>
          ) : undefined
        }
      />

      <div className="mb-6">
        <ExpoSwitcher expoId={expoId} basePath="/client/check-in" />
      </div>

      {expoId === null ? (
        <EmptyState
          title="박람회를 선택해 주세요"
          description="위에서 체크인 현황을 확인할 박람회를 골라 주세요."
        />
      ) : summaryQuery.isPending ? (
        <LoadingBlock label="체크인 현황을 불러오는 중입니다" />
      ) : summaryQuery.isError ? (
        <ErrorState error={summaryQuery.error} onRetry={() => summaryQuery.refetch()} />
      ) : (
        <SummaryStats summary={summaryQuery.data} />
      )}
    </div>
  )
}

const SummaryStats = ({ summary }: { summary: CheckInSummary }) => (
  <div className="flex flex-col gap-4">
    <Card className="text-center">
      <p className="text-label-md text-on-surface-variant font-medium">입장 완료 / 전체</p>
      <p className="text-display-lg text-primary mt-2 font-bold">
        {summary.checkedInCount.toLocaleString('ko-KR')}
        <span className="text-headline-md text-on-surface-variant font-normal">
          {' '}
          / {summary.issuedCount.toLocaleString('ko-KR')}
        </span>
      </p>
    </Card>

    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <StatCard label="미입장" value={summary.notCheckedInCount} />
      <StatCard label="취소·무효" value={summary.canceledCount} />
    </div>
  </div>
)

const StatCard = ({ label, value }: { label: string; value: number }) => (
  <Card>
    <p className="text-label-md text-on-surface-variant font-medium">{label}</p>
    <p className="text-headline-sm text-on-surface mt-1 font-semibold">
      {value.toLocaleString('ko-KR')}
    </p>
  </Card>
)

export default ClientCheckInPage
