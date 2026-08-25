import { useQuery } from '@tanstack/react-query'

import { fetchParticipatingCompanies } from './participatingCompanyApi'
import { clientKeys } from './queryKeys'

/** 내 박람회에 참여 신청한 기업 목록. `/client/expos/{expoId}/participating-companies` 화면이 쓴다. */
export const useParticipatingCompanies = (expoId: number | null) =>
  useQuery({
    queryKey: clientKeys.participatingCompanies(expoId ?? 0),
    queryFn: () => fetchParticipatingCompanies(expoId as number),
    enabled: expoId !== null,
  })
