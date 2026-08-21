import { Badge } from '@/components/ui'

import type { TicketOrderStatus } from '../api'

/** 화면 1·2·4 가 공유하는 주문 상태 배지. `TicketOrderStatus` 값 다섯을 전부 다룬다. */
export const OrderStatusBadge = ({ status }: { status: TicketOrderStatus }) => {
  switch (status) {
    case 'PENDING':
      return <Badge variant="info">결제 대기</Badge>
    case 'PAID':
      return <Badge variant="success">결제 완료</Badge>
    case 'CANCELED':
      return <Badge variant="neutral">취소됨</Badge>
    case 'PAYMENT_FAILED':
      return <Badge variant="error">결제 실패</Badge>
    case 'EXPIRED':
      return <Badge variant="error">기한 만료</Badge>
  }
}
