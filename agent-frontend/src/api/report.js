import request from './request'

/**
 * 提交举报
 * @param {{ targetType: number, targetId: number, reason: string }} data
 *        targetType：1=商品
 */
export function submitReport(data) {
  return request.post('/api/report/submit', data)
}
