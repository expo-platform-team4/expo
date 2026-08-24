'use client'

import Link from 'next/link'

import {
  Badge,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { formatDateTime } from '@/lib/date'

import { useRecruitmentNotices } from '../hooks'
import { NOTICE_STATUS } from '../statusLabels'

/** `/recruitment-notices`. Function.md 2절 — 게시 중인 기업 모집 공고 목록. 공개 화면. */
const RecruitmentNoticeListPage = () => {
  const { data: notices, isPending, isError, error, refetch } = useRecruitmentNotices()

  return (
    <div>
      <PageHeader
        title="공고 모집 목록"
        description="현재 참가 신청을 받고 있는 기업 모집 공고입니다."
      />

      {isPending ? (
        <LoadingBlock label="공고를 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : notices.length === 0 ? (
        <EmptyState
          title="모집 중인 공고가 없습니다"
          description="새 공고가 게시되면 여기에 표시됩니다."
        />
      ) : (
        <ul className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {notices.map((notice) => (
            <li key={notice.id}>
              <Link href={`/recruitment-notices/${notice.id}`} className="block h-full">
                <Card className="h-full transition-shadow hover:shadow-[0_20px_25px_-5px_rgba(26,43,75,0.1),0_10px_10px_-5px_rgba(26,43,75,0.04)]">
                  <div className="mb-2 flex items-start justify-between gap-2">
                    <CardTitle className="mb-0">{notice.title}</CardTitle>
                    <Badge variant={NOTICE_STATUS[notice.status].variant}>
                      {NOTICE_STATUS[notice.status].label}
                    </Badge>
                  </div>
                  <p className="text-body-md text-on-surface-variant line-clamp-2">
                    {notice.content}
                  </p>
                  <p className="text-label-sm text-on-surface-variant mt-4">
                    신청 기간 {formatDateTime(notice.applicationStartAt)} ~{' '}
                    {formatDateTime(notice.applicationEndAt)}
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

export default RecruitmentNoticeListPage
