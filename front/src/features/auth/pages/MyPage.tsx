'use client'

import { Receipt, Ticket } from 'lucide-react'
import Link from 'next/link'

import { Card, CardTitle, PageHeader } from '@/components/ui'

import { useMyProfile } from '../hooks'

/** `/mypage`. 마이페이지 진입점 — 요약 + 예매 내역·나의 티켓 바로가기. */
const MyPage = () => {
  const { data: profile } = useMyProfile()

  return (
    <div>
      <PageHeader
        title="마이페이지"
        description={profile ? `${profile.nickname}님, 환영합니다.` : undefined}
      />
      <div className="grid gap-4 sm:grid-cols-2">
        <Link href="/mypage/orders">
          <Card className="hover:border-secondary flex items-center gap-4 border border-transparent transition-colors">
            <Receipt className="text-secondary h-8 w-8" aria-hidden />
            <div>
              <CardTitle className="mb-0">예매 내역</CardTitle>
              <p className="text-body-md text-on-surface-variant">주문·결제 내역을 확인합니다</p>
            </div>
          </Card>
        </Link>
        <Link href="/mypage/tickets">
          <Card className="hover:border-secondary flex items-center gap-4 border border-transparent transition-colors">
            <Ticket className="text-secondary h-8 w-8" aria-hidden />
            <div>
              <CardTitle className="mb-0">나의 티켓</CardTitle>
              <p className="text-body-md text-on-surface-variant">발권된 티켓과 QR 을 확인합니다</p>
            </div>
          </Card>
        </Link>
      </div>
    </div>
  )
}

export default MyPage
