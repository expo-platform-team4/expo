'use client'

import { useEffect, useState } from 'react'

import { Badge, Button, Input } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useConfirmEmailVerification, useRequestEmailVerification } from '../hooks'

/**
 * 회원·클라이언트 가입 폼이 공유하는 이메일 인증 위젯. 휴대폰 인증(`PhoneVerificationField`)과
 * 흐름은 같다(인증코드 요청 → 6자리 확인) — 다만 이메일 입력칸
 * 자체는 이 컴포넌트가 갖지 않는다. 이메일은 이미 중복확인(onBlur)이 붙은 별도 `Input`이
 * 있어서, 여기서 또 하나 만들면 입력칸이 두 개가 된다. 대신 부모가 현재 이메일 값을
 * `email` prop 으로 내려준다.
 *
 * 인증에 성공하면 서버가 `signupVerificationToken` 을 돌려준다. 이 토큰을 폼의 숨은 필드
 * (`emailVerificationToken`)에 실어 회원가입 요청에 같이 보내야 한다 — 그래야 서버가 "이
 * 이메일이 실제로 확인됐는지"를 검증할 수 있다. 휴대폰 인증은 아직 이 토큰을 회원가입이
 * 검증하지 않지만(별도 이슈), 이메일은 처음부터 검증하도록 만들었다.
 */
export const EmailVerificationField = ({
  email,
  emailHasError,
  onVerified,
}: {
  email: string
  emailHasError: boolean
  onVerified: (signupVerificationToken: string | null) => void
}) => {
  const [verificationId, setVerificationId] = useState<number | null>(null)
  const [code, setCode] = useState('')
  const [requestError, setRequestError] = useState<string | null>(null)
  const [confirmError, setConfirmError] = useState<string | null>(null)
  const [requestMessage, setRequestMessage] = useState<string | null>(null)
  const [verified, setVerified] = useState(false)

  const requestMutation = useRequestEmailVerification()
  const confirmMutation = useConfirmEmailVerification()

  // 이메일 값이 바뀌면 이전 인증은 무효다 — 주소를 바꿔놓고 이전 인증 토큰을 그대로
  // 들고 있으면 안 된다 (다른 이메일로 인증받은 토큰이 새 이메일에 쓰이면 안 되므로).
  useEffect(() => {
    setVerified(false)
    setVerificationId(null)
    setCode('')
    setRequestMessage(null)
    onVerified(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [email])

  const handleRequest = () => {
    setRequestError(null)
    setConfirmError(null)
    requestMutation.mutate(email, {
      onSuccess: (result) => {
        setVerificationId(result.verificationId)
        setRequestMessage(result.message)
        setCode('')
      },
      onError: (error) => setRequestError(getErrorMessage(error)),
    })
  }

  const handleConfirm = () => {
    if (verificationId == null) return
    setConfirmError(null)
    confirmMutation.mutate(
      { verificationId, verificationCode: code },
      {
        onSuccess: (result) => {
          setVerified(true)
          onVerified(result.signupVerificationToken)
        },
        onError: (error) => setConfirmError(getErrorMessage(error)),
      }
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-end gap-2">
        <Button
          type="button"
          variant="secondary"
          size="md"
          disabled={verified || !email || emailHasError}
          loading={requestMutation.isPending}
          onClick={handleRequest}
        >
          {verificationId == null ? '이메일로 인증코드 받기' : '재발송'}
        </Button>
      </div>
      {requestError && <p className="text-label-sm text-error">{requestError}</p>}
      {requestMessage && !requestError && (
        <p className="text-label-sm text-on-surface-variant">{requestMessage}</p>
      )}

      {verificationId != null && !verified && (
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <Input
              label="인증코드 6자리"
              value={code}
              maxLength={6}
              inputMode="numeric"
              onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
              error={confirmError ?? undefined}
            />
          </div>
          <Button
            type="button"
            variant="secondary"
            size="md"
            disabled={code.length !== 6}
            loading={confirmMutation.isPending}
            onClick={handleConfirm}
          >
            확인
          </Button>
        </div>
      )}

      {verified && <Badge variant="success">이메일 인증 완료</Badge>}
    </div>
  )
}
