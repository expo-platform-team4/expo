'use client'

import { Button } from './Button'

/**
 * 목록 페이지 이동. `이전 / N페이지 · 전체 M건 / 다음` 한 줄이다.
 *
 * <h2>페이지 번호는 1부터다</h2>
 *
 * 화면이 보여주는 숫자와 같은 값을 쓴다. 0부터 세면 표시할 때마다 `+1` 을 해야 하고, 그
 * 변환을 한 군데서 빠뜨리면 <b>엉뚱한 페이지를 보여주면서도 에러가 안 난다.</b>
 *
 * <p>번호 목록을 나열하지 않는다. 배너 신청처럼 <b>순서대로 훑는</b> 목록은 특정 페이지로
 * 건너뛸 일이 없다.
 */
export const Pagination = ({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  /** 1부터 센다. */
  page: number
  totalPages: number
  totalElements: number
  onChange: (next: number) => void
}) => {
  // 한 페이지에 다 들어가면 조작할 것이 없다.
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="mt-4 flex items-center justify-between">
      <Button
        variant="secondary"
        size="sm"
        disabled={page <= 1}
        onClick={() => onChange(Math.max(1, page - 1))}
      >
        이전
      </Button>
      <p className="text-label-sm text-on-surface-variant">
        {page} / {totalPages}페이지 · 전체 {totalElements.toLocaleString('ko-KR')}건
      </p>
      <Button
        variant="secondary"
        size="sm"
        disabled={page >= totalPages}
        onClick={() => onChange(Math.min(totalPages, page + 1))}
      >
        다음
      </Button>
    </div>
  )
}
