<template>
  <div class="detail-container">
    <!-- 顶部导航栏 -->
    <Header>
      <!-- 将城市选择器传递到Header组件的插槽中 -->
      <template #city-selector>
        <CitySelector @city-change="changeCity" :currentCity="currentCity" />
      </template>
    </Header>
    
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧区域 -->
      <div class="left-panel">
        <MapPreview :currentCity="currentCity" />
        <VehicleStreamMonitor ref="vehicleStreamMonitor" :rcu-id="rcuId" />
        <HistoryRecords @loadModel="loadModel" />
      </div>

      <!-- 中间地图区域 -->
      <div class="center-panel">
        <!-- 添加装饰角落元素 -->
        <div class="corner-decoration corner-top-left"></div>
        <div class="corner-decoration corner-bottom-right"></div>
        
        <div class="scene-container" ref="sceneContainer">
          <!-- 添加3D场景按钮 -->
          <div class="scene3d-button" @click="goToScene3D">
            <el-button type="primary" :icon="VideoPlay">查看3D实时场景</el-button>
          </div>
          
          <!-- 数据指标 -->
          <div class="data-indicators">
            <div class="indicator">车辆数量(辆) <span>{{ indicatorsData.vehicle_count  }}</span></div>
            <div class="indicator">行人流量(人) <span>{{ indicatorsData.person_count  }}</span></div>
            <div class="indicator">设备类型 <span>{{ indicatorsData.deviceType || '--' }}</span></div>
            <div class="indicator">设备状态 <span>{{ indicatorsData.status || '--' }}</span></div>
            <div class="indicator">设备最久更新时间 <span>{{ formatLastUpdateTime(indicatorsData.last_update_time) }}</span></div>
          </div>
          
          <!-- 3D模型动画控制面板 -->
          <div class="animation-controls" v-if="hasAnimation">
            <div class="animation-progress-container">
              <div class="progress-wrapper">
                <input 
                  type="range" 
                  class="progress-slider" 
                  min="0" 
                  max="100" 
                  step="0.1"
                  v-model="animationProgress"
                  @input="onAnimationSeek"
                  @mousedown="onSliderDragStart"
                  @mouseup="onSliderDragEnd"
                  :disabled="!hasAnimation"
                >
              </div>
              <div class="animation-info">{{ formatTime(currentAnimationTime) }}/{{ formatTime(totalAnimationDuration) }}</div>
            </div>
            <div class="animation-buttons">
              <button 
                class="animation-btn" 
                @click="togglePlayPause" 
                :disabled="!hasAnimation"
                :title="isAnimationPaused ? '播放' : '暂停'"
              >
                <el-icon v-if="isAnimationPaused"><VideoPlay /></el-icon>
                <el-icon v-else><VideoPause /></el-icon>
              </button>
              <button 
                class="animation-btn" 
                @click="restartAnimation" 
                :disabled="!hasAnimation"
                title="重新开始"
              >
                <el-icon><RefreshRight /></el-icon>
              </button>
            </div>
          </div>
          
          <!-- 相机控制按钮和视角切换组合在一起 -->
          <div class="camera-controls">
            <div class="control-group">
              <button @click="switchToPresetView('north')" class="control-btn preset-btn" title="北视角">
                <span>北</span>
              </button>
              <button @click="cameraMove('up')" class="control-btn" title="向上移动">
                <i class="el-icon-arrow-up"></i>
              </button>
              <button @click="switchToPresetView('south')" class="control-btn preset-btn" title="南视角">
                <span>南</span>
              </button>
            </div>
            <div class="control-group">
              <button @click="cameraMove('left')" class="control-btn" title="向左移动">
                <i class="el-icon-arrow-left"></i>
              </button>
              <button @click="cameraMove('reset')" class="control-btn reset" title="重置视角">
                <i class="el-icon-refresh"></i>
              </button>
              <button @click="cameraMove('right')" class="control-btn" title="向右移动">
                <i class="el-icon-arrow-right"></i>
              </button>
            </div>
            <div class="control-group">
              <button @click="switchToPresetView('east')" class="control-btn preset-btn" title="东视角">
                <span>东</span>
              </button>
              <button @click="cameraMove('down')" class="control-btn" title="向下移动">
                <i class="el-icon-arrow-down"></i>
              </button>
              <button @click="switchToPresetView('west')" class="control-btn preset-btn" title="西视角">
                <span>西</span>
              </button>
            </div>
            <div class="control-group zoom-group">
              <button @click="cameraZoom(true)" class="control-btn zoom-btn" title="放大">
                <i class="el-icon-plus"></i>
              </button>
              <button @click="cameraZoom(false)" class="control-btn zoom-btn" title="缩小">
                <i class="el-icon-minus"></i>
              </button>
            </div>
          </div>
          
          <!-- 视角信息显示 -->
          <div class="camera-info">
            <div class="info-label">相机位置:</div>
            <div class="info-value">X: {{ cameraPosition.x.toFixed(2) }}, Y: {{ cameraPosition.y.toFixed(2) }}, Z: {{ cameraPosition.z.toFixed(2) }}</div>
            <div class="info-label">视角方向:</div>
            <div class="info-label">俯仰: {{ cameraPitch.toFixed(2) }}°, 偏航: {{ cameraYaw.toFixed(2) }}°</div>
            <div class="info-label">缩放比例:</div>
            <div class="info-value">{{ zoomLevel.toFixed(2) }}x</div>
          </div>
        </div>
      </div>

      <!-- 右侧统计区域 -->
      <!-- <div class="right-panel">
        <div class="statistics">
          <div class="stat-header">总览统计</div>
          <div class="stat-content"></div>
        </div>
      </div> -->
    </div>
  </div>

  <!-- 导出选项弹窗 -->
  <div v-if="showExportOptions" class="modal-overlay" @click.self="showExportOptions = false">
    <div class="modal-content">
      <div class="modal-header">
        <h3>选择导出格式</h3>
        <div class="close-btn" @click="showExportOptions = false">&times;</div>
      </div>
      <div class="modal-body">
        <div class="export-options">
          <div class="export-option" @click="exportHistoryData('glb')">
            <div class="export-icon">
              <i class="fas fa-cube"></i>
            </div>
            <div class="export-info">
              <div class="export-title">3D模型文件(GLB)</div>
              <div class="export-desc">导出所选时间段内的3D模型文件，用于后续分析或展示</div>
            </div>
          </div>
          <div class="export-option" @click="exportHistoryData('excel')">
            <div class="export-icon">
              <i class="fas fa-file-excel"></i>
            </div>
            <div class="export-info">
              <div class="export-title">Excel数据报表</div>
              <div class="export-desc">导出所选时间段内的交通数据统计报表，含车流量、行人数量等</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, reactive, watch, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import axios from 'axios'
