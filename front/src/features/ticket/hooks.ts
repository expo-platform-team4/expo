import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

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

/** 예매 내역. `GET /api/users/me/orders`. 로그인 필요 (레이아웃의 `RequireAuth` 가 보장한다). */
export const useMyOrders = () =>
  useQuery({
    queryKey: ticketKeys.myOrders(),
    queryFn: ticketApi.fetchMyOrders,
  })

/** 나의 티켓. `GET /api/users/me/tickets`. */
export const useMyTickets = () =>
  useQuery({
    queryKey: ticketKeys.myTickets(),
    queryFn: ticketApi.fetchMyTickets,
  })

// ---------------------------------------------------------------------------
// 주최사(CLIENT) — 티켓 상품 관리
// ---------------------------------------------------------------------------

/** 주최사 본인 박람회의 티켓 상품 목록. `GET /api/client/expos/{expoId}/ticket-search`. */
export const useClientTicketProducts = (expoId: number) =>
  useQuery({
    queryKey: ticketKeys.clientTicketProducts(expoId),
    queryFn: () => ticketApi.fetchClientTicketProducts(expoId),
  })

/** 티켓 상품 생성. 성공하면 목록을 다시 불러온다. */
export const useCreateTicketProduct = (expoId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: ticketApi.CreateTicketProductPayload) =>
      ticketApi.createTicketProduct(expoId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ticketKeys.clientTicketProducts(expoId) })
    },
  })
}

/** 티켓 상품 가격·재고 수정(`DRAFT` 상태만 허용). 성공하면 목록을 다시 불러온다. */
export const useUpdateTicketProduct = (expoId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      ticketProductId,
      payload,
    }: {
      ticketProductId: number
      payload: ticketApi.UpdateTicketProductPayload
    }) => ticketApi.updateTicketProduct(expoId, ticketProductId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ticketKeys.clientTicketProducts(expoId) })
    },
  })
}

/** 티켓 상품 판매 상태 전환(게시·판매 취소). 성공하면 목록을 다시 불러온다. */
export const useUpdateTicketProductStatus = (expoId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      ticketProductId,
      status,
    }: {
      ticketProductId: number
      status: ticketApi.UpdateTicketProductStatusPayload['status']
    }) => ticketApi.updateTicketProductStatus(expoId, ticketProductId, { status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ticketKeys.clientTicketProducts(expoId) })
    },
  })
}
