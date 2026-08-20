import { api } from '@/lib/api'

import type { ExpoFile, ExpoFilePurpose, ExpoImage, ExpoImageType } from '@/features/expo/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/**
 * 주최사의 박람회 이미지·자료 관리.
 *
 * 응답 타입은 공개 상세와 같은 것을 쓴다(`features/expo/api`) — 같은 백엔드 DTO 라
 * 따로 만들면 한쪽만 고쳐지는 순간 어긋난다.
 *
 * **파일 업로드는 여기가 아니다.** `features/file` 의 `uploadFile` 로 먼저 올려
 * `fileId` 를 받고, 그 ID 를 아래 함수에 넘긴다.
 */

export const fetchExpoImages = async (expoId: number): Promise<ExpoImage[]> => {
  const { data } = await api.get<ApiEnvelope<ExpoImage[]>>(`/client/expos/${expoId}/images`)
  return data.data
}

export const fetchExpoFiles = async (expoId: number): Promise<ExpoFile[]> => {
  const { data } = await api.get<ApiEnvelope<ExpoFile[]>>(`/client/expos/${expoId}/files`)
  return data.data
}

export type AttachExpoImagePayload = {
  fileId: number
  imageType: ExpoImageType
  altText?: string
}

/** 대표 이미지를 새로 지정하면 백엔드가 기존 대표를 상세 이미지로 내린다(지우지 않는다). */
export const attachExpoImage = async (
  expoId: number,
  payload: AttachExpoImagePayload
): Promise<ExpoImage> => {
  const { data } = await api.post<ApiEnvelope<ExpoImage>>(`/client/expos/${expoId}/images`, payload)
  return data.data
}

export const detachExpoImage = async (expoId: number, imageId: number): Promise<void> => {
  await api.delete(`/client/expos/${expoId}/images/${imageId}`)
}

export type AttachExpoFilePayload = {
  fileId: number
  filePurpose: ExpoFilePurpose
  title?: string
}

export const attachExpoFile = async (
  expoId: number,
  payload: AttachExpoFilePayload
): Promise<ExpoFile> => {
  const { data } = await api.post<ApiEnvelope<ExpoFile>>(`/client/expos/${expoId}/files`, payload)
  return data.data
}

export const detachExpoFile = async (expoId: number, expoFileId: number): Promise<void> => {
  await api.delete(`/client/expos/${expoId}/files/${expoFileId}`)
}
