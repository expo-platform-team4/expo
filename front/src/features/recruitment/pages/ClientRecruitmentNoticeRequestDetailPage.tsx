'use client'

import { useParams } from 'next/navigation'

import { Badge, Card, CardTitle, ErrorState, LoadingBlock } from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import { useMyRecruitmentNoticeRequest } from '../hooks'
import { NOTICE_REQUEST_STATUS, VENUE_CONFLICT_STATUS, VENUE_DECISION } from '../statusLabels'

/** `/client/recruitment-notice-requests/{requestId}`. 내 모집공고 생성 요청 상세. CLIENT 전용. */
const ClientRecruitmentNoticeRequestDetailPage = () => {
  const params = useParams<{ requestId: string }>()
  const requestId = Number(params.requestId)

  const {
    data: request,
    isPending,
    isError,
    error,
    refetch,
  } = useMyRecruitmentNoticeRequest(Number.isFinite(requestId) ? requestId : null)

  if (!Number.isFinite(requestId)) {
    return <ErrorState error={new Error('잘못된 요청 주소입니다.')} />
  }

  if (isPending) {
    return <LoadingBlock label="요청 상세를 불러오는 중입니다" />
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <div className="mb-2 flex flex-wrap gap-2">
          <Badge variant={NOTICE_REQUEST_STATUS[request.status].variant}>
            {NOTICE_REQUEST_STATUS[request.status].label}
          </Badge>
          <Badge variant={VENUE_CONFLICT_STATUS[request.venueConflictStatus].variant}>
            {VENUE_CONFLICT_STATUS[request.venueConflictStatus].label}
          </Badge>
          <Badge variant={VENUE_DECISION[request.venueDecision].variant}>
            {VENUE_DECISION[request.venueDecision].label}
          </Badge>
        </div>
        <h1 className="text-headline-sm text-on-background font-semibold">{request.title}</h1>
      </div>

      <Card>
        <CardTitle>설명</CardTitle>
        <p className="text-body-md text-on-surface whitespace-pre-wrap">{request.description}</p>
      </Card>

      <Card>
        <CardTitle>일정</CardTitle>
        <dl className="text-body-md text-on-surface grid grid-cols-1 gap-2 sm:grid-cols-2">
          <div>
            <dt className="text-label-sm text-on-surface-variant">신청 시작</dt>
            <dd>{formatDateTime(request.applicationStartAt)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">신청 종료</dt>
            <dd>{formatDateTime(request.applicationEndAt)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">행사 시작</dt>
            <dd>{formatDateTime(request.eventStartAt)}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">행사 종료</dt>
            <dd>{formatDateTime(request.eventEndAt)}</dd>
          </div>
        </dl>
      </Card>

      <Card>
        <CardTitle>희망 장소</CardTitle>
        <dl className="text-body-md text-on-surface grid grid-cols-1 gap-2 sm:grid-cols-3">
          <div>
            <dt className="text-label-sm text-on-surface-variant">가상 장소 ID</dt>
            <dd>{request.virtualVenueId}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">전시관(홀) ID</dt>
            <dd>{request.venueHallId}</dd>
          </div>
          <div>
            <dt className="text-label-sm text-on-surface-variant">구역 ID</dt>
            <dd>{request.venueZoneIds.join(', ')}</dd>
          </div>
        </dl>
      </Card>

      <Card>
        <CardTitle>목표 참가 기업 수</CardTitle>
        <p className="text-body-md text-on-surface">{request.targetCompanyCount ?? '-'}개사</p>
      </Card>

      {request.requestedBoothConfig && (
        <Card>
          <CardTitle>희망 부스 구성</CardTitle>
          <pre className="text-label-sm text-on-surface bg-surface-container-low overflow-x-auto rounded p-3">
            {request.requestedBoothConfig}
          </pre>
        </Card>
      )}
    </div>
  )
}

export default ClientRecruitmentNoticeRequestDetailPage
