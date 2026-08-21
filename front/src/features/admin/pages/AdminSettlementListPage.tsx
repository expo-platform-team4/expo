'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

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
import { formatCurrency } from '@/lib/currency'
import { formatDate } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  canCalculateSettlement,
  canConfirmSettlement,
  canRecordRemittance,
  SETTLEMENT_STATUS_LABEL,
  type AdminSettlement,
} from '../settlementApi'
import {
  useAdminSettlements,
  useCalculateSettlement,
  useConfirmSettlement,
  useRecordRemittance,
} from '../settlementHooks'
import { recordRemittanceSchema, type RecordRemittanceFormValues } from '../schemas'

const STATUS_BADGE: Record<string, BadgeVariant> = {
  WAITING: 'neutral',
  CALCULATED: 'info',
  UNDER_REVIEW: 'info',
  CONFIRMED: 'success',
  REMITTANCE_PENDING: 'info',
  REMITTED: 'success',
  ON_HOLD: 'error',
}

const STATUS_FILTER_OPTIONS = [
  'WAITING',
  'CALCULATED',
  'UNDER_REVIEW',
  'CONFIRMED',
  'REMITTANCE_PENDING',
  'REMITTED',
  'ON_HOLD',
]

/** 송금 결과 기록 폼. `CONFIRMED`·`REMITTANCE_PENDING` 상태에서만 노출한다. */
const RemittanceForm = ({ settlementId, onDone }: { settlementId: number; onDone: () => void }) => {
  const recordMutation = useRecordRemittance()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RecordRemittanceFormValues>({
    resolver: zodResolver(recordRemittanceSchema),
    defaultValues: { status: 'REMITTED', remittedAmount: '', referenceNumber: '', memo: '' },
  })

  const onSubmit = (values: RecordRemittanceFormValues) => {
    recordMutation.mutate(
      {
        settlementId,
        payload: {
          status: values.status,
          remittedAmount: values.remittedAmount ? Number(values.remittedAmount) : undefined,
          referenceNumber: values.referenceNumber || undefined,
          memo: values.memo || undefined,
        },
      },
      { onSuccess: onDone }
    )
  }

  return (
    <form
      className="bg-surface-container-low mt-3 flex flex-col gap-3 rounded-md p-4"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Select label="송금 상태" error={errors.status?.message} {...register('status')}>
          <option value="REMITTED">송금 완료</option>
          <option value="FAILED">송금 실패</option>
          <option value="PENDING">대기</option>
          <option value="PROCESSING">처리 중</option>
          <option value="CANCELED">취소</option>
        </Select>
        <Input
          label="실제 이체 금액"
          inputMode="numeric"
          hint="비워 두면 확정액과 비교하지 않습니다."
          error={errors.remittedAmount?.message}
          {...register('remittedAmount')}
        />
        <Input
          label="은행 거래번호"
          error={errors.referenceNumber?.message}
          {...register('referenceNumber')}
        />
        <Input label="메모" error={errors.memo?.message} {...register('memo')} />
      </div>
      {recordMutation.isError && (
        <p className="text-label-sm text-error">{getErrorMessage(recordMutation.error)}</p>
      )}
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={recordMutation.isPending}>
          기록 저장
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onDone}>
          취소
        </Button>
      </div>
    </form>
  )
}

const SettlementRow = ({ settlement }: { settlement: AdminSettlement }) => {
  const [recording, setRecording] = useState(false)
  const calculateMutation = useCalculateSettlement()
  const confirmMutation = useConfirmSettlement()

  const actionError = calculateMutation.error ?? confirmMutation.error ?? undefined

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-title-lg text-primary font-semibold">{settlement.expoTitle}</h3>
        <Badge variant={STATUS_BADGE[settlement.status] ?? 'neutral'}>
          {SETTLEMENT_STATUS_LABEL[settlement.status] ?? settlement.status}
        </Badge>
      </div>
      <p className="text-label-sm text-on-surface-variant mt-3">
        주최사 {settlement.companyName} (#{settlement.hostClientId}) · 정산 기한{' '}
        {formatDate(settlement.settlementDueAt)}
      </p>
      <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-3">
        <p className="text-body-sm text-on-surface">
          송금할 금액{' '}
          <span className="font-semibold">{formatCurrency(settlement.remittanceDueAmount)}</span>
        </p>
        <p className="text-body-sm text-on-surface">
          조정 합계 {formatCurrency(settlement.adjustmentAmount)}
        </p>
        <p className="text-body-sm text-on-surface">
          실제 송금액{' '}
          {settlement.remittedAmount != null ? formatCurrency(settlement.remittedAmount) : '-'}
        </p>
      </div>
      {settlement.confirmedAt && (
        <p className="text-label-sm text-on-surface-variant mt-1">
          확정 {formatDate(settlement.confirmedAt)} (관리자 #{settlement.confirmedBy})
        </p>
      )}
      {settlement.remittedAt && (
        <p className="text-label-sm text-on-surface-variant mt-1">
          송금 {formatDate(settlement.remittedAt)} · {settlement.remittanceStatus}
        </p>
      )}

      {actionError && (
        <p className="text-label-sm text-error mt-2">{getErrorMessage(actionError)}</p>
      )}

      <div className="mt-3 flex flex-wrap gap-2">
        {canCalculateSettlement(settlement.status) && (
          <Button
            size="sm"
            variant="secondary"
            loading={calculateMutation.isPending}
            onClick={() => {
              if (window.confirm('결제·환불·부스 매출을 다시 집계해 정산액을 덮어쓸까요?')) {
                calculateMutation.mutate(settlement.settlementId)
              }
            }}
          >
            재계산
          </Button>
        )}
        {canConfirmSettlement(settlement.status) && (
          <Button
            size="sm"
            loading={confirmMutation.isPending}
            onClick={() => {
              if (window.confirm('정산 금액을 확정할까요? 확정 후에는 재계산할 수 없습니다.')) {
                confirmMutation.mutate(settlement.settlementId)
              }
            }}
          >
            확정
          </Button>
        )}
        {canRecordRemittance(settlement.status) && !recording && (
          <Button size="sm" variant="secondary" onClick={() => setRecording(true)}>
            송금 기록
          </Button>
        )}
      </div>

      {recording && (
        <RemittanceForm settlementId={settlement.settlementId} onDone={() => setRecording(false)} />
      )}
    </Card>
  )
}

/** `/admin/settlements`. Function.md 4절 — "재계산·확정·송금 기록". ADMIN 전용. */
const AdminSettlementListPage = () => {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error, refetch } = useAdminSettlements({
    status: status || undefined,
    page,
  })

  return (
    <div>
      <PageHeader
        title="정산 관리"
        description="박람회별 정산을 재계산·확정하고 송금 결과를 기록합니다."
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
          {STATUS_FILTER_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {SETTLEMENT_STATUS_LABEL[option] ?? option}
            </option>
          ))}
        </Select>
      </div>

      {isPending ? (
        <LoadingBlock label="정산 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.items.length === 0 ? (
        <EmptyState
          title="해당하는 정산이 없습니다"
          description="필터를 바꿔 다시 확인해 보세요."
        />
      ) : (
        <>
          <ul className="flex flex-col gap-3">
            {data.items.map((settlement) => (
              <li key={settlement.settlementId}>
                <SettlementRow settlement={settlement} />
              </li>
            ))}
          </ul>
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

export default AdminSettlementListPage
