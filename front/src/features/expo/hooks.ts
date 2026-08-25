import { useQuery } from '@tanstack/react-query'

import {
  type ExpoCardQuery,
  fetchExpoCards,
  fetchExpoDetail,
  fetchPurchasableTicketProducts,
  listPublicCategories,
} from './api'
import { expoKeys } from './queryKeys'

/** 공개 박람회 목록. 비회원도 부를 수 있다. */
export const useExpoCards = (query: ExpoCardQuery = {}) =>
  useQuery({
    queryKey: expoKeys.cards(query),
    queryFn: () => fetchExpoCards(query),
  })

/**
 * 공개 박람회 상세.
 *
 * `expoId` 가 `null` 이면(라우트 파라미터가 숫자가 아닌 경우) 요청하지 않는다.
 * 없는 박람회는 400 인데 다시 불러도 같은 결과라 재시도하지 않는다.
 */
export const useExpoDetail = (expoId: number | null) =>
  useQuery({
    queryKey: expoKeys.detail(expoId ?? 0),
    queryFn: () => fetchExpoDetail(expoId as number),
    enabled: expoId !== null,
    retry: false,
  })

/**
 * 박람회 상세의 구매 가능 티켓 상품 목록.
 *
 * `expoId` 가 `null` 이면(라우트 파라미터가 숫자가 아닌 경우) 요청을 보내지 않는다.
 * 존재하지 않는 expoId 는 400 으로 실패하는데, 다시 불러도 같은 결과라 재시도하지
 * 않는다(Spec.md 5절 — checkin 모듈의 `useTicketView` 와 같은 패턴).
 */
export const usePurchasableTicketProducts = (expoId: number | null) =>
  useQuery({
    queryKey: expoKeys.purchasableTicketProducts(expoId ?? 0),
    queryFn: () => fetchPurchasableTicketProducts(expoId as number),
    enabled: expoId !== null,
    retry: false,
  })

/** 공개 카테고리 목록 (활성만). 로그인 여부와 무관하게 조회 가능 — 목록 필터·개최 신청 폼이 쓴다. */
export const useExpoCategories = () =>
  useQuery({
    queryKey: expoKeys.categories(),
    queryFn: listPublicCategories,
  })
