import request from './request'

/**
 * 获取商品列表
 * @param {{ page?: number, size?: number, keyword?: string, minPrice?: number, maxPrice?: number, categoryId?: number }} params
 */
export function getProductList(params) {
  return request.get('/api/product/list', { params })
}

/**
 * 获取商品详情（含卖家信息）
 * @param {string|number} id 商品 ID
 * @returns {Promise<{ code: number, message: string, data: { product: object, seller: object } }>}
 */
export function getProductDetail(id) {
  return request.get(`/api/product/detail/${id}`)
}

/**
 * 发布商品
 * @param {{ title: string, price: number, description: string, imageUrls: string }} data
 */
export function publishProduct(data) {
  return request.post('/api/product/publish', data)
}

/**
 * 修改我发布的商品（修改后重新进入待审核）
 * @param {string|number} id
 * @param {{ title: string, price: number, description: string, imageUrls: string }} data
 */
export function updateProduct(id, data) {
  return request.put(`/api/product/update/${id}`, data)
}

/**
 * 查询我发布的商品
 */
export function getMyProducts() {
  return request.get('/api/product/my')
}

/**
 * 修改商品状态（后端目前固定把状态改成 2 = 已售出）
 * @param {string|number} id
 */
export function updateProductStatus(id) {
  return request.put(`/api/product/updateStatus/${id}`)
}
