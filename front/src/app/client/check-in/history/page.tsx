import { Suspense } from 'react'

import ClientCheckInHistoryPage from '@/features/checkin/pages/ClientCheckInHistoryPage'

/**
 * `useSearchParams()` (`expoId` 판독)를 쓰는 컴포넌트는 Suspense 경계 안에 있어야 한다 —
 * `app/(public)/orders/page.tsx` 의 `OrdersRoute` 와 같은 이유.
 */
const ClientCheckInHistoryRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <ClientCheckInHistoryPage />
  </Suspense>
)

export default ClientCheckInHistoryRoute
