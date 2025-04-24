<template>
  <div class="scene-viewer-container">
    <div class="back-button" @click="goBack">
      <span class="back-icon">←</span>
      <span class="back-text">返回</span>
    </div>
    <div class="scene-header">
      <h1 class="title">3D实时场景</h1>
      <div class="info-panel">
        <div class="time-display">{{ currentTime }}</div>
      </div>
    </div>
    <div class="status-table">
      <div class="status-row">
        <div class="status-label">车辆</div>
        <div class="status-bar-track">
          <div class="status-bar-fill" :style="{ width: `${(vehicleCount / 50) * 100}%` }"></div>
        </div>
        <div class="status-value">{{ vehicleCount }}</div>
      </div>
      <div class="status-row">
        <div class="status-label">行人</div>
        <div class="status-bar-track">
          <div class="status-bar-fill" :style="{ width: `${(pedestrianCount / 20) * 100}%` }"></div>
        </div>
        <div class="status-value">{{ pedestrianCount }}</div>
      </div>
      <div class="status-row">
        <div class="status-label">信号灯</div>
        <div class="status-bar-track">
          <div class="status-bar-fill" :style="{ width: `${(trafficLightCount / 10) * 100}%` }"></div>
        </div>
        <div class="status-value">{{ trafficLightCount }}</div>
      </div>
    </div>
    <div class="scene-content">
      <canvas ref="canvas" class="scene-canvas"></canvas>
      <div class="scene-overlay">
        <div class="controls-hint">
          <div class="hint-item">鼠标左键: 旋转视角</div>
          <div class="hint-item">鼠标右键: 平移视角</div>
          <div class="hint-item">鼠标滚轮: 缩放视角</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, markRaw, nextTick } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { useRouter } from 'vue-router';

const router = useRouter();

// 响应式状态
const canvas = ref(null);
const scene = ref(null);
const camera = ref(null);
const renderer = ref(null);
const controls = ref(null);
const socket = ref(null);
const mixer = ref(null);
const clock = ref(new THREE.Clock());
const vehiclePool = ref([]);
const pedestrianPool = ref([]);
const trafficLightPool = ref([]);
const activeVehicles = ref(new Map());
const activePedestrians = ref(new Map());
const activeTrafficLights = ref(new Map());
const lastFrameNumber = ref(0);
const animationMixers = ref(new Map());
const animationQueue = ref([]);
const isPlaying = ref(false);
const currentTime = ref('');
const sceneModel = ref(null);
const isConnected = ref(false);
const vehicleAnimations = ref(new Map());
const lastUpdateTime = ref(0); // 记录上次更新时间
const updateInterval = 33; // 约30fps的更新间隔

// 计算属性
const vehicleCount = computed(() => activeVehicles.value.size);
const pedestrianCount = computed(() => activePedestrians.value.size);
const trafficLightCount = computed(() => {
  let count = 0;
  for (const lightId in trafficLightPool.value) {
    const lightGroup = trafficLightPool.value[lightId];
    for (const color in lightGroup) {
      if (color !== 'userData' && lightGroup[color].visible) {
        count++;
      }
    }
  }
  return count;
});

// 常量配置
const CONFIG = {
  CAMERA: {
    FOV: 60,
    NEAR: 0.1,
    FAR: 1000,
    POSITION: { x: -50.0, y: 50.0, z: 50.0 },
    TARGET: { x: 0, y: 0, z: 0 }
  },
  CONTROLS: {
    ENABLE_DAMPING: true,
    DAMPING_FACTOR: 0.05,
    MIN_DISTANCE: 30,
    MAX_DISTANCE: 200,
    MAX_POLAR_ANGLE: Math.PI,
    MIN_POLAR_ANGLE: 0
  },
  LIGHT: {
    AMBIENT: { color: 0xffffff, intensity: 0.8 },
    MAIN: { color: 0xffffff, intensity: 1.2, position: { x: 50, y: 100, z: 50 } },
    FILL: { color: 0xffffff, intensity: 0.8, position: { x: -50, y: 50, z: -50 } }
  },
  SCENE: {
    BACKGROUND: 0x1a1a1a
  },
  WEBSOCKET: {
    RECONNECT_DELAY: 10000,
    PORT: 8765
  }
};

// WebSocket配置
const WS_CONFIG = {
  url: `ws://${window.location.hostname}:${CONFIG.WEBSOCKET.PORT}`,
  reconnectInterval: 3000,
  maxReconnectAttempts: 5
}

