import { isAxiosError } from 'axios'

import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `com.expo.booth.entity.BoothAllocationStatus`. */
export type BoothAllocationStatus = 'ASSIGNED' | 'CANCELED' | 'REASSIGNED'

/** `com.expo.booth.entity.BoothContentStatus`. */
export type BoothContentStatus =
  'DRAFT' | 'UNDER_REVIEW' | 'PUBLISHED' | 'CORRECTION_REQUESTED' | 'HIDDEN'

/**
 * 확정 배정된 내 부스 한 행. `com.expo.booth.dto.ClientDashboardBoothResponse` 와 대응한다.
 *
 * `GET /api/client/me/booths` 가 준다 (`ClientBoothDashboardController`). `v_client_dashboard_booths`
 * 뷰를 `boothAllocationId != null` 로 필터링해 **확정 배정 건만** 돌려준다 — 신청만 하고 아직
 * 배정되지 않은 건은 여기 나오지 않는다(그건 `/client/participations` 화면, `features/participation/*`
 * 의 몫이다. 이 화면과 그 화면의 목적이 겹치지 않게 하는 경계선이다).
 */
export type MyConfirmedBooth = {
  clientUserId: number
  recruitmentNoticeId: number
  applicationId: number
  applicationStatus: string
  boothOrderId: number | null
  boothOrderStatus: string | null
  paymentStatus: string | null
  paidAt: string | null
  boothAllocationId: number
  allocationStatus: BoothAllocationStatus
  boothNumber: string | null
}

/** `GET /api/client/me/booths` — 내 확정 배정 부스 목록. CLIENT 전용. */
export const listMyConfirmedBooths = async (): Promise<MyConfirmedBooth[]> => {
  const { data } = await api.get<ApiEnvelope<MyConfirmedBooth[]>>('/client/me/booths')
  return data.data
}

/** 부스 확정 배정 상세. `com.expo.booth.dto.BoothAllocationResponse` 와 대응한다. */
export type BoothAllocation = {
  id: number
  applicationId: number
  boothOrderId: number
  boothProductId: number
  clientUserId: number
  allocatedAt: string
  status: BoothAllocationStatus
  canceledAt: string | null
  cancelReason: string | null
  createdAt: string
  updatedAt: string
}

/** `GET /api/client/booth-allocations/{allocationId}` — 내 배정 상세(배정·취소 일시, 취소 사유). */
export const getMyBoothAllocation = async (allocationId: number): Promise<BoothAllocation> => {
  const { data } = await api.get<ApiEnvelope<BoothAllocation>>(
    `/client/booth-allocations/${allocationId}`
  )
  return data.data
}

/**
 * 부스 콘텐츠(기업·부스 소개). `com.expo.booth.dto.BoothContentResponse` 와 대응한다.
 * 첨부 파일·외부 링크(`files`/`links`)는 이 화면 범위 밖이라 뺐다.
 */
export type BoothContent = {
  id: number
  boothAllocationId: number
  clientUserId: number
  companyDisplayName: string
  title: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
  status: BoothContentStatus
  publishedAt: string | null
  correctionRequestedAt: string | null
  correctionMessage: string | null
  createdAt: string
  updatedAt: string
}

export type BoothContentFormPayload = {
  companyDisplayName: string
  title: string
  companyDescription?: string
  boothDescription?: string
  productDescription?: string
}

/** `POST /api/client/booth-contents` — 부스 콘텐츠 작성. 확정 배정(ASSIGNED) 1건당 하나만 가질 수 있다. */
export const createBoothContent = async (
  boothAllocationId: number,
  payload: BoothContentFormPayload
): Promise<BoothContent> => {
  const { data } = await api.post<ApiEnvelope<BoothContent>>('/client/booth-contents', {
    boothAllocationId,
    ...payload,
  })
  return data.data
}

/** `PUT /api/client/booth-contents/{contentId}` — 콘텐츠 본문 수정. 초안·보완 요청 상태에서만 가능하다. */
export const updateBoothContent = async (
  contentId: number,
  payload: BoothContentFormPayload
): Promise<BoothContent> => {
  const { data } = await api.put<ApiEnvelope<BoothContent>>(
    `/client/booth-contents/${contentId}`,
    payload
  )
  return data.data
}