// 导入 Element Plus 图标
import { VideoPlay, VideoPause, RefreshRight } from '@element-plus/icons-vue'
import TrafficChart from '../components/charts/TrafficChart.vue'
import Header from '../components/Header.vue'
import CitySelector from '../components/CitySelector.vue'
import MapPreview from '../components/map/MapPreview.vue'
import VehicleStreamMonitor from '../components/charts/VehicleStreamMonitor.vue'
import HistoryRecords from '../components/history/HistoryRecords.vue'

// 响应式数据
const sceneContainer = ref(null)
const locationStatus = ref('定位中...')
const route = useRoute()
const rcuId = ref('U-WZ000R') // 添加默认RCU ID
const indicatorsData = ref({
  vehicle_count: '--',
  person_count: '--',
  deviceType: '--',
  status: '--',
  last_update_time: null
})

// 动画控制相关的响应式变量
const hasAnimation = ref(false)
const isAnimationPaused = ref(false)
const animationProgress = ref(0)
const currentAnimationTime = ref(0)
const totalAnimationDuration = ref(0)
const isDraggingSlider = ref(false)
const activeActions = ref([])

// 当前城市数据
const currentCity = ref({
  name: '重庆市',
  center: [106.32554720015287, 29.51776878783989]
})

// 相机相关的响应式数据
const cameraPosition = reactive({ x: 0, y: 0, z: 5 })
const cameraPitch = ref(0)
const cameraYaw = ref(0)
const zoomLevel = ref(1.0)
const initialCameraPosition = { x: 0, y: 0, z: 5 }
const initialTarget = { x: 0, y: 0, z: 0 }
let defaultCameraPosition = null
let defaultTarget = null
let initialDistance = null // 添加初始距离变量，用于计算缩放比例

let timeInterval
let weatherInterval

// Three.js 相关变量
let scene
let camera
let renderer
let controls
let model
let animationFrameId
let clock = new THREE.Clock()
let mixer
let animations = [] // 添加存储动画的数组

// 预设视角位置
const presetViews = {
  east: {
    position: { x: 0.52, y: 0.12, z: -0.04 },
    rotation: { pitch: -16.90, yaw: -92.76 },
    zoom: 10.00
  },
  south: {
    position: { x: -0.01, y: 0.20, z: 0.54 },
    rotation: { pitch: -29.04, yaw: 179.46 },
    zoom: 4.15
  },
  west: {
    position: { x: -0.57, y: 0.09, z: 0.01 },
    rotation: { pitch: -12.50, yaw: 81.42 },
    zoom: 5.49
  },
  north: {
    position: { x: -0.06, y: 0.19, z: -0.59 },
    rotation: { pitch: -9.85, yaw: 11.76 },
    zoom: 10.00
  },
  // 使用用户提供的示例参数创建第五个预设视角
  custom: {
    position: { x: 0.31, y: 0.93, z: -0.33 },
    rotation: { pitch: -82.50, yaw: -64.20 },
    zoom: 3.99
  }
}

// 获取URL参数中的rcuId
const getRouteParams = () => {
  // 从URL查询参数中获取rcuId
  const urlParams = new URLSearchParams(window.location.search)
  const urlRcuId = urlParams.get('rcuId')
  
  if (urlRcuId) {
    rcuId.value = urlRcuId
    console.log('从URL获取rcuId:', rcuId.value)
  } else {
    console.warn('URL中未找到rcuId参数，使用默认值')
  }
}

// 从后端获取数据指标
const fetchIndicatorsData = async () => {
  if (!rcuId.value) {
    console.warn('rcuId为空，无法获取数据指标')
    return
  }
  
  try {
    const response = await axios.get('/menu/device/rcuInfo', {
      params: {
        rcuId: rcuId.value
      }
    })
    
    console.log('获取数据指标响应:', response.data)
    
    if (response.data.status === 'success') {
      // 更新指标数据
      indicatorsData.value = response.data.rcuInfo
    } else {
      console.error('获取数据指标失败:', response.data)
    }
  } catch (error) {
    console.error('获取数据指标出错:', error)
  }
}

// 格式化最后更新时间
const formatLastUpdateTime = (timestamp) => {
  if (!timestamp) return '--'
  
  try {
    // 确保timestamp是数字类型
    const timeMs = typeof timestamp === 'string' ? parseInt(timestamp) : timestamp
    
    // 检查是否为有效数字
    if (isNaN(timeMs)) return '--'
    
    const date = new Date(timeMs)
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) return '--'
    
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    console.error('格式化时间戳出错:', e, '原始值:', timestamp)
    return '--'
  }
}

// 格式化时间显示 (秒数转为 mm:ss 格式)
const formatTime = (seconds) => {
  if (isNaN(seconds) || seconds === undefined) return '00:00'
  
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 初始化3D场景
const init3DScene = () => {
  // 创建场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x16181a)

  // 创建相机
  camera = new THREE.PerspectiveCamera(
    75,
    sceneContainer.value.clientWidth / sceneContainer.value.clientHeight,
    0.1,
    1000
  )
  // 设置初始相机位置
  camera.position.set(-0.26, 0.24, 0.56)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ 
    antialias: true,
    alpha: true
  })
  renderer.setSize(
    sceneContainer.value.clientWidth,
    sceneContainer.value.clientHeight
  )
  renderer.shadowMap.enabled = true
  renderer.outputColorSpace = THREE.SRGBColorSpace
  
  sceneContainer.value.appendChild(renderer.domElement)

  // 添加控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.maxDistance = 100
  controls.minDistance = 0.1  // 调整最小距离，允许更靠近模型
  
  // 添加控制器事件监听，以更新相机信息
  controls.addEventListener('change', updateCameraInfo)

  // 优化光照设置
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 1)
  directionalLight.position.set(10, 20, 10)
  directionalLight.castShadow = true
  scene.add(directionalLight)

  // 加载环境贴图
  const envMap = new THREE.CubeTextureLoader()
    .setPath('/env/')
    .load([
      'px.jpg', 'nx.jpg',
      'py.jpg', 'ny.jpg',
      'pz.jpg', 'nz.jpg',
    ])
  scene.environment = envMap

  // 添加窗口大小改变监听
  window.addEventListener('resize', onWindowResize)
}

