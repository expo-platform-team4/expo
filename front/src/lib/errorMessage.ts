import { isAxiosError } from 'axios'

/**
 * 백엔드 `{ success:false, message }` 의 message 는 이미 사용자에게 보여줄 수 있는
 * 한국어 문장이다(`ApiResponse` 설계, Spec.md 7절 — "서버가 최종 권위다"). 그대로 쓰고
 * 프론트에서 다시 번역하지 않는다.
 *
 * `ErrorState` 뿐 아니라 폼 인라인 에러(회원가입 409, 휴대폰 인증 실패 등)에서도 같은
 * 규칙이 적용되므로 한 곳에 모아 재사용한다.
 */
export const getErrorMessage = (
  error: unknown,
  fallback = '잠시 후 다시 시도해 주세요.'
): string => {
  if (isAxiosError(error)) {
    const serverMessage = (error.response?.data as { message?: string } | undefined)?.message
    if (serverMessage) return serverMessage
  }
  return fallback
}