// 场景配置
const SCENE_CONFIG = {
  modelPath: "./scene_longhu_adjust.glb", // 修改为新的场景文件名
  scale: 1.0,
  position: { x: 0, y: 0, z: 0 },
  rotation: { x: 0, y: 0, z: 0 }
};

// 工具函数
const utils = {
  createScene() {
    const newScene = new THREE.Scene();
    newScene.background = new THREE.Color(CONFIG.SCENE.BACKGROUND);
    return newScene;
  },

  createCamera() {
    const newCamera = new THREE.PerspectiveCamera(
      CONFIG.CAMERA.FOV,
      window.innerWidth / window.innerHeight,
      CONFIG.CAMERA.NEAR,
      CONFIG.CAMERA.FAR
    );
    newCamera.position.set(
      CONFIG.CAMERA.POSITION.x,
      CONFIG.CAMERA.POSITION.y,
      CONFIG.CAMERA.POSITION.z
    );
    newCamera.lookAt(
      CONFIG.CAMERA.TARGET.x,
      CONFIG.CAMERA.TARGET.y,
      CONFIG.CAMERA.TARGET.z
    );
    return newCamera;
  },

  createRenderer(canvas) {
    const newRenderer = new THREE.WebGLRenderer({ 
      canvas: canvas,
      antialias: true,
      powerPreference: "high-performance",
      precision: "mediump",
      stencil: false,
      depth: true,
      alpha: false
    });
    
    // 修改渲染器尺寸设置
    const updateSize = () => {
      const width = window.innerWidth;
      const height = window.innerHeight;
      newRenderer.setSize(width, height);
      if (camera.value) {
        camera.value.aspect = width / height;
        camera.value.updateProjectionMatrix();
      }
    };
    
    // 初始设置尺寸
    updateSize();
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', updateSize);
    
    newRenderer.setClearColor(CONFIG.SCENE.BACKGROUND, 1);
    newRenderer.shadowMap.enabled = false;
    newRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    return newRenderer;
  },

  createControls(camera, renderer) {
    const newControls = new OrbitControls(camera, renderer.domElement);
    newControls.enableDamping = CONFIG.CONTROLS.ENABLE_DAMPING;
    newControls.dampingFactor = CONFIG.CONTROLS.DAMPING_FACTOR;
    newControls.minDistance = CONFIG.CONTROLS.MIN_DISTANCE;
    newControls.maxDistance = CONFIG.CONTROLS.MAX_DISTANCE;
    newControls.maxPolarAngle = CONFIG.CONTROLS.MAX_POLAR_ANGLE;
    newControls.minPolarAngle = CONFIG.CONTROLS.MIN_POLAR_ANGLE;
    newControls.target.set(
      CONFIG.CAMERA.TARGET.x,
      CONFIG.CAMERA.TARGET.y,
      CONFIG.CAMERA.TARGET.z
    );
    return newControls;
  },

  createLights() {
    const lights = [];
    
    // 环境光
    const ambientLight = new THREE.AmbientLight(
      CONFIG.LIGHT.AMBIENT.color,
      CONFIG.LIGHT.AMBIENT.intensity
    );
    lights.push(ambientLight);
    
    // 主光源
    const mainLight = new THREE.DirectionalLight(
      CONFIG.LIGHT.MAIN.color,
      CONFIG.LIGHT.MAIN.intensity
    );
    mainLight.position.set(
      CONFIG.LIGHT.MAIN.position.x,
      CONFIG.LIGHT.MAIN.position.y,
      CONFIG.LIGHT.MAIN.position.z
    );
    mainLight.castShadow = true;
    lights.push(mainLight);
    
    // 辅助光源
    const fillLight = new THREE.DirectionalLight(
      CONFIG.LIGHT.FILL.color,
      CONFIG.LIGHT.FILL.intensity
    );
    fillLight.position.set(
      CONFIG.LIGHT.FILL.position.x,
      CONFIG.LIGHT.FILL.position.y,
      CONFIG.LIGHT.FILL.position.z
    );
    lights.push(fillLight);
    
    return lights;
  }
};

// 初始化场景
const initScene = () => {
  scene.value = markRaw(utils.createScene());
  camera.value = markRaw(utils.createCamera());
  renderer.value = markRaw(utils.createRenderer(canvas.value));
  controls.value = markRaw(utils.createControls(camera.value, renderer.value));
  
  // 添加灯光
  const lights = utils.createLights();
  lights.forEach(light => scene.value.add(light));
  
  // 开始动画循环
  animate();
};

