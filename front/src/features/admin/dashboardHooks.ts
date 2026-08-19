import { useQuery } from '@tanstack/react-query'

import { getAdminDashboardSummary, getAdminPendingTasks } from './dashboardApi'
import { adminKeys } from './queryKeys'

/** `/admin` 대시보드 — 처리 대기 건수 집계. */
export const useAdminDashboardSummary = () =>
  useQuery({
    queryKey: adminKeys.dashboardSummary(),
    queryFn: getAdminDashboardSummary,
  })

/** `/admin` 대시보드 — 심사 대기 목록 통합 조회. */
export const useAdminPendingTasks = () =>
  useQuery({
    queryKey: adminKeys.dashboardPendingTasks(),
    queryFn: getAdminPendingTasks,
  })
