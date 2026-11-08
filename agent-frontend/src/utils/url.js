import { BASE_URL } from '@/api/request'

// 去掉 base 末尾的斜杠，避免拼出 "//images/xxx" 这种协议相对地址
const NORMALIZED_BASE = (BASE_URL || '').replace(/\/+$/, '')

/**
 * 把后端返回的 imageUrls（可能是逗号分隔的多个地址）转成可直接用于 <img> 的地址
 */
export function resolveImageUrl(imageUrls) {
  if (!imageUrls) return ''
  const first = String(imageUrls).split(',')[0].trim()
  if (!first) return ''
  if (/^https?:\/\//i.test(first) || first.startsWith('data:')) return first
  if (first.startsWith('/')) return `${NORMALIZED_BASE}${first}`
  return first
}
