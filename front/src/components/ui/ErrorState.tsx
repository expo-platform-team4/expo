import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/lib/errorMessage'

import { Button } from './Button'

/** Spec.md 5절의 에러 처리 표를 화면 레벨에서 구현하는 자리. */
export const ErrorState = ({ error, onRetry }: { error: unknown; onRetry?: () => void }) => {
  const message = getErrorMessage(error)
  const status = isAxiosError(error) ? error.response?.status : undefined

  // Spec.md 5절 — 409/404 처럼 다시 불러도 결과가 같은 에러는 재시도 버튼을 굳이 달지 않는다.
  const retryable = status === undefined || status >= 500

  return (
    <div className="border-error-container flex flex-col items-center gap-3 rounded-md border px-6 py-12 text-center">
      <p className="text-body-md text-on-surface">{message}</p>
      {retryable && onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          다시 시도
        </Button>
      )}
    </div>
  )
}
