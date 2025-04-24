<template>
  <div class="admin-container">
    <!-- 左侧导航栏 -->
    <div class="admin-sidebar" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <div class="sidebar-header">
        <div 
          v-if="!isSidebarCollapsed" 
          class="sidebar-logo-container"
        >
          <img 
            :src="resolveImagePath(logoUrl)" 
            alt="系统Logo" 
            class="sidebar-logo"
            @error="handleImageError"
            :key="logoKey"
          />
          <!-- <span class="system-name">{{ systemName }}</span> -->
          <span class="logo-path" v-if="showLogoPath">图片路径: {{ logoUrl }}</span>
        </div>
        <div v-else class="sidebar-logo-small">
          <img 
            :src="resolveImagePath(logoUrl)" 
            alt="Logo" 
            class="sidebar-logo-icon"
            @error="handleImageError"
            :key="logoKey"
          />
        </div>
      </div>
      <div class="sidebar-menu">
        <div class="menu-group">
          <div class="menu-item" @click="toggleUserMenu">
            <el-icon><User /></el-icon>
            <span v-show="!isSidebarCollapsed">用户管理</span>
            <el-icon class="arrow-icon" v-show="!isSidebarCollapsed" :class="{ 'arrow-down': isUserMenuOpen }">
              <ArrowDown />
            </el-icon>
          </div>
          <div class="submenu" v-show="isUserMenuOpen && !isSidebarCollapsed">
            <router-link to="/admin/user-manage/users" class="submenu-item" active-class="active">
              <el-icon><User /></el-icon>
              <span>用户信息</span>
            </router-link>
            <router-link to="/admin/user-manage/roles" class="submenu-item" active-class="active">
              <el-icon><Avatar /></el-icon>
              <span>角色管理</span>
            </router-link>
          </div>
        </div>
        <div class="menu-group">
          <div class="menu-item" @click="toggleDataMenu">
            <el-icon><DataAnalysis /></el-icon>
            <span v-show="!isSidebarCollapsed">数据管理</span>
            <el-icon class="arrow-icon" v-show="!isSidebarCollapsed" :class="{ 'arrow-down': isDataMenuOpen }">
              <ArrowDown />
            </el-icon>
          </div>
          <div class="submenu" v-show="isDataMenuOpen && !isSidebarCollapsed">
            <router-link to="/admin/data/vehicles" class="submenu-item" active-class="active">
              <el-icon><Van /></el-icon>
              <span>车辆信息</span>
            </router-link>
            <router-link to="/admin/data/rcu-objects" class="submenu-item" active-class="active">
              <el-icon><Aim /></el-icon>
              <span>RCU感知对象</span>
            </router-link>
          </div>
        </div>
        <div class="menu-group">
          <div class="menu-item" @click="toggleSystemMenu">
            <el-icon><Setting /></el-icon>
            <span v-show="!isSidebarCollapsed">系统管理</span>
            <el-icon class="arrow-icon" v-show="!isSidebarCollapsed" :class="{ 'arrow-down': isSystemMenuOpen }">
              <ArrowDown />
            </el-icon>
          </div>
          <div class="submenu" v-show="isSystemMenuOpen && !isSidebarCollapsed">
            <router-link to="/admin/system/parameters" class="submenu-item" active-class="active">
              <el-icon><SetUp /></el-icon>
              <span>参数调整</span>
            </router-link>
            <router-link to="/admin/system/topics" class="submenu-item" active-class="active">
              <el-icon><Star /></el-icon>
              <span>主题订阅</span>
            </router-link>
            <router-link to="/admin/system/logs" class="submenu-item" active-class="active">
              <el-icon><Document /></el-icon>
              <span>日志管理</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <div class="admin-right">
      <!-- 顶部栏 -->
      <div class="admin-header">
        <div class="header-left">
          <div class="sidebar-toggle" @click="toggleSidebar">
            <el-icon><ArrowLeft v-if="!isSidebarCollapsed" /><ArrowRight v-else /></el-icon>
          </div>
          <div class="logo">{{ systemName }}</div>
        </div>
        <div class="header-right">
          <div class="nav-btn home-btn" @click="goToHome">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </div>
          <div class="nav-btn detail-btn" @click="goToDetail">
            <el-icon><Document /></el-icon>
            <span>详细</span>
          </div>
          <div class="user-info" @click="handleProfileEdit">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </div>
          <div class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="admin-content">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, 
  DataAnalysis, 
  Setting, 
  ArrowLeft, 
  ArrowRight, 
  SwitchButton,
  ArrowDown,
  Van,
  Warning,
  Aim,
  SetUp,
  Star,
  Bell,
  Document,
  Avatar,
  House
} from '@element-plus/icons-vue'
import LogoImage from '@/assets/logo.png'
import { resolveImagePath, handleImageError } from '@/utils/imageUtils'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeMenu = ref('users')
const isSidebarCollapsed = ref(false)
const isDataMenuOpen = ref(false)
const isSystemMenuOpen = ref(false)
const isUserMenuOpen = ref(true) // 默认展开
const logoKey = ref(0) // 用于强制刷新图片

