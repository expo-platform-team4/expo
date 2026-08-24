'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button, Card, Input, Textarea } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'
import { formatCurrency } from '@/lib/currency'

import type { GuestRefundAuthPayload, TicketRefundIneligibilityReason } from '../api'
import { useCheckGuestRefundEligibility, useRequestGuestRefund } from '../hooks'

const INELIGIBILITY_MESSAGE: Record<TicketRefundIneligibilityReason, string> = {
  ORDER_NOT_PAID: '결제가 완료되지 않은 주문은 환불할 수 없습니다.',
  EVENT_STARTS_WITHIN_THREE_DAYS: '행사 시작 3일 이내에는 환불할 수 없습니다.',
  TICKET_ALREADY_CHECKED_IN: '이미 체크인한 티켓이 포함된 주문은 환불할 수 없습니다.',
}

const authSchema = z.object({
  password: z
    .string()
    .min(1, '비밀번호는 필수입니다.')
    .max(100, '비밀번호는 100자 이하여야 합니다.'),
  phoneNumber: z.string().min(1, '연락처는 필수입니다.').max(20, '연락처는 20자 이하여야 합니다.'),
})
type AuthFormValues = z.infer<typeof authSchema>

/**
 * 비회원 주문 상세(`GuestOrderDetailPage`)에 붙는 전체 환불 신청 패널.
 *
 * 조회 결과(`GuestOrderSearchResult`)엔 연락처·비밀번호가 없다 — 조회 스토어가 보안상
 * 저장하지 않기 때문(`store.ts` 참고). 그래서 환불도 그 값들을 다시 입력받아
 * "연락처+비밀번호 조합이 곧 인증" 이라는 같은 규칙으로 백엔드에 보낸다.
 *
 * 2단계: (1) 인증 값 + 사유를 입력하고 "환불 가능 여부 확인" → eligibility 조회.
 * (2) 가능하면 예상 환불 금액을 보여주고 같은 인증 값으로 실제 환불을 요청한다.
 * 인증 값을 두 번 입력받지 않으려고 컴포넌트 state 에 잠깐 들고 있는다(전역 스토어엔 안 둔다).
 */
export const GuestRefundPanel = ({ orderNumber }: { orderNumber: string }) => {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [auth, setAuth] = useState<GuestRefundAuthPayload | null>(null)

  const checkEligibility = useCheckGuestRefundEligibility()
  const requestRefund = useRequestGuestRefund()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AuthFormValues>({
    resolver: zodResolver(authSchema),
    defaultValues: { password: '', phoneNumber: '' },
  })

  if (!open) {
    return (
      <Button variant="secondary" onClick={() => setOpen(true)}>
        환불 신청
      </Button>
    )
  }

  const cancel = () => {
    setOpen(false)
    setAuth(null)
    checkEligibility.reset()
    requestRefund.reset()
  }

  const onCheck = (values: AuthFormValues) => {
    const payload: GuestRefundAuthPayload = { orderNumber, ...values }
    setAuth(payload)
    checkEligibility.mutate(payload)
  }

  const eligibility = checkEligibility.data

  return (
    <Card className="flex flex-col gap-4">
      {!auth && (
        <form className="flex flex-col gap-4" onSubmit={handleSubmit(onCheck)} noValidate>
          <p className="text-body-md text-on-surface-variant">
            환불 신청을 하려면 연락처와 비밀번호를 다시 입력해 주세요.
          </p>
          <Input
            label="연락처"
            placeholder="01012345678"
            error={errors.phoneNumber?.message}
            {...register('phoneNumber')}
          />
          <Input
            label="비밀번호"
            type="password"
            error={errors.password?.message}
            {...register('password')}
          />
          <Textarea
            label="환불 사유 (선택)"
            placeholder="환불 사유를 입력해 주세요."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={1000}
          />
          <div className="flex gap-2">
            <Button type="submit" loading={checkEligibility.isPending}>
              환불 가능 여부 확인
            </Button>
            <Button type="button" variant="ghost" onClick={cancel}>
              취소
            </Button>
          </div>
          {checkEligibility.isError && (
            <p className="text-error text-label-sm">
              {getErrorMessage(checkEligibility.error, '환불 가능 여부를 확인하지 못했습니다.')}
            </p>
          )}
        </form>
      )}

      {auth && eligibility && !eligibility.refundable && (
        <div className="flex flex-col gap-3">
          <p className="text-error text-body-md">
            {eligibility.ineligibilityReason
              ? INELIGIBILITY_MESSAGE[eligibility.ineligibilityReason]
              : '이 주문은 환불할 수 없습니다.'}
          </p>
          <Button variant="ghost" onClick={cancel} className="self-start">
            닫기
          </Button>
        </div>
      )}

      {auth && eligibility && eligibility.refundable && !requestRefund.isSuccess && (
        <div className="flex flex-col gap-3">
          {eligibility.expectedRefundAmount != null && (
            <p className="text-body-md text-on-surface">
              예상 환불 금액{' '}
              <span className="font-semibold">
                {formatCurrency(eligibility.expectedRefundAmount)}
              </span>
            </p>
          )}
          {requestRefund.isError && (
            <p className="text-error text-label-sm">
              {getErrorMessage(requestRefund.error, '환불 신청에 실패했습니다.')}
            </p>
          )}
          <div className="flex gap-2">
            <Button
              variant="danger"
              loading={requestRefund.isPending}
              onClick={() => requestRefund.mutate({ ...auth, reason })}
            >
              환불 신청 확정
            </Button>
            <Button variant="ghost" disabled={requestRefund.isPending} onClick={cancel}>
              취소
            </Button>
          </div>
        </div>
      )}

      {requestRefund.isSuccess && (
        <p className="text-body-md text-on-surface">환불 신청이 접수되었습니다.</p>
      )}
    </Card>
  )
}
