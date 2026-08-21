import type { UseMutationResult } from '@tanstack/react-query'
import { useState } from 'react'

import { getErrorMessage } from '@/lib/errorMessage'

import type { AvailabilityResult } from './api'

export type AvailabilityHint = { ok: boolean; message: string }

/**
 * 이메일·닉네임·사업자등록번호 세 곳에서 똑같이 반복되는 "blur 시 중복 확인" 패턴을
 * 훅 하나로 묶는다. 컴포넌트로 감싸지 않은 이유 — `Input` 하나에 register 와 이 훅의
 * `check` 를 같이 물리는 게 전부라, 감싸는 컴포넌트를 새로 만들면 `Input` 이 두 곳(직접 쓰는
 * 자리·이 안)에 흩어진다.
 */
export const useAvailabilityHint = (
  mutation: UseMutationResult<AvailabilityResult, unknown, string>
) => {
  const [hint, setHint] = useState<AvailabilityHint | null>(null)

  const check = (value: string) => {
    if (!value) {
      setHint(null)
      return
    }
    mutation.mutate(value, {
      onSuccess: (result) => setHint({ ok: result.available, message: result.message }),
      onError: (error) => setHint({ ok: false, message: getErrorMessage(error) }),
    })
  }

  const reset = () => setHint(null)

  return { hint, check, reset, checking: mutation.isPending }
}