// 获取全局状态
const globalState = inject('globalState', null)
// 设置系统名称的默认值
const systemName = ref(globalState?.systemName?.value || 'RoadCloud 管理系统')
// 是否显示图片路径（调试用）
const showLogoPath = ref(false)

// 将备用的logoUrl改为使用定义的路径
const logoUrl = ref(globalState?.logoUrl?.value || LogoImage)

// 打印当前的logoUrl值用于调试
console.log('初始logoUrl值:', logoUrl.value)

// 监听系统名称和Logo变更事件
const handleSystemNameChange = (event) => {
  if (event.detail && event.detail.name) {
    systemName.value = event.detail.name
  }
}

// 监听Logo变更
const handleLogoChange = (event) => {
  if (event && event.detail && event.detail.url) {
    console.log('通过事件接收到新Logo:', event.detail.url)
    logoUrl.value = event.detail.url
    // 强制刷新图片
    logoKey.value += 1
  } else if (globalState && globalState.logoUrl) {
    console.log('从全局状态加载Logo:', globalState.logoUrl.value)
    logoUrl.value = globalState.logoUrl.value
    // 强制刷新图片
    logoKey.value += 1
  }
}

// 在组件挂载时添加事件监听
onMounted(() => {
  window.addEventListener('system-name-changed', handleSystemNameChange)
  window.addEventListener('logo-changed', handleLogoChange)
  
  // 初始设置logo
  handleLogoChange()
  
  // 监听logo变化
  if (globalState && globalState.logoUrl) {
    watch(globalState.logoUrl, (newVal) => {
      console.log('通过watch检测到Logo变化:', newVal)
      logoUrl.value = newVal
    })
  }
})

// 在组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('system-name-changed', handleSystemNameChange)
  window.removeEventListener('logo-changed', handleLogoChange)
})

// 处理菜单选择
const handleMenuSelect = (index) => {
  // 根据选择的菜单项跳转到对应路由
  const routeMap = {
    'users': '/admin/users',
    'roles': '/admin/roles',
    'permissions': '/admin/permissions'
  }
  
  if (routeMap[index]) {
    // 先发送GET请求到/hello验证权限
    fetch('/api/hello')
      .then(response => response.json())
      .then(data => {
        // 在控制台输出服务器回复
        console.log('菜单选择时权限验证(/hello)响应:', data)
        
        if (data.code !== 401) {
          // 如果code不等于401，拥有权限，跳转到目标页面
          window.location.href = routeMap[index]
        } else {
          // code等于401，没有权限，显示提示
          ElMessage.error('您没有访问该页面的权限')
        }
      })
      .catch(error => {
        console.error('权限验证请求失败:', error)
        // 请求失败，显示提示
        ElMessage.error('权限验证失败，无法访问该页面')
      })
  }
}

// 切换侧边栏展开/收起状态
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 切换数据管理菜单展开/收起状态
const toggleDataMenu = () => {
  isDataMenuOpen.value = !isDataMenuOpen.value
}

// 切换系统管理菜单展开/收起状态
const toggleSystemMenu = () => {
  isSystemMenuOpen.value = !isSystemMenuOpen.value
}

// 切换用户管理菜单展开/收起状态
const toggleUserMenu = () => {
  isUserMenuOpen.value = !isUserMenuOpen.value
}

// 处理退出登录
const handleLogout = () => {
  // 清除所有cookies
  const cookies = document.cookie.split(";");
  for (let i = 0; i < cookies.length; i++) {
    const cookie = cookies[i];
    const eqPos = cookie.indexOf("=");
    const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();
    document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/";
  }
  
  // 清除localStorage中的token和用户信息
  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');
  sessionStorage.clear();
  
  ElMessage.success('已安全退出系统');
  
  // 跳转到登录页面
  window.location.href = '/login'
}

