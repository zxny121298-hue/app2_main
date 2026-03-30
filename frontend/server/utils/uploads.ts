import { randomUUID } from 'node:crypto'
import { mkdir } from 'node:fs/promises'
import { extname, relative, resolve } from 'node:path'
import { createError } from 'h3'

export type UploadScope = 'avatar' | 'post' | 'comment'

const UPLOAD_MAX_BYTES = 5 * 1024 * 1024
const UPLOAD_ALLOWED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'image/avif'
] as const
const UPLOAD_SCOPE_DIRS: Record<UploadScope, string> = {
  avatar: 'avatars',
  post: 'posts',
  comment: 'comments'
}

const CONTENT_TYPE_TO_EXT: Record<(typeof UPLOAD_ALLOWED_IMAGE_TYPES)[number], string> = {
  'image/jpeg': '.jpg',
  'image/png': '.png',
  'image/webp': '.webp',
  'image/gif': '.gif',
  'image/avif': '.avif'
}

const EXT_TO_CONTENT_TYPE: Record<string, string> = Object.fromEntries(
  Object.entries(CONTENT_TYPE_TO_EXT).map(([contentType, extension]) => [extension, contentType])
)

const uploadsRoot = resolve(process.cwd(), '.data', 'uploads')

export function parseUploadScope(value?: string | null): UploadScope {
  if (value === 'avatar' || value === 'post' || value === 'comment') {
    return value
  }

  throw createError({
    statusCode: 400,
    statusMessage: '不支持的上传类型'
  })
}

export async function ensureUploadDir(scope: UploadScope) {
  const directory = resolve(uploadsRoot, UPLOAD_SCOPE_DIRS[scope])
  await mkdir(directory, { recursive: true })
  return directory
}

export function assertUploadImage(contentType?: string | null, size = 0) {
  if (!contentType || !UPLOAD_ALLOWED_IMAGE_TYPES.includes(contentType as (typeof UPLOAD_ALLOWED_IMAGE_TYPES)[number])) {
    throw createError({
      statusCode: 400,
      statusMessage: '仅支持 JPG、PNG、WEBP、GIF、AVIF 图片'
    })
  }

  if (size <= 0) {
    throw createError({
      statusCode: 400,
      statusMessage: '上传的图片为空'
    })
  }

  if (size > UPLOAD_MAX_BYTES) {
    throw createError({
      statusCode: 413,
      statusMessage: `图片不能超过 ${Math.floor(UPLOAD_MAX_BYTES / (1024 * 1024))} MB`
    })
  }
}

export function createStoredFileName(fileName: string | undefined, contentType: string) {
  const extension = CONTENT_TYPE_TO_EXT[contentType as keyof typeof CONTENT_TYPE_TO_EXT] || extname(fileName || '').toLowerCase()

  if (!extension || !(extension in EXT_TO_CONTENT_TYPE)) {
    throw createError({
      statusCode: 400,
      statusMessage: '无法识别图片格式'
    })
  }

  return `${Date.now()}-${randomUUID()}${extension}`
}

export function buildUploadUrl(scope: UploadScope, fileName: string) {
  return `/uploads/${UPLOAD_SCOPE_DIRS[scope]}/${fileName}`
}

export function resolveUploadPath(segments: string[]) {
  const nextPath = resolve(uploadsRoot, ...segments)
  const relativePath = relative(uploadsRoot, nextPath)

  if (!relativePath || relativePath.startsWith('..') || relativePath.includes(':')) {
    throw createError({
      statusCode: 400,
      statusMessage: '非法文件路径'
    })
  }

  return nextPath
}

export function getUploadContentType(filePath: string) {
  return EXT_TO_CONTENT_TYPE[extname(filePath).toLowerCase()] || 'application/octet-stream'
}
