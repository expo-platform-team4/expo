export const clientKeys = {
  dashboardProfile: ['client', 'dashboard-profile'] as const,
  myExpos: ['client', 'my-expos'] as const,
  myConfirmedBooths: ['client', 'my-confirmed-booths'] as const,
  myRecruitmentResults: ['client', 'my-recruitment-results'] as const,
  settlements: (page: number) => ['client', 'settlements', page] as const,
  expoImages: (expoId: number) => ['client', 'expo-images', expoId] as const,
  expoFiles: (expoId: number) => ['client', 'expo-files', expoId] as const,
  participatingCompanies: (expoId: number) =>
    ['client', 'participating-companies', expoId] as const,
  myExpoOpeningRequests: () => ['client', 'expo-opening-requests'] as const,
  adminExpoOpeningRequests: (status?: string) =>
    ['admin', 'expo-opening-requests', status ?? 'ALL'] as const,
}
