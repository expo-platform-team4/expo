import type { ReactNode } from 'react'

import { Header } from './Header'

/** 공개 화면 셸. Style.md 5-1 절 — 헤더만. 홈·목록·상세·로그인·가입·예매 화면이 쓴다. */
export const PublicShell = ({ children }: { children: ReactNode }) => (
  <div className="bg-surface min-h-screen">
    <Header />
    <main className="mx-auto max-w-[1280px] px-4 py-8">{children}</main>
  </div>
)
