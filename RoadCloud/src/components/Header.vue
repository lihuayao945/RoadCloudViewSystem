<template>
  <div class="header">
    <div class="header-left">
      <div class="info-container time-container">
        <el-icon class="info-icon time-icon"><Clock /></el-icon>
        <div class="time-info">{{ currentTime }} <span class="week-label">星期{{ dayOfWeek }}</span></div>
      </div>
      <div class="info-container weather-container">
        <el-icon class="info-icon weather-icon"><Sunny /></el-icon>
        <div class="weather-info">天气: 多云</div>
      </div>
      <div class="selector-wrapper">
        <slot name="city-selector"></slot>
      </div>
    </div>
    <div class="header-center">
      <span class="title-decoration left"></span>
      <div class="title-text">路云车辆可视化系统</div>
      <span class="title-decoration right"></span>
    </div>
    <div class="header-right">
      <button @click="goToHome" class="nav-button home-btn">
        <el-icon class="nav-icon"><House /></el-icon>
        <span>首页</span>
      </button>
      <button @click="goToDetail" class="nav-button detail-btn">
        <el-icon class="nav-icon"><Document /></el-icon>
        <span>详细</span>
      </button>
      <button @click="goToAdmin" class="nav-button admin-btn">
        <el-icon class="nav-icon"><User /></el-icon>
        <span>用户中心</span>
      </button>
      <button @click="logout" class="nav-button logout-btn">
        <el-icon class="nav-icon"><SwitchButton /></el-icon>
        <span>退出</span>
      </button>
    </div>
  </div>
</template>
  
<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { House, Document, User, SwitchButton, Clock, Sunny } from '@element-plus/icons-vue'
  
