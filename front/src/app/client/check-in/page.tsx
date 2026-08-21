import { Suspense } from 'react'

import ClientCheckInPage from '@/features/checkin/pages/ClientCheckInPage'

/**
 * `useSearchParams()` (`expoId` 판독)를 쓰는 컴포넌트는 Suspense 경계 안에 있어야 한다 —
 * `app/(public)/orders/page.tsx` 의 `OrdersRoute` 와 같은 이유.
 */
const ClientCheckInRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <ClientCheckInPage />
  </Suspense>
)

export default ClientCheckInRoute
