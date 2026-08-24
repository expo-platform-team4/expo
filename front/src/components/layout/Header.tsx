'use client'

import { LogOut, User } from 'lucide-react'
import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'

import { useLogout } from '@/features/auth/hooks'
import { profileRouteFor, useAuthStore } from '@/lib/auth'

import { GLOBAL_NAV_ITEMS } from './navItems'

/**
 * Public·Sidebar 두 셸이 공유하는 상단 헤더. Style.md 5-2 절 — "글로벌 내비(Public 과 동일)".
 * 로고 | 글로벌 내비 | 로그인 or 프로필. 셸마다 반복해서 만들지 않고 이 컴포넌트 하나를 쓴다.
 *
 * 프로필 영역은 클릭하면 바로 이동하지 않고, 마이페이지·로그아웃을 담은 드롭다운을 연다.
 */
export const Header = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  const role = useAuthStore((state) => state.role)
  const nickname = useAuthStore((state) => state.nickname)
  const logoutMutation = useLogout()

  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

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
          <div className="relative shrink-0" ref={menuRef}>
            <button
              type="button"
              onClick={() => setMenuOpen((open) => !open)}
              aria-expanded={menuOpen}
              aria-controls="header-profile-menu"
              className="text-label-md text-on-surface flex items-center gap-2 font-medium"
            >
              <User className="h-5 w-5" aria-hidden />
              {nickname ?? '내 정보'}
            </button>

            {menuOpen && (
              <div
                id="header-profile-menu"
                className="border-outline-variant bg-surface-container-lowest absolute top-full right-0 z-20 mt-2 w-40 rounded-lg border py-1 shadow-lg"
              >
                <Link
                  href={profileRouteFor(role)}
                  onClick={() => setMenuOpen(false)}
                  className="text-label-md text-on-surface hover:bg-surface-container block px-4 py-2.5"
                >
                  마이페이지
                </Link>
                <button
                  type="button"
                  onClick={() => {
                    setMenuOpen(false)
                    logoutMutation.mutate()
                  }}
                  className="text-label-md text-error hover:bg-error-container flex w-full items-center gap-2 px-4 py-2.5 text-left"
                >
                  <LogOut className="h-4 w-4" aria-hidden />
                  로그아웃
                </button>
              </div>
            )}
          </div>
        ) : (
          <Link href="/login" className="text-label-md text-secondary shrink-0 font-semibold">
            로그인
          </Link>
        )}
      </div>
    </header>
  )
}
