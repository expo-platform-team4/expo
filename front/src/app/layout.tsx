import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import Providers from './providers'

// 디자인 시스템이 Inter 하나만 쓴다 (front/Style.md 1절). Geist 는 보일러플레이트였다.
const inter = Inter({
  variable: '--font-inter',
  subsets: ['latin'],
})

export const metadata: Metadata = {
  title: 'expo | 박람회 예약·티켓 판매 플랫폼',
  description: '박람회 개최와 운영, 티켓 예매, 정산을 한곳에서.',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko" className={`${inter.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
