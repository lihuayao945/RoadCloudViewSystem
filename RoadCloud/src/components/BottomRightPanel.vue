<template>
  <div class="bottom-right-panel panel">
    <div class="panel-header">
      <div class="panel-title">通知公告栏</div>
      <div class="controls">
        <div class="tab-container">
          <div 
            class="tab" 
            :class="{ active: activeTab === 'abnormal' }" 
            @click="switchTab('abnormal')"
          >
            异常设备告警
            <div class="badge" v-if="abnormalDevices.length > 0">{{ abnormalDevices.length }}</div>
          </div>
          <div 
            class="tab" 
            :class="{ active: activeTab === 'traffic' }" 
            @click="switchTab('traffic')"
          >
            交通事件
            <div class="badge" v-if="trafficEvents.length > 0">{{ trafficEvents.length }}</div>
          </div>
        </div>
        <span class="refresh-icon" @click="refreshCurrentTab">↻</span>
      </div>
    </div>

    <!-- 异常设备告警页面 -->
    <div class="alert-list" v-if="activeTab === 'abnormal'" ref="abnormalList">
      <div 
        class="alert-item" 
        v-for="(device, index) in abnormalDevices" 
        :key="'abnormal-' + index"
      >
        <div class="alert-indicator" :class="'alert-' + device.abnormalType"></div>
        <div class="alert-time">{{ formatTime(device.latestTime) }}</div>
        <div class="alert-content">
          <div class="alert-title">{{ device.deviceType }} - {{ device.rcuId }}</div>
          <div class="alert-location">最后响应: {{ formatTimeAgo(device.latestTime) }}</div>
        </div>
        <div class="alert-tag" :class="'tag-' + device.abnormalType">{{ device.abnormalType }}</div>
      </div>
      
      <div class="empty-alerts" v-if="abnormalDevices.length === 0">
        <div class="empty-icon">🔔</div>
        <div class="empty-text">无异常设备</div>
      </div>
    </div>

    <!-- 交通事件页面 -->
    <div class="alert-list" v-if="activeTab === 'traffic'" ref="trafficList">
      <div 
        class="alert-item" 
        v-for="(event, index) in trafficEvents" 
        :key="'traffic-' + index"
      >
        <div class="alert-indicator" :class="'alert-' + getEventLevelClass(event.eventType)"></div>
        <div class="alert-time">{{ formatTime(event.timestamp) }}</div>
        <div class="alert-content">
          <div class="alert-title">{{ event.description }} - {{ event.eventId }}</div>
          <div class="alert-location">{{ event.intersectionName }}</div>
        </div>
        <div class="alert-tag" :class="'tag-' + getEventLevelClass(event.eventType)">{{ getEventTypeLabel(event.eventType) }}</div>
      </div>
      
      <div class="empty-alerts" v-if="trafficEvents.length === 0">
        <div class="empty-icon">🚦</div>
        <div class="empty-text">无交通事件</div>
      </div>
    </div>
    
    <!-- Toast通知 -->
    <transition name="toast-fade">
      <div class="toast-notification" v-if="toast.show" :class="'toast-' + toast.type">
        <span class="toast-icon" v-if="toast.type === 'success'">✓</span>
        <span class="toast-icon" v-else-if="toast.type === 'warning'">!</span>
        <span class="toast-icon" v-else-if="toast.type === 'error'">✕</span>
        <span class="toast-icon" v-else>i</span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>
  
<script>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { generateRandomValue } from '../utils/chartUtils'

