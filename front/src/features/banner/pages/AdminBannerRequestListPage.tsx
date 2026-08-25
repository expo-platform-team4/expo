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
  LoadingBlock,
  PageHeader,
  Select,
  Textarea,
} from '@/components/ui'
import { useExpoCards } from '@/features/expo/hooks'
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { bannerImageUrl, type AdminBannerApplication, type BannerReviewStatus } from '../api'
import {
  useAdminBannerRequests,
  useApproveBannerRequest,
  useBannerConflicts,
  useRejectBannerRequest,
} from '../hooks'
import { rejectBannerSchema, type RejectBannerFormValues } from '../schemas'
import { BANNER_REVIEW_STATUS } from '../statusLabels'

/**
 * 겹치는 배너 목록. **눌렀을 때만 부른다.**
 *
 * 목록이 이미 "겹친다" 를 알려주므로, 관리자가 실제로 확인하려는 한 건에 대해서만 조회한다.
 */
const ConflictList = ({ requestId }: { requestId: number }) => {
  const { data: conflicts, isPending } = useBannerConflicts(requestId)

  if (isPending) {
    return <LoadingBlock label="겹치는 배너를 불러오는 중입니다" />
  }
  if (!conflicts || conflicts.length === 0) {
    return <p className="text-label-sm text-on-surface-variant mt-2">겹치는 배너가 없습니다.</p>
  }
  return (
    <ul className="text-label-sm text-on-surface-variant mt-2 flex flex-col gap-1">
      {conflicts.map((banner) => (
        <li key={banner.id}>
          · {banner.headline ?? `배너 #${banner.id}`} (박람회 #{banner.expoId})
        </li>
      ))}
    </ul>
  )
}

/** 반려 폼. 사유가 신청자 화면에 그대로 보이므로 비워 둘 수 없다. */
const RejectForm = ({ requestId, onDone }: { requestId: number; onDone: () => void }) => {
  const rejectMutation = useRejectBannerRequest()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RejectBannerFormValues>({
    resolver: zodResolver(rejectBannerSchema),
    defaultValues: { reason: '' },
  })

  const onSubmit = (values: RejectBannerFormValues) => {
    rejectMutation.mutate({ requestId, reason: values.reason }, { onSuccess: onDone })
  }

  return (
    <form
      className="bg-surface-container-low mt-3 flex flex-col gap-3 rounded-md p-4"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <Textarea
        label="반려 사유"
        rows={2}
        hint="신청한 주최사에게 그대로 보입니다."
        error={errors.reason?.message}
        {...register('reason')}
      />
      {rejectMutation.isError && (
        <p className="text-label-sm text-error">{getErrorMessage(rejectMutation.error)}</p>
      )}
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={rejectMutation.isPending}>
          반려 제출
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onDone}>
          취소
        </Button>
      </div>
    </form>
  )
}

