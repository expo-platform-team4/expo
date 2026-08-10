import Link from 'next/link'

const links = [
  '/auth/callback',
  '/client',
  '/client/banners',
  '/client/banners/new',
  '/client/booths',
  '/client/booths/1',
  '/client/booths/1/content',
  '/client/check-in',
  '/client/dashboard',
  '/client/expos',
  '/client/expos/1',
  '/client/participations',
  '/client/recruitment-notice-requests',
  '/client/recruitment-notice-requests/1',
  '/client/recruitment-notice-requests/new',
  '/client/settlements',
  '/expos',
  '/expos/1',
  '/login',
  '/mypage',
  '/mypage/orders',
  '/mypage/orders/1',
  '/mypage/tickets',
  '/mypage/tickets/1',
  '/mypage/tickets/1/qr',
  '/orders',
  '/orders/1',
  '/orders/guest',
  '/recruitment-notices',
  '/recruitment-notices/1',
]

const HomePage = () => {
  return (
    <div>
      <div>홈</div>
      <ul>
        {links.map((href) => (
          <li key={href}>
            <Link href={href}>{href}</Link>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default HomePage
