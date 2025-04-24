<template>
  <div class="app" >
    <!-- 加载动画 -->
    <div class="loading-overlay" v-if="isLoading">
      <div class="loading-spinner"></div>
      <div class="loading-text">路云可视化系统数据加载中...</div>
    </div>
    
    <Header>
      <!-- 将城市选择器传递到Header组件的插槽中 -->
      <template #city-selector>
        <CitySelector @city-change="changeCity" :currentCity="currentCity" />
      </template>
    </Header>
    
    <div class="main-content" :class="{ 'expanded': isExpanded }">
      <MapContainer 
        ref="mapContainer" 
        :currentMapType="currentMapType"
        :currentCity="currentCity" 
        @map-type-changed="updateMapType"
      />
      
      <!-- 车辆搜索组件 - 放在左面板上方，可以覆盖左面板内容 -->
      <div class="vehicle-search-wrapper">
        <VehicleSearch @search-active="onVehicleSearchActive" @search-cleared="onVehicleSearchCleared" />
      </div>
      
      <!-- 左上角面板：车辆数据监控 -->
      <LeftPanel v-show="!isExpanded && !vehicleSearchActive" />
      
      <!-- 左下角面板：车辆类型统计 -->
      <BottomPanel v-show="!isExpanded" :activeTab="activeTab" @change-tab="changeTab" />
      
      <!-- 添加设备搜索组件 - 位于右上方，但独立于RightPanel -->
      <DeviceSearch 
        v-show="!isExpanded" 
        @search-active="onSearchActive" 
        @search-cleared="onSearchCleared" 
        :onZoomTo="zoomToLocation"
      />      
      <!-- 右上角面板：交通设备总览 -->
      <RightPanel v-show="!isExpanded && !searchActive" />
      
      <!-- 右下角面板：路控设备告警 -->
      <BottomRightPanel v-show="!isExpanded" />
      
      <!-- 底部数据面板：系统性能指标 -->
      <BottomDataPanel v-show="!isExpanded" />
      
      <!-- 控制按钮 -->
      <MapControls 
        :isExpanded="isExpanded" 
        :currentMapType="currentMapType"
        @toggle-expand="toggleExpand"
        @zoom-in="zoomIn"
        @zoom-out="zoomOut"
        @change-map-type="changeMapType"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import Header from '../components/Header.vue'
import MapContainer from '../components/MapContainer.vue'
import LeftPanel from '../components/LeftPanel.vue'
import BottomPanel from '../components/BottomPanel.vue'
import RightPanel from '../components/RightPanel.vue'
import BottomRightPanel from '../components/BottomRightPanel.vue'
import MapControls from '../components/MapControls.vue'
import BottomDataPanel from '../components/BottomDataPanel.vue'
import CitySelector from '../components/CitySelector.vue'
import DeviceSearch from '../components/DeviceSearch.vue'
import VehicleSearch from '../components/VehicleSearch.vue'