const RequestCard = ({
  request,
  expoTitle,
}: {
  request: AdminBannerApplication
  expoTitle: string | undefined
}) => {
  const [rejecting, setRejecting] = useState(false)
  const [showConflicts, setShowConflicts] = useState(false)
  const approveMutation = useApproveBannerRequest()
  const status = BANNER_REVIEW_STATUS[request.reviewStatus]
  const pending = request.reviewStatus === 'UNDER_REVIEW'

  return (
    <Card>
      <div className="flex gap-4">
        {/* eslint-disable-next-line @next/next/no-img-element -- 백엔드 프록시 경로라 Next 이미지 최적화 대상이 아니다. */}
        <img
          src={bannerImageUrl(request.imageFileId)}
          alt={request.headline ?? '신청된 배너 이미지'}
          className="h-20 w-32 shrink-0 rounded-md object-cover"
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-title-md text-on-surface truncate font-semibold">
              {expoTitle ?? `박람회 #${request.expoId}`}
            </h3>
            <div className="flex flex-wrap gap-2">
              <Badge variant={status.variant}>{status.label}</Badge>
              {request.hasPeriodConflict && <Badge variant="error">기간 충돌</Badge>}
            </div>
          </div>
          {request.headline && (
            <p className="text-label-sm text-on-surface-variant mt-1 truncate">
              {request.headline}
            </p>
          )}
          <p className="text-label-sm text-on-surface-variant mt-2">
            희망 노출 {formatDateTime(request.requestedStartAt)} ~{' '}
            {formatDateTime(request.requestedEndAt)}
          </p>
          <p className="text-label-sm text-on-surface-variant mt-1">
            신청 주최사 #{request.clientUserId} · 접수 {formatDateTime(request.createdAt)}
          </p>
        </div>
      </div>

      {/*
        충돌은 반려 사유가 아니라 판단 재료다. 슬롯 정원이 남으면 겹쳐도 승인할 수 있어서
        승인을 막지 않고 무엇과 겹치는지 볼 수단만 준다.
      */}
      {request.hasPeriodConflict && (
        <div className="mt-3">
          <Button variant="secondary" size="sm" onClick={() => setShowConflicts(!showConflicts)}>
            {showConflicts ? '겹치는 배너 접기' : '겹치는 배너 보기'}
          </Button>
          {showConflicts && <ConflictList requestId={request.id} />}
        </div>
      )}

      {pending &&
        (rejecting ? (
          <RejectForm requestId={request.id} onDone={() => setRejecting(false)} />
        ) : (
          <div className="mt-3 flex gap-2">
            <Button
              size="sm"
              loading={approveMutation.isPending}
              onClick={() => approveMutation.mutate(request.id)}
            >
              승인
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setRejecting(true)}>
              반려
            </Button>
          </div>
        ))}

      {approveMutation.isError && (
        <p className="text-label-sm text-error mt-2">{getErrorMessage(approveMutation.error)}</p>
      )}
    </Card>
  )
}

const STATUS_FILTERS: { value: '' | BannerReviewStatus; label: string }[] = [
  { value: 'UNDER_REVIEW', label: '심사 대기' },
  { value: '', label: '전체' },
  { value: 'APPROVED', label: '승인됨' },
  { value: 'REJECTED', label: '반려됨' },
]

/**
 * `/admin/banner-requests` — 배너 신청 심사. ADMIN 전용.
 *
 * 기본 필터가 **심사 대기**다. 이 화면에 들어오는 이유가 처리할 일을 보기 위해서고, 전체를
 * 먼저 보여주면 이미 끝난 건에 묻힌다.
 */
const AdminBannerRequestListPage = () => {
  const [status, setStatus] = useState<'' | BannerReviewStatus>('UNDER_REVIEW')
  const { data, isPending, isError, error, refetch } = useAdminBannerRequests(status || undefined)
  // 신청 응답에는 `expoId` 만 있다. 공개 박람회 목록 한 번으로 제목을 맞춘다 —
  // 행마다 상세를 부르면 목록 하나에 요청이 열 번 나간다. 배너는 공개된 박람회만 광고한다.
  const { data: expoCards } = useExpoCards()
  const titleOf = (expoId: number) => expoCards?.find((card) => card.expoId === expoId)?.title

  return (
    <div>
      <PageHeader
        title="배너 신청 심사"
        description="주최사의 광고 배너 노출 신청을 확인하고 승인하거나 반려합니다."
        action={
          <Select
            label="상태"
            value={status}
            onChange={(event) => setStatus(event.target.value as '' | BannerReviewStatus)}
          >
            {STATUS_FILTERS.map((filter) => (
              <option key={filter.label} value={filter.value}>
                {filter.label}
              </option>
            ))}
          </Select>
        }
      />

      {isPending ? (
        <LoadingBlock label="신청 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState
          title="해당하는 신청이 없습니다"
          description="주최사가 배너 노출을 신청하면 여기 표시됩니다."
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {data.content.map((request) => (
            <li key={request.id}>
              <RequestCard request={request} expoTitle={titleOf(request.expoId)} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default AdminBannerRequestListPage
