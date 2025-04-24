<template>
  <div class="panel-box map-preview">
    <div class="chart-title">
      <div class="title-text">
        <i class="map-icon"></i>区域预览
      </div>
      <div class="title-controls">
        <span class="refresh-icon" @click="refreshMap" title="刷新地图">↻</span>
      </div>
    </div>
    <div class="panel-content">
      <div id="map-container" class="amap-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import axios from 'axios'

// 定义组件属性
const props = defineProps({
  currentCity: {
    type: Object,
    default: () => ({
      name: '重庆市',
      center: [106.32554720015287, 29.51776878783989]
    })
  }
})

// 高德地图实例
let map = null
// 用于存储地图上的标记点
const markers = ref([])
// 存储设备数据
const devices = ref([])
// 从URL中获取的rcuId
const rcuId = ref('')
// 信息窗口实例
let infoWindow = null

// 监听城市变化，更新地图中心点
watch(() => props.currentCity, (newCity) => {
  if (map && newCity && newCity.center) {
    console.log('MapPreview: 更新地图位置到', newCity.name, newCity.center)
    // 平滑移动地图到新的中心点
    map.setCenter(newCity.center)
  }
}, { deep: true })

// 初始化高德地图
const initMap = () => {
  console.log('开始初始化地图...')
  // 创建地图实例
  map = new AMap.Map('map-container', {
    viewMode: '2D', // 使用2D视图提高兼容性
    zoom: 11, // 缩放级别调低，视图更广阔
    center: props.currentCity.center, // 使用传入的城市中心点
    pitch: 0, // 俯仰角度为0度
    resizeEnable: true, // 自动调整大小
  })
  
  console.log('地图实例创建成功')

  // 创建信息窗口实例
  infoWindow = new AMap.InfoWindow({
    offset: new AMap.Pixel(0, -30),
    closeWhenClickMap: true,
    autoMove: true,  // 自动调整窗口到视野内
    isCustom: true,  // 使用自定义窗口样式
    content: '<div class="info-window-loading">加载中...</div>'
  })

  // 地图加载完成事件
  map.on('complete', () => {
    console.log('地图加载完成')
    
    // 加载控件插件
    AMap.plugin(['AMap.Scale', 'AMap.ToolBar'], function() {
      console.log('地图控件插件加载完成')
      
      // 添加比例尺控件
      map.addControl(new AMap.Scale());
      
      // 添加工具条控件
      map.addControl(new AMap.ToolBar());
      
      // 加载设备数据
      loadDeviceData()
    });
  })
}

// 点击刷新图标时重新加载地图
const refreshMap = () => {
  if (!map) return;
  console.log('刷新地图按钮被点击')
  // 清除现有标记
  clearMarkers()
  // 重新加载设备数据
  loadDeviceData()
  console.log('地图已刷新');
  // 设置适当的缩放级别
  setTimeout(() => {
    // 先设置缩放级别，再设置中心点
    map.setZoom(11);
    setTimeout(() => {
      map.setCenter(props.currentCity.center);
    }, 100);
  }, 100);
}

// 从URL中获取rcuId参数
const getRouteParams = () => {
  try {
    // 从URL查询参数中获取rcuId
    const urlParams = new URLSearchParams(window.location.search)
    const urlRcuId = urlParams.get('rcuId')
    
    if (urlRcuId) {
      rcuId.value = urlRcuId
      console.log('从URL获取rcuId:', rcuId.value)
      } else {
      console.warn('URL中未找到rcuId参数')
    }
  } catch (error) {
    console.error('获取URL参数出错:', error)
  }
}

// 加载设备数据
const loadDeviceData = async () => {
  try {
    console.log('开始请求设备数据...')
    
    // 从后端获取真实设备数据
    const response = await axios.get('/menu/device/list', {
      params: {
        pageNum: 1,
        pageSize: 1000  // 设置较大的页面大小以获取所有设备
      }
    })
    
    console.log('收到设备数据响应:', response)
    
    if (response.data && response.data.status === 'success') {
      devices.value = response.data.rows || []
      console.log('成功加载设备数据:', devices.value.length, '个设备')
      
      if (devices.value.length > 0) {
        console.log('设备数据示例:', devices.value[0])
        // 在地图上显示设备
        displayDevicesOnMap(devices.value)
      } else {
        console.warn('后端返回的设备列表为空')
      }
    } else {
      console.error('加载设备数据失败:', response.data)
      showErrorMessage('无法加载设备数据，请刷新重试')
    }
  } catch (error) {
    console.error('获取设备数据出错:', error)
    
    // 添加更详细的错误信息
    if (error.response) {
      // 服务器响应了，但状态码不在 2xx 范围内
      console.error('错误状态码:', error.response.status)
      console.error('错误数据:', error.response.data)
      showErrorMessage(`请求错误 (${error.response.status})`)
    } else if (error.request) {
      // 请求已发送但没有收到响应
      console.error('未收到响应:', error.request)
      showErrorMessage('服务器未响应，请检查网络连接')
    } else {
      // 设置请求时发生了错误
      console.error('请求错误:', error.message)
      showErrorMessage('请求错误: ' + error.message)
    }
  }
}

