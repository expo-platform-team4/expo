/** venue 도메인 React Query 캐시 키. */
export const venueKeys = {
  all: ['venue'] as const,

  virtualVenues: () => [...venueKeys.all, 'virtual-venues'] as const,
  halls: (venueId: number) => [...venueKeys.all, 'virtual-venues', venueId, 'halls'] as const,
  zones: (hallId: number) => [...venueKeys.all, 'halls', hallId, 'zones'] as const,
}
