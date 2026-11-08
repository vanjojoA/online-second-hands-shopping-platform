<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Check, HomeFilled, Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { getNotifications, markNotificationRead } from '@/api/notification'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const activeTab = ref('all')
const markingAll = ref(false)

const unreadCount = computed(() => list.value.filter((item) => Number(item.isRead) === 0).length)

const filtered = computed(() =>
  activeTab.value === 'unread' ? list.value.filter((item) => Number(item.isRead) === 0) : list.value
)

function timeText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    const res = await getNotifications()
    list.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function markRead(item) {
  if (Number(item.isRead) === 1) return
  try {
    await markNotificationRead(item.id)
    item.isRead = 1
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

async function markAllRead() {
  const unread = list.value.filter((item) => Number(item.isRead) === 0)
  if (unread.length === 0) return
  markingAll.value = true
  try {
    await Promise.all(unread.map((item) => markNotificationRead(item.id)))
    list.value.forEach((item) => {
      item.isRead = 1
    })
    ElMessage.success('已全部标记为已读')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    markingAll.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="notifications-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>系统通知</h1>
          <p>商品审核结果、违规下架等平台消息都会出现在这里</p>
        </div>
        <div class="head-right">
          <el-button :icon="HomeFilled" @click="router.push('/products')">返回主页</el-button>
          <el-button :icon="Check" :loading="markingAll" :disabled="unreadCount === 0" @click="markAllRead">
            全部已读
          </el-button>
          <el-button circle :icon="Refresh" :loading="loading" @click="load" />
        </div>
      </div>

      <el-tabs v-model="activeTab" class="tabs">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane :label="`未读 (${unreadCount})`" name="unread" />
      </el-tabs>

      <div v-loading="loading" class="list-area">
        <el-empty
          v-if="!loading && filtered.length === 0"
          :description="activeTab === 'unread' ? '没有未读通知' : '还没有收到系统通知'"
        />

        <div v-else class="notice-list">
          <div
            v-for="item in filtered"
            :key="item.id"
            class="notice-item"
            :class="{ unread: Number(item.isRead) === 0 }"
            @click="markRead(item)"
          >
            <div class="notice-icon"><el-icon><Bell /></el-icon></div>
            <div class="notice-main">
              <div class="notice-top">
                <span class="notice-title">{{ item.title }}</span>
                <span class="notice-time">{{ timeText(item.createTime) }}</span>
              </div>
              <p class="notice-content">{{ item.content }}</p>
            </div>
            <span v-if="Number(item.isRead) === 0" class="dot" title="未读"></span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.notifications-page {
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

.page-head h1 {
  font-size: 24px;
  font-weight: 800;
}

.page-head p {
  margin-top: 6px;
  font-size: 13px;
  color: var(--sp-muted);
}

.head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tabs {
  margin-top: 8px;
}

.tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.list-area {
  min-height: 200px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.notice-item:hover {
  box-shadow: 0 10px 24px rgba(31, 36, 48, 0.08);
}

.notice-item.unread {
  border-color: #d8d5fb;
  background: linear-gradient(180deg, #fbfaff, #ffffff);
}

.notice-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  flex-shrink: 0;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--sp-text);
}

.notice-time {
  font-size: 12px;
  color: #a4aab8;
  flex-shrink: 0;
}

.notice-content {
  margin-top: 5px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--sp-muted);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  margin-top: 14px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .content {
    padding: 16px 16px 36px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
