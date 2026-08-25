'use client'

import { useParams, useRouter } from 'next/navigation'

import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { formatDate } from '@/lib/date'

import {
  PARTICIPATION_APPLICATION_STATUS_LABEL,
  type ParticipationApplicationStatus,
} from '../participatingCompanyApi'
import { useParticipatingCompanies } from '../participatingCompanyHooks'

const STATUS_VARIANT: Record<ParticipationApplicationStatus, BadgeVariant> = {
  DRAFT: 'neutral',
  PAYMENT_PENDING: 'info',
  SUBMITTED: 'success',
  PAYMENT_FAILED: 'error',
  CANCELED: 'neutral',
}

/**
 * `/client/expos/{expoId}/participating-companies` — 주최사가 자기 박람회에 참여 신청한
 * 기업 목록을 본다.
 *
 * 연결 경로는 `expos` → `recruitment_notices`(expo_id) → `participation_applications`
 * (recruitment_notice_id) 다 — 백엔드도 이 경로를 그대로 따라간다. 신청 취소(CANCELED)
 * 건도 함께 내려온다, 취소 이력도 주최사에게는 참고가 된다.
 */
const ClientParticipatingCompanyListPage = () => {
  const params = useParams<{ expoId: string }>()
  const router = useRouter()
  const parsedExpoId = Number(params.expoId)
  const expoId = Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  const { data, isPending, isError, error, refetch } = useParticipatingCompanies(expoId)

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="참여 기업 목록" />
        <EmptyState title="잘못된 접근입니다" description="박람회 주소가 올바르지 않습니다." />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="참여 기업 목록"
        description="이 박람회에 참여 신청한 기업 목록입니다."
        action={
          <Button variant="secondary" onClick={() => router.push('/client/expos')}>
            내 박람회
          </Button>
        }
      />

      {isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : isPending ? (
        <LoadingBlock label="참여 기업을 불러오는 중입니다" />
      ) : data.length === 0 ? (
        <EmptyState
          title="아직 참여 신청한 기업이 없습니다"
          description="모집공고가 게시되고 기업이 신청하면 여기에 나타납니다."
        />
      ) : (
        <div className="flex flex-col gap-3">
          {data.map((company) => (
            <Card key={company.applicationId}>
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-title-lg text-on-surface font-semibold">
                    {company.companyName}
                  </p>
                  {company.participationPurpose && (
                    <p className="text-body-sm text-on-surface-variant mt-1">
                      참여 목적: {company.participationPurpose}
                    </p>
                  )}
                  {company.exhibitDescription && (
                    <p className="text-body-sm text-on-surface-variant mt-1">
                      전시 품목: {company.exhibitDescription}
                    </p>
                  )}
                  <p className="text-body-sm text-on-surface-variant mt-1">
                    {company.submittedAt
                      ? `참여 확정 ${formatDate(company.submittedAt)}`
                      : `신청 ${formatDate(company.createdAt)}`}
                  </p>
                </div>
                <Badge variant={STATUS_VARIANT[company.status]}>
                  {PARTICIPATION_APPLICATION_STATUS_LABEL[company.status]}
                </Badge>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

export default ClientParticipatingCompanyListPage
