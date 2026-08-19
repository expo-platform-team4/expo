import Link from 'next/link'

import { Card, CardTitle } from '@/components/ui'

/** `/signup`. Function.md 2절 — "일반/기업 분기만. API 호출 없음". */
const SignupTypeSelectPage = () => (
  <div className="flex min-h-[70vh] items-center justify-center">
    <div className="grid w-full max-w-2xl gap-6 sm:grid-cols-2">
      <Link href="/signup/member">
        <Card className="hover:border-secondary flex h-full flex-col gap-2 border border-transparent transition-colors">
          <CardTitle>일반 회원</CardTitle>
          <p className="text-body-md text-on-surface-variant">박람회 티켓을 예매하고 싶어요</p>
        </Card>
      </Link>
      <Link href="/signup/client">
        <Card className="hover:border-secondary flex h-full flex-col gap-2 border border-transparent transition-colors">
          <CardTitle>기업 회원</CardTitle>
          <p className="text-body-md text-on-surface-variant">
            박람회를 주최하거나 부스를 운영하고 싶어요
          </p>
        </Card>
      </Link>
    </div>
  </div>
)

export default SignupTypeSelectPage
