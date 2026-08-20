import { EmptyState, PageHeader } from '@/components/ui'

/**
 * `/mypage/tickets`. Function.md 2절 — "티켓별 QR. API 없음 — 이슈로 별도 요청"
 * (GitHub 이슈 #107). 토큰 기반 조회(`/tickets?token=`)와 별개로, 로그인 사용자가 자기
 * 티켓을 나열하는 API 가 생기면 연결한다.
 */
const MyTicketsPage = () => (
  <div>
    <PageHeader title="나의 티켓" description="발권된 티켓과 QR 을 확인합니다." />
    <EmptyState notReady title="나의 티켓" description="나의 티켓 목록은 아직 준비 중입니다." />
  </div>
)

export default MyTicketsPage
