'use client'

import {
  Badge,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import { REVIEW_TARGET_TYPE_LABEL } from '../dashboardApi'
import { useAdminDashboardSummary, useAdminPendingTasks } from '../dashboardHooks'

type SummaryTile = { label: string; value: number }

/** `/admin`. Function.md 4절 — "요약 지표 + 대기 작업(pending-tasks)". */
const AdminDashboardPage = () => {
  const summaryQuery = useAdminDashboardSummary()
  const tasksQuery = useAdminPendingTasks()

  const isPending = summaryQuery.isPending || tasksQuery.isPending
  const isError = summaryQuery.isError || tasksQuery.isError
  const firstError = summaryQuery.error ?? tasksQuery.error

  const refetchAll = () => {
    summaryQuery.refetch()
    tasksQuery.refetch()
  }

  const header = (
    <PageHeader title="관리자 대시보드" description="처리 대기 건수와 심사 대기 목록입니다." />
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
        <LoadingBlock label="요약 지표를 불러오는 중입니다" />
      </div>
    )
  }

  const summary = summaryQuery.data
  const tasks = tasksQuery.data

  const tiles: SummaryTile[] = [
    { label: '박람회 개최 심사 대기', value: summary.pendingExpoReviewCount },
    { label: '배너 심사 대기', value: summary.pendingBannerReviewCount },
    { label: '모집공고 생성 요청 대기', value: summary.pendingNoticeRequestCount },
    { label: '장소 충돌 검토 대기', value: summary.venueConflictCount },
    { label: '미확인 참여 신청', value: summary.uncheckedApplicationCount },
    { label: '박람회 수정 요청 대기', value: summary.pendingChangeRequestCount },
    { label: '박람회 취소 요청 대기', value: summary.pendingCancellationRequestCount },
    { label: '정산 대기', value: summary.settlementWaitingCount },
  ]

  return (
    <div>
      {header}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {tiles.map((tile) => (
          <Card key={tile.label}>
            <p className="text-body-sm text-on-surface-variant">{tile.label}</p>
            <p className="text-headline-sm text-on-background mt-1 font-semibold">
              {tile.value.toLocaleString('ko-KR')}
            </p>
          </Card>
        ))}
      </div>

      <div className="mt-6">
        <CardTitle className="mb-3">대기 업무</CardTitle>
        {tasks.length === 0 ? (
          <EmptyState
            title="대기 중인 업무가 없습니다"
            description="심사·판정이 필요한 항목이 생기면 여기 표시됩니다."
          />
        ) : (
          <Card className="divide-outline-variant divide-y p-0">
            {tasks.map((task) => (
              <div
                key={`${task.reviewTargetType}-${task.targetId}`}
                className="flex items-center justify-between gap-4 px-6 py-4"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <Badge variant="info">
                      {REVIEW_TARGET_TYPE_LABEL[task.reviewTargetType] ?? task.reviewTargetType}
                    </Badge>
                    <p className="text-title-md text-on-surface font-medium">{task.title}</p>
                  </div>
                  <p className="text-body-sm text-on-surface-variant mt-1">
                    상태 {task.status} · 요청 클라이언트 #{task.requesterClientId}
                  </p>
                </div>
                <p className="text-body-sm text-on-surface-variant shrink-0">
                  {task.submittedAt
                    ? formatDateTime(task.submittedAt)
                    : formatDateTime(task.createdAt)}
                </p>
              </div>
            ))}
          </Card>
        )}
      </div>
    </div>
  )
}

export default AdminDashboardPage
