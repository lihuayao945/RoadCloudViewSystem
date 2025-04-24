<template>
  <div>
    <!-- 地图类型切换 - 水平排列 -->
    <div class="map-type-controls">
      <button 
        class="map-type-btn" 
        :class="{ 'active': currentMapType === '标准' }"
        @click="$emit('change-map-type', '标准')" 
        title="标准地图"
      >
        标准
      </button>
      <button 
        class="map-type-btn" 
        :class="{ 'active': currentMapType === '卫星' }"
        @click="$emit('change-map-type', '卫星')" 
        title="卫星地图"
      >
        卫星
      </button>
      <button 
        class="map-type-btn" 
        :class="{ 'active': currentMapType === '交通' }"
        @click="$emit('change-map-type', '交通')" 
        title="交通地图"
      >
        交通
      </button>
    </div>
    
    <!-- 放大缩小和全屏 - 垂直排列 -->
    <div class="zoom-fullscreen-controls">
      <!-- 全屏切换按钮 -->
      <button 
        class="control-btn expand-btn" 
        @click="$emit('toggle-expand')" 
        :title="isExpanded ? '退出全屏' : '全屏显示'"
      >
        <i :class="['expand-icon', { 'expanded': isExpanded }]"></i>
      </button>
      
      <!-- 缩放控件 -->
      <div class="zoom-controls">
        <button class="control-btn zoom-btn zoom-in" @click="$emit('zoom-in')" title="放大">
          <i class="zoom-in-icon">+</i>
        </button>
        <button class="control-btn zoom-btn zoom-out" @click="$emit('zoom-out')" title="缩小">
          <i class="zoom-out-icon">-</i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MapControls',
  props: {
    isExpanded: {
      type: Boolean,
      default: false
    },
    currentMapType: {
      type: String,
      default: '标准'
    }
  },
  emits: ['toggle-expand', 'zoom-in', 'zoom-out', 'change-map-type']
}
</script>

<style scoped>
/* 地图类型控件样式 - 水平排列 */
.map-type-controls {
  position: absolute;
  top: 70%;
  left: 47%;
  z-index: 100;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.map-type-btn {
  height: 34px;
  background-color: rgba(0, 21, 41, 0.6);
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
  padding: 0 12px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.map-type-btn:last-child {
  border-right: none;
}

.map-type-btn:hover {
  background-color: rgba(0, 21, 41, 0.8);
}

.map-type-btn.active {
  background-color: rgba(0, 145, 234, 0.7);
  font-weight: bold;
}

/* 放大缩小和全屏控件 - 垂直排列 */
.zoom-fullscreen-controls {
  position: absolute;
  top: 60%;
  left: 73%;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 控制按钮基础样式 */
.control-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 4px;
  background-color: rgba(0, 21, 41, 0.6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
}

.control-btn:hover {
  background-color: rgba(0, 21, 41, 0.8);
}

/* 全屏按钮样式 */
.expand-btn {
  background-color: rgba(0, 21, 41, 0.7);
}

.expand-icon {
  width: 16px;
  height: 16px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ffffff'%3E%3Cpath d='M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.expand-icon.expanded {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ffffff'%3E%3Cpath d='M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z'/%3E%3C/svg%3E");
}

/* 缩放控件样式 */
.zoom-controls {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.zoom-btn {
  height: 34px;
  font-size: 18px;
  font-weight: bold;
  color: white;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .map-type-controls {
    top: 10px;
    left: 10px;
  }
  
  .zoom-fullscreen-controls {
    top: 50px;
    left: 10px;
    gap: 5px;
  }
  
  .control-btn {
    width: 32px;
    height: 32px;
  }
  
  .zoom-btn {
    height: 28px;
  }
  
  .map-type-btn {
    height: 28px;
    font-size: 12px;
    padding: 0 8px;
  }
}
</style>