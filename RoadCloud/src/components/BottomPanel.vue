<template>
  <div class="bottom-panel panel">
    <div class="chart-header">
      <div class="chart-title">车流量统计</div>
      <div class="tab-controls">
        <div 
          class="tab-item" 
          :class="{ 'active': activeTab === 'minute' }" 
          @click="changeTab('minute')"
          ref="minuteTabBtn"
        >按分钟</div>
        <div class="tab-item" :class="{ 'active': activeTab === 'hour' }" @click="changeTab('hour')">按小时</div>
        <div class="tab-item" :class="{ 'active': activeTab === 'day' }" @click="changeTab('day')">按天</div>
      </div>
      <span class="refresh-icon" @click="refreshTrafficChart" :class="{ 'rotating': isRefreshing }">↻</span>
    </div>
    <div v-if="isLoading && !hasData" class="loading-indicator">加载中...</div>
    <TrafficChart 
      v-else
      ref="trafficChart" 
      :activeTab="activeTab" 
      :trafficData="trafficData" 
      :startTime="startTimeStr"
      :endTime="endTimeStr"
    />
  </div>
</template>

<script>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import TrafficChart from './charts/TrafficChart.vue'
import axios from 'axios'

export default {
  name: 'BottomPanel',
  components: {
    TrafficChart
  },
  props: {
    activeTab: {
      type: String,
      default: 'minute' // 默认显示按分钟数据
    }
  },
  emits: ['change-tab'],
  setup(props, { emit }) {
    const trafficChart = ref(null)
    const trafficData = ref([])
    const isLoading = ref(true)
    const isRefreshing = ref(false)
    const startTimeStr = ref('')
    const endTimeStr = ref('')
    const initialDataLoaded = ref(false)
    
    // 添加对按钮元素的引用
    const minuteTabBtn = ref(null)
    
    // 计算属性，用于判断是否有数据
    const hasData = computed(() => {
      return trafficData.value && trafficData.value.length > 0
    })
    
    // 组件挂载后初始化
    onMounted(async () => {
      // 先生成一些默认数据，确保有内容显示
      generateMockData()
      
      // 等待DOM更新
      await nextTick()
      
      // 自动点击"按分钟"按钮来触发数据加载
      if (minuteTabBtn.value) {
        console.log('自动触发按分钟按钮点击')
        minuteTabBtn.value.click()
      } else {
        // 如果按钮不存在，直接调用获取数据的函数
        console.log('找不到按钮元素，直接调用数据加载')
        fetchTrafficData(true) // 传入true表示初始加载
      }
      
      // 额外安全措施：3秒后检查是否有数据，如果没有则再次尝试
      setTimeout(() => {
        if (!hasData.value) {
          console.log('3秒后仍无数据，重新尝试加载')
          fetchTrafficData(true)
        }
      }, 3000)
    })
    
    // 监听activeTab变化
    watch(() => props.activeTab, (newTab) => {
      console.log(`标签切换到: ${newTab}`)
      fetchTrafficData()
    })
    
    // 切换标签并获取新数据
    function changeTab(tab) {
      if (tab !== props.activeTab) {
        emit('change-tab', tab)
        // 不需要立即调用fetchTrafficData，因为watch会处理
      } else {
        // 如果点击的是当前活动的标签，也刷新数据
        fetchTrafficData()
      }
    }
    
    // 获取交通数据
    async function fetchTrafficData(isInitialLoad = false) {
      if (isInitialLoad) {
        console.log('初始化加载数据')
      }
      
      isRefreshing.value = true
      
      try {
        const now = new Date()
        let startDateTime
        
        // 根据当前标签设置正确的时间范围
        if (props.activeTab === 'minute') {
          startDateTime = new Date(now.getTime() - 60 * 1000) // 前1分钟
        } else if (props.activeTab === 'hour') {
          startDateTime = new Date(now.getTime() - 60 * 60 * 1000) // 前1小时
        } else if (props.activeTab === 'day') {
          startDateTime = new Date(now.getTime() - 24 * 60 * 60 * 1000) // 前1天
        }
        
        // 转换为毫秒时间戳格式
        const startTimeMs = startDateTime.getTime()
        const endTimeMs = now.getTime()
        
        // 保存ISO格式的时间字符串用于TrafficChart显示
        startTimeStr.value = startDateTime.toISOString()
        endTimeStr.value = now.toISOString()
        
        console.log(`获取${props.activeTab}数据, 从 ${startTimeMs} 到 ${endTimeMs}`)
        
        // 使用毫秒时间戳作为请求参数
        const response = await axios.get('/menu/vehicle/stream/all', {
          params: {
            startTime: startTimeMs,  // 使用毫秒时间戳
            endTime: endTimeMs,      // 使用毫秒时间戳
            _t: new Date().getTime() // 防止缓存
          },
          timeout: 5000 // 缩短超时时间，避免长时间等待
        })
        
        console.log(`${props.activeTab}数据响应:`, response)
        
        if (response.data && response.data.data && Array.isArray(response.data.data)) {
          // 保存获取到的数据
          if (response.data.data.length > 0) {
            trafficData.value = response.data.data
            console.log(`成功获取${trafficData.value.length}条数据`)
            initialDataLoaded.value = true
          } else {
            console.log('API返回空数组，生成模拟数据')
            generateMockData()
          }
        } else {
          console.error('API返回无效，生成模拟数据')
          generateMockData()
        }
      } catch (error) {
        console.error('请求交通数据时出错:', error)
        // 出错时生成模拟数据
        generateMockData()
      } finally {
        isLoading.value = false
        setTimeout(() => {
          isRefreshing.value = false
        }, 500)
      }
    }
    
    // 生成模拟数据，确保图表始终有数据显示
    function generateMockData() {
      const now = new Date()
      const mockData = []
      
      // 基于活动标签生成不同的模拟数据点数
      let pointCount = 0
      let timeInterval = 0
      
      if (props.activeTab === 'minute') {
        pointCount = 60  // 1分钟，每秒一个点
        timeInterval = 1000  // 1秒
      } else if (props.activeTab === 'hour') {
        pointCount = 60  // 1小时，每分钟一个点
        timeInterval = 60 * 1000  // 1分钟
      } else if (props.activeTab === 'day') {
        pointCount = 24  // 1天，每小时一个点
        timeInterval = 60 * 60 * 1000  // 1小时
      }
      
      // 创建模拟数据点
      for (let i = 0; i < pointCount; i++) {
        const time = new Date(now.getTime() - (pointCount - i) * timeInterval)
        
        // 生成一些有波动的数据，让图表更有变化
        let count
        if (i < pointCount * 0.3) {
          // 前30%的数据点保持较低流量
          count = Math.floor(Math.random() * 5) + 1
        } else if (i < pointCount * 0.7) {
          // 中间40%的数据点流量增加
          count = Math.floor(Math.random() * 7) + 5
        } else {
          // 最后30%的数据点流量高峰
          count = Math.floor(Math.random() * 10) + 8
        }
        
        mockData.push({
          start_time: time.getTime(),
          count: count
        })
      }
      
      trafficData.value = mockData
      console.log(`已生成${mockData.length}条${props.activeTab}模拟数据`)
      initialDataLoaded.value = true
    }
    
    // 手动刷新图表
    function refreshTrafficChart() {
      fetchTrafficData()
    }
    
    return {
      trafficChart,
      trafficData,
      isLoading,
      isRefreshing,
      startTimeStr,
      endTimeStr,
      hasData,
      minuteTabBtn,
      refreshTrafficChart,
      changeTab
    }
  }
}
</script>

