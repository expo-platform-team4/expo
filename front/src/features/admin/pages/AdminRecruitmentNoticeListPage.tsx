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
import { NOTICE_STATUS } from '@/features/recruitment/statusLabels'
import { formatDateTime } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  canCancelNotice,
  canCloseNotice,
  canPublishNotice,
  type RecruitmentNotice,
} from '../recruitmentApi'
import {
  useAdminNotices,
  useCancelAdminNotice,
  useCloseAdminNotice,
  usePublishAdminNotice,
} from '../recruitmentHooks'

const NoticeRow = ({ notice }: { notice: RecruitmentNotice }) => {
  const publishMutation = usePublishAdminNotice()
  const closeMutation = useCloseAdminNotice()
  const cancelMutation = useCancelAdminNotice()

  const pending = publishMutation.isPending || closeMutation.isPending || cancelMutation.isPending
  const actionError =
    publishMutation.error ?? closeMutation.error ?? cancelMutation.error ?? undefined

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-title-lg text-primary font-semibold">{notice.title}</h3>
        <Badge variant={NOTICE_STATUS[notice.status].variant}>
          {NOTICE_STATUS[notice.status].label}
        </Badge>
      </div>
      <p className="text-label-sm text-on-surface-variant mt-3">
        요청 #{notice.requestId} · 주최 클라이언트 #{notice.hostClientId}
      </p>
      <p className="text-label-sm text-on-surface-variant mt-1">
        신청 기간 {formatDateTime(notice.applicationStartAt)} ~{' '}
        {formatDateTime(notice.applicationEndAt)}
      </p>
      {notice.publishedAt && (
        <p className="text-label-sm text-on-surface-variant mt-1">
          게시일 {formatDateTime(notice.publishedAt)}
        </p>
      )}
      {notice.closedAt && (
        <p className="text-label-sm text-on-surface-variant mt-1">
          마감일 {formatDateTime(notice.closedAt)}
        </p>
      )}

      {actionError && (
        <p className="text-label-sm text-error mt-2">{getErrorMessage(actionError)}</p>
      )}

      <div className="mt-3 flex flex-wrap gap-2">
        {canPublishNotice(notice.status) && (
          <Button
            size="sm"
            loading={publishMutation.isPending}
            disabled={pending}
            onClick={() => {
              if (window.confirm('이 공고를 게시할까요?')) publishMutation.mutate(notice.id)
            }}
          >
            게시
          </Button>
        )}
        {canCloseNotice(notice.status) && (
          <Button
            size="sm"
            variant="secondary"
            loading={closeMutation.isPending}
            disabled={pending}
            onClick={() => {
              if (window.confirm('이 공고를 조기 마감할까요?')) closeMutation.mutate(notice.id)
            }}
          >
            조기 마감
          </Button>
        )}
        {canCancelNotice(notice.status) && (
          <Button
            size="sm"
            variant="danger"
            loading={cancelMutation.isPending}
            disabled={pending}
            onClick={() => {
              const reason = window.prompt('취소 사유를 입력해 주세요 (선택 사항).') ?? undefined
              if (window.confirm('이 공고를 직권 취소할까요?')) {
                cancelMutation.mutate({ noticeId: notice.id, reason })
              }
            }}
          >
            직권 취소
          </Button>
        )}
      </div>
    </Card>
  )
}

/** `/admin/recruitment-notices`. Function.md 4절 — "게시·마감·취소". ADMIN 전용. */
const AdminRecruitmentNoticeListPage = () => {
  const { data: notices, isPending, isError, error, refetch } = useAdminNotices()

  return (
    <div>
      <PageHeader
        title="모집공고 관리"
        description="기업 모집 공고를 게시·마감·취소합니다."
        action={
          <Link href="/admin/recruitment-notices/new">
            <Button>새 공고 작성</Button>
          </Link>
        }
      />

      {isPending ? (
        <LoadingBlock label="공고 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : notices.length === 0 ? (
        <EmptyState
          title="작성된 공고가 없습니다"
          description="승인된 모집공고 생성 요청을 근거로 새 공고를 작성해 보세요."
          action={
            <Link href="/admin/recruitment-notices/new">
              <Button>새 공고 작성</Button>
            </Link>
          }
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {notices.map((notice) => (
            <li key={notice.id}>
              <NoticeRow notice={notice} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default AdminRecruitmentNoticeListPage
