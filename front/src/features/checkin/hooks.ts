import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  checkInByCode,
  checkInByQr,
  fetchCheckInHistory,
  fetchCheckInSummary,
  fetchTicketView,
  TicketViewError,
} from './api'
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

/**
 * 오늘 체크인 현황. Spec.md 8절 — **폴링으로 충분하다.** `refetchInterval` 5초.
 *
 * 스캔 화면이 아니라 이 화면에서만 폴링한다. 스캔은 뮤테이션 성공 시 이 쿼리를
 * 무효화하는 것으로 충분해서, 스캔 화면 자체는 폴링을 걸지 않는다(`useCheckInByQr`·
 * `useCheckInByCode` 참고).
 */
export const useCheckInSummary = (expoId: number | null) =>
  useQuery({
    queryKey: checkinKeys.summary(expoId ?? -1),
    queryFn: () => fetchCheckInSummary(expoId as number),
    enabled: expoId !== null,
    refetchInterval: 5000,
    retry: false,
  })

/**
 * 체크인 이력. 명시적 페이지네이션 UI 없이 최근 것부터 넉넉히(`size=100`, 서버 최대치)
 * 가져와 화면에서 성공/실패로 필터링한다 — `features/client` 의 정산 목록과 같은 이유
 * (박람회당 이력이 아직 그렇게 많지 않다). `totalCount` 가 100 을 넘으면 화면이 그 사실을
 * 알린다(Function.md 7절 — 근사치임을 명시한다).
 */
export const useCheckInHistory = (expoId: number | null, page = 0, size = 100) =>
  useQuery({
    queryKey: checkinKeys.history(expoId ?? -1, page, size),
    queryFn: () => fetchCheckInHistory(expoId as number, page, size),
    enabled: expoId !== null,
  })

/**
 * QR 스캔 체크인. 성공하면 현황·이력 쿼리를 무효화한다 — Spec.md 6절 "뮤테이션 후 무효화".
 *
 * **거절도 `onSuccess` 로 온다.** `checkInByQr` 가 던지는 에러는 "요청 자체가 잘못된"
 * 경우(주최자 아님·박람회 없음)뿐이고, "이미 입장한 표" 같은 거절은 200 응답의 `result` 로
 * 온다(`api.ts` 의 `CheckInResult` 참고) — 그래도 방문 이력은 늘었으니 무효화한다.
 */
export const useCheckInByQr = (expoId: number | null) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (qrPayload: string) => checkInByQr(expoId as number, qrPayload),
    onSuccess: () => {
      if (expoId === null) return
      queryClient.invalidateQueries({ queryKey: checkinKeys.summary(expoId) })
      queryClient.invalidateQueries({ queryKey: [...checkinKeys.all, 'history', expoId] })
    },
  })
}

/** 티켓 코드 수동 체크인. `useCheckInByQr` 와 같은 무효화 규칙. */
export const useCheckInByCode = (expoId: number | null) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ticketCode: string) => checkInByCode(expoId as number, ticketCode),
    onSuccess: () => {
      if (expoId === null) return
      queryClient.invalidateQueries({ queryKey: checkinKeys.summary(expoId) })
      queryClient.invalidateQueries({ queryKey: [...checkinKeys.all, 'history', expoId] })
    },
  })
}
