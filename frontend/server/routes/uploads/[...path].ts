import { readFile, stat } from 'node:fs/promises'
import { basename } from 'node:path'
import { createError, defineEventHandler, setHeader } from 'h3'
import { getUploadContentType, resolveUploadPath } from '../../utils/uploads'

export default defineEventHandler(async (event) => {
  const params = event.context.params?.path
  const segments = Array.isArray(params) ? params : params ? [params] : []

  if (!segments.length) {
    throw createError({
      statusCode: 404,
      statusMessage: '文件不存在'
    })
  }

  const filePath = resolveUploadPath(segments)

  let fileStat

  try {
    fileStat = await stat(filePath)
  } catch {
    throw createError({
      statusCode: 404,
      statusMessage: '文件不存在'
    })
  }

  if (!fileStat.isFile()) {
    throw createError({
      statusCode: 404,
      statusMessage: '文件不存在'
    })
  }

  const fileBuffer = await readFile(filePath)

  setHeader(event, 'content-type', getUploadContentType(filePath))
  setHeader(event, 'content-length', fileStat.size)
  setHeader(event, 'cache-control', 'public, max-age=31536000, immutable')
  setHeader(event, 'content-disposition', `inline; filename="${basename(filePath)}"`)

  return fileBuffer
})
