<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, ChatDotRound, Promotion, Refresh } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getMessageHistory, markConversationRead } from '@/api/message'
import { getUser } from '@/utils/auth'
import { useChatSocket } from '@/composables/useChatSocket'

const route = useRoute()
const router = useRouter()

const me = getUser()
const myId = Number(me.userId) || null
const myText = (me.nickname || '我').slice(0, 1)

const friendId = computed(() => Number(route.params.friendId))
const peerName = computed(() => {
  const name = route.query.name
  return typeof name === 'string' && name ? name : `用户 ${friendId.value || ''}`.trim()
})
const peerText = computed(() => (peerName.value || '对').slice(0, 1))

const messages = ref([])
const draft = ref('')
const loading = ref(false)
const listRef = ref(null)

/* ----------------------------- WebSocket ----------------------------- */
const { status, connect, send, close } = useChatSocket({
  userId: myId,
  onMessage: handleIncoming
})

const statusText = computed(() => {
  switch (status.value) {
    case 'open':
      return '已连接 · 消息实时送达'
    case 'connecting':
      return '连接中…'
    case 'reconnecting':
      return '连接断开，正在重连…'
    default:
      return '未连接'
  }
})
const statusClass = computed(() => {
  if (status.value === 'open') return 'ok'
  if (status.value === 'closed') return 'bad'
  return 'warn'
})

function handleIncoming(raw) {
  // 新协议：后端推送 JSON（带 senderId），能区分是谁发来的
  let payload = null
  try {
    payload = JSON.parse(raw)
  } catch {
    payload = null
  }

  if (payload && payload.type === 'message') {
    if (Number(payload.senderId) !== friendId.value) {
      // 不是当前会话的消息：不渲染到当前聊天框，顶栏未读数会在轮询/切页时更新
      return
    }
    messages.value.push({
      key: `push-${payload.messageId || Date.now()}`,
      mine: false,
      content: payload.content,
      time: formatTime(payload.createTime)
    })
    markRead()
    scrollToBottom()
    return
  }

  // 兼容旧版纯文本推送："收到新消息：内容"
  const prefix = '收到新消息：'
  const content = raw.startsWith(prefix) ? raw.slice(prefix.length) : raw
  if (!content) return
  messages.value.push({
    key: `push-${Date.now()}-${Math.random()}`,
    mine: false,
    content,
    time: formatNow()
  })
  markRead()
  scrollToBottom()
}

/* ------------------------------- 已读 ------------------------------- */
let lastReadAt = 0

function markRead(force = false) {
  if (!friendId.value) return
  const now = Date.now()
  if (!force && now - lastReadAt < 1000) return
  lastReadAt = now
  markConversationRead(friendId.value).catch(() => {
    // 已读失败不影响聊天
  })
}

/* ------------------------------- 消息 ------------------------------- */
function formatTime(value) {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})/)
  if (!match) return text.slice(0, 16)
  const [, , month, day, hour, minute] = match
  const now = new Date()
  const isToday = Number(month) === now.getMonth() + 1 && Number(day) === now.getDate()
  return isToday ? `${hour}:${minute}` : `${month}-${day} ${hour}:${minute}`
}

function formatNow() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function normalize(item) {
  return {
    key: `db-${item.id}`,
    mine: Number(item.senderId) === myId,
    content: item.content,
    time: formatTime(item.createTime)
  }
}

