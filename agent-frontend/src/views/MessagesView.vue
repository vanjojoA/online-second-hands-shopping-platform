<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Refresh } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getConversations } from '@/api/message'

const router = useRouter()

const conversations = ref([])
const loading = ref(false)

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

async function load() {
  loading.value = true
  try {
    const res = await getConversations()
    conversations.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function openChat(item) {
  router.push({
    name: 'chat',
    params: { friendId: item.friendId },
    query: { name: item.friendNickname || '' }
  })
}

onMounted(load)
</script>

<template>
  <div class="messages-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>消息</h1>
          <p>买家卖家的私信都会汇总在这里，点击即可继续聊天</p>
        </div>
        <div class="head-right">
          <el-button :icon="HomeFilled" @click="router.push('/products')">返回主页</el-button>
          <el-button circle :icon="Refresh" :loading="loading" @click="load" />
        </div>
      </div>

      <el-card class="list-card" shadow="never">
        <div v-loading="loading" class="list-body">
          <el-empty
            v-if="!loading && conversations.length === 0"
            description="还没有私信，去商品详情页联系卖家吧"
          >
            <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
          </el-empty>

          <ul v-else class="conv-list">
            <li v-for="item in conversations" :key="item.friendId" class="conv-item" @click="openChat(item)">
              <el-badge :value="item.unreadCount" :max="99" :hidden="!item.unreadCount" class="conv-badge">
                <el-avatar :size="46" class="conv-avatar">
                  {{ (item.friendNickname || '对').slice(0, 1) }}
                </el-avatar>
              </el-badge>

              <div class="conv-main">
                <div class="conv-top">
                  <span class="conv-name">{{ item.friendNickname || `用户 ${item.friendId}` }}</span>
                  <span class="conv-time">{{ formatTime(item.lastTime) }}</span>
                </div>
                <div class="conv-last">{{ item.lastContent || '（暂无消息）' }}</div>
              </div>

              <el-icon class="conv-arrow"><ArrowRight /></el-icon>
            </li>
          </ul>
        </div>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.messages-page {
  min-height: 100vh;
}

.content {
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-head h1 {
  font-size: 24px;
  font-weight: 800;
}

.page-head p {
  margin-top: 6px;
  font-size: 13px;
  color: var(--sp-muted);
}

.list-card {
  margin-top: 18px;
  border-radius: 20px;
  border: 1px solid var(--sp-border);
}

.list-card :deep(.el-card__body) {
  padding: 8px 6px;
}

.list-body {
  min-height: 220px;
}

.conv-list {
  display: flex;
  flex-direction: column;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.conv-item:hover {
  background: #f6f7fc;
}

.conv-badge :deep(.el-badge__content) {
  border: none;
}

.conv-avatar {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.conv-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--sp-text);
}

.conv-time {
  font-size: 12px;
  color: #a4aab8;
  flex-shrink: 0;
}

.conv-last {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sp-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-arrow {
  color: #c6cbda;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .content {
    padding: 16px 16px 36px;
  }
}
</style>
