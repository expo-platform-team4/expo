'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { UserRound } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { useForm } from 'react-hook-form'

import { Badge, Button, Card, CardTitle, ErrorState, Input, LoadingBlock } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { uploadFile } from '../../file/api'
import {
  useChangeNickname,
  useChangePassword,
  useChangeProfileImage,
  useMyProfile,
  useNicknameAvailability,
  useRemoveProfileImage,
  useWithdraw,
} from '../hooks'
import { useAvailabilityHint } from '../useAvailabilityHint'
import {
  changePasswordSchema,
  nicknameChangeSchema,
  withdrawalSchema,
  type ChangePasswordFormValues,
  type NicknameChangeFormValues,
  type WithdrawalFormValues,
} from '../schemas'

/** `/mypage/profile`. 사이드바의 "프로필 수정" 링크가 여기로 온다. */
const ProfileEditPage = () => {
  const { data: profile, isPending, isError, error, refetch } = useMyProfile()

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }
  if (isPending || !profile) {
    return <LoadingBlock label="프로필을 불러오는 중입니다" />
  }

  return (
    <div className="flex flex-col gap-6">
      <ProfileHeaderCard
        email={profile.email}
        currentNickname={profile.nickname}
        profileImageFileId={profile.profileImageFileId}
      />
      <ChangePasswordSection />
      <WithdrawalSection />
    </div>
  )
}

/**
 * 프로필 사진 + 닉네임을 한 카드에 나란히 둔다.
 *
 * 사진은 원 안을 눌러 파일을 고르면 (1) `POST /api/files`(purpose=PROFILE_IMAGE)로 먼저 올리고
 * (2) 받은 fileId를 `PATCH /api/users/me/profile-image`로 내 계정에 연결하는 2단계로 처리한다 —
 * `features/file/api.ts` 의 범용 업로드를 그대로 재사용한다(A-API-017). 닉네임은 평소엔
 * 텍스트로만 보이다가 "수정" 버튼을 눌러야 입력칸 + 중복확인 버튼이 나온다 — 화면에 상시
 * 노출된 입력칸이 없으면 실수로 건드릴 일도 없다.
 */
