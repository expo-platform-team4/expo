import axios, { AxiosError } from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'

import { useAuthStore } from './auth'

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

/** 401 재시도 여부를 요청 config 에 얹어 두기 위한 타입. 무한 재시도를 막는 표식이다. */
type RetriableRequestConfig = InternalAxiosRequestConfig & { _retried?: boolean }

/**
 * 액세스 토큰을 붙인다. Spec.md 3-3절.
 *
 * 서버 컴포넌트에는 Zustand persist 가 없다(localStorage 가 없다) — 이 인터셉터는
 * 브라우저에서만 의미가 있고, 서버에서는 그냥 토큰 없이 나간다. 서버에서 인증이 필요한
 * 호출은 아직 없다(전부 클라이언트 컴포넌트에서 일어난다).
 */
api.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

/**
 * 401 을 받으면 재발급을 한 번 시도하고, 원래 요청을 한 번만 재시도한다.
 *
 * `reissueApi` 를 따로 둔 이유 — 이 요청 자체가 `api` 인터셉터를 타면, reissue 가 또 401 을
 * 받았을 때 재귀적으로 재발급을 시도하다 무한 루프에 빠질 수 있다. 재발급 호출은
 * 인터셉터가 없는 별도 인스턴스로 보낸다.
 */
const reissueApi = axios.create({ baseURL, timeout: 10_000 })

let refreshingPromise: Promise<string> | null = null

const reissueAccessToken = async (): Promise<string> => {
  const { refreshToken } = useAuthStore.getState()
  if (!refreshToken) throw new Error('no refresh token')

  const { data } = await reissueApi.post<{ data: { accessToken: string } }>('/auth/reissue', {
    refreshToken,
  })
  const newAccessToken = data.data.accessToken
  useAuthStore.getState().setAccessToken(newAccessToken)
  return newAccessToken
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined
    if (error.response?.status !== 401 || !originalRequest || originalRequest._retried) {
      return Promise.reject(error)
    }
    originalRequest._retried = true

    try {
      // 동시에 여러 요청이 401 을 받아도 재발급은 한 번만 나가게 한다.
      refreshingPromise ??= reissueAccessToken().finally(() => {
        refreshingPromise = null
      })
      const newAccessToken = await refreshingPromise

      originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`)
      return api(originalRequest)
    } catch {
      useAuthStore.getState().clear()
      if (typeof window !== 'undefined') {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
  }
)
