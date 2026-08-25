import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 백엔드 `ExpoOpeningRequestStatus` 와 값이 같다. */
export type ExpoOpeningRequestStatus =
  'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'CANCELED'

export const EXPO_OPENING_STATUS_LABEL: Record<ExpoOpeningRequestStatus, string> = {
  DRAFT: '임시저장',
  SUBMITTED: '심사 요청됨',
  UNDER_REVIEW: '심사 중',
  APPROVED: '승인됨',
  REJECTED: '반려됨',
  CANCELED: '철회됨',
}

/**
 * 박람회 개최 신청. 백엔드 `ExpoOpeningRequestResponse` 와 짝이다.
 *
 * 비어 있을 수 있는 필드는 선택 필드(`?`)다 — 백엔드가 non_null 직렬화라 값이 없으면 키가
 * 아예 빠진다(Spec.md 1-1절).
 */
export type ExpoOpeningRequest = {
  id: number
  hostClientId: number
  hostCompanyName?: string
  title: string
  description: string
  eventStartAt: string
  eventEndAt: string
  salesStartAt: string
  salesEndAt: string
  desiredVenueId?: number
  desiredVenueName?: string
  desiredVenueHallId?: number
  desiredVenueZoneId?: number
  categoryIds?: number[]
  status: ExpoOpeningRequestStatus
  submittedAt?: string
  reviewedByAdminId?: number
  reviewedAt?: string
  rejectionReason?: string
  /** 승인된 건에만 있다. "내 박람회" 로 넘어가는 링크에 쓴다. */
  createdExpoId?: number
  createdAt: string
}

/** 백엔드 `ExpoOpeningRequestPayload` 와 짝이다. */
export type ExpoOpeningRequestPayload = {
  title: string
  description: string
  eventStartAt: string
  eventEndAt: string
  salesStartAt: string
  salesEndAt: string
  desiredVenueId: number
  desiredVenueHallId?: number
  desiredVenueZoneId?: number
  categoryIds?: number[]
}

/** `POST /api/client/expo-opening-requests` — 개최 신청 작성. */
export const createExpoOpeningRequest = async (
  content: ExpoOpeningRequestPayload,
  submitNow: boolean
): Promise<ExpoOpeningRequest> => {
  const { data } = await api.post<ApiEnvelope<ExpoOpeningRequest>>(
    '/client/expo-opening-requests',
    {
      content,
      submitNow,
    }
  )
  return data.data
}

/** `GET /api/client/expo-opening-requests` — 내 개최 신청 목록. */
export const listMyExpoOpeningRequests = async (): Promise<ExpoOpeningRequest[]> => {
  const { data } = await api.get<ApiEnvelope<ExpoOpeningRequest[]>>('/client/expo-opening-requests')
  return data.data
}

/** `POST /api/client/expo-opening-requests/{id}/submit` — 임시저장 → 심사 요청. */
export const submitExpoOpeningRequest = async (requestId: number): Promise<ExpoOpeningRequest> => {
  const { data } = await api.post<ApiEnvelope<ExpoOpeningRequest>>(
    `/client/expo-opening-requests/${requestId}/submit`
  )
  return data.data
}

/** `GET /api/admin/expo-opening-requests` — 관리자 심사 목록. */
export const listAdminExpoOpeningRequests = async (
  status?: ExpoOpeningRequestStatus
): Promise<ExpoOpeningRequest[]> => {
  const { data } = await api.get<ApiEnvelope<ExpoOpeningRequest[]>>(
    '/admin/expo-opening-requests',
    { params: status ? { status } : undefined }
  )
  return data.data
}

/** `POST /api/admin/expo-opening-requests/{id}/approve` — 승인. expos 행이 만들어진다. */
export const approveExpoOpeningRequest = async (requestId: number): Promise<ExpoOpeningRequest> => {
  const { data } = await api.post<ApiEnvelope<ExpoOpeningRequest>>(
    `/admin/expo-opening-requests/${requestId}/approve`
  )
  return data.data
}

/** `POST /api/admin/expo-opening-requests/{id}/reject` — 반려. 사유 필수. */
export const rejectExpoOpeningRequest = async (
  requestId: number,
  reason: string
): Promise<ExpoOpeningRequest> => {
  const { data } = await api.post<ApiEnvelope<ExpoOpeningRequest>>(
    `/admin/expo-opening-requests/${requestId}/reject`,
    { reason }
  )
  return data.data
}
