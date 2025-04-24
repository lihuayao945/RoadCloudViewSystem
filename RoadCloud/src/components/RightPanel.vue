<template>
  <div class="right-panel panel">
    <div class="panel-content">
      <!-- 1. 设备总览卡片部分 -->
      <section class="panel-section devices-overview">
        <div class="section-header">
          <h3 class="section-title">设备总览</h3>
          <span class="refresh-icon" @click="refreshDataCards">↻</span>
        </div>
        
        <div class="modern-data-cards-container">
          <div class="modern-data-card" v-for="(card, index) in dataCards" :key="index">
            <div class="card-title">{{ card.name }}</div>
            <div class="card-value">{{ formatLargeNumber(card.rawValue || 0) }}</div>
            <div class="waveform-visualization" :class="getWaveformClass(index)"></div>
          </div>
        </div>
      </section>
      
      <!-- 2. 设备健康状态分布 -->
      <section class="panel-section device-health">
        <div class="section-header">
          <h3 class="section-title">设备健康状态分布</h3>
          <span class="refresh-icon" @click="refreshHealthData">↻</span>
        </div>
        
        <div class="device-health-container">
          <DeviceHealthChart ref="deviceHealthChart" :healthData="deviceHealthData" />
        </div>
      </section>
      
      <!-- 3. 路控设备拓扑图 -->
      <section class="panel-section device-topology">
        <div class="section-header">
          <h3 class="section-title">路控设备层级拓扑</h3>
          <div class="controls-container">
            <div class="tag-controls">
              <div class="tag" :class="{ 'active': currentTopologyView === 'hierarchy' }" @click="switchTopologyView('hierarchy')">层级视图</div>
              <div class="tag" :class="{ 'active': currentTopologyView === 'district' }" @click="switchTopologyView('district')">区域分布</div>
            </div>
            <!-- 修改：移除缩放控件，改为单一的展开按钮 -->
            <div class="expand-control">
              <span class="expand-icon" @click="expandTopology" title="查看大图">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M15 3H21V9" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M9 21H3V15" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M21 3L14 10" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M3 21L10 14" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
            </div>
          </div>
        </div>
        
        <div class="topology-container">
          <DeviceTopologyChart ref="topologyChart" :viewType="currentTopologyView" />
        </div>
      </section>
    </div>
  </div>
</template>
  
<script>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { formatNumber } from '../utils/chartUtils'
import DeviceHealthChart from './charts/DeviceHealthChart.vue'
import DeviceTopologyChart from './charts/DeviceTopologyChart.vue'
import axios from 'axios'
  
