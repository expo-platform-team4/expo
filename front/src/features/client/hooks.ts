import { useQuery } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import { getClientDashboardProfile } from './api'
import { clientKeys } from './queryKeys'

export const useClientDashboardProfile = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.dashboardProfile,
    queryFn: getClientDashboardProfile,
    enabled: Boolean(accessToken),
  })
}
