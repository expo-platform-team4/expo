import { api } from '@/lib/api'

/** 백엔드 공통 응답 봉투. `com.expo.common.response.ApiResponse`. */
type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 백엔드 `TicketPaymentStatus` 와 값이 같아야 한다. */
export type TicketPaymentStatus = 'READY' | 'IN_PROGRESS' | 'DONE' | 'FAILED' | 'CANCELED'

/** 백엔드 `TicketPaymentResponse` 와 짝이다. */
export type TicketPaymentInitiation = {
  paymentId: number
  /** 토스 결제위젯을 초기화할 때 쓰는 공개 클라이언트 키. */
  clientKey: string
  /**
   * 토스에 보낼 주문 식별자(`pgOrderId`). 우리 `orderNumber` 그대로가 아니라
   * `{orderNumber}-P1` 형태다 — `requestPayment` 호출과 이후 confirm 요청 모두 이 값을 써야 한다.
   */
  orderId: string
  orderName: string
  amount: number
}

/** `POST /api/payments/initiate`. 회원(JWT)·비회원 공용 — 인증 불필요. */
export const initiateTicketPayment = async (
  orderNumber: string
): Promise<TicketPaymentInitiation> => {
  const { data } = await api.post<ApiEnvelope<TicketPaymentInitiation>>('/payments/initiate', {
    orderNumber,
  })
  return data.data
}

/** 백엔드 `ConfirmTicketPaymentResponse` 와 짝이다. */
export type ConfirmTicketPaymentResult = {
  paymentId: number
  orderNumber: string
  paymentKey?: string
  method?: string
  status: TicketPaymentStatus
  approvedAmount?: number
  approvedAt?: string
}

export type ConfirmTicketPaymentPayload = {
  paymentKey: string
  /** 토스 결제창에서 돌아온 `orderId` 쿼리파라미터. `pgOrderId` 형태 그대로 보낸다. */
  orderId: string
  amount: number
}

/**
 * `POST /api/payments/tickets/confirm`.
 *
 * 회원 주문이면 `api.ts` 인터셉터가 붙인 JWT 로 소유자를 검증하고, 비회원 주문이면
 * 검증을 건너뛴다(주문번호 자체가 예측 불가능한 값이라 그걸로 충분하다고 보는 설계).
 */
export const confirmTicketPayment = async (
  payload: ConfirmTicketPaymentPayload
): Promise<ConfirmTicketPaymentResult> => {
  const { data } = await api.post<ApiEnvelope<ConfirmTicketPaymentResult>>(
    '/payments/tickets/confirm',
    payload
  )
  return data.data
}

/** 백엔드 `FailTicketPaymentResponse` 와 짝이다. */
export type FailTicketPaymentResult = {
  paymentId: number
  orderNumber: string
  paymentStatus: TicketPaymentStatus
  orderStatus: 'PENDING' | 'PAID' | 'CANCELED' | 'PAYMENT_FAILED' | 'EXPIRED'
}

export type FailTicketPaymentPayload = {
  /** 토스 결제창에서 돌아온 `orderId` 쿼리파라미터. `pgOrderId` 형태 그대로 보낸다. */
  orderId: string
  failureCode: string
}

/** `POST /api/payments/tickets/fail`. 결제 실패 시 임시확보 재고를 반환한다. */
export const failTicketPayment = async (
  payload: FailTicketPaymentPayload
): Promise<FailTicketPaymentResult> => {
  const { data } = await api.post<ApiEnvelope<FailTicketPaymentResult>>(
    '/payments/tickets/fail',
    payload
  )
  return data.data
}