// 显示错误消息
const showErrorMessage = (message) => {
  if (!map) return
  
  // 在地图中心显示错误提示
  const errorInfo = document.createElement('div')
  errorInfo.className = 'map-error-message'
  errorInfo.innerHTML = `
    <div class="error-icon">!</div>
    <div class="error-text">${message}</div>
    <div class="error-hint">点击刷新按钮重试</div>
  `
  
  // 添加到地图容器
  const mapContainer = document.getElementById('map-container')
  if (mapContainer) {
    // 移除可能存在的旧错误消息
    const oldError = mapContainer.querySelector('.map-error-message')
    if (oldError) {
      mapContainer.removeChild(oldError)
    }
    
    mapContainer.appendChild(errorInfo)
  }
}

// 清除地图上的所有标记
const clearMarkers = () => {
  if (map && markers.value.length > 0) {
    // 移除所有标记
    map.remove(markers.value)
    markers.value = []
    console.log('地图标记点已清除')
  }
}

// 在地图上显示设备
const displayDevicesOnMap = (devices) => {
  console.log('开始在地图上显示设备标记点...')
  
  // 清除现有标记
  clearMarkers()
  
  // 检查地图实例
  if (!map) {
    console.error('地图实例不存在，无法添加标记点')
    return
  }
  
  // 目标设备的坐标，用于后续聚焦
  let targetDevicePosition = null
  let targetDevice = null
  let validDeviceCount = 0
  
  // 检查是否有rcuId参数
  if (rcuId.value) {
    console.log('正在查找rcuId对应的设备:', rcuId.value)
    // 先尝试直接找到目标设备
    targetDevice = devices.find(d => d.rcuId === rcuId.value)
    
    if (targetDevice && targetDevice.longitude && targetDevice.latitude) {
      targetDevicePosition = [parseFloat(targetDevice.longitude), parseFloat(targetDevice.latitude)]
      console.log('直接找到目标设备位置:', targetDevicePosition)
    } else {
      console.warn('未找到rcuId对应的设备或设备缺少坐标:', rcuId.value)
    }
  }
  
  // 为每个设备创建标记
  devices.forEach(device => {
    try {
      // 检查设备是否有有效的坐标
      if (!device.longitude || !device.latitude) {
        console.warn('设备缺少坐标信息:', device.rcuId)
        return
      }
      
      validDeviceCount++
      
      // 创建坐标数组
      const coordinates = [parseFloat(device.longitude), parseFloat(device.latitude)]
      
      // 如果这是目标设备，记录坐标（作为备份，以防之前没有找到）
      if (device.rcuId === rcuId.value && !targetDevicePosition) {
        targetDevicePosition = coordinates
        targetDevice = device
        console.log('在循环中找到目标设备位置:', targetDevicePosition)
      }
      
      // 根据设备类型选择不同的图标
      let icon
      switch (device.deviceType) {
        case '1': // 激光雷达
          icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png'
          break
        case '2': // 摄像头
          icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
          break
        case '3': // 毫米波雷达
          icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_y.png'
          break
        case '4': // 融合设备
          icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png'
          break
        default:
          icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
      }
      
      // 创建设备标记
      const marker = new AMap.Marker({
        position: coordinates,
        title: device.rcuId,
        icon: icon,
        offset: new AMap.Pixel(-12, -12),
        zIndex: device.rcuId === rcuId.value ? 100 : 90, // 目标设备的标记层级更高
        extData: {
          id: device.rcuId,
          deviceType: device.deviceType,
          receiveTime: device.receiveTime,
          coordinates: coordinates
        }
      })
      
      // 添加点击事件
      marker.on('click', () => {
        showDeviceInfo(device, marker)
      })
      
      // 添加到标记集合
      markers.value.push(marker)
    } catch (err) {
      console.error(`创建设备 ${device.rcuId} 的标记点时出错:`, err)
    }
  })
  
  console.log(`有效设备数量: ${validDeviceCount}, 创建的标记点数量: ${markers.value.length}`)
  
  // 将所有标记添加到地图
  if (markers.value.length > 0) {
    try {
      map.add(markers.value)
      console.log(`成功添加 ${markers.value.length} 个标记点到地图`)
    } catch (err) {
      console.error('添加标记到地图时出错:', err)
    }
  } else {
    console.warn('没有有效的标记点可以添加到地图')
  }
  
  // 如果找到了目标设备，将地图中心设置到该设备位置
  if (targetDevicePosition) {
    console.log('设置地图中心到目标设备位置:', targetDevicePosition)
    
    // 先确保地图完全加载
    setTimeout(() => {
      // 先设置缩放级别，再设置中心点
      map.setZoom(13) // 首先设置适当的缩放级别
      
      console.log('地图缩放级别已设置为:', map.getZoom())
      
      // 增加延迟，确保缩放级别设置完成后再设置中心点
      setTimeout(() => {
        map.setCenter(targetDevicePosition)
        
        // 确保中心点精确设置
        map.panTo(targetDevicePosition)
        
        console.log('地图中心已设置为:', map.getCenter())
        
        // 不再自动显示目标设备的信息窗口
        // 只在用户点击标记点时显示信息窗口
      }, 200)
    }, 300) // 延迟300毫秒以确保地图已准备好
  } else if (rcuId.value) {
    console.warn('找不到与rcuId匹配的设备坐标，无法设置地图中心:', rcuId.value)
  }
}

