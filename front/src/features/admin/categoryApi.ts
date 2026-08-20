import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `CategoryResponse` 와 짝이다. */
export type Category = {
  id: number
  parentId: number | null
  name: string
  slug: string
  sortOrder: number
  active: boolean
}

/** `GET /api/admin/categories` — 카테고리 목록. */
export const listCategories = async (): Promise<Category[]> => {
  const { data } = await api.get<ApiEnvelope<Category[]>>('/admin/categories')
  return data.data
}

/** `CategoryCreateRequest` 와 짝이다. */
export type CreateCategoryPayload = {
  parentId?: number
  name: string
  sortOrder?: number
}

/** `POST /api/admin/categories` — 카테고리 등록. */
export const createCategory = async (payload: CreateCategoryPayload): Promise<Category> => {
  const { data } = await api.post<ApiEnvelope<Category>>('/admin/categories', payload)
  return data.data
}

/** `CategoryUpdateRequest` 와 짝이다. 전달된 필드만 변경한다. */
export type UpdateCategoryPayload = {
  name?: string
  sortOrder?: number
  active?: boolean
}

/** `PATCH /api/admin/categories/{categoryId}` — 카테고리명·노출순서·활성상태 수정. */
export const updateCategory = async (
  categoryId: number,
  payload: UpdateCategoryPayload
): Promise<Category> => {
  const { data } = await api.patch<ApiEnvelope<Category>>(
    `/admin/categories/${categoryId}`,
    payload
  )
  return data.data
}

/** `DELETE /api/admin/categories/{categoryId}` — 미사용 카테고리 비활성/삭제. 응답 본문 없음. */
export const deactivateCategory = async (categoryId: number): Promise<void> => {
  await api.delete(`/admin/categories/${categoryId}`)
}
