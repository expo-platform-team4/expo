import { api } from '@/lib/api'

import type { VenueHall, VenueZone, VirtualVenue } from '@/features/recruitment/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

export type { VenueHall, VenueZone, VirtualVenue } from '@/features/recruitment/api'

/**
 * 가상 장소·전시장·홀 관리자용 조회. `VirtualVenueController`/`VenueHallController`/
 * `VenueZoneController` — 전부 `/api/admin/**`, ADMIN 전용이다.
 *
 * 공개 조회(`/api/virtual-venues` 등, `features/recruitment/api.ts`)와 타입은 같지만
 * 경로가 다르다 — 이 화면은 관리자 전용이라 공개 API 대신 admin 경로를 그대로 쓴다.
 */
export const listAdminVirtualVenues = async (): Promise<VirtualVenue[]> => {
  const { data } = await api.get<ApiEnvelope<VirtualVenue[]>>('/admin/virtual-venues')
  return data.data
}

/** `GET /api/admin/virtual-venues/{venueId}/halls` — 장소 내 전시장 목록. */
export const listAdminVenueHalls = async (venueId: number): Promise<VenueHall[]> => {
  const { data } = await api.get<ApiEnvelope<VenueHall[]>>(`/admin/virtual-venues/${venueId}/halls`)
  return data.data
}

/** `GET /api/admin/venue-halls/{hallId}/zones` — 전시장 내 홀 목록. */
export const listAdminVenueZones = async (hallId: number): Promise<VenueZone[]> => {
  const { data } = await api.get<ApiEnvelope<VenueZone[]>>(`/admin/venue-halls/${hallId}/zones`)
  return data.data
}

/**
 * `PATCH /api/admin/virtual-venues/{venueId}/halls/{hallId}/layout` — 전시장 배치도 교체.
 *
 * `layoutFileId` 는 `POST /api/files`(purpose=VENUE_LAYOUT)로 먼저 올린 뒤 받은 id다 —
 * `features/file/api.ts` 의 범용 업로드를 그대로 재사용한다(프로필 이미지와 같은 2단계 패턴).
 */
export const updateVenueHallLayout = async (
  venueId: number,
  hallId: number,
  layoutFileId: number
): Promise<VenueHall> => {
  const { data } = await api.patch<ApiEnvelope<VenueHall>>(
    `/admin/virtual-venues/${venueId}/halls/${hallId}/layout`,
    { layoutFileId }
  )
  return data.data
}

/** `PATCH /api/admin/venue-halls/{hallId}/zones/{zoneId}/layout` — 홀 배치도 교체. */
export const updateVenueZoneLayout = async (
  hallId: number,
  zoneId: number,
  layoutFileId: number
): Promise<VenueZone> => {
  const { data } = await api.patch<ApiEnvelope<VenueZone>>(
    `/admin/venue-halls/${hallId}/zones/${zoneId}/layout`,
    { layoutFileId }
  )
  return data.data
}
