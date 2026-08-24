import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `com.expo.participation.entity.ParticipationApplicationStatus`. */
export type ParticipationApplicationStatus =
  'DRAFT' | 'PAYMENT_PENDING' | 'SUBMITTED' | 'PAYMENT_FAILED' | 'CANCELED'

/** 참여 신청서. `ParticipationApplicationResponse` 와 필드가 대응한다. */
export type ParticipationApplication = {
  id: number
  recruitmentNoticeId: number
  companyNameSnapshot: string
  participationPurpose: string | null
  exhibitDescription: string | null
  selectedBoothProductId: number | null
  boothOrderId: number | null
  status: ParticipationApplicationStatus
  createdAt: string
  updatedAt: string
}

/** `POST /api/client/participation-applications` 요청 바디. `CreateParticipationApplicationRequest` 와 대응한다. */
export type CreateParticipationApplicationPayload = {
  recruitmentNoticeId: number
  companyNameSnapshot: string
  participationPurpose?: string
  exhibitDescription?: string
  selectedBoothProductId?: number
}

/** `POST /api/client/participation-applications` — 참여 신청서 작성. CLIENT 전용. */
export const createParticipationApplication = async (
  payload: CreateParticipationApplicationPayload
): Promise<ParticipationApplication> => {
  const { data } = await api.post<ApiEnvelope<ParticipationApplication>>(
    '/client/participation-applications',
    payload
  )
  return data.data
}

/** `GET /api/client/participation-applications/{applicationId}` — 내 참여 신청서 상세. */
export const getMyParticipationApplication = async (
  applicationId: number
): Promise<ParticipationApplication> => {
  const { data } = await api.get<ApiEnvelope<ParticipationApplication>>(
    `/client/participation-applications/${applicationId}`
  )
  return data.data
}

/** `PATCH /api/client/participation-applications/{applicationId}` 요청 바디. 초안 상태에서만 가능하다. */
export type UpdateParticipationApplicationPayload = {
  companyNameSnapshot: string
  participationPurpose?: string
  exhibitDescription?: string
  selectedBoothProductId?: number
}

/** `PATCH /api/client/participation-applications/{applicationId}` — 참여 신청서 초안 수정. */
export const updateParticipationApplication = async (
  applicationId: number,
  payload: UpdateParticipationApplicationPayload
): Promise<ParticipationApplication> => {
  const { data } = await api.patch<ApiEnvelope<ParticipationApplication>>(
    `/client/participation-applications/${applicationId}`,
    payload
  )
  return data.data
}

/**
 * `POST /api/client/participation-applications/{applicationId}/withdraw` — 참여 신청 철회.
 * 초안 상태에서만 가능하다 — 결제가 시작된 뒤에는 주문(`/client/booth-orders/{id}`)을 먼저
 * 취소해야 한다.
 */
export const withdrawParticipationApplication = async (
  applicationId: number
): Promise<ParticipationApplication> => {
  const { data } = await api.post<ApiEnvelope<ParticipationApplication>>(
    `/client/participation-applications/${applicationId}/withdraw`
  )
  return data.data
}

/**
 * 참여 신청 목록 한 행. `com.expo.booth.dto.ClientDashboardBoothResponse` 와 대응한다.
 *
 * **주의.** `participation` 패키지의 컨트롤러(`ParticipationApplicationController`)에는
 * 목록 API 가 없다(작성·단건 조회만 있다) — 목록은 `booth` 패키지의
 * `ClientParticipationDashboardController`(`GET /api/client/me/participations`)가 준다.
 * URL 프리픽스가 `/api/client/me` 인 이유도 그 컨트롤러 주석이 "데이터 소유 도메인은
 * booth 지만 URL 은 마이페이지 규약을 따른다" 고 밝힌 그대로다. 신청서 필드 자체(목적·전시
 * 품목 설명 등)는 이 뷰에 없다 — 필요하면 `applicationId` 로 `getMyParticipationApplication`
 * 을 따로 부른다.
 */
export type MyParticipation = {
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

/** `GET /api/client/me/participations` — 내가 참여기업으로 신청한 공고·결제·부스 목록. */
export const listMyParticipations = async (): Promise<MyParticipation[]> => {
  const { data } = await api.get<ApiEnvelope<MyParticipation[]>>('/client/me/participations')
  return data.data
}

/**
 * 부스 상품. `com.expo.booth.dto.BoothProductResponse` 와 대응한다.
 *
 * 참여 신청 폼의 "선택한 부스 상품" 드롭다운에 쓴다. 목록은 공개 API
 * (`GET /api/public/booth-products?recruitmentNoticeId=`)로 가져온다 — `booth` 화면 코드
 * (`features/booth/*`)를 건드리지 않고, 이 화면이 필요한 만큼만 직접 호출한다.
 */
export type BoothProduct = {
  id: number
  recruitmentNoticeId: number
  boothId: number
  boothNumber: string
  venueHallId: number
  venueHallName: string
  venueZoneId: number
  venueZoneName: string
  supplyPrice: string
  vatAmount: string
  totalPrice: string
  vatIncluded: boolean
  includedItems: string | null
  salesStartAt: string
  salesEndAt: string
  paymentEnabled: boolean
  salesStatus: string
  createdAt: string
  updatedAt: string
}

/** `GET /api/public/booth-products?recruitmentNoticeId=` — 공고별 구매 가능한 부스 상품 목록. 공개. */
export const listAvailableBoothProducts = async (
  recruitmentNoticeId: number
): Promise<BoothProduct[]> => {
  const { data } = await api.get<ApiEnvelope<BoothProduct[]>>('/public/booth-products', {
    params: { recruitmentNoticeId },
  })
  return data.data
}
