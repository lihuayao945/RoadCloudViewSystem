<template>
  <div class="panel-box vehicle-stream-monitor">
    <div class="chart-title">
      <div class="title-text">
        <i class="title-icon"></i>车流量监控
      </div>
      <div class="controls">
        <div class="time-tabs">
          <div 
            class="tab-item" 
            :class="{ active: timeMode === 'day' }" 
            @click="switchTimeMode('day')"
          >
            按天
          </div>
          <div 
            class="tab-item" 
            :class="{ active: timeMode === 'hour' }" 
            @click="switchTimeMode('hour')"
          >
            按时
          </div>
          <div 
            class="tab-item" 
            :class="{ active: timeMode === 'minute' }" 
            @click="switchTimeMode('minute')"
          >
            按分
          </div>
        </div>
        <span class="refresh-icon" @click="refreshData" title="刷新数据">↻</span>
      </div>
    </div>
    <div class="panel-content">
      <div v-if="noData" class="no-data">
        <el-empty description="暂无车流量数据" :image-size="80">
          <el-button type="primary" size="small" @click="refreshData">刷新数据</el-button>
        </el-empty>
      </div>
      <VehicleStreamChart 
        ref="streamChart" 
        :rcu-id="currentRcuId" 
        :time-range="timeRange" 
        :time-mode="timeMode"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, onBeforeUnmount } from 'vue'
import VehicleStreamChart from './VehicleStreamChart.vue'

// 获取URL参数
const getUrlParam = (name) => {
  const urlParams = new URLSearchParams(window.location.search)
  return urlParams.get(name)
}

// 从URL获取rcuId，如果没有则使用默认值
const currentRcuId = ref(getUrlParam('rcuId') || 'U-WZ000R')

// 处理URL变化
const handleUrlChange = () => {
  const newRcuId = getUrlParam('rcuId')
  if (newRcuId && newRcuId !== currentRcuId.value) {
    currentRcuId.value = newRcuId
    console.log('URL变化，更新rcuId为:', currentRcuId.value)
    // 这里可以添加刷新数据的逻辑
  }
}

// 添加URL变化的事件监听器
onMounted(() => {
  window.addEventListener('popstate', handleUrlChange)
  window.addEventListener('hashchange', handleUrlChange)
  // 检查初始URL
  handleUrlChange()
  console.log('当前使用的rcuId:', currentRcuId.value)
})

// 在组件卸载时移除事件监听器
onBeforeUnmount(() => {
  window.removeEventListener('popstate', handleUrlChange)
  window.removeEventListener('hashchange', handleUrlChange)
})

// 其他现有props不再需要rcuId，因为我们直接从URL获取
const props = defineProps({})

const streamChart = ref(null)
const noData = ref(false)
const timeMode = ref('day') // 默认按天显示

// 根据timeMode计算时间范围
const timeRange = computed(() => {
  const now = Date.now() // 当前时间的毫秒级时间戳
  
  if (timeMode.value === 'day') {
    // 按天显示：当前时间-24小时 到 当前时间
    const dayAgo = now - 24 * 60 * 60 * 1000 // 24小时前的毫秒级时间戳
    return {
      startTime: dayAgo,  // 毫秒级时间戳
      endTime: now  // 毫秒级时间戳
    }
  } else if (timeMode.value === 'hour') {
    // 按时显示：当前时间-1小时 到 当前时间
    const hourAgo = now - 60 * 60 * 1000 // 1小时前的毫秒级时间戳
    return {
      startTime: hourAgo,  // 毫秒级时间戳
      endTime: now  // 毫秒级时间戳
    }
  } else {
    // 按分显示：当前时间-1分钟 到 当前时间
    const minuteAgo = now - 60 * 1000 // 1分钟前的毫秒级时间戳
    return {
      startTime: minuteAgo,  // 毫秒级时间戳
      endTime: now  // 毫秒级时间戳
    }
  }
})

// 切换时间模式
function switchTimeMode(mode) {
  if (timeMode.value !== mode) {
    timeMode.value = mode
    refreshData()
  }
}

// 监听时间模式变化
watch(timeMode, () => {
  console.log('时间模式变更为:', timeMode.value, '时间范围:', timeRange.value)
})

// 刷新数据
function refreshData() {
  if (streamChart.value) {
    streamChart.value.refreshData()
  }
}

// 组件挂载
onMounted(() => {
  refreshData()
})

// 暴露方法给父组件
defineExpose({
  refreshData
})
</script>

<style scoped>
.panel-box {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 4px;
  overflow: hidden;
  max-height: 500px;
  backdrop-filter: blur(5px);
  transition: all 0.5s ease;
  position: relative;
}

.panel-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.7), transparent);
  z-index: 1;
}

.chart-title {
  font-size: 16px;
  margin-bottom: 15px;
  color: #00ffff;
  border-bottom: 1px solid rgba(14, 89, 134, 0.6);
  padding-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-text {
  font-weight: 500;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, #00ffff, #1e90ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.title-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 8px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M3,13h2v-2H3V13z M3,17h2v-2H3V17z M3,9h2V7H3V9z M7,13h14v-2H7V13z M7,17h14v-2H7V17z M7,7v2h14V7H7z"/></svg>');
  background-size: contain;
  background-repeat: no-repeat;
}

.controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-tabs {
  display: flex;
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.1);
}

.tab-item {
  padding: 4px 12px;
  cursor: pointer;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  background-color: rgba(2, 12, 32, 0.5);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
  text-align: center;
  min-width: 42px;
}

.tab-item:hover {
  background-color: rgba(14, 89, 134, 0.4);
  color: rgba(0, 255, 255, 0.9);
}

.tab-item.active {
  background-color: rgba(0, 255, 255, 0.2);
  color: #00ffff;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, rgba(0, 255, 255, 0.2), rgba(0, 255, 255, 0.8), rgba(0, 255, 255, 0.2));
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: rgba(0, 255, 255, 0.7);
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: rgba(14, 89, 134, 0.2);
}

.refresh-icon:hover {
  color: rgba(0, 255, 255, 1);
  transform: rotate(180deg);
  background-color: rgba(14, 89, 134, 0.4);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 10px;
  min-height: 180px;
}

.no-data {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(2, 12, 32, 0.5);
  z-index: 1;
  backdrop-filter: blur(2px);
}

:deep(.el-empty__description) {
  color: #fff;
}

:deep(.el-empty__image) {
  opacity: 0.7;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #1e90ff, #00bfff);
  border-color: #00bfff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #00bfff, #00ffff);
  border-color: #00ffff;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.5);
}
</style> 