<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Clock, Goods, HomeFilled, Lock, Upload, User } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getUserInfo, updatePassword, updateUserInfo } from '@/api/user'
import { uploadImage } from '@/api/file'
import { clearAuth, updateUser } from '@/utils/auth'
import { resolveImageUrl } from '@/utils/url'

const router = useRouter()

const loading = ref(false)
const savingInfo = ref(false)
const savingPwd = ref(false)
const uploadingAvatar = ref(false)
const userInfo = ref({})
const pwdFormRef = ref(null)

const infoForm = reactive({
  nickname: '',
  avatar: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarText = computed(() => (userInfo.value.nickname || '同').slice(0, 1))
const avatarPreview = computed(() => resolveImageUrl(infoForm.avatar))
const isAdmin = computed(() => Number(userInfo.value.role) === 1)

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为 6 ~ 20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  loading.value = true
  try {
    const res = await getUserInfo()
    userInfo.value = res.data || {}
    infoForm.nickname = userInfo.value.nickname || ''
    infoForm.avatar = userInfo.value.avatar || ''
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function beforeAvatarUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('头像图片不能超过 10MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options) {
  uploadingAvatar.value = true
  try {
    const res = await uploadImage(options.file)
    // 上传接口把地址放在 message 里
    const raw = res.data || res.message
    if (typeof raw !== 'string' || !raw.startsWith('/')) {
      throw new Error('上传接口没有返回图片地址')
    }
    infoForm.avatar = raw

    // 上传成功直接保存到后端，换设备登录也能看到
    await updateUserInfo({ nickname: userInfo.value.nickname, avatar: raw })
    userInfo.value.avatar = raw
    updateUser({ avatar: raw })
    ElMessage.success('头像已更新')
  } catch (e) {
    if (!e?.__toasted) {
      ElMessage.error(e?.message || '头像上传失败，请重试')
    }
  } finally {
    uploadingAvatar.value = false
  }
}

async function saveInfo() {
  const nickname = infoForm.nickname.trim()
  if (!nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (nickname.length > 20) {
    ElMessage.warning('昵称最多 20 个字')
    return
  }

  savingInfo.value = true
  try {
    const res = await updateUserInfo({ nickname, avatar: infoForm.avatar.trim() })
    userInfo.value.nickname = nickname
    userInfo.value.avatar = infoForm.avatar.trim()
    updateUser({ nickname, avatar: infoForm.avatar.trim() })
    ElMessage.success(res.message || '个人信息修改成功')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    savingInfo.value = false
  }
}

async function savePassword() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }

  savingPwd.value = true
  try {
    const res = await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success(res.message || '密码修改成功，请重新登录')
    clearAuth()
    router.replace('/login')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    savingPwd.value = false
  }
}

function timeText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '——'
}

onMounted(load)
</script>

<template>
  <div class="profile-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>个人中心</h1>
          <p>管理你的资料、密码和常用入口</p>
        </div>
        <el-button :icon="HomeFilled" @click="router.push('/products')">返回主页</el-button>
      </div>

      <div v-loading="loading" class="grid">
        <!-- 左侧：资料概览 -->
        <section class="card summary">
          <el-avatar :size="76" :src="avatarPreview || undefined" class="big-avatar">
            {{ avatarText }}
          </el-avatar>
          <h2 class="name">{{ userInfo.nickname || '同学' }}</h2>
          <div class="tags">
            <el-tag v-if="isAdmin" type="danger" effect="plain" size="small">管理员</el-tag>
            <el-tag v-else type="info" effect="plain" size="small">普通用户</el-tag>
          </div>

          <ul class="meta">
            <li><span>学号</span><b>{{ userInfo.studentId || '——' }}</b></li>
            <li><span>用户 ID</span><b>#{{ userInfo.id || '——' }}</b></li>
            <li><span>加入时间</span><b>{{ timeText(userInfo.createTime) }}</b></li>
          </ul>

          <div class="quick">
            <el-button :icon="Goods" @click="router.push('/my-products')">我的商品</el-button>
            <el-button :icon="ChatDotRound" @click="router.push('/messages')">我的消息</el-button>
            <el-button :icon="Clock" @click="router.push('/history')">浏览记录</el-button>
          </div>
        </section>

        <!-- 右侧：表单 -->
        <section class="right">
          <el-card class="card form-card" shadow="never">
            <template #header>
              <div class="card-title"><el-icon><User /></el-icon> 基本资料</div>
            </template>
            <el-form label-position="top" size="large">
              <el-form-item label="昵称">
                <el-input v-model="infoForm.nickname" maxlength="20" show-word-limit placeholder="给自己起个昵称" />
              </el-form-item>
              <el-form-item label="头像">
                <div class="avatar-editor">
                  <el-avatar :size="64" :src="avatarPreview || undefined" class="edit-avatar">
                    {{ avatarText }}
                  </el-avatar>
                  <div class="avatar-actions">
                    <el-upload
                      :show-file-list="false"
                      :http-request="handleAvatarUpload"
                      :before-upload="beforeAvatarUpload"
                      accept="image/*"
                    >
                      <el-button :icon="Upload" :loading="uploadingAvatar">上传头像</el-button>
                    </el-upload>
                    <span class="avatar-tip">支持 jpg / png / webp，单张不超过 10MB</span>
                  </div>
                </div>
              </el-form-item>
              <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存资料</el-button>
            </el-form>
          </el-card>

          <el-card class="card form-card" shadow="never">
            <template #header>
              <div class="card-title"><el-icon><Lock /></el-icon> 修改密码</div>
            </template>
            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" size="large">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6 ~ 20 位新密码" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-button type="primary" plain :loading="savingPwd" @click="savePassword">修改密码</el-button>
              <span class="pwd-tip">修改成功后需要重新登录</span>
            </el-form>
          </el-card>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
}

.content {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h1 {
  font-size: 24px;
  font-weight: 800;
}

.page-head p {
  margin-top: 6px;
  font-size: 13px;
  color: var(--sp-muted);
}

.grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.card {
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 18px;
}

.summary {
  padding: 30px 24px 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: sticky;
  top: 84px;
}

.big-avatar {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 28px;
}

.name {
  margin-top: 12px;
  font-size: 19px;
  font-weight: 700;
}

.tags {
  margin-top: 8px;
}

.meta {
  width: 100%;
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
}

.meta li {
  display: flex;
  justify-content: space-between;
}

.meta span {
  color: var(--sp-muted);
}

.meta b {
  font-weight: 500;
}

.quick {
  width: 100%;
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.right {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-card {
  border-radius: 18px;
}

.form-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--sp-border);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.pwd-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--sp-muted);
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
}

.edit-avatar {
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.avatar-tip {
  font-size: 12px;
  color: var(--sp-muted);
}

@media (max-width: 800px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .summary {
    position: static;
  }

  .content {
    padding: 16px 16px 36px;
  }
}
</style>
