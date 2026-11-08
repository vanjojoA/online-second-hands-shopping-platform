import request from './request'

/** 我的系统通知列表 */
export function getNotifications() {
  return request.get('/api/notification/list')
}

/** 把某条通知标记为已读 */
export function markNotificationRead(id) {
  return request.put(`/api/notification/read/${id}`)
}
