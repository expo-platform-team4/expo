import { useMutation } from '@tanstack/react-query'

import { type FilePurpose, uploadFile } from './api'

/**
 * 파일 업로드.
 *
 * 업로드만 하고 어디에도 연결하지 않는다 — 연결은 그 파일을 쓰는 화면이 받은
 * `fileId` 로 따로 호출한다(예: 박람회 이미지 연결). 두 단계로 나눈 이유는 같은
 * 파일을 여러 곳에 붙일 수 있어야 하고, 업로드가 느린 동안 연결까지 묶이면 안 되기
 * 때문이다.
 *
 * 캐시를 건드리지 않으므로 무효화는 호출하는 쪽이 자기 목록에 대해 한다.
 */
export const useUploadFile = () =>
  useMutation({
    mutationFn: ({ file, purpose }: { file: File; purpose: FilePurpose }) =>
      uploadFile(file, purpose),
  })
