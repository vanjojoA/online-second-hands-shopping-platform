<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, Picture, Warning } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getProductDetail } from '@/api/product'
import { submitReport } from '@/api/report'
import { getUser } from '@/utils/auth'
import { recordBrowse } from '@/api/history'
import { resolveImageUrl } from '@/utils/url'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const notFound = ref(false)
const product = ref(null)
const seller = ref(null)
const activeImage = ref(0)

const images = computed(() => {
  const raw = product.value?.imageUrls
  if (!raw) return []
  return String(raw)
    .split(',')
    .map((url) => resolveImageUrl(url))
    .filter(Boolean)
})

const priceText = computed(() => {
  const value = Number(product.value?.price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
})

const statusMeta = computed(() => {
  switch (product.value?.status) {
    case 1:
      return { text: '在售', type: 'success' }
    case 2:
      return { text: '已售出', type: 'info' }
    default:
      return { text: '待审核', type: 'warning' }
  }
})

const timeText = computed(() => {
  const time = product.value?.createTime
  return time ? String(time).replace('T', ' ').slice(0, 16) : '——'
})

const isMine = computed(() => {
  const me = getUser()
  if (!me.userId || !product.value?.userId) return false
  return Number(me.userId) === Number(product.value.userId)
})

// 从「我的商品」点进来的，返回时回到我的商品页
const fromMyProducts = computed(() => route.query.from === 'my-products')
const backTarget = computed(() => (fromMyProducts.value ? '/my-products' : '/products'))
const backLabel = computed(() => (fromMyProducts.value ? '返回我的商品' : '返回列表'))

const sellerText = computed(() => (seller.value?.nickname || '卖').slice(0, 1))

function contactSeller() {
  const id = seller.value?.id
  if (!id) {
    ElMessage.warning('暂时无法获取卖家信息')
    return
  }
  if (isMine.value) {
    ElMessage.info('这是你自己发布的商品，不能和自己聊天')
    return
  }
  // 跳到私信页，顺便把对方昵称带过去用于展示
  router.push({
    name: 'chat',
    params: { friendId: id },
    query: { name: seller.value.nickname || '' }
  })
}

/* ------------------------------ 举报商品 ------------------------------ */
const reportVisible = ref(false)
const reportReason = ref('')
const reporting = ref(false)

function openReport() {
  reportReason.value = ''
  reportVisible.value = true
}

async function handleReport() {
  const reason = reportReason.value.trim()
  if (reason.length < 5 || reason.length > 200) {
    ElMessage.warning('举报原因需在 5 ~ 200 字之间')
    return
  }

  reporting.value = true
  try {
    const res = await submitReport({
      targetType: 1,
      targetId: product.value.id,
      reason
    })
    ElMessage.success(res.message || '举报已提交')
    reportVisible.value = false
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    reporting.value = false
  }
}

async function load() {
  loading.value = true
  notFound.value = false
  product.value = null
  seller.value = null
  try {
    const res = await getProductDetail(route.params.id)
    product.value = res.data?.product || null
    seller.value = res.data?.seller || null
    activeImage.value = 0
    if (!product.value) notFound.value = true
    else recordBrowse(product.value.id).catch(() => {}) // 记录浏览历史（后端存库，可跨设备同步）
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
</script>

<template>
  <div class="detail-page">
    <AppHeader />

    <main class="content">
      <div class="back-row">
        <el-button link :icon="ArrowLeft" @click="router.push(backTarget)">
          {{ backLabel }}
        </el-button>
      </div>

      <el-skeleton v-if="loading" class="skeleton" :rows="8" animated />

      <el-result
        v-else-if="notFound"
        icon="warning"
        title="商品不存在"
        sub-title="该商品可能已被删除或者已经下架"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/products')">回到商品列表</el-button>
        </template>
      </el-result>

      <div v-else-if="product" class="detail-grid">
        <!-- 左侧图片 -->
        <section class="gallery card">
          <div class="main-image">
            <el-image v-if="images.length" :src="images[activeImage]" fit="contain" class="main-img">
              <template #error>
                <div class="image-empty">
                  <el-icon><Picture /></el-icon>
                  <span>图片加载失败</span>
                </div>
              </template>
            </el-image>
            <div v-else class="image-empty">
              <el-icon><Picture /></el-icon>
              <span>卖家还没有上传图片</span>
            </div>
          </div>

          <div v-if="images.length > 1" class="thumbs">
            <button
              v-for="(img, index) in images"
              :key="img + index"
              type="button"
              class="thumb-btn"
              :class="{ active: index === activeImage }"
              @click="activeImage = index"
            >
              <el-image :src="img" fit="cover" class="thumb-img" />
            </button>
          </div>
        </section>

        <!-- 右侧信息 -->
        <section class="info">
          <div class="card info-card">
            <div class="tags">
              <el-tag :type="statusMeta.type" effect="dark">{{ statusMeta.text }}</el-tag>
              <el-tag v-if="isMine" type="warning" effect="plain">我发布的</el-tag>
            </div>

            <h1 class="title">{{ product.title || '未命名商品' }}</h1>

            <div class="price"><em>¥</em>{{ priceText }}</div>

            <p class="desc">{{ product.description || '卖家还没有填写商品描述' }}</p>

            <el-divider />

            <ul class="meta-list">
              <li>
                <span>发布时间</span>
                <b>{{ timeText }}</b>
              </li>
              <li>
                <span>商品编号</span>
                <b>#{{ product.id }}</b>
              </li>
              <li>
                <span>交易方式</span>
                <b>校园面交 / 自提</b>
              </li>
            </ul>

            <div class="actions">
              <el-button type="primary" size="large" :icon="ChatDotRound" @click="contactSeller">
                联系卖家
              </el-button>
              <el-button size="large" :icon="ArrowLeft" @click="router.push(backTarget)">
                {{ backLabel }}
              </el-button>
              <el-button
                v-if="!isMine"
                size="large"
                type="danger"
                plain
                :icon="Warning"
                @click="openReport"
              >
                举报
              </el-button>
            </div>
          </div>

          <!-- 卖家信息 -->
          <div class="card seller-card">
            <el-avatar :size="46" class="seller-avatar">{{ sellerText }}</el-avatar>
            <div class="seller-info">
              <div class="seller-name">
                {{ seller?.nickname || '未知卖家' }}
                <el-tag v-if="seller?.role === 1" type="danger" size="small" effect="plain">
                  管理员
                </el-tag>
              </div>
              <div class="seller-meta">
                学号 {{ seller?.studentId || '——' }} · 加入于 {{ (seller?.createTime || '').slice(0, 10) || '——' }}
              </div>
            </div>
            <el-tag type="info" effect="plain" class="seller-tag">卖家</el-tag>
          </div>
        </section>
      </div>
    </main>

    <!-- 举报商品弹窗 -->
    <el-dialog v-model="reportVisible" title="举报商品" width="440px" :close-on-click-modal="false">
      <p class="report-tip">请描述违规原因，管理员会尽快核实处理。</p>
      <el-input
        v-model="reportReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        resize="none"
        placeholder="例如：商品描述与实际不符、涉嫌诈骗、违禁品等（5-200 字）"
      />
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" :loading="reporting" @click="handleReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
}

.content {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 24px 56px;
}

.back-row {
  margin-bottom: 12px;
}

.skeleton {
  padding: 40px;
  background: #fff;
  border-radius: 20px;
}

.card {
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  gap: 22px;
}

/* 图片区 */
.gallery {
  padding: 18px;
}

.main-image {
  border-radius: 14px;
  overflow: hidden;
  background: #f4f6fc;
  height: 420px;
}

.main-img {
  width: 100%;
  height: 100%;
}

.image-empty {
  height: 100%;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #b3b9c9;
  font-size: 13px;
  background: linear-gradient(135deg, #f4f6fc, #eaeef9);
}

.image-empty .el-icon {
  font-size: 40px;
}

.thumbs {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.thumb-btn {
  width: 74px;
  height: 74px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 10px;
  overflow: hidden;
  background: #f4f6fc;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.thumb-btn.active {
  border-color: var(--sp-primary);
}

.thumb-img {
  width: 100%;
  height: 100%;
}

/* 信息区 */
.info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card {
  padding: 26px 26px 24px;
}

.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.title {
  font-size: 24px;
  line-height: 1.45;
  font-weight: 700;
  word-break: break-word;
}

.price {
  margin-top: 14px;
  color: var(--sp-price);
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.price em {
  font-style: normal;
  font-size: 18px;
  margin-right: 2px;
}

.desc {
  margin-top: 18px;
  font-size: 14px;
  line-height: 1.9;
  color: #4a5164;
  white-space: pre-wrap;
  word-break: break-word;
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
}

.meta-list li {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.meta-list span {
  color: var(--sp-muted);
}

.meta-list b {
  font-weight: 500;
  color: var(--sp-text);
}

.actions {
  margin-top: 22px;
  display: flex;
  gap: 12px;
}

.report-tip {
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--sp-muted);
}

.actions :deep(.el-button) {
  flex: 1;
  border-radius: 10px;
}

/* 卖家卡片 */
.seller-card {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.seller-avatar {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 18px;
}

.seller-info {
  flex: 1;
  min-width: 0;
}

.seller-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.seller-meta {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--sp-muted);
}

.seller-tag {
  border-radius: 8px;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .main-image {
    height: 300px;
  }

  .title {
    font-size: 20px;
  }

  .price {
    font-size: 28px;
  }
}

@media (max-width: 640px) {
  .content {
    padding: 12px 16px 40px;
  }

  .info-card {
    padding: 20px;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