// 显示设备信息
const showDeviceInfo = (device, marker) => {
  // 将时间戳转换为可读时间
  let formattedTime = '未知'
  if (device.receiveTime) {
    const timestamp = parseInt(device.receiveTime)
    if (!isNaN(timestamp)) {
      const date = new Date(timestamp)
      formattedTime = date.toLocaleString()
    }
  }
  
  // 获取设备类型文本
  let deviceTypeText = '未知设备'
  let deviceTypeClass = 'device-unknown'
  let deviceName = '未知设备'
  
  switch (device.deviceType) {
    case '1':
      deviceTypeText = '激光雷达'
      deviceTypeClass = 'device-lidar'
      deviceName = '激光雷达设备'
      break
    case '2':
      deviceTypeText = '摄像头'
      deviceTypeClass = 'device-camera'
      deviceName = '监控摄像头'
      break
    case '3':
      deviceTypeText = '毫米波雷达'
      deviceTypeClass = 'device-radar'
      deviceName = '毫米波雷达设备'
      break
    case '4':
      deviceTypeText = '融合设备'
      deviceTypeClass = 'device-fusion'
      deviceName = '多功能融合设备'
      break
  }
  
  const content = `
    <div class="amap-custom-infowindow ${deviceTypeClass}">
      <div class="info-title">
        <span class="info-name">${deviceName}</span>
        <span class="info-close" onclick="document.querySelectorAll('.amap-info').forEach(e => e.style.display = 'none')">×</span>
      </div>
      <div class="info-content">
        <div class="info-item">
          <span class="info-label">设备编号:</span>
          <span class="info-value">${device.rcuId}</span>
        </div>
        <div class="info-item">
          <span class="info-label">设备类型:</span>
          <span class="info-value type-value">${deviceTypeText}</span>
        </div>
        <div class="info-item">
          <span class="info-label">更新时间:</span>
          <span class="info-value">${formattedTime}</span>
        </div>
        <div class="info-item">
          <span class="info-label">坐标:</span>
          <span class="info-value">[${parseFloat(device.longitude).toFixed(4)}, ${parseFloat(device.latitude).toFixed(4)}]</span>
        </div>
        <div class="info-actions">
          <a href="javascript:void(0)" class="info-btn view-btn full-width-btn">查看详细</a>
        </div>
      </div>
      <div class="info-arrow"></div>
    </div>
  `
  
  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
  console.log('信息窗口已打开，位置:', marker.getPosition())
  
  // 添加关闭按钮点击事件
  setTimeout(() => {
    const closeBtn = document.querySelector('.info-close')
    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        infoWindow.close()
      })
    }
    
    // 添加查看详情按钮事件
    const viewBtn = document.querySelector('.view-btn')
    if (viewBtn) {
      viewBtn.addEventListener('click', () => {
        // 跳转到详情页面并传递rcuId参数和model参数
        window.location.href = `/detail?rcuId=${device.rcuId}&model=3d`
      })
    }
  }, 10)
}

