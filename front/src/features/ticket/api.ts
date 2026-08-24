import { api } from '@/lib/api'

/** 백엔드 공통 응답 봉투. `com.expo.common.response.ApiResponse`. */
type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/**
 * 구매 가능한 티켓 상품 한 종류. 백엔드 `PurchasableTicketProductResponse` 와 짝이다.
 *
 * `features/expo/api.ts` 에도 같은 모양의 타입·호출이 있다 — 화면 소유자가 다른 모듈이라
 * (이 작업은 `features/expo/*` 를 건드리지 않는다) 여기서 독립적으로 다시 정의한다.
 */
export type PurchasableTicketProduct = {
  ticketProductId: number
  name: string
  description: string
  price: number
  availableQuantity: number
  maxQuantityPerOrder: number
  salesStartAt: string
  salesEndAt: string
}

/** `GET /api/expos/{expoId}/ticket-products/purchasable`. 인증 불필요. */
export const fetchPurchasableTicketProducts = async (
  expoId: number
): Promise<PurchasableTicketProduct[]> => {
  const { data } = await api.get<ApiEnvelope<PurchasableTicketProduct[]>>(
    `/expos/${expoId}/ticket-products/purchasable`
  )
  return data.data
}

/** 백엔드 `TicketOrderItemRequest` 와 짝이다. */
export type TicketOrderItemPayload = {
  ticketProductId: number
  quantity: number
}

/** 백엔드 `TicketOrderItemResponse` 와 짝이다. */
export type TicketOrderItem = {
  ticketProductId: number
  ticketName: string
  unitPrice: number
  quantity: number
  itemSubtotalAmount: number
}

/**
 * 주문 상태. 백엔드 `TicketOrderStatus` 와 값이 같아야 한다.
 *
 * 결제 연동이 없어(위 컨텍스트 참고) 실제로 화면이 만나는 값은 항상 `PENDING` 이다 —
 * 나머지는 백엔드에 전이 경로가 없다는 걸 알면서도, 응답 타입 자체는 정직하게 전부 옮긴다.
 */
export type TicketOrderStatus = 'PENDING' | 'PAID' | 'CANCELED' | 'PAYMENT_FAILED' | 'EXPIRED'

/** 백엔드 `TicketOrderResponse` 와 짝이다. */
export type TicketOrder = {
  ticketOrderId: number
  orderNumber: string
  status: TicketOrderStatus
  items: TicketOrderItem[]
  ticketSubtotalAmount: number
  bookingFeeAmount: number
  totalAmount: number
  expiresAt: string
  createdAt: string
}

/** `POST /api/orders/member`. 로그인(MEMBER) 필요 — `api.ts` 인터셉터가 토큰을 붙인다. */
export const createMemberOrder = async (items: TicketOrderItemPayload[]): Promise<TicketOrder> => {
  const { data } = await api.post<ApiEnvelope<TicketOrder>>('/orders/member', { items })
  return data.data
}

export type GuestOrderPayload = {
  items: TicketOrderItemPayload[]
  name: string
  phoneNumber: string
  age: number
  password: string
}

/** 백엔드 `GuestTicketOrderReponse`(원문 오탈자) 와 짝이다. */
export type GuestTicketOrder = {
  ticketOrderResponse: TicketOrder
  guestName: string
  guestAge: number
}

/** `POST /api/orders/guest`. 인증 불필요. */
export const createGuestOrder = async (payload: GuestOrderPayload): Promise<GuestTicketOrder> => {
  const { data } = await api.post<ApiEnvelope<GuestTicketOrder>>('/orders/guest', payload)
  return data.data
}

export type GuestOrderSearchPayload = {
  orderNumber: string
  password: string
  phoneNumber: string
}

/** 백엔드 `GuestTicketSearchResponse` 와 짝이다. */
export type GuestOrderSearchResult = {
  orderNumber: string
  status: TicketOrderStatus
  items: TicketOrderItem[]
  ticketSubtotalAmount: number
  bookingFeeAmount: number
  totalAmount: number
}

/**
 * `POST /api/orders/search/guest`. 인증 불필요 — 주문번호·연락처·비밀번호 조합이 곧 인증이다.
 *
 * 이 주문 하나에 대한 조회 전용 API 다. **GET-by-id 엔드포인트는 없다** — 그래서
 * `/orders/guest/{orderNumber}` 상세 화면도 이 함수를 재사용해 재조회한다
 * (`pages/GuestOrderDetailPage.tsx` 참고).
 */
export const searchGuestOrder = async (
  payload: GuestOrderSearchPayload
): Promise<GuestOrderSearchResult> => {
  const { data } = await api.post<ApiEnvelope<GuestOrderSearchResult>>(
    '/orders/search/guest',
    payload
  )
  return data.data
}

// ---------------------------------------------------------------------------
// 마이페이지 — 예매 내역 · 나의 티켓 (A-API-019~022, `member` 도메인
// `MemberOrderController`·`MemberTicketController` 와 짝이다)
// ---------------------------------------------------------------------------

/** 백엔드 `MemberOrderResponse` 와 짝이다. 목록·상세를 나누지 않는다 — 항목 하나가 곧 상세다. */
export type MemberOrder = {
  orderId: number
  orderNumber: string
  expoId: number | null
  expoTitle: string | null
  orderStatus: TicketOrderStatus
  paymentStatus: string | null
  refundStatus: string | null
  totalQuantity: number
  ticketSubtotalAmount: number
  bookingFeeRate: number
  bookingFeeAmount: number
  totalAmount: number
  refundable: boolean
  createdAt: string
}

/** `GET /api/users/me/orders` — 로그인한 회원 본인의 주문 최신순 전체. */
export const fetchMyOrders = async (): Promise<MemberOrder[]> => {
  const { data } = await api.get<ApiEnvelope<MemberOrder[]>>('/users/me/orders')
  return data.data
}