/** `POST /api/client/booth-contents/{contentId}/submit-for-review` — 검수 요청. 관리자 승인 후 공개된다. */
export const submitBoothContentForReview = async (contentId: number): Promise<BoothContent> => {
  const { data } = await api.post<ApiEnvelope<BoothContent>>(
    `/client/booth-contents/${contentId}/submit-for-review`
  )
  return data.data
}

/**
 * `GET /api/client/booth-contents/by-allocation/{allocationId}` — 배정 ID 로 **내** 콘텐츠를
 * 상태 무관(DRAFT·UNDER_REVIEW·PUBLISHED·CORRECTION_REQUESTED·HIDDEN)하게 조회한다. 로그인 필요.
 *
 * 이슈 #111 로 추가된 엔드포인트 — 전에는 `contentId` 를 이미 알아야 조회할 수 있어, 콘텐츠를
 * 만든 뒤 새로고침하면 다시 찾을 방법이 없었다(재작성 시도는 409 `DUPLICATE_BOOTH_CONTENT`).
 * 없으면 404 → `null`. 공개(`getPublishedBoothContent`)와는 반대로 소유자 본인만 보는
 * "내 콘텐츠 원본"이라 상태를 가리지 않는다 — 방문객에게 보여줄 공개 미리보기는 여전히
 * `getPublishedBoothContent` 를 쓴다.
 */
export const getMyBoothContentByAllocation = async (
  allocationId: number
): Promise<BoothContent | null> => {
  try {
    const { data } = await api.get<ApiEnvelope<BoothContent>>(
      `/client/booth-contents/by-allocation/${allocationId}`
    )
    return data.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null
    }
    throw error
  }
}

/** 공개(PUBLISHED)된 부스 콘텐츠. `com.expo.booth.dto.PublicBoothContentResponse` 와 대응한다. */
export type PublishedBoothContent = {
  id: number
  boothAllocationId: number
  companyDisplayName: string
  title: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
  publishedAt: string
}

/**
 * `GET /api/public/booth-contents/by-allocation/{allocationId}` — 배정 ID 로 공개된 콘텐츠를 조회한다.
 * 로그인이 필요 없다. **PUBLISHED 상태만** 돌려준다 — 방문객에게 보여줄 공개 미리보기용이다.
 * 소유자 본인이 상태 무관하게 자기 콘텐츠를 다시 찾을 때는 `getMyBoothContentByAllocation` 을 쓴다.
 *
 * 없으면(아직 공개 전) 404 → `null` 로 다룬다. 화면이 이걸 "에러"가 아니라 "콘텐츠 없음"
 * 상태로 렌더링해야 하기 때문이다.
 */
export const getPublishedBoothContent = async (
  allocationId: number
): Promise<PublishedBoothContent | null> => {
  try {
    const { data } = await api.get<ApiEnvelope<PublishedBoothContent>>(
      `/public/booth-contents/by-allocation/${allocationId}`
    )
    return data.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null
    }
    throw error
  }
}

/** `com.expo.booth.entity.BoothOrderStatus`. */
export type BoothOrderStatus =
  'PENDING_PAYMENT' | 'PAYMENT_COMPLETED' | 'FAILED' | 'CANCELED' | 'EXPIRED'

/** 부스 상품 주문. `com.expo.booth.dto.BoothOrderResponse` 와 대응한다. */
export type BoothOrder = {
  id: number
  applicationId: number
  clientUserId: number
  boothProductId: number
  orderNumber: string
  unitPrice: string
  totalAmount: string
  status: BoothOrderStatus
  expiresAt: string
  paidAt: string | null
  createdAt: string
  updatedAt: string
}

/**
 * `POST /api/client/booth-orders` — 부스 상품 주문 생성. 참여 신청서가 초안 상태이고 부스
 * 상품을 선택해뒀어야 한다. 생성과 동시에 15분간 그 상품을 임시 확보한다.
 */
export const createBoothOrder = async (applicationId: number): Promise<BoothOrder> => {
  const { data } = await api.post<ApiEnvelope<BoothOrder>>('/client/booth-orders', {
    applicationId,
  })
  return data.data
}