// 定义emit
const emit = defineEmits([])

// 生命周期钩子 - 挂载
onMounted(() => {
  // 获取URL中的rcuId参数
  getRouteParams()
  
  console.log('组件挂载，开始加载高德地图API...')
  // 加载高德地图
  const script = document.createElement('script')
  // 加载不同的插件，确保Scale和ToolBar可用
  script.src = 'https://webapi.amap.com/maps?v=2.0&key=e4abe1ae6a2474fdf2220157830b2b99&plugin=AMap.Geolocation,AMap.InfoWindow,AMap.Scale,AMap.ToolBar'
  script.async = true
  script.onload = () => {
    console.log('高德地图API加载完成')
    initMap()
    
    // 地图初始化后，也注册路由变化监听
    window.addEventListener('popstate', () => {
      console.log('URL变化，重新获取rcuId参数')
      getRouteParams()
      // 如果地图已加载，刷新地图
      if (map) {
        refreshMap()
      }
    })
  }
  script.onerror = (error) => {
    console.error('加载高德地图API失败:', error)
  }
  document.head.appendChild(script)
  
  // 添加全局样式
  const style = document.createElement('style')
  style.innerHTML = `
    .amap-custom-infowindow {
      width: 280px;
      background: rgba(6, 30, 70, 0.9);
      border-radius: 8px;
      box-shadow: 0 0 20px rgba(0, 180, 255, 0.5);
      border: 1px solid rgba(0, 180, 255, 0.5);
      color: #fff;
      padding: 0;
      position: relative;
      backdrop-filter: blur(5px);
      overflow: hidden;
    }
    
    .info-title {
      padding: 10px 15px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(0, 180, 255, 0.2);
      border-bottom: 1px solid rgba(0, 180, 255, 0.3);
    }
    
    .info-name {
      font-weight: bold;
      font-size: 16px;
      color: #00ffff;
      text-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
    }
    
    .info-close {
      cursor: pointer;
      font-size: 18px;
      width: 24px;
      height: 24px;
      line-height: 24px;
      text-align: center;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      transition: all 0.3s;
    }
    
    .info-close:hover {
      background: rgba(255, 255, 255, 0.3);
      transform: rotate(90deg);
    }
    
    .info-content {
      padding: 12px 15px;
    }
    
    .info-item {
      margin-bottom: 8px;
      display: flex;
      align-items: center;
    }
    
    .info-item:last-child {
      margin-bottom: 0;
    }
    
    .info-label {
      color: rgba(255, 255, 255, 0.7);
      width: 80px;
    }
    
    .info-value {
      color: #ffffff;
      font-weight: 500;
    }
    
    .type-value {
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
    }
    
    .device-lidar .type-value {
      background-color: rgba(255, 59, 48, 0.3);
      border: 1px solid rgba(255, 59, 48, 0.5);
      color: #ff3b30;
    }
    
    .device-camera .type-value {
      background-color: rgba(0, 122, 255, 0.3);
      border: 1px solid rgba(0, 122, 255, 0.5);
      color: #007aff;
    }
    
    .device-radar .type-value {
      background-color: rgba(255, 204, 0, 0.3);
      border: 1px solid rgba(255, 204, 0, 0.5);
      color: #ffcc00;
    }
    
    .device-fusion .type-value {
      background-color: rgba(52, 199, 89, 0.3);
      border: 1px solid rgba(52, 199, 89, 0.5);
      color: #34c759;
    }
    
    .device-unknown .type-value {
      background-color: rgba(142, 142, 147, 0.3);
      border: 1px solid rgba(142, 142, 147, 0.5);
      color: #8e8e93;
    }
    
    .info-arrow {
      position: absolute;
      bottom: -8px;
      left: 50%;
      transform: translateX(-50%);
      width: 0;
      height: 0;
      border-left: 8px solid transparent;
      border-right: 8px solid transparent;
      border-top: 8px solid rgba(0, 180, 255, 0.5);
    }
    
    .info-window-loading {
      padding: 10px 15px;
      color: #00ffff;
      text-align: center;
    }
    
    .info-actions {
      display: flex;
      justify-content: space-between;
      margin-top: 12px;
      padding-top: 10px;
      border-top: 1px dashed rgba(0, 180, 255, 0.3);
    }
    
    .info-btn {
      padding: 6px 10px;
      border-radius: 4px;
      color: #fff;
      text-decoration: none;
      font-size: 13px;
      text-align: center;
      transition: all 0.3s;
    }
    
    .full-width-btn {
      width: 100%;
      display: block;
    }
    
    .view-btn {
      background-color: rgba(0, 122, 255, 0.3);
      border: 1px solid rgba(0, 122, 255, 0.5);
    }
    
    .view-btn:hover {
      background-color: rgba(0, 122, 255, 0.5);
      box-shadow: 0 0 10px rgba(0, 122, 255, 0.5);
    }
    
    .analyze-btn {
      background-color: rgba(52, 199, 89, 0.3);
      border: 1px solid rgba(52, 199, 89, 0.5);
    }
    
    .analyze-btn:hover {
      background-color: rgba(52, 199, 89, 0.5);
      box-shadow: 0 0 10px rgba(52, 199, 89, 0.5);
    }
    
    /* 错误消息样式 */
    .map-error-message {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      background: rgba(6, 30, 70, 0.9);
      border-radius: 8px;
      box-shadow: 0 0 20px rgba(255, 59, 48, 0.5);
      border: 1px solid rgba(255, 59, 48, 0.5);
      color: #fff;
      padding: 20px;
      text-align: center;
      max-width: 80%;
      z-index: 100;
    }
    
    .error-icon {
      width: 40px;
      height: 40px;
      line-height: 40px;
      text-align: center;
      background-color: rgba(255, 59, 48, 0.3);
      border: 2px solid rgba(255, 59, 48, 0.7);
      border-radius: 50%;
      margin: 0 auto 10px;
      font-size: 24px;
      font-weight: bold;
      color: #ff3b30;
    }
    
    .error-text {
      font-size: 16px;
      margin-bottom: 8px;
      color: #ffffff;
    }
    
    .error-hint {
      font-size: 13px;
      color: rgba(255, 255, 255, 0.7);
    }
    
    /* 修改AMap信息窗口默认样式 */
    :deep(.amap-info-sharp) {
      display: none !important;
    }
    
    :deep(.amap-info-content) {
      padding: 0 !important;
      background: transparent !important;
      box-shadow: none !important;
      border: none !important;
    }
    
    :deep(.amap-info-close) {
      display: none !important;
    }
  `
  document.head.appendChild(style)
})

