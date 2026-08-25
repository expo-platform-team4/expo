'use client'

import { useState } from 'react'

import { Button, Input, Select } from '@/components/ui'
import { uploadFile } from '@/features/file/api'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAddBoothContentFile,
  useAddBoothContentLink,
  useRemoveBoothContentFile,
  useRemoveBoothContentLink,
} from '../hooks'
import type {
  BoothContentFile,
  BoothContentFileType,
  BoothContentLink,
  ExternalLinkType,
} from '../api'

const FILE_TYPE_LABEL: Record<BoothContentFileType, string> = {
  GALLERY_IMAGE: '갤러리 이미지',
  PROMO_VIDEO: '홍보 영상',
  CATALOG: '카탈로그',
  LEAFLET: '리플렛',
  OTHER: '기타',
}

/** 폼에서 실제로 고를 수 있는 첨부 종류. `PROMO_VIDEO`·`OTHER` 는 허용되는 업로드 형식이 없어 뺐다. */
const SELECTABLE_FILE_TYPES: BoothContentFileType[] = ['GALLERY_IMAGE', 'CATALOG', 'LEAFLET']

const LINK_TYPE_LABEL: Record<ExternalLinkType, string> = {
  HOMEPAGE: '홈페이지',
  SOCIAL: 'SNS',
  RESERVATION: '예약',
  PRODUCT: '제품',
  OTHER: '기타',
}

/** 첨부 파일 목록 + 추가 폼. `BoothContentSection` 의 수정 가능한 상태(초안·보완 요청)에서만 렌더링된다. */
export const BoothContentFileManager = ({
  contentId,
  allocationId,
  files,
}: {
  contentId: number
  allocationId: number
  files: BoothContentFile[]
}) => {
  const [fileType, setFileType] = useState<BoothContentFileType>('GALLERY_IMAGE')
  const [title, setTitle] = useState('')
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const addMutation = useAddBoothContentFile()
  const removeMutation = useRemoveBoothContentFile()

  const handleFileSelected = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setError(null)
    setIsUploading(true)
    try {
      const uploaded = await uploadFile(
        file,
        fileType === 'GALLERY_IMAGE' ? 'BOOTH_IMAGE' : 'BOOTH_DOCUMENT'
      )
      addMutation.mutate(
        {
          contentId,
          allocationId,
          payload: { fileId: uploaded.fileId, fileType, title: title.trim() || undefined },
        },
        {
          onSuccess: () => setTitle(''),
          onError: (err) => setError(getErrorMessage(err)),
        }
      )
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <p className="text-label-md text-on-surface-variant font-medium">첨부 파일</p>
      {files.length > 0 && (
        <ul className="flex flex-col gap-1">
          {files.map((file) => (
            <li
              key={file.id}
              className="border-outline-variant flex items-center justify-between gap-2 rounded border px-3 py-2"
            >
              <span className="text-label-md text-on-surface">
                [{FILE_TYPE_LABEL[file.fileType]}] {file.title || `파일 #${file.fileId}`}
              </span>
              <button
                type="button"
                className="text-label-sm text-error"
                disabled={removeMutation.isPending}
                onClick={() =>
                  removeMutation.mutate(
                    { contentId, fileEntryId: file.id, allocationId },
                    { onError: (err) => setError(getErrorMessage(err)) }
                  )
                }
              >
                삭제
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex flex-wrap items-end gap-2">
        <Select
          label="종류"
          hint=" "
          value={fileType}
          onChange={(e) => setFileType(e.target.value as BoothContentFileType)}
          className="w-40"
        >
          {SELECTABLE_FILE_TYPES.map((type) => (
            <option key={type} value={type}>
              {FILE_TYPE_LABEL[type]}
            </option>
          ))}
        </Select>
        <Input
          label="제목"
          hint="선택 항목입니다."
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-48"
        />
        <div className="flex flex-col gap-1.5">
          <span className="text-label-md invisible font-medium">파일</span>
          <label>
            <span className="border-outline text-label-md hover:bg-surface-container-low inline-flex h-11 cursor-pointer items-center rounded border px-3">
              {isUploading ? '업로드 중...' : '파일 추가'}
            </span>
            <input
              type="file"
              accept={
                fileType === 'GALLERY_IMAGE' ? 'image/jpeg,image/png,image/webp' : 'application/pdf'
              }
              className="hidden"
              disabled={isUploading}
              onChange={handleFileSelected}
            />
          </label>
          <p className="text-label-sm invisible"> </p>
        </div>
      </div>
      {error && <p className="text-label-sm text-error">{error}</p>}
    </div>
  )
}

/** 외부 링크 목록 + 추가 폼. `BoothContentSection` 의 수정 가능한 상태에서만 렌더링된다. */
export const BoothContentLinkManager = ({
  contentId,
  allocationId,
  links,
}: {
  contentId: number
  allocationId: number
  links: BoothContentLink[]
}) => {
  const [linkType, setLinkType] = useState<ExternalLinkType>('HOMEPAGE')
  const [label, setLabel] = useState('')
  const [url, setUrl] = useState('')
  const [error, setError] = useState<string | null>(null)

  const addMutation = useAddBoothContentLink()
  const removeMutation = useRemoveBoothContentLink()

  const handleAdd = () => {
    if (!url.trim()) {
      setError('URL을 입력하세요.')
      return
    }
    setError(null)
    addMutation.mutate(
      {
        contentId,
        allocationId,
        payload: { linkType, label: label.trim() || undefined, url: url.trim() },
      },
      {
        onSuccess: () => {
          setLabel('')
          setUrl('')
        },
        onError: (err) => setError(getErrorMessage(err)),
      }
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <p className="text-label-md text-on-surface-variant font-medium">외부 링크</p>
      {links.length > 0 && (
        <ul className="flex flex-col gap-1">
          {links.map((link) => (
            <li
              key={link.id}
              className="border-outline-variant flex items-center justify-between gap-2 rounded border px-3 py-2"
            >
              <span className="text-label-md text-on-surface truncate">
                [{LINK_TYPE_LABEL[link.linkType]}] {link.label || link.url}
              </span>
              <button
                type="button"
                className="text-label-sm text-error shrink-0"
                disabled={removeMutation.isPending}
                onClick={() =>
                  removeMutation.mutate(
                    { contentId, linkId: link.id, allocationId },
                    { onError: (err) => setError(getErrorMessage(err)) }
                  )
                }
              >
                삭제
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex flex-wrap items-end gap-2">
        <Select
          label="종류"
          hint=" "
          value={linkType}
          onChange={(e) => setLinkType(e.target.value as ExternalLinkType)}
          className="w-32"
        >
          {(Object.keys(LINK_TYPE_LABEL) as ExternalLinkType[]).map((type) => (
            <option key={type} value={type}>
              {LINK_TYPE_LABEL[type]}
            </option>
          ))}
        </Select>
        <Input
          label="표시 라벨"
          hint="선택 항목입니다."
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          className="w-40"
        />
        <Input
          label="URL"
          hint=" "
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          className="w-64"
        />
        <div className="flex flex-col gap-1.5">
          <span className="text-label-md invisible font-medium">추가</span>
          <Button type="button" loading={addMutation.isPending} onClick={handleAdd}>
            추가
          </Button>
          <p className="text-label-sm invisible"> </p>
        </div>
      </div>
      {error && <p className="text-label-sm text-error">{error}</p>}
    </div>
  )
}
