type UploadScope = 'avatar' | 'post' | 'comment'

type UploadImageResponse = {
  url: string
  fileName: string
  contentType: string
  size: number
}

const UPLOAD_MAX_BYTES = 5 * 1024 * 1024
const UPLOAD_ALLOWED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'image/avif'
] as const
const UPLOAD_IMAGE_ACCEPT = UPLOAD_ALLOWED_IMAGE_TYPES.join(',')

const allowedImageTypes = new Set<string>(UPLOAD_ALLOWED_IMAGE_TYPES)

export function useImageUpload() {
  function validateImageFile(file: File) {
    if (!allowedImageTypes.has(file.type)) {
      throw new Error('仅支持 JPG、PNG、WEBP、GIF、AVIF 图片')
    }

    if (file.size <= 0) {
      throw new Error('图片文件为空')
    }

    if (file.size > UPLOAD_MAX_BYTES) {
      throw new Error(`图片不能超过 ${Math.floor(UPLOAD_MAX_BYTES / (1024 * 1024))} MB`)
    }
  }

  async function uploadImage(file: File, scope: UploadScope) {
    validateImageFile(file)

    const formData = new FormData()
    formData.append('image', file)
    formData.append('scope', scope)

    return await $fetch<UploadImageResponse>('/api/upload', {
      method: 'POST',
      body: formData
    })
  }

  return {
    accept: UPLOAD_IMAGE_ACCEPT,
    maxBytes: UPLOAD_MAX_BYTES,
    uploadImage,
    validateImageFile
  }
}
