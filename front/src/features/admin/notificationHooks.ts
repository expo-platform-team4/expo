import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  retryNotification,
  searchAdminNotifications,
  type SearchNotificationsParams,
} from './notificationApi'
import { adminKeys } from './queryKeys'

/** `/admin/notifications` — 알림 이력 조회. */
export const useAdminNotifications = (params: SearchNotificationsParams = {}) =>
  useQuery({
    queryKey: adminKeys.notifications(params),
    queryFn: () => searchAdminNotifications(params),
  })

/** 실패 알림 재발송. 성공하면 이력 목록을 무효화한다. */
export const useRetryNotification = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: retryNotification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.all })
    },
  })
}
