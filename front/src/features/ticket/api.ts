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