// 加载模型函数
const loadModel = (markerTitle = null, modelFilePath = null) => {
  // 移除当前模型
  if (model) {
    scene.remove(model)
  }
  
  // 清除之前的动画
  if (mixer) {
    mixer.stopAllAction()
    mixer.uncacheRoot(model)
  }
  animations = []
  activeActions.value = []
  hasAnimation.value = false
  isAnimationPaused.value = false
  animationProgress.value = 0
  currentAnimationTime.value = 0
  totalAnimationDuration.value = 0
  
  // 加载器
  const loader = new GLTFLoader()
  
  // 使用提供的文件路径，如果没有则使用默认路径
  const modelPath = modelFilePath || '/car_animation.glb';
  

  // 加载模型
  loader.load(
    modelPath,
    (gltf) => {
      console.log('模型加载成功', markerTitle ? `(${markerTitle})` : '')
      
      // 优化材质处理
      gltf.scene.traverse((child) => {
        if (child.isMesh) {
          if (child.material) {
            const materials = Array.isArray(child.material) ? child.material : [child.material]
            materials.forEach((mat) => {
              mat.side = THREE.DoubleSide
              mat.transparent = true
              mat.opacity = 1.0
              mat.needsUpdate = true
            })
          }
        }
      })
      
      // 保存动画引用
      if (gltf.animations && gltf.animations.length > 0) {
        animations = gltf.animations
        hasAnimation.value = true
        console.log('模型包含动画数据：', animations.length, '个动画片段')
        
        // 找出最长的动画时长作为总时长
        totalAnimationDuration.value = animations.reduce((maxDuration, clip) => {
          return Math.max(maxDuration, clip.duration)
        }, 0)
        
        // 打印所有动画名称
        console.log('所有动画名称：')
        animations.forEach((clip, index) => {
          console.log(`${index + 1}. ${clip.name}`)
        })
      } else {
        console.warn('模型没有动画数据')
        hasAnimation.value = false
      }
      
      // 调整模型缩放
      gltf.scene.scale.setScalar(0.01)
      
      // 保存模型引用
      model = gltf.scene
      
      // 添加到场景
      scene.add(model)
      
      // 创建新的动画混合器
      mixer = new THREE.AnimationMixer(model)
      
      // 播放所有动画
      if (animations.length > 0) {
        const actionsArray = []
        animations.forEach((clip) => {
          const action = mixer.clipAction(clip)
          action.setLoop(THREE.LoopRepeat)
          action.clampWhenFinished = true
          action.timeScale = 1.0
          action.reset().play()
          actionsArray.push(action)
          console.log(`开始播放动画: ${clip.name}, 时长: ${clip.duration}秒`)
        })
        activeActions.value = actionsArray
      }
      
      // 自动调整相机位置以适应模型
      const box = new THREE.Box3().setFromObject(model)
      const center = box.getCenter(new THREE.Vector3())
      const size = box.getSize(new THREE.Vector3())
      
      // 使用用户指定的相机位置、视角和缩放比例
      camera.position.set(-0.26, 0.24, 0.56)
      
      // 计算相机朝向
      const direction = new THREE.Vector3()
      const pitchRad = -17.79 * Math.PI / 180 // 转换为弧度
      const yawRad = 151.54 * Math.PI / 180 // 转换为弧度
      
      // 根据俯仰角和偏航角计算方向向量
      direction.x = Math.sin(yawRad) * Math.cos(pitchRad)
      direction.y = Math.sin(pitchRad)
      direction.z = Math.cos(yawRad) * Math.cos(pitchRad)
      
      // 计算目标点
      const targetPoint = new THREE.Vector3().copy(camera.position).add(direction)
      
      // 保存默认相机位置和目标点，用于重置
      defaultCameraPosition = camera.position.clone()
      defaultTarget = targetPoint.clone()
      
      // 设置相机朝向目标点
      camera.lookAt(targetPoint)
      
      // 更新控制器
      controls.target.copy(targetPoint)
      controls.update()
      
      // 保存初始距离，用于计算缩放比例
      initialDistance = camera.position.distanceTo(targetPoint) * 3.99 // 使缩放比例为 3.99x
      
      // 初始化相机信息
      updateCameraInfo()
      
      // 开始动画循环
      animate()
    },
    (xhr) => {
      // 加载进度
      const percent = (xhr.loaded / xhr.total * 100).toFixed(2)
      console.log(`加载进度: ${percent}%`)
    },
    (error) => {
      console.error('加载GLB模型时出错:', error)
    }
  )
}

// 添加onMounted钩子初始化场景和加载模型
onMounted(() => {
  // 获取URL参数
  getRouteParams()
  
  // 获取数据指标
  fetchIndicatorsData()
  
  // 初始化3D场景
  init3DScene();
  
  // 加载默认模型
  loadModel();
  
  console.log('3D场景和模型已初始化');
});

// 添加卸载前的清理代码
onBeforeUnmount(() => {
  // 停止动画循环
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId);
  }
  
  // 停止动画混合器
  if (mixer) {
    mixer.stopAllAction();
  }
  
  // 清理场景中的对象
  if (scene) {
    disposeScene(scene);
  }
  
  // 移除事件监听器
  window.removeEventListener('resize', onWindowResize);
  
  console.log('3D场景资源已清理');
});