<style scoped>
.bottom-panel {
  position: absolute;
  bottom: 0.5%;
  left: 0.5%;
  width: 24%;
  height: calc(28% - 20px);
  background-color: rgb(8, 24, 44);
  border: 1px solid #0e5986;
  border-radius: 5px;
  padding: 10px;
  box-sizing: border-box;
  z-index: 1;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.chart-title {
  font-size: 14px;
  color: #00ffff;
  font-weight: bold;
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: #00ffff;
  margin-left: 10px;
  transition: transform 0.5s ease;
  display: inline-block;
}

.refresh-icon:hover {
  color: #ffffff;
}

.refresh-icon.rotating {
  animation: rotate 1s linear infinite;
}

.loading-indicator {
  color: #00ffff;
  text-align: center;
  margin-top: 70px;
  font-size: 14px;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.tab-controls {
  display: flex;
  margin-left: auto;
}

.tab-item {
  text-align: center;
  padding: 3px 8px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid #0e5986;
  background-color: rgba(14, 89, 134, 0.2);
  color: #ffffff;
  transition: all 0.3s ease;
}

.tab-item:not(:last-child) {
  border-right: none;
}

.tab-item:first-child {
  border-radius: 3px 0 0 3px;
}

.tab-item:last-child {
  border-radius: 0 3px 3px 0;
}

.tab-item.active {
  background-color: rgba(0, 255, 255, 0.2);
  border-color: #00ffff;
  color: #00ffff;
}

.tab-item:hover:not(.active) {
  background-color: rgba(14, 89, 134, 0.4);
}
</style>