export default {
  name: 'RightPanel',
  components: {
    DeviceHealthChart,
    DeviceTopologyChart
  },
  setup() {
    // 数据卡片初始化
    const dataCards = ref([
      { name: '激光雷达', value: '0', rawValue: 2, icon: 'icon-lidar' },
      { name: '毫秒波雷达', value: '0', rawValue: 2, icon: 'icon-mmwave' },
      { name: '摄像头', value: '0', rawValue: 8, icon: 'icon-camera' },
      { name: '融合设备', value: '0', rawValue: 2, icon: 'icon-fusion' }
    ])
    
    const deviceHealthChart = ref(null)
    const topologyChart = ref(null)
    // 默认设置为区域分布视图
    const currentTopologyView = ref('district')
    const deviceHealthData = ref({}) // 初始化为空对象
    
    let dataUpdateTimer = null
    let healthDataTimer = null
    
    // 根据索引获取波形样式类
    const getWaveformClass = (index) => {
      const classes = ['blue-yellow', 'blue-green', 'purple-pink', 'orange-teal', 'red-blue'];
      return classes[index % classes.length];
    }
    
    // 格式化大数字
    const formatLargeNumber = (num) => {
      return num;
    }
    
    // 刷新所有面板数据
    function refreshPanelData() {
      refreshDataCards();
      refreshHealthData();
      if (topologyChart.value) {
        topologyChart.value.refreshData();
      }
    }
    
    // 从API获取设备数量
    const fetchDeviceNumbers = async () => {
      try {
        const response = await axios.get('/menu/device/numList')
        
        if (response.data && response.data.status === 'success') {
          const deviceData = response.data.deviceNumList
          
          // 重新格式化卡片数据
          const updatedCards = []
          
          deviceData.forEach(device => {
            const deviceName = Object.keys(device)[0]
            const deviceValue = device[deviceName]
            
            // 明确过滤掉RCU总设备数量 - 不添加到updatedCards中
            if (deviceName !== 'RCU' && deviceName !== 'rcu' && !deviceName.toLowerCase().includes('rcu')) {
              updatedCards.push({
                name: deviceName,
                value: formatNumber(deviceValue),
                rawValue: deviceValue
              })
            }
          })
          
          // 只有在有数据时才更新
          if (updatedCards.length > 0) {
            dataCards.value = updatedCards
          }
          
          showToast('设备数据已更新')
        } else {
          console.error('获取设备数据失败:', response.data)
          showToast('设备数据更新失败', 'error')
        }
      } catch (error) {
        console.error('获取设备数据出错:', error)
        showToast('设备数据更新失败', 'error')
      }
    }
    
    // 从API获取设备健康状态数据
    const fetchDeviceHealthData = async () => {
      try {
        const response = await axios.get('/menu/device/status')
        
        console.log('RightPanel - 设备健康状态API响应:', JSON.stringify(response.data))
        
        if (response.data && response.data.status === 'success') {
          // 直接使用API返回的deviceStatus
          const apiData = response.data.deviceStatus || {}
          
          // 过滤掉RCU相关的数据
          const filteredData = {}
          Object.keys(apiData).forEach(key => {
            if (key !== 'RCU' && key !== 'rcu' && !key.toLowerCase().includes('rcu')) {
              filteredData[key] = apiData[key]
            }
          })
          
          deviceHealthData.value = filteredData
          
          console.log('RightPanel - 设置健康状态数据:', JSON.stringify(deviceHealthData.value))
          
          // 检查deviceHealthData是否为空
          if (Object.keys(deviceHealthData.value).length === 0) {
            console.warn('RightPanel - API返回的deviceStatus为空，使用模拟数据')
            deviceHealthData.value = {
              '摄像头': [10, 5, 3],
              '激光雷达': [20, 2, 1],
              '毫秒波雷达': [5, 2, 1],
              '融合设备': [15, 3, 2]
            }
          }
          
          // 更新设备健康图表
          if (deviceHealthChart.value) {
            // 确保组件已加载
            setTimeout(() => {
              deviceHealthChart.value.updateChart(deviceHealthData.value)
            }, 100)
          }
          
          showToast('设备健康状态数据已更新')
        } else {
          console.error('RightPanel - 获取设备健康状态数据失败:', response.data)
          showToast('设备健康状态数据更新失败', 'error')
          
          // 使用模拟数据作为备选
          deviceHealthData.value = {
            '摄像头': [15, 8, 3],
            '激光雷达': [25, 5, 2],
            '毫秒波雷达': [8, 2, 1],
            '融合设备': [18, 4, 1]
          }
          
          // 更新图表
          if (deviceHealthChart.value) {
            setTimeout(() => {
              deviceHealthChart.value.updateChart(deviceHealthData.value)
            }, 100)
          }
        }
      } catch (error) {
        console.error('RightPanel - 获取设备健康状态数据出错:', error)
        showToast('设备健康状态数据更新失败', 'error')
        
        // 发生错误时使用模拟数据
        deviceHealthData.value = {
          '摄像头': [15, 8, 3],
          '激光雷达': [25, 5, 2],
          '毫秒波雷达': [8, 2, 1],
          '融合设备': [18, 4, 1]
        }
        
        // 更新图表
        if (deviceHealthChart.value) {
          setTimeout(() => {
            deviceHealthChart.value.updateChart(deviceHealthData.value)
          }, 100)
        }
      }
    }
    
    // 展开拓扑图到弹窗
    function expandTopology() {
      console.log('RightPanel - 触发拓扑图弹窗展示')
      if (topologyChart.value) {
        topologyChart.value.expandTopology()
      }
    }
    
    onMounted(() => {
      // 立即获取数据
      fetchDeviceNumbers()
      fetchDeviceHealthData()
      
      // 初始化图表 - 确保延迟足够长，以便组件完全挂载
      setTimeout(() => {
        if (deviceHealthChart.value) {
          deviceHealthChart.value.initChart()
        }
        
        if (topologyChart.value) {
          topologyChart.value.initChart()
        }
      }, 500)


      
      // 设置定时刷新 (每60秒刷新一次)
      dataUpdateTimer = setInterval(fetchDeviceNumbers, 60000)
      healthDataTimer = setInterval(fetchDeviceHealthData, 60000)
      

      // 监听设备相关事件
      window.addEventListener('locate-device', () => {
        setTimeout(() => {
          fetchDeviceNumbers()
          fetchDeviceHealthData()
        }, 1000)
      })

                  // 添加处理刷新右侧面板的事件监听
    const handleRefreshPanel = () => {
        console.log('RightPanel - 接收到刷新事件');
        refreshPanelData();
      };
      window.addEventListener('refresh-right-panel', handleRefreshPanel);
    })
    
    // 组件卸载时清除定时器
    onUnmounted(() => {
      if (dataUpdateTimer) {
        clearInterval(dataUpdateTimer)
      }
      if (healthDataTimer) {
        clearInterval(healthDataTimer)
      }
      
      // 移除事件监听
      window.removeEventListener('locate-device', () => {})
      window.removeEventListener('refresh-right-panel', handleRefreshPanel);
    })
    
    // 监听拓扑图视图切换
    watch(currentTopologyView, (newView) => {
      console.log('RightPanel - 视图类型变化监听器:', newView)
      if (topologyChart.value) {
        setTimeout(() => {
          topologyChart.value.switchView(newView)
        }, 100)
      }
    })
    
    // 添加监视器，确保数据变化时更新图表
    watch(deviceHealthData, (newData) => {
      console.log('deviceHealthData 变化:', newData)
      if (deviceHealthChart.value && Object.keys(newData).length > 0) {
        deviceHealthChart.value.updateChart(newData)
      }
    }, { deep: true })
    
    function switchTopologyView(view) {
      console.log('RightPanel - 切换拓扑视图到:', view)
      currentTopologyView.value = view
      
      // 添加延迟确保子组件更新
      setTimeout(() => {
        if (topologyChart.value) {
          console.log('RightPanel - 直接调用子组件切换视图方法')
          topologyChart.value.switchView(view)
        }
      }, 100)
    }
    
    // 手动刷新设备数据
    function refreshDataCards() {
      // 显示加载动画
      dataCards.value = dataCards.value.map(card => ({
        ...card,
        value: '...'
      }))
      
      // 调用API获取最新数据
      fetchDeviceNumbers()
    }
    
    // 手动刷新设备健康状态数据
    function refreshHealthData() {
      // 调用API获取设备健康状态数据
      fetchDeviceHealthData()
    }
    
    // 显示操作提示
    function showToast(message, type = 'info', duration = 2000) {
      // 创建自定义事件来显示提示
      window.dispatchEvent(new CustomEvent('show-toast', {
        detail: { message, type, duration }
      }))
    }
    
    return {
      dataCards,
      deviceHealthChart,
      topologyChart,
      currentTopologyView,
      deviceHealthData,
      refreshDataCards,
      refreshHealthData,
      switchTopologyView,
      getWaveformClass,
      formatLargeNumber,
      expandTopology,
      refreshPanelData
    }
  }
}
</script>
  
