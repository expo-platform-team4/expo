'use client'

import { EmptyState, PageHeader } from '@/components/ui'

/**
 * `/admin/expos`. Function.md 4절 — "박람회 개최 승인 관리".
 *
 * **백엔드 API 가 없다.** 박람회 개최 신청·승인을 처리하는 엔드포인트가 아직 구현되지
 * 않았다 — [이슈 #107](https://github.com/expo-platform-team4/expo/issues/107)에 이미
 * 기록돼 있다. Function.md 7절 원칙대로 화면은 만들되 "준비 중"임을 명시한다. 승인 UI를
 * 가짜로 만들지 않는다 — API 가 생기면 그때 실제 목록·승인/반려 액션으로 채운다.
 */
const AdminExpoApprovalsPage = () => (
  <div>
    <PageHeader
      title="박람회 개최 승인 관리"
      description="주최사의 박람회 개최 신청을 심사합니다."
    />
    <EmptyState
      title="준비 중입니다"
      notReady
      description="박람회 개최 신청·승인 API가 아직 없습니다 (이슈 #107). API가 추가되면 이 화면에 목록과 승인/반려 액션이 채워집니다."
    />
  </div>
)

export default AdminExpoApprovalsPage
