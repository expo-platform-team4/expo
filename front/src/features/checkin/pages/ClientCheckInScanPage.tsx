'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import jsQR from 'jsqr'
import { useSearchParams } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'

import { Badge, Button, Card, EmptyState, Input, PageHeader } from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import type { CheckInResult } from '../api'
import { ExpoSwitcher } from '../components/ExpoSwitcher'
import { useCheckInByCode, useCheckInByQr } from '../hooks'
import { manualCheckInSchema, type ManualCheckInFormValues } from '../schemas'

const parseExpoId = (raw: string | null): number | null => {
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

type Mode = 'camera' | 'manual'

/**
 * `/client/check-in/scan` — QR 스캔. Function.md 5절, 디자인 없이 자유 생성한 화면.
 *
 * "카메라로 QR 스캔 + 코드 수동 입력 폼을 같은 화면에서 전환 가능하게" — 탭으로 두 모드를
 * 오간다. 카메라가 막힌 환경(HTTP 배포 등)을 대비해 **수동 입력은 항상** 탭으로 노출한다.
 * 카메라 실패 시에만 나타나는 열등한 대체가 아니다(작업 지시서 원칙).
 *
 * Spec.md 8절 — 이 화면은 **폴링하지 않는다.** 스캔 성공 시 `useCheckInByQr`/`useCheckInByCode`
 * 가 현황 쿼리를 무효화하는 것으로 충분하다.
 */
const ClientCheckInScanPage = () => {
  const expoId = parseExpoId(useSearchParams().get('expoId'))
  const [mode, setMode] = useState<Mode>('camera')

  return (
    <div>
      <PageHeader
        title="QR 스캔"
        description="관람객의 QR 을 스캔하거나 티켓 코드를 입력해 입장 처리합니다."
      />

      <div className="mb-6">
        <ExpoSwitcher expoId={expoId} basePath="/client/check-in/scan" />
      </div>

      {expoId === null ? (
        <EmptyState
          title="박람회를 선택해 주세요"
          description="위에서 스캔을 진행할 박람회를 골라 주세요."
        />
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex gap-2">
            <Button
              type="button"
              variant={mode === 'camera' ? 'primary' : 'secondary'}
              onClick={() => setMode('camera')}
            >
              카메라로 스캔
            </Button>
            <Button
              type="button"
              variant={mode === 'manual' ? 'primary' : 'secondary'}
              onClick={() => setMode('manual')}
            >
              코드 직접 입력
            </Button>
          </div>

          {mode === 'camera' ? (
            <CameraScanPanel expoId={expoId} />
          ) : (
            <ManualCodePanel expoId={expoId} />
          )}
        </div>
      )}
    </div>
  )
}

/** 판정별 배지 색·문구. 백엔드가 이미 사람이 읽을 문구(`message`)를 주지만, 결과 라벨은 화면에서 붙인다. */
const RESULT_BADGE: Record<CheckInResult['result'], { label: string; variant: BadgeVariant }> = {
  SUCCESS: { label: '입장 완료', variant: 'success' },
  ALREADY_USED: { label: '이미 사용된 티켓', variant: 'error' },
  CANCELED_TICKET: { label: '취소된 티켓', variant: 'error' },
  WRONG_EXPO: { label: '다른 박람회 티켓', variant: 'error' },
  INVALID_TOKEN: { label: '인식할 수 없는 코드', variant: 'error' },
}

/** 스캔·수동 입력 공통 결과 배너. 거절도 200 으로 온다 — `admitted` 로 통과 여부를 가른다. */
const CheckInResultBanner = ({ result }: { result: CheckInResult }) => {
  const badge = RESULT_BADGE[result.result]
  return (
    <Card className={result.admitted ? undefined : 'border-error border'}>
      <div className="flex items-center gap-2">
        <Badge variant={badge.variant}>{badge.label}</Badge>
        {result.ticketCode && (
          <span className="text-body-md text-on-surface font-mono">{result.ticketCode}</span>
        )}
      </div>
      <p className="text-body-md text-on-surface mt-3">{result.message}</p>
    </Card>
  )
}

/**
 * 카메라 스캔 패널.
 *
 * `getUserMedia` 로 받은 스트림을 `<video>` 에 붙이고, 매 프레임을 숨긴 `<canvas>` 에 그려
 * `jsQR` 로 디코드한다. 한 번 찾으면 `pausedRef` 를 세워 같은 프레임을 반복 처리하지 않고,
 * 뮤테이션이 끝나 결과를 확인("다시 스캔")하면 다시 연다 — 카메라 스트림 자체는 그대로
 * 유지해서 매번 권한을 다시 묻지 않는다.
 */
const CameraScanPanel = ({ expoId }: { expoId: number }) => {
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const pausedRef = useRef(false)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const [result, setResult] = useState<CheckInResult | null>(null)
  const [mutationError, setMutationError] = useState<string | null>(null)

  const scanMutation = useCheckInByQr(expoId)

  useEffect(() => {
    let stream: MediaStream | null = null
    let rafId = 0
    let stopped = false

    const decodeTick = () => {
      if (stopped) return
      if (!pausedRef.current) {
        const video = videoRef.current
        const canvas = canvasRef.current
        if (video && canvas && video.readyState === video.HAVE_ENOUGH_DATA) {
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight
          const context = canvas.getContext('2d', { willReadFrequently: true })
          if (context) {
            context.drawImage(video, 0, 0, canvas.width, canvas.height)
            const imageData = context.getImageData(0, 0, canvas.width, canvas.height)
            const code = jsQR(imageData.data, imageData.width, imageData.height)
            if (code?.data) {
              pausedRef.current = true
              setMutationError(null)
              scanMutation.mutate(code.data, {
                onSuccess: (data) => setResult(data),
                onError: (err) => {
                  setMutationError(getErrorMessage(err))
                  pausedRef.current = false
                },
              })
            }
          }
        }
      }
      rafId = requestAnimationFrame(decodeTick)
    }

    const start = async () => {
      if (!navigator.mediaDevices?.getUserMedia) {
        setCameraError('이 브라우저는 카메라를 지원하지 않습니다. 코드 직접 입력을 이용해 주세요.')
        return
      }
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
          audio: false,
        })
        if (stopped) {
          stream.getTracks().forEach((track) => track.stop())
          return
        }
        if (videoRef.current) {
          videoRef.current.srcObject = stream
          await videoRef.current.play()
        }
        setCameraError(null)
        decodeTick()
      } catch {
        setCameraError(
          '카메라를 사용할 수 없습니다. 권한을 확인하거나 코드 직접 입력을 이용해 주세요.'
        )
      }
    }

    start()

    return () => {
      stopped = true
      cancelAnimationFrame(rafId)
      stream?.getTracks().forEach((track) => track.stop())
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- expoId 가 바뀌면 스캔 대상이 바뀌므로 스트림을 새로 연다
  }, [expoId])

  const resumeScanning = () => {
    setResult(null)
    setMutationError(null)
    pausedRef.current = false
  }

  return (
    <Card>
      <div className="bg-on-background/90 relative aspect-video w-full overflow-hidden rounded-md">
        <video ref={videoRef} className="h-full w-full object-cover" playsInline muted />
        <canvas ref={canvasRef} className="hidden" />
        {cameraError && (
          <div className="absolute inset-0 flex items-center justify-center p-6 text-center">
            <p className="text-body-md text-on-primary">{cameraError}</p>
          </div>
        )}
      </div>

      <p className="text-label-sm text-on-surface-variant mt-3 text-center">
        QR 을 화면 안에 맞춰 주세요. 인식되면 자동으로 처리합니다.
      </p>

      {mutationError && <p className="text-label-sm text-error mt-3">{mutationError}</p>}

      {result && (
        <div className="mt-4 flex flex-col gap-3">
          <CheckInResultBanner result={result} />
          <Button type="button" onClick={resumeScanning}>
            다시 스캔
          </Button>
        </div>
      )}
    </Card>
  )
}