<style scoped>
.right-panel {
  position: absolute;
  top: 4.3%;
  right: 0.5%;
  width: 24%;
  height: 78.5%;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: auto;
  padding-right: 1px;
  z-index: 10;
}

.panel-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(108, 99, 255, 0.6) rgba(4, 34, 53, 0.4);
  height: 100%;
}

.panel-content::-webkit-scrollbar {
  width: 4px;
}

.panel-content::-webkit-scrollbar-track {
  background: rgba(4, 34, 53, 0.4);
}

.panel-content::-webkit-scrollbar-thumb {
  background-color: rgba(108, 99, 255, 0.6);
  border-radius: 4px;
}

/* 通用面板部分样式 */
.panel-section {
  background-color: rgba(8, 24, 44, 0.7);
  border: 1px solid rgba(0, 181, 255, 0.3);
  border-radius: 4px;
  padding: 8px;
  flex-shrink: 0; /* 防止被压缩 */
  box-shadow: 0 0 10px rgba(0, 181, 255, 0.1);
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
  font-weight: bold;
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

/* 新的超酷炫卡片样式 */
.devices-overview {
  height: 20%;
  background-color: rgba(6, 16, 32, 0.9);
  border: 1px solid rgba(0, 131, 204, 0.3);
  border-radius: 8px;
}

.modern-data-cards-container {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 0 2px;
}

.modern-data-card {
  flex: 1;
  min-height: 110px;
  background-color: rgba(7, 27, 47, 0.95);
  border: 1px solid rgba(37, 41, 63, 0.7);
  border-radius: 6px;
  padding: 10px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 8px;
  font-weight: 500;
}

.card-value {
  font-size: 24px;
  font-weight: 700;
  color: white;
  margin: 5px 0 15px 0;
}

/* 波形可视化样式 */
.waveform-visualization {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 28px;
  overflow: hidden;
}

/* 蓝色黄色波形 */
.waveform-visualization.blue-yellow {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100%25' height='28' viewBox='0 0 200 28'%3E%3Crect width='200' height='28' fill='%23091428' /%3E%3Cg%3E%3Crect x='10' y='14' width='3' height='4' rx='1' fill='%23FFC700' /%3E%3Crect x='18' y='12' width='3' height='8' rx='1' fill='%23FFC700' /%3E%3Crect x='26' y='10' width='3' height='12' rx='1' fill='%23FFC700' /%3E%3Crect x='34' y='8' width='3' height='16' rx='1' fill='%23FFC700' /%3E%3Crect x='42' y='6' width='3' height='20' rx='1' fill='%23FFC700' /%3E%3Crect x='50' y='8' width='3' height='16' rx='1' fill='%23FFC700' /%3E%3Crect x='58' y='10' width='3' height='12' rx='1' fill='%23FFC700' /%3E%3Crect x='66' y='12' width='3' height='8' rx='1' fill='%23FFC700' /%3E%3Crect x='74' y='14' width='3' height='4' rx='1' fill='%23FFC700' /%3E%3Crect x='82' y='13' width='3' height='6' rx='1' fill='%23FFC700' /%3E%3Crect x='90' y='12' width='3' height='8' rx='1' fill='%23FFC700' /%3E%3Crect x='98' y='11' width='3' height='10' rx='1' fill='%23FFC700' /%3E%3Crect x='106' y='10' width='3' height='12' rx='1' fill='%23FFC700' /%3E%3Crect x='114' y='9' width='3' height='14' rx='1' fill='%23FFC700' /%3E%3Crect x='122' y='10' width='3' height='12' rx='1' fill='%23FFC700' /%3E%3Crect x='130' y='11' width='3' height='10' rx='1' fill='%23FFC700' /%3E%3Crect x='138' y='12' width='3' height='8' rx='1' fill='%23FFC700' /%3E%3Crect x='146' y='13' width='3' height='6' rx='1' fill='%23FFC700' /%3E%3Crect x='154' y='14' width='3' height='4' rx='1' fill='%23FFC700' /%3E%3Crect x='162' y='15' width='3' height='2' rx='1' fill='%23FFC700' /%3E%3Crect x='170' y='14' width='3' height='4' rx='1' fill='%23FFC700' /%3E%3Crect x='178' y='15' width='3' height='2' rx='1' fill='%23FFC700' /%3E%3Crect x='186' y='15.5' width='3' height='1' rx='0.5' fill='%23FFC700' /%3E%3C/g%3E%3C/svg%3E");
}

/* 蓝色绿色波形 */
.waveform-visualization.blue-green {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100%25' height='28' viewBox='0 0 200 28'%3E%3Crect width='200' height='28' fill='%23091428' /%3E%3Cg%3E%3Crect x='10' y='14' width='3' height='4' rx='1' fill='%2300FF99' /%3E%3Crect x='18' y='12' width='3' height='8' rx='1' fill='%2300FF99' /%3E%3Crect x='26' y='10' width='3' height='12' rx='1' fill='%2300FF99' /%3E%3Crect x='34' y='8' width='3' height='16' rx='1' fill='%2300FF99' /%3E%3Crect x='42' y='6' width='3' height='20' rx='1' fill='%2300FF99' /%3E%3Crect x='50' y='8' width='3' height='16' rx='1' fill='%2300FF99' /%3E%3Crect x='58' y='10' width='3' height='12' rx='1' fill='%2300FF99' /%3E%3Crect x='66' y='12' width='3' height='8' rx='1' fill='%2300FF99' /%3E%3Crect x='74' y='14' width='3' height='4' rx='1' fill='%2300FF99' /%3E%3Crect x='82' y='13' width='3' height='6' rx='1' fill='%2300FF99' /%3E%3Crect x='90' y='12' width='3' height='8' rx='1' fill='%2300FF99' /%3E%3Crect x='98' y='11' width='3' height='10' rx='1' fill='%2300FF99' /%3E%3Crect x='106' y='10' width='3' height='12' rx='1' fill='%2300FF99' /%3E%3Crect x='114' y='9' width='3' height='14' rx='1' fill='%2300FF99' /%3E%3Crect x='122' y='10' width='3' height='12' rx='1' fill='%2300FF99' /%3E%3Crect x='130' y='11' width='3' height='10' rx='1' fill='%2300FF99' /%3E%3Crect x='138' y='12' width='3' height='8' rx='1' fill='%2300FF99' /%3E%3Crect x='146' y='13' width='3' height='6' rx='1' fill='%2300FF99' /%3E%3Crect x='154' y='14' width='3' height='4' rx='1' fill='%2300FF99' /%3E%3Crect x='162' y='15' width='3' height='2' rx='1' fill='%2300FF99' /%3E%3Crect x='170' y='14' width='3' height='4' rx='1' fill='%2300FF99' /%3E%3Crect x='178' y='15' width='3' height='2' rx='1' fill='%2300FF99' /%3E%3Crect x='186' y='15.5' width='3' height='1' rx='0.5' fill='%2300FF99' /%3E%3C/g%3E%3C/svg%3E");
}

/* 紫色粉色波形 */
.waveform-visualization.purple-pink {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100%25' height='28' viewBox='0 0 200 28'%3E%3Crect width='200' height='28' fill='%23120F29' /%3E%3Cg%3E%3Crect x='10' y='14' width='3' height='4' rx='1' fill='%23FF55DD' /%3E%3Crect x='18' y='12' width='3' height='8' rx='1' fill='%23FF55DD' /%3E%3Crect x='26' y='10' width='3' height='12' rx='1' fill='%23FF55DD' /%3E%3Crect x='34' y='8' width='3' height='16' rx='1' fill='%23FF55DD' /%3E%3Crect x='42' y='6' width='3' height='20' rx='1' fill='%23FF55DD' /%3E%3Crect x='50' y='8' width='3' height='16' rx='1' fill='%23FF55DD' /%3E%3Crect x='58' y='10' width='3' height='12' rx='1' fill='%23FF55DD' /%3E%3Crect x='66' y='12' width='3' height='8' rx='1' fill='%23FF55DD' /%3E%3Crect x='74' y='14' width='3' height='4' rx='1' fill='%23FF55DD' /%3E%3Crect x='82' y='13' width='3' height='6' rx='1' fill='%23FF55DD' /%3E%3Crect x='90' y='12' width='3' height='8' rx='1' fill='%23FF55DD' /%3E%3Crect x='98' y='11' width='3' height='10' rx='1' fill='%23FF55DD' /%3E%3Crect x='106' y='10' width='3' height='12' rx='1' fill='%23FF55DD' /%3E%3Crect x='114' y='9' width='3' height='14' rx='1' fill='%23FF55DD' /%3E%3Crect x='122' y='10' width='3' height='12' rx='1' fill='%23FF55DD' /%3E%3Crect x='130' y='11' width='3' height='10' rx='1' fill='%23FF55DD' /%3E%3Crect x='138' y='12' width='3' height='8' rx='1' fill='%23FF55DD' /%3E%3Crect x='146' y='13' width='3' height='6' rx='1' fill='%23FF55DD' /%3E%3Crect x='154' y='14' width='3' height='4' rx='1' fill='%23FF55DD' /%3E%3Crect x='162' y='15' width='3' height='2' rx='1' fill='%23FF55DD' /%3E%3Crect x='170' y='14' width='3' height='4' rx='1' fill='%23FF55DD' /%3E%3Crect x='178' y='15' width='3' height='2' rx='1' fill='%23FF55DD' /%3E%3Crect x='186' y='15.5' width='3' height='1' rx='0.5' fill='%23FF55DD' /%3E%3C/g%3E%3C/svg%3E");
}

/* 橙色青色波形 */
.waveform-visualization.orange-teal {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100%25' height='28' viewBox='0 0 200 28'%3E%3Crect width='200' height='28' fill='%23091428' /%3E%3Cg%3E%3Crect x='10' y='14' width='3' height='4' rx='1' fill='%23FF9500' /%3E%3Crect x='18' y='12' width='3' height='8' rx='1' fill='%23FF9500' /%3E%3Crect x='26' y='10' width='3' height='12' rx='1' fill='%23FF9500' /%3E%3Crect x='34' y='8' width='3' height='16' rx='1' fill='%23FF9500' /%3E%3Crect x='42' y='6' width='3' height='20' rx='1' fill='%23FF9500' /%3E%3Crect x='50' y='8' width='3' height='16' rx='1' fill='%23FF9500' /%3E%3Crect x='58' y='10' width='3' height='12' rx='1' fill='%23FF9500' /%3E%3Crect x='66' y='12' width='3' height='8' rx='1' fill='%23FF9500' /%3E%3Crect x='74' y='14' width='3' height='4' rx='1' fill='%23FF9500' /%3E%3Crect x='82' y='13' width='3' height='6' rx='1' fill='%23FF9500' /%3E%3Crect x='90' y='12' width='3' height='8' rx='1' fill='%23FF9500' /%3E%3Crect x='98' y='11' width='3' height='10' rx='1' fill='%23FF9500' /%3E%3Crect x='106' y='10' width='3' height='12' rx='1' fill='%23FF9500' /%3E%3Crect x='114' y='9' width='3' height='14' rx='1' fill='%23FF9500' /%3E%3Crect x='122' y='10' width='3' height='12' rx='1' fill='%23FF9500' /%3E%3Crect x='130' y='11' width='3' height='10' rx='1' fill='%23FF9500' /%3E%3Crect x='138' y='12' width='3' height='8' rx='1' fill='%23FF9500' /%3E%3Crect x='146' y='13' width='3' height='6' rx='1' fill='%23FF9500' /%3E%3Crect x='154' y='14' width='3' height='4' rx='1' fill='%23FF9500' /%3E%3Crect x='162' y='15' width='3' height='2' rx='1' fill='%23FF9500' /%3E%3Crect x='170' y='14' width='3' height='4' rx='1' fill='%23FF9500' /%3E%3Crect x='178' y='15' width='3' height='2' rx='1' fill='%23FF9500' /%3E%3Crect x='186' y='15.5' width='3' height='1' rx='0.5' fill='%23FF9500' /%3E%3C/g%3E%3C/svg%3E");
}

/* 红色蓝色波形 */
.waveform-visualization.red-blue {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100%25' height='28' viewBox='0 0 200 28'%3E%3Crect width='200' height='28' fill='%23091428' /%3E%3Cg%3E%3Crect x='10' y='14' width='3' height='4' rx='1' fill='%23FF3B30' /%3E%3Crect x='18' y='12' width='3' height='8' rx='1' fill='%23FF3B30' /%3E%3Crect x='26' y='10' width='3' height='12' rx='1' fill='%23FF3B30' /%3E%3Crect x='34' y='8' width='3' height='16' rx='1' fill='%23FF3B30' /%3E%3Crect x='42' y='6' width='3' height='20' rx='1' fill='%23FF3B30' /%3E%3Crect x='50' y='8' width='3' height='16' rx='1' fill='%23FF3B30' /%3E%3Crect x='58' y='10' width='3' height='12' rx='1' fill='%23FF3B30' /%3E%3Crect x='66' y='12' width='3' height='8' rx='1' fill='%23FF3B30' /%3E%3Crect x='74' y='14' width='3' height='4' rx='1' fill='%23FF3B30' /%3E%3Crect x='82' y='13' width='3' height='6' rx='1' fill='%23FF3B30' /%3E%3Crect x='90' y='12' width='3' height='8' rx='1' fill='%23FF3B30' /%3E%3Crect x='98' y='11' width='3' height='10' rx='1' fill='%23FF3B30' /%3E%3Crect x='106' y='10' width='3' height='12' rx='1' fill='%23FF3B30' /%3E%3Crect x='114' y='9' width='3' height='14' rx='1' fill='%23FF3B30' /%3E%3Crect x='122' y='10' width='3' height='12' rx='1' fill='%23FF3B30' /%3E%3Crect x='130' y='11' width='3' height='10' rx='1' fill='%23FF3B30' /%3E%3Crect x='138' y='12' width='3' height='8' rx='1' fill='%23FF3B30' /%3E%3Crect x='146' y='13' width='3' height='6' rx='1' fill='%23FF3B30' /%3E%3Crect x='154' y='14' width='3' height='4' rx='1' fill='%23FF3B30' /%3E%3Crect x='162' y='15' width='3' height='2' rx='1' fill='%23FF3B30' /%3E%3Crect x='170' y='14' width='3' height='4' rx='1' fill='%23FF3B30' /%3E%3Crect x='178' y='15' width='3' height='2' rx='1' fill='%23FF3B30' /%3E%3Crect x='186' y='15.5' width='3' height='1' rx='0.5' fill='%23FF3B30' /%3E%3C/g%3E%3C/svg%3E");
}

/* 设备健康状态图表 */
.device-health {
  height: 37%;
}

.device-health-container {
  height: calc(100% - 30px);
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(0, 181, 255, 0.3);
  border-radius: 4px;
  padding: 4px;
}

/* 路控设备拓扑图 */
.device-topology {
  height: 40%; 
  position: relative; /* 添加相对定位 */
}

.topology-container {
  height: calc(100% - 30px);
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(0, 181, 255, 0.3);
  border-radius: 4px;
  padding: 4px;
  position: relative; /* 确保可以容纳绝对定位的子元素 */
}

/* 控制容器样式 - 新增 */
.controls-container {
  display: flex;
  align-items: center;
  gap: 8px;
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
  border: 1px solid rgba(0, 181, 255, 0.3);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag.active {
  background-color: rgba(0, 181, 255, 0.6);
  border-color: rgba(0, 181, 255, 0.8);
}

/* 修改: 移除缩放控件样式，添加展开控件样式 */
.expand-control {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 5px;
}

.expand-icon {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 70, 120, 0.3);
  border: 1px solid rgba(0, 181, 255, 0.4);
  border-radius: 3px;
  color: rgba(0, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.expand-icon:hover {
  background-color: rgba(0, 181, 255, 0.4);
  color: #ffffff;
  transform: scale(1.05);
}

.expand-icon:active {
  transform: scale(0.95);
}

/* 响应式调整 */
@media (max-width: 1600px) {
  .modern-data-cards-container {
    flex-wrap: nowrap;
  }
  
  .card-value {
    font-size: 20px;
  }
  
  .card-title {
    font-size: 11px;
  }

  .device-health {
    height: 180px;
  }
  
  .device-topology {
    height: 300px;
  }
}

@media (max-width: 1200px) {
  .right-panel {
    padding-right: 1px;
  }
  
  .modern-data-cards-container {
    flex-wrap: wrap;
  }
  
  .modern-data-card {
    flex: 1 1 calc(50% - 6px);
    margin-bottom: 6px;
  }
  
  .device-health {
    height: 160px;
  }
  
  .device-topology {
    height: 280px;
  }
}

@media (max-width: 992px) {
  .card-value {
    font-size: 18px;
  }
  
  .card-title {
    font-size: 10px;
  }

  .device-health {
    height: 150px;
  }
  
  .device-topology {
    height: 260px;
  }
}
</style>