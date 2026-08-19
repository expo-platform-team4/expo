import { EmptyState, PageHeader } from '@/components/ui'

/**
 * `/mypage/orders`. Function.md 2절 — "회원 주문 목록. API 없음 — 이슈로 별도 요청"
 * (GitHub 이슈 #107). 회원 본인 주문 목록 조회 API 가 생기면 연결한다.
 */
const MyOrdersPage = () => (
  <div>
    <PageHeader title="예매 내역" description="주문·결제 내역을 확인합니다." />
    <EmptyState notReady title="예매 내역" description="예매 내역 조회는 아직 준비 중입니다." />
  </div>
)

export default MyOrdersPage
