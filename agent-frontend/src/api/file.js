import request from './request'

/**
 * 上传商品图片
 * 注意：后端这个接口比较特殊，返回的是
 *   { code: 200, data: null, message: "/images/xxx.png" }
 * 图片地址在 message 里，取地址时要兼容一下。
 *
 * @param {File} file
 * @returns {Promise<{ code: number, message: string, data: string|null }>}
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/file/upload', formData, { timeout: 60000 })
}
