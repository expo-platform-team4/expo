export const clientKeys = {
  dashboardProfile: ['client', 'dashboard-profile'] as const,
  myExpos: ['client', 'my-expos'] as const,
  myConfirmedBooths: ['client', 'my-confirmed-booths'] as const,
  myRecruitmentResults: ['client', 'my-recruitment-results'] as const,
  settlements: (page: number) => ['client', 'settlements', page] as const,
}