export default {
  name: 'AppHeader',
  components: {
    House,
    Document,
    User,
    SwitchButton,
    Clock,
    Sunny
  },
  setup() {
    const currentTime = ref('15:04:22')
    const dayOfWeek = ref('一')
    let timer = null
    const router = useRouter()
    const route = useRoute()
    
    onMounted(() => {
      updateTime()
      timer = setInterval(updateTime, 1000)
    })
    
    onBeforeUnmount(() => {
      if (timer) clearInterval(timer)
    })
    
    function updateTime() {
      const now = new Date()
      const hours = String(now.getHours()).padStart(2, '0')
      const minutes = String(now.getMinutes()).padStart(2, '0')
      const seconds = String(now.getSeconds()).padStart(2, '0')
      currentTime.value = `${hours}:${minutes}:${seconds}`
      
      const days = ['日', '一', '二', '三', '四', '五', '六']
      dayOfWeek.value = days[now.getDay()]
    }
    
    // 导航到首页
    const goToHome = () => {
      // 根据当前页面决定行为
      if (route.path === '/index' || route.path === '/') {
        // 如果当前已经在首页，不执行任何操作
        return
      } else {
        // 否则导航到首页，包括从detail页面
        window.location.href = '/index'
      }
    }
    
    // 导航到详细页面
    const goToDetail = () => {
      window.location.href = '/detail'
    }
    
    // 导航到用户中心
    const goToAdmin = () => {
      // 先发送GET请求到/hello验证权限
      //fetch('/api/hello')
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
            window.location.href = '/admin'
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
    
    // 登出操作
    const logout = () => {
      // 针对localhost的特殊处理
      if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        console.log('检测到本地开发环境，使用特殊方法清除Cookie');
        
        // 尝试方法1: 设置过期时间为过去
        document.cookie = 'accessToken=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';
        
        // 尝试方法2: 通过设置空值覆盖
        document.cookie = 'accessToken=;path=/';
        
        // 尝试方法3: 添加max-age=0
        document.cookie = 'accessToken=;max-age=0;path=/';
        
        // 尝试方法4: 明确指定localhost域名
        document.cookie = 'accessToken=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=localhost';
        
        // 尝试方法5: 设置SameSite属性
        document.cookie = 'accessToken=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;SameSite=Lax';
        document.cookie = 'accessToken=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;SameSite=Strict';
        document.cookie = 'accessToken=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;SameSite=None;Secure';
        
        console.log('尝试使用Ajax请求清除服务器端Cookie');
        try {
          // 尝试方法6: 发送请求到服务器要求清除Cookie (如果后端有此API)
          fetch('/api/logout', { 
            method: 'POST',
            credentials: 'include' // 包含cookie
          }).catch(e => console.log('发送登出请求失败，可能是API不存在，这是正常的', e));
        } catch (e) {
          console.log('尝试服务器清除失败，继续其他方法');
        }
      }
      
      // 获取所有可能的路径
      const pathsToTry = ['/', '/index', '/detail', '/admin', '/login', ''];
      // 获取所有可能的域
      const domain = window.location.hostname;
      const domainsToTry = [domain, '', 'localhost', '127.0.0.1'];
      
      // 如果是子域名，添加主域名
      if (domain.indexOf('.') !== -1) {
        // 获取顶级域名 (如 example.com)
        const parts = domain.split('.');
        if (parts.length >= 2) {
          // 域名格式：顶级域 (.com, .org等)
          if (parts.length === 2) {
            domainsToTry.push('.' + domain);
          } 
          // 子域名格式 (如 sub.example.com)
          else {
            const mainDomain = parts.slice(parts.length - 2).join('.');
            domainsToTry.push(mainDomain);
            domainsToTry.push('.' + mainDomain);
          }
        }
      }
      
      // 获取所有cookie名称
      const cookies = document.cookie.split(';');
      const cookieNames = [];
      
      cookies.forEach(cookie => {
        const cookieParts = cookie.split('=');
        if (cookieParts.length > 0) {
          const cookieName = cookieParts[0].trim();
          if (cookieName && !cookieNames.includes(cookieName)) {
            cookieNames.push(cookieName);
          }
        }
      });
      
      console.log('检测到以下Cookies:', cookieNames);
      
      // 对每个cookie名称尝试所有可能的路径和域组合
      cookieNames.forEach(name => {
        // 基本清除 (不指定域)
        pathsToTry.forEach(path => {
          document.cookie = `${name}=;expires=${new Date(0).toUTCString()};path=${path}`;
          document.cookie = `${name}=;max-age=0;path=${path}`;
        });
        
        // 指定域名清除
        domainsToTry.forEach(domainToTry => {
          if (domainToTry) {
            pathsToTry.forEach(path => {
              document.cookie = `${name}=;expires=${new Date(0).toUTCString()};path=${path};domain=${domainToTry}`;
              document.cookie = `${name}=;max-age=0;path=${path};domain=${domainToTry}`;
            });
          }
        });
        
        // 尝试不同的SameSite设置
        const sameSiteOptions = ['Lax', 'Strict', 'None'];
        sameSiteOptions.forEach(sameSite => {
          const secureFlag = sameSite === 'None' ? ';Secure' : '';
          pathsToTry.forEach(path => {
            document.cookie = `${name}=;expires=${new Date(0).toUTCString()};path=${path};SameSite=${sameSite}${secureFlag}`;
            domainsToTry.forEach(domainToTry => {
              if (domainToTry) {
                document.cookie = `${name}=;expires=${new Date(0).toUTCString()};path=${path};domain=${domainToTry};SameSite=${sameSite}${secureFlag}`;
              }
            });
          });
        });
      });
      
      // 特别处理accessToken (确保无遗漏)
      console.log('特别处理accessToken');
      // 使用iframe尝试在不同上下文中删除cookie
      try {
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        document.body.appendChild(iframe);
        iframe.contentDocument.cookie = 'accessToken=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';
        document.body.removeChild(iframe);
      } catch (e) {
        console.log('iframe方法失败:', e);
      }
      
      // 尝试清除HttpOnly cookie (虽然JS无法直接访问)
      console.log('尝试转向登出API清除可能的HttpOnly cookies');
      // 清除localStorage和sessionStorage中的所有内容
      localStorage.clear();
      sessionStorage.clear();
      
      // 最后显示当前cookie状态
      console.log('清除后的Cookie状态:', document.cookie);
      
      // 导航到登录页面前延迟一小段时间确保清除操作完成
      setTimeout(() => {
        console.log('准备跳转到登录页面...');
        window.location.href = '/login';
      }, 100);
    }
    
    return {
      currentTime,
      dayOfWeek,
      goToHome,
      goToDetail,
      goToAdmin,
      logout
    }
  }
}
</script>
  
