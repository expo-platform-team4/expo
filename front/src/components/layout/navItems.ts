/**
 * Public·Sidebar 셸이 공유하는 글로벌 내비. Style.md 5-1·5-2 절.
 *
 * 모집공고 생성 요청 작성은 이제 CLIENT가 아니라 ADMIN이 승인된 박람회를 골라 대신
 * 작성한다(`/admin/recruitment-notice-requests/new`) — 그래서 이 목록엔 "공고 신청"이
 * 없다. CLIENT는 자기 요청 조회만 한다(`ClientSidebarShell` 의 "모집공고 요청 관리").
 */
export const GLOBAL_NAV_ITEMS = [
  { label: '박람회', href: '/expos' },
  { label: '공고 모집', href: '/recruitment-notices' },
  { label: '비회원 주문 조회', href: '/orders/guest/search' },
] as const
