import { useMutation, useQueryClient } from '@tanstack/react-query'

import { ticketKeys } from '@/features/ticket/queryKeys'

import * as refundApi from './api'

/**
 * 회원 전체 환불 요청. 성공하면 예매 내역(`GET /api/users/me/orders`) 캐시를 무효화한다 —
 * 이 뮤테이션 이후 목록의 `refundStatus`·`refundable` 가 바뀌므로 다시 불러야 화면이 맞는다.
 */
export const useRequestMemberRefund = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: refundApi.requestMemberRefund,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ticketKeys.myOrders() })
    },
  })
}

/** 비회원 환불 가능 여부 조회. `POST /api/orders/guest/refund-eligibility`. */
export const useCheckGuestRefundEligibility = () =>
  useMutation({ mutationFn: refundApi.checkGuestRefundEligibility })

/** 비회원 전체 환불 요청. `POST /api/orders/guest/refunds`. */
export const useRequestGuestRefund = () => useMutation({ mutationFn: refundApi.requestGuestRefund })