export default {
  name: 'IndexView',
  components: {
    Header,
    MapContainer,
    LeftPanel,
    BottomPanel,
    RightPanel,
    BottomRightPanel,
    MapControls,
    BottomDataPanel,
    CitySelector,
    DeviceSearch,
    VehicleSearch
  },
  setup() {
    const isLoading = ref(true)
    const isExpanded = ref(false)
    const mapContainer = ref(null)
    const currentMapType = ref('标准')
    const activeTab = ref('按天')
    const searchActive = ref(false) // 控制设备搜索是否激活
    const vehicleSearchActive = ref(false) // 控制车辆搜索是否激活
    const currentCity = ref({
      name: '重庆市',
      center: [106.32554720015287, 29.51776878783989]
    })

    // 隐藏展开按钮
    onMounted(() => {
      document.getElementsByClassName("control-btn expand-btn")[0].style.display = "none";
    });


      // 传给子组件的定位方法
    const zoomToLocation = (longitude, latitude,zoomLevel) => {
      console.log('Zooming to location:', longitude, latitude);
      if (mapContainer.value) {
        // 设置地图中心点
        mapContainer.value.changeMapCenter([longitude, latitude]);
        // 设置合适的缩放级别
        mapContainer.value.map.setZoom(zoomLevel);
      }
    }



   

    function onVehicleSearchActive(event) {
      console.log('车辆搜索激活，隐藏左侧面板内容');
      vehicleSearchActive.value = true;
    }

    function onVehicleSearchCleared(event) {
      console.log('车辆搜索清除，显示左侧面板内容');
      // 检查事件来源，确保只响应来自车辆搜索的事件
      if (!event || !event.source || event.source === 'vehicle-search') {
        vehicleSearchActive.value = false;
      }
    }

    // 处理设备搜索激活和清除事件
    function onSearchActive() {
      searchActive.value = false;
      console.log('设备搜索激活，隐藏右侧面板内容');
    }
    
    function onSearchCleared() {
      searchActive.value = false;
      console.log('设备搜索清除，显示右侧面板内容');
    }
    
    // 处理车辆搜索激活和清除事件
    function onVehicleSearchActive() {
      vehicleSearchActive.value = true;
      console.log('车辆搜索激活，隐藏左侧面板内容');
    }
    
    function onVehicleSearchCleared() {
      vehicleSearchActive.value = false;
      console.log('车辆搜索清除，显示左侧面板内容');
    }

    // 切换全屏/正常模式
    const toggleExpand = () => {
      isExpanded.value = !isExpanded.value
      // 地图组件可能需要在状态变化后重新计算尺寸
      if (mapContainer.value) {
        setTimeout(() => {
          mapContainer.value.resizeMap()
        }, 300)
      }
    }

    // 放大地图
    const zoomIn = () => {
      if (mapContainer.value) {
        mapContainer.value.zoomIn()
      }
    }

    // 缩小地图
    const zoomOut = () => {
      if (mapContainer.value) {
        mapContainer.value.zoomOut()
      }
    }

    // 切换地图类型
    const changeMapType = (type) => {
      console.log('IndexView: Changing map type to', type);
      currentMapType.value = type;
    }
    
    // 添加新方法监听来自 MapContainer 的地图类型更改
    const updateMapType = (type) => {
      console.log('IndexView: Updating map type from MapContainer to', type);
      currentMapType.value = type;
    }

    // 切换底部面板选项卡
    const changeTab = (tab) => {
      activeTab.value = tab
    }

    // 切换城市
    const changeCity = (city) => {
      currentCity.value = city
      // 如果地图容器已经挂载，通过地图组件切换中心点
      if (mapContainer.value) {
        mapContainer.value.changeMapCenter(city.center)
      }
    }

    onMounted(() => {
      setTimeout(() => {
        isLoading.value = false
      }, 2000)
      
      // 确保视图在窗口大小变化时重新计算
      window.addEventListener('resize', () => {
        if (mapContainer.value) {
          mapContainer.value.resizeMap()
        }
      })
    })

    return {
      isLoading,
      isExpanded,
      mapContainer,
      currentMapType,
      activeTab,
      currentCity,
      searchActive,
      vehicleSearchActive,
      toggleExpand,
      zoomIn,
      zoomOut,
      changeMapType,
      updateMapType,
      changeTab,
      changeCity,
      onSearchActive,
      onSearchCleared,
      onVehicleSearchActive,
      zoomToLocation,
      onVehicleSearchCleared
    }
  }
}
</script>

<style>
/* 全局样式，不使用scoped */
html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>

<style scoped>
.app {
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
}

.main-content {
  position: relative;
  flex: 1;
  width: 100%;
  height: calc(100% - 60px); /* 减去 Header 的高度 */
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.main-content.expanded {
  /* 全屏模式样式，可以添加过渡效果等 */
  transition: all 0.3s ease;
  height: 100%;
}

/* 添加车辆搜索组件的容器样式 */
.vehicle-search-wrapper {
  position: absolute;
  top: 1%;
  left: 0.5%;
  width: 24%;
  z-index: 11; /* 确保在LeftPanel之上 */
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(255, 255, 255, 0.3);
  border-top: 5px solid #00ffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  margin-top: 10px;
  color: #00ffff;
  font-size: 16px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .main-content {
    width: 100%;
    max-width: 100%;
  }
  
  .vehicle-search-wrapper {
    width: 30%;
  }
}

@media (max-width: 768px) {
  .app {
    width: 100vw;
  }
  
  .loading-text {
    font-size: 14px;
  }
  
  .loading-spinner {
    width: 40px;
    height: 40px;
    border-width: 4px;
  }
  
  .vehicle-search-wrapper {
    width: 50%;
  }
}

@media (max-width: 480px) {
  .loading-text {
    font-size: 12px;
  }
  
  .loading-spinner {
    width: 30px;
    height: 30px;
    border-width: 3px;
  }
  
  .vehicle-search-wrapper {
    width: 90%;
  }
}
</style>