import { api } from '@/lib/api'
import type {
  RecruitmentNotice,
  RecruitmentNoticeRequest,
  RecruitmentNoticeStatus,
} from '@/features/recruitment/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

export type {
  RecruitmentNotice,
  RecruitmentNoticeRequest,
  RecruitmentNoticeRequestStatus,
  RecruitmentNoticeStatus,
  VenueConflictStatus,
  VenueDecision,
} from '@/features/recruitment/api'

/** `GET /api/admin/recruitment-notice-requests` — 모집공고 생성 요청 목록(관리자). */
export const listAdminNoticeRequests = async (): Promise<RecruitmentNoticeRequest[]> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNoticeRequest[]>>(
    '/admin/recruitment-notice-requests'
  )
  return data.data
}

/** `DecideVenueRequest` 와 짝이다. `decision` 은 `ALLOWED`·`CANCELED` 만 허용한다. */
export type DecideVenuePayload = {
  decision: 'ALLOWED' | 'CANCELED'
  reason?: string
}

/** `PATCH /api/admin/recruitment-notice-requests/{requestId}/venue-decision` — 장소 충돌 판정. */
export const decideVenue = async (
  requestId: number,
  payload: DecideVenuePayload
): Promise<RecruitmentNoticeRequest> => {
  const { data } = await api.patch<ApiEnvelope<RecruitmentNoticeRequest>>(
    `/admin/recruitment-notice-requests/${requestId}/venue-decision`,
    payload
  )
  return data.data
}

/** `GET /api/admin/recruitment-notices` — 기업 모집 공고 목록(관리자). */
export const listAdminNotices = async (): Promise<RecruitmentNotice[]> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNotice[]>>('/admin/recruitment-notices')
  return data.data
}

/** `CreateRecruitmentNoticeRequest`(admin dto) 와 짝이다. */
export type CreateAdminNoticePayload = {
  requestId: number
  title: string
  content: string
  eligibility?: string
  submissionRequirements?: string
  applicationStartAt: string
  applicationEndAt: string
}

/** `POST /api/admin/recruitment-notices` — 기업 모집 공고 초안 생성. */
export const createAdminNotice = async (
  payload: CreateAdminNoticePayload
): Promise<RecruitmentNotice> => {
  const { data } = await api.post<ApiEnvelope<RecruitmentNotice>>(
    '/admin/recruitment-notices',
    payload
  )
  return data.data
}

/** `POST /api/admin/recruitment-notices/{noticeId}/publish` — 공고 게시. */
export const publishAdminNotice = async (noticeId: number): Promise<RecruitmentNotice> => {
  const { data } = await api.post<ApiEnvelope<RecruitmentNotice>>(
    `/admin/recruitment-notices/${noticeId}/publish`
  )
  return data.data
}

/** `POST /api/admin/recruitment-notices/{noticeId}/close` — 공고 조기 마감. */
export const closeAdminNotice = async (noticeId: number): Promise<RecruitmentNotice> => {
  const { data } = await api.post<ApiEnvelope<RecruitmentNotice>>(
    `/admin/recruitment-notices/${noticeId}/close`
  )
  return data.data
}

/** `POST /api/admin/recruitment-notices/{noticeId}/cancel` — 공고 직권 취소. */
export const cancelAdminNotice = async (
  noticeId: number,
  reason?: string
): Promise<RecruitmentNotice> => {
  const { data } = await api.post<ApiEnvelope<RecruitmentNotice>>(
    `/admin/recruitment-notices/${noticeId}/cancel`,
    reason ? { reason } : undefined
  )
  return data.data
}

/** `RecruitmentNoticeStatus` 값 중 액션이 걸리는 조건을 한 곳에 모은다. 화면이 직접 상태를 나열하지 않는다. */
export const canPublishNotice = (status: RecruitmentNoticeStatus): boolean =>
  status === 'DRAFT' || status === 'SCHEDULED'
export const canCloseNotice = (status: RecruitmentNoticeStatus): boolean => status === 'OPEN'
export const canCancelNotice = (status: RecruitmentNoticeStatus): boolean =>
  status === 'DRAFT' || status === 'SCHEDULED' || status === 'OPEN'
