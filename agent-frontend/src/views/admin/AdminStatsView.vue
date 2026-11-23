<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { getStatistics } from '@/api/admin'

const loading = ref(false)
const stats = ref({
  today: {},
  totals: {},
  trend: [],
  statusDistribution: []
})

const lineRef = ref(null)
const pieRef = ref(null)
let lineChart = null
let pieChart = null

function money(value) {
  const num = Number(value || 0)
  return num.toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const res = await getStatistics()
    stats.value = res.data || {}
    await nextTick()
    renderCharts()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  const trend = Array.isArray(stats.value.trend) ? stats.value.trend : []
  const dates = trend.map((item) => String(item.date).slice(5))

  if (lineRef.value) {
    lineChart = lineChart || echarts.init(lineRef.value)
    lineChart.setOption(
      {
        tooltip: { trigger: 'axis' },
        legend: { data: ['新增用户', '新增商品', '成交额'], top: 0 },
        grid: { left: 44, right: 56, top: 42, bottom: 30 },
        xAxis: { type: 'category', boundaryGap: false, data: dates },
        yAxis: [
          { type: 'value', name: '人数/件', minInterval: 1 },
          { type: 'value', name: '金额(元)' }
        ],
        series: [
          {
            name: '新增用户',
            type: 'line',
            smooth: true,
            symbolSize: 7,
            itemStyle: { color: '#4f46e5' },
            data: trend.map((item) => item.newUsers)
          },
          {
            name: '新增商品',
            type: 'line',
            smooth: true,
            symbolSize: 7,
            itemStyle: { color: '#22c55e' },
            data: trend.map((item) => item.newProducts)
          },
          {
            name: '成交额',
            type: 'line',
            smooth: true,
            yAxisIndex: 1,
            symbolSize: 7,
            itemStyle: { color: '#f59e0b' },
            data: trend.map((item) => item.tradeAmount)
          }
        ]
      },
      true
    )
  }

  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption(
      {
        tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
        legend: { bottom: 0 },
        color: ['#f59e0b', '#22c55e', '#94a3b8', '#ef4444', '#4f46e5'],
        series: [
          {
            type: 'pie',
            radius: ['42%', '66%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            label: { formatter: '{b}\n{c} 件' },
            data: Array.isArray(stats.value.statusDistribution)
              ? stats.value.statusDistribution
              : []
          }
        ]
      },
      true
    )
  }
}

function handleResize() {
  lineChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<template>
  <div v-loading="loading" class="admin-stats">
    <div class="page-head">
      <div>
        <h2>数据统计</h2>
        <p>今天的新增数据、近 7 天趋势和商品状态分布</p>
      </div>
      <el-button circle :icon="Refresh" :loading="loading" @click="load" />
    </div>

    <div class="cards">
      <div class="stat-card">
        <span class="label">今日新增用户</span>
        <b class="value">{{ stats.today?.newUsers ?? 0 }}</b>
        <span class="sub">累计 {{ stats.totals?.users ?? 0 }} 人</span>
      </div>
      <div class="stat-card">
        <span class="label">今日新增商品</span>
        <b class="value">{{ stats.today?.newProducts ?? 0 }}</b>
        <span class="sub">累计 {{ stats.totals?.products ?? 0 }} 件</span>
      </div>
      <div class="stat-card">
        <span class="label">今日成交额</span>
        <b class="value">¥{{ money(stats.today?.tradeAmount) }}</b>
        <span class="sub">今日成交 {{ stats.today?.soldCount ?? 0 }} 单</span>
      </div>
      <div class="stat-card">
        <span class="label">累计成交额</span>
        <b class="value">¥{{ money(stats.totals?.tradeAmount) }}</b>
        <span class="sub">按已售出商品统计</span>
      </div>
    </div>

    <div class="charts">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-title">近 7 天趋势（新增用户 / 新增商品 / 成交额）</div>
        </template>
        <div ref="lineRef" class="chart line-chart"></div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-title">商品状态分布</div>
        </template>
        <div ref="pieRef" class="chart pie-chart"></div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.admin-stats {
  min-height: 240px;
}

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

.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 18px 16px;
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
}

.stat-card .label {
  font-size: 12.5px;
  color: var(--sp-muted);
}

.stat-card .value {
  font-size: 26px;
  font-weight: 800;
  color: var(--sp-text);
  line-height: 1.1;
}

.stat-card .sub {
  font-size: 12px;
  color: #a4aab8;
}

.charts {
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 14px;
}

.chart-card {
  border-radius: 16px;
  border: 1px solid var(--sp-border);
}

.chart-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid var(--sp-border);
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
}

.chart {
  width: 100%;
}

.line-chart {
  height: 320px;
}

.pie-chart {
  height: 320px;
}

@media (max-width: 900px) {
  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .charts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
