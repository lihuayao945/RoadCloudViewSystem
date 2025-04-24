<template>
  <div class="left-panel panel">
    <!-- 添加车辆搜索组件 -->
    <VehicleSearch @search-active="onSearchActive" @search-cleared="onSearchCleared" />
    
    <!-- 左侧面板内容 - 当搜索不活跃时显示 -->
    <div v-show="!searchActive" class="panel-content">
      <!-- 1. 实时路口统计数据卡片 (替代原来的流量监控图表) -->
      <section class="panel-section traffic-stats">
        <div class="section-header">
          <h3 class="section-title">实时路口流量</h3>
          <span class="refresh-icon" @click="refreshTrafficStats">↻</span>
        </div>
        <div class="stats-cards">
          <!-- 车辆总数卡片 -->
          <div class="stat-card">
            <div class="stat-title">车辆总数</div>
            <div class="stat-value-container">
              <div class="stat-value">{{ formatNumber(trafficStats.vehicleCount) }}</div>
              <div class="stat-change" :class="getChangeClass(trafficStats.vehicleDiff)">
                {{ trafficStats.vehicleDiff > 0 ? '+' : '' }}{{ trafficStats.vehicleDiff }}
              </div>
            </div>
            <div class="stat-footer">
              <div class="stat-icon vehicle-icon"></div>
              <div class="stat-indicator">
                <div class="indicator-bar"></div>
              </div>
            </div>
          </div>
          
          <!-- 行人总数卡片 -->
          <div class="stat-card">
            <div class="stat-title">行人总数</div>
            <div class="stat-value-container">
              <div class="stat-value">{{ formatNumber(trafficStats.pedestrianCount) }}</div>
              <div class="stat-change" :class="getChangeClass(trafficStats.pedestrianDiff)">
                {{ trafficStats.pedestrianDiff > 0 ? '+' : '' }}{{ trafficStats.pedestrianDiff }}
              </div>
            </div>
            <div class="stat-footer">
              <div class="stat-icon pedestrian-icon"></div>
              <div class="stat-indicator">
                <div class="indicator-bar"></div>
              </div>
            </div>
          </div>
        </div>
      </section>
      
      <!-- 2. 交通模式分析图表 -->
      <section class="panel-section traffic-pattern">
        <div class="section-header">
          <h3 class="section-title">交通模式分析</h3>
          <div class="tab-controls-small">
            <div class="tab-item-small" :class="{ 'active': activePatternTab === 'minute' }" @click="activePatternTab = 'minute'">分钟</div>
            <div class="tab-item-small" :class="{ 'active': activePatternTab === 'hour' }" @click="activePatternTab = 'hour'">小时</div>
            <div class="tab-item-small" :class="{ 'active': activePatternTab === 'day' }" @click="activePatternTab = 'day'">天</div>
          </div>
          <span class="refresh-icon" @click="refreshPatternData">↻</span>
        </div>
        
        <div class="pattern-container">
          <PatternChart ref="patternChart" :activeTab="activePatternTab" />
        </div>
      </section>

      <!-- 3. 路口流量动态排名 (从右侧移动过来的) -->
      <section class="panel-section dynamic-ranking">
        <div class="section-header">
          <h3 class="section-title">路口流量动态排名</h3>
          <span class="refresh-icon" @click="refreshRankingData">↻</span>
        </div>
        
        <div class="dynamic-chart-container">
          <DynamicRankingChart ref="rankingChart" />
        </div>
      </section>
      
    </div>
  </div>
</template>
 
<script>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import PatternChart from './charts/PatternChart.vue'
import DynamicRankingChart from './charts/DynamicRankingChart.vue'
import DistrictChart from './charts/DistrictChart.vue'
import VehicleSearch from './VehicleSearch.vue' // 导入新的车辆搜索组件
import axios from 'axios'
  
