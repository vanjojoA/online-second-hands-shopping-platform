import request from './request'

/**
 * 记录一次商品浏览（后端按 user_id + product_id 去重，重复浏览只更新时间）
 * @param {number|string} productId
 */
export function recordBrowse(productId) {
  return request.post('/api/history/record', null, { params: { productId } })
}

/** 我的浏览记录（最近 50 条，带商品信息） */
export function getBrowseHistory() {
  return request.get('/api/history/list')
}

/** 删除一条浏览记录 */
export function deleteBrowseHistory(id) {
  return request.delete(`/api/history/${id}`)
}

/** 清空浏览记录 */
export function clearBrowseHistory() {
  return request.delete('/api/history/clear')
}
