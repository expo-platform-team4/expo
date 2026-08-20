/**
 * Public·Sidebar 셸이 공유하는 글로벌 내비. Style.md 5-1·5-2 절.
 *
 * "공고 신청" 은 CLIENT 전용 액션(`/client/recruitment-notice-requests/new`)이지만 헤더에는
 * 로그인 여부와 무관하게 항상 노출한다 — 비로그인·MEMBER 사용자가 눌렀을 때는
 * `RequireAuth` 가 알아서 `/login` 또는 자기 역할 홈으로 돌려보낸다. 헤더에서 역할별로
 * 항목을 감추는 조건 분기를 추가하지 않는다.
 */
export const GLOBAL_NAV_ITEMS = [
  { label: '박람회', href: '/expos' },
  { label: '공고 신청', href: '/client/recruitment-notice-requests/new' },
  { label: '공고 모집', href: '/recruitment-notices' },
  { label: '비회원 주문 조회', href: '/orders/guest/search' },
] as const
