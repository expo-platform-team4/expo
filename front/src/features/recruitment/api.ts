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

/** `GET /api/admin/recruitment-notices` — 상태 무관 전체 공고 목록. ADMIN 전용. */
export const listAdminRecruitmentNotices = async (): Promise<RecruitmentNotice[]> => {
  const { data } = await api.get<ApiEnvelope<RecruitmentNotice[]>>('/admin/recruitment-notices')
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
  expoId: number | null
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
  /**
   * 장소 예약이 확정되어 있는지. **공고 초안을 만들 수 있는 조건 중 하나다.**
   *
   * 예약이 한 건도 없거나 해제된 예약이 섞여 있으면 `false` 다.
   */
  venueReservationConfirmed: boolean
  /** 이 요청으로 만든 공고가 이미 있는지. 요청 하나당 공고는 하나다. */
  noticeCreated: boolean
  createdAt: string
  updatedAt: string
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

/** `GET /api/virtual-venues` — 가상 장소 목록. 공개. */
export const listVirtualVenues = async (): Promise<VirtualVenue[]> => {
  const { data } = await api.get<ApiEnvelope<VirtualVenue[]>>('/virtual-venues')
  return data.data
}

/** 홀. `VenueHallResponse` 와 필드가 대응한다. */
export type VenueHall = {
  id: number
  venueId: number
  hallCode: string
  name: string
  width: number | null
  depth: number | null
  layoutFileId: number | null
  operationalStatus: OperationalStatus
  createdAt: string
  updatedAt: string
}

/**
 * `GET /api/virtual-venues/{venueId}/halls` — 장소 내 홀 목록. 공개.
 *
 * 전에는 홀·구역 목록 조회가 `/api/admin/**` 전용이라 CLIENT 가 부를 수 없어 공고 생성 요청
 * 폼에서 ID 직접 입력을 받았다(이슈 #107). `PublicVenueHallController`/`PublicVenueZoneController`
 * 로 공개 엔드포인트가 추가되어(PR #110) 이제 실제 드롭다운으로 고를 수 있다.
 */
export const listVenueHalls = async (virtualVenueId: number): Promise<VenueHall[]> => {
  const { data } = await api.get<ApiEnvelope<VenueHall[]>>(
    `/virtual-venues/${virtualVenueId}/halls`
  )
  return data.data
}

/** 구역. `VenueZoneResponse` 와 필드가 대응한다. */
export type VenueZone = {
  id: number
  hallId: number
  zoneCode: string
  name: string
  maxBoothCount: number
  width: number | null
  depth: number | null
  layoutFileId: number | null
  operationalStatus: OperationalStatus
  createdAt: string
  updatedAt: string
}

/** `GET /api/venue-halls/{hallId}/zones` — 홀 내 구역 목록. 공개. */
export const listVenueZones = async (hallId: number): Promise<VenueZone[]> => {
  const { data } = await api.get<ApiEnvelope<VenueZone[]>>(`/venue-halls/${hallId}/zones`)
  return data.data
}
