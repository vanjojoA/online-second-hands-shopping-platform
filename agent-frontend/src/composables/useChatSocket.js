import { onBeforeUnmount, ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://8.138.25.66:8080'

// WebSocket 地址：优先用环境变量，没有就从 API 地址推导（http -> ws）
export const WS_BASE = (import.meta.env.VITE_WS_BASE_URL || API_BASE).replace(/^http/, 'ws')

/**
 * 聊天 WebSocket 封装：负责连接、断线重连、发送
 *
 * 后端协议：
 *   连接：ws://8.138.25.66:8080/ws/chat?userId=当前登录人ID
 *   发送："接收者ID:消息内容"
 *   接收：后端推送 "收到新消息：内容"（注意：推送里没有发送者 ID）
 *
 * @param {{ userId: number|string, onMessage?: (text: string) => void }} options
 */
export function useChatSocket({ userId, onMessage }) {
  // closed | connecting | open | reconnecting
  const status = ref('closed')
  const retryCount = ref(0)

  let socket = null
  let retryTimer = null
  let manualClosed = false
  let retries = 0

  function clearTimer() {
    if (retryTimer) {
      clearTimeout(retryTimer)
      retryTimer = null
    }
  }

  function scheduleReconnect() {
    retries += 1
    retryCount.value = retries
    status.value = 'reconnecting'
    // 指数退避：1s、2s、4s、8s…最多 15s
    const delay = Math.min(1000 * 2 ** (retries - 1), 15000)
    clearTimer()
    retryTimer = setTimeout(() => {
      if (!manualClosed) connect()
    }, delay)
  }

  function connect() {
    if (!userId) return
    manualClosed = false
    clearTimer()

    // 已经连着就不用重连
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    status.value = retries > 0 ? 'reconnecting' : 'connecting'

    try {
      socket = new WebSocket(`${WS_BASE}/ws/chat?userId=${userId}`)
    } catch {
      scheduleReconnect()
      return
    }

    socket.onopen = () => {
      retries = 0
      retryCount.value = 0
      status.value = 'open'
    }

    socket.onmessage = (event) => {
      if (typeof onMessage === 'function') onMessage(String(event.data))
    }

    socket.onerror = () => {
      // 出错后浏览器一般会紧接着触发 onclose，这里不重复处理
    }

    socket.onclose = () => {
      socket = null
      if (manualClosed) {
        status.value = 'closed'
        return
      }
      scheduleReconnect()
    }
  }

  function send(text) {
    if (!socket || socket.readyState !== WebSocket.OPEN) return false
    socket.send(text)
    return true
  }

  function close() {
    manualClosed = true
    clearTimer()
    retries = 0
    retryCount.value = 0
    if (socket) {
      socket.onopen = null
      socket.onmessage = null
      socket.onerror = null
      socket.onclose = null
      socket.close()
      socket = null
    }
    status.value = 'closed'
  }

  // 网络恢复时立刻重连，不用等退避时间
  function handleOnline() {
    if (!manualClosed && (!socket || socket.readyState !== WebSocket.OPEN)) {
      retries = 0
      connect()
    }
  }

  window.addEventListener('online', handleOnline)

  onBeforeUnmount(() => {
    window.removeEventListener('online', handleOnline)
    close()
  })

  return { status, retryCount, connect, send, close }
}
