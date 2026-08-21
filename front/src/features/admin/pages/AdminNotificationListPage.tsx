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
  Select,
} from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  NOTIFICATION_STATUS_LABEL,
  NOTIFICATION_TEMPLATE_LABEL,
  type NotificationHistory,
} from '../notificationApi'
import { useAdminNotifications, useRetryNotification } from '../notificationHooks'

const STATUS_BADGE: Record<string, BadgeVariant> = {
  PENDING: 'neutral',
  SENT: 'success',
  FAILED: 'error',
  RETRYING: 'info',
  CANCELED: 'neutral',
}

const NotificationRow = ({ notification }: { notification: NotificationHistory }) => {
  const retryMutation = useRetryNotification()

  return (
    <div className="flex items-center justify-between gap-4 px-6 py-4">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant={STATUS_BADGE[notification.status] ?? 'neutral'}>
            {NOTIFICATION_STATUS_LABEL[notification.status] ?? notification.status}
          </Badge>
          <p className="text-title-md text-on-surface font-medium">
            {NOTIFICATION_TEMPLATE_LABEL[notification.templateCode] ?? notification.templateCode}
          </p>
        </div>
        <p className="text-body-sm text-on-surface-variant mt-1">
          {notification.referenceType} #{notification.referenceId} · 수신{' '}
          {notification.recipientPhoneNumber ?? '번호 없음'} · 시도 {notification.attemptCount}회
        </p>
        <p className="text-body-sm text-on-surface-variant">
          생성 {formatDateTime(notification.createdAt)}
          {notification.sentAt && ` · 발송 ${formatDateTime(notification.sentAt)}`}
        </p>
        {notification.lastError && (
          <p className="text-label-sm text-error mt-1">마지막 오류: {notification.lastError}</p>
        )}
        {retryMutation.isError && (
          <p className="text-label-sm text-error mt-1">{getErrorMessage(retryMutation.error)}</p>
        )}
      </div>
      {notification.retryable && (
        <Button
          size="sm"
          variant="secondary"
          className="shrink-0"
          loading={retryMutation.isPending}
          onClick={() => {
            if (window.confirm('이 알림을 재발송할까요?')) {
              retryMutation.mutate(notification.notificationId)
            }
          }}
        >
          재발송
        </Button>
      )}
    </div>
  )
}

/** `/admin/notifications`. Function.md 4절 — "신설. Style.md 의 Admin 셸 표 UI 참고". ADMIN 전용. */
const AdminNotificationListPage = () => {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error, refetch } = useAdminNotifications({
    status: status || undefined,
    page,
  })

  return (
    <div>
      <PageHeader
        title="알림 이력·재발송"
        description="발송된 알림 이력을 확인하고 실패 건을 재발송합니다."
      />

      <div className="mb-4 max-w-xs">
        <Select
          label="상태 필터"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPage(0)
          }}
        >
          <option value="">전체</option>
          {Object.entries(NOTIFICATION_STATUS_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </div>

      {isPending ? (
        <LoadingBlock label="알림 이력을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.items.length === 0 ? (
        <EmptyState
          title="해당하는 알림 이력이 없습니다"
          description="필터를 바꿔 다시 확인해 보세요."
        />
      ) : (
        <>
          <Card className="divide-outline-variant divide-y p-0">
            {data.items.map((notification) => (
              <NotificationRow key={notification.notificationId} notification={notification} />
            ))}
          </Card>
          <div className="mt-4 flex items-center justify-between">
            <Button
              variant="secondary"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            >
              이전
            </Button>
            <p className="text-label-sm text-on-surface-variant">
              {page + 1}페이지 · 전체 {data.totalCount.toLocaleString('ko-KR')}건
            </p>
            <Button
              variant="secondary"
              size="sm"
              disabled={(page + 1) * data.size >= data.totalCount}
              onClick={() => setPage((prev) => prev + 1)}
            >
              다음
            </Button>
          </div>
        </>
      )}
    </div>
  )
}

export default AdminNotificationListPage
