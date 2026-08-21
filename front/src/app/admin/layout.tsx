import RequireAuth from '@/components/auth/RequireAuth'
import { AdminShellWrapper } from '@/components/layout/AdminShellWrapper'

const AdminLayout = ({ children }: { children: React.ReactNode }) => (
  <RequireAuth roles={['ADMIN']}>
    <AdminShellWrapper>{children}</AdminShellWrapper>
  </RequireAuth>
)

export default AdminLayout
