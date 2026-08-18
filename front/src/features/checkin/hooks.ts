import { useQuery } from '@tanstack/react-query'

import { fetchTicketView, TicketViewError } from './api'
import { checkinKeys } from './queryKeys'

/**
 * SMS 링크의 티켓 조회.
 *
 * 재시도하지 않는다. 실패 넷(없음·만료·폐기·그 외)이 전부 다시 부른다고 달라지지 않는
 * 종류라, 재시도는 화면에 로딩만 길게 남긴다.
 */
export const useTicketView = (token: string | null) =>
  useQuery({
    queryKey: checkinKeys.ticketView(token ?? ''),
    queryFn: () => fetchTicketView(token as string),
    enabled: Boolean(token),
    retry: false,
  })

export { TicketViewError }
