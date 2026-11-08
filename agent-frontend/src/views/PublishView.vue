<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, Plus, UploadFilled } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getProductDetail, publishProduct, updateProduct } from '@/api/product'
import { uploadImage } from '@/api/file'
import { resolveImageUrl } from '@/utils/url'

const MAX_IMAGES = 5
const MAX_SIZE = 10 * 1024 * 1024

const router = useRouter()
const route = useRoute()

// 有路由参数就是编辑模式（/publish/:id）
const editId = computed(() => route.params.id)
const isEdit = computed(() => Boolean(editId.value))

const loading = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const uploading = ref(0)

// 上传成功的图片：raw 是后端返回的原始路径（存库用），display 是带域名的展示地址
const images = ref([])

const form = reactive({
  title: '',
  price: 10,
  description: ''
})

const rules = {
  title: [
    {
      validator: (rule, value, callback) => {
        const text = (value || '').trim()
        if (!text) return callback(new Error('请输入商品标题'))
        if (text.length < 2) return callback(new Error('标题至少 2 个字'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  price: [
    {
      validator: (rule, value, callback) => {
        if (typeof value !== 'number' || Number.isNaN(value) || value <= 0) {
          return callback(new Error('价格必须大于 0'))
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const canUploadMore = computed(() => images.value.length < MAX_IMAGES)

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('单张图片不能超过 10MB')
    return false
  }
  if (images.value.length >= MAX_IMAGES) {
    ElMessage.warning(`最多上传 ${MAX_IMAGES} 张图片`)
    return false
  }
  return true
}

async function handleUpload(options) {
  if (images.value.length >= MAX_IMAGES) {
    ElMessage.warning(`最多上传 ${MAX_IMAGES} 张图片`)
    return
  }

  uploading.value += 1
  try {
    const res = await uploadImage(options.file)
    // 特别注意：上传接口把地址放在 message 里，data 是 null
    const raw = res.data || res.message
    if (typeof raw !== 'string' || !raw.startsWith('/')) {
      throw new Error('上传接口没有返回图片地址')
    }
    images.value.push({
      raw,
      display: resolveImageUrl(raw),
      name: options.file.name
    })
  } catch (e) {
    if (!e?.__toasted) {
      ElMessage.error(e?.message || '图片上传失败，请重试')
    }
  } finally {
    uploading.value -= 1
  }
}

function removeImage(index) {
  images.value.splice(index, 1)
}

async function submit() {
  if (submitting.value) return

  if (uploading.value > 0) {
    ElMessage.warning('图片还在上传中，请稍等一下')
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  const payload = {
    title: form.title.trim(),
    price: form.price,
    description: form.description.trim(),
    imageUrls: images.value.map((item) => item.raw).join(',')
  }

  try {
    const res = isEdit.value
      ? await updateProduct(editId.value, payload)
      : await publishProduct(payload)
    ElMessage.success(res.message || (isEdit.value ? '修改成功，已提交重新审核' : '发布成功，等待审核'))
    // 新发布/修改的商品都进入待审核，跳到「我的商品」方便查看状态
    router.replace({ name: 'my-products' })
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function loadForEdit() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getProductDetail(editId.value)
    const product = res.data?.product
    if (!product) {
      ElMessage.error('商品不存在')
      router.replace({ name: 'my-products' })
      return
    }
    form.title = product.title || ''
    form.price = Number(product.price) || 0.01
    form.description = product.description || ''
    images.value = String(product.imageUrls || '')
      .split(',')
      .map((url) => url.trim())
      .filter(Boolean)
      .map((raw) => ({ raw, display: resolveImageUrl(raw), name: raw.split('/').pop() }))
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(loadForEdit)
</script>

<template>
  <div class="publish-page">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <h1>{{ isEdit ? '编辑商品' : '发布商品' }}</h1>
        <p>
          {{
            isEdit
              ? '修改后会重新提交审核，审核通过前不会出现在集市列表'
              : '把闲置好物分享给同学，提交后需要管理员审核通过才会展示'
          }}
        </p>
      </div>

      <el-card v-loading="loading" class="form-card" shadow="never">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <el-form-item label="商品标题" prop="title">
            <el-input
              v-model="form.title"
              maxlength="40"
              show-word-limit
              placeholder="例如：出二手 C++ 教材，九成新，无笔记"
            />
          </el-form-item>

          <el-form-item label="价格（元）" prop="price">
            <el-input-number
              v-model="form.price"
              :min="0.01"
              :max="99999"
              :precision="2"
              :step="1"
              controls-position="right"
              class="price-input"
            />
          </el-form-item>

          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="描述一下成色、入手渠道、交易地点等，信息越详细越容易卖出～"
            />
          </el-form-item>

          <el-form-item :label="`商品图片（${images.length}/${MAX_IMAGES}，第一张作为封面）`">
            <div class="uploader">
              <div class="image-grid">
                <div v-for="(img, index) in images" :key="img.raw" class="image-item">
                  <el-image :src="img.display" fit="cover" class="image-preview">
                    <template #error>
                      <div class="image-error">加载失败</div>
                    </template>
                  </el-image>
                  <span v-if="index === 0" class="cover-badge">封面</span>
                  <button type="button" class="remove-btn" title="删除图片" @click="removeImage(index)">
                    <el-icon><Close /></el-icon>
                  </button>
                </div>

                <el-upload
                  v-if="canUploadMore"
                  class="upload-trigger"
                  :show-file-list="false"
                  :http-request="handleUpload"
                  :before-upload="beforeUpload"
                  :disabled="uploading > 0"
                  accept="image/*"
                  multiple
                >
                  <div class="upload-box" :class="{ uploading: uploading > 0 }">
                    <el-icon><Plus v-if="!uploading" /><UploadFilled v-else /></el-icon>
                    <span>{{ uploading > 0 ? '上传中…' : '上传图片' }}</span>
                  </div>
                </el-upload>
              </div>

              <p class="upload-tip">
                支持 jpg / png / webp 等图片格式，单张不超过 10MB，最多 {{ MAX_IMAGES }} 张
              </p>
            </div>
          </el-form-item>

          <div class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="uploading > 0"
              @click="submit"
            >
              {{
                submitting
                  ? isEdit
                    ? '保存中…'
                    : '发布中…'
                  : isEdit
                    ? '保存修改'
                    : '发布商品'
              }}
            </el-button>
            <el-button size="large" @click="router.push(isEdit ? '/my-products' : '/products')">
              取消
            </el-button>
          </div>
        </el-form>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.publish-page {
  min-height: 100vh;
}

.content {
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  padding: 26px 24px 56px;
}

.page-head h1 {
  font-size: 24px;
  font-weight: 800;
}

.page-head p {
  margin-top: 8px;
  font-size: 13px;
  color: var(--sp-muted);
}

.form-card {
  margin-top: 20px;
  border-radius: 20px;
  border: 1px solid var(--sp-border);
}

.form-card :deep(.el-card__body) {
  padding: 28px 30px 26px;
}

.form-card :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--sp-text);
}

.price-input {
  width: 220px;
}

/* 图片上传 */
.uploader {
  width: 100%;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: 104px;
  height: 104px;
  border-radius: 12px;
  overflow: hidden;
  background: #f4f6fc;
  border: 1px solid var(--sp-border);
}

.image-preview {
  width: 100%;
  height: 100%;
  display: block;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b3b9c9;
  font-size: 12px;
}

.cover-badge {
  position: absolute;
  left: 6px;
  bottom: 6px;
  padding: 1px 6px;
  border-radius: 6px;
  font-size: 11px;
  color: #fff;
  background: rgba(79, 70, 229, 0.9);
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 50%;
  color: #fff;
  background: rgba(31, 36, 48, 0.55);
  cursor: pointer;
  transition: background 0.2s ease;
}

.remove-btn:hover {
  background: rgba(31, 36, 48, 0.8);
}

.upload-trigger :deep(.el-upload) {
  display: block;
}

.upload-box {
  width: 104px;
  height: 104px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed #c9cfdd;
  border-radius: 12px;
  color: var(--sp-muted);
  font-size: 12px;
  background: #fafbfe;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-box:hover {
  border-color: var(--sp-primary);
  color: var(--sp-primary);
  background: #f5f4ff;
}

.upload-box .el-icon {
  font-size: 20px;
}

.upload-box.uploading {
  cursor: progress;
}

.upload-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #a4aab8;
}

.form-actions {
  margin-top: 26px;
  display: flex;
  gap: 12px;
}

.form-actions :deep(.el-button) {
  min-width: 140px;
  border-radius: 10px;
}

@media (max-width: 640px) {
  .content {
    padding: 18px 16px 40px;
  }

  .form-card :deep(.el-card__body) {
    padding: 20px 18px;
  }

  .price-input {
    width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