<style scoped>
.header {
  height: 60px;
  background-color: rgba(0, 36, 61, 0.9);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #0e5986;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  z-index: 1000;
}

/* 左侧信息区域样式 */
.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.info-container {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(0, 183, 255, 0.1), rgba(0, 119, 182, 0.2));
  border: 1px solid rgba(0, 183, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.info-container:hover {
  background: linear-gradient(135deg, rgba(0, 183, 255, 0.15), rgba(0, 119, 182, 0.25));
  box-shadow: 0 4px 8px rgba(0, 183, 255, 0.2);
  transform: translateY(-2px);
}

.info-icon {
  font-size: 16px;
  margin-right: 8px;
  color: #00b7ff;
  transition: all 0.3s ease;
}

.info-container:hover .info-icon {
  transform: scale(1.1);
}

.time-container {
  min-width: 150px;
}

.time-info {
  font-size: 14px;
  font-weight: 500;
  color: #e1f2ff;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.week-label {
  font-size: 12px;
  color: rgba(225, 242, 255, 0.7);
  background-color: rgba(0, 183, 255, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
}

.weather-container {
  min-width: 100px;
}

.weather-info {
  font-size: 14px;
  font-weight: 500;
  color: #e1f2ff;
}

.selector-wrapper {
  display: flex;
  align-items: center;
}

/* 为城市选择器插槽设置样式，将会应用到其中的select组件 */
.selector-wrapper :deep(.el-select) {
  width: 110px;
}

.selector-wrapper :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.5);
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.3);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.selector-wrapper :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.5), 0 0 10px rgba(0, 183, 255, 0.2);
}

.selector-wrapper :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7), 0 0 10px rgba(0, 183, 255, 0.3);
}

.selector-wrapper :deep(.el-input__inner) {
  color: #e1f2ff;
  font-size: 14px;
}

.selector-wrapper :deep(.el-select .el-select__tags) {
  background-color: transparent;
}

.selector-wrapper :deep(.el-tag) {
  background-color: rgba(0, 183, 255, 0.2);
  border-color: rgba(0, 183, 255, 0.3);
  color: #e1f2ff;
}

/* 中间标题美化样式 */
.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.title-text {
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(90deg, #c471ed, #f7797d, #ffd200);
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradient 8s ease infinite, glow 2s ease-in-out infinite alternate;
  text-shadow: 0 0 10px rgba(196, 113, 237, 0.3);
  letter-spacing: 2px;
  position: relative;
  white-space: nowrap;
}

.title-decoration {
  display: block;
  height: 3px;
  width: 30px;
  background: linear-gradient(90deg, rgba(196, 113, 237, 0), rgba(247, 121, 125, 0.8));
  position: relative;
}

.title-decoration.left {
  transform: rotate(180deg);
}

.title-decoration::before, 
.title-decoration::after {
  content: '';
  position: absolute;
  height: 8px;
  width: 8px;
  border-radius: 50%;
  background-color: #f7797d;
  top: -2.5px;
  animation: pulse 2s ease-in-out infinite;
}

.title-decoration::before {
  right: 0;
}

.title-decoration::after {
  right: 12px;
  animation-delay: 0.5s;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes glow {
  from {
    text-shadow: 0 0 5px rgba(196, 113, 237, 0.2), 0 0 20px rgba(196, 113, 237, 0.2);
  }
  to {
    text-shadow: 0 0 10px rgba(247, 121, 125, 0.4), 0 0 30px rgba(247, 121, 125, 0.3);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 新的导航按钮样式 */
.nav-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
  border: none;
  outline: none;
  min-width: 100px;
  background-color: transparent;
}

/* 按钮内的图标样式 */
.nav-icon {
  font-size: 16px;
  transition: all 0.3s ease;
}

/* 为每个按钮设置不同的背景和边框 */
.home-btn {
  background: linear-gradient(135deg, rgba(0, 156, 255, 0.2), rgba(0, 198, 255, 0.3));
  box-shadow: 0 2px 8px rgba(0, 156, 255, 0.2);
  border: 1px solid rgba(0, 156, 255, 0.4);
}

.detail-btn {
  background: linear-gradient(135deg, rgba(64, 150, 238, 0.2), rgba(59, 130, 246, 0.3));
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
  border: 1px solid rgba(59, 130, 246, 0.4);
}

.admin-btn {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(79, 70, 229, 0.3));
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
  border: 1px solid rgba(79, 70, 229, 0.4);
}

.logout-btn {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.2), rgba(225, 29, 72, 0.3));
  box-shadow: 0 2px 8px rgba(225, 29, 72, 0.2);
  border: 1px solid rgba(225, 29, 72, 0.4);
}

