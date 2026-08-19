import { create } from 'zustand'

import type { GuestOrderSearchResult } from './api'

/**
 * 비회원 주문 조회 → 상세 화면 전달용 임시 상태. **`persist` 를 쓰지 않는다** — 주문 금액·
 * 상태 같은 조회 결과를 localStorage 에 남겨 둘 이유가 없고, 새로고침하면 사라지는 편이
 * 맞다(상세 화면은 스토어가 비어 있으면 같은 조회 API 로 다시 인증받는다 —
 * `pages/GuestOrderDetailPage.tsx` 참고).
 *
 * `lib/auth.ts` 의 `useAuthStore` 와 같은 이유로 Zustand 를 쓰되, 이건 인증 상태가 아니라
 * 화면 간 데이터 전달용이라 `lib/*` 가 아니라 이 모듈 안에 둔다(작업 지시 — `lib/*` 비소유).
 */
type GuestOrderLookupState = {
  result: GuestOrderSearchResult | null
  setResult: (result: GuestOrderSearchResult) => void
  clear: () => void
}

export const useGuestOrderLookupStore = create<GuestOrderLookupState>()((set) => ({
  result: null,
  setResult: (result) => set({ result }),
  clear: () => set({ result: null }),
}))
