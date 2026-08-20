import { EmptyState, PageHeader } from '@/components/ui'

/**
 * `/expos` — 박람회 목록. Function.md 2절.
 *
 * 박람회 단위 목록 조회 API 가 없다(이슈 #107). DB 에는 `v_public_expo_cards` 뷰가 이미
 * 있지만 이를 노출하는 컨트롤러·서비스가 없어서, `ticket-products/purchasable` 를 박람회별로
 * 묶는 임시 방편도 쓸 수 없다 — 그 엔드포인트조차 expoId 를 이미 알아야 호출할 수 있고,
 * 박람회 목록을 얻을 방법(공개 목록·검색 API)이 전혀 없기 때문이다. 그래서 목록 화면은
 * "준비 중" 으로 채운다(Function.md 7절 원칙 — 조용히 빈 목록으로 두지 않는다).
 */
const ExpoListPage = () => (
  <div>
    <PageHeader title="박람회 목록" description="전체 박람회를 둘러볼 수 있는 공간입니다." />
    <EmptyState
      notReady
      title="박람회 목록"
      description="박람회 목록 조회 API 가 아직 연결되지 않았습니다. 곧 준비하겠습니다."
    />
  </div>
)

export default ExpoListPage
