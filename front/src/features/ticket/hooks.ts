import { useMutation, useQuery } from '@tanstack/react-query'

import * as ticketApi from './api'
import { ticketKeys } from './queryKeys'

/**
 * 박람회의 구매 가능 티켓 상품 목록.
 *
 * `expoId` 가 `null` 이면(쿼리스트링에 `expoId` 가 없는 경우) 요청을 보내지 않는다.
 * 존재하지 않는 expoId 는 400 으로 실패하는데, 다시 불러도 같은 결과라 재시도하지
 * 않는다(Spec.md 5절 — checkin 모듈 `useTicketView`, expo 모듈 `usePurchasableTicketProducts`
 * 와 같은 패턴).
 */
export const usePurchasableTicketProducts = (expoId: number | null) =>
  useQuery({
    queryKey: ticketKeys.purchasableTicketProducts(expoId ?? 0),
    queryFn: () => ticketApi.fetchPurchasableTicketProducts(expoId as number),
    enabled: expoId !== null,
    retry: false,
  })

/** 회원 티켓 주문 생성. `POST /api/orders/member`. */
export const useCreateMemberOrder = () => useMutation({ mutationFn: ticketApi.createMemberOrder })

/** 비회원 티켓 주문 생성. `POST /api/orders/guest`. */
export const useCreateGuestOrder = () => useMutation({ mutationFn: ticketApi.createGuestOrder })

/**
 * 비회원 주문 조회. `POST /api/orders/search/guest`.
 *
 * 검색 화면(`GuestOrderSearchPage`)과 상세 화면의 재조회 폼(`GuestOrderDetailPage`)이
 * 함께 쓴다 — 뒤로 GET-by-id 가 없어서 상세도 같은 조회를 다시 부른다.
 */
export const useGuestOrderSearch = () => useMutation({ mutationFn: ticketApi.searchGuestOrder })
