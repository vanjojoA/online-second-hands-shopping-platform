import request from './request'

/**
 * 用户注册
 * @param {{ studentId: string, password: string, nickname?: string }} data
 */
export function register(data) {
  return request.post('/api/user/register', data)
}

/**
 * 用户登录
 * @param {{ studentId: string, password: string }} data
 * @returns {Promise<{ code: number, message: string, data: { token: string, userId: number, nickname: string } }>}
 */
export function login(data) {
  return request.post('/api/user/login', data)
}

/**
 * 获取当前登录用户信息（需要 Token）
 */
export function getUserInfo() {
  return request.get('/api/user/info')
}

/**
 * 退出登录：把当前 Token 加入后端 Redis 黑名单（需要 Token）
 * 调用成功后，这个 Token 再拿来请求会直接 401
 */
export function logout() {
  return request.post('/api/user/logout')
}

/**
 * 修改昵称 / 头像
 * @param {{ nickname?: string, avatar?: string }} data
 */
export function updateUserInfo(data) {
  return request.put('/api/user/updateInfo', data)
}

/**
 * 修改密码
 * @param {{ oldPassword: string, newPassword: string }} data
 */
export function updatePassword(data) {
  return request.put('/api/user/updatePassword', data)
}
