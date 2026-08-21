import { api } from '@/lib/api'

/** 입장권 한 장. 백엔드 `TicketViewResponse.Ticket` 과 짝이다. */
export type TicketViewTicket = {
  issuedTicketId: number
  ticketCode: string
  /**
   * QR 원문. 이 문자열을 그대로 QR 이미지로 만든다.
   *
   * DB 에 저장돼 있지 않고 조회할 때마다 다시 계산된다. 같은 티켓이면 언제나 같은 값이라
   * 몇 번을 다시 열어도 같은 QR 이 나온다.
   */
  qrPayload: string
  status: 'ISSUED' | 'CHECKED_IN' | 'CANCELED' | 'INVALIDATED'
  /** 입장 시각. 아직 입장 전이면 null */
  checkedInAt: string | null
  expoTitle: string
  expoStartAt: string
  expoEndAt: string
}

export type TicketView = {
  orderNumber: string
  ticketCount: number
  tickets: TicketViewTicket[]
}

type ApiResponse<T> = {
  success: boolean
  data?: T
  message?: string
}

/**
 * 링크로 열 수 없는 이유. **셋을 구분하는 데 의미가 있다.**
 *
 * 백엔드가 굳이 상태 코드를 셋으로 나눈 이유가 여기서 쓰인다 — 받는 사람이 할 수 있는 일이
 * 다르기 때문이다. 만료는 재발급을 안내할 수 있지만, 폐기는 안내하면 안 된다.
 */
export type TicketViewErrorKind = 'NOT_FOUND' | 'EXPIRED' | 'REVOKED' | 'UNKNOWN'

export class TicketViewError extends Error {
  constructor(
    readonly kind: TicketViewErrorKind,
    message: string
  ) {
    super(message)
    this.name = 'TicketViewError'
  }
}

/**
 * SMS 링크의 토큰으로 그 주문의 입장권을 전부 가져온다.
 *
 * 로그인이 필요 없다. 토큰 자체가 인증 수단이라 헤더에 아무것도 붙이지 않는다.
 */
export const fetchTicketView = async (token: string): Promise<TicketView> => {
  try {
    const { data } = await api.get<ApiResponse<TicketView>>('/public/tickets', {
      params: { token },
    })
    if (!data.data) {
      throw new TicketViewError('UNKNOWN', data.message ?? '티켓을 불러오지 못했습니다.')
    }
    return data.data
  } catch (error) {
    throw toTicketViewError(error)
  }
}

/**
 * HTTP 상태를 화면이 쓸 수 있는 종류로 옮긴다.
 *
 * 상태 코드를 화면까지 끌고 가면 컴포넌트가 HTTP 를 알아야 한다. 여기서 한 번 번역한다.
 */
// ---------------------------------------------------------------------------
// 주최사 체크인 (Function.md 3·5절, `ClientCheckInController` 와 짝이다)
//
// 전부 `/api/client/expos/{expoId}/...` 아래에 있다 — 주최사가 여러 박람회를 열 수 있어
// expoId 가 항상 경로에 붙는다. `ExpoHostVerifier` 가 자기 박람회인지 서버에서 다시
// 검증하므로, 프론트는 "내가 연 박람회 목록"(`features/client`)에서 고른 expoId 를
// 그대로 넘기면 된다.
// ---------------------------------------------------------------------------

/** 백엔드 `CheckInSummaryResponse` 와 짝이다. */
export type CheckInSummary = {
  issuedCount: number
  checkedInCount: number
  notCheckedInCount: number
  canceledCount: number
}

/** `GET /api/client/expos/{expoId}/check-in` — 오늘 체크인 현황. */
export const fetchCheckInSummary = async (expoId: number): Promise<CheckInSummary> => {
  const { data } = await api.get<ApiResponse<CheckInSummary>>(`/client/expos/${expoId}/check-in`)
  if (!data.data) {
    throw new Error(data.message ?? '체크인 현황을 불러오지 못했습니다.')
  }
  return data.data
}

