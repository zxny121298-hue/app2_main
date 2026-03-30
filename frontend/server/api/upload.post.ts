import { writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { createError, defineEventHandler, readMultipartFormData } from 'h3'
import {
  assertUploadImage,
  buildUploadUrl,
  createStoredFileName,
  ensureUploadDir,
  parseUploadScope
} from '../utils/uploads'

type UploadImageResponse = {
  url: string
  fileName: string
  contentType: string
  size: number
}

export default defineEventHandler(async (event) => {
  const parts = await readMultipartFormData(event)

  if (!parts?.length) {
    throw createError({
      statusCode: 400,
      statusMessage: '请选择要上传的图片'
    })
  }

  const scopePart = parts.find(part => part.name === 'scope')
  const image = parts.find(part => part.name === 'image' && part.filename && part.data)

  if (!image?.filename || !image.data?.byteLength) {
    throw createError({
      statusCode: 400,
      statusMessage: '请选择要上传的图片'
    })
  }

  const scope = parseUploadScope(scopePart?.data?.toString('utf8').trim() || 'post')

  assertUploadImage(image.type, image.data.byteLength)

  const uploadDir = await ensureUploadDir(scope)
  const fileName = createStoredFileName(image.filename, image.type || '')

  await writeFile(join(uploadDir, fileName), image.data)

  const response: UploadImageResponse = {
    url: buildUploadUrl(scope, fileName),
    fileName,
    contentType: image.type || 'application/octet-stream',
    size: image.data.byteLength
  }

  return response
})
