import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 구매 가능한 티켓 상품 한 종류. 백엔드 `PurchasableTicketProductResponse` 와 짝이다. */
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

/**
 * 박람회 상세의 구매 가능 티켓 상품 목록. `GET /api/expos/{expoId}/ticket-products/purchasable`.
 *
 * **박람회 자체를 나열·조회하는 공개 API 는 없다** (GitHub 이슈 #107) — 이 엔드포인트는
 * expoId 를 이미 아는 상태에서만 호출할 수 있고, 박람회의 제목·소개·일정 같은 정보는 주지
 * 않는다(티켓 상품 정보만 준다). 존재하지 않는 expoId 를 넘기면 400 으로
 * "박람회를 찾을 수 없습니다" 를 준다(`ErrorCode.EXPO_NOT_FOUND`).
 */
export const fetchPurchasableTicketProducts = async (
  expoId: number
): Promise<PurchasableTicketProduct[]> => {
  const { data } = await api.get<ApiEnvelope<PurchasableTicketProduct[]>>(
    `/expos/${expoId}/ticket-products/purchasable`
  )
  return data.data
}
