import { Suspense } from 'react'

import PaymentFailPage from '@/features/payment/pages/PaymentFailPage'

/** `useSearchParams()` 를 쓰므로 Suspense 경계가 필요하다 — `(public)/auth/callback/page.tsx` 와 같은 이유. */
const PaymentFailRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <PaymentFailPage />
  </Suspense>
)

export default PaymentFailRoute
