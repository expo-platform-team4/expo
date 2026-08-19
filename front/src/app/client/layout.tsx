import RequireAuth from '@/components/auth/RequireAuth'
import { ClientSidebarShell } from '@/components/layout/ClientSidebarShell'

const ClientLayout = ({ children }: { children: React.ReactNode }) => (
  <RequireAuth roles={['CLIENT', 'ADMIN']}>
    <ClientSidebarShell>{children}</ClientSidebarShell>
  </RequireAuth>
)

export default ClientLayout
