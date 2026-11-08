<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Refresh } from '@element-plus/icons-vue'
import { auditProduct, getPendingProducts } from '@/api/admin'
import { resolveImageUrl } from '@/utils/url'

const router = useRouter()

const products = ref([])
const loading = ref(false)
const actingId = ref(null)

function viewDetail(item) {
  router.push({ name: 'product-detail', params: { id: item.id } })
}

function imageOf(item) {
  return resolveImageUrl(item.imageUrls)
}

function priceText(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function timeText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    const res = await getPendingProducts()
    products.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function audit(item, pass) {
  const label = pass === 1 ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确定${label}商品「${item.title}」吗？${pass === 0 ? '驳回后商品会被下架。' : ''}`,
      '商品审核',
      { type: pass === 1 ? 'success' : 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  actingId.value = item.id
  try {
    const res = await auditProduct(item.id, pass)
    ElMessage.success(res.message || `已${label}`)
    products.value = products.value.filter((p) => p.id !== item.id)
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    actingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-products">
    <div class="page-head">
      <div>
        <h2>商品审核</h2>
        <p>审核通过后商品才会出现在集市列表，驳回会通知卖家</p>
      </div>
      <div class="head-right">
        <el-tag type="warning" effect="plain">待审核 {{ products.length }} 件</el-tag>
        <el-button circle :icon="Refresh" :loading="loading" @click="load" />
      </div>
    </div>

    <div v-loading="loading" class="list-area">
      <el-empty v-if="!loading && products.length === 0" description="暂无待审核商品" />

      <div v-else class="list">
        <el-card v-for="item in products" :key="item.id" class="row-card" shadow="never">
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
                <span class="id">商品 #{{ item.id }}</span>
              </div>
            </div>

            <div class="actions" @click.stop>
              <el-button size="small" @click="viewDetail(item)">查看详情</el-button>
              <el-button
                type="success"
                plain
                :loading="actingId === item.id"
                @click="audit(item, 1)"
              >
                通过
              </el-button>
              <el-button
                type="danger"
                plain
                :disabled="actingId === item.id"
                @click="audit(item, 0)"
              >
                驳回
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h2 {
  font-size: 20px;
  font-weight: 800;
}

.page-head p {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--sp-muted);
}

.head-right {
  display: flex;
  align-items: center;
  gap: 12px;
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
  width: 92px;
  height: 92px;
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

.desc {
  margin-top: 5px;
  font-size: 12.5px;
  color: var(--sp-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  margin-top: 9px;
  display: flex;
  align-items: baseline;
  gap: 14px;
  font-size: 12px;
  color: #a4aab8;
}

.meta .price {
  font-size: 17px;
  font-weight: 700;
  color: var(--sp-price);
}

.actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .row {
    flex-wrap: wrap;
  }

  .actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