// 加载场景
const loadScene = async () => {
  if (socket.value) {
    socket.value.close();
  }
  
  cleanupScene();
  
  const loader = new GLTFLoader();
  try {
    const gltf = await loader.loadAsync(SCENE_CONFIG.modelPath);
    console.log('GLTF loaded successfully:', gltf);
    
    if (!gltf || !gltf.scene) {
      throw new Error('Invalid GLTF data: scene is undefined');
    }
    
    sceneModel.value = markRaw(gltf.scene);
    
    // 初始化对象池
    vehiclePool.value = [];
    pedestrianPool.value = [];
    trafficLightPool.value = {};
    activeVehicles.value.clear();
    activePedestrians.value.clear();
    activeTrafficLights.value.clear();
    
    // 先处理信号灯
    if (sceneModel.value) {
      sceneModel.value.traverse((child) => {
        if (child.isGroup) {
          if (child.name.toLowerCase().includes('light') || 
              child.name.toLowerCase().includes('_red') || 
              child.name.toLowerCase().includes('_yellow') || 
              child.name.toLowerCase().includes('_green')) {
            handleTrafficLightGroup(child);
          }
        }
      });
    }
    
    // 缩放场景
    sceneModel.value.scale.set(SCENE_CONFIG.scale, SCENE_CONFIG.scale, SCENE_CONFIG.scale);
    sceneModel.value.position.set(SCENE_CONFIG.position.x, SCENE_CONFIG.position.y, SCENE_CONFIG.position.z);
    
    // 处理其他模型对象
    if (sceneModel.value) {
      sceneModel.value.traverse((child) => {
        if (child.isGroup) {
          if (child.name.toLowerCase().startsWith('car')) {
            handleVehicleGroup(child);
          } else if (child.name.toLowerCase().includes('man') || child.name.toLowerCase().includes('pedestrian')) {
            handlePedestrianGroup(child);
          }
        }
      });
    }
    
    scene.value.add(sceneModel.value);
    
    // 确保在场景加载完成后立即调整摄像机
    nextTick(() => {
      adjustCamera();
      // 强制更新控制器
      if (controls.value) {
        controls.value.update();
      }
    });
    
    connectWebSocket();
  } catch (error) {
    console.error('Error loading GLTF:', error);
    setTimeout(loadScene, 1000);
  }
};

// 处理车辆组
const handleVehicleGroup = (group) => {
  group.position.set(0, -5, 0);
  group.rotation.set(0, 0, 0);
  group.rotateX(Math.PI/2);
  group.scale.set(1, 1, 1);
  group.visible = false;
  
  // 优化材质处理
  if (group.children && group.children.length > 0) {
    const firstMesh = group.children.find(c => c.isMesh);
    if (firstMesh) {
      const material = firstMesh.material.clone();
      material.color.set(getColor('gray'));
      material.needsUpdate = true;
      material.dithering = false;
      material.precision = "mediump";
      material.emissive = new THREE.Color(0x111111); // 添加自发光
      material.emissiveIntensity = 0.1; // 设置自发光强度
      firstMesh.material = material;
    }
  }
  
  vehiclePool.value.push(group);
};

// 处理行人组
const handlePedestrianGroup = (group) => {
  group.position.set(0, -5, 0);
  group.rotation.set(0, 0, 0);
  group.rotateX(Math.PI/2);
  group.visible = false;
  
  // 优化材质处理
  if (group.children && group.children.length > 0) {
    const firstMesh = group.children.find(c => c.isMesh);
    if (firstMesh) {
      const material = firstMesh.material.clone();
      material.color.set(getColor('gray'));
      material.needsUpdate = true;
      material.dithering = false;
      material.precision = "mediump";
      material.emissive = new THREE.Color(0x111111); // 添加自发光
      material.emissiveIntensity = 0.1; // 设置自发光强度
      firstMesh.material = material;
    }
  }
  
  pedestrianPool.value.push(group);
};

