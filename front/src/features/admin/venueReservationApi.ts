import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }
type PageResponse<T> = { content: T[]; totalElements: number }

/** `com.expo.venue.entity.VenueReservationStatus`. */
export type VenueReservationStatus = 'CONFIRMED' | 'RELEASED' | 'CANCELED'

/** `com.expo.venue.entity.ReservationSourceType`. */
export type ReservationSourceType = 'RECRUITMENT_NOTICE' | 'EXPO_DIRECT'

/** 장소 예약. `com.expo.venue.dto.VenueReservationResponse` 와 대응한다. */
export type AdminVenueReservation = {
  id: number
  reservationSourceType: ReservationSourceType
  noticeRequestId: number | null
  virtualVenueId: number
  venueHallId: number
  venueZoneId: number
  useStartAt: string
  useEndAt: string
  status: VenueReservationStatus
  confirmedByAdminId: number | null
  confirmedAt: string | null
  releasedAt: string | null
}

/**
 * `POST /api/admin/venue-reservations` — 확정 장소 예약 생성. 장소·홀·구역은 이미 승인
 * (`venueDecision=ALLOWED`)된 모집공고 생성 요청에 실려있어, 여기서는 사용 기간만 받는다.
 * 그 요청이 고른 구역 개수만큼 한 번에 확정되고, 하나라도 실패하면 전체가 롤백된다.
 */
export const createVenueReservations = async (payload: {
  noticeRequestId: number
  useStartAt: string
  useEndAt: string
}): Promise<AdminVenueReservation[]> => {
  const { data } = await api.post<ApiEnvelope<AdminVenueReservation[]>>(
    '/admin/venue-reservations',
    payload
  )
  return data.data
}

/** `GET /api/admin/venue-reservations` — 장소 예약 목록(전체 상태). ADMIN 전용. */
export const listAdminVenueReservations = async (): Promise<AdminVenueReservation[]> => {
  const { data } = await api.get<ApiEnvelope<PageResponse<AdminVenueReservation>>>(
    '/admin/venue-reservations',
    { params: { size: 200 } }
  )
  return data.data.content
}

/** `PATCH /api/admin/venue-reservations/{reservationId}/release` — 확정 예약 직권 해제. */
export const releaseVenueReservation = async (
  reservationId: number,
  reason: string
): Promise<AdminVenueReservation> => {
  const { data } = await api.patch<ApiEnvelope<AdminVenueReservation>>(
    `/admin/venue-reservations/${reservationId}/release`,
    { reason }
  )
  return data.data
}