// 动画循环
const animate = () => {
  // 保存动画帧ID以便清理
  animationFrameId = requestAnimationFrame(animate)
  
  // 更新控制器
  if (controls) {
    controls.update()
  }
  
  // 更新动画混合器
  if (mixer && !isDraggingSlider.value) {
    const delta = clock.getDelta()
    mixer.update(delta)
    
    // 更新当前动画时间和进度
    if (hasAnimation.value && activeActions.value.length > 0) {
      // 使用第一个动作的时间作为当前时间
      const action = activeActions.value[0]
      if (action) {
        currentAnimationTime.value = action.time
        if (totalAnimationDuration.value > 0) {
          animationProgress.value = (currentAnimationTime.value / totalAnimationDuration.value) * 100
        }
      }
    }
    
    // 检查动画状态 (仅在开发时打印)
    if (animationFrameId % 100 === 0 && animations.length > 0) {
      animations.forEach((clip, index) => {
        const action = mixer.existingAction(clip)
        if (action) {
          console.log(`动画 ${index + 1}: ${clip.name}`, {
            playing: !action.paused,
            time: action.time.toFixed(2),
            duration: clip.duration.toFixed(2),
            progress: ((action.time / clip.duration) * 100).toFixed(0) + '%'
          })
        }
      })
    }
  }
  
  // 定期更新相机信息(不用每帧都更新，减少性能开销)
  if (animationFrameId % 30 === 0) {
    updateCameraInfo()
  }
  
  try {
    // 渲染场景
    if (renderer && scene && camera) {
      renderer.render(scene, camera)
    }
  } catch (error) {
    console.error('渲染时出错:', error)
    cancelAnimationFrame(animationFrameId)
  }
}

// 窗口大小改变事件处理
const onWindowResize = () => {
  if (camera && renderer && sceneContainer.value) {
    camera.aspect = sceneContainer.value.clientWidth / sceneContainer.value.clientHeight
    camera.updateProjectionMatrix()
    renderer.setSize(
      sceneContainer.value.clientWidth,
      sceneContainer.value.clientHeight
    )
  }
}

// 处理重新加载模型的事件函数
const handleReloadModel = (event) => {
  const { title, lat, lng, data } = event.detail;
  console.log('重新加载模型:', title, '坐标:', lat, lng, '数据:', data);
  
  // 更新指标数据
  if (data) {
    updateIndicators(data);
  }
  
  // 重新加载3D模型
  loadModel(title);
  
  // 移动地图到坐标点位置
  moveToMarker(lng, lat);
};

// 更新指标数据
const updateIndicators = (data) => {
  // 如果存在DOM元素，则更新内容
  const indicators = document.querySelectorAll('.indicator');
  if (indicators.length >= 5 && data) {
    // 车辆数量
    if (indicators[0]) {
      const spanElement = indicators[0].querySelector('span');
      if (spanElement) spanElement.textContent = data.vehicleCount.toLocaleString();
    }
    
    // 行人数量
    if (indicators[1]) {
      const spanElement = indicators[1].querySelector('span');
      if (spanElement) spanElement.textContent = data.pedestrianCount.toLocaleString();
    }
    
    // 红绿灯数量
    if (indicators[2]) {
      const spanElement = indicators[2].querySelector('span');
      if (spanElement) spanElement.textContent = data.trafficLights;
    }
    
    // 十字路口数量保持不变
    
    // 拥堵指数
    if (indicators[4]) {
      const spanElement = indicators[4].querySelector('span');
      if (spanElement) {
        spanElement.textContent = data.congestionIndex.toFixed(1);
        // 根据拥堵指数设置不同的颜色
        if (data.congestionIndex > 3.5) {
          spanElement.style.color = '#ff4d4f'; // 红色
        } else if (data.congestionIndex > 2.5) {
          spanElement.style.color = '#faad14'; // 黄色
        } else {
          spanElement.style.color = '#52c41a'; // 绿色
        }
      }
    }
  }
};

// 清理场景中的对象
const disposeScene = (scene) => {
  scene.traverse((object) => {
    if (object.geometry) {
      object.geometry.dispose()
    }
    
    if (object.material) {
      if (Array.isArray(object.material)) {
        object.material.forEach(material => material.dispose())
      } else {
        object.material.dispose()
      }
    }
  })
}

const router = useRouter()

const goToPage = (path) => {
  router.push(path)
}

// 相机移动函数
const cameraMove = (direction) => {
  if (!camera || !controls) return
  
  const moveDistance = 0.2 // 减小移动距离，原值为0.5
  const currentDirection = new THREE.Vector3()
  
  switch (direction) {
    case 'up':
      // 向上移动(Y轴正方向)
      camera.position.y += moveDistance
      controls.target.y += moveDistance
      break
    case 'down':
      // 向下移动(Y轴负方向)
      camera.position.y -= moveDistance
      controls.target.y -= moveDistance
      break
    case 'left':
      // 向左移动(X轴负方向)
      camera.getWorldDirection(currentDirection)
      const leftVector = new THREE.Vector3().crossVectors(
        currentDirection,
        camera.up
      ).normalize().multiplyScalar(moveDistance)
      camera.position.sub(leftVector)
      controls.target.sub(leftVector)
      break
    case 'right':
      // 向右移动(X轴正方向)
      camera.getWorldDirection(currentDirection)
      const rightVector = new THREE.Vector3().crossVectors(
        currentDirection,
        camera.up
      ).normalize().multiplyScalar(moveDistance)
      camera.position.add(rightVector)
      controls.target.add(rightVector)
      break
    case 'reset':
      // 重置相机位置
      if (defaultCameraPosition && defaultTarget) {
        camera.position.copy(defaultCameraPosition)
        controls.target.copy(defaultTarget)
      }
      break
  }
  
  // 更新控制器
  controls.update()
  
  // 更新相机位置信息
  updateCameraInfo()
}

// 相机缩放函数
const cameraZoom = (isZoomIn) => {
  if (!camera || !controls) return
  
  const zoomFactor = 0.05 // 减小缩放因子，原值为0.1
  const direction = new THREE.Vector3()
  camera.getWorldDirection(direction)
  
  if (isZoomIn) {
    // 放大 - 相机向前移动，增加移动距离
    camera.position.addScaledVector(direction, zoomFactor * 1.5)
  } else {
    // 缩小 - 相机向后移动
    camera.position.addScaledVector(direction, -zoomFactor)
  }
  
  // 更新控制器
  controls.update()
  
  // 更新相机位置信息，缩放比例会在updateCameraInfo中计算
  updateCameraInfo()
}