// 处理交通灯组
const handleTrafficLightGroup = (group) => {
  const [lightId, color] = group.name.split('_');
  console.log('Processing traffic light:', lightId, color, group.position);
  
  if (!trafficLightPool.value[lightId]) {
    trafficLightPool.value[lightId] = {};
  }
  trafficLightPool.value[lightId][color] = group;
  
  // 优化材质处理
  group.traverse((child) => {
    if (child.isMesh) {
      if (child.material) {
        child.material.dithering = false;
        child.material.precision = "mediump";
        child.material.needsUpdate = true;
        child.material.emissive = new THREE.Color(0x111111); // 添加自发光
        child.material.emissiveIntensity = 0.1; // 设置自发光强度
      }
    }
  });
  
  // 根据不同的light_id设置不同的正常大小
  let normalScale;
  if (lightId.includes('biglights1') || lightId.includes('biglights2')) {
    normalScale = new THREE.Vector3(2.055, 1.912, 2.444);
  } else if (lightId.includes('biglights3') || lightId.includes('biglights4')) {
    normalScale = new THREE.Vector3(1.212, 2.865, 2.840);
  } else {
    normalScale = new THREE.Vector3(0.019, 0.019, 0.019);
  }
  
  group.userData.normalScale = normalScale;
  group.userData.originalPosition = group.position.clone();
  
  if (color === 'red') {
    group.scale.copy(normalScale);
    group.visible = true;
  } else {
    group.scale.set(0.001, 0.001, 0.001);
    group.visible = false;
  }
  
  if (!group.parent) {
    scene.value.add(group);
  }
};

// 处理信号灯更新
const handleTrafficLightUpdates = (trafficLights) => {
  if (!trafficLights || !Array.isArray(trafficLights)) {
    console.warn('Invalid traffic lights data:', trafficLights);
    return;
  }

  console.log('Updating traffic lights:', trafficLights);
  console.log('Current traffic light pool:', trafficLightPool.value);

  // 处理每个信号灯
  trafficLights.forEach(lightData => {
    if (!lightData || !lightData.light_id || !lightData.state) {
      console.warn('Invalid traffic light data:', lightData);
      return;
    }

    const lightId = lightData.light_id;
    const color = lightData.state;
    
    console.log('Processing update for light:', lightId, color);
    
    // 获取该信号灯的所有颜色版本
    const lightGroup = trafficLightPool.value[lightId];
    if (!lightGroup) {
      console.warn('No traffic light group found for:', lightId);
      return;
    }
    
    // 获取上一次的颜色状态
    const lastColor = lightGroup.userData?.lastColor;
    
    // 如果颜色没有变化，保持当前状态
    if (lastColor === color) {
      return;
    }
    
    // 处理每个颜色版本
    for (const lightColor in lightGroup) {
      if (lightColor === 'userData') continue; // 跳过userData属性
      
      const light = lightGroup[lightColor];
      if (!light || !light.scale) {
        console.warn('Invalid light object:', light);
        continue;
      }

      console.log('Updating light:', lightId, lightColor, 'visible:', lightColor === color);

      if (lightColor === color) {
        // 当前颜色显示
        if (light.userData && light.userData.normalScale) {
          light.scale.copy(light.userData.normalScale);
          // 恢复原始位置，不进行任何缩放
          if (light.userData.originalPosition) {
            light.position.copy(light.userData.originalPosition);
            // 确保交通灯在场景中
            if (!light.parent) {
              scene.value.add(light);
            }
          }
          light.visible = true;
        } else {
          light.scale.set(1, 1, 1);
          light.visible = true;
          if (!light.parent) {
            scene.value.add(light);
          }
        }
      } else {
        // 其他颜色隐藏
        light.scale.set(0.001, 0.001, 0.001);
        light.visible = false;
      }
      
      // 更新所有子对象
      light.traverse((child) => {
        if (child.isMesh) {
          child.visible = light.visible;
          if (child.material) {
            child.material.transparent = true;
            child.material.opacity = 1;
            child.material.needsUpdate = true;
          }
        }
      });
    }
    
    // 更新颜色状态
    lightGroup.userData = lightGroup.userData || {};
    lightGroup.userData.lastColor = color;
  });
};

// 调整相机位置
const adjustCamera = () => {
  if (!sceneModel.value || !camera.value || !controls.value) return;
  
  // 计算场景的边界框
  const box = new THREE.Box3().setFromObject(sceneModel.value);
  const center = box.getCenter(new THREE.Vector3());
  const size = box.getSize(new THREE.Vector3());
  
  // 使用预设的摄像机位置和目标
  camera.value.position.set(
    CONFIG.CAMERA.POSITION.x,
    CONFIG.CAMERA.POSITION.y,
    CONFIG.CAMERA.POSITION.z
  );
  
  // 确保摄像机看向目标点
  camera.value.lookAt(
    CONFIG.CAMERA.TARGET.x,
    CONFIG.CAMERA.TARGET.y,
    CONFIG.CAMERA.TARGET.z
  );
  
  // 更新控制器目标点
  controls.value.target.set(
    CONFIG.CAMERA.TARGET.x,
    CONFIG.CAMERA.TARGET.y,
    CONFIG.CAMERA.TARGET.z
  );
  
  // 设置控制器的限制
  controls.value.maxDistance = CONFIG.CONTROLS.MAX_DISTANCE;
  controls.value.minDistance = CONFIG.CONTROLS.MIN_DISTANCE;
  controls.value.maxPolarAngle = CONFIG.CONTROLS.MAX_POLAR_ANGLE;
  controls.value.minPolarAngle = CONFIG.CONTROLS.MIN_POLAR_ANGLE;
  
  // 强制更新控制器
  controls.value.update();
  
  console.log('Camera adjusted:', {
    position: camera.value.position,
    target: controls.value.target,
    distance: camera.value.position.distanceTo(controls.value.target)
  });
};

