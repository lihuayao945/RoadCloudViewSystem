<template>
  <div class="panel-box traffic-monitor">
    <div class="chart-title">车流量监控
      <span class="refresh-icon" @click="refreshTrafficChart">↻</span>
    </div>
    <div class="panel-content">
      <div class="tab-controls">
        <div class="tab-item" :class="{ 'active': activeTab === 'day' }" @click="updateTab('day')">按天</div>
        <div class="tab-item" :class="{ 'active': activeTab === 'week' }" @click="updateTab('week')">按周</div>
        <div class="tab-item" :class="{ 'active': activeTab === 'month' }" @click="updateTab('month')">按月</div>
      </div>
      <TrafficChart ref="trafficChart" :activeTab="activeTab" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import TrafficChart from './TrafficChart.vue'

// 响应式数据
const activeTab = ref('day')
const trafficChart = ref(null)

// 更新标签页
const updateTab = (tab) => {
  activeTab.value = tab
}

// 刷新图表数据
const refreshTrafficChart = () => {
  if (trafficChart.value) {
    trafficChart.value.refreshData()
  }
}

// 暴露方法给父组件
defineExpose({
  refreshTrafficChart
})
</script>

<style scoped>
.panel-box {
  background-color: rgba(2, 12, 32, 0.8);
  border: 1px solid #0e5986;
  box-shadow: 0 0 15px rgba(0, 145, 234, 0.3);
  border-radius: 5px;
  padding: 15px;
  height: calc((100% - 40px) / 3);
  backdrop-filter: blur(5px);
  transition: all 0.5s ease;
}

.chart-title {
  font-size: 16px;
  margin-bottom: 10px;
  color: #00ffff;
  border-bottom: 1px solid rgba(14, 89, 134, 0.6);
  padding-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.refresh-icon {
  cursor: pointer;
  margin-left: 10px;
  font-size: 14px;
  color: rgba(0, 255, 255, 0.7);
  transition: all 0.3s;
}

.refresh-icon:hover {
  color: rgba(0, 255, 255, 1);
  transform: rotate(180deg);
}

.panel-content {
  height: calc(100% - 40px);
  overflow: hidden;
}

.tab-controls {
  display: flex;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.4);
}

.tab-item {
  padding: 6px 12px;
  margin-right: 10px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
}

.tab-item:hover {
  color: #00ffff;
}

.tab-item.active {
  color: #00ffff;
  border-bottom-color: #00ffff;
}
</style> 