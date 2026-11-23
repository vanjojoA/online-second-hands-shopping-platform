<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete, HomeFilled, Picture, StarFilled } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
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

function statusText(status) {
  if (status === 1) return '在售'
  if (status === 2) return '已售出'
  if (status === 3) return '已下架'
  return '待审核'
}

function statusType(status) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  if (status === 3) return 'danger'
  return 'warning'
}

function openDetail(item) {
  router.push({ name: 'product-detail', params: { id: item.productId } })
}

async function load() {
  loading.value = true
  try {
    const res = await getFavoriteList()
    list.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function remove(item) {
  try {
    await removeFavorite(item.productId)
    list.value = list.value.filter((entry) => entry.productId !== item.productId)
    ElMessage.success('已取消收藏')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

onMounted(load)
</script>

<template>
  <div class="favorites-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>我的收藏</h1>
          <p>收藏的商品都在这里，登录同一账号可跨设备同步</p>
        </div>
        <el-button :icon="HomeFilled" @click="router.push('/products')">返回主页</el-button>
      </div>

      <div v-loading="loading" class="list-area">
        <el-empty v-if="!loading && list.length === 0" description="还没有收藏商品，去列表逛逛吧">
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
                <h3 class="title" @click="openDetail(item)">{{ item.title || '未命名商品' }}</h3>
                <div class="meta">
                  <span class="price">¥{{ priceText(item.price) }}</span>
                  <el-tag :type="statusType(item.status)" size="small" effect="plain">
                    {{ statusText(item.status) }}
                  </el-tag>
                </div>
              </div>

              <div class="actions">
                <el-button size="small" @click="openDetail(item)">查看</el-button>
                <el-button size="small" type="warning" plain :icon="StarFilled" @click="remove(item)">
                  取消收藏
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.favorites-page {
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
  align-items: center;
  gap: 12px;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: var(--sp-price);
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
