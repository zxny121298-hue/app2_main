import { joinURL, withQuery } from 'ufo'

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const params = event.context.params?.path
  const path = Array.isArray(params) ? params.join('/') : params || ''
  const target = withQuery(joinURL(config.backendBase, path), getQuery(event))
  const method = getMethod(event)
  const headers = Object.entries(getRequestHeaders(event)).filter((entry): entry is [string, string] => (
    (() => {
      const [key, value] = entry

      return (
        key !== 'host'
        && key !== 'connection'
        && key !== 'content-length'
        && typeof value === 'string'
      )
    })()
  ))

  const body = method === 'GET' || method === 'HEAD' ? undefined : await readBody(event)

  try {
    const response = await $fetch.raw(target, {
      method,
      body,
      headers
    })

    setResponseStatus(event, response.status, response.statusText)

    for (const [key, value] of response.headers.entries()) {
      const lowerKey = key.toLowerCase()

      if (lowerKey === 'content-length' || lowerKey === 'transfer-encoding') {
        continue
      }

      setHeader(event, key, value)
    }

    return response._data
  } catch (error: unknown) {
    const fetchError = error as {
      response?: {
        status?: number
        statusText?: string
        headers?: Headers
        _data?: unknown
      }
    }

    if (fetchError.response) {
      setResponseStatus(
        event,
        fetchError.response.status || 500,
        fetchError.response.statusText || 'Upstream Error'
      )

      for (const [key, value] of fetchError.response.headers?.entries() || []) {
        const lowerKey = key.toLowerCase()

        if (lowerKey === 'content-length' || lowerKey === 'transfer-encoding') {
          continue
        }

        setHeader(event, key, value)
      }

      return fetchError.response._data
    }

    throw createError({
      statusCode: 502,
      statusMessage: 'Backend service unavailable'
    })
  }
})
