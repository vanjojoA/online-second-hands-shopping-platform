import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'

// 生产环境默认直连云端后端；开发环境走 Vite 代理（见 .env.development 与 vite.config.js）
const useProxy = import.meta.env.VITE_USE_PROXY === 'true'
export const BASE_URL = useProxy ? '' : import.meta.env.VITE_API_BASE_URL || 'http://8.138.25.66:8080'

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
})

/* ------------------------------ 请求拦截器 ------------------------------ */
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      // 后端 JwtInterceptor 只认 "Bearer xxx" 这种格式
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/* ------------------------------ 响应拦截器 ------------------------------ */
let redirectingToLogin = false

// 游客也能访问的页面：这些页面上 Token 失效时不要强制跳登录
const PUBLIC_PREFIXES = ['/products', '/login', '/register']

function isPublicPage() {
  const path = window.location.pathname
  if (path === '/' || path === '') return true
  return PUBLIC_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))
}

function redirectToLogin() {
  if (redirectingToLogin) return
  if (window.location.pathname.startsWith('/login')) return
  redirectingToLogin = true
  const current = window.location.pathname + window.location.search
  window.location.replace(`/login?redirect=${encodeURIComponent(current)}`)
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    const silent = Boolean(response.config?.__silent)

    // 后端统一返回 { code, message, data }
    if (res && typeof res === 'object' && Object.prototype.hasOwnProperty.call(res, 'code')) {
      if (res.code === 200) {
        return res
      }
      if (!silent) ElMessage.error(res.message || '请求失败')
      const error = new Error(res.message || '请求失败')
      // 标记一下：错误提示已经弹过了，页面里不用重复弹
      error.__toasted = true
      return Promise.reject(error)
    }

    return res
  },
  (error) => {
    const silent = Boolean(error.config?.__silent)

    // 已经拿到响应：按 HTTP 状态码处理
    if (error.response) {
      const { status, data } = error.response
      const serverMessage = typeof data === 'string' && data.trim() ? data.trim() : ''

      if (status === 401) {
        // Token 缺失 / 过期 / 已退出（Redis 黑名单）：先清掉本地登录态
        clearAuth()

        if (silent) {
          return Promise.reject(error)
        }

        const message = serverMessage || '登录已过期，请重新登录'

        // 游客页面：提示一下就好，不要强制跳转，避免逛商品时被踢走
        if (isPublicPage()) {
          ElMessage.warning(message)
          return Promise.reject(error)
        }

        ElMessage.error(message)
        redirectToLogin()
      } else if (silent) {
        // 后台轮询类请求静默失败
      } else if (status === 403) {
        ElMessage.error(serverMessage || '没有权限执行该操作')
      } else if (status === 404) {
        ElMessage.error('接口不存在（404），请检查后端地址或接口路径')
      } else if (status >= 500) {
        ElMessage.error('服务器开小差了，请稍后再试')
      } else {
        ElMessage.error(serverMessage || `请求失败（${status}）`)
      }
    } else if (!silent) {
      if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请检查网络或服务器状态')
      } else {
        ElMessage.error('网络异常，无法连接服务器')
      }
    }

    return Promise.reject(error)
  }
)

export default request
