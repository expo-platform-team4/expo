import { useQuery } from '@tanstack/react-query'

import { fetchActiveBanners } from './api'
import { bannerKeys } from './queryKeys'

/**
 * 메인 배너 조회.
 *
 * 배너는 자주 바뀌지 않는데(승인·기간 단위로 움직인다) 메인 홈에서 매번 부른다.
 * `staleTime` 을 길게 잡아 페이지를 오갈 때마다 다시 부르지 않게 한다.
 *
 * 실패해도 재시도하지 않는다 — 배너는 <b>보조 요소</b>라, 안 보이더라도 홈은 그대로
 * 쓸 수 있어야 한다. 로딩을 길게 끌지 않는 편이 낫다.
 */
export const useActiveBanners = () =>
  useQuery({
    queryKey: bannerKeys.active(),
    queryFn: fetchActiveBanners,
    staleTime: 5 * 60 * 1000,
    retry: false,
  })
