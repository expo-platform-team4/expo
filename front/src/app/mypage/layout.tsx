import RequireAuth from '@/components/auth/RequireAuth'
import { MemberSidebarShell } from '@/components/layout/MemberSidebarShell'

const MyPageLayout = ({ children }: { children: React.ReactNode }) => (
  <RequireAuth roles={['MEMBER']}>
    <MemberSidebarShell>{children}</MemberSidebarShell>
  </RequireAuth>
)

export default MyPageLayout
