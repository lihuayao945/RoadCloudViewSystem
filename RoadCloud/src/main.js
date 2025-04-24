import "./assets/main.css"

import { createApp, ref, readonly, provide } from "vue"
import App from "./App.vue"
import router from "./router"
import "./assets/styles/global.css"

// 导入 Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 导入 Element Plus 图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 高德地图配置
window._AMapSecurityConfig = {
  securityJsCode: "e4abe1ae6a2474fdf2220157830b2b99",
}

// 添加窗口大小变化监听，确保地图和组件能够响应窗口大小变化
window.addEventListener("resize", () => {
  // 触发自定义事件，让组件知道窗口大小已变化
  window.dispatchEvent(new CustomEvent("app-resize"))
})

// 导入logo图片
import LogoImage from '@/assets/logo.png'
import { resolveImagePath } from '@/utils/imageUtils'

// 创建全局状态
const globalState = {
  systemName: ref('RoadCloud 管理系统'),
  logoUrl: ref(LogoImage)
}

// 全局状态更新函数
const updateGlobalState = (newState) => {
  if (newState.systemName) {
    globalState.systemName.value = newState.systemName
    // 触发全局事件，通知其他组件系统名称已更新
    window.dispatchEvent(new CustomEvent('system-name-changed', { 
      detail: { name: newState.systemName }
    }))
  }
  
  if (newState.logoUrl) {
    console.log('更新全局LOGO路径 (原始值):', newState.logoUrl)
    
    // 使用工具函数处理路径
    const resolvedPath = resolveImagePath(newState.logoUrl)
    console.log('处理后的LOGO路径 (将赋值给globalState):', resolvedPath)
    
    // 强制重新赋值以确保响应式更新
    globalState.logoUrl.value = null
    setTimeout(() => {
      globalState.logoUrl.value = resolvedPath
      
      // 触发全局事件，通知其他组件LOGO已更新
      window.dispatchEvent(new CustomEvent('logo-changed', { 
        detail: { url: resolvedPath }
      }))
      
      console.log('触发logo-changed事件，新路径:', resolvedPath)
    }, 0)
  }
}

const app = createApp(App)

// 向全局提供状态和更新函数
app.provide('globalState', readonly(globalState))
app.provide('updateGlobalState', updateGlobalState)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)

app.mount("#app")