// 更新相机信息
const updateCameraInfo = () => {
  if (!camera || !controls || !controls.target) return
  
  // 更新位置信息
  cameraPosition.x = camera.position.x
  cameraPosition.y = camera.position.y
  cameraPosition.z = camera.position.z
  
  // 计算俯仰角和偏航角
  const direction = new THREE.Vector3()
  camera.getWorldDirection(direction)
  
  // 计算俯仰角 (X-Z平面上的倾斜角)
  cameraPitch.value = Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * (180 / Math.PI)
  
  // 计算偏航角 (水平面上的旋转角)
  cameraYaw.value = Math.atan2(direction.x, direction.z) * (180 / Math.PI)

  // 计算当前相机到目标点的距离
  const currentDistance = camera.position.distanceTo(controls.target)
  
  // 如果有初始距离，则计算缩放比例
  if (initialDistance !== null) {
    // 这里使用倒数关系，因为距离越小，缩放比例越大
    zoomLevel.value = initialDistance / currentDistance
    
    // 限制缩放比例在合理范围内，防止极端值
    zoomLevel.value = Math.max(0.1, Math.min(zoomLevel.value, 10.0))
  }
}

// 在script setup部分添加处理地图数据更新的方法
const handleMapDataUpdate = (data) => {
  // 更新指标数据
  if (data) {
    updateIndicators(data.data);
  }
  
  // 重新加载3D模型
  loadModel(data.title);
  
  // 移动地图到坐标点位置
  moveToMarker(data.lng, data.lat);
}

// 设置动画播放进度
const onAnimationSeek = () => {
  if (!hasAnimation.value || !mixer || activeActions.value.length === 0) return
  
  const targetTime = (parseFloat(animationProgress.value) / 100) * totalAnimationDuration.value
  
  // 设置所有动画的时间
  activeActions.value.forEach(action => {
    action.time = targetTime
  })
  
  // 更新当前时间
  currentAnimationTime.value = targetTime
  
  // 立即更新一帧以显示进度
  mixer.update(0)
}

// 拖动进度条开始
const onSliderDragStart = () => {
  isDraggingSlider.value = true
  if (hasAnimation.value && !isAnimationPaused.value) {
    // 暂时暂停，但不更改状态标志
    activeActions.value.forEach(action => {
      action.paused = true
    })
  }
}

// 拖动进度条结束
const onSliderDragEnd = () => {
  isDraggingSlider.value = false
  if (hasAnimation.value && !isAnimationPaused.value) {
    // 恢复播放
    activeActions.value.forEach(action => {
      action.paused = false
    })
  }
}

// 切换播放/暂停
const togglePlayPause = () => {
  if (!hasAnimation.value || activeActions.value.length === 0) return
  
  isAnimationPaused.value = !isAnimationPaused.value
  
  activeActions.value.forEach(action => {
    action.paused = isAnimationPaused.value
  })
}

// 重新开始动画
const restartAnimation = () => {
  if (!hasAnimation.value || activeActions.value.length === 0) return
  
  // 设置时间为0并更新进度条
  activeActions.value.forEach(action => {
    action.time = 0
    action.paused = false
  })
  
  // 重置动画状态
  isAnimationPaused.value = false
  currentAnimationTime.value = 0
  animationProgress.value = 0
  
  // 立即更新一帧以显示进度
  if (mixer) mixer.update(0)
}

// 切换到预设视角
const switchToPresetView = (viewName) => {
  if (!camera || !controls || !presetViews[viewName]) return
  
  const preset = presetViews[viewName]
  
  // 设置相机位置
  camera.position.set(preset.position.x, preset.position.y, preset.position.z)
  
  // 计算目标点（根据俯仰角和偏航角）
  const pitchRad = preset.rotation.pitch * Math.PI / 180 // 转换为弧度
  const yawRad = preset.rotation.yaw * Math.PI / 180 // 转换为弧度
  
  // 计算方向向量
  const direction = new THREE.Vector3()
  direction.x = Math.sin(yawRad) * Math.cos(pitchRad)
  direction.y = Math.sin(pitchRad)
  direction.z = Math.cos(yawRad) * Math.cos(pitchRad)
  
  // 计算目标点
  const targetPoint = new THREE.Vector3().copy(camera.position).add(direction)
  
  // 设置相机朝向目标点
  camera.lookAt(targetPoint)
  
  // 更新控制器目标
  controls.target.copy(targetPoint)
  controls.update()
  
  // 更新相机信息显示
  updateCameraInfo()
  
  console.log(`已切换到${viewName}视角`)
}

// 处理城市选择变更
const changeCity = (city) => {
  currentCity.value = city
  console.log('选择城市:', city.name, '坐标:', city.center)
  
  // 如果 3D 场景已初始化，也更新它的视角位置
  if (scene && camera && controls) {
    try {
      // 设置相机位置到一个合适的观察点，向下俯视城市
      const cityLng = city.center[0]
      const cityLat = city.center[1]
      
      // 简单的示例调整，实际项目中可能需要更复杂的计算
      camera.position.set(cityLng * 0.01, 0.5, cityLat * 0.01)
      
      // 让相机看向城市中心点
      const targetPoint = new THREE.Vector3(cityLng * 0.01, 0, cityLat * 0.01)
      camera.lookAt(targetPoint)
      
      // 更新相机控制器
      if (controls) {
        controls.target.copy(targetPoint)
        controls.update()
      }
      
      // 更新相机信息显示
      updateCameraInfo()
      
      console.log('已更新3D场景相机位置到城市:', city.name)
    } catch (error) {
      console.error('更新3D场景相机位置失败:', error)
    }
  }
  
  // MapPreview组件会通过props自动接收currentCity的变化并更新地图
  // 地图组件使用了watch监听currentCity变化，所以不需要在这里手动触发更新
  console.log('地图组件将自动更新到:', city.name)
}

// 可用的模型数据
const availableModels = [
  { time: '2023-05-01T08:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T09:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T10:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T11:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T12:00:00', file: '/car_animation.glb' }
]

// 跳转到3D场景页面
const goToScene3D = () => {
  window.location.href = '/scene3d';
};
</script>