export default {
  name: 'BottomRightPanel',
  setup() {
    const activeTab = ref('abnormal') // 默认选中异常设备告警
    const abnormalDevices = ref([])
    const trafficEvents = ref([])
    
    const abnormalList = ref(null)
    const trafficList = ref(null)
    
    // Toast通知状态
    const toast = ref({
      show: false,
      message: '',
      type: 'info', // info, success, warning, error
      timer: null
    })
    
    // 监听Toast事件 - 仅接收来自上层组件的通知，不为自身操作显示
    const handleToast = (event) => {
      const { message, type, duration } = event.detail
      showToast(message, type, duration)
    }
    
    onMounted(() => {
      window.addEventListener('show-toast', handleToast)
      
      // 初始加载异常设备数据
      fetchAbnormalDevices()

      // 设置定时刷新
      const refreshInterval = setInterval(() => {
        if (activeTab.value === 'abnormal') {
          fetchAbnormalDevices(false) // 不显示通知
        } else {
          fetchTrafficEvents(false) // 不显示通知
        }
      }, 30000) // 每30秒刷新一次

      // 添加数据动态更新
      const dynamicUpdateInterval = setInterval(() => {
        // 随机添加新的异常设备或交通事件
        if (Math.random() > 0.5) {
          addRandomAbnormalDevice()
        } else {
          addRandomTrafficEvent()
        }
      }, 15000) // 每15秒可能添加一个新项目，方便测试

      return () => {
        clearInterval(refreshInterval)
        clearInterval(dynamicUpdateInterval)
      }
    })
    
    onUnmounted(() => {
      window.removeEventListener('show-toast', handleToast)
    })
    
    // 添加随机新异常设备
    function addRandomAbnormalDevice() {
      const deviceTypes = ['摄像头', '信号灯', '雷达', '路况检测器', '电子屏']
      const abnormalTypes = ['告警', '离线']
      
      const deviceType = deviceTypes[Math.floor(Math.random() * deviceTypes.length)]
      const abnormalType = abnormalTypes[Math.floor(Math.random() * abnormalTypes.length)]
      const latestTime = Date.now()
      const rcuId = `RCU-${Math.floor(Math.random() * 10000).toString().padStart(5, '0')}`
      
      const newDevice = {
        latestTime,
        rcuId,
        deviceType,
        abnormalType
      }
      
      // 添加到列表前端
      abnormalDevices.value.unshift(newDevice)
      
      // 移除toast通知显示
    }
    
    // 添加随机新交通事件
    function addRandomTrafficEvent() {
      const descriptions = ['慢行', '快行', '紧急制动', '车辆追尾', '道路拥堵', '交通管制']
      const locations = ['石坪桥路口2', '三峡广场路口1', '临江路口1', '高新区路口', '城东大桥', '西环路出口']
      const eventTypes = [1, 2, 3]
      
      const eventType = eventTypes[Math.floor(Math.random() * eventTypes.length)]
      const description = descriptions[Math.floor(Math.random() * descriptions.length)]
      const intersectionName = locations[Math.floor(Math.random() * locations.length)]
      const timestamp = Date.now().toString()
      const eventId = `E${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`
      const rcuId = `U-WZ${Math.floor(Math.random() * 10000).toString().padStart(4, '0')}`
      const longitude = (106 + Math.random()).toFixed(4)
      const latitude = (29 + Math.random()).toFixed(4)
      
      const newEvent = { 
        eventId, 
        eventType, 
        rcuId, 
        longitude, 
        latitude, 
        timestamp, 
        targetIdsLen: 0, 
        targetIds: null, 
        intersectionName, 
        isView: 0, 
        description 
      }
      
      // 添加到列表前端
      trafficEvents.value.unshift(newEvent)
      
      // 移除toast通知显示
    }
    
    function showToast(message, type = 'info', duration = 3000) {
      // 清除现有计时器
      if (toast.value.timer) {
        clearTimeout(toast.value.timer)
      }
      
      // 设置新的Toast
      toast.value.show = true
      toast.value.message = message
      toast.value.type = type
      
      // 设置定时器自动关闭
      toast.value.timer = setTimeout(() => {
        toast.value.show = false
      }, duration)
    }

    function switchTab(tab) {
      activeTab.value = tab
      if (tab === 'abnormal') {
        fetchAbnormalDevices(false) // 不显示通知
      } else if (tab === 'traffic') {
        fetchTrafficEvents(false) // 不显示通知
      }
    }

    function refreshCurrentTab() {
      if (activeTab.value === 'abnormal') {
        fetchAbnormalDevices(false) // 不显示通知
      } else if (activeTab.value === 'traffic') {
        fetchTrafficEvents(false) // 不显示通知
      }
    }
    
    // 获取异常设备数据
    function fetchAbnormalDevices(showNotification = false) {
      // 移除通知显示
      
      // 实际情况下应该调用API
      fetch('/menu/device/abnormal')
        .then(response => response.json())
        .then(data => {
          if (data.status === 'success') {
            abnormalDevices.value = data.abnormalDevices || []
            // 移除通知显示
          } else {
            // 移除通知显示
            // 模拟数据用于展示
            simulateAbnormalDevices()
          }
        })
        .catch(error => {
          console.error('获取异常设备数据出错:', error)
          // 移除通知显示
          // 模拟数据用于展示
          simulateAbnormalDevices()
        })
    }

    // 模拟异常设备数据（实际项目中应删除此函数）
    function simulateAbnormalDevices() {
      const deviceTypes = ['摄像头', '信号灯', '雷达', '路况检测器', '电子屏']
      const abnormalTypes = ['告警', '离线']
      const newDevices = []
      
      for (let i = 0; i < generateRandomValue(5, 12); i++) {
        const deviceType = deviceTypes[Math.floor(Math.random() * deviceTypes.length)]
        const abnormalType = abnormalTypes[Math.floor(Math.random() * abnormalTypes.length)]
        const latestTime = Date.now() - Math.floor(Math.random() * 3600000) // 最近1小时内
        const rcuId = `RCU-${Math.floor(Math.random() * 10000).toString().padStart(5, '0')}`
        
        newDevices.push({
          latestTime,
          rcuId,
          deviceType,
          abnormalType
        })
      }
      
      abnormalDevices.value = newDevices
    }
    
    // 获取交通事件
    function fetchTrafficEvents(showNotification = false) {
      // 移除通知显示
      
      // 根据API规范调用后端接口
      fetch('/menu/device/event')
        .then(response => response.json())
        .then(data => {
          if (data.status === 'success') {
            trafficEvents.value = data.events || []
            // 移除通知显示
          } else {
            // 移除通知显示
            // 模拟数据用于展示
            simulateTrafficEvents()
          }
        })
        .catch(error => {
          console.error('获取交通事件数据出错:', error)
          // 移除通知显示
          // 模拟数据用于展示
          simulateTrafficEvents()
        })
    }
    
    // 模拟交通事件数据（实际项目中应删除此函数）
    function simulateTrafficEvents() {
      const descriptions = ['慢行', '快行', '紧急制动', '车辆追尾', '道路拥堵', '交通管制']
      const locations = ['石坪桥路口2', '三峡广场路口1', '临江路口1', '高新区路口', '城东大桥', '西环路出口']
      const eventTypes = [1, 2, 3]
      const newEvents = []
      
      for (let i = 0; i < generateRandomValue(5, 12); i++) {
        const eventType = eventTypes[Math.floor(Math.random() * eventTypes.length)]
        const description = descriptions[Math.floor(Math.random() * descriptions.length)]
        const intersectionName = locations[Math.floor(Math.random() * locations.length)]
        const timestamp = (Date.now() - Math.floor(Math.random() * 3600000)).toString() // 最近1小时内
        const eventId = `E${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`
        const rcuId = `U-WZ${Math.floor(Math.random() * 10000).toString().padStart(4, '0')}`
        const longitude = (106 + Math.random()).toFixed(4)
        const latitude = (29 + Math.random()).toFixed(4)
        
        newEvents.push({ 
          eventId, 
          eventType, 
          rcuId, 
          longitude, 
          latitude, 
          timestamp, 
          targetIdsLen: 0, 
          targetIds: null, 
          intersectionName, 
          isView: 0, 
          description 
        })
      }
      
      // 按时间戳排序
      newEvents.sort((a, b) => Number(b.timestamp) - Number(a.timestamp))
      trafficEvents.value = newEvents
    }
    
    function generateRandomTime() {
      const now = new Date()
      const date = new Date(now.getTime() - Math.random() * 2 * 24 * 60 * 60 * 1000) // 随机2天内的日期
      return date.getTime().toString()
    }

    // 根据事件类型返回对应的级别样式类
    function getEventLevelClass(eventType) {
      switch (Number(eventType)) {
        case 1:
          return '轻微'
        case 2:
          return '中等'
        case 3:
          return '严重'
        default:
          return '轻微'
      }
    }
    
    // 根据事件类型返回对应的文字标签
    function getEventTypeLabel(eventType) {
      switch (Number(eventType)) {
        case 1:
          return '轻微'
        case 2:
          return '中等'
        case 3:
          return '严重'
        default:
          return '轻微'
      }
    }

    // 格式化时间戳为可读时间
    function formatTime(timestamp) {
      const date = new Date(Number(timestamp))
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      const second = String(date.getSeconds()).padStart(2, '0')
      
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    }

    // 计算最后响应时间到现在的时长
    function formatTimeAgo(timestamp) {
      const seconds = Math.floor((Date.now() - Number(timestamp)) / 1000)
      
      if (seconds < 60) {
        return `${seconds}秒前`
      } else if (seconds < 3600) {
        return `${Math.floor(seconds / 60)}分钟前`
      } else if (seconds < 86400) {
        return `${Math.floor(seconds / 3600)}小时前`
      } else {
        return `${Math.floor(seconds / 86400)}天前`
      }
    }
    
    return {
      activeTab,
      abnormalDevices,
      trafficEvents,
      toast,
      abnormalList,
      trafficList,
      switchTab,
      refreshCurrentTab,
      formatTime,
      formatTimeAgo,
      showToast,
      getEventLevelClass,
      getEventTypeLabel
    }
  }
}
</script>
  
