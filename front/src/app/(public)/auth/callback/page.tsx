import { Suspense } from 'react'

import AuthCallbackPage from '@/features/auth/pages/AuthCallbackPage'

/** `useSearchParams()` 를 쓰므로 Suspense 경계가 필요하다 — `(public)/login/page.tsx` 와 같은 이유. */
const AuthCallbackRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <AuthCallbackPage />
  </Suspense>
)

export default AuthCallbackRoute