// 生命周期钩子 - 卸载前
onBeforeUnmount(() => {
  // 清理地图
  if (map) {
    clearMarkers()
    map.destroy()
  }
})
</script>

<style scoped>
.panel-box {
  background-color: rgba(2, 12, 32, 0.8);
  border: 1px solid #0e5986;
  box-shadow: 0 0 20px rgba(0, 145, 234, 0.4);
  border-radius: 8px;
  padding: 15px;
  backdrop-filter: blur(5px);
  transition: all 0.5s ease;
  position: relative;
  overflow: hidden;
  height: 350px;
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

.map-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 8px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>');
  background-size: contain;
  background-repeat: no-repeat;
}

.title-controls {
  display: flex;
  align-items: center;
  gap: 10px;
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
  height: calc(100% - 50px);
  position: relative;
}

.amap-container {
  height: 100%;
  width: 100%;
  border-radius: 8px;
  border: 1px solid rgba(14, 89, 134, 0.4);
  overflow: hidden;
  box-shadow: 0 0 15px rgba(0, 145, 234, 0.2) inset;
  transition: all 0.3s ease;
}

.amap-container:hover {
  box-shadow: 0 0 20px rgba(0, 180, 255, 0.3) inset;
  border-color: rgba(0, 255, 255, 0.5);
}

:deep(.amap-logo) {
  opacity: 0.6;
  transition: opacity 0.3s;
}

:deep(.amap-copyright) {
  opacity: 0.6;
  transition: opacity 0.3s;
}

:deep(.amap-toolbar) {
  background-color: rgba(2, 12, 32, 0.8) !important;
  border: 1px solid rgba(14, 89, 134, 0.6) !important;
  border-radius: 4px !important;
  box-shadow: 0 0 10px rgba(0, 145, 234, 0.3) !important;
}

:deep(.amap-toolbar .amap-toolbar-control) {
  color: #00ffff !important;
  background-color: transparent !important;
}

:deep(.amap-toolbar .amap-toolbar-control:hover) {
  color: #ffffff !important;
  background-color: rgba(14, 89, 134, 0.4) !important;
}

:deep(.amap-scale) {
  background-color: rgba(2, 12, 32, 0.6) !important;
  border-color: rgba(14, 89, 134, 0.6) !important;
  color: #00ffff !important;
  border-radius: 2px;
  padding: 0 5px;
}
</style> 