<style scoped>
.bottom-right-panel {
  position: absolute;
  bottom: 0.5%;
  right: 0.5%;
  width: 24%;
  height: 15%;
  /* 修改背景颜色，去掉透明度，使其与拓扑图背景一致 */
  background-color: rgb(8, 24, 44);
  border: 5px solid rgba(0, 181, 255, 0.3);
  border-radius: 4px;
  box-shadow: 0 0 10px rgba(0, 181, 255, 0.1);
}


.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  margin-top: -5px;
  height:16%;
  padding: 3px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 181, 255, 0.2);
}

.panel-title {
  /* 将字体大小从16px调小到14px */
  font-size: 14px;
  color: #00ffff;
  font-weight: bold;
  white-space: nowrap;
}

.controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tab-container {
  display: flex;
  gap: 10px;
  margin-right: 1px;
  margin-top: 0px;
}

.tab {
  padding: 1px 5px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  border-radius: 3px;
  position: relative;
  display: flex;
  align-items: center;
  transition: all 0.3s;
}

.tab:hover {
  color: #ffffff;
  background-color: rgba(0, 255, 255, 0.1);
}

.tab.active {
  color: #ffffff;
  background-color: rgba(0, 255, 255, 0.2);
}

.badge {
  position: relative;
  top: -8px;
  right: -3px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 0 5px;
  font-size: 10px;
  min-width: 16px;
  height: 5%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-list {
  height: 90%;
  overflow-y: auto;
  scroll-behavior: smooth;
  padding: 0 5px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  padding: 8px 10px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
  background-color: rgba(4, 34, 53, 0.2);
  margin-bottom: 8px;
  border-radius: 4px;
  transition: all 0.3s;
  position: relative;
  animation: newItem 1s ease-in-out;
}

.alert-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 3px 0 0 3px;
}