/** `GET /api/client/booth-orders/{orderId}` — 내 부스 상품 주문 상세. */
export const getMyBoothOrder = async (orderId: number): Promise<BoothOrder> => {
  const { data } = await api.get<ApiEnvelope<BoothOrder>>(`/client/booth-orders/${orderId}`)
  return data.data
}

/** `POST /api/client/booth-orders/{orderId}/cancel` — 결제 전 주문 취소. */
export const cancelBoothOrder = async (orderId: number): Promise<BoothOrder> => {
  const { data } = await api.post<ApiEnvelope<BoothOrder>>(`/client/booth-orders/${orderId}/cancel`)
  return data.data
}

/** 부스 상품 결제 시작 응답. `com.expo.booth.dto.InitiateBoothPaymentResponse` 와 대응한다. */
export type BoothPaymentInitiation = {
  boothPaymentId: number
  clientKey: string
  pgOrderId: string
  orderName: string
  amount: number
}

/** `POST /api/client/booth-orders/{orderId}/payments` — 부스 상품 결제 시작(토스). */
export const initiateBoothPayment = async (orderId: number): Promise<BoothPaymentInitiation> => {
  const { data } = await api.post<ApiEnvelope<BoothPaymentInitiation>>(
    `/client/booth-orders/${orderId}/payments`
  )
  return data.data
}

/** `com.expo.booth.entity.BoothPaymentStatus`. */
export type BoothPaymentStatus = 'READY' | 'IN_PROGRESS' | 'APPROVED' | 'CANCELED' | 'FAILED'

/** 부스 상품 결제 승인 결과. `com.expo.booth.dto.BoothPaymentResponse` 와 대응한다. */
export type BoothPaymentResult = {
  id: number
  boothOrderId: number
  pgOrderId: string
  paymentKey: string | null
  method: string | null
  status: BoothPaymentStatus
  requestedAmount: string
  approvedAmount: string | null
  approvedAt: string | null
  lastFailureCode: string | null
  createdAt: string
  updatedAt: string
}

export type ConfirmBoothPaymentPayload = {
  paymentKey: string
  /** 토스 결제창에서 돌아온 `orderId` 쿼리파라미터. `pgOrderId` 형태 그대로 보낸다. */
  orderId: string
  amount: number
}

/**
 * `POST /api/client/booth-payments/confirm` — 결제 승인 확정. 토스 결제창에서 돌아온 뒤
 * 이 화면(`BoothPaymentSuccessPage`)이 직접 호출해야 승인이 실제로 끝난다 — 결제창 리다이렉트
 * 만으로는 승인이 확정되지 않는다.
 */
export const confirmBoothPayment = async (
  payload: ConfirmBoothPaymentPayload
): Promise<BoothPaymentResult> => {
  const { data } = await api.post<ApiEnvelope<BoothPaymentResult>>(
    '/client/booth-payments/confirm',
    payload
  )
  return data.data
}

/** 부스 공간(물리적 자리). `com.expo.booth.dto.BoothResponse` 와 대응한다. */
export type AdminBooth = {
  id: number
  venueZoneId: number
  boothTemplateId: number | null
  boothNumber: string
  shapeCode: string
  width: string
  height: string | null
  depth: string
  dimensionUnit: string
  positionX: string | null
  positionY: string | null
  rotationDegree: string | null
  sortOrder: number
  operationalStatus: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
}

/** `POST /api/admin/venue-zones/{zoneId}/booths` 요청 바디 한 건. `CreateBoothRequest` 와 대응한다. */
export type CreateBoothPayload = {
  boothTemplateId?: number
  boothNumber: string
  shapeCode: string
  width: number
  height?: number
  depth: number
  dimensionUnit?: string
  positionX?: number
  positionY?: number
  rotationDegree?: number
  sortOrder?: number
}

/** `GET /api/admin/venue-zones/{zoneId}/booths` — 구역 내 부스 공간 목록. ADMIN 전용. */
export const listAdminBooths = async (zoneId: number): Promise<AdminBooth[]> => {
  const { data } = await api.get<ApiEnvelope<AdminBooth[]>>(`/admin/venue-zones/${zoneId}/booths`)
  return data.data
}

