'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { useState } from 'react'

/**
 * 클라이언트 전역 프로바이더.
 *
 * QueryClient 를 모듈 최상단이 아니라 useState 안에서 만드는 이유:
 * 서버에서 렌더링될 때 모듈 스코프에 두면 모든 요청이 같은 캐시를 공유하게 되어
 * 다른 사용자의 데이터가 섞일 수 있다. 컴포넌트마다 새로 만들어야 안전하다.
 */
export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // 서버 컴포넌트에서 받아온 데이터를 마운트 직후 다시 불러오지 않게 한다.
            staleTime: 60 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  )

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}
