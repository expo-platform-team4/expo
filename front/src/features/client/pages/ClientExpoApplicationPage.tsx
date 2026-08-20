'use client'

import { useRouter } from 'next/navigation'

import { Button, EmptyState, PageHeader } from '@/components/ui'

/**
 * `/client/expos/new`. Function.md 3절 — "박람회 개최 신청".
 *
 * 이 화면을 뒷받침할 API 가 아직 없다(이슈 #107 이 이미 추적 중). Function.md 7절 원칙대로
 * 화면 자체는 두되, 없는 API 위에 없는 폼을 지어 올리지 않는다 — 제출해도 어디에도 닿지
 * 않는 폼은 버그처럼 보인다. `EmptyState` 의 `notReady` 로 "준비 중"임을 명시한다.
 */
const ClientExpoApplicationPage = () => {
  const router = useRouter()

  return (
    <div>
      <PageHeader title="박람회 개최 신청" />
      <EmptyState
        notReady
        title="박람회 개최 신청"
        description="박람회 개최 신청을 처리할 API 가 아직 준비되지 않았습니다. 이슈 #107 에서 진행 상황을 확인해 주세요."
        action={
          <Button variant="secondary" onClick={() => router.push('/client/expos')}>
            내 박람회로 돌아가기
          </Button>
        }
      />
    </div>
  )
}

export default ClientExpoApplicationPage