// 动画循环
const animate = () => {
  requestAnimationFrame(animate);
  
  // 更新车辆动画
  const currentTime = performance.now();
  for (const [vehicle, animations] of vehicleAnimations.value) {
    const { position, rotation } = animations;
    
    // 更新位置
    if (position && !position.completed) {
      const elapsed = currentTime - position.startTime;
      const progress = Math.min(elapsed / position.duration, 1);
      
      vehicle.position.lerpVectors(
        position.startPosition,
        position.targetPosition,
        progress
      );
      
      if (progress >= 1) {
        position.completed = true;
        vehicle.position.copy(position.targetPosition);
      }
    }
    
    // 更新旋转
    if (rotation && !rotation.completed) {
      const elapsed = currentTime - rotation.startTime;
      const progress = Math.min(elapsed / rotation.duration, 1);
      
      vehicle.rotation.z = THREE.MathUtils.lerp(
        rotation.startRotation,
        rotation.targetRotation,
        progress
      );
      
      if (progress >= 1) {
        rotation.completed = true;
        vehicle.rotation.z = rotation.targetRotation;
      }
    }
    
    // 如果动画都完成了，从动画列表中移除
    if ((!position || position.completed) && (!rotation || rotation.completed)) {
      vehicleAnimations.value.delete(vehicle);
    }
  }
  
  if (scene.value && camera.value && renderer.value) {
    controls.value.update();
    renderer.value.render(scene.value, camera.value);
  }
};

// WebSocket连接
const connectWebSocket = () => {
  if (socket.value) {
    socket.value.close();
  }
  
  socket.value = new WebSocket(WS_CONFIG.url);
  
  socket.value.onopen = () => {
    console.log('WebSocket connected to:', WS_CONFIG.url);
    isConnected.value = true;
    socket.value.send(JSON.stringify({
      type: 'start_processing',
      csv_path: 'motion_data.csv'
    }));
  };

  socket.value.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'scene_update') {
        handleWebSocketMessage(event);
      } else if (data.type === 'error') {
        console.error('Server error:', data.message);
      }
    } catch (error) {
      console.error('Error parsing WebSocket message:', error);
    }
  };

  socket.value.onerror = (error) => {
    console.error('WebSocket error:', error);
    isConnected.value = false;
    setTimeout(reconnectWebSocket, CONFIG.WEBSOCKET.RECONNECT_DELAY);
  };

  socket.value.onclose = () => {
    console.log('WebSocket disconnected');
    isConnected.value = false;
    setTimeout(reconnectWebSocket, CONFIG.WEBSOCKET.RECONNECT_DELAY);
  };
};

// 重连WebSocket
const reconnectWebSocket = () => {
  if (!socket.value || socket.value.readyState === WebSocket.CLOSED) {
    console.log('Attempting to reconnect...');
    connectWebSocket();
  }
};

// 处理WebSocket消息
const handleWebSocketMessage = (event) => {
  const currentTime = performance.now();
  lastUpdateTime.value = currentTime;

  const data = JSON.parse(event.data);
  if (data.type !== 'scene_update') {
    console.warn('Unknown message type:', data.type);
    return;
  }
  
  const sceneData = data.data;
  const frameNumber = sceneData.frame_number;
  
  if (sceneData.vehicles) {
    handleVehicleUpdates(sceneData.vehicles);
  }
  
  if (sceneData.pedestrians) {
    handlePedestrianUpdates(sceneData.pedestrians);
  }
  
  if (sceneData.traffic_lights) {
    handleTrafficLightUpdates(sceneData.traffic_lights);
  }
  
  lastFrameNumber.value = frameNumber;
};