const ProfileHeaderCard = ({
  email,
  currentNickname,
  profileImageFileId,
}: {
  email: string
  currentNickname: string
  profileImageFileId: number | null
}) => {
  const [editing, setEditing] = useState(false)
  const changeNicknameMutation = useChangeNickname()
  const nicknameAvailability = useAvailabilityHint(useNicknameAvailability())
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)
  const changeProfileImageMutation = useChangeProfileImage()
  const removeProfileImageMutation = useRemoveProfileImage()
  const [imageEditing, setImageEditing] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const [imageError, setImageError] = useState<string | null>(null)

  // 로컬 미리보기용 objectURL은 쓰고 나면 반드시 지운다 — 안 지우면 메모리에 계속 쌓인다.
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl)
    }
  }, [previewUrl])

  const startImageEditing = () => {
    setImageError(null)
    setImageEditing(true)
  }

  const cancelImageEditing = () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl)
    setSelectedFile(null)
    setPreviewUrl(null)
    setImageError(null)
    setImageEditing(false)
  }

  const handleFileSelected = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = '' // 같은 파일을 다시 골라도 onChange가 뜨도록 초기화
    if (!file) return

    if (previewUrl) URL.revokeObjectURL(previewUrl)
    setImageError(null)
    setSelectedFile(file)
    setPreviewUrl(URL.createObjectURL(file))
  }

  const saveImage = async () => {
    if (!selectedFile) return
    setImageError(null)
    setIsUploadingImage(true)
    try {
      const uploaded = await uploadFile(selectedFile, 'PROFILE_IMAGE')
      changeProfileImageMutation.mutate(uploaded.fileId, {
        onSuccess: () => cancelImageEditing(),
        onError: (error) => setImageError(getErrorMessage(error)),
      })
    } catch (error) {
      setImageError(getErrorMessage(error))
    } finally {
      setIsUploadingImage(false)
    }
  }

  const deleteImage = () => {
    setImageError(null)
    removeProfileImageMutation.mutate(undefined, {
      onSuccess: () => cancelImageEditing(),
      onError: (error) => setImageError(getErrorMessage(error)),
    })
  }

  const isImageBusy =
    isUploadingImage || changeProfileImageMutation.isPending || removeProfileImageMutation.isPending

  const {
    register,
    handleSubmit,
    getValues,
    reset,
    formState: { errors },
  } = useForm<NicknameChangeFormValues>({
    resolver: zodResolver(nicknameChangeSchema),
    defaultValues: { nickname: currentNickname },
  })

  const startEditing = () => {
    reset({ nickname: currentNickname })
    nicknameAvailability.reset()
    setSuccessMessage(null)
    setEditing(true)
  }

  const cancelEditing = () => {
    reset({ nickname: currentNickname })
    nicknameAvailability.reset()
    setEditing(false)
  }

  const onSubmit = (values: NicknameChangeFormValues) => {
    changeNicknameMutation.mutate(values.nickname, {
      onSuccess: () => {
        setSuccessMessage('닉네임이 변경되었습니다.')
        setEditing(false)
      },
    })
  }

  return (
    <Card>
      <CardTitle>프로필</CardTitle>
      <div className="flex items-start gap-4">
        <div className="flex shrink-0 flex-col items-center gap-2">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={handleFileSelected}
          />
          <div className="bg-surface-container-high text-on-surface-variant flex h-20 w-20 items-center justify-center overflow-hidden rounded-full">
            {previewUrl || profileImageFileId ? (
              // eslint-disable-next-line @next/next/no-img-element -- 로컬 미리보기/백엔드 프록시 경로라 Next 이미지 최적화 대상이 아니다.
              <img
                src={previewUrl ?? `/api/files/${profileImageFileId}/content`}
                alt="프로필 이미지"
                className="h-full w-full object-cover"
              />
            ) : (
              <UserRound className="h-10 w-10" aria-hidden />
            )}
          </div>

          {!imageEditing ? (
            <Button type="button" variant="ghost" size="sm" onClick={startImageEditing}>
              수정
            </Button>
          ) : (
            <div className="flex flex-col items-center gap-1">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                disabled={isImageBusy}
                onClick={() => fileInputRef.current?.click()}
              >
                파일 선택
              </Button>
              <div className="flex gap-1">
                <Button
                  type="button"
                  size="sm"
                  disabled={!selectedFile || isImageBusy}
                  loading={isUploadingImage || changeProfileImageMutation.isPending}
                  onClick={saveImage}
                >
                  저장
                </Button>
                {profileImageFileId && (
                  <Button
                    type="button"
                    variant="danger"
                    size="sm"
                    disabled={isImageBusy}
                    loading={removeProfileImageMutation.isPending}
                    onClick={deleteImage}
                  >
                    삭제
                  </Button>
                )}
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  disabled={isImageBusy}
                  onClick={cancelImageEditing}
                >
                  취소
                </Button>
              </div>
            </div>
          )}
        </div>

        <div className="flex-1">
          <p className="text-body-sm text-on-surface-variant">{email}</p>
          {imageError && <p className="text-label-sm text-error mt-1">{imageError}</p>}

          {!editing ? (
            <div className="mt-1 flex items-center gap-3">
              <p className="text-title-md text-on-surface font-semibold">{currentNickname}</p>
              <Button type="button" variant="ghost" size="sm" onClick={startEditing}>
                수정
              </Button>
            </div>
          ) : (
            <form className="mt-2 flex flex-col gap-2" onSubmit={handleSubmit(onSubmit)} noValidate>
              <div className="flex items-start gap-2">
                <div className="flex-1">
                  <Input
                    label="닉네임"
                    error={errors.nickname?.message}
                    hint={
                      nicknameAvailability.hint?.ok ? nicknameAvailability.hint.message : undefined
                    }
                    {...register('nickname')}
                  />
                  {nicknameAvailability.hint?.ok === false && (
                    <p className="text-label-sm text-error mt-1">
                      {nicknameAvailability.hint.message}
                    </p>
                  )}
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  className="mt-6"
                  loading={nicknameAvailability.checking}
                  onClick={() => nicknameAvailability.check(getValues('nickname'))}
                >
                  중복확인
                </Button>
              </div>
              {changeNicknameMutation.isError && (
                <p className="text-label-sm text-error">
                  {getErrorMessage(changeNicknameMutation.error)}
                </p>
              )}
              <div className="flex gap-2">
                <Button type="submit" size="sm" loading={changeNicknameMutation.isPending}>
                  저장
                </Button>
                <Button type="button" variant="secondary" size="sm" onClick={cancelEditing}>
                  취소
                </Button>
              </div>
            </form>
          )}

          {!editing && successMessage && (
            <Badge variant="success" className="mt-2">
              {successMessage}
            </Badge>
          )}
        </div>
      </div>
    </Card>
  )
}

/**
 * 로그인 상태에서의 비밀번호 변경. 처음엔 버튼만 보이고, 누르면 현재/새/새 비밀번호 확인
 * 세 칸이 펼쳐진다. 이메일 토큰 재설정("비밀번호를 잊어버렸을 때")과는 다른 화면이다 —
 * 그건 로그인 페이지의 "비밀번호 찾기"에서 별도로 제공한다.
 */
