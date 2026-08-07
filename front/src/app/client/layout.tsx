import RequireAuth from '@/components/auth/RequireAuth'

const ClientLayout = ({ children }: { children: React.ReactNode }) => (
  <RequireAuth>{children}</RequireAuth>
)

export default ClientLayout
