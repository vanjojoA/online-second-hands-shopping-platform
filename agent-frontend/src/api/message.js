import request from './request'

/**
 * 获取与某个好友的聊天记录（需要 Token）
 * @param {string|number} friendId 对方用户 ID
 */
export function getMessageHistory(friendId) {
  return request.get('/api/message/history', { params: { friendId } })
}

/**
 * 会话列表（每个聊过天的人一条，带最后一条消息与未读数）
 */
export function getConversations() {
  return request.get('/api/message/conversations')
}

/**
 * 我的总未读数（顶栏红点）
 */
export function getUnreadCount() {
  return request.get('/api/message/unread-count')
}

/**
 * 进入会话后，把对方发给我的消息标记为已读
 * @param {string|number} friendId
 */
export function markConversationRead(friendId) {
  return request.put('/api/message/read', null, { params: { friendId } })
}
