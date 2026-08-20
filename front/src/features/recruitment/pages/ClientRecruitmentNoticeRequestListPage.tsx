'use client'

import Link from 'next/link'

import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import { useMyRecruitmentNoticeRequests } from '../hooks'
import { NOTICE_REQUEST_STATUS, VENUE_CONFLICT_STATUS, VENUE_DECISION } from '../statusLabels'

/** `/client/recruitment-notice-requests`. Function.md 3절 — "내 요청 목록·상세". CLIENT 전용. */
const ClientRecruitmentNoticeRequestListPage = () => {
  const { data: requests, isPending, isError, error, refetch } = useMyRecruitmentNoticeRequests()

  return (
    <div>
      <PageHeader
        title="모집공고 요청 관리"
        description="내가 작성한 모집공고 생성 요청 목록입니다."
        action={
          <Link href="/client/recruitment-notice-requests/new">
            <Button>새 요청 작성</Button>
          </Link>
        }
      />

      {isPending ? (
        <LoadingBlock label="요청 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : requests.length === 0 ? (
        <EmptyState
          title="작성한 요청이 없습니다"
          description="새 요청 작성 버튼으로 모집공고 생성을 요청해 보세요."
          action={
            <Link href="/client/recruitment-notice-requests/new">
              <Button>새 요청 작성</Button>
            </Link>
          }
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {requests.map((request) => (
            <li key={request.id}>
              <Link href={`/client/recruitment-notice-requests/${request.id}`}>
                <Card className="transition-shadow hover:shadow-[0_20px_25px_-5px_rgba(26,43,75,0.1),0_10px_10px_-5px_rgba(26,43,75,0.04)]">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="text-title-lg text-primary font-semibold">{request.title}</h3>
                    <div className="flex flex-wrap gap-2">
                      <Badge variant={NOTICE_REQUEST_STATUS[request.status].variant}>
                        {NOTICE_REQUEST_STATUS[request.status].label}
                      </Badge>
                      <Badge variant={VENUE_CONFLICT_STATUS[request.venueConflictStatus].variant}>
                        {VENUE_CONFLICT_STATUS[request.venueConflictStatus].label}
                      </Badge>
                      <Badge variant={VENUE_DECISION[request.venueDecision].variant}>
                        {VENUE_DECISION[request.venueDecision].label}
                      </Badge>
                    </div>
                  </div>
                  <p className="text-label-sm text-on-surface-variant mt-3">
                    행사 기간 {formatDateTime(request.eventStartAt)} ~{' '}
                    {formatDateTime(request.eventEndAt)}
                  </p>
                  <p className="text-label-sm text-on-surface-variant mt-1">
                    작성일 {formatDateTime(request.createdAt)}
                  </p>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default ClientRecruitmentNoticeRequestListPage
