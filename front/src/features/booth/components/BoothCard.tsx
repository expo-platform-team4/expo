'use client'

import Link from 'next/link'
import { useState } from 'react'

import { Badge, Button, Card, ErrorState, Spinner } from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import type { BoothAllocationStatus, MyConfirmedBooth } from '../api'
import { useBoothAllocation } from '../hooks'
import { BoothContentSection } from './BoothContentSection'

const ALLOCATION_STATUS_LABEL: Record<BoothAllocationStatus, string> = {
  ASSIGNED: '배정됨',
  CANCELED: '취소됨',
  REASSIGNED: '재배정됨',
}

const ALLOCATION_STATUS_VARIANT: Record<
  BoothAllocationStatus,
  'success' | 'neutral' | 'error' | 'info'
> = {
  ASSIGNED: 'success',
  CANCELED: 'error',
  REASSIGNED: 'info',
}

/** 참여 신청 상태 라벨. `features/participation/pages/ClientParticipationListPage.tsx` 와 같은 값 집합. */
const applicationStatusLabel = (status: string): string => {
  switch (status) {
    case 'DRAFT':
      return '임시저장'
    case 'PAYMENT_PENDING':
      return '결제 대기'
    case 'SUBMITTED':
      return '제출 완료'
    case 'PAYMENT_FAILED':
      return '결제 실패'
    case 'CANCELED':
      return '취소됨'
    default:
      return status
  }
}

/** `/client/booths` 카드 한 장 — 확정 배정 부스 하나. 배정 상세(펼치기)와 콘텐츠 섹션을 담는다. */
export const BoothCard = ({ booth }: { booth: MyConfirmedBooth }) => {
  const [expanded, setExpanded] = useState(false)
  const {
    data: allocation,
    isPending: isAllocationPending,
    isError: isAllocationError,
    error: allocationError,
    refetch: refetchAllocation,
  } = useBoothAllocation(expanded ? booth.boothAllocationId : null)

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-title-lg text-primary font-semibold">
            {booth.boothNumber ?? `부스 배정 #${booth.boothAllocationId}`}
          </p>
          <Link
            href={`/recruitment-notices/${booth.recruitmentNoticeId}`}
            className="text-label-sm text-secondary hover:underline"
          >
            공고 #{booth.recruitmentNoticeId}
          </Link>
        </div>
        <Badge variant={ALLOCATION_STATUS_VARIANT[booth.allocationStatus]}>
          {ALLOCATION_STATUS_LABEL[booth.allocationStatus]}
        </Badge>
      </div>

      <dl className="text-body-md text-on-surface mt-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
        <div>
          <dt className="text-label-sm text-on-surface-variant">참여 신청 상태</dt>
          <dd>{applicationStatusLabel(booth.applicationStatus)}</dd>
        </div>
        <div>
          <dt className="text-label-sm text-on-surface-variant">부스 주문 상태</dt>
          <dd>{booth.boothOrderStatus ?? '-'}</dd>
        </div>
        <div>
          <dt className="text-label-sm text-on-surface-variant">결제 상태</dt>
          <dd>
            {booth.paymentStatus ?? '-'}
            {booth.paidAt ? ` · ${formatDateTime(booth.paidAt)}` : ''}
          </dd>
        </div>
      </dl>

      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="mt-2 px-0"
        onClick={() => setExpanded((prev) => !prev)}
      >
        {expanded ? '배정 상세 숨기기' : '배정 상세 보기'}
      </Button>

      {expanded && (
        <div className="border-outline-variant mt-2 border-t pt-3">
          {isAllocationPending ? (
            <div className="flex items-center gap-2 py-2">
              <Spinner size="sm" />
              <span className="text-label-md text-on-surface-variant">
                배정 정보를 불러오는 중입니다
              </span>
            </div>
          ) : isAllocationError ? (
            <ErrorState error={allocationError} onRetry={() => refetchAllocation()} />
          ) : allocation ? (
            <dl className="text-body-md text-on-surface grid grid-cols-1 gap-2 sm:grid-cols-2">
              <div>
                <dt className="text-label-sm text-on-surface-variant">배정 일시</dt>
                <dd>{formatDateTime(allocation.allocatedAt)}</dd>
              </div>
              {allocation.canceledAt && (
                <div>
                  <dt className="text-label-sm text-on-surface-variant">취소 일시</dt>
                  <dd>{formatDateTime(allocation.canceledAt)}</dd>
                </div>
              )}
              {allocation.cancelReason && (
                <div className="sm:col-span-2">
                  <dt className="text-label-sm text-on-surface-variant">취소 사유</dt>
                  <dd>{allocation.cancelReason}</dd>
                </div>
              )}
            </dl>
          ) : null}
        </div>
      )}

      {booth.allocationStatus === 'ASSIGNED' && (
        <BoothContentSection allocationId={booth.boothAllocationId} />
      )}
    </Card>
  )
}
