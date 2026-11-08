<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, ChatDotRound, Clock, Goods, Plus, Setting, SwitchButton, User as UserIcon } from '@element-plus/icons-vue'
import { userState } from '@/stores/user'
import { clearAuth, updateUser } from '@/utils/auth'
import { getUserInfo } from '@/api/user'
import { getUnreadCount } from '@/api/message'
import { getNotifications } from '@/api/notification'
import { resolveImageUrl } from '@/utils/url'

const router = useRouter()
const route = useRoute()

const unread = ref(0)
const noticeUnread = ref(0)
let timer = null

const avatarUrl = computed(() => resolveImageUrl(userState.avatar))

const avatarText = computed(() => {
  const name = userState.nickname || ''
  return name ? name.slice(0, 1) : '同'
})

const isAdmin = computed(() => Number(userState.role) === 1)

async function refreshChatUnread() {
  try {
    const res = await getUnreadCount()
    unread.value = Number(res.data) || 0
  } catch {
    // 401 时拦截器会统一处理
  }
}

async function refreshNoticeUnread() {
  try {
    const res = await getNotifications()
    const list = Array.isArray(res.data) ? res.data : []
    noticeUnread.value = list.filter((item) => Number(item.isRead) === 0).length
  } catch {
    // 忽略
  }
}

function refreshBadges() {
  refreshChatUnread()
  refreshNoticeUnread()
}

function handleCommand(command) {
  if (command === 'logout') {
    clearAuth()
    ElMessage.success('已退出登录')
    router.replace('/login')
  } else if (command === 'my-products') {
    router.push('/my-products')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'history') {
    router.push('/history')
  } else if (command === 'admin') {
    router.push('/admin')
  }
}

onMounted(async () => {
  refreshBadges()
  // 轮询未读数：私信 + 系统通知
  timer = setInterval(refreshBadges, 20000)

  try {
    const res = await getUserInfo()
    const info = res.data || {}
    updateUser({
      userId: info.id,
      nickname: info.nickname || '',
      avatar: info.avatar || '',
      role: info.role
    })
  } catch {
    // 401 已由 axios 拦截器统一处理
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

// 路由变化（例如从聊天页返回）时刷新未读
watch(
  () => route.fullPath,
  () => refreshBadges()
)
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/products" class="brand">
        <span class="brand-mark">🛒</span>
        <span class="brand-name">校园二手集市</span>
      </router-link>

      <div class="header-right">
        <el-button type="primary" round :icon="Plus" @click="router.push('/publish')">
          发布商品
        </el-button>

        <el-badge :value="noticeUnread" :max="99" :hidden="noticeUnread <= 0" class="icon-badge">
          <el-button circle :icon="Bell" class="icon-btn" title="系统通知" @click="router.push('/notifications')" />
        </el-badge>

        <el-badge :value="unread" :max="99" :hidden="unread <= 0" class="icon-badge">
          <el-button circle :icon="ChatDotRound" class="icon-btn" title="私信" @click="router.push('/messages')" />
        </el-badge>

        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-chip">
            <el-avatar :size="30" :src="avatarUrl || undefined" class="user-avatar">{{ avatarText }}</el-avatar>
            <span class="user-name">{{ userState.nickname || '同学' }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile" :icon="UserIcon">个人中心</el-dropdown-item>
              <el-dropdown-item command="my-products" :icon="Goods">我的商品</el-dropdown-item>
              <el-dropdown-item command="history" :icon="Clock">浏览记录</el-dropdown-item>
              <el-dropdown-item v-if="isAdmin" command="admin" :icon="Setting">管理员后台</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--sp-border);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.3px;
}

.brand-mark {
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.icon-badge {
  display: inline-flex;
}

.icon-btn {
  color: #4a5164;
}

.icon-badge :deep(.el-badge__content) {
  border: none;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease;
  outline: none;
}

.user-chip:hover {
  background: #f1f3fa;
}

.user-avatar {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 13px;
}

.user-name {
  font-size: 14px;
  color: var(--sp-text);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow {
  color: var(--sp-muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  .header-inner {
    padding: 0 16px;
  }

  .brand-name,
  .user-name {
    display: none;
  }

  .header-right {
    gap: 10px;
  }
}
</style>
