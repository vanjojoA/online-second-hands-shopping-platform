import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址（本地开发时通过 Vite 代理转发，避免浏览器跨域）
// 默认连云端；需要连本地后端时可以用 BACKEND_URL 环境变量覆盖
const BACKEND_URL = process.env.BACKEND_URL || 'http://8.138.25.66:8080'

export default defineConfig({
  plugins: [vue()],
  build: {
    // 当前机器上 esbuild 在压缩阶段清理临时文件会被系统拒绝（Access is denied），
    // 改用 terser 压缩，`npm run build` 才能正常完成
    minify: 'terser'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: false,
    proxy: {
      // 开发环境下 /api 和 /images 都转发到 Cloud 后端，
      // 这样浏览器里是同源请求，不会有 CORS 问题
      '/api': {
        target: BACKEND_URL,
        changeOrigin: true
      },
      '/images': {
        target: BACKEND_URL,
        changeOrigin: true
      }
    }
  }
})