/**
 * `POST /api/admin/venue-zones/{zoneId}/booths/bulk` — 구역 안에 부스 공간 일괄 등록. ADMIN 전용.
 * 배치 안의 중복이든 기존 등록분과의 중복이든 하나라도 걸리면 전체가 저장되지 않는다.
 */
export const createBoothsBulk = async (
  zoneId: number,
  booths: CreateBoothPayload[]
): Promise<AdminBooth[]> => {
  const { data } = await api.post<ApiEnvelope<AdminBooth[]>>(
    `/admin/venue-zones/${zoneId}/booths/bulk`,
    { booths }
  )
  return data.data
}

/** `com.expo.booth.entity.BoothSalesStatus`. */
export type BoothSalesStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'UNAVAILABLE' | 'CANCELED'

/**
 * 관리자 관점의 부스 상품. `com.expo.booth.dto.BoothProductResponse` 와 대응한다.
 *
 * 참여 신청 화면(`features/participation/api.ts`)에도 같은 모양의 `BoothProduct` 타입이 따로
 * 있다 — 그쪽은 공개 API(구매 가능한 것만) 응답이고 이건 관리자 API(전체 상태) 응답이라, 일부러
 * 재사용하지 않고 이 화면이 필요한 만큼만 따로 둔다.
 */
export type AdminBoothProduct = {
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
  salesStartAt: string | null
  salesEndAt: string | null
  paymentEnabled: boolean
  salesStatus: BoothSalesStatus
  createdAt: string
  updatedAt: string
}

/** `GET /api/admin/booth-products?recruitmentNoticeId=` — 공고별 부스 상품 목록(전체 상태). ADMIN 전용. */
export const listAdminBoothProducts = async (
  recruitmentNoticeId: number
): Promise<AdminBoothProduct[]> => {
  const { data } = await api.get<ApiEnvelope<AdminBoothProduct[]>>('/admin/booth-products', {
    params: { recruitmentNoticeId },
  })
  return data.data
}

/** `POST /api/admin/booth-products` 요청 바디 한 건. `CreateBoothProductRequest` 와 대응한다. */
export type CreateBoothProductPayload = {
  recruitmentNoticeId: number
  boothId: number
  supplyPrice: number
  vatAmount?: number
  vatIncluded: boolean
  includedItems?: string
  salesStartAt?: string
  salesEndAt?: string
  paymentEnabled: boolean
}

/**
 * `POST /api/admin/booth-products/bulk` — 부스 상품 일괄 등록. ADMIN 전용. 배치 안에서 같은
 * (공고, 부스) 조합이 중복되거나 기존 등록분과 겹치면 하나라도 걸려 전체가 저장되지 않는다.
 */
export const createBoothProductsBulk = async (
  boothProducts: CreateBoothProductPayload[]
): Promise<AdminBoothProduct[]> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothProduct[]>>('/admin/booth-products/bulk', {
    boothProducts,
  })
  return data.data
}

/** Spring `Page<T>` 응답. 페이지네이션 컨트롤은 아직 두지 않고, 한 번에 넉넉히 불러와 목록만 보여준다. */
type PageResponse<T> = {
  content: T[]
  totalElements: number
}

/** 부스 확정 배정. `com.expo.booth.dto.BoothAllocationResponse` 와 대응한다. */
export type AdminBoothAllocation = {
  id: number
  applicationId: number
  boothOrderId: number
  boothProductId: number
  clientUserId: number
  allocatedAt: string
  status: BoothAllocationStatus
  canceledAt: string | null
  cancelReason: string | null
  createdAt: string
  updatedAt: string
}

/** `GET /api/admin/booth-allocations` — 부스 확정 배정 목록(전체 상태). ADMIN 전용. */
export const listAdminBoothAllocations = async (): Promise<AdminBoothAllocation[]> => {
  const { data } = await api.get<ApiEnvelope<PageResponse<AdminBoothAllocation>>>(
    '/admin/booth-allocations',
    { params: { size: 200 } }
  )
  return data.data.content
}

