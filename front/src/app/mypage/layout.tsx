import RequireAuth from '@/components/auth/RequireAuth'

const MyPageLayout = ({ children }: { children: React.ReactNode }) => (
  <RequireAuth>{children}</RequireAuth>
)

export default MyPageLayout