// 处理车辆更新
const handleVehicleUpdates = (vehicles) => {
  const currentVehicleIds = new Set(vehicles.map(v => v.car_id));
  
  // 处理不再活跃的车辆
  for (const [id, vehicle] of activeVehicles.value) {
    if (!currentVehicleIds.has(id)) {
      // 清理动画状态
      vehicleAnimations.value.delete(vehicle);
      // 瞬移到场景外并隐藏
      vehicle.position.y = -5;
      vehicle.visible = false;
      activeVehicles.value.delete(id);
      console.log('Deactivated vehicle:', id);
    }
  }
  
  // 更新或添加活跃车辆
  vehicles.forEach(vehicleData => {
    let vehicle = activeVehicles.value.get(vehicleData.car_id);
    
    if (!vehicle) {
      // 从对象池中获取一个未使用的车辆
      vehicle = vehiclePool.value.find(v => !v.visible);
      
      if (vehicle) {
        // 清理可能存在的旧动画状态
        vehicleAnimations.value.delete(vehicle);
        
        // 设置初始材质颜色
        if (vehicle.children && vehicle.children.length > 0) {
          const firstMesh = vehicle.children.find(child => child.isMesh);
          if (firstMesh) {
            firstMesh.material.color.set(getColor(vehicleData.color));
            firstMesh.material.needsUpdate = true;
          }
        }
        
        // 瞬移到目标位置
        vehicle.position.set(vehicleData.x, 0, -vehicleData.y);
        vehicle.rotation.z = -vehicleData.rot_z;
        vehicle.visible = true;
        activeVehicles.value.set(vehicleData.car_id, vehicle);
        
        console.log('Activated vehicle:', vehicleData.car_id, 'using object:', vehicle.name);
      } else {
        console.warn('No available vehicle object for:', vehicleData.car_id);
        return;
      }
    } else {
      // 确保车辆可见
      if (!vehicle.visible) {
        vehicle.visible = true;
        console.log('Reactivated vehicle:', vehicleData.car_id);
      }
      updateVehicle(vehicle, vehicleData);
    }
  });
};

// 更新车辆状态
const updateVehicle = (vehicle, vehicleData) => {
  // 检查车辆是否仍然在活动列表中
  if (!activeVehicles.value.has(vehicleData.car_id)) {
    console.warn('Vehicle not in active list:', vehicleData.car_id);
    return;
  }

  const currentPosition = vehicle.position.clone();
  const targetPosition = new THREE.Vector3(vehicleData.x, 0, -vehicleData.y);
  const currentRotation = vehicle.rotation.z;
  const targetRotation = -vehicleData.rot_z;
  
  // 检查是否需要更新动画
  const existingAnimation = vehicleAnimations.value.get(vehicle);
  if (existingAnimation) {
    const { position, rotation } = existingAnimation;
    // 如果目标位置相同，不需要更新动画
    if (position && position.targetPosition.equals(targetPosition) && 
        rotation && rotation.targetRotation === targetRotation) {
      return;
    }
  }
  
  // 使用固定时间
  const duration = 333; // 固定1/3秒
  
  // 创建位置动画
  const positionAnimation = {
    startPosition: currentPosition.clone(),
    targetPosition: targetPosition.clone(),
    startTime: performance.now(),
    duration: duration,
    completed: false
  };
  
  // 创建旋转动画
  const rotationAnimation = {
    startRotation: currentRotation,
    targetRotation: targetRotation,
    startTime: performance.now(),
    duration: duration,
    completed: false
  };
  
  // 存储动画数据
  vehicleAnimations.value.set(vehicle, {
    position: positionAnimation,
    rotation: rotationAnimation
  });
  
  // 只在颜色变化时更新材质
  if (vehicle.children && vehicle.children.length > 0) {
    const firstMesh = vehicle.children.find(child => child.isMesh);
    if (firstMesh) {
      const newColor = getColor(vehicleData.color);
      if (firstMesh.material.color.getHex() !== newColor) {
        firstMesh.material.color.set(newColor);
        firstMesh.material.needsUpdate = true;
      }
    }
  }
};