export default {
  name: 'LeftPanel',
  components: {
    PatternChart,
    DynamicRankingChart,
    DistrictChart,
    VehicleSearch
  },
  setup() {
    const patternChart = ref(null)
    const rankingChart = ref(null)
    const districtChart = ref(null)
    const activePatternTab = ref('minute') // 默认显示分钟视图
    const searchActive = ref(false) // 控制搜索是否激活
    
    // 实时路口统计数据
    const trafficStats = ref({
      vehicleCount: 3456,  // 初始值
      vehicleDiff: 0,
      pedestrianCount: 1234, // 初始值
      pedestrianDiff: 0
    })
    
    let trafficStatsInterval = null
    
    // 处理搜索激活和清除事件
    function onSearchActive() {
      searchActive.value = true;
      console.log('搜索激活，隐藏左侧面板内容');
    }
    
    function onSearchCleared() {
      searchActive.value = false;
      console.log('搜索清除，显示左侧面板内容');
      
      // 当搜索被清除时，刷新图表数据
      setTimeout(() => {
        refreshPanelData();
      }, 300);
    }
    
    // 刷新所有面板数据
    function refreshPanelData() {
      refreshTrafficStats();
      refreshPatternData();
      refreshRankingData();
    }
    
    // 格式化数字
    function formatNumber(num) {
      return num.toLocaleString()
    }
    
    // 获取实时路口统计数据
    async function fetchTrafficStats() {
      try {
        const timestamp = new Date().getTime()
        const response = await axios.get(`/menu/vehicle/count?timestamp=${timestamp}`)
        
        if (response && response.data && response.data.status === 'success') {
          // 存储之前的值，用于计算差异
          const oldVehicleCount = trafficStats.value.vehicleCount
          const oldPedestrianCount = trafficStats.value.pedestrianCount
          
          // 更新到新值
          trafficStats.value = {
            vehicleCount: response.data.vehicleCount,
            vehicleDiff: response.data.vehicleCount - oldVehicleCount,
            pedestrianCount: response.data.pedestrianCount,
            pedestrianDiff: response.data.pedestrianCount - oldPedestrianCount
          }
        } else {
          throw new Error('返回数据格式不正确');
        }
      } catch (error) {
        console.error('获取路口统计数据异常:', error)
        // 当 API 请求失败时，保持当前数据不变而不是使用模拟数据
      }
    }
    
    // 刷新路口统计数据
    function refreshTrafficStats() {
      fetchTrafficStats()
      showToast('实时路口统计已更新')
    }
    
    // 刷新交通模式分析数据
    function refreshPatternData() {
      if (patternChart.value) {
        patternChart.value.refreshData()
        showToast('交通模式分析已更新')
      }
    }
    
    // 刷新排名数据
    function refreshRankingData() {
      if (rankingChart.value) {
        rankingChart.value.updateChart()
        showToast('路口流量动态排名已更新')
      }
    }
    
    // 获取变化样式类
    function getChangeClass(diff) {
      if (diff > 0) return 'change-increase'
      if (diff < 0) return 'change-decrease'
      return 'change-neutral'
    }
    
    function refreshDistrictData() {
      if (districtChart.value) {
        districtChart.value.refreshData()
        showToast('区域交通数据已更新')
      }
    }
    
    // 显示操作提示
    function showToast(message, type = 'info', duration = 2000) {
      // 创建自定义事件来显示提示
      window.dispatchEvent(new CustomEvent('show-toast', {
        detail: { message, type, duration }
      }))
    }
    
    // 初始化图表
    function initCharts() {
      // 初始化交通模式分析图
      if (patternChart.value) {
        try {
          patternChart.value.initChart();
        } catch (error) {
          console.error('初始化交通模式图表失败:', error);
        }
      }
      
      // 初始化动态排名图
      if (rankingChart.value) {
        try {
          rankingChart.value.initChart();
        } catch (error) {
          console.error('初始化动态排名图表失败:', error);
        }
      }
      
      // 初始化区域交通图
      if (districtChart.value) {
        try {
          districtChart.value.initChart();
        } catch (error) {
          console.error('初始化区域交通图表失败:', error);
        }
      }
    }
    
    // 初始化
    onMounted(() => {
      // 初始化实时路口统计数据
      fetchTrafficStats();
      
      // 延迟初始化图表，确保DOM已经渲染
      setTimeout(() => {
        initCharts();
      }, 500);
      
      // 设置定时更新路口统计数据（每30秒更新一次）
      trafficStatsInterval = setInterval(fetchTrafficStats, 30000);
      
      // 监听车辆轨迹和定位事件以更新图表
      const updateChartsOnVehicleActivity = () => {
        if (!searchActive.value) {
          setTimeout(() => {
            refreshPanelData();
          }, 1000);
        }
      };
      
      window.addEventListener('locate-vehicle', updateChartsOnVehicleActivity);
      window.addEventListener('show-trajectory', updateChartsOnVehicleActivity);
      
      // 在组件卸载时清除定时器和事件监听
      onBeforeUnmount(() => {
        clearInterval(trafficStatsInterval);
        window.removeEventListener('locate-vehicle', updateChartsOnVehicleActivity);
        window.removeEventListener('show-trajectory', updateChartsOnVehicleActivity);
      });
    });
    
    // 监听标签页变化
    watch(activePatternTab, (newTab) => {
      if (patternChart.value) {
        try {
          patternChart.value.switchTab(newTab);
        } catch (error) {
          console.error('切换交通模式标签页失败:', error);
        }
      }
    });
    
    return {
      patternChart,
      rankingChart,
      districtChart,
      activePatternTab,
      trafficStats,
      searchActive,
      refreshTrafficStats,
      refreshPatternData,
      refreshRankingData,
      refreshDistrictData,
      getChangeClass,
      formatNumber,
      onSearchActive,
      onSearchCleared
    }
  }
}
</script>
  
