import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  createVenueReservations,
  listAdminVenueReservations,
  releaseVenueReservation,
} from './venueReservationApi'
import { adminKeys } from './queryKeys'

/** `/admin/venue-reservations` — 장소 예약 목록(전체 상태). */
export const useAdminVenueReservations = () =>
  useQuery({
    queryKey: adminKeys.venueReservations(),
    queryFn: listAdminVenueReservations,
  })

/**
 * 확정 장소 예약 생성. 승인된 모집공고 생성 요청 카드에서 부른다. 성공하면 예약 목록 캐시를
 * 무효화한다 — 요청 목록 자체는 바뀌지 않으므로 그쪽 캐시는 건드리지 않는다.
 */
export const useCreateVenueReservations = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createVenueReservations,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.venueReservations() })
    },
  })
}

/** 확정 예약 직권 해제. 이중 예약 등 운영상 정정 전용이다. */
export const useReleaseVenueReservation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ reservationId, reason }: { reservationId: number; reason: string }) =>
      releaseVenueReservation(reservationId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.venueReservations() })
    },
  })
}
