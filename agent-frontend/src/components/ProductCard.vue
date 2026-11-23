<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Star, StarFilled } from '@element-plus/icons-vue'
import { toggleFavorite } from '@/api/favorite'
import { getToken } from '@/utils/auth'
import { resolveImageUrl } from '@/utils/url'

const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  favorited: {
    type: Boolean,
    default: false
  },
  canFavorite: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['favorite-changed'])

const router = useRouter()
const localFavorited = ref(props.favorited)
const toggling = ref(false)

watch(
  () => props.favorited,
  (value) => {
    localFavorited.value = value
  }
)

async function onToggleFavorite() {
  if (!getToken()) {
    ElMessage.warning('登录后才能收藏商品')
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (toggling.value) return

  toggling.value = true
  try {
    const res = await toggleFavorite(props.product.id)
    localFavorited.value = Boolean(res.data?.favorited)
    emit('favorite-changed', { id: props.product.id, favorited: localFavorited.value })
    ElMessage.success(localFavorited.value ? '已收藏' : '已取消收藏')
  } catch {
    // 错误提示由拦截器处理
  } finally {
    toggling.value = false
  }
}

const image = computed(() => resolveImageUrl(props.product.imageUrls))

const statusMeta = computed(() => {
  switch (props.product.status) {
    case 1:
      return { text: '在售', type: 'success' }
    case 2:
      return { text: '已售出', type: 'info' }
    default:
      return { text: '待审核', type: 'warning' }
  }
})

const priceText = computed(() => {
  const value = Number(props.product.price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
})

const timeText = computed(() => {
  const time = props.product.createTime
  return time ? String(time).replace('T', ' ').slice(0, 16) : ''
})

function goDetail() {
  if (!props.product.id) return
  router.push({ name: 'product-detail', params: { id: props.product.id } })
}
</script>

<template>
  <el-card
    class="product-card"
    shadow="hover"
    :body-style="{ padding: '0' }"
    tabindex="0"
    @click="goDetail"
    @keyup.enter="goDetail"
  >
    <div class="thumb">
      <el-image v-if="image" :src="image" fit="cover" lazy class="thumb-img">
        <template #placeholder>
          <div class="thumb-fallback">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
        <template #error>
          <div class="thumb-fallback">
            <el-icon><Picture /></el-icon>
            <span>图片加载失败</span>
          </div>
        </template>
      </el-image>
      <div v-else class="thumb-fallback">
        <el-icon><Picture /></el-icon>
        <span>暂无图片</span>
      </div>

      <el-tag :type="statusMeta.type" effect="dark" size="small" class="status-tag">
        {{ statusMeta.text }}
      </el-tag>

      <button
        v-if="canFavorite"
        type="button"
        class="fav-btn"
        :class="{ active: localFavorited }"
        :title="localFavorited ? '取消收藏' : '收藏商品'"
        :disabled="toggling"
        @click.stop="onToggleFavorite"
      >
        <el-icon>
          <StarFilled v-if="localFavorited" />
          <Star v-else />
        </el-icon>
      </button>

      <div class="thumb-hover">查看详情</div>
    </div>

    <div class="card-body">
      <h3 class="title" :title="product.title">{{ product.title || '未命名商品' }}</h3>
      <p class="desc">{{ product.description || '卖家还没有填写商品描述' }}</p>

      <div class="meta">
        <span class="price"><em>¥</em>{{ priceText }}</span>
        <span class="time">{{ timeText }}</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.product-card {
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  outline: none;
}

.product-card:hover,
.product-card:focus-visible {
  transform: translateY(-4px);
  box-shadow: 0 18px 40px rgba(31, 36, 48, 0.12);
}

.thumb {
  position: relative;
  height: 168px;
  background: #f2f4fa;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
  display: block;
  transition: transform 0.35s ease;
}

.product-card:hover .thumb-img {
  transform: scale(1.04);
}

.thumb-fallback {
  height: 168px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #b3b9c9;
  font-size: 12px;
  background: linear-gradient(135deg, #f4f6fc, #eaeef9);
}

.thumb-fallback .el-icon {
  font-size: 30px;
}

.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  border-radius: 6px;
}

.fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #8a90a0;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(31, 36, 48, 0.12);
  transition: color 0.2s ease, transform 0.2s ease;
  z-index: 2;
}

.fav-btn:hover {
  transform: scale(1.08);
  color: #f59e0b;
}

.fav-btn.active {
  color: #f59e0b;
}

.thumb-hover {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  letter-spacing: 1px;
  background: rgba(31, 36, 48, 0.38);
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.product-card:hover .thumb-hover {
  opacity: 1;
}

.card-body {
  padding: 14px 16px 16px;
}

.title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--sp-text);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.desc {
  margin-top: 6px;
  min-height: 38px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--sp-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  margin-top: 12px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.price {
  color: var(--sp-price);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.price em {
  font-style: normal;
  font-size: 13px;
  margin-right: 1px;
}

.time {
  font-size: 12px;
  color: #a4aab8;
}
</style>
