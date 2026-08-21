/* eslint-disable @next/next/no-img-element -- 이미지 경로가 /api/files/{id}/content 라
   Next 이미지 최적화 서버를 한 번 더 태울 이유가 없다. 저장소의 다른 이미지(SidebarShell 의
   아바타)도 같은 이유로 <img> 를 쓴다. */
'use client'

import { useParams, useRouter } from 'next/navigation'
import { useRef, useState } from 'react'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Select,
} from '@/components/ui'
import {
  EXPO_FILE_PURPOSE_LABEL,
  EXPO_IMAGE_TYPE_LABEL,
  type ExpoFilePurpose,
  type ExpoImageType,
} from '@/features/expo/api'
import { useUploadFile } from '@/features/file/hooks'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAttachExpoFile,
  useAttachExpoImage,
  useDetachExpoFile,
  useDetachExpoImage,
  useExpoFiles,
  useExpoImages,
} from '../expoContentHooks'

/** 브라우저 파일 선택 대화상자에서 미리 걸러 준다. 실제 검증은 백엔드가 한다. */
const IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'
const DOCUMENT_ACCEPT = 'application/pdf'

/**
 * `/client/expos/{expoId}/content` — 주최사가 자기 박람회의 이미지·자료를 올린다.
 *
 * **업로드와 연결은 두 번의 호출이다.** 먼저 `POST /api/files` 로 파일을 올려 `fileId`
 * 를 받고, 그 ID 를 `POST /api/client/expos/{expoId}/images|files` 로 연결한다. 화면에서는
 * 파일을 고르는 한 번의 동작으로 보이도록 두 호출을 이어 붙인다.
 *
 * 대표 이미지는 하나뿐이다. 새로 지정하면 백엔드가 기존 대표를 **상세 이미지로 내린다** —
 * 지우지 않으므로 올려 둔 사진이 사라지지 않는다.
 */
const ClientExpoContentPage = () => {
  const params = useParams<{ expoId: string }>()
  const router = useRouter()
  const parsedExpoId = Number(params.expoId)
  const expoId = Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="박람회 자료 관리" />
        <EmptyState title="잘못된 접근입니다" description="박람회 주소가 올바르지 않습니다." />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="박람회 이미지·자료 관리"
        description="여기서 올린 대표 이미지가 목록·상세 화면에 그대로 나갑니다."
        action={
          <Button variant="secondary" onClick={() => router.push(`/expos/${expoId}`)}>
            공개 화면 보기
          </Button>
        }
      />
      <ImageSection expoId={expoId} />
      <FileSection expoId={expoId} />
    </div>
  )
}