// 处理行人更新
const handlePedestrianUpdates = (pedestrians) => {
  const currentPedestrianIds = new Set(pedestrians.map(p => p.man_id));
  
  // 处理不再活跃的行人
  for (const [id, pedestrian] of activePedestrians.value) {
    if (!currentPedestrianIds.has(id)) {
      // 清理动画状态
      vehicleAnimations.value.delete(pedestrian);
      // 瞬移到场景外并隐藏
      pedestrian.position.y = -5;
      pedestrian.visible = false;
      activePedestrians.value.delete(id);
      console.log('Deactivated pedestrian:', id);
    }
  }
  
  // 更新或添加活跃行人
  pedestrians.forEach(pedestrianData => {
    // 确保man_id存在
    if (!pedestrianData.man_id) {
      console.warn('Pedestrian data missing man_id:', pedestrianData);
      return;
    }

    let pedestrian = activePedestrians.value.get(pedestrianData.man_id);
    
    if (!pedestrian) {
      // 从对象池中获取一个未使用的行人
      pedestrian = pedestrianPool.value.find(p => !p.visible);
      
      if (pedestrian) {
        // 清理可能存在的旧动画状态
        vehicleAnimations.value.delete(pedestrian);
        
        // 设置初始材质颜色
        if (pedestrian.children && pedestrian.children.length > 0) {
          const firstMesh = pedestrian.children.find(child => child.isMesh);
          if (firstMesh) {
            firstMesh.material.color.set(getColor(pedestrianData.color || 'gray'));
            firstMesh.material.needsUpdate = true;
          }
        }
        
        // 设置初始位置和旋转
        pedestrian.position.set(pedestrianData.x, -0.3, -pedestrianData.y);
        pedestrian.rotation.z = -pedestrianData.rot_z;
        pedestrian.visible = true;
        activePedestrians.value.set(pedestrianData.man_id, pedestrian);
        
        console.log('Activated pedestrian:', pedestrianData.man_id, 'using object:', pedestrian.name);
      } else {
        console.warn('No available pedestrian object for:', pedestrianData.man_id);
        return;
      }
    } else {
      // 确保行人可见
      if (!pedestrian.visible) {
        pedestrian.visible = true;
        console.log('Reactivated pedestrian:', pedestrianData.man_id);
      }
      updatePedestrian(pedestrian, pedestrianData);
    }
  });
};

// 更新行人状态
const updatePedestrian = (pedestrian, pedestrianData) => {
  // 检查行人是否仍然在活动列表中
  if (!activePedestrians.value.has(pedestrianData.man_id)) {
    console.warn('Pedestrian not in active list:', pedestrianData.man_id);
    return;
  }

  const currentPosition = pedestrian.position.clone();
  const targetPosition = new THREE.Vector3(pedestrianData.x, -0.3, -pedestrianData.y);
  const currentRotation = pedestrian.rotation.z;
  const targetRotation = -pedestrianData.rot_z;
  
  // 检查是否需要更新动画
  const existingAnimation = vehicleAnimations.value.get(pedestrian);
  if (existingAnimation) {
    const { position, rotation } = existingAnimation;
    // 如果目标位置相同，不需要更新动画
    if (position && position.targetPosition.equals(targetPosition) && 
        rotation && rotation.targetRotation === targetRotation) {
      return;
    }
  }
  
  // 使用固定时间
  const duration = 333; // 固定1/3秒
  
  // 创建位置动画
  const positionAnimation = {
    startPosition: currentPosition.clone(),
    targetPosition: targetPosition.clone(),
    startTime: performance.now(),
    duration: duration,
    completed: false
  };
  
  // 创建旋转动画
  const rotationAnimation = {
    startRotation: currentRotation,
    targetRotation: targetRotation,
    startTime: performance.now(),
    duration: duration,
    completed: false
  };
  
  // 存储动画数据
  vehicleAnimations.value.set(pedestrian, {
    position: positionAnimation,
    rotation: rotationAnimation
  });
  
  // 只在颜色变化时更新材质
  if (pedestrian.children && pedestrian.children.length > 0) {
    const firstMesh = pedestrian.children.find(child => child.isMesh);
    if (firstMesh) {
      const newColor = getColor(pedestrianData.color);
      if (firstMesh.material.color.getHex() !== newColor) {
        firstMesh.material.color.set(newColor);
        firstMesh.material.needsUpdate = true;
      }
    }
  }
};

// 颜色映射
const getColor = (colorName) => {
  const colorMap = {
    'blue': 0x0000FF,
    'red': 0xFF0000,
    'green': 0x00FF00,
    'yellow': 0xFFFF00,
    'white': 0xFFFFFF,
    'black': 0x000000,
    'gray': 0x808080,
    'orange': 0xFFA500,
    'purple': 0x800080,
    'pink': 0xFFC0CB,
    'brown': 0x996633,
    'cyan': 0x00FFFF,
    'magenta': 0xFF00FF
  };
  return colorMap[colorName.toLowerCase()] || 0x808080;
};

