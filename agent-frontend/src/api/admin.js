import request from './request'

/** 待审核商品列表（status = 0） */
export function getPendingProducts() {
  return request.get('/api/admin/product/pending')
}

/**
 * 审核商品
 * @param {number|string} id 商品 ID
 * @param {0|1} pass 1=通过，0=驳回
 */
export function auditProduct(id, pass) {
  return request.put(`/api/admin/product/audit/${id}`, null, { params: { pass } })
}

/** 待处理举报列表（status = 0） */
export function getPendingReports() {
  return request.get('/api/admin/report/pending')
}

/** 已处理（历史）举报列表（status = 1） */
export function getHandledReports() {
  return request.get('/api/admin/report/handled')
}

/**
 * 处理举报
 * @param {number|string} reportId
 * @param {0|1} action 1=判定违规并下架商品（通知卖家）；0=驳回举报（商品保持原状态，不发通知）
 */
export function handleReport(reportId, action = 1) {
  return request.put(`/api/admin/report/handle/${reportId}`, null, { params: { action } })
}