const ImageSection = ({ expoId }: { expoId: number }) => {
  const [imageType, setImageType] = useState<ExpoImageType>('THUMBNAIL')
  const [altText, setAltText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const { data, isPending, isError, error: loadError, refetch } = useExpoImages(expoId)
  const uploadMutation = useUploadFile()
  const attachMutation = useAttachExpoImage(expoId)
  const detachMutation = useDetachExpoImage(expoId)

  const busy = uploadMutation.isPending || attachMutation.isPending

  const handleSelect = async (file: File) => {
    setError(null)
    try {
      const uploaded = await uploadMutation.mutateAsync({ file, purpose: 'EXPO_IMAGE' })
      await attachMutation.mutateAsync({
        fileId: uploaded.fileId,
        imageType,
        altText: altText.trim() || undefined,
      })
      setAltText('')
    } catch (caught) {
      setError(getErrorMessage(caught))
    } finally {
      // 같은 파일을 다시 고를 수 있게 비운다. 안 비우면 change 이벤트가 안 뜬다.
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <Card className="flex flex-col gap-4">
      <CardTitle>이미지</CardTitle>

      <div className="flex flex-wrap items-end gap-3">
        <div className="w-40">
          <Select
            label="종류"
            value={imageType}
            onChange={(event) => setImageType(event.target.value as ExpoImageType)}
          >
            {(Object.keys(EXPO_IMAGE_TYPE_LABEL) as ExpoImageType[]).map((type) => (
              <option key={type} value={type}>
                {EXPO_IMAGE_TYPE_LABEL[type]}
              </option>
            ))}
          </Select>
        </div>
        <div className="min-w-56 flex-1">
          <Input
            label="대체 텍스트 (선택)"
            hint="이미지가 안 보일 때 대신 읽히는 설명입니다."
            value={altText}
            onChange={(event) => setAltText(event.target.value)}
          />
        </div>
        <Button type="button" loading={busy} onClick={() => inputRef.current?.click()}>
          이미지 올리기
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept={IMAGE_ACCEPT}
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) void handleSelect(file)
          }}
        />
      </div>

      <p className="text-label-sm text-on-surface-variant">
        JPG·PNG·WEBP, 10MB 까지. 대표 이미지는 하나만 유지되며, 새로 지정하면 이전 대표는 상세
        이미지로 내려갑니다.
      </p>

      {error && <p className="text-label-sm text-error">{error}</p>}

      {isError ? (
        <ErrorState error={loadError} onRetry={() => refetch()} />
      ) : isPending ? (
        <LoadingBlock label="이미지를 불러오는 중입니다" />
      ) : data.length === 0 ? (
        <p className="text-body-md text-on-surface-variant">아직 올린 이미지가 없습니다.</p>
      ) : (
        <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3">
          {data.map((image) => (
            <li key={image.id} className="flex flex-col gap-2">
              <div className="bg-surface-variant aspect-[3/2] overflow-hidden rounded">
                <img
                  src={image.url}
                  alt={image.altText ?? ''}
                  className="h-full w-full object-cover"
                />
              </div>
              <div className="flex items-center justify-between gap-2">
                <Badge variant={image.imageType === 'THUMBNAIL' ? 'success' : 'neutral'}>
                  {EXPO_IMAGE_TYPE_LABEL[image.imageType]}
                </Badge>
                <Button
                  size="sm"
                  variant="danger"
                  loading={detachMutation.isPending}
                  onClick={() => detachMutation.mutate(image.id)}
                >
                  삭제
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

const FileSection = ({ expoId }: { expoId: number }) => {
  const [filePurpose, setFilePurpose] = useState<ExpoFilePurpose>('LEAFLET')
  const [title, setTitle] = useState('')
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const { data, isPending, isError, error: loadError, refetch } = useExpoFiles(expoId)
  const uploadMutation = useUploadFile()
  const attachMutation = useAttachExpoFile(expoId)
  const detachMutation = useDetachExpoFile(expoId)

  const busy = uploadMutation.isPending || attachMutation.isPending

  const handleSelect = async (file: File) => {
    setError(null)
    try {
      const uploaded = await uploadMutation.mutateAsync({ file, purpose: 'EXPO_DOCUMENT' })
      await attachMutation.mutateAsync({
        fileId: uploaded.fileId,
        filePurpose,
        title: title.trim() || undefined,
      })
      setTitle('')
    } catch (caught) {
      setError(getErrorMessage(caught))
    } finally {
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <Card className="flex flex-col gap-4">
      <CardTitle>소개 자료·팜플렛</CardTitle>

      <div className="flex flex-wrap items-end gap-3">
        <div className="w-40">
          <Select
            label="종류"
            value={filePurpose}
            onChange={(event) => setFilePurpose(event.target.value as ExpoFilePurpose)}
          >
            {(Object.keys(EXPO_FILE_PURPOSE_LABEL) as ExpoFilePurpose[]).map((purpose) => (
              <option key={purpose} value={purpose}>
                {EXPO_FILE_PURPOSE_LABEL[purpose]}
              </option>
            ))}
          </Select>
        </div>
        <div className="min-w-56 flex-1">
          <Input
            label="제목 (선택)"
            hint="비우면 원본 파일명을 그대로 씁니다."
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </div>
        <Button type="button" loading={busy} onClick={() => inputRef.current?.click()}>
          자료 올리기
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept={DOCUMENT_ACCEPT}
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) void handleSelect(file)
          }}
        />
      </div>

      <p className="text-label-sm text-on-surface-variant">PDF, 20MB 까지.</p>

      {error && <p className="text-label-sm text-error">{error}</p>}

      {isError ? (
        <ErrorState error={loadError} onRetry={() => refetch()} />
      ) : isPending ? (
        <LoadingBlock label="자료를 불러오는 중입니다" />
      ) : data.length === 0 ? (
        <p className="text-body-md text-on-surface-variant">아직 올린 자료가 없습니다.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {data.map((file) => (
            <li key={file.id} className="flex flex-wrap items-center gap-2">
              <Badge variant="neutral">{EXPO_FILE_PURPOSE_LABEL[file.filePurpose]}</Badge>
              <a
                href={file.url}
                target="_blank"
                rel="noreferrer"
                className="text-secondary text-body-md min-w-0 flex-1 truncate font-medium underline"
              >
                {file.title ?? '자료'}
              </a>
              <Button
                size="sm"
                variant="danger"
                loading={detachMutation.isPending}
                onClick={() => detachMutation.mutate(file.id)}
              >
                삭제
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

export default ClientExpoContentPage