<style scoped>
/* 添加全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.detail-container {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 12, 30, 0.95);
  color: #e1f2ff;
}

.main-content {
  display: flex;
  flex: 1;
  width: 100%;
  height: calc(100% - 60px); /* 减去 Header 的高度 */
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.left-panel {
  width: 25%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.center-panel {
  width: 75%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 8px;
  padding: 15px;
  margin: 10px;
  background: linear-gradient(135deg, rgba(2, 12, 32, 0.8) 0%, rgba(4, 28, 50, 0.85) 100%);
  box-shadow: 0 0 25px rgba(0, 145, 234, 0.25), inset 0 0 15px rgba(0, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

/* 添加发光边缘效果 */
.center-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    transparent, 
    rgba(0, 180, 255, 0.7), 
    rgba(0, 255, 255, 0.8), 
    rgba(0, 180, 255, 0.7), 
    transparent
  );
  z-index: 1;
}

/* 添加右侧发光边缘 */
.center-panel::after {
  content: '';
  position: absolute;
  top: 30%;
  bottom: 30%;
  right: 0;
  width: 1px;
  background: linear-gradient(180deg, 
    transparent, 
    rgba(0, 255, 255, 0.6), 
    transparent
  );
}

/* 添加左上角和右下角装饰 */
.center-panel .corner-decoration {
  position: absolute;
  width: 20px;
  height: 20px;
  pointer-events: none;
}

.center-panel .corner-top-left {
  top: 0;
  left: 0;
  border-top: 2px solid rgba(0, 180, 255, 0.8);
  border-left: 2px solid rgba(0, 180, 255, 0.8);
  border-top-left-radius: 6px;
}

.center-panel .corner-bottom-right {
  bottom: 0;
  right: 0;
  border-bottom: 2px solid rgba(0, 180, 255, 0.8);
  border-right: 2px solid rgba(0, 180, 255, 0.8);
  border-bottom-right-radius: 6px;
}

.scene-container {
  position: relative;
  width: 100%;
  height: 100%;
  background-color: rgba(2, 12, 32, 0.4);
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(14, 89, 134, 0.4);
  box-shadow: inset 0 0 20px rgba(0, 145, 234, 0.15);
  transition: all 0.3s ease;
}

.scene-container:hover {
  box-shadow: inset 0 0 30px rgba(0, 180, 255, 0.2);
  border-color: rgba(0, 255, 255, 0.4);
}

.right-panel {
  width: 25%;
}

.map-preview, .traffic-monitor, .scene-container, .statistics {
  background: rgba(22, 24, 26, 0.81);
  border-radius: 8px;
  padding: 15px;
}

.stat-item {
  margin-bottom: 15px;
  padding: 10px 15px;
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.8) 0%, rgba(4, 47, 79, 0.9) 100%);
  border: 1px solid #0e5986;
  border-radius: 5px;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 0 15px rgba(0, 119, 255, 0.15);
  overflow: hidden;
}

.stat-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #00b4ff 0%, #0077ff 100%);
}

.stat-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10%;
  width: 80%;
  height: 1px;
  background: rgba(0, 180, 255, 0.3);
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(0, 119, 255, 0.25);
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  letter-spacing: 1px;
}

.panel-content {
  height: calc(100% - 40px);
  overflow: hidden;
}

.amap-container {
  height: 100%;
  width: 100%;
  border-radius: 4px;
  border: 1px solid rgba(14, 89, 134, 0.4);
}

.chart-container {
  height: calc(100% - 40px);
  width: 100%;
  border-radius: 4px;
  border: 1px solid rgba(14, 89, 134, 0.4);
  background-color: rgba(0, 145, 234, 0.05);
}

.data-indicators {
  display: flex;
  justify-content: space-between;
  gap: 10px; /* 减小间距 */
  margin-bottom: 15px;
  position: absolute;
  top: 10px; /* 位置靠上一点 */
  left: 15px;
  right: 15px;
  z-index: 10;
}

.indicator {
  padding: 8px 12px; /* 减小内边距 */
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.7) 0%, rgba(4, 47, 79, 0.8) 100%); /* 稍微透明一点 */
  border: 1px solid rgba(14, 89, 134, 0.5); /* 边框更细、更透明 */
  border-radius: 4px; /* 减小圆角 */
  font-size: 12px; /* 减小字体 */
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  flex: 1;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 0 8px rgba(0, 119, 255, 0.1); /* 减小阴影 */
  overflow: hidden;
  backdrop-filter: blur(4px); /* 添加毛玻璃效果 */
}

.indicator::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px; /* 顶部线更细 */
  background: linear-gradient(90deg, rgba(0, 180, 255, 0.5), #0077ff, rgba(0, 180, 255, 0.5));
}

.indicator::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  width: 60%; /* 减小底部线宽度 */
  height: 1px;
  background: rgba(0, 180, 255, 0.2);
}

.indicator:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 119, 255, 0.2);
  border-color: rgba(0, 255, 255, 0.4);
}

.indicator span {
  display: block;
  font-size: 16px; /* 减小数值字体 */
  font-weight: 600;
  color: #00ffff;
  margin-top: 5px; /* 减小上边距 */
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.4);
  letter-spacing: 1px;
}

/* 响应式布局 */
@media screen and (max-width: 1600px) {
  .data-indicators {
    flex-wrap: wrap;
  }
  
  .indicator {
    font-size: 12px;
    padding: 6px 10px; /* 在小屏幕上进一步减小内边距 */
  }
  
  .indicator span {
    font-size: 14px; /* 小屏幕上减小数值字体 */
    margin-top: 3px;
  }
}

@media screen and (max-width: 1200px) {
  .left-panel, .right-panel {
    width: 30%;
  }
  
  .center-panel {
    width: 40%;
  }
}

.border-decoration {
  height: 12px;  /* 增加高度 */
  position: relative;
  width: 100%;
}

.border-decoration .line-short {
  position: absolute;
  width: 20px;
  height: 2px;
  background: #1e90ff;
}

.border-decoration .line-short.left {
  left: 20px;
}

.border-decoration .line-short.right {
  right: 20px;
}

.border-decoration .line-long {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 3px;
  background: #1e90ff;
}

.border-decoration.top .line-short {
  top: 4px;  /* 调整顶部短线位置 */
}

.border-decoration.top .line-long {
  top: 4px;  /* 调整顶部长线位置 */
}

.border-decoration.bottom .line-short {
  bottom: 4px;  /* 调整底部短线位置 */
}

.border-decoration.bottom .line-long {
  bottom: 6px;  /* 调整底部长线位置 */
}

.center-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
}

.arrow-group {
  display: flex;
  gap: 2px;
}

/* 添加模型切换按钮样式 */
.model-switcher {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 1000;
}

.model-switcher button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: rgba(30, 144, 255, 0.2);
  color: #1e90ff;
  cursor: pointer;
  transition: all 0.3s ease;
}

