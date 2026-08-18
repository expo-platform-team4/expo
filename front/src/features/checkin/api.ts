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
