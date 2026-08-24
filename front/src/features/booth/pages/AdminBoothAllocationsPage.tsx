'use client'

import { useState } from 'react'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Textarea,
} from '@/components/ui'
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { useAdminBoothAllocations, useCancelBoothAllocation, useReassignBoothAllocation } from '../hooks'
import type { AdminBoothAllocation, BoothAllocationStatus } from '../api'

const STATUS_LABEL: Record<BoothAllocationStatus, string> = {
  ASSIGNED: '확정 배정',
  CANCELED: '취소됨',
  REASSIGNED: '재배정됨',
}

const STATUS_VARIANT: Record<BoothAllocationStatus, 'success' | 'neutral' | 'error' | 'info'> = {
  ASSIGNED: 'success',
  CANCELED: 'error',
  REASSIGNED: 'info',
}

type ActionMode = 'none' | 'cancel' | 'reassign'

/** 배정 한 건 — ASSIGNED 상태일 때만 취소·재배정 액션을 제공한다. */
const AllocationRow = ({ allocation }: { allocation: AdminBoothAllocation }) => {
  const [mode, setMode] = useState<ActionMode>('none')
  const [reason, setReason] = useState('')
  const [targetBoothProductId, setTargetBoothProductId] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const cancelMutation = useCancelBoothAllocation()
  const reassignMutation = useReassignBoothAllocation()

  const closeAction = () => {
    setMode('none')
    setReason('')
    setTargetBoothProductId('')
    setActionError(null)
  }

  const handleCancel = () => {
    if (!reason.trim()) {
      setActionError('취소 사유를 입력하세요.')
      return
    }
    setActionError(null)
    cancelMutation.mutate(
      { allocationId: allocation.id, reason },
      { onSuccess: closeAction, onError: (err) => setActionError(getErrorMessage(err)) }
    )
  }

  const handleReassign = () => {
    if (!targetBoothProductId || !reason.trim()) {
      setActionError('옮겨갈 부스 상품 ID와 재배정 사유를 모두 입력하세요.')
      return
    }
    setActionError(null)
    reassignMutation.mutate(
      { allocationId: allocation.id, boothProductId: Number(targetBoothProductId), reason },
      { onSuccess: closeAction, onError: (err) => setActionError(getErrorMessage(err)) }
    )
  }

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <CardTitle className="mb-0.5">배정 #{allocation.id}</CardTitle>
          <p className="text-label-sm text-on-surface-variant">
            신청 #{allocation.applicationId} · 주문 #{allocation.boothOrderId} · 부스 상품 #
            {allocation.boothProductId} · 배정 {formatDateTime(allocation.allocatedAt)}
          </p>
        </div>
        <Badge variant={STATUS_VARIANT[allocation.status]}>
          {STATUS_LABEL[allocation.status]}
        </Badge>
      </div>

      {allocation.status !== 'ASSIGNED' && allocation.cancelReason && (
        <p className="text-label-sm text-on-surface-variant">
          {allocation.canceledAt && `${formatDateTime(allocation.canceledAt)} · `}
          사유: {allocation.cancelReason}
        </p>
      )}

      {allocation.status === 'ASSIGNED' && mode === 'none' && (
        <div className="flex gap-2">
          <Button type="button" size="sm" variant="danger" onClick={() => setMode('cancel')}>
            취소
          </Button>
          <Button type="button" size="sm" variant="secondary" onClick={() => setMode('reassign')}>
            재배정
          </Button>
        </div>
      )}

      {mode === 'cancel' && (
        <div className="flex flex-col gap-2">
          <Textarea
            label="취소 사유"
            hint="이중 배정 등 운영상 정정 전용입니다 — 일반적인 환불 취소가 아닙니다."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              variant="danger"
              loading={cancelMutation.isPending}
              onClick={handleCancel}
            >
              배정 취소
            </Button>
            <Button type="button" size="sm" variant="secondary" onClick={closeAction}>
              닫기
            </Button>
          </div>
        </div>
      )}

      {mode === 'reassign' && (
        <div className="flex flex-col gap-2">
          <Input
            label="옮겨갈 부스 상품 ID"
            type="number"
            value={targetBoothProductId}
            onChange={(e) => setTargetBoothProductId(e.target.value)}
          />
          <Textarea label="재배정 사유" value={reason} onChange={(e) => setReason(e.target.value)} />
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              loading={reassignMutation.isPending}
              onClick={handleReassign}
            >
              재배정 확정
            </Button>
            <Button type="button" size="sm" variant="secondary" onClick={closeAction}>
              닫기
            </Button>
          </div>
        </div>
      )}

      {actionError && <p className="text-label-sm text-error">{actionError}</p>}
    </Card>
  )
}

/**
 * `/admin/booth-allocations`. ADMIN 전용.
 *
 * 이중 배정 등 운영상 실수를 정정하기 위한 화면이다 — 일반적인 취소·환불 흐름(주문 취소,
 * 참여 신청 철회)과는 다르다. 그래서 사유를 필수로 받고, ASSIGNED 상태에서만 액션을 보여준다.
 */
const AdminBoothAllocationsPage = () => {
  const { data: allocations, isPending, isError, error, refetch } = useAdminBoothAllocations()

  return (
    <div>
      <PageHeader
        title="부스 배정 관리"
        description="이중 배정 등 운영상 정정이 필요할 때 확정 배정을 취소하거나 다른 부스로 재배정합니다."
      />

      {isPending ? (
        <LoadingBlock label="배정 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : allocations.length === 0 ? (
        <EmptyState title="확정 배정된 부스가 없습니다" />
      ) : (
        <div className="flex flex-col gap-3">
          {allocations.map((allocation) => (
            <AllocationRow key={allocation.id} allocation={allocation} />
          ))}
        </div>
      )}
    </div>
  )
}

export default AdminBoothAllocationsPage