/** 백엔드 `MemberTicketResponse` 와 짝이다. */
export type MemberTicket = {
  orderId: number
  issuedTicketId: number
  ticketCode: string
  status: 'ISSUED' | 'CHECKED_IN' | 'CANCELED' | 'INVALIDATED'
  checkedInAt: string | null
  /** QR 원문. 입장에 쓸 수 없는 티켓(CANCELED·INVALIDATED)이면 null. */
  qrPayload: string | null
  orderItemQuantity: number
}

/** 백엔드 `MemberTicketGroupResponse` 와 짝이다. 박람회 1개 = 카드 1개. */
export type MemberTicketGroup = {
  expoId: number
  expoTitle: string
  eventStartAt: string
  eventEndAt: string
  tickets: MemberTicket[]
}

/** `GET /api/users/me/tickets` — 로그인한 회원 본인의 발권 티켓을 박람회별로 묶어 전체. */
export const fetchMyTickets = async (): Promise<MemberTicketGroup[]> => {
  const { data } = await api.get<ApiEnvelope<MemberTicketGroup[]>>('/users/me/tickets')
  return data.data
}

// ---------------------------------------------------------------------------
// 주최사(CLIENT) — 티켓 상품 관리 (`TicketController` 의 `/client/expos/{expoId}/**` 와 짝이다)
// ---------------------------------------------------------------------------

/**
 * 백엔드 `TicketProductStatus` 와 값이 같아야 한다.
 *
 * `SOLD_OUT`·`SALE_ENDED` 는 재고·판매기간에서 계산돼야 할 값인데 그 계산 로직이 백엔드에
 * 아직 없다(2026-08-24 QA 로 확인) — 지금은 어떤 API 도 이 두 값을 만들어내지 않는다.
 * 타입은 정직하게 다섯 값 다 옮기되, 화면에서 주최사가 직접 고를 수 있는 목적지는
 * `ON_SALE`·`CANCELED` 뿐이다(`updateTicketProductStatus` 참고).
 */
export type TicketProductStatus = 'DRAFT' | 'ON_SALE' | 'SOLD_OUT' | 'SALE_ENDED' | 'CANCELED'

/**
 * 주최사가 보는 티켓 상품 하나. 백엔드 `TicketProductSearchResponse`·`TicketProductCreateResponse`
 * ·상태 전환 응답이 공유하는 필드만 모았다(세 응답이 이 부분은 모양이 같다).
 */
export type ClientTicketProduct = {
  ticketProductId: number
  name: string
  description: string | null
  price: number
  salesStartAt: string
  salesEndAt: string
  totalQuantity: number
  availableQuantity: number
  maxQuantityPerOrder: number
  status: TicketProductStatus
}

/** `GET /api/client/expos/{expoId}/ticket-search`. 로그인(주최사 CLIENT) 필요. */
export const fetchClientTicketProducts = async (expoId: number): Promise<ClientTicketProduct[]> => {
  const { data } = await api.get<ApiEnvelope<ClientTicketProduct[]>>(
    `/client/expos/${expoId}/ticket-search`
  )
  return data.data
}

/** 백엔드 `TicketProductCreateRequest` 와 짝이다. */
export type CreateTicketProductPayload = {
  name: string
  description?: string
  price: number
  salesStartAt: string
  salesEndAt: string
  totalQuantity: number
  maxQuantityPerOrder: number
}

/** `POST /api/client/expos/{expoId}/ticket-products`. 새 상품은 항상 `DRAFT` 로 시작한다. */
export const createTicketProduct = async (
  expoId: number,
  payload: CreateTicketProductPayload
): Promise<ClientTicketProduct> => {
  const { data } = await api.post<ApiEnvelope<ClientTicketProduct>>(
    `/client/expos/${expoId}/ticket-products`,
    payload
  )
  return data.data
}

/** 백엔드 `TicketUpdateRequest` 와 짝이다. `DRAFT` 상태일 때만 허용된다(서버가 검증). */
export type UpdateTicketProductPayload = {
  price: number
  totalQuantity: number
}

/** `PATCH /api/client/expos/{expoId}/ticket-products/{ticketProductId}`. */
export const updateTicketProduct = async (
  expoId: number,
  ticketProductId: number,
  payload: UpdateTicketProductPayload
): Promise<{
  ticketProductId: number
  price: number
  totalQuantity: number
  availableQuantity: number
}> => {
  const { data } = await api.patch<
    ApiEnvelope<{
      ticketProductId: number
      price: number
      totalQuantity: number
      availableQuantity: number
    }>
  >(`/client/expos/${expoId}/ticket-products/${ticketProductId}`, payload)
  return data.data
}

/**
 * 판매 상태 전환. 서버가 허용하는 전환만 통과한다 — `DRAFT`→`ON_SALE`(게시), `DRAFT`→`CANCELED`,
 * `ON_SALE`→`CANCELED`(판매 취소). 그 외는 `TICKET_PRODUCT_STATUS_TRANSITION_NOT_ALLOWED` 로 막힌다.
 */
export type UpdateTicketProductStatusPayload = {
  status: Extract<TicketProductStatus, 'ON_SALE' | 'CANCELED'>
}

/** `PATCH /api/client/expos/{expoId}/ticket-products/{ticketProductId}/status`. */
export const updateTicketProductStatus = async (
  expoId: number,
  ticketProductId: number,
  payload: UpdateTicketProductStatusPayload
): Promise<ClientTicketProduct> => {
  const { data } = await api.patch<ApiEnvelope<ClientTicketProduct>>(
    `/client/expos/${expoId}/ticket-products/${ticketProductId}/status`,
    payload
  )
  return data.data
}