/**
 * `POST /api/admin/booth-allocations/{allocationId}/cancel` — 확정 배정 취소. 이중 배정 등
 * 운영상 정정 전용이다(일반적인 취소·환불 흐름과 다르다).
 */
export const cancelBoothAllocation = async (
  allocationId: number,
  reason: string
): Promise<AdminBoothAllocation> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothAllocation>>(
    `/admin/booth-allocations/${allocationId}/cancel`,
    { reason }
  )
  return data.data
}

/** `POST /api/admin/booth-allocations/{allocationId}/reassign` — 다른 부스 상품으로 재배정. */
export const reassignBoothAllocation = async (
  allocationId: number,
  boothProductId: number,
  reason: string
): Promise<AdminBoothAllocation> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothAllocation>>(
    `/admin/booth-allocations/${allocationId}/reassign`,
    { boothProductId, reason }
  )
  return data.data
}

/** 부스 콘텐츠 첨부 파일. `com.expo.booth.dto.BoothContentFileResponse` 와 대응한다. */
export type AdminBoothContentFile = {
  id: number
  fileId: number
  fileType: string
  title: string | null
  sortOrder: number
  createdAt: string
}

/** 부스 콘텐츠 외부 링크. `com.expo.booth.dto.ExternalLinkResponse` 와 대응한다. */
export type AdminBoothContentLink = {
  id: number
  linkType: string
  label: string
  url: string
  sortOrder: number
}

/** 관리자 관점의 부스 콘텐츠. `com.expo.booth.dto.BoothContentResponse` 와 대응한다. */
export type AdminBoothContent = {
  id: number
  boothAllocationId: number
  clientUserId: number
  companyDisplayName: string
  title: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
  logoFileId: number | null
  mainImageFileId: number | null
  status: BoothContentStatus
  publishedAt: string | null
  correctionRequestedAt: string | null
  correctionMessage: string | null
  checkedByAdminId: number | null
  checkedAt: string | null
  files: AdminBoothContentFile[]
  links: AdminBoothContentLink[]
  createdAt: string
  updatedAt: string
}

/** `GET /api/admin/booth-contents` — 부스 콘텐츠 목록(전체 상태). ADMIN 전용. */
export const listAdminBoothContents = async (): Promise<AdminBoothContent[]> => {
  const { data } = await api.get<ApiEnvelope<PageResponse<AdminBoothContent>>>(
    '/admin/booth-contents',
    { params: { size: 200 } }
  )
  return data.data.content
}

/** `POST /api/admin/booth-contents/{contentId}/check` — 운영 확인(검수 시작). */
export const checkBoothContent = async (contentId: number): Promise<AdminBoothContent> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothContent>>(
    `/admin/booth-contents/${contentId}/check`
  )
  return data.data
}

/** `POST /api/admin/booth-contents/{contentId}/approve` — 검수 승인(공개). */
export const approveBoothContent = async (contentId: number): Promise<AdminBoothContent> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothContent>>(
    `/admin/booth-contents/${contentId}/approve`
  )
  return data.data
}

/** `POST /api/admin/booth-contents/{contentId}/request-correction` — 보완 요청. */
export const requestBoothContentCorrection = async (
  contentId: number,
  message: string
): Promise<AdminBoothContent> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothContent>>(
    `/admin/booth-contents/${contentId}/request-correction`,
    { message }
  )
  return data.data
}

/** `POST /api/admin/booth-contents/{contentId}/hide` — 직권 숨김(공개 중인 콘텐츠를 내림). */
export const hideBoothContent = async (
  contentId: number,
  reason?: string
): Promise<AdminBoothContent> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothContent>>(
    `/admin/booth-contents/${contentId}/hide`,
    reason ? { reason } : undefined
  )
  return data.data
}

/** `POST /api/admin/booth-contents/{contentId}/restore` — 숨김 해제. */
export const restoreBoothContent = async (
  contentId: number,
  reason?: string
): Promise<AdminBoothContent> => {
  const { data } = await api.post<ApiEnvelope<AdminBoothContent>>(
    `/admin/booth-contents/${contentId}/restore`,
    reason ? { reason } : undefined
  )
  return data.data
}