/**
 * 백엔드 `CheckInHistoryRow` 와 짝이다.
 *
 * `issuedTicketId`·`ticketCode` 는 **없을 수 있다** — 위조·미등록 QR 시도(`INVALID_TOKEN`)는
 * 가리킬 티켓이 없다(이슈 #73). 백엔드가 `non_null` 직렬화라 키 자체가 빠지므로
 * `| null` 이 아니라 `?` 다 (Spec.md 1-1절).
 */
export type CheckInHistoryRow = {
  id: number
  issuedTicketId?: number
  ticketCode?: string
  method: 'QR' | 'MANUAL_CODE'
  result: 'SUCCESS' | 'ALREADY_USED' | 'CANCELED_TICKET' | 'WRONG_EXPO' | 'INVALID_TOKEN'
  checkedAt: string
  processedByClientId: number
  detail?: string
}

/** 백엔드 `CheckInHistoryPage` 와 짝이다. Spec.md 2절 페이지네이션 봉투. */
export type CheckInHistoryList = {
  totalCount: number
  page: number
  size: number
  items: CheckInHistoryRow[]
}

/** `GET /api/client/expos/{expoId}/check-ins/history` — 체크인 이력. 최신순, 성공·거절 모두 포함. */
export const fetchCheckInHistory = async (
  expoId: number,
  page = 0,
  size = 100
): Promise<CheckInHistoryList> => {
  const { data } = await api.get<ApiResponse<CheckInHistoryList>>(
    `/client/expos/${expoId}/check-ins/history`,
    { params: { page, size } }
  )
  if (!data.data) {
    throw new Error(data.message ?? '체크인 이력을 불러오지 못했습니다.')
  }
  return data.data
}

/**
 * 백엔드 `CheckInResponse` 와 짝이다.
 *
 * **거절도 HTTP 200 으로 온다** (컨트롤러 주석 참고). `result !== 'SUCCESS'` 인 응답은
 * axios 에러가 아니라 정상 응답이므로, 화면은 `admitted` 로 통과 여부를 가른다.
 */
export type CheckInResult = {
  result: 'SUCCESS' | 'ALREADY_USED' | 'CANCELED_TICKET' | 'WRONG_EXPO' | 'INVALID_TOKEN'
  admitted: boolean
  issuedTicketId: number | null
  ticketCode: string | null
  checkedInAt: string | null
  message: string
}

/** `POST /api/client/expos/{expoId}/check-ins/qr` — 스캔한 QR 원문을 그대로 보낸다. */
export const checkInByQr = async (expoId: number, qrPayload: string): Promise<CheckInResult> => {
  const { data } = await api.post<ApiResponse<CheckInResult>>(
    `/client/expos/${expoId}/check-ins/qr`,
    { qrPayload }
  )
  if (!data.data) {
    throw new Error(data.message ?? '체크인 처리에 실패했습니다.')
  }
  return data.data
}

/** `POST /api/client/expos/{expoId}/check-ins/code` — 카메라가 안 될 때의 대체 경로. */
export const checkInByCode = async (expoId: number, ticketCode: string): Promise<CheckInResult> => {
  const { data } = await api.post<ApiResponse<CheckInResult>>(
    `/client/expos/${expoId}/check-ins/code`,
    { ticketCode }
  )
  if (!data.data) {
    throw new Error(data.message ?? '체크인 처리에 실패했습니다.')
  }
  return data.data
}

const toTicketViewError = (error: unknown): TicketViewError => {
  if (error instanceof TicketViewError) {
    return error
  }

  const status = (error as { response?: { status?: number } })?.response?.status
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message

  switch (status) {
    case 404:
      return new TicketViewError('NOT_FOUND', message ?? '유효하지 않은 링크입니다.')
    case 410:
      return new TicketViewError('EXPIRED', message ?? '링크 유효기간이 지났습니다.')
    case 403:
      return new TicketViewError('REVOKED', message ?? '사용할 수 없는 링크입니다.')
    default:
      return new TicketViewError('UNKNOWN', message ?? '티켓을 불러오지 못했습니다.')
  }
}
