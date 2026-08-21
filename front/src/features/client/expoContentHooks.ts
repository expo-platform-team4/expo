import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { expoKeys } from '@/features/expo/queryKeys'

import {
  type AttachExpoFilePayload,
  type AttachExpoImagePayload,
  attachExpoFile,
  attachExpoImage,
  detachExpoFile,
  detachExpoImage,
  fetchExpoFiles,
  fetchExpoImages,
} from './expoContentApi'
import { clientKeys } from './queryKeys'

export const useExpoImages = (expoId: number | null) =>
  useQuery({
    queryKey: clientKeys.expoImages(expoId ?? 0),
    queryFn: () => fetchExpoImages(expoId as number),
    enabled: expoId !== null,
  })

export const useExpoFiles = (expoId: number | null) =>
  useQuery({
    queryKey: clientKeys.expoFiles(expoId ?? 0),
    queryFn: () => fetchExpoFiles(expoId as number),
    enabled: expoId !== null,
  })

/**
 * 관리 화면의 목록과 **공개 상세·목록**을 함께 무효화한다.
 *
 * 이미지를 바꾸면 공개 카드의 대표 이미지도 달라진다. 관리 화면만 새로 고치면
 * 주최사가 "바꿨는데 사이트엔 그대로" 를 보게 된다.
 */
const useInvalidateExpoContent = (expoId: number) => {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: clientKeys.expoImages(expoId) })
    queryClient.invalidateQueries({ queryKey: clientKeys.expoFiles(expoId) })
    queryClient.invalidateQueries({ queryKey: expoKeys.detail(expoId) })
    queryClient.invalidateQueries({ queryKey: [...expoKeys.all, 'cards'] })
  }
}

export const useAttachExpoImage = (expoId: number) => {
  const invalidate = useInvalidateExpoContent(expoId)
  return useMutation({
    mutationFn: (payload: AttachExpoImagePayload) => attachExpoImage(expoId, payload),
    onSuccess: invalidate,
  })
}

export const useDetachExpoImage = (expoId: number) => {
  const invalidate = useInvalidateExpoContent(expoId)
  return useMutation({
    mutationFn: (imageId: number) => detachExpoImage(expoId, imageId),
    onSuccess: invalidate,
  })
}

export const useAttachExpoFile = (expoId: number) => {
  const invalidate = useInvalidateExpoContent(expoId)
  return useMutation({
    mutationFn: (payload: AttachExpoFilePayload) => attachExpoFile(expoId, payload),
    onSuccess: invalidate,
  })
}

export const useDetachExpoFile = (expoId: number) => {
  const invalidate = useInvalidateExpoContent(expoId)
  return useMutation({
    mutationFn: (expoFileId: number) => detachExpoFile(expoId, expoFileId),
    onSuccess: invalidate,
  })
}
