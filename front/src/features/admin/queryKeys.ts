/**
 * admin 도메인 React Query 캐시 키. Spec.md 6절 — `{module}Keys.{자원}(...params)` 팩토리.
 *
 * 관리자 화면 7개가 도메인 넷(대시보드·모집공고·카테고리·정산·알림)에 걸쳐 있어 한 팩토리
 * 안에 자원별로 묶는다 — `checkinKeys` 패턴을 그대로 넓힌 모양이다.
 */
export const adminKeys = {
  all: ['admin'] as const,

  dashboardSummary: () => [...adminKeys.all, 'dashboard', 'summary'] as const,
  dashboardPendingTasks: () => [...adminKeys.all, 'dashboard', 'pending-tasks'] as const,

  categories: () => [...adminKeys.all, 'categories'] as const,

  noticeRequests: () => [...adminKeys.all, 'notice-requests'] as const,

  notices: () => [...adminKeys.all, 'notices'] as const,

  settlements: (params?: Record<string, unknown>) =>
    [...adminKeys.all, 'settlements', params ?? {}] as const,

  notifications: (params?: Record<string, unknown>) =>
    [...adminKeys.all, 'notifications', params ?? {}] as const,
}