.model-switcher button:hover {
  background-color: rgba(30, 144, 255, 0.3);
}

.model-switcher button.active {
  background-color: rgba(30, 144, 255, 0.7);
  color: white;
}

.scene-container canvas {
  width: 100% !important;
  height: 100% !important;
  border-radius: 4px;
}

.title-box, .nav-box {
  display: none;
}

.title-content, .nav-content {
  display: none;
}

.nav-btn.active {
  display: none;
}

.header-container.center-container, .header-container.right-container {
  display: none;
}

.nav-btn.compact, .nav-btn.compact:hover, .nav-btn.compact.active {
  display: none;
}

.location-status {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 8px;
  background: rgba(31, 198, 255, 0.2);
  border: 1px solid #1FC6FF;
  border-radius: 4px;
  font-size: 12px;
  color: #1FC6FF;
  z-index: 100;
}

.location-status.error {
  background: rgba(255, 77, 79, 0.2);
  border-color: #ff4d4f;
  color: #ff4d4f;
}

/* 相机控制按钮样式 */
.camera-controls {
  position: absolute;
  left: 20px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  z-index: 10;
  background: rgba(0, 20, 40, 0.6);
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(0, 180, 255, 0.3);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.2);
  backdrop-filter: blur(5px);
}

.control-group {
  display: flex;
  justify-content: center;
  gap: 5px;
}

.zoom-group {
  margin-top: 8px;
  border-top: 1px solid rgba(0, 180, 255, 0.2);
  padding-top: 8px;
  flex-direction: row;
}

.control-btn {
  width: 32px;
  height: 32px;
  background-color: rgba(0, 50, 100, 0.5);
  border: 1px solid rgba(0, 180, 255, 0.4);
  border-radius: 6px;
  color: #00ffff;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.control-btn:hover {
  background-color: rgba(0, 100, 180, 0.5);
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.5);
  transform: translateY(-1px);
  border-color: rgba(0, 255, 255, 0.6);
}

.control-btn:active {
  background-color: rgba(0, 150, 255, 0.3);
  transform: translateY(1px);
}

.control-btn.reset {
  background-color: rgba(0, 80, 120, 0.5);
  font-size: 14px;
}

.control-btn.zoom-btn {
  flex: 1;
  font-size: 16px;
  font-weight: bold;
}

.control-btn.preset-btn {
  background-color: rgba(0, 70, 110, 0.5);
  font-weight: 600;
}

.control-btn.preset-btn:hover {
  background-color: rgba(0, 120, 180, 0.5);
}

/* 使用 Element UI 图标 */
[class^="el-icon-"] {
  font-family: element-icons !important;
  speak: none;
  font-style: normal;
  font-weight: 400;
  font-variant: normal;
  text-transform: none;
  line-height: 1;
  vertical-align: baseline;
  display: inline-block;
}

.el-icon-arrow-up:before {
  content: "↑";
}

.el-icon-arrow-down:before {
  content: "↓";
}

.el-icon-arrow-left:before {
  content: "←";
}

.el-icon-arrow-right:before {
  content: "→";
}

.el-icon-refresh:before {
  content: "⟲";
}

.el-icon-plus:before {
  content: "+";
}

.el-icon-minus:before {
  content: "-";
}

.camera-info {
  position: absolute;
  right: 15px;
  bottom: 15px;
  background-color: rgba(2, 12, 32, 0.8);
  border: 1px solid #0e5986;
  border-radius: 5px;
  padding: 10px;
  width: 260px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 2px;
  backdrop-filter: blur(2px);
  box-shadow: 0 0 10px rgba(0, 145, 234, 0.3);
}

.info-label {
  font-size: 12px;
  color: #e6e6e6;
}

.info-value {
  font-size: 12px;
  color: #00ffff;
  font-family: monospace;
  margin-bottom: 5px;
}

/* 自定义信息窗体样式 */
:deep(.custom-info-window) {
  padding: 15px;
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.9) 0%, rgba(4, 47, 79, 0.95) 100%);
  border: 1px solid #0e5986;
  border-radius: 5px;
  box-shadow: 0 0 15px rgba(0, 145, 234, 0.4);
  color: #fff;
  min-width: 260px;
  position: relative;
}

:deep(.custom-info-window::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #00b4ff 0%, #0077ff 100%);
}

:deep(.info-title) {
  font-size: 14px;
  font-weight: 600;
  color: #00ffff;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.6);
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

:deep(.info-content) {
  font-size: 12px;
  line-height: 1.5;
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.9);
}

:deep(.info-data) {
  background: rgba(0, 30, 60, 0.5);
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 12px;
  border: 1px solid rgba(14, 89, 134, 0.4);
}

:deep(.info-data-item) {
  font-size: 12px;
  margin-bottom: 6px;
  display: flex;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.85);
}

:deep(.info-data-item:last-child) {
  margin-bottom: 0;
}

:deep(.info-data-item span) {
  font-weight: 600;
  letter-spacing: 0.5px;
}

:deep(.info-button-container) {
  text-align: center;
}

:deep(.info-button) {
  padding: 6px 16px;
  background-color: rgba(0, 145, 234, 0.3);
  border: 1px solid rgba(0, 145, 234, 0.7);
  border-radius: 4px;
  color: #00ffff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

:deep(.info-button:hover) {
  background-color: rgba(0, 145, 234, 0.5);
  box-shadow: 0 0 8px rgba(0, 145, 234, 0.8);
  transform: translateY(-1px);
}

:deep(.info-button:active) {
  transform: translateY(1px);
}

:deep(.amap-info-close) {
  color: rgba(255, 255, 255, 0.7) !important;
  background-color: transparent !important;
  font-size: 16px !important;
  top: 10px !important;
  right: 10px !important;
}

:deep(.amap-info-close:hover) {
  color: #fff !important;
}

:deep(.amap-info-sharp) {
  background-color: #0e5986 !important;
}

/* 为地图标记添加动画 */
@keyframes markerPulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

:deep(.amap-marker:hover) {
  animation: markerPulse 1.5s infinite ease-in-out;
  z-index: 110 !important;
}

:deep(.amap-info-contentContainer) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

:deep(.amap-info-content) {
  padding: 0 !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

/* 在CSS样式中添加或修改traffic-chart样式 */
.traffic-chart {
  height: 180px;
  width: 100%;
  border-radius: 4px;
  border: 1px solid rgba(14, 89, 134, 0.4);
  background-color: rgba(0, 145, 234, 0.05);
}

/* 历史记录模块样式 */
.history-records .panel-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 5px;
}

.time-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.time-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.time-input {
  background-color: rgba(2, 12, 32, 0.7);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 4px;
  padding: 6px 10px;
  color: #fff;
  font-size: 12px;
  width: 100%;
  outline: none;
  transition: all 0.3s;
}

.time-input:focus {
  border-color: #00ffff;
  box-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.history-controls {
  display: flex;
  gap: 10px;
  margin-top: 5px;
}

.history-btn {
  flex: 1;
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.8) 0%, rgba(4, 47, 79, 0.9) 100%);
  border: 1px solid #0e5986;
  border-radius: 4px;
  padding: 8px 0;
  color: #00ffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.history-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(6, 92, 132, 0.8) 0%, rgba(4, 57, 89, 0.9) 100%);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
  transform: translateY(-1px);
}