.alert-告警, .alert-严重 {
  background-color: #ff4d4f;
}

.alert-离线 {
  background-color: #722ed1; /* 更改为紫色，与告警状态明显区分 */
}

.alert-中等 {
  background-color: #faad14;
}

.alert-轻微 {
  background-color: #52c41a;
}

@keyframes newItem {
  from {
    background-color: rgba(0, 255, 255, 0.15);
  }
  to {
    background-color: rgba(4, 34, 53, 0.2);
  }
}
  
.alert-item:hover {
  background-color: rgba(14, 89, 134, 0.2);
}
  
.alert-tag {
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 10px;
  white-space: nowrap;
  color: white;
}

.tag-告警, .tag-严重 {
  background-color: rgba(255, 77, 79, 0.8);
}

.tag-离线 {
  background-color: rgba(114, 46, 209, 0.8); /* 紫色对应离线状态 */
}

.tag-中等 {
  background-color: rgba(250, 173, 20, 0.8);
}

.tag-轻微 {
  background-color: rgba(82, 196, 26, 0.8);
}
  
.alert-time {
  color: #00ffff;
  margin-right: 8px;
  width: 40%;
  font-size: 11px;
}
  
.alert-content {
  flex: 1;
  font-size: 11px;
  margin-right: 8px;
}

.alert-title {
  font-weight: 500;
  margin-bottom: 2px;
  color: #ffffff;
}

.alert-location {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.7);
}

/* Toast通知样式 */
.toast-notification {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 16px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  z-index: 9999;
  min-width: 200px;
  justify-content: center;
}

.toast-info {
  background-color: rgba(0, 145, 234, 0.9);
  color: white;
}

.toast-success {
  background-color: rgba(0, 210, 91, 0.9);
  color: white;
}

.toast-warning {
  background-color: rgba(255, 165, 0, 0.9);
  color: white;
}

.toast-error {
  background-color: rgba(255, 77, 79, 0.9);
  color: white;
}

.toast-icon {
  margin-right: 10px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.toast-message {
  font-size: 14px;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: #00ffff;
  transition: transform 0.3s;
}

.refresh-icon:hover {
  transform: rotate(180deg);
  color: #ffffff;
}

.empty-alerts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-icon {
  font-size: 24px;
  margin-bottom: 10px;
}

.empty-text {
  font-size: 14px;
}

/* Scrollbar styling */
.alert-list::-webkit-scrollbar {
  width: 4px;
}

.alert-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
}

.alert-list::-webkit-scrollbar-thumb {
  background: rgba(0, 181, 255, 0.4);
  border-radius: 4px;
}

.alert-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 181, 255, 0.6);
}
</style>