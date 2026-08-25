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

import { useAdminVenueReservations, useReleaseVenueReservation } from '../venueReservationHooks'
import type { AdminVenueReservation, VenueReservationStatus } from '../venueReservationApi'

const STATUS_LABEL: Record<VenueReservationStatus, string> = {
  CONFIRMED: '확정',
  RELEASED: '해제됨',
  CANCELED: '취소됨',
}

const STATUS_VARIANT: Record<VenueReservationStatus, 'success' | 'neutral' | 'error' | 'info'> = {
  CONFIRMED: 'success',
  RELEASED: 'neutral',
  CANCELED: 'error',
}

/** 예약 한 건 — CONFIRMED 상태일 때만 해제 액션을 제공한다. */
const ReservationRow = ({ reservation }: { reservation: AdminVenueReservation }) => {
  const [releasing, setReleasing] = useState(false)
  const [reason, setReason] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const releaseMutation = useReleaseVenueReservation()

  const handleRelease = () => {
    if (!reason.trim()) {
      setActionError('해제 사유를 입력하세요.')
      return
    }
    setActionError(null)
    releaseMutation.mutate(
      { reservationId: reservation.id, reason },
      {
        onSuccess: () => {
          setReleasing(false)
          setReason('')
        },
        onError: (err) => setActionError(getErrorMessage(err)),
      }
    )
  }

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <CardTitle className="mb-0.5">예약 #{reservation.id}</CardTitle>
          <p className="text-label-sm text-on-surface-variant">
            {reservation.reservationSourceType === 'RECRUITMENT_NOTICE'
              ? `모집공고 생성 요청 #${reservation.noticeRequestId}`
              : '박람회 직접 등록'}{' '}
            · 가상 장소 #{reservation.virtualVenueId} · 전시장 #{reservation.venueHallId} · 홀 #
            {reservation.venueZoneId}
          </p>
          <p className="text-label-sm text-on-surface-variant">
            사용 기간 {formatDateTime(reservation.useStartAt)} ~{' '}
            {formatDateTime(reservation.useEndAt)}
          </p>
        </div>
        <Badge variant={STATUS_VARIANT[reservation.status]}>
          {STATUS_LABEL[reservation.status]}
        </Badge>
      </div>

      {reservation.status === 'CONFIRMED' && !releasing && (
        <Button type="button" size="sm" variant="danger" onClick={() => setReleasing(true)}>
          해제
        </Button>
      )}

      {releasing && (
        <div className="flex flex-col gap-2">
          <Textarea
            label="해제 사유"
            hint="이중 예약 등 운영상 정정 전용입니다."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              variant="danger"
              loading={releaseMutation.isPending}
              onClick={handleRelease}
            >
              예약 해제
            </Button>
            <Button
              type="button"
              size="sm"
              variant="secondary"
              onClick={() => {
                setReleasing(false)
                setActionError(null)
              }}
            >
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
 * `/admin/venue-reservations`. ADMIN 전용.
 *
 * 확정 장소 예약 자체는 "모집공고 요청 관리" 화면에서 승인된(ALLOWED) 요청 카드의 "장소 예약
 * 확정" 버튼으로 만든다 — 여기는 만들어진 예약을 전체 조회하고, 이중 예약 등 운영상 정정이
 * 필요할 때 직권 해제하는 화면이다.
 */
const AdminVenueReservationListPage = () => {
  const { data: reservations, isPending, isError, error, refetch } = useAdminVenueReservations()

  return (
    <div>
      <PageHeader
        title="장소 예약 관리"
        description="확정된 장소 예약을 조회하고, 이중 예약 등 운영상 정정이 필요할 때 직권 해제합니다."
      />

      {isPending ? (
        <LoadingBlock label="예약 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : reservations.length === 0 ? (
        <EmptyState
          title="확정된 장소 예약이 없습니다"
          description="모집공고 요청 관리 화면에서 승인된 요청에 예약을 확정하면 여기 표시됩니다."
        />
      ) : (
        <div className="flex flex-col gap-3">
          {reservations.map((reservation) => (
            <ReservationRow key={reservation.id} reservation={reservation} />
          ))}
        </div>
      )}
    </div>
  )
}

export default AdminVenueReservationListPage