<style scoped>
.left-panel {
  position: absolute;
  top: 1%;
  left: 0.5%;
  width: 24%;
  height: 70.5%; 
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: visible; /* 修改为visible，让搜索结果可以溢出显示 */
  padding-right: 2px;
  z-index: 10; /* 增加z-index确保搜索结果可见 */
}

.panel-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 145, 234, 0.6) rgba(4, 34, 53, 0.4);
  height: 100%;
  margin-top: 6px;
}

.panel-content::-webkit-scrollbar {
  width: 4px;
}

.panel-content::-webkit-scrollbar-track {
  background: rgba(4, 34, 53, 0.4);
}

.panel-content::-webkit-scrollbar-thumb {
  background-color: rgba(0, 145, 234, 0.6);
  border-radius: 4px;
}

/* 通用面板部分样式 */
.panel-section {
  background-color: rgba(4, 34, 53, 0.4);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 4px;
  padding: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.section-title {
  font-size: 14px;
  color: #00ffff;
  font-weight: normal;
  margin: 0;
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: #00ffff;
  transition: transform 0.3s;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-icon:hover {
  transform: rotate(180deg);
  color: #ffffff;
}

/* 小尺寸tab控件 */
.tab-controls-small {
  display: flex;
  border: 1px solid rgba(0, 145, 234, 0.4);
  border-radius: 3px;
  overflow: hidden;
  margin-left: auto;
}

.tab-item-small {
  padding: 2px 8px;
  font-size: 12px;
  cursor: pointer;
  background-color: rgba(4, 34, 53, 0.4);
  transition: all 0.2s;
  text-align: center;
}

.tab-item-small.active {
  background-color: rgba(0, 145, 234, 0.4);
  color: white;
}

/* 标签控件样式 */
.tag-controls {
  display: flex;
  gap: 6px;
}

.tag {
  padding: 2px 8px;
  font-size: 12px;
  background-color: rgba(4, 34, 53, 0.6);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag.active {
  background-color: rgba(0, 145, 234, 0.6);
  border-color: rgba(0, 145, 234, 0.8);
}

/* 实时路口统计数据卡片样式 (新样式) */
.traffic-stats {
  height: 180px;
  min-height: 180px;
  flex-shrink: 0;
}

.stats-cards {
  display: flex;
  gap: 10px;
  height: calc(100% - 30px);
}

.stat-card {
  flex: 1;
  background: linear-gradient(135deg, rgba(0, 12, 36, 0.9), rgba(0, 24, 48, 0.85));
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
  transition: transform 0.3s, box-shadow 0.3s;
  border: 1px solid rgba(21, 101, 192, 0.3);
  padding: 12px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(0, 123, 255, 0.3);
  border-color: rgba(21, 101, 192, 0.6);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #00a3ff, #00c3ff);
  z-index: 1;
}

.stat-title {
  font-size: 14px;
  font-weight: 600;
  color: #00ffff;
  margin-bottom: 5px;
  background: linear-gradient(90deg, #00a3ff, #00ffff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0px 0px 5px rgba(0, 255, 255, 0.3);
}

.stat-date {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
}

.stat-value-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin-bottom: 4px;
  text-shadow: 0 0 8px rgba(0, 195, 255, 0.6);
  letter-spacing: 1px;
}

.stat-change {
  font-size: 14px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

.change-increase {
  color: #00ff9d;
  background-color: rgba(12, 218, 123, 0.1);
  border-left: 3px solid #00ff9d;
}

.change-decrease {
  color: #ff4d4f;
  background-color: rgba(255, 77, 79, 0.1);
  border-left: 3px solid #ff4d4f;
}

.change-neutral {
  color: #ffad33;
  background-color: rgba(255, 173, 51, 0.1);
  border-left: 3px solid #ffad33;
}

.stat-footer {
  display: flex;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(33, 150, 243, 0.2);
  position: relative;
}

.stat-icon {
  width: 24px;
  height: 24px;
  margin-right: 8px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  filter: drop-shadow(0 0 3px rgba(0, 195, 255, 0.5));
}

.stat-indicator {
  flex: 1;
  height: 4px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.indicator-bar {
  height: 100%;
  width: 70%;
  background: linear-gradient(90deg, #00c3ff, #00ffff);
  border-radius: 2px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.6;
  }
}

.vehicle-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%2300c3ff'%3E%3Cpath d='M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z'/%3E%3C/svg%3E");
}

.pedestrian-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%2300c3ff'%3E%3Cpath d='M13.5 5.5c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zM9.8 8.9L7 23h2.1l1.8-8 2.1 2v6h2v-7.5l-2.1-2 .6-3C14.8 12 16.8 13 19 13v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1L6 8.3V13h2V9.6l1.8-.7'/%3E%3C/svg%3E");
}

/* 交通模式分析样式 */
.traffic-pattern {
  height: 38%;
  min-height: 180px;
}

.pattern-container {
  height: calc(100% - 30px);
  border: 1px solid rgba(14, 89, 134, 0.3);
  border-radius: 3px;
  overflow: hidden;
}

/* 动态排名样式 */
.dynamic-ranking {
  height: 35%;
}

.dynamic-chart-container {
  height: calc(100% - 30px);
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 4px;
  padding: 4px;
}

/* 区域数据对比样式 */
.district-stats {
  height: 200px;
}

.district-chart-container {
  height: calc(100% - 30px);
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 4px;
  padding: 4px;
}

/* 响应式调整 */
@media (max-width: 1600px) {
  .traffic-stats {
    height: 160px;
    min-height: 160px;
  }
  
  .traffic-pattern {
    height: 160px;
    min-height: 160px;
  }
  
  .dynamic-ranking, .district-stats {
    height: 180px;
  }
  
  .stat-value {
    font-size: 28px;
  }
}

@media (max-width: 1200px) {
  .traffic-stats {
    height: 150px;
    min-height: 150px;
  }
  
  .traffic-pattern {
    height: 150px;
    min-height: 150px;
  }
  
  .dynamic-ranking, .district-stats {
    height: 160px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .stat-card {
    padding: 10px;
  }
}

@media (max-width: 992px) {
  .traffic-stats {
    height: 140px;
    min-height: 140px;
  }
  
  .traffic-pattern {
    height: 140px;
    min-height: 140px;
  }
  
  .dynamic-ranking, .district-stats {
    height: 150px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .stat-card {
    padding: 8px;
  }
  
  .stat-change {
    font-size: 12px;
  }
  
  .stat-footer {
    margin-top: 8px;
    padding-top: 8px;
  }
}
</style>