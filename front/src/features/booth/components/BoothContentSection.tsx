'use client'

import { useState } from 'react'

import { Badge, Button, ErrorState, Spinner } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'
import { formatDateTime } from '@/lib/date'

import type { BoothContentStatus } from '../api'
import {
  useCreateBoothContent,
  useMyBoothContent,
  useSubmitBoothContentForReview,
  useUpdateBoothContent,
} from '../hooks'
import type { BoothContentFormValues } from '../schemas'
import { BoothContentForm } from './BoothContentForm'

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

const EDITABLE_STATUSES: BoothContentStatus[] = ['DRAFT', 'CORRECTION_REQUESTED']

const EMPTY_FORM_VALUES: BoothContentFormValues = {
  companyDisplayName: '',
  title: '',
  companyDescription: '',
  boothDescription: '',
  productDescription: '',
}

const toFormValues = (content: {
  companyDisplayName: string
  title: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
}): BoothContentFormValues => ({
  companyDisplayName: content.companyDisplayName,
  title: content.title,
  companyDescription: content.companyDescription ?? '',
  boothDescription: content.boothDescription ?? '',
  productDescription: content.productDescription ?? '',
})

/**
 * 부스 소개 콘텐츠 섹션. 배정 상태가 `ASSIGNED` 인 부스 카드에서만 렌더링된다
 * (`BoothCard`) — 콘텐츠 작성은 확정 배정 건에만 가능하다(백엔드 `BOOTH_ALLOCATION_NOT_ASSIGNED`).
 *
 * `useMyBoothContent` 로 마운트 시 배정 ID 기준 내 콘텐츠를 상태 무관하게 불러온다(이슈 #111
 * 로 추가된 `GET /api/client/booth-contents/by-allocation/{id}`). 새로고침해도 기존 콘텐츠를
 * 다시 찾을 수 있다 — 예전에는 이 API 가 없어 세션 로컬 상태로만 "방금 만든 콘텐츠"를
 * 기억했다.
 */
export const BoothContentSection = ({ allocationId }: { allocationId: number }) => {
  const { data: content, isPending, isError, error, refetch } = useMyBoothContent(allocationId)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const createMutation = useCreateBoothContent()
  const updateMutation = useUpdateBoothContent()
  const submitMutation = useSubmitBoothContentForReview()

  if (isPending) {
    return (
      <div className="flex items-center gap-2 py-2">
        <Spinner size="sm" />
        <span className="text-label-md text-on-surface-variant">
          콘텐츠 정보를 불러오는 중입니다
        </span>
      </div>
    )
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  const handleCreate = (values: BoothContentFormValues) => {
    setActionError(null)
    createMutation.mutate(
      { boothAllocationId: allocationId, payload: values },
      {
        onSuccess: () => setShowCreateForm(false),
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }

  const handleUpdate = (values: BoothContentFormValues) => {
    if (!content) return
    setActionError(null)
    updateMutation.mutate(
      { contentId: content.id, allocationId, payload: values },
      { onError: (err) => setActionError(getErrorMessage(err)) }
    )
  }

  const handleSubmitForReview = () => {
    if (!content) return
    setActionError(null)
    submitMutation.mutate(
      { contentId: content.id, allocationId },
      { onError: (err) => setActionError(getErrorMessage(err)) }
    )
  }

  // 1) 내 콘텐츠가 있다 — 상태에 맞춰 편집 폼 또는 읽기 전용 미리보기를 보여준다.
  if (content) {
    const editable = EDITABLE_STATUSES.includes(content.status)
    return (
      <div className="border-outline-variant mt-4 border-t pt-4">
        <div className="mb-3 flex items-center justify-between gap-2">
          <h4 className="text-label-md text-on-surface font-semibold">부스 소개 콘텐츠</h4>
          <Badge variant={STATUS_VARIANT[content.status]}>{STATUS_LABEL[content.status]}</Badge>
        </div>

        {content.status === 'CORRECTION_REQUESTED' && content.correctionMessage && (
          <p className="bg-error-container text-on-error-container text-body-md mb-3 rounded px-3 py-2">
            보완 요청 사유: {content.correctionMessage}
          </p>
        )}

        {editable ? (
          <>
            <BoothContentForm
              defaultValues={toFormValues(content)}
              onSubmit={handleUpdate}
              submitting={updateMutation.isPending}
              submitLabel="저장"
              formError={actionError}
            />
            <p className="text-label-sm text-on-surface-variant mt-3">
              수정한 내용을 저장한 뒤 검수를 요청하세요. 검수 요청 후에는 관리자가 승인하거나 보완을
              요청할 때까지 수정할 수 없습니다.
            </p>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              className="mt-2"
              loading={submitMutation.isPending}
              onClick={handleSubmitForReview}
            >
              검수 요청
            </Button>
          </>
        ) : (
          <ReadOnlyContentPreview
            title={content.title}
            companyDisplayName={content.companyDisplayName}
            companyDescription={content.companyDescription}
            boothDescription={content.boothDescription}
            productDescription={content.productDescription}
            footnote={
              content.status === 'UNDER_REVIEW'
                ? '관리자 승인을 기다리고 있습니다.'
                : content.status === 'PUBLISHED' && content.publishedAt
                  ? `공개일: ${formatDateTime(content.publishedAt)}`
                  : content.status === 'HIDDEN'
                    ? '관리자가 노출을 중지했습니다.'
                    : undefined
            }
          />
        )}
      </div>
    )
  }

  // 2) 콘텐츠가 없다 — 새로 작성하는 폼을 연다.
  return (
    <div className="border-outline-variant mt-4 border-t pt-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h4 className="text-label-md text-on-surface font-semibold">부스 소개 콘텐츠</h4>
        <Badge variant="neutral">작성 전</Badge>
      </div>
      {showCreateForm ? (
        <BoothContentForm
          defaultValues={EMPTY_FORM_VALUES}
          onSubmit={handleCreate}
          submitting={createMutation.isPending}
          submitLabel="작성하기"
          formError={actionError}
        />
      ) : (
        <div className="flex flex-col items-start gap-2">
          <p className="text-body-md text-on-surface-variant">
            아직 등록된 부스 소개가 없습니다. 방문객에게 보여줄 기업·부스 소개를 작성해 보세요.
          </p>
          <Button type="button" size="sm" onClick={() => setShowCreateForm(true)}>
            콘텐츠 작성하기
          </Button>
        </div>
      )}
    </div>
  )
}

const ReadOnlyContentPreview = ({
  title,
  companyDisplayName,
  companyDescription,
  boothDescription,
  productDescription,
  footnote,
}: {
  title: string
  companyDisplayName: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
  footnote?: string
}) => (
  <div className="flex flex-col gap-2">
    <p className="text-title-lg text-on-surface font-semibold">{title}</p>
    <p className="text-label-md text-on-surface-variant">{companyDisplayName}</p>
    {companyDescription && <p className="text-body-md text-on-surface">{companyDescription}</p>}
    {boothDescription && <p className="text-body-md text-on-surface">{boothDescription}</p>}
    {productDescription && <p className="text-body-md text-on-surface">{productDescription}</p>}
    {footnote && <p className="text-label-sm text-on-surface-variant mt-1">{footnote}</p>}
  </div>
)
