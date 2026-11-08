<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Goods, Warning } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
</script>

<template>
  <div class="admin-page">
    <AppHeader />

    <div class="admin-body">
      <aside class="admin-side">
        <div class="side-title">
          <span class="badge">ADMIN</span>
          管理后台
        </div>
        <el-menu :default-active="activeMenu" router class="side-menu">
          <el-menu-item index="/admin/products">
            <el-icon><Goods /></el-icon>
            <span>商品审核</span>
          </el-menu-item>
          <el-menu-item index="/admin/reports">
            <el-icon><Warning /></el-icon>
            <span>举报处理</span>
          </el-menu-item>
        </el-menu>
        <div class="side-foot">
          <el-button link :icon="ArrowLeft" @click="router.push('/products')">返回商城</el-button>
        </div>
      </aside>

      <section class="admin-main">
        <router-view />
      </section>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  min-height: 100vh;
}

.admin-body {
  display: flex;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 40px;
  align-items: flex-start;
}

.admin-side {
  width: 210px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 18px;
  padding: 16px 10px 12px;
  position: sticky;
  top: 84px;
}

.side-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 10px 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--sp-text);
}

.badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  padding: 2px 6px;
  border-radius: 6px;
}

.side-menu {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  height: 42px;
  border-radius: 10px;
  margin: 4px 0;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: #eef0fe;
  color: var(--sp-primary);
  font-weight: 600;
}

.side-foot {
  margin-top: 10px;
  padding: 10px 10px 0;
  border-top: 1px dashed var(--sp-border);
}

.admin-main {
  flex: 1;
  min-width: 0;
}

@media (max-width: 800px) {
  .admin-body {
    flex-direction: column;
    padding: 14px 14px 30px;
  }

  .admin-side {
    width: 100%;
    position: static;
  }

  .side-menu {
    display: flex;
  }

  .side-menu :deep(.el-menu-item) {
    flex: 1;
    justify-content: center;
  }

  .side-foot {
    display: none;
  }
}
</style>
