import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `NotificationHistoryResponse` 와 짝이다. 수신번호는 서버가 마스킹해서 준다. */
export type NotificationHistory = {
  notificationId: number
  recipientUserId: number | null
  recipientPhoneNumber: string | null
  channel: string
  templateCode: string
  referenceType: string
  referenceId: number
  status: string
  retryCount: number
  lastError: string | null
  sentAt: string | null
  createdAt: string
  attemptCount: number
  lastAttemptNo: number | null
  lastAttemptStatus: string | null
  lastAttemptErrorCode: string | null
  lastAttemptAt: string | null
  retryable: boolean
}

/** `NotificationHistoryPage` 와 짝이다. */
export type NotificationHistoryPage = {
  totalCount: number
  page: number
  size: number
  items: NotificationHistory[]
}

export type SearchNotificationsParams = {
  status?: string
  templateCode?: string
  page?: number
  size?: number
}

/** `GET /api/admin/notifications` — 알림 이력 조회. 상태·템플릿·참조 대상·기간으로 필터링 가능. */
export const searchAdminNotifications = async (
  params: SearchNotificationsParams = {}
): Promise<NotificationHistoryPage> => {
  const { data } = await api.get<ApiEnvelope<NotificationHistoryPage>>('/admin/notifications', {
    params: { page: 0, size: 20, ...params },
  })
  return data.data
}

/** `NotificationRetryResult` 와 짝이다. HTTP 는 항상 200 — 발송 실패도 `success:false` 로 알린다. */
export type NotificationRetryResult = {
  notificationId: number
  attemptNo: number
  status: string
  success: boolean
  errorCode: string | null
}

/** `POST /api/admin/notifications/{notificationId}/retry` — 실패 알림 재발송. `retryable` 인 것만 가능. */
export const retryNotification = async (
  notificationId: number
): Promise<NotificationRetryResult> => {
  const { data } = await api.post<ApiEnvelope<NotificationRetryResult>>(
    `/admin/notifications/${notificationId}/retry`
  )
  return data.data
}

/** 알림 상태 값 → 한글 라벨. */
export const NOTIFICATION_STATUS_LABEL: Record<string, string> = {
  PENDING: '대기',
  SENT: '발송 완료',
  FAILED: '발송 실패',
  RETRYING: '재시도 중',
  CANCELED: '취소됨',
}

/** 알림 템플릿 코드 → 한글 라벨. */
export const NOTIFICATION_TEMPLATE_LABEL: Record<string, string> = {
  TICKET_ISSUED: '발권 안내',
  REFUND_COMPLETED: '환불 완료',
  EXPO_CANCELED: '박람회 취소',
}
