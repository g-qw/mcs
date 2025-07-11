import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import requireTransform from 'vite-plugin-require-transform';
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  base: '/', //默认为根路径
  plugins: [
    vue(),
    vueDevTools(),
    createSvgIconsPlugin({
      iconDirs: [path.resolve(process.cwd(), 'src/assets/svg')], // 指定需要缓存的图标文件夹
      symbolId: 'icon-[name]', // 指定symbolId格式
    }),
    requireTransform({
      fileRegex: /\.(js|vue|png|jpe?g|gif|svg)$/, // 匹配文件的正则表达式，默认为/\.ts$|\.tsx$/
      importPrefix: '_vite_plugin_require_transform_', // 导入前缀，默认为'_vite_plugin_require_transform_'
      //importPathHandler: Function // 处理 require 路径的函数
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  rollupOptions: {
    input: {
      main: './index.html', // 入口文件
    },
  },
})
