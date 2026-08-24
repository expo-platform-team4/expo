'use client'

import { useState } from 'react'
import type { Control, FieldValues, Path } from 'react-hook-form'
import { useController } from 'react-hook-form'

import { Badge, Button, Input } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useConfirmPhoneVerification, useRequestPhoneVerification } from '../hooks'

/**
 * 회원·클라이언트 가입 폼이 공유하는 휴대폰 본인인증 위젯. Function.md 2절 —
 * "휴대폰 인증 포함". 번호 입력 → 인증번호 요청 → 6자리 확인의 2단계 흐름을 한 컴포넌트로
 * 묶는다. 두 가입 폼에 이 흐름을 각각 손으로 옮기면 상태 기계가 두 벌 생긴다.
 *
 * 인증번호는 백엔드가 요청마다 무작위 6자리를 만들어 SMS 로 보낸다. 응답에는 들어 있지 않으므로
 * 화면이 미리 알 수 있는 값이 없다. 같은 번호로 재요청하려면 60초를 기다려야 한다(429).
 */
export const PhoneVerificationField = <T extends FieldValues>({
  control,
  name,
  verified,
  onVerifiedChange,
}: {
  control: Control<T>
  name: Path<T>
  verified: boolean
  onVerifiedChange: (verified: boolean) => void
}) => {
  const { field, fieldState } = useController({ control, name })
  const [verificationId, setVerificationId] = useState<number | null>(null)
  const [code, setCode] = useState('')
  const [requestError, setRequestError] = useState<string | null>(null)
  const [confirmError, setConfirmError] = useState<string | null>(null)
  const [requestMessage, setRequestMessage] = useState<string | null>(null)

  const requestMutation = useRequestPhoneVerification()
  const confirmMutation = useConfirmPhoneVerification()

  const phoneNumber = field.value as string

  const handleRequest = () => {
    setRequestError(null)
    setConfirmError(null)
    onVerifiedChange(false)
    requestMutation.mutate(phoneNumber, {
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
        onSuccess: () => onVerifiedChange(true),
        onError: (error) => setConfirmError(getErrorMessage(error)),
      }
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-end gap-2">
        <div className="flex-1">
          <Input
            {...field}
            label="휴대폰 번호"
            placeholder="01012345678"
            disabled={verified}
            error={fieldState.error?.message}
            onChange={(event) => {
              onVerifiedChange(false)
              setVerificationId(null)
              field.onChange(event)
            }}
          />
        </div>
        <Button
          type="button"
          variant="secondary"
          size="md"
          disabled={verified || !phoneNumber || Boolean(fieldState.error)}
          loading={requestMutation.isPending}
          onClick={handleRequest}
        >
          {verificationId == null ? '인증번호 받기' : '재발송'}
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
              label="인증번호 6자리"
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

      {verified && <Badge variant="success">휴대폰 인증 완료</Badge>}
    </div>
  )
}
