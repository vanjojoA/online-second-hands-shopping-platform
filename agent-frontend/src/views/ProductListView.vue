<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import ProductCard from '@/components/ProductCard.vue'
import { getProductList } from '@/api/product'

const products = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(8)

const filters = reactive({
  keyword: '',
  minPrice: null,
  maxPrice: null
})

const hasFilter = computed(
  () =>
    Boolean(filters.keyword.trim()) ||
    typeof filters.minPrice === 'number' ||
    typeof filters.maxPrice === 'number'
)

function buildParams() {
  const params = { page: page.value, size: size.value }

  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (typeof filters.minPrice === 'number') params.minPrice = filters.minPrice
  if (typeof filters.maxPrice === 'number') params.maxPrice = filters.maxPrice

  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getProductList(buildParams())
    const data = res.data || {}
    products.value = Array.isArray(data.records) ? data.records : []
    total.value = Number(data.total || 0)
  } catch {
    // 错误提示由 axios 拦截器统一处理，这里保持列表可用
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  const { minPrice, maxPrice } = filters
  if (typeof minPrice === 'number' && typeof maxPrice === 'number' && minPrice > maxPrice) {
    ElMessage.warning('最低价不能大于最高价')
    return
  }
  page.value = 1
  fetchList()
}

function resetFilters() {
  filters.keyword = ''
  filters.minPrice = null
  filters.maxPrice = null
  page.value = 1
  fetchList()
}

function handleSizeChange(val) {
  size.value = val
  page.value = 1
  fetchList()
}

function handlePageChange(val) {
  page.value = val
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="product-page">
    <AppHeader />

    <main class="content">
      <!-- 搜索区 -->
      <section class="hero">
        <h1>发现身边的二手好物</h1>
        <p>教材、数码、生活用品…… 总有一件正好需要</p>

        <div class="search-bar">
          <el-input
            v-model="filters.keyword"
            :prefix-icon="Search"
            size="large"
            clearable
            placeholder="搜索商品标题，例如：C++ 教材"
            class="keyword-input"
            @keyup.enter="handleSearch"
          />

          <el-input-number
            v-model="filters.minPrice"
            :min="0"
            :controls="false"
            size="large"
            placeholder="最低价"
            class="price-input"
          />
          <span class="dash">—</span>
          <el-input-number
            v-model="filters.maxPrice"
            :min="0"
            :controls="false"
            size="large"
            placeholder="最高价"
            class="price-input"
          />

          <el-button type="primary" size="large" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button size="large" :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </section>

      <!-- 结果区 -->
      <div class="result-bar">
        <div class="result-title">
          {{ hasFilter ? '筛选结果' : '最新上架' }}
          <span class="result-count">共 {{ total }} 件</span>
        </div>
        <span class="result-hint">仅展示在售商品</span>
      </div>

      <section v-loading="loading" class="list-area">
        <el-empty
          v-if="!loading && products.length === 0"
          description="暂时没有符合条件的商品，换个关键词试试吧"
        />

        <div v-else class="product-grid">
          <ProductCard v-for="item in products" :key="item.id" :product="item" />
        </div>
      </section>

      <div v-if="total > 0" class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[8, 12, 20, 40]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </main>

    <footer class="app-footer">校园二手交易平台 · Vue 3 + Element Plus</footer>
  </div>
</template>

<style scoped>
.product-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  flex: 1;
}

.hero {
  padding: 30px 32px 28px;
  border-radius: 22px;
  color: #fff;
  background:
    radial-gradient(600px 220px at 88% -10%, rgba(255, 255, 255, 0.25), transparent 60%),
    linear-gradient(135deg, #4f46e5 0%, #6d5ce8 55%, #8b5cf6 100%);
  box-shadow: 0 20px 45px rgba(79, 70, 229, 0.28);
}

.hero h1 {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.hero p {
  margin-top: 8px;
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.82);
}

.search-bar {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
}

.keyword-input {
  flex: 1 1 260px;
  min-width: 200px;
}

.price-input {
  width: 112px;
}

.dash {
  color: rgba(255, 255, 255, 0.6);
}

.search-bar :deep(.el-input__wrapper),
.search-bar :deep(.el-input-number) {
  border-radius: 10px;
}

.search-bar :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.result-bar {
  margin: 24px 4px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-title {
  font-size: 16px;
  font-weight: 700;
}

.result-count {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: var(--sp-muted);
}

.result-hint {
  font-size: 12.5px;
  color: #a4aab8;
}

.list-area {
  min-height: 220px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 18px;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.app-footer {
  padding: 22px 24px 30px;
  text-align: center;
  font-size: 12.5px;
  color: #a4aab8;
}

@media (max-width: 640px) {
  .content {
    padding: 16px 16px 36px;
  }

  .hero {
    padding: 22px 20px;
    border-radius: 18px;
  }

  .hero h1 {
    font-size: 21px;
  }

  .search-bar {
    padding: 10px;
  }

  .keyword-input {
    flex: 1 1 100%;
  }

  .price-input {
    flex: 1 1 calc(50% - 40px);
    width: auto;
  }

  .search-bar :deep(.el-button) {
    flex: 1 1 100%;
    margin-left: 0;
  }
}
</style>
