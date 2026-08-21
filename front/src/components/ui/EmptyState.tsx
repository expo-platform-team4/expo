import type { ReactNode } from 'react'

/**
 * 빈 목록 · API 미구현 화면에서 쓴다.
 *
 * front/Function.md 7절 원칙 — API 가 없는 화면은 "화면은 만들되 준비 중임을 명시한다."
 * `notReady` 를 켜면 그 문구로 렌더링한다. 조용히 빈 목록만 보여주면 버그처럼 보인다.
 */
export const EmptyState = ({
  title,
  description,
  notReady = false,
  action,
}: {
  title: string
  description?: string
  notReady?: boolean
  action?: ReactNode
}) => (
  <div className="border-outline-variant flex flex-col items-center gap-2 rounded-md border border-dashed px-6 py-16 text-center">
    <p className="text-title-lg text-on-surface font-semibold">
      {notReady ? '준비 중입니다' : title}
    </p>
    <p className="text-body-md text-on-surface-variant max-w-sm">
      {notReady ? (description ?? '이 기능은 아직 연결되지 않았습니다.') : description}
    </p>
    {action}
  </div>
)
