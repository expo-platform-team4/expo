import { Suspense } from 'react'

import TicketViewPage from '@/features/checkin/pages/TicketViewPage'

/**
 * SMS 링크가 도착하는 경로. `TicketIssuedMessageComposer` 가 이 주소를 만든다.
 *
 * ```
 * frontBaseUrl + "/tickets?token=" + accessTokenValue
 * ```
 *
 * 다른 라우트와 달리 `export { default } from ...` 한 줄로 끝내지 않았다.
 * `useSearchParams()` 를 쓰는 컴포넌트는 **Suspense 경계 안에 있어야 한다** — 없으면
 * 프리렌더 단계에서 빌드가 깨진다. 쿼리 문자열은 서버가 미리 알 수 없는 값이라
 * Next 가 그 경계까지만 정적으로 만들고 나머지를 클라이언트에 맡긴다.
 */
const TicketsRoute = () => (
  <Suspense fallback={<div className="p-6 text-center">불러오는 중…</div>}>
    <TicketViewPage />
  </Suspense>
)

export default TicketsRoute
