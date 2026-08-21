'use client'

import Link from 'next/link'
import { useParams, usePathname, useRouter } from 'next/navigation'

import { Badge, Button, Card, CardTitle, ErrorState, LoadingBlock } from '@/components/ui'
import { ParticipationApplyForm } from '@/features/participation/components/ParticipationApplyForm'
import { useAuthStore } from '@/lib/auth'
import { formatDateTime } from '@/lib/date'

import { useRecruitmentNotice } from '../hooks'
import { NOTICE_STATUS } from '../statusLabels'

/**
 * `/recruitment-notices/{noticeId}`. Function.md 2절 — "상세 + 참여 신청 폼 (CLIENT 전용 액션)".
 *
 * 페이지 자체는 공개다(비로그인도 상세를 본다). 참여 신청 액션만 CLIENT 로그인을 요구한다.
 * `RequireAuth` 는 라우트 단위 가드라 이 페이지에 통째로 씌울 수 없다(그러면 상세 열람까지
 * 로그인을 요구하게 된다) — 대신 `navItems.ts` 의 "공고 신청" 링크가 쓰는 것과 같은 아이디어로,
 * 액션 부분에서만 로그인 여부·역할을 직접 확인하고 미충족 시 로그인으로 보낸다.
 */
const RecruitmentNoticeDetailPage = () => {
  const params = useParams<{ noticeId: string }>()
  const noticeId = Number(params.noticeId)
  const router = useRouter()
  const pathname = usePathname()
  const accessToken = useAuthStore((state) => state.accessToken)
  const role = useAuthStore((state) => state.role)

  const {
    data: notice,
    isPending,
    isError,
    error,
    refetch,
  } = useRecruitmentNotice(Number.isFinite(noticeId) ? noticeId : null)

  if (!Number.isFinite(noticeId)) {
    return <ErrorState error={new Error('잘못된 공고 주소입니다.')} />
  }

  if (isPending) {
    return <LoadingBlock label="공고를 불러오는 중입니다" />
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  const isOpen = notice.status === 'OPEN'

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <div className="mb-2 flex items-center gap-2">
          <Badge variant={NOTICE_STATUS[notice.status].variant}>
            {NOTICE_STATUS[notice.status].label}
          </Badge>
        </div>
        <h1 className="text-headline-sm text-on-background font-semibold">{notice.title}</h1>
        <p className="text-label-sm text-on-surface-variant mt-2">
          신청 기간 {formatDateTime(notice.applicationStartAt)} ~{' '}
          {formatDateTime(notice.applicationEndAt)}
        </p>
      </div>

      <Card>
        <CardTitle>공고 내용</CardTitle>
        <p className="text-body-md text-on-surface whitespace-pre-wrap">{notice.content}</p>
      </Card>

      {notice.eligibility && (
        <Card>
          <CardTitle>참가 자격 요건</CardTitle>
          <p className="text-body-md text-on-surface whitespace-pre-wrap">{notice.eligibility}</p>
        </Card>
      )}

      {notice.submissionRequirements && (
        <Card>
          <CardTitle>제출 자료 요구사항</CardTitle>
          <p className="text-body-md text-on-surface whitespace-pre-wrap">
            {notice.submissionRequirements}
          </p>
        </Card>
      )}

      <Card>
        <CardTitle>참여 신청</CardTitle>
        {!isOpen ? (
          <p className="text-body-md text-on-surface-variant">
            현재는 신청을 받는 기간이 아닙니다. 공고 상태가 &quot;모집 중&quot;일 때만 신청할 수
            있습니다.
          </p>
        ) : !accessToken ? (
          <div className="flex flex-col items-start gap-3">
            <p className="text-body-md text-on-surface-variant">
              참여 신청은 기업(CLIENT) 계정으로 로그인해야 할 수 있습니다.
            </p>
            <Button
              type="button"
              onClick={() => router.push(`/login?redirect=${encodeURIComponent(pathname)}`)}
            >
              로그인 후 참여 신청하기
            </Button>
          </div>
        ) : role !== 'CLIENT' ? (
          <p className="text-body-md text-on-surface-variant">
            참여 신청은 기업(CLIENT) 계정만 가능합니다. 기업회원이신가요?{' '}
            <Link href="/signup/client" className="text-secondary font-semibold">
              기업회원 가입
            </Link>
          </p>
        ) : (
          <ParticipationApplyForm noticeId={notice.id} />
        )}
      </Card>
    </div>
  )
}

export default RecruitmentNoticeDetailPage
