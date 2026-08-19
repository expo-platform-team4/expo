import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `com.expo.recruitment.entity.RecruitmentNoticeStatus`. */
export type RecruitmentNoticeStatus =
  'DRAFT' | 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'CANCELED' | 'ARCHIVED'

/** 기업 모집 공고. `RecruitmentNoticeResponse` 와 필드가 대응한다. */
export type RecruitmentNotice = {
  id: number
  requestId: number
  hostClientId: number
  venueHallId: number | null
  venueZoneIds: number[]
  title: string
  content: string
  eligibility: string | null
  submissionRequirements: string | null
  applicationStartAt: string
  applicationEndAt: string
  status: RecruitmentNoticeStatus
  publishedAt: string | null
  closedAt: string | null
  createdByAdminId: number | null
  createdAt: string
  updatedAt: string
}

/** `GET /api/recruitment-notices` — 게시 중인 공고 목록. 공개. */
export const listPublicRecruitmentNotices = async (): Promise<RecruitmentNotice[]> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNotice[]>>('/recruitment-notices')
  return data.data
}

/** `GET /api/recruitment-notices/{noticeId}` — 공고 상세·참여조건. 공개. */
export const getPublicRecruitmentNotice = async (noticeId: number): Promise<RecruitmentNotice> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNotice>>(`/recruitment-notices/${noticeId}`)
  return data.data
}

/** `com.expo.recruitment.entity.RecruitmentNoticeRequestStatus`. */
export type RecruitmentNoticeRequestStatus =
  'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'CANCELED'

/** `com.expo.recruitment.entity.VenueConflictStatus`. */
export type VenueConflictStatus = 'CLEAR' | 'CONFLICT_PENDING' | 'RESOLVED'

/** `com.expo.recruitment.entity.VenueDecision`. */
export type VenueDecision = 'PENDING' | 'ALLOWED' | 'CANCELED'

/** 모집공고 생성 요청. `RecruitmentNoticeRequestResponse` 와 필드가 대응한다. */
export type RecruitmentNoticeRequest = {
  id: number
  hostClientId: number
  title: string
  description: string
  applicationStartAt: string
  applicationEndAt: string
  eventStartAt: string
  eventEndAt: string
  virtualVenueId: number
  venueHallId: number
  venueZoneIds: number[]
  targetCompanyCount: number | null
  requestedBoothConfig: string | null
  status: RecruitmentNoticeRequestStatus
  venueConflictStatus: VenueConflictStatus
  venueDecision: VenueDecision
  createdAt: string
  updatedAt: string
}

/** `POST /api/client/recruitment-notice-requests` 요청 바디. `CreateRecruitmentNoticeRequestRequest` 와 대응한다. */
export type CreateRecruitmentNoticeRequestPayload = {
  title: string
  description: string
  applicationStartAt: string
  applicationEndAt: string
  eventStartAt: string
  eventEndAt: string
  virtualVenueId: number
  venueHallId: number
  venueZoneIds: number[]
  targetCompanyCount: number
  requestedBoothConfig?: string
}

/** `POST /api/client/recruitment-notice-requests` — 모집공고 생성 요청 작성. */
export const createRecruitmentNoticeRequest = async (
  payload: CreateRecruitmentNoticeRequestPayload
): Promise<RecruitmentNoticeRequest> => {
  const { data } = await api.post<ApiEnvelope<RecruitmentNoticeRequest>>(
    '/client/recruitment-notice-requests',
    payload
  )
  return data.data
}

/** `GET /api/client/recruitment-notice-requests` — 내 모집공고 생성 요청 목록. */
export const listMyRecruitmentNoticeRequests = async (): Promise<RecruitmentNoticeRequest[]> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNoticeRequest[]>>(
    '/client/recruitment-notice-requests'
  )
  return data.data
}

/** `GET /api/client/recruitment-notice-requests/{requestId}` — 내 모집공고 생성 요청 상세. */
export const getMyRecruitmentNoticeRequest = async (
  requestId: number
): Promise<RecruitmentNoticeRequest> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNoticeRequest>>(
    `/client/recruitment-notice-requests/${requestId}`
  )
  return data.data
}

/** `com.expo.venue.entity.OperationalStatus`. */
export type OperationalStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'

/**
 * 가상 장소. `VirtualVenueResponse` 와 필드가 대응한다.
 *
 * 공고 생성 요청 폼의 "희망 가상 장소" 선택지로 쓴다. `GET /api/virtual-venues` 는 공개
 * API 다(`/api/admin/**` 가 아니다) — `SecurityConfig` 의 `anyRequest().permitAll()` 로 열려 있다.
 */
export type VirtualVenue = {
  id: number
  name: string
  address: string | null
  regionCode: string | null
  description: string | null
  mapFileId: number | null
  operationalStatus: OperationalStatus
  createdAt: string
  updatedAt: string
}

/**
 * `GET /api/virtual-venues` — 가상 장소 목록. 공개.
 *
 * **주의.** 장소 하위의 홀·구역 목록 조회(`/api/admin/virtual-venues/{id}/halls`,
 * `/api/admin/venue-halls/{hallId}/zones`)는 `ADMIN` 전용이라 CLIENT 가 부를 수 없다
 * (`SecurityConfig` 의 `/api/admin/**` → `hasRole("ADMIN")`). 공고 생성 요청 폼에서 희망
 * 전시관·구역은 ID 직접 입력으로만 받는다 — Function.md 7절 원칙에 따라 화면은 만들되
 * 이 제약을 힌트 문구로 명시한다.
 */
export const listVirtualVenues = async (): Promise<VirtualVenue[]> => {
  const { data } = await api.get<ApiEnvelope<VirtualVenue[]>>('/virtual-venues')
  return data.data
}
