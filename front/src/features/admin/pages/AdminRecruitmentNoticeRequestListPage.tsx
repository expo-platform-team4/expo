'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
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
  Textarea,
} from '@/components/ui'
import type { RecruitmentNoticeRequest } from '@/features/recruitment/api'
import {
  NOTICE_REQUEST_STATUS,
  VENUE_CONFLICT_STATUS,
  VENUE_DECISION,
} from '@/features/recruitment/statusLabels'
import { formatDateTime, toDatetimeLocalValue } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { useAdminNoticeRequests, useDecideVenue } from '../recruitmentHooks'
import {
  createVenueReservationSchema,
  decideVenueSchema,
  type CreateVenueReservationFormValues,
  type DecideVenueFormValues,
} from '../schemas'
import { useCreateVenueReservations } from '../venueReservationHooks'

/**
 * 요청 한 건의 확정 장소 예약 생성 폼. `venueDecision` 이 `ALLOWED` 일 때만 펼쳐 보인다.
 *
 * 장소·홀·구역은 이미 요청에 실려있어(희망 장소) 다시 고르지 않는다 — 사용 기간만 받는다.
 * 이미 예약이 확정된 요청에 또 만들면 백엔드가 기간 겹침으로 거절한다(하나만 확정될 수 있는
 * 건 아니고, 같은 구역·기간 조합만 막힌다 — 여기서 중복 여부를 미리 알 방법이 없어 에러
 * 메시지로 알린다).
 */
const VenueReservationForm = ({
  request,
  onDone,
}: {
  request: RecruitmentNoticeRequest
  onDone: () => void
}) => {
  const createMutation = useCreateVenueReservations()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateVenueReservationFormValues>({
    resolver: zodResolver(createVenueReservationSchema),
    defaultValues: {
      useStartAt: toDatetimeLocalValue(request.eventStartAt),
      useEndAt: toDatetimeLocalValue(request.eventEndAt),
    },
  })

  const onSubmit = (values: CreateVenueReservationFormValues) => {
    createMutation.mutate(
      {
        noticeRequestId: request.id,
        useStartAt: new Date(values.useStartAt).toISOString(),
        useEndAt: new Date(values.useEndAt).toISOString(),
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
      <Input
        label="사용 시작 일시"
        type="datetime-local"
        error={errors.useStartAt?.message}
        {...register('useStartAt')}
      />
      <Input
        label="사용 종료 일시"
        type="datetime-local"
        error={errors.useEndAt?.message}
        {...register('useEndAt')}
      />
      {createMutation.isError && (
        <p className="text-label-sm text-error">{getErrorMessage(createMutation.error)}</p>
      )}
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={createMutation.isPending}>
          예약 확정
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onDone}>
          취소
        </Button>
      </div>
    </form>
  )
}

/** 요청 한 건의 장소 충돌 판정 폼. `venueDecision` 이 `PENDING` 일 때만 펼쳐 보인다. */
const VenueDecisionForm = ({ requestId, onDone }: { requestId: number; onDone: () => void }) => {
  const decideMutation = useDecideVenue()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<DecideVenueFormValues>({
    resolver: zodResolver(decideVenueSchema),
    defaultValues: { decision: 'ALLOWED', reason: '' },
  })

  const onSubmit = (values: DecideVenueFormValues) => {
    decideMutation.mutate(
      {
        requestId,
        payload: { decision: values.decision, reason: values.reason || undefined },
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
      <Select label="장소 결정" error={errors.decision?.message} {...register('decision')}>
        <option value="ALLOWED">장소 허용</option>
        <option value="CANCELED">장소 취소</option>
      </Select>
      <Textarea label="판정 사유" rows={2} error={errors.reason?.message} {...register('reason')} />
      {decideMutation.isError && (
        <p className="text-label-sm text-error">{getErrorMessage(decideMutation.error)}</p>
      )}
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={decideMutation.isPending}>
          판정 제출
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onDone}>
          취소
        </Button>
      </div>
    </form>
  )
}

const RequestCard = ({ request }: { request: RecruitmentNoticeRequest }) => {
  const [deciding, setDeciding] = useState(false)
  const [reserving, setReserving] = useState(false)

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-title-lg text-primary font-semibold">{request.title}</h3>
        <div className="flex flex-wrap gap-2">
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
      </div>
      <p className="text-label-sm text-on-surface-variant mt-3">
        주최 클라이언트 #{request.hostClientId} · 목표 참가 기업 {request.targetCompanyCount ?? '-'}
        개사
      </p>
      <p className="text-label-sm text-on-surface-variant mt-1">
        행사 기간 {formatDateTime(request.eventStartAt)} ~ {formatDateTime(request.eventEndAt)}
      </p>
      <p className="text-label-sm text-on-surface-variant mt-1">
        희망 장소 — 가상 장소 #{request.virtualVenueId} · 전시관 #{request.venueHallId} · 구역{' '}
        {request.venueZoneIds.join(', ')}
      </p>

      {request.venueDecision === 'PENDING' &&
        (deciding ? (
          <VenueDecisionForm requestId={request.id} onDone={() => setDeciding(false)} />
        ) : (
          <div className="mt-3">
            <Button size="sm" onClick={() => setDeciding(true)}>
              장소 충돌 판정하기
            </Button>
          </div>
        ))}

      {request.venueDecision === 'ALLOWED' &&
        (reserving ? (
          <VenueReservationForm request={request} onDone={() => setReserving(false)} />
        ) : (
          <div className="mt-3">
            <Button size="sm" variant="secondary" onClick={() => setReserving(true)}>
              장소 예약 확정
            </Button>
          </div>
        ))}
    </Card>
  )
}

/** `/admin/recruitment-notice-requests`. Function.md 4절 — "장소 결정(venue-decision) 포함". ADMIN 전용. */
const AdminRecruitmentNoticeRequestListPage = () => {
  const { data: requests, isPending, isError, error, refetch } = useAdminNoticeRequests()

  return (
    <div>
      <PageHeader
        title="모집공고 요청 관리"
        description="승인된 박람회를 골라 모집공고 생성 요청을 작성하고, 장소 충돌을 판정합니다."
        action={
          <Link href="/admin/recruitment-notice-requests/new">
            <Button>새 요청 작성</Button>
          </Link>
        }
      />

      {isPending ? (
        <LoadingBlock label="요청 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : requests.length === 0 ? (
        <EmptyState
          title="접수된 요청이 없습니다"
          description="승인된 박람회를 골라 모집공고 생성 요청을 작성해 보세요."
          action={
            <Link href="/admin/recruitment-notice-requests/new">
              <Button>새 요청 작성</Button>
            </Link>
          }
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {requests.map((request) => (
            <li key={request.id}>
              <RequestCard request={request} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default AdminRecruitmentNoticeRequestListPage