// 跳转到个人资料页面
const handleProfileEdit = () => {
  // 先发送GET请求到/hello验证权限
      fetch('/api/hello')
        .then(response => {
          // 在控制台记录原始响应
          console.log('权限验证接口(/hello)原始响应:', response)
          
          // 将响应解析为文本
          return response.text()
        })
        .then(text => {
          // 在控制台输出文本响应
          console.log('权限验证接口(/hello)响应文本:', text)
          
          // 判断文本内容
          if (text === "你好世界！") {
            console.log('收到特定响应文本"你好世界！"，验证成功')
            // 验证成功，跳转到/admin
            window.location.href = '/admin/profile'
          } else {
            // 尝试解析为JSON判断code
            try {
              const data = JSON.parse(text)
              console.log('响应为JSON格式:', data)
              
              if (data.code !== 401) {
                // window.location.href = '/admin'
              } else {
                alert('您没有访问用户中心的权限')
              }
            } catch (e) {
              // 非特定文本且非有效JSON，验证失败
              console.log('响应既不是"你好世界！"也不是有效的JSON，验证失败')
              alert('您没有访问用户中心的权限')
            }
          }
        })
        .catch(error => {
          console.error('权限验证请求失败:', error)
          // 请求失败，显示提示
          alert('权限验证失败，无法访问用户中心')
        })
}

// 跳转到首页
const goToHome = () => {
  window.location.href = '/index'
}

// 跳转到详细页面
const goToDetail = () => {
  window.location.href = '/detail'
}
</script>

<style scoped>
.admin-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  background-color: #060818;
  color: #fff;
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 10% 20%, rgba(28, 175, 244, 0.1) 0%, rgba(0, 0, 0, 0) 20%),
    radial-gradient(circle at 90% 80%, rgba(124, 0, 255, 0.1) 0%, rgba(0, 0, 0, 0) 20%);
}

.admin-sidebar {
  width: 240px;
  min-width: 240px;
  background: linear-gradient(135deg, rgba(13, 71, 161, 0.9), rgba(33, 39, 80, 0.9));
  color: white;
  padding: 20px 0;
  transition: all 0.3s ease;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 20px rgba(0, 140, 255, 0.2);
  border-right: 1px solid rgba(0, 195, 255, 0.3);
  position: relative;
  z-index: 10;
}

.sidebar-collapsed {
  width: 64px;
  min-width: 64px;
}

