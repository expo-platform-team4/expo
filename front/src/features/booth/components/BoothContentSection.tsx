'use client'

import { useState } from 'react'

import { Badge, Button, ErrorState, Spinner } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'
import { formatDateTime } from '@/lib/date'

import type { BoothContent, BoothContentStatus } from '../api'
import {
  useCreateBoothContent,
  usePublishedBoothContent,
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
 * **알려진 배경 API 갭.** 배정 ID 로 "내 콘텐츠"(공개 여부 무관)를 조회하는 API 가 없다 —
 * 공개(PUBLISHED) 콘텐츠만 `GET /api/public/booth-contents/by-allocation/{id}` 로 볼 수 있고,
 * 초안·검수중·보완요청 콘텐츠는 `contentId` 를 알아야만 조회·수정할 수 있는데 그 ID 를
 * 배정 ID 로 찾는 방법이 없다(`api.ts` 의 `getPublishedBoothContent` 주석 참고). 그래서 이
 * 컴포넌트는 **이번 세션에서 만들거나 불러온 콘텐츠만** 계속 편집할 수 있다 — 작성/수정
 * 화면을 새로고침하면(아직 공개 전) 다시 "작성하기" 상태로 보이고, 재작성을 시도하면 서버가
 * 409 를 준다. 이 경우 에러 메시지에 그 사실을 함께 안내한다. 근본 해결은 백엔드에
 * "배정 ID 로 내 콘텐츠 조회" API 추가가 필요하다(작업 보고에서 이슈로 남긴다).
 */
export const BoothContentSection = ({ allocationId }: { allocationId: number }) => {
  const {
    data: publishedContent,
    isPending,
    isError,
    error,
    refetch,
  } = usePublishedBoothContent(allocationId)

  const [sessionContent, setSessionContent] = useState<BoothContent | null>(null)
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
        onSuccess: (content) => {
          setSessionContent(content)
          setShowCreateForm(false)
        },
        onError: (err) => {
          const message = getErrorMessage(err)
          const isDuplicate = message.includes('이미 해당 배정에 등록된')
          setActionError(
            isDuplicate
              ? `${message} 이미 작성된 콘텐츠는 공개(승인)되기 전까지 이 화면에서 다시 불러올 수 없습니다(알려진 제약). 공개 후 이 카드에서 미리보기로 확인할 수 있습니다.`
              : message
          )
        },
      }
    )
  }

  const handleUpdate = (values: BoothContentFormValues) => {
    if (!sessionContent) return
    setActionError(null)
    updateMutation.mutate(
      { contentId: sessionContent.id, payload: values },
      {
        onSuccess: (content) => setSessionContent(content),
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }

  const handleSubmitForReview = () => {
    if (!sessionContent) return
    setActionError(null)
    submitMutation.mutate(sessionContent.id, {
      onSuccess: (content) => setSessionContent(content),
      onError: (err) => setActionError(getErrorMessage(err)),
    })
  }

  // 1) 이번 세션에서 만들었거나 불러온 콘텐츠가 있다 — 그 상태에 맞춰 편집/읽기 전용을 보여준다.
  if (sessionContent) {
    const editable = EDITABLE_STATUSES.includes(sessionContent.status)
    return (
      <div className="border-outline-variant mt-4 border-t pt-4">
        <div className="mb-3 flex items-center justify-between gap-2">
          <h4 className="text-label-md text-on-surface font-semibold">부스 소개 콘텐츠</h4>
          <Badge variant={STATUS_VARIANT[sessionContent.status]}>
            {STATUS_LABEL[sessionContent.status]}
          </Badge>
        </div>

        {sessionContent.status === 'CORRECTION_REQUESTED' && sessionContent.correctionMessage && (
          <p className="bg-error-container text-on-error-container text-body-md mb-3 rounded px-3 py-2">
            보완 요청 사유: {sessionContent.correctionMessage}
          </p>
        )}

        {editable ? (
          <>
            <BoothContentForm
              defaultValues={toFormValues(sessionContent)}
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
            title={sessionContent.title}
            companyDisplayName={sessionContent.companyDisplayName}
            companyDescription={sessionContent.companyDescription}
            boothDescription={sessionContent.boothDescription}
            productDescription={sessionContent.productDescription}
            footnote={
              sessionContent.status === 'UNDER_REVIEW'
                ? '관리자 승인을 기다리고 있습니다.'
                : sessionContent.status === 'PUBLISHED' && sessionContent.publishedAt
                  ? `공개일: ${formatDateTime(sessionContent.publishedAt)}`
                  : sessionContent.status === 'HIDDEN'
                    ? '관리자가 노출을 중지했습니다.'
                    : undefined
            }
          />
        )}
      </div>
    )
  }

  // 2) 세션에서 만든 건 없지만, 이미 공개(PUBLISHED)된 콘텐츠가 있다 — 미리보기만 보여준다.
  if (publishedContent) {
    return (
      <div className="border-outline-variant mt-4 border-t pt-4">
        <div className="mb-3 flex items-center justify-between gap-2">
          <h4 className="text-label-md text-on-surface font-semibold">부스 소개 콘텐츠</h4>
          <Badge variant="success">공개됨</Badge>
        </div>
        <ReadOnlyContentPreview
          title={publishedContent.title}
          companyDisplayName={publishedContent.companyDisplayName}
          companyDescription={publishedContent.companyDescription}
          boothDescription={publishedContent.boothDescription}
          productDescription={publishedContent.productDescription}
          footnote={`공개일: ${formatDateTime(publishedContent.publishedAt)}`}
        />
      </div>
    )
  }

  // 3) 콘텐츠가 없다 — 새로 작성하는 폼을 연다.
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
