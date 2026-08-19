import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

export type ClientDashboardProfile = {
  clientUserId: number
  nickname: string
  companyName: string
  profileImageFileId: number | null
  profileImageStorageKey: string | null
  profileImageUpdatedAt: string | null
}

/** `GET /api/client/me/dashboard` — 클라이언트 마이페이지 상단 프로필(E-API-001). */
export const getClientDashboardProfile = async (): Promise<ClientDashboardProfile> => {
  const { data } = await api.get<ApiEnvelope<ClientDashboardProfile>>('/client/me/dashboard')
  return data.data
}

/** 박람회 심사 상태. 백엔드 `expos.review_status` CHECK 제약과 값이 같다. */
export type ExpoReviewStatus = 'DRAFT' | 'UNDER_REVIEW' | 'REJECTED' | 'APPROVED'
/** 공개 상태. 백엔드 `expos.visibility_status` CHECK 제약과 값이 같다. */
export type ExpoVisibilityStatus = 'PRIVATE' | 'PUBLIC' | 'ARCHIVED'
/** 행사 진행 상태. 백엔드 `expos.event_status` CHECK 제약과 값이 같다. */
export type ExpoEventStatus = 'SCHEDULED' | 'ONGOING' | 'ENDED' | 'CANCELED'

/** 화면에 그대로 보여줄 한국어 라벨. 서버 enum 값을 프론트에서 다시 번역하지 않도록 한 곳에 모은다. */
export const EXPO_REVIEW_STATUS_LABEL: Record<ExpoReviewStatus, string> = {
  DRAFT: '임시저장',
  UNDER_REVIEW: '심사중',
  REJECTED: '반려',
  APPROVED: '승인완료',
}

export const EXPO_EVENT_STATUS_LABEL: Record<ExpoEventStatus, string> = {
  SCHEDULED: '예정',
  ONGOING: '진행중',
  ENDED: '종료',
  CANCELED: '취소',
}

/** 백엔드 `ClientDashboardExpoResponse` 와 짝이다 (`GET /api/client/me/expos`). */
export type ClientDashboardExpo = {
  clientUserId: number
  expoId: number
  title: string
  eventStartAt: string
  eventEndAt: string
  salesStartAt: string
  salesEndAt: string
  reviewStatus: ExpoReviewStatus
  visibilityStatus: ExpoVisibilityStatus
  eventStatus: ExpoEventStatus
  approvedAt: string | null
  ticketProductCount: number
}

/** `GET /api/client/me/expos` — 내가 연 박람회 목록 (E-API-002). */
export const getClientMyExpos = async (): Promise<ClientDashboardExpo[]> => {
  const { data } = await api.get<ApiEnvelope<ClientDashboardExpo[]>>('/client/me/expos')
  return data.data
}

/** 백엔드 `ClientDashboardBoothResponse` 와 짝이다 (`GET /api/client/me/booths`). */
export type ClientDashboardBooth = {
  clientUserId: number
  recruitmentNoticeId: number
  applicationId: number
  applicationStatus: string
  boothOrderId: number | null
  boothOrderStatus: string | null
  paymentStatus: string | null
  paidAt: string | null
  boothAllocationId: number | null
  allocationStatus: string | null
  boothNumber: string | null
}

/** `GET /api/client/me/booths` — 확정 배정 부스 목록 (E-API-009). */
export const getClientMyConfirmedBooths = async (): Promise<ClientDashboardBooth[]> => {
  const { data } = await api.get<ApiEnvelope<ClientDashboardBooth[]>>('/client/me/booths')
  return data.data
}

/** 백엔드 `ClientDashboardRecruitmentResponse` 와 짝이다 (`GET /api/client/me/recruitment-results`). */
export type ClientDashboardRecruitment = {
  hostClientId: number
  recruitmentNoticeId: number
  title: string
  status: string
  applicationStartAt: string
  applicationEndAt: string
  publishedAt: string | null
  closedAt: string | null
  submittedApplicationCount: number
  confirmedAllocationCount: number
}

/** `GET /api/client/me/recruitment-results` — 모집공고 신청·확정 배정 현황 (E-API-005). */
export const getClientMyRecruitmentResults = async (): Promise<ClientDashboardRecruitment[]> => {
  const { data } = await api.get<ApiEnvelope<ClientDashboardRecruitment[]>>(
    '/client/me/recruitment-results'
  )
  return data.data
}

/** 백엔드 `ClientSettlementResponse` 와 짝이다 (`GET /api/client/settlements`). */
export type ClientSettlement = {
  settlementId: number
  expoId: number
  expoTitle: string
  eventEndAt: string
  settlementDueAt: string
  status: string
  grossTicketSalesAmount: number
  ticketRefundAmount: number
  netTicketSalesAmount: number
  bookingFeeGrossAmount: number
  bookingFeeRefundAmount: number
  bookingFeeNetAmount: number
  grossBoothSalesAmount: number
  adjustmentAmount: number
  remittanceDueAmount: number
  remittedAmount: number | null
  remittedAt: string | null
  remittanceStatus: string
  latestReportFileId: number | null
  latestReportFormat: string | null
  latestReportVersion: number | null
}

/** 백엔드 `SettlementPage<T>` 와 짝이다. Spec.md 2절 페이지네이션 봉투. */
export type ClientSettlementPage = {
  totalCount: number
  page: number
  size: number
  items: ClientSettlement[]
}

/**
 * `GET /api/client/settlements` — 내 정산 목록 (D-API-011).
 *
 * "내 박람회" 화면의 매출 요약은 박람회별로 하나씩 있는 정산 건을 `expoId` 로 매칭해 쓴다.
 * 화면에 박람회가 많지 않은 MVP 라 페이지네이션 UI 없이 한 번에 넉넉히(`size=100`) 가져온다.
 */
export const getClientSettlements = async (page = 0, size = 100): Promise<ClientSettlementPage> => {
  const { data } = await api.get<ApiEnvelope<ClientSettlementPage>>('/client/settlements', {
    params: { page, size },
  })
  return data.data
}

/** `ClientSettlementResponse.status` 값. `ClientSettlementController` Swagger 설명에 있는 일곱 가지다. */
export const SETTLEMENT_STATUS_LABEL: Record<string, string> = {
  WAITING: '집계 대기',
  CALCULATED: '집계 완료',
  UNDER_REVIEW: '검토중',
  CONFIRMED: '확정',
  REMITTANCE_PENDING: '송금 대기',
  REMITTED: '송금 완료',
  ON_HOLD: '보류',
}