.admin-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  height: 60px;
  background: linear-gradient(90deg, rgba(6, 11, 40, 0.8) 0%, rgba(10, 14, 35, 0.8) 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(0, 223, 252, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  z-index: 100;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sidebar-toggle {
  width: 28px;
  height: 28px;
  background: rgba(0, 183, 255, 0.1);
  border: 1px solid rgba(0, 183, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.sidebar-toggle:hover {
  background: rgba(0, 183, 255, 0.2);
  transform: scale(1.1);
  box-shadow: 0 0 15px rgba(0, 183, 255, 0.5);
}

.sidebar-toggle i {
  color: #00b7ff;
  font-size: 14px;
  transition: all 0.3s ease;
}

.header-left .logo {
  font-size: clamp(16px, 4vw, 20px);
  font-weight: bold;
  color: #00dffc;
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.7);
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.home-btn {
  background: rgba(0, 183, 255, 0.1);
  border: 1px solid rgba(0, 183, 255, 0.3);
}

.detail-btn {
  background: rgba(0, 183, 255, 0.1);
  border: 1px solid rgba(0, 183, 255, 0.3);
}

.home-btn:hover {
  box-shadow: 0 0 15px rgba(0, 183, 255, 0.5);
  background: rgba(0, 183, 255, 0.2);
  border: 1px solid rgba(0, 183, 255, 0.5);
}

.detail-btn:hover {
  box-shadow: 0 0 15px rgba(0, 183, 255, 0.5);
  background: rgba(0, 183, 255, 0.2);
  border: 1px solid rgba(0, 183, 255, 0.5);
}

.user-info, .logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.user-info {
  background: rgba(0, 183, 255, 0.1);
  border: 1px solid rgba(0, 183, 255, 0.3);
}

.logout-btn {
  background: rgba(255, 77, 79, 0.1);
  border: 1px solid rgba(255, 77, 79, 0.3);
}

.user-info:hover {
  box-shadow: 0 0 15px rgba(0, 183, 255, 0.5);
  background: rgba(0, 183, 255, 0.2);
  border: 1px solid rgba(0, 183, 255, 0.5);
}

.logout-btn:hover {
  box-shadow: 0 0 15px rgba(255, 77, 79, 0.5);
  background: rgba(255, 77, 79, 0.2);
  border: 1px solid rgba(255, 77, 79, 0.5);
}

.admin-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #060818;
  position: relative;
  background-image: 
    radial-gradient(circle at 20% 25%, rgba(28, 175, 244, 0.05) 0%, rgba(0, 0, 0, 0) 25%),
    radial-gradient(circle at 80% 75%, rgba(124, 0, 255, 0.05) 0%, rgba(0, 0, 0, 0) 25%);
}

.sidebar-header {
  padding: 16px;
  background-color: var(--el-color-primary-dark-2);
  color: white;
  text-align: center;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-logo-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.sidebar-logo {
  height: 40px;
  max-width: 100%;
  object-fit: contain;
  filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.7));
  margin-bottom: 8px;
}

.sidebar-logo-small {
  display: flex;
  justify-content: center;
}

.sidebar-logo-icon {
  height: 32px;
  width: 32px;
  object-fit: contain;
}

.system-name {
  font-size: 14px;
  font-weight: bold;
  color: white;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.menu-group {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: all 0.3s ease;
  white-space: nowrap;
  position: relative;
  cursor: pointer;
  border-left: 3px solid transparent;
}

.menu-item i, .menu-item el-icon {
  margin-right: 10px;
  font-size: 16px;
  min-width: 20px;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(0, 220, 252, 0.1);
  border-left: 3px solid rgba(0, 223, 252, 0.5);
}

.menu-item:hover i, .menu-item:hover el-icon {
  color: #00dffc;
  transform: scale(1.1);
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.7);
}

.menu-item.active {
  background: rgba(0, 220, 252, 0.15);
  border-left: 3px solid #00dffc;
}

.menu-item.active i, .menu-item.active el-icon {
  color: #00dffc;
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.7);
}

.arrow-icon {
  margin-left: auto;
  transition: transform 0.3s ease;
}

.arrow-down {
  transform: rotate(180deg);
}

.submenu {
  display: flex;
  flex-direction: column;
  padding-left: 20px;
  background: rgba(0, 0, 0, 0.2);
  border-left: 1px solid rgba(0, 183, 255, 0.2);
}

.submenu-item {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: all 0.3s ease;
  white-space: nowrap;
  position: relative;
  border-left: 2px solid transparent;
}

.submenu-item i, .submenu-item el-icon {
  margin-right: 10px;
  font-size: 16px;
  min-width: 20px;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.submenu-item:hover i, .submenu-item:hover el-icon {
  color: #00dffc;
  transform: scale(1.1);
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.7);
}

.submenu-item.active i, .submenu-item.active el-icon {
  color: #00dffc;
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.7);
}

.submenu-item:hover {
  background-color: rgba(0, 220, 252, 0.1);
  color: white;
  border-left: 2px solid rgba(0, 223, 252, 0.5);
}

.submenu-item.active {
  background-color: rgba(0, 220, 252, 0.15);
  color: white;
  border-left: 2px solid #00dffc;
}

.sidebar-collapsed .submenu {
  display: none;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .admin-sidebar {
    width: 200px;
    min-width: 200px;
  }
}

@media (max-width: 992px) {
  .admin-sidebar {
    width: 180px;
    min-width: 180px;
  }
  
  .admin-content {
    padding: 15px;
  }
}

@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(0);
    transition: transform 0.3s ease;
  }
  
  .sidebar-collapsed {
    transform: translateX(-100%);
    width: 64px;
    min-width: 64px;
  }
  
  .admin-right {
    width: 100%;
  }
  
  .header-left .logo {
    font-size: 16px;
  }
  
  .user-info span, .logout-btn span {
    display: none;
  }
}

@media (max-width: 576px) {
  .admin-header {
    padding: 0 10px;
  }
  
  .admin-content {
    padding: 10px;
  }
}
</style> 