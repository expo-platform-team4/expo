import type { NextConfig } from 'next'

/**
 * 백엔드(Spring) 오리진.
 *
 * 로컬에서는 localhost:8080 이지만, Docker 로 띄우면 localhost 는 Next 컨테이너 자기 자신이라
 * Spring 에 닿지 않는다. 그래서 반드시 환경변수로 받는다 (예: http://backend:8080).
 */
const BACKEND_ORIGIN = process.env.BACKEND_ORIGIN ?? 'http://localhost:8080'

const nextConfig: NextConfig = {
  // Docker 이미지를 최소 크기로 만들기 위한 설정.
  // 빌드하면 .next/standalone 에 필요한 node_modules 까지 포함된 실행 묶음이 생긴다.
  output: 'standalone',

  async rewrites() {
    return [
      // 브라우저가 /api/* 로 요청하면 Next 서버가 Spring 으로 넘긴다.
      // 브라우저 입장에서는 same-origin 이라 CORS 설정이 필요 없다.
      //
      // destination 에도 /api 가 있어야 한다. 스프링 컨트롤러가 전부 /api/... 로 매핑돼 있고
      // context-path 설정도 없어서, 접두어를 떼고 넘기면 전부 404 가 된다.
      {
        source: '/api/:slug*',
        destination: `${BACKEND_ORIGIN}/api/:slug*`,
      },
      // OAuth2 로그인은 브라우저가 백엔드로 직접 리다이렉트되어야 하므로
      // 스프링이 쓰는 경로를 그대로 넘긴다.
      {
        source: '/oauth2/:slug*',
        destination: `${BACKEND_ORIGIN}/oauth2/:slug*`,
      },
      {
        source: '/login/oauth2/:slug*',
        destination: `${BACKEND_ORIGIN}/login/oauth2/:slug*`,
      },
    ]
  },
}

export default nextConfig
