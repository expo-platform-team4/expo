import { api } from '@/lib/api'

/** 백엔드 공통 응답 봉투. */
type ApiEnvelope<T> = { success: boolean; data: T; message?: string }

/**
 * 지금 노출 중인 메인 배너 한 건. 백엔드 `ActiveBannerResponse` 와 짝이다.
 *
 * **이미지 URL 이 아니라 파일 ID 를 준다.** 화면은 `/api/files/{id}/content` 로 받아 그린다 —
 * 프로필 이미지와 같은 방식이다.
 */
export type ActiveBanner = {
  id: number
  /** 홍보 대상 박람회. 배너를 누르면 이 박람회 상세로 간다 */
  expoId: number
  imageFileId: number
  headline: string | null
  /** 노출 순서. 백엔드가 이미 이 순서로 정렬해 준다 */
  sortOrder: number
}

/**
 * 지금 노출 가능한 메인 배너. `GET /api/banners/active`.
 *
 * **로그인이 필요 없다.** 공개 화면에서 부르는 것이라 `SecurityConfig` 에 이 경로만
 * `permitAll` 로 열려 있다.
 *
 * 슬롯 정원(현재 5)만큼 잘라서, 노출 순서대로 온다. 정렬·개수 제한은 서버가 한다.
 */
export const fetchActiveBanners = async (): Promise<ActiveBanner[]> => {
  const { data } = await api.get<ApiEnvelope<ActiveBanner[]>>('/banners/active')
  return data.data
}

/** 배너 이미지 경로. Next 이미지 최적화를 타지 않는 백엔드 프록시 경로다. */
export const bannerImageUrl = (imageFileId: number): string => `/api/files/${imageFileId}/content`

// ---------------------------------------------------------------------------
// 배너 노출 신청 — 주최사(CLIENT) · 관리자(ADMIN)
// ---------------------------------------------------------------------------

/** 백엔드 `BannerApplication.ReviewStatus` 와 같은 값이어야 한다. */
export type BannerReviewStatus = 'DRAFT' | 'UNDER_REVIEW' | 'REJECTED' | 'APPROVED' | 'CANCELED'

/**
 * 배너 신청 한 건. 백엔드 `BannerApplicationResponse` 와 짝이다.
 *
 * 신청자 본인과 관리자만 보는 화면이라 반려 사유가 그대로 들어 있다.
 */
export type BannerApplication = {
  id: number
  clientUserId: number
  expoId: number
  imageFileId: number
  headline: string | null
  requestedStartAt: string
  requestedEndAt: string
  reviewStatus: BannerReviewStatus
  submittedAt: string | null
  reviewedByAdminId: number | null
  reviewedAt: string | null
  rejectionReason: string | null
  createdAt: string
  updatedAt: string
}

/**
 * 관리자 목록 항목. 신청 정보에 **기간 충돌 여부**가 붙는다.
 *
 * 실제로 어느 배너와 겹치는지는 `/{id}/conflicts` 를 따로 불러야 나온다 — 목록에서는
 * "겹친다/안 겹친다" 만 알면 되고, 겹치는 건만 펼쳐 보면 되기 때문이다.
 */
export type AdminBannerApplication = Omit<
  BannerApplication,
  'submittedAt' | 'reviewedByAdminId' | 'reviewedAt' | 'rejectionReason' | 'updatedAt'
> & { hasPeriodConflict: boolean }

/** Spring `Page<T>` 중 화면이 쓰는 부분만 추린 것. */
export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
}

/**
 * 한 페이지에 보여줄 건수.
 *
 * 배너 신청은 이미지가 딸린 카드라 한 화면에 많이 들어가지 않는다.
 */
export const BANNER_PAGE_SIZE = 10

export type CreateBannerRequestPayload = {
  expoId: number
  imageFileId: number
  headline?: string
  requestedStartAt: string
  requestedEndAt: string
}

/** `POST /api/client/banner-requests` — 신청. 등록과 동시에 심사 대기(UNDER_REVIEW)가 된다. */
export const createBannerRequest = async (payload: CreateBannerRequestPayload): Promise<number> => {
  const { data } = await api.post<ApiEnvelope<number>>('/client/banner-requests', payload)
  return data.data
}

/**
 * `GET /api/client/banner-requests` — 내 신청 목록. CLIENT 전용.
 *
 * **`page` 는 1부터 센다.** 저장소 안에서도 통일되어 있지 않다 — 알림 이력 API 는 0부터 센다.
 * 0을 보내면 에러가 아니라 **1페이지와 같은 결과**가 조용히 온다(백엔드가 `Math.max(page-1, 0)`
 * 로 눌러 버린다). 그래서 여기서 기본값을 1로 못 박고, 화면은 이 함수만 부른다.
 */
export const listMyBannerRequests = async (page = 1): Promise<PageResponse<BannerApplication>> => {
  const { data } = await api.get<ApiEnvelope<PageResponse<BannerApplication>>>(
    '/client/banner-requests',
    { params: { page, size: BANNER_PAGE_SIZE } }
  )
  return data.data
}

/**
 * `GET /api/admin/banner-requests` — 심사 목록. 상태를 비우면 전체다. ADMIN 전용.
 *
 * `page` 는 1부터 센다 — 위 함수와 같은 이유다.
 */
export const listAdminBannerRequests = async (
  status?: BannerReviewStatus,
  page = 1
): Promise<PageResponse<AdminBannerApplication>> => {
  const { data } = await api.get<ApiEnvelope<PageResponse<AdminBannerApplication>>>(
    '/admin/banner-requests',
    { params: { page, size: BANNER_PAGE_SIZE, ...(status ? { status } : {}) } }
  )
  return data.data
}

/** `GET /api/admin/banner-requests/{id}/conflicts` — 이 신청과 노출 기간이 겹치는 배너들. */
export const listBannerConflicts = async (requestId: number): Promise<ActiveBanner[]> => {
  const { data } = await api.get<ApiEnvelope<ActiveBanner[]>>(
    `/admin/banner-requests/${requestId}/conflicts`
  )
  return data.data
}

/** `POST /api/admin/banner-requests/{id}/approve` — 승인. 본문이 없다. 노출 배너가 만들어진다. */
export const approveBannerRequest = async (requestId: number): Promise<number> => {
  const { data } = await api.post<ApiEnvelope<number>>(
    `/admin/banner-requests/${requestId}/approve`
  )
  return data.data
}

/** `POST /api/admin/banner-requests/{id}/reject` — 반려. 사유가 신청자에게 그대로 보인다. */
export const rejectBannerRequest = async (params: {
  requestId: number
  reason: string
}): Promise<void> => {
  await api.post(`/admin/banner-requests/${params.requestId}/reject`, { reason: params.reason })
}

/** `POST /api/client/banner-requests/{id}/cancel` — 승인 전인 본인 신청을 취소한다. */
export const cancelBannerRequest = async (requestId: number): Promise<void> => {
  await api.post(`/client/banner-requests/${requestId}/cancel`)
}
