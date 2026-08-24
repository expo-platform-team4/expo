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

/** 부스 주문 상태 라벨. `booth_orders.status` CHECK 제약과 같은 값 집합. */
const BOOTH_ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: '결제 대기',
  PAYMENT_COMPLETED: '결제 완료',
  FAILED: '결제 실패',
  CANCELED: '취소됨',
  EXPIRED: '기간 만료',
}

/** 결제 상태 라벨. `booth_payments.status` CHECK 제약과 같은 값 집합. */
const PAYMENT_STATUS_LABEL: Record<string, string> = {
  READY: '결제 준비',
  IN_PROGRESS: '결제 진행 중',
  APPROVED: '승인 완료',
  CANCELED: '취소됨',
  FAILED: '실패',
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

      {/*
        min-w-0 이 없으면 PAYMENT_COMPLETED 같은 긴 상태값이 그리드 칸을 밀고 나가
        옆 칸 글자와 겹친다(실제로 겹쳤다). 라벨을 한국어로 바꿔 길이를 줄이면서
        방어적으로 함께 걸어 둔다.
      */}
      <dl className="text-body-md text-on-surface mt-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
        <div className="min-w-0">
          <dt className="text-label-sm text-on-surface-variant">참여 신청 상태</dt>
          <dd>{applicationStatusLabel(booth.applicationStatus)}</dd>
        </div>
        <div className="min-w-0">
          <dt className="text-label-sm text-on-surface-variant">부스 주문 상태</dt>
          <dd>
            {booth.boothOrderStatus
              ? (BOOTH_ORDER_STATUS_LABEL[booth.boothOrderStatus] ?? booth.boothOrderStatus)
              : '-'}
          </dd>
        </div>
        <div className="min-w-0">
          <dt className="text-label-sm text-on-surface-variant">결제 상태</dt>
          <dd>
            {booth.paymentStatus
              ? (PAYMENT_STATUS_LABEL[booth.paymentStatus] ?? booth.paymentStatus)
              : '-'}
          </dd>
          {booth.paidAt && (
            <dd className="text-label-sm text-on-surface-variant">
              {formatDateTime(booth.paidAt)}
            </dd>
          )}
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
