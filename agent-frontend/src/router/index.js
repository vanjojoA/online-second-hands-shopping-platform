import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { getUserInfo } from '@/api/user'

const routes = [
  { path: '/', redirect: '/products' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guestOnly: true, title: '注册' }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },
  {
    path: '/history',
    name: 'history',
    component: () => import('@/views/HistoryView.vue'),
    meta: { requiresAuth: true, title: '浏览记录' }
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('@/views/ProductListView.vue'),
    meta: { requiresAuth: true, title: '商品列表' }
  },
  {
    path: '/products/:id',
    name: 'product-detail',
    component: () => import('@/views/ProductDetailView.vue'),
    meta: { requiresAuth: true, title: '商品详情' }
  },
  {
    path: '/publish',
    name: 'publish',
    component: () => import('@/views/PublishView.vue'),
    meta: { requiresAuth: true, title: '发布商品' }
  },
  {
    path: '/publish/:id',
    name: 'product-edit',
    component: () => import('@/views/PublishView.vue'),
    meta: { requiresAuth: true, title: '编辑商品' }
  },
  {
    path: '/chat/:friendId',
    name: 'chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true, title: '私信' }
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { requiresAuth: true, title: '消息' }
  },
  {
    path: '/notifications',
    name: 'notifications',
    component: () => import('@/views/NotificationsView.vue'),
    meta: { requiresAuth: true, title: '系统通知' }
  },
  {
    path: '/my-products',
    name: 'my-products',
    component: () => import('@/views/MyProductsView.vue'),
    meta: { requiresAuth: true, title: '我的商品' }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, title: '管理员后台' },
    redirect: '/admin/products',
    children: [
      {
        path: 'products',
        name: 'admin-products',
        component: () => import('@/views/admin/AdminProductsView.vue'),
        meta: { title: '商品审核' }
      },
      {
        path: 'reports',
        name: 'admin-reports',
        component: () => import('@/views/admin/AdminReportsView.vue'),
        meta: { title: '举报处理' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/products' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const token = getToken()

  // 需要登录但没有 Token：跳登录页，并记住原本要去的地址
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 已登录还去登录页：直接回商品列表
  if (to.meta.guestOnly && token) {
    return { name: 'products' }
  }

  // 管理员页面：校验当前用户 role 是否为 1
  if (to.meta.requiresAdmin) {
    try {
      const res = await getUserInfo()
      if (Number(res.data?.role) !== 1) {
        ElMessage.error('你没有管理员权限')
        return { name: 'products' }
      }
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 校园二手集市` : '校园二手集市'
})

export default router
