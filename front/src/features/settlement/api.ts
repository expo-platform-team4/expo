import { api } from '@/lib/api'
import type { ClientSettlement } from '@/features/client/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 백엔드 `SettlementItemResponse.itemType` 값. */
export type SettlementItemType =
  | 'TICKET_SALE'
  | 'TICKET_REFUND'
  | 'BOOKING_FEE'
  | 'BOOKING_FEE_REFUND'
  | 'BOOTH_SALE'
  | 'ADJUSTMENT'

/**
 * 백엔드 `SettlementItemResponse` 와 짝이다. 정산 금액의 구성 항목 하나.
 *
 * `amount` 는 더하면 정산금이 되도록 부호가 맞춰져 있다 — 환불은 음수다.
 * `includedInRemittance` 가 정산금(받을 금액)에 포함되는 항목인지를 가른다.
 */
export type SettlementItem = {
  itemType: SettlementItemType
  amount: number
  includedInRemittance: boolean
  occurredAt: string
}

/** 화면에 그대로 보여줄 한국어 라벨. */
export const SETTLEMENT_ITEM_TYPE_LABEL: Record<SettlementItemType, string> = {
  TICKET_SALE: '티켓 판매',
  TICKET_REFUND: '티켓 환불',
  BOOKING_FEE: '예매 수수료',
  BOOKING_FEE_REFUND: '예매 수수료 환불',
  BOOTH_SALE: '부스 매출',
  ADJUSTMENT: '조정',
}

/** `ClientSettlementResponse.remittanceStatus` 값. 백엔드 `RemittanceStatus` 와 짝이다. */
export const REMITTANCE_STATUS_LABEL: Record<string, string> = {
  PENDING: '송금 예정',
  PROCESSING: '송금 처리중',
  REMITTED: '송금 완료',
  FAILED: '송금 실패',
  CANCELED: '송금 취소',
}

/**
 * 백엔드 `ClientSettlementDetail` 과 짝이다 (`GET /api/client/settlements/{id}`).
 *
 * 목록의 `ClientSettlementResponse`(요약)에 금액 구성 항목(`items`)이 붙은 형태다 — 이게
 * WBS 가 "티켓/부스 구분 리포트" 라고 부르는 것이다.
 */
export type ClientSettlementDetail = {
  settlement: ClientSettlement
  items: SettlementItem[]
}

/**
 * `GET /api/client/settlements/{settlementId}` — 정산 상세·티켓/부스 구분 리포트 조회 (D-API-012).
 *
 * 내 정산이 아니면 404 다. Spec.md 5절 — 정산 API 는 존재 여부를 숨기려는 의도로 403 이 아니라
 * 404 를 준다. 프론트에서 이 404 를 "권한 없음" 으로 바꿔 보여주지 않는다.
 */
export const getClientSettlementDetail = async (
  settlementId: number
): Promise<ClientSettlementDetail> => {
  const { data } = await api.get<ApiEnvelope<ClientSettlementDetail>>(
    `/client/settlements/${settlementId}`
  )
  return data.data
}
