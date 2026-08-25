import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  listAdminVenueHalls,
  listAdminVenueZones,
  listAdminVirtualVenues,
  updateVenueHallLayout,
  updateVenueZoneLayout,
} from './api'
import { venueKeys } from './queryKeys'

/** `/admin/virtual-venues` — 가상 장소 목록. 이 플랫폼은 1개(킨텍스)까지만 등록된다. */
export const useAdminVirtualVenues = () =>
  useQuery({
    queryKey: venueKeys.virtualVenues(),
    queryFn: listAdminVirtualVenues,
  })

/** `/admin/virtual-venues/{venueId}/halls` — 장소 내 전시장 목록. */
export const useAdminVenueHalls = (venueId: number | undefined) =>
  useQuery({
    queryKey: venueKeys.halls(venueId ?? -1),
    queryFn: () => listAdminVenueHalls(venueId as number),
    enabled: venueId !== undefined,
  })

/** `/admin/venue-halls/{hallId}/zones` — 전시장 내 홀 목록. */
export const useAdminVenueZones = (hallId: number | undefined) =>
  useQuery({
    queryKey: venueKeys.zones(hallId ?? -1),
    queryFn: () => listAdminVenueZones(hallId as number),
    enabled: hallId !== undefined,
  })

/** 전시장 배치도 교체. 성공하면 그 장소의 전시장 목록 캐시를 무효화한다. */
export const useUpdateVenueHallLayout = (venueId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ hallId, layoutFileId }: { hallId: number; layoutFileId: number }) =>
      updateVenueHallLayout(venueId, hallId, layoutFileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: venueKeys.halls(venueId) })
    },
  })
}

/** 홀 배치도 교체. 성공하면 그 전시장의 홀 목록 캐시를 무효화한다. */
export const useUpdateVenueZoneLayout = (hallId: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ zoneId, layoutFileId }: { zoneId: number; layoutFileId: number }) =>
      updateVenueZoneLayout(hallId, zoneId, layoutFileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: venueKeys.zones(hallId) })
    },
  })
}
