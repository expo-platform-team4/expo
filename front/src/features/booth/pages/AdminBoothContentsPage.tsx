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
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAdminBoothContents,
  useApproveBoothContent,
  useCheckBoothContent,
  useHideBoothContent,
  useRequestBoothContentCorrection,
  useRestoreBoothContent,
} from '../hooks'
import type { AdminBoothContent, BoothContentStatus } from '../api'

const STATUS_LABEL: Record<BoothContentStatus, string> = {
  DRAFT: '초안',
  UNDER_REVIEW: '검수 중',
  PUBLISHED: '공개됨',
  CORRECTION_REQUESTED: '보완 요청됨',
  HIDDEN: '숨김',
}

const STATUS_VARIANT: Record<BoothContentStatus, 'success' | 'neutral' | 'error' | 'info'> = {
  DRAFT: 'neutral',
  UNDER_REVIEW: 'info',
  PUBLISHED: 'success',
  CORRECTION_REQUESTED: 'error',
  HIDDEN: 'neutral',
}

/** 콘텐츠 한 건 — 상세 정보 + 상태별 운영 액션. */
const ContentRow = ({ content }: { content: AdminBoothContent }) => {
  const [correctionOpen, setCorrectionOpen] = useState(false)
  const [correctionMessage, setCorrectionMessage] = useState('')
  const [hideOpen, setHideOpen] = useState(false)
  const [hideReason, setHideReason] = useState('')
  const [restoreOpen, setRestoreOpen] = useState(false)
  const [restoreReason, setRestoreReason] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const checkMutation = useCheckBoothContent()
  const approveMutation = useApproveBoothContent()
  const correctionMutation = useRequestBoothContentCorrection()
  const hideMutation = useHideBoothContent()
  const restoreMutation = useRestoreBoothContent()

  const handleCheck = () => {
    setActionError(null)
    checkMutation.mutate(content.id, { onError: (err) => setActionError(getErrorMessage(err)) })
  }
  const handleApprove = () => {
    setActionError(null)
    approveMutation.mutate(content.id, { onError: (err) => setActionError(getErrorMessage(err)) })
  }
  const handleSubmitCorrection = () => {
    if (!correctionMessage.trim()) {
      setActionError('보완 요청 사유를 입력하세요.')
      return
    }
    setActionError(null)
    correctionMutation.mutate(
      { contentId: content.id, message: correctionMessage },
      {
        onSuccess: () => {
          setCorrectionOpen(false)
          setCorrectionMessage('')
        },
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }
  const handleHide = () => {
    setActionError(null)
    hideMutation.mutate(
      { contentId: content.id, reason: hideReason.trim() || undefined },
      {
        onSuccess: () => {
          setHideOpen(false)
          setHideReason('')
        },
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }
  const handleRestore = () => {
    setActionError(null)
    restoreMutation.mutate(
      { contentId: content.id, reason: restoreReason.trim() || undefined },
      {
        onSuccess: () => {
          setRestoreOpen(false)
          setRestoreReason('')
        },
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <CardTitle className="mb-0.5">{content.title}</CardTitle>
          <p className="text-label-sm text-on-surface-variant">
            {content.companyDisplayName} · 배정 #{content.boothAllocationId} · 작성{' '}
            {formatDateTime(content.createdAt)}
          </p>
        </div>
        <Badge variant={STATUS_VARIANT[content.status]}>{STATUS_LABEL[content.status]}</Badge>
      </div>

      <dl className="text-body-md text-on-surface grid grid-cols-1 gap-2 sm:grid-cols-3">
        {content.companyDescription && (
          <div>
            <dt className="text-label-sm text-on-surface-variant">기업 소개</dt>
            <dd className="whitespace-pre-wrap">{content.companyDescription}</dd>
          </div>
        )}
        {content.boothDescription && (
          <div>
            <dt className="text-label-sm text-on-surface-variant">부스 소개</dt>
            <dd className="whitespace-pre-wrap">{content.boothDescription}</dd>
          </div>
        )}
        {content.productDescription && (
          <div>
            <dt className="text-label-sm text-on-surface-variant">제품 소개</dt>
            <dd className="whitespace-pre-wrap">{content.productDescription}</dd>
          </div>
        )}
      </dl>

      {(content.files.length > 0 || content.links.length > 0) && (
        <p className="text-label-sm text-on-surface-variant">
          첨부 파일 {content.files.length}개 · 외부 링크 {content.links.length}개
        </p>
      )}

      {content.status === 'CORRECTION_REQUESTED' && content.correctionMessage && (
        <p className="text-label-sm text-error">보완 요청 사유: {content.correctionMessage}</p>
      )}

      {content.status === 'DRAFT' && (
        <p className="text-label-sm text-on-surface-variant">아직 검수 요청 전입니다.</p>
      )}

      {content.status === 'UNDER_REVIEW' && (
        <div className="flex flex-wrap gap-2">
          {!content.checkedAt && (
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
            loading={approveMutation.isPending}
            onClick={handleApprove}
          >
            승인(공개)
          </Button>
          <Button
            type="button"
            size="sm"
            variant="danger"
            onClick={() => setCorrectionOpen((prev) => !prev)}
          >
            보완 요청
          </Button>
        </div>
      )}
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

      {content.status === 'PUBLISHED' &&
        (hideOpen ? (
          <div className="flex flex-col gap-2">
            <Textarea
              label="숨김 사유"
              hint="선택 항목입니다. 남기면 운영 이력에 기록됩니다."
              value={hideReason}
              onChange={(e) => setHideReason(e.target.value)}
            />
            <div className="flex gap-2">
              <Button
                type="button"
                size="sm"
                variant="danger"
                loading={hideMutation.isPending}
                onClick={handleHide}
              >
                숨김 확정
              </Button>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={() => setHideOpen(false)}
              >
                취소
              </Button>
            </div>
          </div>
        ) : (
          <Button type="button" size="sm" variant="danger" onClick={() => setHideOpen(true)}>
            숨김
          </Button>
        ))}
      {content.status === 'HIDDEN' &&
        (restoreOpen ? (
          <div className="flex flex-col gap-2">
            <Textarea
              label="숨김 해제 사유"
              hint="선택 항목입니다. 남기면 운영 이력에 기록됩니다."
              value={restoreReason}
              onChange={(e) => setRestoreReason(e.target.value)}
            />
            <div className="flex gap-2">
              <Button
                type="button"
                size="sm"
                loading={restoreMutation.isPending}
                onClick={handleRestore}
              >
                숨김 해제 확정
              </Button>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={() => setRestoreOpen(false)}
              >
                취소
              </Button>
            </div>
          </div>
        ) : (
          <Button type="button" size="sm" onClick={() => setRestoreOpen(true)}>
            숨김 해제
          </Button>
        ))}

      {actionError && <p className="text-label-sm text-error">{actionError}</p>}
    </Card>
  )
}

/**
 * `/admin/booth-contents`. ADMIN 전용.
 *
 * 확정 배정된 기업이 부스 소개(콘텐츠)를 작성해 검수 요청까지는 낼 수 있었는데, 그걸
 * 승인·반려할 관리자 화면이 없어서 검수 대기 상태로 멈춰있었다. 이 화면이 그 막힌 지점을
 * 뚫는다 — 운영 확인·승인·보완 요청·숨김·숨김 해제까지 상태별로 할 수 있는 액션만 보여준다.
 */
const AdminBoothContentsPage = () => {
  const { data: page, isPending, isError, error, refetch } = useAdminBoothContents()
  const contents = page?.content ?? []
  const isTruncated = page != null && page.totalElements > contents.length

  return (
    <div>
      <PageHeader
        title="부스 콘텐츠 관리"
        description="기업이 작성한 부스 소개를 검수·승인·숨김 처리합니다."
      />

      {isPending ? (
        <LoadingBlock label="콘텐츠를 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : contents.length === 0 ? (
        <EmptyState title="등록된 부스 콘텐츠가 없습니다" />
      ) : (
        <div className="flex flex-col gap-3">
          {isTruncated && (
            <p className="text-label-sm text-error">
              전체 {page.totalElements}건 중 {contents.length}건만 표시됩니다.
            </p>
          )}
          {contents.map((content) => (
            <ContentRow key={content.id} content={content} />
          ))}
        </div>
      )}
    </div>
  )
}

export default AdminBoothContentsPage
