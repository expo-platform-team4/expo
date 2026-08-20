import Link from 'next/link'

import { EmptyState, PageHeader } from '@/components/ui'

/**
 * `/` — 홈. Function.md 2절 "진입점. 박람회 추천·배너".
 *
 * 배너는 이번 범위에서 제외다(이슈 #103). 박람회 추천도 나열할 원본이 없다 — 공개
 * 박람회 목록·추천 API 자체가 없어서다(이슈 #107). 둘 다 조용히 빈 화면으로 두지 않고
 * "준비 중" 을 명시한다(Function.md 7절 원칙).
 */
const HomePage = () => (
  <div className="flex flex-col gap-10">
    <PageHeader
      title="박람회 예약·티켓 플랫폼"
      description="다양한 박람회를 둘러보고 티켓을 예매해 보세요."
    />

    <section aria-label="배너">
      <EmptyState notReady title="배너" description="배너 영역은 아직 연결되지 않았습니다." />
    </section>

    <section aria-label="박람회 추천" className="flex flex-col gap-4">
      <h2 className="text-title-lg text-on-background font-semibold">추천 박람회</h2>
      <EmptyState
        notReady
        title="추천 박람회"
        description="박람회 추천 목록은 아직 연결되지 않았습니다. 박람회 목록에서 전체 목록을 확인해 주세요."
        action={
          <Link href="/expos" className="text-secondary text-label-md font-semibold">
            박람회 목록 보기 →
          </Link>
        }
      />
    </section>
  </div>
)

export default HomePage
