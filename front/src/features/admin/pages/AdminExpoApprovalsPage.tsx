'use client'

import { useState } from 'react'

import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Select,
} from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import {
  EXPO_OPENING_STATUS_LABEL,
  type ExpoOpeningRequest,
  type ExpoOpeningRequestStatus,
} from '@/features/client/expoOpeningApi'
import { formatDate, formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAdminExpoOpeningRequests,
  useApproveExpoOpening,
  useRejectExpoOpening,
} from '../expoOpeningHooks'

const STATUS_VARIANT: Record<ExpoOpeningRequestStatus, BadgeVariant> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  UNDER_REVIEW: 'info',
  APPROVED: 'success',
  REJECTED: 'error',
  CANCELED: 'neutral',
}

/** 심사 가능한 상태 — 백엔드 `requireReviewable()` 과 같은 조건. */
const isReviewable = (status: ExpoOpeningRequestStatus) =>
  status === 'SUBMITTED' || status === 'UNDER_REVIEW'

/**
 * `/admin/expos`. Function.md 4절 — "박람회 개최 승인 관리".
 *
 * 디자인(`262c25c5`)의 목록 컬럼(신청번호·주최자·박람회명·행사기간·요청장소·신청일·심사상태)을
 * 그대로 담았다. 승인하면 백엔드가 같은 트랜잭션에서 `expos` 행을 만든다(이슈 #116).
 */
const AdminExpoApprovalsPage = () => {
  const [statusFilter, setStatusFilter] = useState<ExpoOpeningRequestStatus | ''>('SUBMITTED')
  const { data, isPending, isError, error, refetch } = useAdminExpoOpeningRequests(
    statusFilter || undefined
  )

  const header = (
    <PageHeader
      title="박람회 개최 승인 관리"
      description="주최사의 박람회 개최 신청을 심사합니다. 승인하면 박람회가 개설됩니다."
    />
  )

  return (
    <div>
      {header}

      <div className="mb-4 max-w-xs">
        <Select
          label="심사 상태"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as ExpoOpeningRequestStatus | '')}
        >
          <option value="">전체</option>
          {(Object.keys(EXPO_OPENING_STATUS_LABEL) as ExpoOpeningRequestStatus[]).map((status) => (
            <option key={status} value={status}>
              {EXPO_OPENING_STATUS_LABEL[status]}
            </option>
          ))}
        </Select>
      </div>

      {isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : isPending ? (
        <LoadingBlock label="신청 목록을 불러오는 중입니다" />
      ) : data.length === 0 ? (
        <EmptyState
          title="심사할 신청이 없습니다"
          description="주최사가 개최 신청을 올리면 여기에 표시됩니다."
        />
      ) : (
        <div className="flex flex-col gap-3">
          {data.map((request) => (
            <ExpoApprovalRow key={request.id} request={request} />
          ))}
        </div>
      )}
    </div>
  )
}

const ExpoApprovalRow = ({ request }: { request: ExpoOpeningRequest }) => {
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const approveMutation = useApproveExpoOpening()
  const rejectMutation = useRejectExpoOpening()

  const handleApprove = () => {
    setActionError(null)
    approveMutation.mutate(request.id, {
      onError: (err) => setActionError(getErrorMessage(err)),
    })
  }

  const handleReject = () => {
    setActionError(null)
    rejectMutation.mutate(
      { requestId: request.id, reason },
      {
        onSuccess: () => {
          setRejecting(false)
          setReason('')
        },
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-label-sm text-on-surface-variant">#{request.id}</span>
            <p className="text-title-lg text-on-surface font-semibold">{request.title}</p>
            <Badge variant={STATUS_VARIANT[request.status]}>
              {EXPO_OPENING_STATUS_LABEL[request.status]}
            </Badge>
          </div>
          <p className="text-body-sm text-on-surface-variant mt-1">
            {request.hostCompanyName ?? `클라이언트 #${request.hostClientId}`} · 행사{' '}
            {formatDate(request.eventStartAt)} ~ {formatDate(request.eventEndAt)}
          </p>
          <p className="text-body-sm text-on-surface-variant">
            요청 장소 {request.desiredVenueName ?? '미정'}
            {request.submittedAt ? ` · 신청일 ${formatDateTime(request.submittedAt)}` : ''}
          </p>
        </div>

        {isReviewable(request.status) && (
          <div className="flex shrink-0 gap-2">
            <Button size="sm" loading={approveMutation.isPending} onClick={handleApprove}>
              승인
            </Button>
            <Button
              size="sm"
              variant="danger"
              onClick={() => setRejecting((previous) => !previous)}
            >
              반려
            </Button>
          </div>
        )}
      </div>

      {request.status === 'REJECTED' && request.rejectionReason && (
        <p className="bg-error-container text-on-error-container text-body-md mt-3 rounded px-3 py-2">
          반려 사유: {request.rejectionReason}
        </p>
      )}

      {request.status === 'APPROVED' && request.createdExpoId && (
        <p className="text-label-sm text-on-surface-variant mt-3">
          박람회 #{request.createdExpoId} 로 개설됨
          {request.reviewedAt ? ` · ${formatDateTime(request.reviewedAt)}` : ''}
        </p>
      )}

      {rejecting && (
        <div className="mt-3 flex flex-col gap-2">
          <Input
            label="반려 사유"
            hint="주최사에게 그대로 보입니다."
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="danger"
              disabled={!reason.trim()}
              loading={rejectMutation.isPending}
              onClick={handleReject}
            >
              반려 확정
            </Button>
            <Button size="sm" variant="secondary" onClick={() => setRejecting(false)}>
              취소
            </Button>
          </div>
        </div>
      )}

      {actionError && <p className="text-label-sm text-error mt-2">{actionError}</p>}
    </Card>
  )
}

export default AdminExpoApprovalsPage
