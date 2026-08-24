import { Suspense } from 'react'

import PaymentSuccessPage from '@/features/payment/pages/PaymentSuccessPage'

/** `useSearchParams()` 를 쓰므로 Suspense 경계가 필요하다 — `(public)/auth/callback/page.tsx` 와 같은 이유. */
const PaymentSuccessRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <PaymentSuccessPage />
  </Suspense>
)

export default PaymentSuccessRoute