const ChangePasswordSection = () => {
  const changePasswordMutation = useChangePassword()
  const [expanded, setExpanded] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({ resolver: zodResolver(changePasswordSchema) })

  const onSubmit = (values: ChangePasswordFormValues) => {
    changePasswordMutation.mutate(values, {
      onSuccess: () => {
        setSuccessMessage('비밀번호가 변경되었습니다.')
        reset()
        setExpanded(false)
      },
    })
  }

  // react-hook-form 은 언마운트된 필드 값을 기본적으로 유지한다 — 취소하고 다시 열면
  // 방금 입력했던 비밀번호가 그대로 남아 있으면 안 되니 폼과 mutation 상태를 같이 지운다.
  const cancel = () => {
    reset()
    changePasswordMutation.reset()
    setExpanded(false)
  }

  return (
    <Card>
      <CardTitle>비밀번호 변경</CardTitle>

      {!expanded ? (
        <Button type="button" variant="secondary" size="sm" onClick={() => setExpanded(true)}>
          비밀번호 변경
        </Button>
      ) : (
        <form className="flex flex-col gap-3" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input
            label="현재 비밀번호"
            type="password"
            autoComplete="current-password"
            autoFocus
            error={errors.currentPassword?.message}
            {...register('currentPassword')}
          />
          <Input
            label="새 비밀번호"
            type="password"
            autoComplete="new-password"
            error={errors.newPassword?.message}
            {...register('newPassword')}
          />
          <Input
            label="새 비밀번호 확인"
            type="password"
            autoComplete="new-password"
            error={errors.newPasswordConfirm?.message}
            {...register('newPasswordConfirm')}
          />
          {changePasswordMutation.isError && (
            <p className="text-label-sm text-error">
              {getErrorMessage(changePasswordMutation.error)}
            </p>
          )}
          <div className="flex gap-2">
            <Button type="submit" size="sm" loading={changePasswordMutation.isPending}>
              변경하기
            </Button>
            <Button type="button" variant="secondary" size="sm" onClick={cancel}>
              취소
            </Button>
          </div>
        </form>
      )}

      {!expanded && successMessage && (
        <Badge variant="success" className="mt-2">
          {successMessage}
        </Badge>
      )}
    </Card>
  )
}

/**
 * 회원 탈퇴. "탈퇴하기"를 누르면 비밀번호 입력칸이 펼쳐지고, 거기서 다시 제출하면
 * `window.confirm` 으로 "정말 탈퇴하시겠습니까?" 를 한 번 더 물은 뒤에야 실제로 탈퇴 처리한다.
 */
const WithdrawalSection = () => {
  const router = useRouter()
  const withdrawMutation = useWithdraw()
  const [expanded, setExpanded] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<WithdrawalFormValues>({ resolver: zodResolver(withdrawalSchema) })

  const onSubmit = (values: WithdrawalFormValues) => {
    const confirmed = window.confirm('정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')
    if (!confirmed) return

    withdrawMutation.mutate(values.password, {
      onSuccess: () => router.replace('/login'),
    })
  }

  // 비밀번호 변경 섹션과 같은 이유 — 취소 후 다시 열었을 때 입력했던 비밀번호가 남아있지 않게 한다.
  const cancel = () => {
    reset()
    withdrawMutation.reset()
    setExpanded(false)
  }

  return (
    <Card className="border-error-container border">
      <CardTitle className="text-error">회원 탈퇴</CardTitle>
      <p className="text-body-sm text-on-surface-variant">
        탈퇴하면 다시 로그인할 수 없습니다. 예매·발권 기록은 삭제되지 않고 그대로 남습니다.
      </p>

      {!expanded ? (
        <Button
          type="button"
          variant="danger"
          size="sm"
          className="mt-3"
          onClick={() => setExpanded(true)}
        >
          탈퇴하기
        </Button>
      ) : (
        <form className="mt-3 flex flex-col gap-3" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input
            label="현재 비밀번호"
            type="password"
            autoComplete="current-password"
            autoFocus
            error={errors.password?.message}
            {...register('password')}
          />
          {withdrawMutation.isError && (
            <p className="text-label-sm text-error">{getErrorMessage(withdrawMutation.error)}</p>
          )}
          <div className="flex gap-2">
            <Button type="submit" variant="danger" loading={withdrawMutation.isPending}>
              탈퇴하기
            </Button>
            <Button type="button" variant="secondary" onClick={cancel}>
              취소
            </Button>
          </div>
        </form>
      )}
    </Card>
  )
}

export default ProfileEditPage
