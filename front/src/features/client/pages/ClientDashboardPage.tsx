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
import { NOTICE_STATUS } from '@/features/recruitment/statusLabels'
import { formatDate } from '@/lib/date'

import {
  EXPO_EVENT_STATUS_LABEL,
  EXPO_REVIEW_STATUS_LABEL,
  type ClientDashboardRecruitment,
} from '../api'
import {
  useClientMyConfirmedBooths,
  useClientMyExpos,
  useClientMyRecruitmentResults,
} from '../hooks'

type StatCard = { label: string; value: number; description: string }

/** `/client/dashboard`. Function.md 3절 — "요약 지표". 세 도메인(박람회·부스·모집공고)을 모아 보여준다. */
const ClientDashboardPage = () => {
  const exposQuery = useClientMyExpos()
  const boothsQuery = useClientMyConfirmedBooths()
  const recruitmentQuery = useClientMyRecruitmentResults()

  const isPending = exposQuery.isPending || boothsQuery.isPending || recruitmentQuery.isPending
  const isError = exposQuery.isError || boothsQuery.isError || recruitmentQuery.isError
  const firstError = exposQuery.error ?? boothsQuery.error ?? recruitmentQuery.error

  const refetchAll = () => {
    exposQuery.refetch()
    boothsQuery.refetch()
    recruitmentQuery.refetch()
  }

  if (isError) {
    return (
      <div>
        <PageHeader title="클라이언트 대시보드" />
        <ErrorState error={firstError} onRetry={refetchAll} />
      </div>
    )
  }

  if (isPending) {
    return (
      <div>
        <PageHeader title="클라이언트 대시보드" />
        <LoadingBlock label="요약 지표를 불러오는 중입니다" />
      </div>
    )
  }

  const expos = exposQuery.data
  const booths = boothsQuery.data
  const recruitments = recruitmentQuery.data

  const stats: StatCard[] = [
    {
      label: '등록 박람회',
      value: expos.length,
      description: '내가 개최를 등록한 박람회 전체 수',
    },
    {
      label: '진행중인 박람회',
      value: expos.filter((expo) => expo.eventStatus === 'ONGOING').length,
      description: '현재 행사가 진행중인 박람회 수',
    },
    {
      label: '확정 배정 부스',
      value: booths.length,
      description: '모집공고를 통해 확정 배정받은 부스 수',
    },
    {
      label: '모집공고 신청 합계',
      value: recruitments.reduce((sum, item) => sum + item.submittedApplicationCount, 0),
      description: '내 모집공고에 접수된 신청 건수 합계',
    },
  ]

  const recentExpos = [...expos]
    .sort((a, b) => new Date(b.eventStartAt).getTime() - new Date(a.eventStartAt).getTime())
    .slice(0, 5)

  return (
    <div>
      <PageHeader title="클라이언트 대시보드" description="박람회·부스·모집공고 요약 지표입니다." />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.label}>
            <p className="text-body-sm text-on-surface-variant">{stat.label}</p>
            <p className="text-headline-sm text-on-background mt-1 font-semibold">
              {stat.value.toLocaleString('ko-KR')}
            </p>
            <p className="text-body-sm text-on-surface-variant mt-1">{stat.description}</p>
          </Card>
        ))}
      </div>

      <div className="mt-6">
        <CardTitle className="mb-3">최근 박람회</CardTitle>
        {recentExpos.length === 0 ? (
          <EmptyState
            title="등록한 박람회가 없습니다"
            description="박람회 개최 신청 화면에서 새 박람회를 등록해 보세요."
          />
        ) : (
          <Card className="divide-outline-variant divide-y p-0">
            {recentExpos.map((expo) => (
              <Link
                key={expo.expoId}
                href="/client/expos"
                className="hover:bg-surface-container-low flex items-center justify-between gap-4 px-6 py-4 transition-colors"
              >
                <div>
                  <p className="text-title-md text-on-surface font-medium">{expo.title}</p>
                  <p className="text-body-sm text-on-surface-variant mt-1">
                    {formatDate(expo.eventStartAt)} ~ {formatDate(expo.eventEndAt)}
                  </p>
                </div>
                <div className="text-body-sm text-on-surface-variant shrink-0 text-right">
                  <p>{EXPO_EVENT_STATUS_LABEL[expo.eventStatus]}</p>
                  <p>{EXPO_REVIEW_STATUS_LABEL[expo.reviewStatus]}</p>
                </div>
              </Link>
            ))}
          </Card>
        )}
      </div>

      <div className="mt-6">
        <CardTitle className="mb-3">내 모집공고</CardTitle>
        {recruitments.length === 0 ? (
          <EmptyState
            title="작성된 모집공고가 없습니다"
            description="박람회가 승인되면 관리자가 모집공고 생성 요청을 검토해 공고를 작성합니다."
          />
        ) : (
          <Card className="divide-outline-variant divide-y p-0">
            {recruitments.map((notice) => (
              <RecruitmentNoticeRow key={notice.recruitmentNoticeId} notice={notice} />
            ))}
          </Card>
        )}
      </div>
    </div>
  )
}

const RecruitmentNoticeRow = ({ notice }: { notice: ClientDashboardRecruitment }) => {
  const status = NOTICE_STATUS[notice.status as keyof typeof NOTICE_STATUS] as
    (typeof NOTICE_STATUS)[keyof typeof NOTICE_STATUS] | undefined
  const content = (
    <div className="flex items-center justify-between gap-4 px-6 py-4">
      <div>
        <div className="flex items-center gap-2">
          <p className="text-title-md text-on-surface font-medium">{notice.title}</p>
          <Badge variant={status?.variant ?? 'neutral'}>{status?.label ?? notice.status}</Badge>
        </div>
        <p className="text-body-sm text-on-surface-variant mt-1">
          신청 기간 {formatDate(notice.applicationStartAt)} ~ {formatDate(notice.applicationEndAt)}
        </p>
      </div>
      <div className="text-body-sm text-on-surface-variant shrink-0 text-right">
        <p>신청 {notice.submittedApplicationCount}건</p>
        <p>확정 {notice.confirmedAllocationCount}건</p>
      </div>
    </div>
  )

  if (notice.status !== 'OPEN') {
    return <div className="text-inherit">{content}</div>
  }

  return (
    <Link
      href={`/recruitment-notices/${notice.recruitmentNoticeId}`}
      className="hover:bg-surface-container-low block transition-colors"
    >
      {content}
    </Link>
  )
}

export default ClientDashboardPage
