<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Refresh } from '@element-plus/icons-vue'
import { getHandledReports, getPendingReports, handleReport } from '@/api/admin'
import { getProductDetail } from '@/api/product'
import { resolveImageUrl } from '@/utils/url'

const router = useRouter()

const pendingList = ref([])
const handledList = ref([])
const productMap = ref({})
const loading = ref(false)
const actingId = ref(null)
const activeTab = ref('pending')

const current = computed(() =>
  activeTab.value === 'pending' ? pendingList.value : handledList.value
)

function timeText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    const [pendingRes, handledRes] = await Promise.all([getPendingReports(), getHandledReports()])
    pendingList.value = Array.isArray(pendingRes.data) ? pendingRes.data : []
    handledList.value = Array.isArray(handledRes.data) ? handledRes.data : []

    // 举报只存了 targetId，这里把涉及到的商品信息都拉出来展示
    const ids = [...new Set([...pendingList.value, ...handledList.value]
      .filter((item) => Number(item.targetType) === 1)
      .map((item) => item.targetId))]
    const map = {}
    await Promise.all(
      ids.map(async (id) => {
        try {
          const detail = await getProductDetail(id)
          map[id] = detail.data?.product || null
        } catch {
          map[id] = null
        }
      })
    )
    productMap.value = map
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function handle(item, action) {
  const takeDown = action === 1
  try {
    await ElMessageBox.confirm(
      takeDown
        ? '判定违规后会下架该商品，并给卖家发送系统通知。确定继续吗？'
        : '驳回举报不会修改商品状态，也不会给卖家发通知。确定驳回吗？',
      takeDown ? '判定违规并下架' : '驳回举报',
      {
        type: takeDown ? 'warning' : 'info',
        confirmButtonText: takeDown ? '处理并下架' : '确认驳回',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  actingId.value = item.id
  try {
    const res = await handleReport(item.id, action)
    ElMessage.success(res.message || (takeDown ? '举报已处理' : '举报已驳回'))
    await load()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    actingId.value = null
  }
}

function viewProduct(product) {
  if (!product?.id) return
  router.push({ name: 'product-detail', params: { id: product.id } })
}

function imageOf(product) {
  return product ? resolveImageUrl(product.imageUrls) : ''
}

onMounted(load)
</script>

<template>
  <div class="admin-reports">
    <div class="page-head">
      <div>
        <h2>举报处理</h2>
        <p>待处理举报可判定违规下架或驳回；已处理记录可在「已处理」中回看</p>
      </div>
      <div class="head-right">
        <el-button circle :icon="Refresh" :loading="loading" @click="load" />
      </div>
    </div>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane :label="`待处理 (${pendingList.length})`" name="pending" />
      <el-tab-pane :label="`已处理 (${handledList.length})`" name="handled" />
    </el-tabs>

    <div v-loading="loading" class="list-area">
      <el-empty
        v-if="!loading && current.length === 0"
        :description="activeTab === 'pending' ? '暂无待处理举报' : '还没有处理过的举报'"
      />

      <div v-else class="list">
        <el-card v-for="item in current" :key="item.id" class="report-card" shadow="never">
          <div class="report-head">
            <div class="report-title">
              <el-tag type="danger" size="small" effect="dark">举报 #{{ item.id }}</el-tag>
              <span class="target-type">
                {{ Number(item.targetType) === 1 ? '商品举报' : '用户举报' }}
              </span>
              <el-tag v-if="activeTab === 'handled'" type="info" size="small" effect="plain">
                已处理
              </el-tag>
            </div>
            <span class="time">{{ timeText(item.createTime) }}</span>
          </div>

          <p class="reason">{{ item.reason }}</p>

          <div class="target">
            <template v-if="productMap[item.targetId]">
              <div class="target-thumb" @click="viewProduct(productMap[item.targetId])">
                <el-image
                  v-if="imageOf(productMap[item.targetId])"
                  :src="imageOf(productMap[item.targetId])"
                  fit="cover"
                  class="target-img"
                >
                  <template #error>
                    <div class="thumb-empty"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div v-else class="thumb-empty"><el-icon><Picture /></el-icon></div>
              </div>
              <div class="target-info">
                <div class="target-title" @click="viewProduct(productMap[item.targetId])">
                  {{ productMap[item.targetId].title }}
                </div>
                <div class="target-meta">
                  ¥{{ Number(productMap[item.targetId].price || 0).toFixed(2) }} · 商品 #{{
                    productMap[item.targetId].id
                  }}
                </div>
              </div>
            </template>
            <template v-else>
              <div class="target-info">
                <div class="target-title">商品 #{{ item.targetId }}</div>
                <div class="target-meta">未能加载商品信息</div>
              </div>
            </template>

            <div class="meta-right">
              <span>举报人 #{{ item.reporterId }}</span>
            </div>
          </div>

          <div v-if="activeTab === 'pending'" class="actions">
            <el-button
              type="danger"
              plain
              :loading="actingId === item.id"
              @click="handle(item, 1)"
            >
              处理并下架商品
            </el-button>
            <el-button plain :disabled="actingId === item.id" @click="handle(item, 0)">
              驳回举报
            </el-button>
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
  margin-bottom: 12px;
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

.tabs {
  margin-top: 4px;
}

.tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.list-area {
  min-height: 200px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.report-card {
  border-radius: 16px;
  border: 1px solid var(--sp-border);
}

.report-card :deep(.el-card__body) {
  padding: 16px 18px;
}

.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.target-type {
  font-size: 13px;
  font-weight: 600;
  color: var(--sp-text);
}

.time {
  font-size: 12px;
  color: #a4aab8;
}

.reason {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #3c4354;
  background: #fbfbff;
  border: 1px dashed var(--sp-border);
  border-radius: 10px;
  padding: 10px 12px;
}

.target {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.target-thumb {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  overflow: hidden;
  background: #f4f6fc;
  flex-shrink: 0;
  cursor: pointer;
}

.target-img {
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
}

.target-info {
  flex: 1;
  min-width: 0;
}

.target-title {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.target-title:hover {
  color: var(--sp-primary);
}

.target-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--sp-muted);
}

.meta-right {
  font-size: 12px;
  color: #a4aab8;
  flex-shrink: 0;
}

.actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 640px) {
  .target {
    flex-wrap: wrap;
  }

  .meta-right {
    width: 100%;
  }
}
</style>
