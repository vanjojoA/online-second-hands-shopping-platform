<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Lock, User } from '@element-plus/icons-vue'
import { login } from '@/api/user'
import { setAuth } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  studentId: '',
  password: ''
})

const rules = {
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { min: 4, max: 20, message: '学号长度应为 4 ~ 20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function submit() {
  if (loading.value || !formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await login({
      studentId: form.studentId.trim(),
      password: form.password
    })
    // res.data = { token, userId, nickname }
    setAuth(res.data)
    ElMessage.success(`欢迎回来，${res.data.nickname || form.studentId}`)

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/products'
    router.replace(redirect)
  } catch {
    // 具体错误提示已由 axios 响应拦截器统一弹出
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 注册成功跳回来时，自动带上学号
  const studentId = route.query.studentId
  if (typeof studentId === 'string' && studentId) {
    form.studentId = studentId
  }
})
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <!-- 左侧品牌区 -->
      <section class="brand-panel">
        <div class="brand-top">
          <span class="brand-mark">🛒</span>
          <span class="brand-name">校园二手集市</span>
        </div>

        <div class="brand-main">
          <h1>让闲置<br />遇见新主人</h1>
          <p class="brand-desc">
            教材、数码、生活好物，在校园里继续流转。<br />
            登录后即可浏览与发布商品。
          </p>
          <ul class="brand-points">
            <li><el-icon><CircleCheckFilled /></el-icon> 同校交易，见面自提更放心</li>
            <li><el-icon><CircleCheckFilled /></el-icon> 商品实时更新，先到先得</li>
            <li><el-icon><CircleCheckFilled /></el-icon> 闲置不浪费，环保又省钱</li>
          </ul>
        </div>

        <div class="brand-foot">Secondhand Platform · 校园二手交易平台</div>
      </section>

      <!-- 右侧登录表单 -->
      <section class="form-panel">
        <div class="form-head">
          <h2>欢迎回来</h2>
          <p>请使用学号和密码登录</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="submit"
        >
          <el-form-item label="学号" prop="studentId">
            <el-input
              v-model="form.studentId"
              :prefix-icon="User"
              placeholder="请输入学号"
              autocomplete="username"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
            />
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            {{ loading ? '登录中…' : '登 录' }}
          </el-button>
        </el-form>

        <el-alert class="demo-tip" type="info" :closable="false" show-icon>
          <template #title>演示账号：20260001 ／ 123456</template>
        </el-alert>

        <div class="register-link">
          还没有账号？
          <el-button link type="primary" @click="router.push('/register')">立即注册</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 20px;
  background:
    radial-gradient(900px 500px at 12% 8%, rgba(79, 70, 229, 0.18), transparent 60%),
    radial-gradient(800px 500px at 90% 92%, rgba(168, 85, 247, 0.16), transparent 60%),
    #eef1fb;
}

.login-shell {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: 1.08fr 1fr;
  background: #fff;
  border-radius: 26px;
  overflow: hidden;
  box-shadow: 0 32px 80px rgba(31, 36, 48, 0.16);
}

/* ---------- 左侧品牌区 ---------- */
.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 36px;
  padding: 46px 44px;
  color: #fff;
  background: linear-gradient(150deg, #4f46e5 0%, #6d5ce8 45%, #8b5cf6 100%);
  overflow: hidden;
}

.brand-panel::after {
  content: '';
  position: absolute;
  width: 340px;
  height: 340px;
  right: -140px;
  bottom: -150px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}

.brand-panel::before {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  right: 30px;
  top: -90px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.brand-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-mark {
  font-size: 22px;
}

.brand-name {
  font-size: 16px;
}

.brand-main {
  position: relative;
  z-index: 1;
}

.brand-main h1 {
  font-size: 40px;
  line-height: 1.25;
  font-weight: 800;
  letter-spacing: 1px;
}

.brand-desc {
  margin-top: 18px;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.82);
}

.brand-points {
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-foot {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* ---------- 右侧表单区 ---------- */
.form-panel {
  padding: 52px 46px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-head h2 {
  font-size: 26px;
  font-weight: 700;
}

.form-head p {
  margin-top: 8px;
  font-size: 13px;
  color: var(--sp-muted);
}

.el-form {
  margin-top: 28px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 10px;
  margin-top: 6px;
}

.demo-tip {
  margin-top: 24px;
  border-radius: 10px;
}

.register-link {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--sp-muted);
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .brand-panel {
    padding: 30px 28px;
    gap: 20px;
  }

  .brand-main h1 {
    font-size: 28px;
  }

  .brand-points,
  .brand-foot {
    display: none;
  }

  .form-panel {
    padding: 34px 28px 40px;
  }
}
</style>
