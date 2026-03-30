import type { ApiResponse } from '~/types/forum'

type RequestOptions = Parameters<typeof $fetch<ApiResponse<unknown>>>[1]

export class ApiError extends Error {
  code: number
  data: unknown

  constructor(message: string, code = 5000, data: unknown = null) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (!value || typeof value !== 'object') {
    return false
  }

  const response = value as Record<string, unknown>

  return (
    typeof response.code === 'number'
    && typeof response.message === 'string'
    && 'data' in response
  )
}

export function useApi() {
  const auth = useAuth()

  async function request<T>(path: string, options: RequestOptions = {}) {
    auth.bootstrap()

    const headers = {
      ...(options.headers as Record<string, string> | undefined)
    }

    if (auth.token.value) {
      headers.Authorization = `Bearer ${auth.token.value}`
    }

    try {
      const response = await $fetch<ApiResponse<T>>(path, {
        ...options,
        headers
      })

      if (response.code !== 0) {
        if (response.code === 4001) {
          auth.clear()
        }

        throw new ApiError(response.message || '请求失败', response.code, response.data)
      }

      return response.data as T
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        throw error
      }

      const fetchError = error as {
        data?: unknown
        message?: string
        response?: {
          _data?: unknown
        }
      }
      const payload = fetchError.data || fetchError.response?._data

      if (isApiResponse(payload)) {
        if (payload.code === 4001) {
          auth.clear()
        }

        throw new ApiError(payload.message || '请求失败', payload.code, payload.data)
      }

      throw new ApiError(fetchError.message || '网络请求失败')
    }
  }

  return {
    request
  }
}
