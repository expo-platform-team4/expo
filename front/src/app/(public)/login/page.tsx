import { Suspense } from 'react'

import LoginPage from '@/features/auth/pages/LoginPage'

/**
 * `useSearchParams()` (redirect·signup 쿼리 판독)를 쓰는 컴포넌트는 Suspense 경계 안에
 * 있어야 한다 — `app/(public)/tickets/page.tsx` 의 `TicketsRoute` 와 같은 이유.
 */
const LoginRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <LoginPage />
  </Suspense>
)

export default LoginRoute
