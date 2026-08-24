import { useMutation } from '@tanstack/react-query'

import * as paymentApi from './api'

/** 결제 시작. `POST /api/payments/initiate`. */
export const useInitiateTicketPayment = () =>
  useMutation({ mutationFn: paymentApi.initiateTicketPayment })

/** 결제 승인 확정. `POST /api/payments/tickets/confirm`. */
export const useConfirmTicketPayment = () =>
  useMutation({ mutationFn: paymentApi.confirmTicketPayment })

/** 결제 실패 처리. `POST /api/payments/tickets/fail`. */
export const useFailTicketPayment = () => useMutation({ mutationFn: paymentApi.failTicketPayment })