.history-btn:active:not(:disabled) {
  transform: translateY(1px);
}

.history-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.history-btn .icon {
  font-size: 14px;
}

.history-btn.play-btn i {
  color: #52c41a;
}

.history-btn.stop-btn i {
  color: #ff4d4f;
}

.history-btn.export-btn i {
  color: #faad14;
}

.history-status {
  font-size: 12px;
  color: #00ffff;
  text-align: center;
  margin-top: 8px;
  min-height: 18px;
  background-color: rgba(0, 30, 60, 0.5);
  border-radius: 4px;
  padding: 4px 8px;
  border: 1px solid rgba(14, 89, 134, 0.4);
}

.playback-progress {
  margin-top: 8px;
}

.progress-bar {
  height: 6px;
  background-color: rgba(14, 89, 134, 0.3);
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00b4ff 0%, #0077ff 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-time {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 5px;
  font-family: monospace;
}

.time-input-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: rgba(2, 12, 32, 0.7);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 4px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.time-input-wrapper:hover {
  border-color: rgba(0, 255, 255, 0.7);
  background-color: rgba(6, 82, 122, 0.4);
}

.time-display {
  font-size: 12px;
  color: #fff;
}

.time-icon {
  font-size: 16px;
  color: rgba(0, 255, 255, 0.7);
}

/* 修改弹出窗口样式，使其符合暗色主题 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.modal-content {
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.9) 0%, rgba(4, 47, 79, 0.95) 100%);
  border: 1px solid #0e5986;
  border-radius: 5px;
  padding: 20px;
  box-shadow: 0 0 20px rgba(0, 145, 234, 0.3);
  width: 350px;
  max-width: 90%;
  color: #fff;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.6);
  padding-bottom: 10px;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: bold;
  color: #00ffff;
  margin: 0;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.close-btn {
  font-size: 24px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s;
}

.close-btn:hover {
  color: #fff;
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.5);
}

.modal-body {
  margin-bottom: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-btn {
  padding: 8px 16px;
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  background: rgba(4, 47, 79, 0.6);
  color: #fff;
}

.modal-btn:hover {
  background: rgba(6, 92, 132, 0.7);
  box-shadow: 0 0 8px rgba(0, 145, 234, 0.5);
  transform: translateY(-1px);
}

.modal-btn.cancel {
  background: rgba(40, 40, 40, 0.6);
  color: rgba(255, 255, 255, 0.8);
}

.modal-btn.confirm {
  background: rgba(0, 120, 215, 0.6);
  color: #fff;
}

.time-input-modal {
  background-color: rgba(2, 12, 32, 0.7);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 4px;
  padding: 10px;
  width: 100%;
  color: #fff;
  outline: none;
}

.time-input-modal:focus {
  border-color: #00ffff;
  box-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.export-options {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.export-option {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(2, 12, 32, 0.5);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 5px;
  padding: 15px;
}

.export-option:hover {
  background: rgba(6, 92, 132, 0.3);
  border-color: rgba(0, 180, 255, 0.5);
  box-shadow: 0 0 10px rgba(0, 145, 234, 0.3);
  transform: translateY(-2px);
}

.export-icon {
  font-size: 28px;
  margin-right: 15px;
}

.export-title {
  font-size: 16px;
  font-weight: bold;
  color: #00ffff;
  margin-bottom: 5px;
}

.export-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.container {
  width: 100%;
  height: 100%;
}

/* 修改动画控制面板样式 */
.animation-controls {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 600px;
  padding: 5px 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.animation-progress-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.progress-wrapper {
  flex: 1;
  position: relative;
}

.animation-info {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-family: 'Courier New', monospace;
  white-space: nowrap;
  min-width: 75px;
  text-align: center;
  background: rgba(0, 0, 0, 0.2);
  padding: 2px 5px;
  border-radius: 3px;
  box-shadow: 0 0 4px rgba(0, 255, 255, 0.1);
}

.progress-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
  outline: none;
  transition: 0.2s;
  cursor: pointer;
  box-shadow: 0 0 3px rgba(0, 255, 255, 0.2);
}

.progress-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #00ffff;
  cursor: pointer;
  box-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
}

.progress-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #00ffff;
  cursor: pointer;
  box-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
  border: none;
}

.progress-slider:hover::-webkit-slider-thumb {
  transform: scale(1.1);
  background: #7fffff;
}

.progress-slider:hover::-moz-range-thumb {
  transform: scale(1.1);
  background: #7fffff;
}

.animation-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.animation-btn {
  background: rgba(0, 0, 0, 0.2);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #00ffff;
  transition: all 0.2s ease;
}

.animation-btn:hover:not(:disabled) {
  color: #7fffff;
  transform: scale(1.1);
  background: rgba(0, 255, 255, 0.1);
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3);
}

.animation-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.animation-btn .el-icon {
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.progress-slider:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.animation-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  color: rgba(0, 255, 255, 0.5);
}

.scene3d-button {
  position: absolute;
  top: 80px; /* data-indicators的高度+边距，确保在数据指标下方 */
  right: 20px;
  z-index: 10;
}

/* 车流量监控模块样式 */
.vehicle-stream-monitor {
  height: 35%;
  min-height: 200px;
  max-height: 500px; /* 添加最大高度限制 */
}
</style>