/** 수동 코드 입력 패널. 카메라 실패와 무관하게 항상 탭으로 접근 가능하다. */
const ManualCodePanel = ({ expoId }: { expoId: number }) => {
  const [result, setResult] = useState<CheckInResult | null>(null)
  const [mutationError, setMutationError] = useState<string | null>(null)
  const manualMutation = useCheckInByCode(expoId)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ManualCheckInFormValues>({
    resolver: zodResolver(manualCheckInSchema),
    defaultValues: { ticketCode: '' },
  })

  const onSubmit = (values: ManualCheckInFormValues) => {
    setMutationError(null)
    manualMutation.mutate(values.ticketCode, {
      onSuccess: (data) => {
        setResult(data)
        reset()
      },
      onError: (err) => setMutationError(getErrorMessage(err)),
    })
  }

  return (
    <Card>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Input
          label="티켓 코드"
          placeholder="EXPO-20260810-000004"
          error={errors.ticketCode?.message}
          {...register('ticketCode')}
        />
        {mutationError && <p className="text-label-sm text-error">{mutationError}</p>}
        <Button type="submit" loading={manualMutation.isPending}>
          입장 처리
        </Button>
      </form>

      {result && (
        <div className="mt-4">
          <CheckInResultBanner result={result} />
        </div>
      )}
    </Card>
  )
}

export default ClientCheckInScanPage
