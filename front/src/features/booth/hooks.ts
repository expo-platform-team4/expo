import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  approveBoothContent,
  cancelBoothAllocation,
  cancelBoothOrder,
  checkBoothContent,
  confirmBoothPayment,
  createBoothContent,
  createBoothOrder,
  createBoothProductsBulk,
  createBoothsBulk,
  getMyBoothAllocation,
  getMyBoothContentByAllocation,
  getMyBoothOrder,
  getPublishedBoothContent,
  hideBoothContent,
  initiateBoothPayment,
  listAdminBooths,
  listAdminBoothAllocations,
  listAdminBoothContents,
  listAdminBoothProducts,
  listMyConfirmedBooths,
  reassignBoothAllocation,
  requestBoothContentCorrection,
  restoreBoothContent,
  submitBoothContentForReview,
  updateBoothContent,
  type BoothContentFormPayload,
  type CreateBoothPayload,
  type CreateBoothProductPayload,
} from './api'
import { boothKeys } from './queryKeys'

/** `/client/booths` 화면 — 내 확정 배정 부스 목록. */
export const useMyConfirmedBooths = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: boothKeys.myList(),
    queryFn: listMyConfirmedBooths,
    enabled: Boolean(accessToken),
  })
}

/** 배정 상세(배정·취소 일시, 취소 사유). 카드를 펼쳤을 때만 부른다(`enabled`). */
export const useBoothAllocation = (allocationId: number | null) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: boothKeys.allocationDetail(allocationId ?? 0),
    queryFn: () => getMyBoothAllocation(allocationId as number),
    enabled: Boolean(accessToken) && allocationId !== null,
  })
}

/**
 * 공개된 콘텐츠 미리보기(방문객용, 로그인 불필요). 404 는 `api.ts` 가 이미 `null` 로 바꿔
 * 주므로 여기서는 평범한 성공 케이스로 다룬다(에러가 아니다 — Spec.md 5절의 "404 는
 * 재시도해도 결과가 같다"는 원칙과 달리, 이 경우 404 자체가 유효한 화면 상태다).
 *
 * 부스 소유자 본인이 자기 콘텐츠를 관리할 때는 이걸 쓰지 않는다 — `useMyBoothContent`.
 */
export const usePublishedBoothContent = (allocationId: number) =>
  useQuery({
    queryKey: boothKeys.publishedContent(allocationId),
    queryFn: () => getPublishedBoothContent(allocationId),
  })

/**
 * 배정 ID 로 **내** 콘텐츠를 상태 무관하게 조회한다(이슈 #111). `BoothContentSection` 이
 * 마운트될 때마다 이걸로 기존 콘텐츠 유무를 먼저 확인한다 — 세션에서 만든 것만 기억하던
 * 이전 방식(새로고침하면 잃어버림)을 대체한다.
 */
export const useMyBoothContent = (allocationId: number) =>
  useQuery({
    queryKey: boothKeys.myContent(allocationId),
    queryFn: () => getMyBoothContentByAllocation(allocationId),
  })

export const useCreateBoothContent = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      boothAllocationId,
      payload,
    }: {
      boothAllocationId: number
      payload: BoothContentFormPayload
    }) => createBoothContent(boothAllocationId, payload),
    onSuccess: (_content, { boothAllocationId }) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.myContent(boothAllocationId) })
    },
  })
}

export const useUpdateBoothContent = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      contentId,
      payload,
    }: {
      contentId: number
      allocationId: number
      payload: BoothContentFormPayload
    }) => updateBoothContent(contentId, payload),
    onSuccess: (_content, { allocationId }) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.myContent(allocationId) })
    },
  })
}

export const useSubmitBoothContentForReview = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ contentId }: { contentId: number; allocationId: number }) =>
      submitBoothContentForReview(contentId),
    onSuccess: (_content, { allocationId }) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.myContent(allocationId) })
    },
  })
}

/** 부스 상품 주문 생성. `/client/participations/{applicationId}` 에서 "주문하고 결제하기" 시 부른다. */
export const useCreateBoothOrder = () => useMutation({ mutationFn: createBoothOrder })

/**
 * 주문 상세. 결제 화면(`ClientBoothOrderPage`)이 15초마다 다시 불러 만료·승인 여부를 반영한다.
 * 주문이 종료 상태(결제 완료·실패·취소·만료)가 되면 더 반영할 변화가 없으니 폴링을 멈춘다.
 */
export const useMyBoothOrder = (orderId: number) =>
  useQuery({
    queryKey: boothKeys.orderDetail(orderId),
    queryFn: () => getMyBoothOrder(orderId),
    refetchInterval: (query) => (query.state.data?.status === 'PENDING_PAYMENT' ? 15_000 : false),
  })

export const useCancelBoothOrder = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cancelBoothOrder,
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.orderDetail(order.id) })
    },
  })
}

/** 결제 시작. `BoothCheckout` 이 마운트되자마자 부른다. */
export const useInitiateBoothPayment = () => useMutation({ mutationFn: initiateBoothPayment })

