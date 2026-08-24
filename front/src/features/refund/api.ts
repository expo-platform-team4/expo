import { api } from '@/lib/api'

/** 백엔드 공통 응답 봉투. `com.expo.common.response.ApiResponse`. */
type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 백엔드 `TicketRefundStatus` 와 값이 같아야 한다. */
export type TicketRefundStatus = 'REQUESTED' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

/** 백엔드 `TicketRefundResponse` 와 짝이다. */
export type TicketRefundResult = {
  refundId: number
  ticketOrderId: number
  ticketPaymentId: number
  refundAmount: number
  status: TicketRefundStatus
  requestedAt: string
}

/**
 * `POST /api/members/me/orders/{orderId}/refunds`. 로그인(MEMBER) 필요.
 *
 * 환불 가능 여부는 이 API 를 부르기 전에 이미 안다 — `GET /api/users/me/orders` 응답의
 * `refundable` 필드로 목록에서부터 판단한다(회원은 비회원과 달리 별도 eligibility
 * 조회 API 가 없다). 그래서 별도 확인 호출 없이 바로 요청한다.
 */
export const requestMemberRefund = async ({
  orderId,
  reason,
}: {
  orderId: number
  reason: string
}): Promise<TicketRefundResult> => {
  const { data } = await api.post<ApiEnvelope<TicketRefundResult>>(
    `/members/me/orders/${orderId}/refunds`,
    { reason: reason || null }
  )
  return data.data
}

/** 백엔드 `TicketRefundIneligibilityReason` 과 값이 같아야 한다. */
export type TicketRefundIneligibilityReason =
  'ORDER_NOT_PAID' | 'EVENT_STARTS_WITHIN_THREE_DAYS' | 'TICKET_ALREADY_CHECKED_IN'

/** 백엔드 `TicketRefundEligibilityResponse` 와 짝이다. */
export type TicketRefundEligibility = {
  refundable: boolean
  ineligibilityReason: TicketRefundIneligibilityReason | null
  expectedRefundAmount: number | null
}

export type GuestRefundAuthPayload = {
  orderNumber: string
  phoneNumber: string
  password: string
}

/**
 * `POST /api/orders/guest/refund-eligibility`. 인증 불필요 — 주문번호·연락처·비밀번호
 * 조합 자체가 인증이다(`GuestOrderDetailPage` 의 재조회 폼과 같은 패턴).
 */
export const checkGuestRefundEligibility = async (
  payload: GuestRefundAuthPayload
): Promise<TicketRefundEligibility> => {
  const { data } = await api.post<ApiEnvelope<TicketRefundEligibility>>(
    '/orders/guest/refund-eligibility',
    payload
  )
  return data.data
}

/** `POST /api/orders/guest/refunds`. 인증 불필요 — 요청 body 의 연락처·비밀번호가 곧 인증이다. */
export const requestGuestRefund = async (
  payload: GuestRefundAuthPayload & { reason: string }
): Promise<TicketRefundResult> => {
  const { data } = await api.post<ApiEnvelope<TicketRefundResult>>('/orders/guest/refunds', {
    ...payload,
    reason: payload.reason || null,
  })
  return data.data
}