async function loadHistory() {
  if (!friendId.value) return
  loading.value = true
  try {
    const res = await getMessageHistory(friendId.value)
    messages.value = (Array.isArray(res.data) ? res.data : []).map(normalize)
    // 打开会话即把对方发来的消息标记为已读
    markRead(true)
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function sendMessage() {
  const content = draft.value.trim()
  if (!content) return

  if (status.value !== 'open') {
    ElMessage.warning('连接已断开，正在自动重连，请稍后再发')
    return
  }

  // 后端约定格式："接收者ID:消息内容"
  const ok = send(`${friendId.value}:${content}`)
  if (!ok) {
    ElMessage.error('发送失败，请重试')
    return
  }

  messages.value.push({
    key: `local-${Date.now()}-${Math.random()}`,
    mine: true,
    content,
    time: formatNow()
  })
  draft.value = ''
  scrollToBottom()
}

function reconnect() {
  connect()
}

/* ------------------------------ 生命周期 ------------------------------ */
watch(
  status,
  (value, oldValue) => {
    // 断线重连成功后，拉一次历史，补齐离线期间的消息
    if (value === 'open' && oldValue === 'reconnecting') loadHistory()
  }
)

watch(friendId, async () => {
  if (!friendId.value) {
    router.replace('/products')
    return
  }
  close()
  messages.value = []
  await loadHistory()
  connect()
})

onMounted(async () => {
  if (!myId) {
    router.replace({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!friendId.value) {
    router.replace('/products')
    return
  }
  await loadHistory()
  connect()
})
</script>

<template>
  <div class="chat-page">
    <AppHeader />

    <main class="chat-wrap">
      <section class="chat-card">
        <header class="chat-head">
          <div class="head-left">
            <el-button link :icon="Back" @click="router.push('/messages')">返回消息</el-button>

            <div class="peer">
              <el-avatar :size="38" class="peer-avatar">{{ peerText }}</el-avatar>
              <div class="peer-info">
                <div class="peer-name">{{ peerName }}</div>
                <div class="peer-status" :class="statusClass">
                  <span class="dot"></span>{{ statusText }}
                </div>
              </div>
            </div>
          </div>

          <el-button v-if="status === 'closed'" size="small" :icon="Refresh" @click="reconnect">
            重新连接
          </el-button>
        </header>

        <div ref="listRef" v-loading="loading" class="msg-list">
          <div v-if="!loading && messages.length === 0" class="empty">
            <el-icon><ChatDotRound /></el-icon>
            <p>还没有聊天记录，先打个招呼吧～</p>
          </div>

          <div v-for="item in messages" :key="item.key" class="msg" :class="{ mine: item.mine }">
            <el-avatar :size="34" class="msg-avatar" :class="{ mine: item.mine }">
              {{ item.mine ? myText : peerText }}
            </el-avatar>
            <div class="msg-main">
              <div class="bubble">{{ item.content }}</div>
              <div class="msg-time">{{ item.time }}</div>
            </div>
          </div>
        </div>

        <footer class="chat-input">
          <el-input
            v-model="draft"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            resize="none"
            maxlength="500"
            placeholder="输入消息，Enter 发送，Shift + Enter 换行"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            :disabled="!draft.trim()"
            @click="sendMessage"
          >
            发送
          </el-button>
        </footer>
      </section>
    </main>
  </div>
</template>

<style scoped>
.chat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-wrap {
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  padding: 18px 24px 28px;
  flex: 1;
  display: flex;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 46px);
  min-height: 480px;
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 20px;
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--sp-border);
  background: rgba(250, 251, 255, 0.9);
}

.head-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.peer {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.peer-avatar {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 15px;
  flex-shrink: 0;
}

.peer-name {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.peer-status {
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--sp-muted);
}

.peer-status .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b3b9c9;
}

.peer-status.ok .dot {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.14);
}

.peer-status.warn .dot {
  background: #f59e0b;
}

.peer-status.bad .dot {
  background: #ef4444;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 22px 22px 8px;
  background:
    radial-gradient(600px 300px at 100% 0%, rgba(79, 70, 229, 0.05), transparent 60%),
    #fbfcff;
}

.empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #a4aab8;
  font-size: 13px;
}

.empty .el-icon {
  font-size: 34px;
  color: #cdd3e1;
}

.msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.msg.mine {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  background: #e8eaf4;
  color: #5b6172;
  font-size: 13px;
}

.msg-avatar.mine {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
}

.msg-main {
  max-width: 68%;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.msg.mine .msg-main {
  align-items: flex-end;
}

.bubble {
  padding: 10px 14px;
  border-radius: 14px;
  border-top-left-radius: 4px;
  background: #fff;
  border: 1px solid var(--sp-border);
  font-size: 14px;
  line-height: 1.65;
  color: var(--sp-text);
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(31, 36, 48, 0.04);
}

.msg.mine .bubble {
  border: none;
  border-radius: 14px;
  border-top-right-radius: 4px;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.22);
}

.msg-time {
  font-size: 11.5px;
  color: #a4aab8;
  padding: 0 4px;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px 14px;
  border-top: 1px solid var(--sp-border);
  background: #fff;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 9px 12px;
  line-height: 1.6;
}

.chat-input :deep(.el-button) {
  height: 38px;
  border-radius: 10px;
  padding: 0 20px;
}

@media (max-width: 640px) {
  .chat-wrap {
    padding: 10px 12px 16px;
  }

  .chat-card {
    height: calc(100vh - 64px - 26px);
    border-radius: 16px;
  }

  .msg-main {
    max-width: 76%;
  }
}
</style>
