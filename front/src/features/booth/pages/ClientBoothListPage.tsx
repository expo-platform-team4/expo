'use client'

import { EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'

import { BoothCard } from '../components/BoothCard'
import { useMyConfirmedBooths } from '../hooks'

/**
 * `/client/booths`. Function.md 3절에는 이 화면에 대응하는 전용 행이 없다 — CLIENT 역할
 * 설명("주최사 포털 전체 (박람회·부스·모집공고·정산·체크인)")에서만 언급된다. `docs/init_table_schema.md`
 * 의 booth 절과 실제 컨트롤러를 근거로 범위를 정했다.
 *
 * **목록은 `GET /api/client/me/booths` 로 가져온다**(`ClientBoothDashboardController`,
 * `features/booth/api.ts` 의 `listMyConfirmedBooths`). `v_client_dashboard_booths` 뷰를
 * `boothAllocationId != null` 로 필터링해 **확정 배정된 부스만** 돌려준다 — 신청·주문·결제
 * 진행 중인 건까지 전부 보여주는 "참여 신청 내역"(`/client/participations`,
 * `features/participation/*`, `GET /api/client/me/participations`)과 목적이 겹치지 않는다.
 * 이 화면은 "지금 내가 운영해야 할 부스가 무엇인가" 에 답한다.
 *
 * 카드 하나(`BoothCard`)가 부스 하나를 담당한다 — 배정 상세(펼치기)와 부스 소개 콘텐츠 관리
 * (`BoothContentSection`)를 그 안에 둔다. 콘텐츠 관리의 알려진 API 갭은
 * `BoothContentSection` 주석에 정리했다.
 */
const ClientBoothListPage = () => {
  const { data: booths, isPending, isError, error, refetch } = useMyConfirmedBooths()

  return (
    <div>
      <PageHeader
        title="부스 관리"
        description="확정 배정된 내 부스와 부스 소개 콘텐츠를 관리합니다."
      />

      {isPending ? (
        <LoadingBlock label="부스 목록을 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : booths.length === 0 ? (
        <EmptyState
          title="확정 배정된 부스가 없습니다"
          description="참여 신청이 승인되고 부스가 배정되면 이 화면에 나타납니다. 신청 진행 상황은 참여 신청 내역에서 확인할 수 있습니다."
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {booths.map((booth) => (
            <li key={booth.boothAllocationId}>
              <BoothCard booth={booth} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default ClientBoothListPage
