<template>
  <div class="header">
    <div class="header-left">
      <div class="time-info">{{ currentTime }} 星期{{ dayOfWeek }}</div>
      <div class="weather-info">天气: {{ weather }}</div>
    </div>
    <div class="header-center">城市智能管理系统</div>
    <div class="header-right">
      <div class="nav-item" @click="goToPage('/')">首页</div>
      <div class="nav-item active">详细</div>
      <div class="nav-item" @click="goToPage('/login')">用户中心</div>
      <div class="nav-item">其他</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

// 响应式数据
const currentTime = ref('')
const currentDate = ref('')
const weather = ref('获取中...')
const temperature = ref('--')
const dayOfWeek = ref('一')

// 更新时间函数
const updateDateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentDate.value = now.toLocaleDateString('zh-CN')
  
  // 更新星期几
  const days = ['日', '一', '二', '三', '四', '五', '六']
  dayOfWeek.value = days[now.getDay()]
}

// 获取天气数据
const fetchWeather = async () => {
  try {
    // 由于API key未授权，这里使用模拟数据
    const mockWeatherData = {
      weather: '晴',
      temperature: '25',
      humidity: '40',
      windDirection: '东南',
      windPower: '3级'
    }
    
    weather.value = mockWeatherData.weather
    temperature.value = mockWeatherData.temperature + '°C'
    
    // 每隔一段时间随机变化温度，模拟实时效果
    const randomTemp = () => {
      const baseTemp = 25
      const variation = Math.floor(Math.random() * 5) - 2 // -2到2之间的随机数
      return baseTemp + variation
    }
    
    // 每5分钟更新一次温度
    setInterval(() => {
      temperature.value = randomTemp() + '°C'
    }, 5 * 60 * 1000)

  } catch (error) {
    console.error('获取天气数据失败:', error)
    weather.value = '获取失败'
    temperature.value = '--'
  }
}

let timeInterval
let weatherInterval

const router = useRouter()

const goToPage = (path) => {
  router.push(path)
}

// 生命周期钩子 - 挂载
onMounted(() => {
  // 初始化时间
  updateDateTime()
  // 设置定时器每秒更新时间
  timeInterval = setInterval(updateDateTime, 1000)
  
  // 立即获取天气数据
  fetchWeather()
  // 每10分钟更新一次天气
  weatherInterval = setInterval(fetchWeather, 10 * 60 * 1000)
})

// 生命周期钩子 - 卸载前
onBeforeUnmount(() => {
  // 清理定时器
  if (timeInterval) {
    clearInterval(timeInterval)
  }
  if (weatherInterval) {
    clearInterval(weatherInterval)
  }
})
</script>

<style scoped>
/* 添加Font Awesome图标库 */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');

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

.header-left {
  display: flex;
  align-items: center;
}

.time-info {
  margin-right: 20px;
  font-size: 14px;
}

.weather-info {
  font-size: 14px;
}

.header-center {
  font-size: 28px;
  font-weight: bold;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  letter-spacing: 2px;
}

.header-right {
  display: flex;
}

.nav-item {
  margin-left: 15px;
  padding: 6px 18px;
  background-color: rgba(0, 145, 234, 0.2);
  border: 1px solid rgba(0, 145, 234, 0.6);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  color: white;
}

.nav-item:hover {
  background-color: rgba(0, 145, 234, 0.4);
  box-shadow: 0 0 8px rgba(0, 145, 234, 0.6);
}

.nav-item.active {
  background-color: rgba(0, 145, 234, 0.4);
  box-shadow: 0 0 8px rgba(0, 145, 234, 0.6);
}
</style> 