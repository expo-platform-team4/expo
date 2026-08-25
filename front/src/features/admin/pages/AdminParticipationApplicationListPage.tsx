'use client'

import { useState } from 'react'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
  Textarea,
} from '@/components/ui'
import type { ParticipationApplicationStatus } from '@/features/participation/api'
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAdminParticipationApplications,
  useCheckParticipationApplication,
  useCompleteParticipationApplicationCorrection,
  useParticipationApplicationHistory,
  useRequestParticipationApplicationCorrection,
  useUpdateParticipationApplicationMemo,
} from '../participationHooks'
import type {
  AdminParticipationApplication,
  ApplicationOperationActionType,
} from '../participationApi'

const STATUS_LABEL: Record<ParticipationApplicationStatus, string> = {
  DRAFT: '임시저장',
  PAYMENT_PENDING: '결제 대기',
  SUBMITTED: '제출 완료',
  PAYMENT_FAILED: '결제 실패',
  CANCELED: '철회됨',
}

const STATUS_VARIANT: Record<
  ParticipationApplicationStatus,
  'success' | 'neutral' | 'error' | 'info'
> = {
  DRAFT: 'neutral',
  PAYMENT_PENDING: 'info',
  SUBMITTED: 'success',
  PAYMENT_FAILED: 'error',
  CANCELED: 'error',
}

const HISTORY_ACTION_LABEL: Record<ApplicationOperationActionType, string> = {
  CHECKED: '운영 확인',
  CORRECTION_REQUESTED: '보완 요청',
  CORRECTION_COMPLETED: '보완 완료',
  MEMO_UPDATED: '메모 갱신',
}

/** 신청서 한 건의 운영 이력. "이력 보기"를 눌렀을 때만 불러온다(N+1 방지). */
const HistoryList = ({ applicationId }: { applicationId: number }) => {
  const {
    data: history,
    isPending,
    isError,
  } = useParticipationApplicationHistory(applicationId, true)

  if (isPending) return <p className="text-label-sm text-on-surface-variant">불러오는 중...</p>
  if (isError) return <p className="text-label-sm text-error">이력을 불러오지 못했습니다.</p>
  if (history.length === 0) {
    return <p className="text-label-sm text-on-surface-variant">아직 운영 이력이 없습니다.</p>
  }

  return (
    <ul className="flex flex-col gap-1">
      {history.map((entry) => (
        <li key={entry.id} className="text-label-sm text-on-surface-variant">
          [{HISTORY_ACTION_LABEL[entry.actionType]}] {formatDateTime(entry.createdAt)}
          {entry.message && ` · ${entry.message}`}
        </li>
      ))}
    </ul>
  )
}

