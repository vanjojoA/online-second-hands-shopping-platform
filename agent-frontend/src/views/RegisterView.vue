<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  studentId: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { min: 4, max: 20, message: '学号长度应为 4 ~ 20 位', trigger: 'blur' }
  ],
  nickname: [{ max: 20, message: '昵称最多 20 个字', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为 6 ~ 20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur'
    }
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
    const res = await register({
      studentId: form.studentId.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined
    })
    ElMessage.success(res.message || '注册成功，请登录')
    router.replace({ name: 'login', query: { studentId: form.studentId.trim() } })
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <div class="brand">
        <span class="brand-mark">🛒</span>
        <div>
          <h1>校园二手集市</h1>
          <p>注册一个账号，开始你的闲置交易</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="submit">
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="form.studentId" :prefix-icon="User" placeholder="请输入学号，例如 20260010" clearable />
        </el-form-item>
        <el-form-item label="昵称（可选）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="不填会用“用户+学号后4位”" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password placeholder="6 ~ 20 位密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" :prefix-icon="Lock" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
          {{ loading ? '注册中…' : '注 册' }}
        </el-button>
      </el-form>

      <div class="foot">
        已经有账号了？
        <el-button link type="primary" @click="router.push('/login')">去登录</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 20px;
  background:
    radial-gradient(900px 500px at 12% 8%, rgba(79, 70, 229, 0.18), transparent 60%),
    radial-gradient(800px 500px at 90% 92%, rgba(168, 85, 247, 0.16), transparent 60%),
    #eef1fb;
}

.register-card {
  width: 100%;
  max-width: 460px;
  padding: 36px 36px 28px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 30px 70px rgba(31, 36, 48, 0.16);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.brand-mark {
  font-size: 30px;
}

.brand h1 {
  font-size: 20px;
  font-weight: 800;
}

.brand p {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--sp-muted);
}

.el-form {
  margin-top: 4px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 10px;
}

.foot {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--sp-muted);
}

@media (max-width: 480px) {
  .register-card {
    padding: 28px 20px 22px;
  }
}
</style>