/** 결제 승인 확정. 토스 결제창에서 돌아온 성공 콜백 페이지가 마운트 시 자동으로 부른다. */
export const useConfirmBoothPayment = () => useMutation({ mutationFn: confirmBoothPayment })

/** 구역 내 부스 공간 목록. 관리자 부스 등록 화면에서 이미 등록된 부스를 보여줄 때 쓴다. */
export const useAdminBooths = (zoneId: number | null) =>
  useQuery({
    queryKey: boothKeys.adminBooths(zoneId ?? 0),
    queryFn: () => listAdminBooths(zoneId as number),
    enabled: zoneId !== null,
  })

/** 부스 공간 일괄 등록. 성공하면 그 구역의 부스 목록 캐시를 무효화한다. */
export const useCreateBoothsBulk = (zoneId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (booths: CreateBoothPayload[]) => createBoothsBulk(zoneId, booths),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: boothKeys.adminBooths(zoneId) })
    },
  })
}

/** 공고별 부스 상품 목록(전체 상태). 관리자 부스 상품 등록 화면에서 이미 등록된 상품을 걸러낼 때 쓴다. */
export const useAdminBoothProducts = (recruitmentNoticeId: number | null) =>
  useQuery({
    queryKey: boothKeys.adminProducts(recruitmentNoticeId ?? 0),
    queryFn: () => listAdminBoothProducts(recruitmentNoticeId as number),
    enabled: recruitmentNoticeId !== null,
  })

/** 부스 상품 일괄 등록. 성공하면 그 공고의 부스 상품 목록 캐시를 무효화한다. */
export const useCreateBoothProductsBulk = (recruitmentNoticeId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (boothProducts: CreateBoothProductPayload[]) =>
      createBoothProductsBulk(boothProducts),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: boothKeys.adminProducts(recruitmentNoticeId),
      })
    },
  })
}

/** 부스 확정 배정 목록(전체 상태). `/admin/booth-allocations` 화면. */
export const useAdminBoothAllocations = () =>
  useQuery({
    queryKey: boothKeys.adminAllocations(),
    queryFn: listAdminBoothAllocations,
  })

/** 확정 배정 취소(운영상 정정 전용). 성공하면 배정 목록 캐시를 무효화한다. */
export const useCancelBoothAllocation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ allocationId, reason }: { allocationId: number; reason: string }) =>
      cancelBoothAllocation(allocationId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: boothKeys.adminAllocations() })
    },
  })
}

/** 확정 배정을 다른 부스 상품으로 재배정. 성공하면 배정 목록 캐시를 무효화한다. */
export const useReassignBoothAllocation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      allocationId,
      boothProductId,
      reason,
    }: {
      allocationId: number
      boothProductId: number
      reason: string
    }) => reassignBoothAllocation(allocationId, boothProductId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: boothKeys.adminAllocations() })
    },
  })
}

/** 부스 콘텐츠 목록(전체 상태). `/admin/booth-contents` 화면. */
export const useAdminBoothContents = () =>
  useQuery({
    queryKey: boothKeys.adminContents(),
    queryFn: listAdminBoothContents,
  })

const useInvalidateAdminBoothContents = () => {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: boothKeys.adminContents() })
}

/** 운영 확인(검수 시작). 보완 요청·숨김 이력이 있어도 다시 검수 대기로 돌아온 콘텐츠에 쓴다. */
export const useCheckBoothContent = () => {
  const invalidate = useInvalidateAdminBoothContents()
  return useMutation({ mutationFn: checkBoothContent, onSuccess: invalidate })
}

/** 검수 승인 — 공개(PUBLISHED)로 전환한다. */
export const useApproveBoothContent = () => {
  const invalidate = useInvalidateAdminBoothContents()
  return useMutation({ mutationFn: approveBoothContent, onSuccess: invalidate })
}

/** 보완 요청 — 사유를 남기고 기업이 다시 고쳐 쓰게 한다. */
export const useRequestBoothContentCorrection = () => {
  const invalidate = useInvalidateAdminBoothContents()
  return useMutation({
    mutationFn: ({ contentId, message }: { contentId: number; message: string }) =>
      requestBoothContentCorrection(contentId, message),
    onSuccess: invalidate,
  })
}

/** 직권 숨김 — 이미 공개된 콘텐츠를 내린다. */
export const useHideBoothContent = () => {
  const invalidate = useInvalidateAdminBoothContents()
  return useMutation({
    mutationFn: ({ contentId, reason }: { contentId: number; reason?: string }) =>
      hideBoothContent(contentId, reason),
    onSuccess: invalidate,
  })
}

/** 숨김 해제 — 다시 공개 상태로 되돌린다. */
export const useRestoreBoothContent = () => {
  const invalidate = useInvalidateAdminBoothContents()
  return useMutation({
    mutationFn: ({ contentId, reason }: { contentId: number; reason?: string }) =>
      restoreBoothContent(contentId, reason),
    onSuccess: invalidate,
  })
}
