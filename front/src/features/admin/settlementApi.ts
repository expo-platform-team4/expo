import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/**
 * `AdminSettlementResponse` 와 짝이다. `v_admin_settlement_status` 뷰 기반.
 *
 * 비어 있을 수 있는 필드는 `| null` 이 아니라 선택 필드(`?`)다 — 백엔드가
 * `non_null` 직렬화라 값이 없으면 응답에서 아예 빠지기 때문이다. 자세한 배경은
 * `features/client/api.ts` 의 `ClientSettlement` 주석 참고.
 */
export type AdminSettlement = {
  settlementId: number
  status: string
  expoId: number
  expoTitle: string
  eventEndAt: string
  hostClientId: number
  companyName: string
  settlementDueAt: string
  remittanceDueAmount: number
  adjustmentAmount: number
  confirmedAt?: string
  confirmedBy?: number
  remittedAmount?: number
  remittedAt?: string
  remittanceStatus?: string
}

/** `SettlementPage<AdminSettlementResponse>` 와 짝이다. Spec.md 2절 페이지네이션 봉투. */
export type AdminSettlementPage = {
  totalCount: number
  page: number
  size: number
  items: AdminSettlement[]
}

export type SearchSettlementsParams = {
  status?: string
  hostClientId?: number
  expoId?: number
  page?: number
  size?: number
}

/** `GET /api/admin/settlements` — 정산 대상 목록·검색. 정산 기한이 임박한 순서로 온다. */
export const searchAdminSettlements = async (
  params: SearchSettlementsParams = {}
): Promise<AdminSettlementPage> => {
  const { data } = await api.get<ApiEnvelope<AdminSettlementPage>>('/admin/settlements', {
    params: { page: 0, size: 20, ...params },
  })
  return data.data
}

/** `SettlementCalculationResult` 와 짝이다. */
export type SettlementCalculationResult = {
  settlementId: number
  status: string
  grossTicketSalesAmount: number
  ticketRefundAmount: number
  netTicketSalesAmount: number
  bookingFeeGrossAmount: number
  bookingFeeRefundAmount: number
  bookingFeeNetAmount: number
  grossBoothSalesAmount: number
  pgFeeReferenceAmount: number
  adjustmentAmount: number
  remittanceDueAmount: number
}

/** `POST /api/admin/settlements/{settlementId}/calculate` — 서버 기준 정산액 재계산. 확정 후에는 불가능(409). */
export const calculateSettlement = async (
  settlementId: number
): Promise<SettlementCalculationResult> => {
  const { data } = await api.post<ApiEnvelope<SettlementCalculationResult>>(
    `/admin/settlements/${settlementId}/calculate`
  )
  return data.data
}

/** `SettlementConfirmResult` 와 짝이다. */
export type SettlementConfirmResult = {
  settlementId: number
  status: string
  remittanceDueAmount: number
  confirmedAt: string
  confirmedBy: number
}

/** `POST /api/admin/settlements/{settlementId}/confirm` — 정산 확정. 되돌리는 API가 없다. */
export const confirmSettlement = async (settlementId: number): Promise<SettlementConfirmResult> => {
  const { data } = await api.post<ApiEnvelope<SettlementConfirmResult>>(
    `/admin/settlements/${settlementId}/confirm`
  )
  return data.data
}

/** `RemittanceRecordRequest` 와 짝이다. 시스템이 송금하지 않는다 — 담당자가 은행에서 이체한 결과를 적는다. */
export type RecordRemittancePayload = {
  status: 'REMITTED' | 'FAILED' | 'PENDING' | 'PROCESSING' | 'CANCELED'
  remittedAmount?: number
  referenceNumber?: string
  memo?: string
}

/** `RemittanceRecordResult` 와 짝이다. */
export type RemittanceRecordResult = {
  settlementId: number
  remittanceId: number
  settlementStatus: string
  remittanceStatus: string
  remittanceDueAmount: number
  remittedAmount: number | null
  amountMatchesDue: boolean
  remittedAt: string | null
}

/** `POST /api/admin/settlements/{settlementId}/transfers` — 외부 송금 결과 기록. 확정된 정산만 가능. */
export const recordSettlementRemittance = async (
  settlementId: number,
  payload: RecordRemittancePayload
): Promise<RemittanceRecordResult> => {
  const { data } = await api.post<ApiEnvelope<RemittanceRecordResult>>(
    `/admin/settlements/${settlementId}/transfers`,
    payload
  )
  return data.data
}

/** `AdminSettlementResponse.status` 값 → 한글 라벨. `ClientSettlementController` 와 값 체계를 공유한다. */
export const SETTLEMENT_STATUS_LABEL: Record<string, string> = {
  WAITING: '집계 대기',
  CALCULATED: '집계 완료',
  UNDER_REVIEW: '검토중',
  CONFIRMED: '확정',
  REMITTANCE_PENDING: '송금 대기',
  REMITTED: '송금 완료',
  ON_HOLD: '보류',
}

/** 상태별 가능한 액션. 화면이 직접 상태를 나열하지 않도록 한 곳에 모은다. */
export const canCalculateSettlement = (status: string): boolean =>
  status === 'WAITING' || status === 'CALCULATED'
export const canConfirmSettlement = (status: string): boolean =>
  status === 'CALCULATED' || status === 'UNDER_REVIEW'
export const canRecordRemittance = (status: string): boolean =>
  status === 'CONFIRMED' || status === 'REMITTANCE_PENDING'
