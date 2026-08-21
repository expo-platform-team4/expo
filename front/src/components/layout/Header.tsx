'use client'

import { User } from 'lucide-react'
import Link from 'next/link'

import { profileRouteFor, useAuthStore } from '@/lib/auth'

import { GLOBAL_NAV_ITEMS } from './navItems'

/**
 * Public·Sidebar 두 셸이 공유하는 상단 헤더. Style.md 5-2 절 — "글로벌 내비(Public 과 동일)".
 * 로고 | 글로벌 내비 | 로그인 or 프로필. 셸마다 반복해서 만들지 않고 이 컴포넌트 하나를 쓴다.
 */
export const Header = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  const role = useAuthStore((state) => state.role)
  const nickname = useAuthStore((state) => state.nickname)

  return (
    <header className="border-outline-variant bg-surface-container-lowest sticky top-0 z-10 border-b">
      <div className="mx-auto flex h-16 max-w-[1280px] items-center justify-between gap-6 px-4">
        <Link href="/" className="text-title-lg text-primary shrink-0 font-bold">
          엑스포티켓
        </Link>

        <nav className="hidden items-center gap-6 md:flex">
          {GLOBAL_NAV_ITEMS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="text-label-md text-on-surface-variant hover:text-on-surface transition-colors"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        {accessToken && role ? (
          <Link
            href={profileRouteFor(role)}
            className="text-label-md text-on-surface flex shrink-0 items-center gap-2 font-medium"
          >
            <User className="h-5 w-5" aria-hidden />
            {nickname ?? '내 정보'}
          </Link>
        ) : (
          <Link href="/login" className="text-label-md text-secondary shrink-0 font-semibold">
            로그인
          </Link>
        )}
      </div>
    </header>
  )
}
