import axios from 'axios'

/**
 * 서버에서 실행 중인지 여부.
 *
 * 서버 컴포넌트·서버 액션에서는 상대 경로('/api/...')로 요청할 수 없다.
 * 브라우저처럼 "현재 오리진" 이라는 개념이 없기 때문에 절대 URL 이 필요하다.
 * 그래서 실행 위치에 따라 baseURL 을 다르게 잡는다.
 *
 *   브라우저 -> '/api'                    (next.config.ts 의 rewrites 가 Spring 으로 넘긴다)
 *   서버     -> 'http://localhost:8080'   (BACKEND_ORIGIN, Spring 을 직접 호출)
 */
const isServer = typeof window === 'undefined'

const baseURL = isServer
  ? (process.env.BACKEND_ORIGIN ?? 'http://localhost:8080')
  : (process.env.NEXT_PUBLIC_API_BASE_URL ?? '/api')

export const api = axios.create({
  baseURL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * 액세스 토큰을 붙이는 자리.
 *
 * 인증은 JWT(Access/Refresh)로 가기로 했다. 토큰 저장 위치와 401 재발급 처리는
 * auth 작업에서 정한 뒤 여기에 인터셉터로 붙인다.
 */
