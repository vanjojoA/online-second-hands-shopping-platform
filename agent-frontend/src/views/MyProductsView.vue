<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, HomeFilled } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getMyProducts, updateProductStatus } from '@/api/product'
import { resolveImageUrl } from '@/utils/url'

const router = useRouter()

const products = ref([])
const loading = ref(false)
const activeTab = ref('all')
const updatingId = ref(null)

const counts = computed(() => {
  const result = { onSale: 0, sold: 0, pending: 0 }
  for (const item of products.value) {
    if (item.status === 1) result.onSale += 1
    else if (item.status === 2) result.sold += 1
    else result.pending += 1
  }
  return result
})

const filtered = computed(() => {
  if (activeTab.value === 'all') return products.value
  if (activeTab.value === 'sale') return products.value.filter((item) => item.status === 1)
  if (activeTab.value === 'sold') return products.value.filter((item) => item.status === 2)
  return products.value.filter((item) => item.status !== 1 && item.status !== 2)
})

function statusMeta(status) {
  if (status === 1) return { text: '在售', type: 'success' }
  if (status === 2) return { text: '已售出', type: 'info' }
  return { text: '待审核', type: 'warning' }
}

function priceText(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function timeText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function imageOf(item) {
  return resolveImageUrl(item.imageUrls)
}

async function load() {
  loading.value = true
  try {
    const res = await getMyProducts()
    products.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function viewDetail(item) {
  // 带上来源，详情页的返回按钮才能回到「我的商品」
  router.push({
    name: 'product-detail',
    params: { id: item.id },
    query: { from: 'my-products' }
  })
}

function editProduct(item) {
  router.push({ name: 'product-edit', params: { id: item.id } })
}

async function markSold(item) {
  try {
    await ElMessageBox.confirm(
      `确定把「${item.title}」标记为已售出吗？标记后会从在售列表中移除。`,
      '标记已售出',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }

  updatingId.value = item.id
  try {
    const res = await updateProductStatus(item.id)
    item.status = 2
    ElMessage.success(res.message || '已标记为售出')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    updatingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="my-products-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>我的商品</h1>
          <p>管理你发布的商品，成交后记得标记为已售出</p>
        </div>
        <div class="head-right">
          <el-button :icon="HomeFilled" @click="router.push('/products')">返回主页</el-button>
          <el-button type="primary" :icon="Plus" @click="router.push('/publish')">
            发布新商品
          </el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="tabs">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane :label="`在售 (${counts.onSale})`" name="sale" />
        <el-tab-pane :label="`已售出 (${counts.sold})`" name="sold" />
        <el-tab-pane :label="`待审核 (${counts.pending})`" name="pending" />
      </el-tabs>

      <div v-loading="loading" class="list-area">
        <el-empty
          v-if="!loading && filtered.length === 0"
          :description="activeTab === 'all' ? '你还没有发布过商品' : '这个分类下暂时没有商品'"
        >
          <el-button type="primary" @click="router.push('/publish')">去发布</el-button>
        </el-empty>

        <div v-else class="product-list">
          <el-card v-for="item in filtered" :key="item.id" class="row-card" shadow="hover">
            <div class="row">
              <div class="thumb" @click="viewDetail(item)">
                <el-image v-if="imageOf(item)" :src="imageOf(item)" fit="cover" class="thumb-img">
                  <template #error>
                    <div class="thumb-empty"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div v-else class="thumb-empty"><el-icon><Picture /></el-icon></div>
              </div>

              <div class="info">
                <h3 class="title" @click="viewDetail(item)">{{ item.title || '未命名商品' }}</h3>
                <p class="desc">{{ item.description || '没有填写商品描述' }}</p>
                <div class="meta">
                  <span class="price">¥{{ priceText(item.price) }}</span>
                  <span class="time">发布于 {{ timeText(item.createTime) }}</span>
                </div>
              </div>

              <div class="actions">
                <el-tag :type="statusMeta(item.status).type" effect="plain" class="status">
                  {{ statusMeta(item.status).text }}
                </el-tag>
                <el-button size="small" @click="viewDetail(item)">查看</el-button>
                <el-button
                  v-if="item.status === 0 || item.status === 1"
                  size="small"
                  @click="editProduct(item)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="item.status === 1"
                  size="small"
                  type="primary"
                  plain
                  :loading="updatingId === item.id"
                  @click="markSold(item)"
                >
                  标记已售出
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
.my-products-page {
  min-height: 100vh;
}

.content {
  width: 100%;
  max-width: 1000px;
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

.tabs {
  margin-top: 8px;
}

.tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.list-area {
  min-height: 220px;
}

.product-list {
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
  width: 96px;
  height: 96px;
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
  font-size: 24px;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.title:hover {
  color: var(--sp-primary);
}

.desc {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--sp-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.status {
  border-radius: 8px;
}

@media (max-width: 640px) {
  .content {
    padding: 16px 16px 36px;
  }

  .row {
    align-items: flex-start;
  }

  .thumb {
    width: 72px;
    height: 72px;
  }

  .actions {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .row {
    flex-wrap: wrap;
  }
}
</style>
