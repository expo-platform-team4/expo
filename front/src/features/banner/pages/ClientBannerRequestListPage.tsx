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
import { useClientMyExpos } from '@/features/client/hooks'
import { formatDateTime } from '@/lib/date'

import { bannerImageUrl, type BannerApplication } from '../api'
import { useMyBannerRequests } from '../hooks'
import { BANNER_REVIEW_STATUS } from '../statusLabels'

const RequestCard = ({
  request,
  expoTitle,
}: {
  request: BannerApplication
  expoTitle: string | undefined
}) => {
  const status = BANNER_REVIEW_STATUS[request.reviewStatus]

  return (
    <Card>
      <div className="flex gap-4">
        {/* eslint-disable-next-line @next/next/no-img-element -- 백엔드 프록시 경로라 Next 이미지 최적화 대상이 아니다. */}
        <img
          src={bannerImageUrl(request.imageFileId)}
          alt={request.headline ?? '신청한 배너 이미지'}
          className="h-20 w-32 shrink-0 rounded-md object-cover"
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-title-md text-on-surface truncate font-semibold">
              {expoTitle ?? `박람회 #${request.expoId}`}
            </h3>
            <Badge variant={status.variant}>{status.label}</Badge>
          </div>
          {request.headline && (
            <p className="text-label-sm text-on-surface-variant mt-1 truncate">
              {request.headline}
            </p>
          )}
          <p className="text-label-sm text-on-surface-variant mt-2">
            희망 노출 {formatDateTime(request.requestedStartAt)} ~{' '}
            {formatDateTime(request.requestedEndAt)}
          </p>
          {request.reviewedAt && (
            <p className="text-label-sm text-on-surface-variant mt-1">
              심사 완료 {formatDateTime(request.reviewedAt)}
            </p>
          )}
        </div>
      </div>

      {/* 반려 사유는 신청자가 다시 신청할 때 유일하게 참고할 정보다. 접어 두지 않는다. */}
      {request.reviewStatus === 'REJECTED' && request.rejectionReason && (
        <p className="bg-error-container text-on-error-container text-label-sm mt-3 rounded-md p-3">
          반려 사유 — {request.rejectionReason}
        </p>
      )}
    </Card>
  )
}

/** `/client/banner-requests` — 내가 낸 배너 신청과 심사 결과. CLIENT 전용. */
const ClientBannerRequestListPage = () => {
  const { data, isPending, isError, error, refetch } = useMyBannerRequests()
  // 신청 응답에는 `expoId` 만 있다. 제목은 이미 불러 둔 내 박람회 목록에서 맞춘다 —
  // 행마다 박람회 상세를 부르면 목록 하나에 요청이 열 번 나간다.
  const { data: expos } = useClientMyExpos()
  const titleOf = (expoId: number) => expos?.find((expo) => expo.expoId === expoId)?.title

  return (
    <div>
      <PageHeader
        title="배너 노출 신청"
        description="내가 신청한 광고 배너의 심사 상태를 확인합니다."
        action={
          <Link href="/client/banner-requests/new">
            <Button>새 신청</Button>
          </Link>
        }
      />

      {isPending ? (
        <LoadingBlock label="신청 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState
          title="신청한 배너가 없습니다"
          description="메인 홈 상단에 박람회를 광고하려면 새 신청을 눌러 주세요."
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {data.content.map((request) => (
            <li key={request.id}>
              <RequestCard request={request} expoTitle={titleOf(request.expoId)} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default ClientBannerRequestListPage
