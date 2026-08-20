'use client'

import { useRouter } from 'next/navigation'
import { useEffect } from 'react'

import { EmptyState, ErrorState, Select, Spinner } from '@/components/ui'
import { useClientMyExpos } from '@/features/client/hooks'

/**
 * 체크인 3화면(현황·스캔·이력)이 공유하는 "어느 박람회인가" 선택기.
 *
 * 체크인 API 전부가 `/api/client/expos/{expoId}/...` 라 화면마다 expoId 가 필요하다
 * (docs/screen-api-map.md 4-1절, `ClientCheckInController`). 현장 스태프 계정이 없어
 * **주최사 본인이** 스캔하는데, 주최사가 박람회를 여러 개 열 수 있으므로 "내 박람회"
 * 목록(`features/client`)에서 골라야 한다.
 *
 * 선택 상태는 URL 쿼리(`?expoId=`)에 둔다 — 새로고침·북마크에서 살아남고, 세 화면이
 * 링크로 넘나들 때 그대로 들고 갈 수 있다(`OrderHomePage.tsx` 의 `?expoId=` 패턴과 같다).
 * 내가 연 박람회가 하나뿐이면 고를 것도 없으므로 자동으로 그 박람회를 선택한다.
 */
export const ExpoSwitcher = ({ expoId, basePath }: { expoId: number | null; basePath: string }) => {
  const router = useRouter()
  const exposQuery = useClientMyExpos()
  const expos = exposQuery.data

  useEffect(() => {
    if (!expos || expoId !== null) return
    if (expos.length === 1) {
      router.replace(`${basePath}?expoId=${expos[0].expoId}`)
    }
  }, [expos, expoId, basePath, router])

  if (exposQuery.isPending) {
    return (
      <div className="flex items-center gap-2">
        <Spinner size="sm" />
        <span className="text-body-md text-on-surface-variant">
          박람회 목록을 불러오는 중입니다
        </span>
      </div>
    )
  }

  if (exposQuery.isError) {
    return <ErrorState error={exposQuery.error} onRetry={() => exposQuery.refetch()} />
  }

  const loadedExpos = exposQuery.data

  if (loadedExpos.length === 0) {
    return (
      <EmptyState
        title="열린 박람회가 없습니다"
        description="체크인을 진행하려면 먼저 박람회를 개최해야 합니다."
      />
    )
  }

  return (
    <div className="max-w-xs">
      <Select
        label="박람회"
        value={expoId ? String(expoId) : ''}
        onChange={(event) => router.replace(`${basePath}?expoId=${event.target.value}`)}
      >
        <option value="" disabled>
          박람회를 선택해 주세요
        </option>
        {loadedExpos.map((expo) => (
          <option key={expo.expoId} value={expo.expoId}>
            {expo.title}
          </option>
        ))}
      </Select>
    </div>
  )
}
