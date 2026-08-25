'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

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
} from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import type { Category } from '../categoryApi'
import {
  useAdminCategories,
  useCreateCategory,
  useDeactivateCategory,
  useUpdateCategory,
} from '../categoryHooks'
import {
  createCategorySchema,
  updateCategorySchema,
  type CreateCategoryFormValues,
  type UpdateCategoryFormValues,
} from '../schemas'

/** 카테고리 등록 폼. */
const CreateCategoryForm = () => {
  const createMutation = useCreateCategory()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateCategoryFormValues>({
    resolver: zodResolver(createCategorySchema),
    defaultValues: { name: '', sortOrder: '' },
  })

  const onSubmit = (values: CreateCategoryFormValues) => {
    createMutation.mutate(
      {
        name: values.name,
        sortOrder: values.sortOrder ? Number(values.sortOrder) : undefined,
      },
      { onSuccess: () => reset() }
    )
  }

  return (
    <Card>
      <CardTitle>카테고리 등록</CardTitle>
      <form
        className="flex flex-wrap items-start gap-3"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <div className="min-w-[200px] flex-1">
          <Input label="카테고리명" error={errors.name?.message} {...register('name')} />
        </div>
        <div className="w-28">
          <Input
            label="노출 순서"
            inputMode="numeric"
            error={errors.sortOrder?.message}
            {...register('sortOrder')}
          />
        </div>
        <Button type="submit" className="mt-6" loading={createMutation.isPending}>
          등록
        </Button>
      </form>
      {createMutation.isError && (
        <p className="text-label-sm text-error mt-2">{getErrorMessage(createMutation.error)}</p>
      )}
    </Card>
  )
}

/** 카테고리 한 행. 보기/수정 모드를 로컬 상태로 전환한다. */
const CategoryRow = ({ category }: { category: Category }) => {
  const [editing, setEditing] = useState(false)
  const updateMutation = useUpdateCategory()
  const deactivateMutation = useDeactivateCategory()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<UpdateCategoryFormValues>({
    resolver: zodResolver(updateCategorySchema),
    defaultValues: {
      name: category.name,
      sortOrder: String(category.sortOrder),
      active: category.active,
    },
  })

  const onSubmit = (values: UpdateCategoryFormValues) => {
    updateMutation.mutate(
      {
        categoryId: category.id,
        payload: {
          name: values.name || undefined,
          sortOrder: values.sortOrder ? Number(values.sortOrder) : undefined,
          active: values.active,
        },
      },
      { onSuccess: () => setEditing(false) }
    )
  }

  if (editing) {
    return (
      <form
        className="flex flex-wrap items-start gap-3 px-6 py-4"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <div className="min-w-[200px] flex-1">
          <Input label="카테고리명" error={errors.name?.message} {...register('name')} />
        </div>
        <div className="w-28">
          <Input
            label="노출 순서"
            inputMode="numeric"
            error={errors.sortOrder?.message}
            {...register('sortOrder')}
          />
        </div>
        <label className="text-label-md text-on-surface-variant mt-6 flex items-center gap-2 font-medium">
          <input type="checkbox" {...register('active')} />
          활성 상태
        </label>
        <div className="mt-6 flex gap-2">
          <Button type="submit" size="sm" loading={updateMutation.isPending}>
            저장
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(false)}>
            취소
          </Button>
        </div>
        {updateMutation.isError && (
          <p className="text-label-sm text-error w-full">{getErrorMessage(updateMutation.error)}</p>
        )}
      </form>
    )
  }

  return (
    <div className="flex items-center justify-between gap-4 px-6 py-4">
      <div>
        <div className="flex items-center gap-2">
          <p className="text-title-md text-on-surface font-medium">{category.name}</p>
          <Badge variant={category.active ? 'success' : 'neutral'}>
            {category.active ? '활성' : '비활성'}
          </Badge>
        </div>
        <p className="text-body-sm text-on-surface-variant mt-1">
          ID {category.id} · slug {category.slug} · 상위 {category.parentId ?? '없음'} · 순서{' '}
          {category.sortOrder}
        </p>
      </div>
      <div className="flex shrink-0 gap-2">
        <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>
          수정
        </Button>
        {category.active && (
          <Button
            size="sm"
            variant="danger"
            loading={deactivateMutation.isPending}
            onClick={() => {
              if (window.confirm(`"${category.name}" 카테고리를 비활성화할까요?`)) {
                deactivateMutation.mutate(category.id)
              }
            }}
          >
            비활성화
          </Button>
        )}
      </div>
    </div>
  )
}

/** `/admin/categories`. Function.md 4절 — "CRUD". ADMIN 전용. */
const AdminCategoryListPage = () => {
  const { data: categories, isPending, isError, error, refetch } = useAdminCategories()

  return (
    <div>
      <PageHeader title="카테고리 관리" description="박람회 카테고리를 등록·수정·비활성화합니다." />

      <div className="flex flex-col gap-6">
        <CreateCategoryForm />

        {isPending ? (
          <LoadingBlock label="카테고리 목록을 불러오는 중입니다" />
        ) : isError ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : categories.length === 0 ? (
          <EmptyState
            title="등록된 카테고리가 없습니다"
            description="위 폼으로 첫 카테고리를 등록해 보세요."
          />
        ) : (
          <Card className="divide-outline-variant divide-y p-0">
            {categories.map((category) => (
              <CategoryRow key={category.id} category={category} />
            ))}
          </Card>
        )}
      </div>
    </div>
  )
}

export default AdminCategoryListPage