const ApplicationCard = ({ application }: { application: AdminParticipationApplication }) => {
  const [correctionOpen, setCorrectionOpen] = useState(false)
  const [correctionMessage, setCorrectionMessage] = useState('')
  const [memo, setMemo] = useState(application.adminMemo ?? '')
  const [historyOpen, setHistoryOpen] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const checkMutation = useCheckParticipationApplication(application.id)
  const correctionMutation = useRequestParticipationApplicationCorrection(application.id)
  const completeCorrectionMutation = useCompleteParticipationApplicationCorrection(application.id)
  const memoMutation = useUpdateParticipationApplicationMemo(application.id)

  const handleCheck = () => {
    setActionError(null)
    checkMutation.mutate(undefined, { onError: (err) => setActionError(getErrorMessage(err)) })
  }

  const handleSubmitCorrection = () => {
    if (!correctionMessage.trim()) {
      setActionError('보완 요청 사유를 입력하세요.')
      return
    }
    setActionError(null)
    correctionMutation.mutate(correctionMessage, {
      onSuccess: () => {
        setCorrectionOpen(false)
        setCorrectionMessage('')
      },
      onError: (err) => setActionError(getErrorMessage(err)),
    })
  }

  const handleCompleteCorrection = () => {
    setActionError(null)
    completeCorrectionMutation.mutate(undefined, {
      onError: (err) => setActionError(getErrorMessage(err)),
    })
  }

  const handleSaveMemo = () => {
    if (!memo.trim()) {
      setActionError('메모를 입력하세요.')
      return
    }
    setActionError(null)
    memoMutation.mutate(memo, { onError: (err) => setActionError(getErrorMessage(err)) })
  }

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <CardTitle className="mb-0.5">{application.companyNameSnapshot}</CardTitle>
          <p className="text-label-sm text-on-surface-variant">
            신청 #{application.id} · 공고 #{application.recruitmentNoticeId}
            {application.submittedAt && ` · 제출 ${formatDateTime(application.submittedAt)}`}
          </p>
        </div>
        <Badge variant={STATUS_VARIANT[application.status]}>
          {STATUS_LABEL[application.status]}
        </Badge>
      </div>

      <dl className="text-body-md text-on-surface grid grid-cols-1 gap-2 sm:grid-cols-2">
        {application.participationPurpose && (
          <div>
            <dt className="text-label-sm text-on-surface-variant">참여 목적</dt>
            <dd className="whitespace-pre-wrap">{application.participationPurpose}</dd>
          </div>
        )}
        {application.exhibitDescription && (
          <div>
            <dt className="text-label-sm text-on-surface-variant">전시 품목 설명</dt>
            <dd className="whitespace-pre-wrap">{application.exhibitDescription}</dd>
          </div>
        )}
      </dl>

      <p className="text-label-sm text-on-surface-variant">
        {application.adminCheckedAt
          ? `운영 확인됨 · ${formatDateTime(application.adminCheckedAt)}`
          : '아직 운영 확인 전입니다.'}
      </p>

      <div className="flex flex-wrap gap-2">
        {!application.adminCheckedAt && (
          <Button
            type="button"
            size="sm"
            variant="secondary"
            loading={checkMutation.isPending}
            onClick={handleCheck}
          >
            운영 확인
          </Button>
        )}
        <Button
          type="button"
          size="sm"
          variant="danger"
          onClick={() => setCorrectionOpen((v) => !v)}
        >
          보완 요청
        </Button>
        <Button
          type="button"
          size="sm"
          loading={completeCorrectionMutation.isPending}
          onClick={handleCompleteCorrection}
        >
          보완 완료 처리
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={() => setHistoryOpen((v) => !v)}>
          {historyOpen ? '이력 숨기기' : '이력 보기'}
        </Button>
      </div>

      {correctionOpen && (
        <div className="flex flex-col gap-2">
          <Textarea
            label="보완 요청 사유"
            value={correctionMessage}
            onChange={(e) => setCorrectionMessage(e.target.value)}
          />
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              loading={correctionMutation.isPending}
              onClick={handleSubmitCorrection}
            >
              요청 보내기
            </Button>
            <Button
              type="button"
              size="sm"
              variant="secondary"
              onClick={() => setCorrectionOpen(false)}
            >
              취소
            </Button>
          </div>
        </div>
      )}

      <div className="border-outline-variant flex flex-col gap-2 border-t pt-3">
        <Textarea
          label="관리자 메모"
          hint="신청 기업에게는 보이지 않습니다."
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
        />
        <Button
          type="button"
          size="sm"
          variant="secondary"
          className="self-start"
          loading={memoMutation.isPending}
          onClick={handleSaveMemo}
        >
          메모 저장
        </Button>
      </div>

      {historyOpen && (
        <div className="border-outline-variant border-t pt-3">
          <HistoryList applicationId={application.id} />
        </div>
      )}

      {actionError && <p className="text-label-sm text-error">{actionError}</p>}
    </Card>
  )
}

/**
 * `/admin/participation-applications`. ADMIN 전용.
 *
 * 회사가 참여 신청서를 제출해도 관리자가 확인하거나 지적할 방법이 없었다 — 부스 콘텐츠
 * 승인 화면과 같은 이유로 완전히 비어있던 화면이다. 운영 확인·보완 요청·보완 완료·메모
 * 작성까지 이 화면 하나로 처리한다.
 *
 * 신청서 상태(`status`) 자체는 이 화면의 액션으로 바뀌지 않는다 — 보완 요청·완료는
 * 상태가 아니라 운영 이력으로만 남는다(백엔드 설계 그대로). "지금 보완 대기 중인가"는
 * 이력을 펼쳐 봐야 정확히 알 수 있다.
 */
const AdminParticipationApplicationListPage = () => {
  const {
    data: applications,
    isPending,
    isError,
    error,
    refetch,
  } = useAdminParticipationApplications()

  return (
    <div>
      <PageHeader
        title="참여 신청서 관리"
        description="기업이 제출한 참여 신청서를 확인하고, 보완이 필요하면 요청합니다."
      />

      {isPending ? (
        <LoadingBlock label="신청서를 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : applications.length === 0 ? (
        <EmptyState title="접수된 참여 신청서가 없습니다" />
      ) : (
        <div className="flex flex-col gap-3">
          {applications.map((application) => (
            <ApplicationCard key={application.id} application={application} />
          ))}
        </div>
      )}
    </div>
  )
}

export default AdminParticipationApplicationListPage
