'use client'

import Link from 'next/link'

import { Button, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'

import MainBannerCarousel from '@/features/banner/pages/MainBannerCarousel'

import { ExpoCardItem } from '../components/ExpoCardItem'
import { useExpoCards } from '../hooks'

/** 홈에 띄울 카드 수. 한 줄(데스크톱 3칸)이 두 줄로 딱 떨어진다. */
const FEATURED_COUNT = 6

/**
 * `/` — 홈. Function.md 2절 "진입점. 박람회 추천·배너".
 *
 * 추천은 목록 API 의 인기순 상위를 그대로 쓴다(`sort=POPULAR`). 인기 점수는 뷰가
 * 판매량·주문량으로 계산한다 — 별도 추천 엔진이 있는 것이 아니다.
 *
 * 배너는 `MainBannerCarousel` 이 맡는다. 노출할 배너가 없으면 아무것도 그리지 않아서,
 * 광고가 없는 날 홈 상단이 빈 상자로 남지 않는다.
 */
const HomePage = () => {
  const { data, isPending, isError, error, refetch } = useExpoCards({ sort: 'POPULAR' })

  return (
    <div className="flex flex-col gap-10">
      <PageHeader
        title="박람회 예약·티켓 플랫폼"
        description="다양한 박람회를 둘러보고 티켓을 예매해 보세요."
      />

      <MainBannerCarousel />

      <section aria-label="박람회 추천" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-title-lg text-on-background font-semibold">추천 박람회</h2>
          <Link href="/expos">
            <Button variant="secondary" size="sm">
              전체 목록
            </Button>
          </Link>
        </div>

        {isError ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : isPending ? (
          <LoadingBlock label="박람회를 불러오는 중입니다" />
        ) : data.length === 0 ? (
          <EmptyState
            title="아직 공개된 박람회가 없습니다"
            description="박람회가 개설되면 여기에서 가장 먼저 보실 수 있습니다."
          />
        ) : (
          <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {data.slice(0, FEATURED_COUNT).map((expo) => (
              <li key={expo.expoId}>
                <ExpoCardItem expo={expo} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

export default HomePage
