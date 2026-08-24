'use client'

import Link from 'next/link'

import { Badge, Card, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import { useMyParticipations } from '../hooks'

/**
 * `/client/participations`. Function.md 3절 — "내가 신청한 부스 참여 목록". CLIENT 전용.
 *
 * 목록은 `GET /api/client/me/participations` 로 가져온다(참여 신청서를 만드는
 * `ParticipationApplicationController` 에는 목록 API 가 없다 — `participation/api.ts` 주석
 * 참고). 그 응답(`ClientDashboardBoothResponse`)은 신청 상태뿐 아니라 부스 주문·결제·배정
 * 진행 상황까지 한 행에 담고 있어, 참여 신청서 자체의 상세 조회(`getMyParticipationApplication`)
 * 없이도 이 화면의 목적("내 신청이 지금 어디까지 왔나")을 채운다.
 */
const ClientParticipationListPage = () => {
  const { data: participations, isPending, isError, error, refetch } = useMyParticipations()

  return (
    <div>
      <PageHeader title="참여 신청 내역" description="내가 참여기업으로 신청한 부스 목록입니다." />

      {isPending ? (
        <LoadingBlock label="참여 신청 내역을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : participations.length === 0 ? (
        <EmptyState
          title="참여 신청 내역이 없습니다"
          description="공고 모집 목록에서 참여하고 싶은 공고를 찾아 신청해 보세요."
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {participations.map((item) => (
            <li key={item.applicationId}>
              <Card>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <Link
                    href={`/recruitment-notices/${item.recruitmentNoticeId}`}
                    className="text-title-lg text-primary font-semibold hover:underline"
                  >
                    공고 #{item.recruitmentNoticeId}
                  </Link>
                  <Badge variant={applicationStatusVariant(item.applicationStatus)}>
                    {applicationStatusLabel(item.applicationStatus)}
                  </Badge>
                </div>

                <dl className="text-body-md text-on-surface mt-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
                  <div>
                    <dt className="text-label-sm text-on-surface-variant">부스 주문 상태</dt>
                    <dd>{item.boothOrderStatus ?? '-'}</dd>
                  </div>
                  <div>
                    <dt className="text-label-sm text-on-surface-variant">결제 상태</dt>
                    <dd>
                      {item.paymentStatus ?? '-'}
                      {item.paidAt ? ` · ${formatDateTime(item.paidAt)}` : ''}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-label-sm text-on-surface-variant">부스 배정</dt>
                    <dd>
                      {item.allocationStatus ?? '-'}
                      {item.boothNumber ? ` · ${item.boothNumber}` : ''}
                    </dd>
                  </div>
                </dl>

                <div className="mt-3 flex flex-wrap gap-3">
                  <Link
                    href={`/client/participations/${item.applicationId}`}
                    className="text-secondary text-label-md underline"
                  >
                    신청 상세·수정
                  </Link>
                  {item.applicationStatus === 'PAYMENT_PENDING' && item.boothOrderId != null && (
                    <Link
                      href={`/client/booth-orders/${item.boothOrderId}`}
                      className="text-secondary text-label-md underline"
                    >
                      결제 계속하기
                    </Link>
                  )}
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

const applicationStatusLabel = (status: string): string => {
  switch (status) {
    case 'DRAFT':
      return '임시저장'
    case 'PAYMENT_PENDING':
      return '결제 대기'
    case 'SUBMITTED':
      return '제출 완료'
    case 'PAYMENT_FAILED':
      return '결제 실패'
    case 'CANCELED':
      return '취소됨'
    default:
      return status
  }
}

const applicationStatusVariant = (status: string): 'success' | 'neutral' | 'error' | 'info' => {
  switch (status) {
    case 'SUBMITTED':
      return 'success'
    case 'PAYMENT_PENDING':
      return 'info'
    case 'PAYMENT_FAILED':
    case 'CANCELED':
      return 'error'
    default:
      return 'neutral'
  }
}

export default ClientParticipationListPage