/* 悬停效果 */
.nav-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  z-index: -1;
}

.nav-button:hover::before {
  transform: translateX(0);
}

.home-btn:hover {
  background: linear-gradient(135deg, rgba(0, 156, 255, 0.3), rgba(0, 198, 255, 0.4));
  box-shadow: 0 4px 12px rgba(0, 156, 255, 0.3), 0 0 0 1px rgba(0, 156, 255, 0.5);
  transform: translateY(-2px);
}

.detail-btn:hover {
  background: linear-gradient(135deg, rgba(64, 150, 238, 0.3), rgba(59, 130, 246, 0.4));
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3), 0 0 0 1px rgba(59, 130, 246, 0.5);
  transform: translateY(-2px);
}

.admin-btn:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.3), rgba(79, 70, 229, 0.4));
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3), 0 0 0 1px rgba(79, 70, 229, 0.5);
  transform: translateY(-2px);
}

.logout-btn:hover {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.3), rgba(225, 29, 72, 0.4));
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.3), 0 0 0 1px rgba(225, 29, 72, 0.5);
  transform: translateY(-2px);
}

/* 图标悬停动画 */
.nav-button:hover .nav-icon {
  transform: scale(1.2);
}

/* 按钮按下效果 */
.nav-button:active {
  transform: translateY(1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.1s ease;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .header-left {
    gap: 10px;
  }
  
  .info-container {
    padding: 4px 10px;
  }
  
  .time-container {
    min-width: 140px;
  }
  
  .weather-container {
    min-width: 90px;
  }
  
  .header-right {
    gap: 10px;
  }
  
  .nav-button {
    padding: 6px 12px;
    min-width: 80px;
    font-size: 13px;
  }
  
  .title-text {
    font-size: 24px;
  }
  
  .title-decoration {
    width: 25px;
  }
}

@media (max-width: 768px) {
  .title-text {
    font-size: 22px;
  }
  
  .title-decoration {
    width: 20px;
  }
  
  .info-container {
    padding: 3px 8px;
  }
  
  .time-info, .weather-info {
    font-size: 13px;
  }
  
  .week-label {
    display: none;
  }
  
  .time-container {
    min-width: auto;
  }
  
  .weather-container {
    min-width: auto;
  }
  
  .nav-button {
    padding: 5px 10px;
    min-width: 70px;
    font-size: 12px;
  }
  
  .header-left {
    gap: 8px;
  }
}

@media (max-width: 576px) {
  .title-text {
    font-size: 18px;
  }
  
  .title-decoration {
    width: 15px;
    height: 2px;
  }
  
  .title-decoration::before, 
  .title-decoration::after {
    height: 6px;
    width: 6px;
    top: -2px;
  }
  
  .title-decoration::after {
    right: 8px;
  }
  
  .header-right {
    gap: 6px;
  }
  
  .nav-button {
    padding: 4px 8px;
    min-width: 60px;
    font-size: 11px;
  }
  
  .nav-icon {
    font-size: 14px;
  }
  
  .info-icon {
    font-size: 14px;
    margin-right: 5px;
  }
  
  .info-container {
    padding: 2px 6px;
  }
}

@media (max-width: 480px) {
  .title-text {
    font-size: 16px;
  }
  
  .title-decoration {
    display: none;
  }
  
  .nav-button {
    padding: 3px 6px;
    min-width: auto;
    font-size: 10px;
  }
  
  .nav-button span {
    display: none;
  }
  
  .nav-icon {
    font-size: 12px;
    margin-right: 0;
  }
  
  .info-container {
    padding: 2px 5px;
  }
  
  .time-info, .weather-info {
    font-size: 12px;
  }
  
  .header-left {
    gap: 4px;
  }
  
  .info-icon {
    font-size: 12px;
    margin-right: 3px;
  }
  
  .selector-wrapper {
    display: none;
  }
}
</style>