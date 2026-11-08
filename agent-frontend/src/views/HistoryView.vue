<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, HomeFilled, Picture } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { clearBrowseHistory, deleteBrowseHistory, getBrowseHistory } from '@/api/history'
import { resolveImageUrl } from '@/utils/url'

const router = useRouter()
const list = ref([])
const loading = ref(false)

function imageOf(item) {
  return resolveImageUrl(item.imageUrls)
}

function priceText(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function viewedText(value) {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  return text.slice(0, 16)
}

function openDetail(item) {
  router.push({ name: 'product-detail', params: { id: item.productId } })
}

async function load() {
  loading.value = true
  try {
    const res = await getBrowseHistory()
    list.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function remove(item) {
  try {
    await deleteBrowseHistory(item.id)
    list.value = list.value.filter((entry) => entry.id !== item.id)
    ElMessage.success('已删除该浏览记录')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空全部浏览记录吗？清空后其他设备也会同步消失。', '清空浏览记录', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await clearBrowseHistory()
    list.value = []
    ElMessage.success('浏览记录已清空')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

onMounted(load)
</script>

<template>
  <div class="history-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>浏览记录</h1>
          <p>最近看过的商品会记录在这里，登录同一账号可在其他设备同步</p>
        </div>
        <div class="head-right">
          <el-button :icon="HomeFilled" @click="router.push('/products')">返回主页</el-button>
          <el-button :icon="Delete" :disabled="list.length === 0" @click="clearAll">清空</el-button>
        </div>
      </div>

      <div v-loading="loading" class="list-area">
        <el-empty v-if="!loading && list.length === 0" description="还没有浏览记录，去商品列表逛逛吧">
          <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
        </el-empty>

        <div v-else class="list">
          <el-card v-for="item in list" :key="item.id" class="row-card" shadow="hover">
            <div class="row">
              <div class="thumb" @click="openDetail(item)">
                <el-image v-if="imageOf(item)" :src="imageOf(item)" fit="cover" class="thumb-img">
                  <template #error>
                    <div class="thumb-empty"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div v-else class="thumb-empty"><el-icon><Picture /></el-icon></div>
              </div>

              <div class="info">
                <h3 class="title" @click="openDetail(item)">{{ item.title }}</h3>
                <div class="meta">
                  <span class="price">¥{{ priceText(item.price) }}</span>
                  <span class="time">浏览于 {{ viewedText(item.viewedAt) }}</span>
                </div>
              </div>

              <div class="actions">
                <el-button size="small" @click="openDetail(item)">查看</el-button>
                <el-button size="small" type="danger" plain @click="remove(item)">删除</el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.history-page {
  min-height: 100vh;
}

.content {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
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
  gap: 10px;
}

.list-area {
  min-height: 200px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.row-card {
  border-radius: 16px;
  border: 1px solid var(--sp-border);
}

.row-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.thumb {
  width: 86px;
  height: 86px;
  border-radius: 12px;
  overflow: hidden;
  background: #f4f6fc;
  flex-shrink: 0;
  cursor: pointer;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.thumb-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c2c8d6;
  font-size: 22px;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 15.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.title:hover {
  color: var(--sp-primary);
}

.meta {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: var(--sp-price);
}

.time {
  font-size: 12px;
  color: #a4aab8;
}

.actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .content {
    padding: 16px 16px 36px;
  }

  .row {
    flex-wrap: wrap;
  }

  .actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