// 清理场景
const cleanupScene = () => {
  while(scene.value.children.length > 0){ 
    scene.value.remove(scene.value.children[0]); 
  }
  
  const lights = utils.createLights();
  lights.forEach(light => scene.value.add(light));
};

// 窗口大小改变处理
const handleResize = () => {
  if (!camera.value || !renderer.value) return;
  camera.value.aspect = window.innerWidth / window.innerHeight;
  camera.value.updateProjectionMatrix();
  renderer.value.setSize(window.innerWidth, window.innerHeight);
};

// 监听连接状态
watch(isConnected, (newValue) => {
  console.log('WebSocket connection status changed:', newValue);
});

// 添加时间显示
const updateTime = () => {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit',
    hour12: false 
  });
};

// 返回按钮点击事件
const goBack = () => {
  router.back();
};

// 生命周期钩子
onMounted(() => {
  initScene();
  loadScene();
  window.addEventListener('resize', handleResize);
  updateTime();
  setInterval(updateTime, 1000);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (socket.value) {
    socket.value.close();
  }
  if (renderer.value) {
    renderer.value.dispose();
  }
  if (controls.value) {
    controls.value.dispose();
  }
  if (scene.value) {
    scene.value.traverse((object) => {
      if (object.geometry) {
        object.geometry.dispose();
      }
      if (object.material) {
        if (Array.isArray(object.material)) {
          object.material.forEach(material => material.dispose());
        } else {
          object.material.dispose();
        }
      }
    });
  }
});
</script>

<style scoped>
.scene-viewer-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.back-button {
  position: absolute;
  top: 10px;
  left: 15px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  backdrop-filter: blur(5px);
  cursor: pointer;
  z-index: 1001;
  transition: all 0.3s ease;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.8);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateX(-2px);
}

.back-icon {
  font-size: 1.2em;
  background: linear-gradient(45deg, #00ff9d, #00b8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 10px rgba(0, 255, 157, 0.3);
}

.back-text {
  font-size: 0.9em;
  background: linear-gradient(45deg, #00ff9d, #00b8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 10px rgba(0, 255, 157, 0.3);
}

.scene-header {
  padding: 10px 15px;
  background: linear-gradient(to right, #1a1a1a, #2a2a2a);
  border-bottom: 1px solid #333;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.status-table {
  position: absolute;
  top: 50px; /* 调整位置，为返回按钮留出空间 */
  left: 15px;
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  backdrop-filter: blur(5px);
  min-width: 200px;
  z-index: 1000;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0;
  color: #aaa;
  font-size: 0.85em;
}

.status-row::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #00ff9d;
  border-radius: 50%;
  margin-right: 4px;
  box-shadow: 0 0 5px #00ff9d;
}

.status-label {
  width: 50px;
  white-space: nowrap;
}

.status-bar-track {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
  min-width: 60px;
}

.status-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #00ff9d, #00b8ff);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.status-value {
  min-width: 30px;
  text-align: right;
  color: #fff;
}

.title {
  font-size: 1.8em;
  margin: 0;
  background: linear-gradient(45deg, #00ff9d, #00b8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 10px rgba(0, 255, 157, 0.3);
  letter-spacing: 2px;
  text-align: right;
  flex: 2;
  padding-right: 5%; /* 增加右内边距，使标题更靠右 */
}

.info-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
  margin-left: auto;
}

.time-display {
  font-size: 1.8em;
  font-weight: bold;
  background: linear-gradient(45deg, #00ff9d, #00b8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 10px rgba(0, 255, 157, 0.3);
  letter-spacing: 2px;
}

.scene-content {
  flex: 1;
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.scene-canvas {
  width: 100% !important;
  height: 100% !important;
  display: block;
}

.scene-overlay {
  position: absolute;
  bottom: 15px;
  right: 15px;
  pointer-events: none;
}

.controls-hint {
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  backdrop-filter: blur(5px);
}

.hint-item {
  color: #aaa;
  font-size: 0.85em;
  margin: 4px 0;
  display: flex;
  align-items: center;
}

.hint-item::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #00ff9d;
  border-radius: 50%;
  margin-right: 8px;
  box-shadow: 0 0 5px #00ff9d;
}

/* 添加一些动画效果 */
@keyframes pulse {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

.status-value.connected {
  animation: pulse 2s infinite;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .status-bar {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .title {
    font-size: 1.6em;
  }
  
  .status-item {
    min-width: 80px;
  }
}
</style> 