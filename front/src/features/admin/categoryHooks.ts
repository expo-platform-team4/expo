import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  createCategory,
  deactivateCategory,
  listCategories,
  updateCategory,
  type UpdateCategoryPayload,
} from './categoryApi'
import { adminKeys } from './queryKeys'

/** `/admin/categories` — 카테고리 목록. */
export const useAdminCategories = () =>
  useQuery({
    queryKey: adminKeys.categories(),
    queryFn: listCategories,
  })

/** 카테고리 등록. 성공하면 목록 캐시를 무효화한다(Spec.md 6절). */
export const useCreateCategory = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.categories() })
    },
  })
}

/** 카테고리명·노출순서·활성상태 수정. */
export const useUpdateCategory = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ categoryId, payload }: { categoryId: number; payload: UpdateCategoryPayload }) =>
      updateCategory(categoryId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.categories() })
    },
  })
}

/** 미사용 카테고리 비활성/삭제. */
export const useDeactivateCategory = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deactivateCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.categories() })
    },
  })
}
