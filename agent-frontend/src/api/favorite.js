import request from './request'

/** 收藏 / 取消收藏（后端切换），返回 { favorited } */
export function toggleFavorite(productId) {
  return request.post('/api/favorite/toggle', null, { params: { productId } })
}

/** 我收藏的商品 ID 列表（列表页渲染红心） */
export function getFavoriteIds(config = {}) {
  return request.get('/api/favorite/ids', config)
}

/** 我的收藏（带商品信息） */
export function getFavoriteList() {
  return request.get('/api/favorite/list')
}

/** 取消收藏 */
export function removeFavorite(productId) {
  return request.delete(`/api/favorite/${productId}`)
}
