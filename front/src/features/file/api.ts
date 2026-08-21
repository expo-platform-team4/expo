import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/**
 * 업로드 용도. 백엔드 `FilePurpose` 와 같은 값이어야 한다.
 *
 * 용도가 **허용 형식·최대 크기·공개 범위**를 한꺼번에 정한다. 예를 들어 프로필 이미지는
 * 5MB·이미지만이고, 박람회 자료는 20MB·PDF 다. 잘못 고르면 업로드가 400 으로 거절된다.
 */
export type FilePurpose =
  | 'PROFILE_IMAGE'
  | 'EXPO_IMAGE'
  | 'EXPO_DOCUMENT'
  | 'BOOTH_IMAGE'
  | 'BOOTH_DOCUMENT'
  | 'BANNER_IMAGE'
  | 'VENUE_LAYOUT'
  | 'SETTLEMENT_REPORT'

/** 업로드된 파일 한 건. 백엔드 `FileMetadataResponse` 와 짝이다. */
export type UploadedFile = {
  fileId: number
  originalFilename: string
  contentType: string
  fileSize: number
  accessLevel: 'PUBLIC' | 'PRIVATE'
  /** `<img src>` 나 링크에 그대로 넣는 경로. 프록시를 타므로 호스트가 없다. */
  downloadUrl: string
}

/**
 * 파일을 올린다. `POST /api/files?purpose=...` (multipart).
 *
 * **브라우저가 저장소로 직접 붙지 않는다.** 백엔드가 바이트를 중계한다 —
 * `docs/s3-presigned-url.md` 참고.
 *
 * `Content-Type` 을 `null` 로 지우는 것이 핵심이다. `lib/api.ts` 의 axios 인스턴스가
 * 기본값으로 `application/json` 을 박아 두는데, 그대로 두면 FormData 를 보내면서 헤더만
 * JSON 이라고 말하게 되어 백엔드가 415 `Content-Type 'application/json' is not supported`
 * 를 낸다. 헤더를 비우면 브라우저가 `multipart/form-data` 와 경계 문자열을 스스로 붙인다.
 *
 * 타임아웃도 늘린다. 인스턴스 기본값 10초는 20MB 짜리 자료에 모자란다.
 */
export const uploadFile = async (file: File, purpose: FilePurpose): Promise<UploadedFile> => {
  const form = new FormData()
  form.append('file', file)

  const { data } = await api.post<ApiEnvelope<UploadedFile>>('/files', form, {
    params: { purpose },
    headers: { 'Content-Type': null },
    timeout: 60_000,
  })
  return data.data
}
