<template>
  <div class="vehicle-search-container">
    <div class="search-section">
      <div class="search-form">
        <input 
          type="text" 
          v-model="vehicleId" 
          placeholder="输入车辆ID查询 (例如: QD1E000J)" 
          class="search-input"
          @keyup.enter="searchVehicle"
          @input="handleSearchInput"
          ref="searchInput"
        />
        <button @click="searchVehicle" class="search-button" title="搜索">
          <i class="search-icon">🔍</i>
        </button>
        <button @click="showAllVehicles" class="list-button" title="显示所有车辆">
          <i class="list-icon">📋</i>
        </button>
      </div>
      
      <!-- 搜索建议下拉菜单 -->
      <div v-if="showSearchSuggestions && filteredSuggestions.length > 0" class="search-suggestions">
        <div 
          v-for="suggestion in filteredSuggestions" 
          :key="suggestion.vehicleId"
          class="suggestion-item"
          @click="selectSuggestion(suggestion)"
        >
          <div class="suggestion-icon icon-car"></div>
          <div class="suggestion-text">{{ suggestion.vehicleId }}</div>
        </div>
      </div>
    </div>
    
    <!-- 搜索结果面板 (当有搜索结果时覆盖左侧面板) -->
    <div v-if="showSearchResults" class="search-results-panel">
      <!-- 车辆列表模式 -->
      <div v-if="isInListMode" class="vehicle-content-wrapper vehicle-list-mode">
        <div class="vehicle-list-header">
          <div class="list-title">
            <i class="list-title-icon">🚗</i> 车辆列表
          </div>
          <div class="list-actions">
            <button @click="refreshVehicleList" class="refresh-list-btn" title="刷新">
              <i class="refresh-icon">↻</i>
            </button>
            <button @click="exitListMode" class="exit-list-btn" title="关闭">
              <i class="exit-icon">✕</i>
            </button>
          </div>
        </div>
        
        <div class="vehicle-list-container">
          <div class="vehicle-table-header">
            <div class="header-cell id-cell">车辆ID</div>
            <div class="header-cell speed-cell">当前速度</div>
            <div class="header-cell time-cell">最后更新时间</div>
            <div class="header-cell action-cell">操作</div>
          </div>
          
          <div class="vehicle-table-body">
            <div 
              v-for="vehicle in vehicleList" 
              :key="vehicle.vehicleId"
              class="vehicle-row"
              @click="selectVehicleFromList(vehicle)"
            >
              <div class="vehicle-cell id-cell">
                <div class="vehicle-row-icon icon-car"></div>
                <div class="vehicle-id">{{ vehicle.vehicleId }}</div>
              </div>
              <div class="vehicle-cell speed-cell">
                {{ vehicle.velocityKMH !== undefined ? vehicle.velocityKMH.toFixed(2) + ' km/h' : '未知' }}
              </div>
              <div class="vehicle-cell time-cell">
                {{ formatTimeForDisplay(vehicle.latestTime || vehicle.timestampGNSS || vehicle.formatted_time) }}
              </div>
              <div class="vehicle-cell action-cell">
                <button @click.stop="selectVehicleFromList(vehicle)" class="row-action-btn details-action">
                  <i class="action-icon">📋</i>详细信息
                </button>
              </div>
            </div>
            
            <!-- 没有数据时显示 -->
            <div v-if="vehicleList.length === 0" class="no-vehicle-data">
              <div class="no-data-icon">🔍</div>
              <div class="no-data-text">暂无车辆数据</div>
            </div>
          </div>
        </div>
        
        <!-- 分页控件 -->
        <div class="pagination-container">
          <div class="pagination-info">
            共 {{ total }} 条，当前 {{ pageNum }}/{{ Math.ceil(total / pageSize) }} 页
          </div>
          <div class="pagination-controls">
            <button 
              @click="changePage(1)" 
              class="page-btn" 
              :disabled="pageNum === 1"
              title="首页"
            >
              «
            </button>
            <button 
              @click="changePage(pageNum - 1)" 
              class="page-btn" 
              :disabled="pageNum === 1"
              title="上一页"
            >
              ‹
            </button>
            
            <!-- 页码按钮 -->
            <div class="page-numbers">
              <button 
                v-for="page in displayedPages" 
                :key="page"
                @click="changePage(page)" 
                class="page-btn"
                :class="{'active-page': page === pageNum}"
              >
                {{ page }}
              </button>
            </div>
            
            <button 
              @click="changePage(pageNum + 1)" 
              class="page-btn" 
              :disabled="pageNum >= Math.ceil(total / pageSize)"
              title="下一页"
            >
              ›
            </button>
            <button 
              @click="changePage(Math.ceil(total / pageSize))" 
              class="page-btn" 
              :disabled="pageNum >= Math.ceil(total / pageSize)"
              title="末页"
            >
              »
            </button>
          </div>
        </div>
      </div>
      
      <!-- 车辆定位模式 -->
      <div v-else-if="isInLocateMode" class="vehicle-content-wrapper vehicle-vertical-layout">
        <div class="locate-mode-content">
          <div class="located-vehicle-info">
            <div class="located-header">
              <div class="vehicle-icon icon-car"></div>
              <div class="vehicle-info-main">
                <div class="plate-number">{{ locatedVehicle.vehicleId }}</div>
                <div class="vehicle-status status-normal">
                  正常 · 更新时间: {{ formatTime(locatedVehicle.latestTime) }}
                </div>
              </div>
            </div>
            
            <div class="located-position">
              <i class="position-icon">📍</i>
              <span class="position-text">经度: {{ parseFloat(locatedVehicle.longitude || 0).toFixed(6) }}, 纬度: {{ parseFloat(locatedVehicle.latitude || 0).toFixed(6) }}</span>
            </div>
            
            <div class="info-segment">
              <div class="info-segment-title">行驶信息</div>
              <div class="info-segment-content">
                <div class="info-row">
                  <span class="info-label">轨迹点数:</span>
                  <span class="info-value">{{ locatedVehicle.count || trackPoints.length || '未知' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">方向角:</span>
                  <span class="info-value">{{ locatedVehicle.heading ? parseFloat(locatedVehicle.heading).toFixed(1) + '°' : '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">海拔高度:</span>
                  <span class="info-value">{{ locatedVehicle.elevation ? parseFloat(locatedVehicle.elevation).toFixed(1) + ' m' : '-' }}</span>
                </div>
              </div>
            </div>
            
            <div class="locate-actions">
              <button 
                @click="isLocated ? exitVehicleLocation() : centerOnVehicle()" 
                :class="['action-btn', isLocated ? 'exit-btn' : 'center-btn']"
              >
                <i class="action-icon">{{ isLocated ? '✕' : '🎯' }}</i> 
                {{ isLocated ? '退出定位' : '立即定位' }}
              </button>
              <button 
                @click="isTrackShown ? clearVehicleTrajectory() : showVehicleTrajectory()" 
                :class="['action-btn', isTrackShown ? 'clear-btn' : 'track-btn']"
              >
                <i class="action-icon">{{ isTrackShown ? '✕' : '📊' }}</i> 
                {{ isTrackShown ? '清除轨迹' : '轨迹查询' }}
              </button>
              <button @click="exitLocateMode" class="action-btn exit-btn">
                <i class="action-icon">✕</i> 退出
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 车辆搜索结果 -->
      <div v-else-if="vehicleData" class="vehicle-content-wrapper vehicle-vertical-layout">
        <div class="vehicle-result-card">
          <div class="vehicle-card-header">
            <div class="vehicle-item-icon icon-car"></div>
            <div class="vehicle-item-info">
              <div class="vehicle-item-plate">{{ vehicleData.vehicleId }}</div>
              <div class="vehicle-item-detail">更新时间: {{ formatTimeForDisplay(vehicleData.latestTime) }}</div>
            </div>
            <div class="vehicle-item-status status-normal">
              正常
            </div>
          </div>
          
          <div class="vehicle-card-body">
            <div class="vehicle-info-section">
              <div class="vehicle-info-row">
                <span class="info-label">车辆ID:</span>
                <span class="info-value">{{ vehicleData.vehicleId }}</span>
              </div>
              <div class="vehicle-info-row">
                <span class="info-label">更新时间:</span>
                <span class="info-value">{{ formatTime(vehicleData.latestTime) }}</span>
              </div>
            </div>
            
            <div class="info-segment">
              <div class="info-segment-title">位置数据</div>
              <div class="info-segment-content">
                <div class="info-row">
                  <span class="info-label">经度:</span>
                  <span class="info-value">{{ vehicleData.longitude ? parseFloat(vehicleData.longitude).toFixed(6) : '未知' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">纬度:</span>
                  <span class="info-value">{{ vehicleData.latitude ? parseFloat(vehicleData.latitude).toFixed(6) : '未知' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">轨迹点数:</span>
                  <span class="info-value">{{ vehicleData.count || (trackPoints && trackPoints.length) || '未知' }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="vehicle-card-footer">
            <button @click="startLocateMode(vehicleData)" class="vehicle-action-btn locate-btn">
              <i class="action-icon">📍</i> 定位追踪
            </button>
            <button @click="showVehicleTrajectoryFromResult(vehicleData)" class="vehicle-action-btn history-btn">
              <i class="action-icon">📊</i> 轨迹查询
            </button>
            <button @click="clearVehicleSearch" class="vehicle-action-btn clear-btn">
              <i class="clear-icon">✕</i> 清除搜索
            </button>
          </div>
        </div>
      </div>
      
      <!-- 新增：显示搜索不到结果的提示 -->
      <div v-else-if="showNoVehicleFound" class="vehicle-content-wrapper">
        <div class="no-result-wrapper">
          <div class="no-result-content">
            <div class="no-result-icon">❓</div>
            <div class="no-result-title">未找到车辆</div>
            <div class="no-result-message">没有找到ID为 "{{ lastSearchedVehicleId }}" 的车辆信息</div>
            <div class="no-result-suggestion">请检查车辆ID是否正确，或尝试其他ID</div>
          </div>
        </div>
      </div>
      
      <!-- 车辆搜索历史和示例 -->
      <div v-else class="vehicle-content-wrapper empty-state">
        <div class="vehicle-history-section" v-if="recentVehicleSearches.length > 0">
          <div class="recent-title">
            <i class="recent-title-icon">🕒</i> 最近搜索记录
            <button class="clear-history-btn" @click="clearVehicleHistory" title="清空记录">
              <i class="clear-icon">🗑️</i>
            </button>
          </div>
          <div class="recent-list">
            <div 
              v-for="(item, index) in recentVehicleSearches" 
              :key="index"
              class="recent-item"
              @click="selectRecentVehicleSearch(item)"
            >
              <div class="recent-icon icon-car"></div>
              <div class="recent-text">{{ item.vehicleId }}</div>
              <div class="recent-time">{{ formatRelativeTime(item.timestamp) }}</div>
            </div>
          </div>
        </div>
        
        <div class="vehicle-example-section">
          <div class="empty-search">
            <div class="empty-icon">🚗</div>
            <div class="empty-text">请输入车辆ID进行查询</div>
            <div class="list-all-vehicles">
              <button @click="showAllVehicles" class="list-all-btn">
                <i class="list-all-icon">📋</i> 查看所有车辆
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import axios from 'axios'

export default {
  name: 'VehicleSearch',
  emits: ['search-active', 'search-cleared'],
  setup(props, { emit }) {
    const searchInput = ref(null);
    const vehicleId = ref('');
    const vehicleData = ref(null);
    const trackPoints = ref([]); // 存储车辆轨迹点数据
    const recentVehicleSearches = ref([]);
    const isInLocateMode = ref(false);
    const locatedVehicle = ref(null);
    // 用于处理未找到车辆的情况
    const showNoVehicleFound = ref(false);
    const lastSearchedVehicleId = ref('');
    // 搜索建议相关
    const showSearchSuggestions = ref(false);
    const filteredSuggestions = ref([]);
    
    // 新增: 定位和轨迹状态
    const isLocated = ref(false);
    const isTrackShown = ref(false);
    
    // 新增: 车辆列表模式相关
    const isInListMode = ref(false);
    const vehicleList = ref([]);
    const total = ref(0);
    const pageNum = ref(1);
    const pageSize = ref(15); // 每页15条
    const loadingList = ref(false);
    
    // 计算属性：是否显示搜索结果（覆盖左侧面板）
    const showSearchResults = computed(() => {
      return vehicleData.value || isInLocateMode.value || showNoVehicleFound.value || isInListMode.value || (vehicleId.value && vehicleId.value.length > 0);
    });
    
    // 计算显示的页码按钮
    const displayedPages = computed(() => {
      const totalPages = Math.ceil(total.value / pageSize.value);
      const maxVisiblePages = 5; // 最多显示5个页码按钮
      
      if (totalPages <= maxVisiblePages) {
        // 总页数小于或等于最大显示数，显示所有页码
        return Array.from({ length: totalPages }, (_, i) => i + 1);
      }
      
      // 总页数大于最大显示数，需要部分显示
      let startPage = Math.max(1, pageNum.value - Math.floor(maxVisiblePages / 2));
      let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
      
      // 如果结束页到最后，调整开始页
      if (endPage === totalPages) {
        startPage = Math.max(1, endPage - maxVisiblePages + 1);
      }
      
      return Array.from({ length: endPage - startPage + 1 }, (_, i) => i + startPage);
    });
    
    // 当搜索结果状态变化时，通知父组件
    watch(showSearchResults, (newValue) => {
      if (newValue) {
        emit('search-active');
      } else {
        emit('search-cleared');
      }
    });
    
    // 搜索建议过滤
    function handleSearchInput() {
      if (vehicleId.value.length > 0) {
        // 从后端获取匹配的车辆ID
        axios.get('/menu/vehicle/list', {
          params: {
            vehicleId: vehicleId.value
          }
        }).then(response => {
          if (response.data && response.data.status === 'success' && response.data.rows) {
            filteredSuggestions.value = response.data.rows.slice(0, 3); // 最多显示3个建议
            showSearchSuggestions.value = filteredSuggestions.value.length > 0;
          } else {
            showSearchSuggestions.value = false;
            filteredSuggestions.value = [];
          }
        }).catch(error => {
          console.error('获取搜索建议失败:', error);
          showSearchSuggestions.value = false;
          filteredSuggestions.value = [];
        });
      } else {
        showSearchSuggestions.value = false;
        filteredSuggestions.value = [];
      }
    }
    
    // 选择搜索建议
    function selectSuggestion(vehicle) {
      vehicleId.value = vehicle.vehicleId;
      showSearchSuggestions.value = false;
      searchVehicle();
    }
    
    // 清空车辆搜索
    function clearVehicleSearch() {
      vehicleData.value = null;
      trackPoints.value = []; // 清空轨迹点数据
      showNoVehicleFound.value = false; // 重置未找到车辆的状态
      vehicleId.value = ''; // 清空搜索框
      
      if (isInLocateMode.value) {
        exitLocateMode();
      }
      
      if (isInListMode.value) {
        exitListMode();
      }
      
      // 只有在搜索之前是激活状态时才发送清除事件
      if (wasSearchActive) {
        // 确保通知父组件搜索已清除，指明来源是车辆搜索
        emit('search-cleared', { source: 'vehicle-search' });
        
        使用setTimeout确保DOM更新后再触发刷新左侧面板
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('refresh-left-panel', {
            detail: { source: 'vehicle-search' }
          }));
        }, 100);
      }
    }

    
    // 选择最近搜索记录
    function selectRecentVehicleSearch(vehicle) {
      vehicleId.value = vehicle.vehicleId;
      searchVehicle();
    }
    
    // 添加到最近搜索
    function addToRecentVehicleSearches(vehicle) {
      const exists = recentVehicleSearches.value.findIndex(item => item.vehicleId === vehicle.vehicleId);
      if (exists !== -1) {
        recentVehicleSearches.value.splice(exists, 1);
      }
      
      recentVehicleSearches.value.unshift({
        vehicleId: vehicle.vehicleId,
        timestamp: Date.now()
      });
      
      if (recentVehicleSearches.value.length > 5) {
        recentVehicleSearches.value = recentVehicleSearches.value.slice(0, 5);
      }
      
      localStorage.setItem('recentVehicleSearches', JSON.stringify(recentVehicleSearches.value));
    }
    
    // 格式化时间戳
    function formatTime(timestamp) {
      if (!timestamp) return '-';
      
      // 如果是ISO格式时间字符串
      if (typeof timestamp === 'string' && timestamp.includes('T')) {
        const date = new Date(timestamp);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      }
      
      // 如果是时间戳
      const date = new Date(parseInt(timestamp));
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
    
    // 格式化时间显示 - 用于列表中的简短时间显示
    function formatTimeForDisplay(timestamp) {
      if (!timestamp) return '-';
      
      // 如果是ISO格式时间字符串
      if (typeof timestamp === 'string' && timestamp.includes('T')) {
        const date = new Date(timestamp);
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${hours}:${minutes}:${seconds}`;
      }
      
      // 如果是时间戳
      const date = new Date(parseInt(timestamp));
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      return `${hours}:${minutes}:${seconds}`;
    }
    
    // 格式化相对时间
    function formatRelativeTime(timestamp) {
      if (!timestamp) return '';
      
      const now = Date.now();
      const diff = now - timestamp;
      
      if (diff < 60 * 1000) return '刚刚';
      if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`;
      if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`;
      if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`;
      
      const date = new Date(timestamp);
      return `${date.getMonth() + 1}月${date.getDate()}日`;
    }
    
    // 从列表中选择车辆，使用车辆详情API
    async function selectVehicleFromList(vehicle) {
      // 退出列表模式
      isInListMode.value = false;
      
      // 重置状态
      vehicleData.value = null;
      trackPoints.value = [];
      showNoVehicleFound.value = false;
      
      console.log('正在请求车辆详情:', vehicle.vehicleId);
      
      try {
        // 使用车辆ID调用详情API
        const response = await axios.get('/menu/vehicle', {
          params: {
            vehicleId: vehicle.vehicleId
          }
        });
        
        console.log('获取车辆详情返回结果:', response.data);
        
        if (response.data.status === 'success') {
          if (response.data.vehicle) {
            console.log('API返回了车辆数据');
            
            // 处理vehicle可能是数组的情况
            const vehicleInfo = Array.isArray(response.data.vehicle) 
              ? response.data.vehicle[0] 
              : response.data.vehicle;
              
            // 设置车辆数据
            vehicleData.value = vehicleInfo;
            
            // 设置轨迹点
            if (response.data.trackPoints && response.data.trackPoints.length > 0) {
              trackPoints.value = response.data.trackPoints;
              console.log('获取到轨迹点:', trackPoints.value.length);
            } else {
              console.log('没有轨迹点数据');
              trackPoints.value = [];
            }
            
            // 添加到最近搜索
            addToRecentVehicleSearches({ vehicleId: vehicleData.value.vehicleId });
          } else {
            console.warn('API返回成功但没有车辆数据，尝试使用列表中的数据');
            // 如果API返回成功但没有车辆数据，使用列表中的车辆数据
            vehicleData.value = {
              vehicleId: vehicle.vehicleId,
              latestTime: vehicle.latestTime || vehicle.timestampGNSS || vehicle.formatted_time
            };
            
            // 添加到最近搜索
            addToRecentVehicleSearches({ vehicleId: vehicle.vehicleId });
          }
        } else {
          // API返回失败状态时，也使用列表中的数据
          console.warn('API返回失败状态，使用列表中的数据');
          vehicleData.value = {
            vehicleId: vehicle.vehicleId,
            latestTime: vehicle.latestTime || vehicle.timestampGNSS || vehicle.formatted_time
          };
          
          // 添加到最近搜索
          addToRecentVehicleSearches({ vehicleId: vehicle.vehicleId });
        }
      } catch (error) {
        console.error('获取车辆详情失败:', error);
        
        // 出错时也使用列表中的数据
        vehicleData.value = {
          vehicleId: vehicle.vehicleId,
          latestTime: vehicle.latestTime || vehicle.timestampGNSS || vehicle.formatted_time
        };
        
        // 添加到最近搜索
        addToRecentVehicleSearches({ vehicleId: vehicle.vehicleId });
      }
    }
    
    // 查询车辆 - 使用API接口
    async function searchVehicle() {
      if (!vehicleId.value) {
        return;
      }
      
      if (isInLocateMode.value) {
        exitLocateMode();
      }
      
      if (isInListMode.value) {
        exitListMode();
      }
      
      // 重置状态
      vehicleData.value = null;
      trackPoints.value = [];
      showNoVehicleFound.value = false;
      lastSearchedVehicleId.value = vehicleId.value;
      showSearchSuggestions.value = false;
      
      emit('search-active');
      
      console.log('正在搜索车辆:', vehicleId.value);
      
      try {
        // 调用API接口
        const response = await axios.get('/menu/vehicle', {
          params: {
            vehicleId: vehicleId.value
          }
        });
        
        console.log('搜索车辆返回结果:', response.data);
        
        if (response.data.status === 'success') {
          if (response.data.vehicle) {
            console.log('API返回了车辆数据:', response.data.vehicle);
            
            // 处理vehicle可能是数组的情况
            const vehicleInfo = Array.isArray(response.data.vehicle) 
              ? response.data.vehicle[0] 
              : response.data.vehicle;
              
            console.log('处理后的车辆数据:', vehicleInfo);
            
            // 设置车辆数据
            vehicleData.value = vehicleInfo;
            
            // 设置轨迹点
            if (response.data.trackPoints && response.data.trackPoints.length > 0) {
              trackPoints.value = response.data.trackPoints;
              console.log('获取到轨迹点:', trackPoints.value.length);
            } else {
              console.log('没有轨迹点数据');
              trackPoints.value = [];
            }
            
            // 添加到最近搜索
            addToRecentVehicleSearches({ vehicleId: vehicleData.value.vehicleId });
          } else {
            // 尝试从列表API获取基本信息
            console.log('API没有返回车辆数据，尝试从列表API获取');
            
            try {
              const listResponse = await axios.get('/menu/vehicle/list', {
                params: {
                  vehicleId: vehicleId.value
                }
              });
              
              if (listResponse.data.status === 'success' && listResponse.data.rows && listResponse.data.rows.length > 0) {
                // 使用列表API返回的车辆信息
                const listVehicle = listResponse.data.rows[0];
                vehicleData.value = {
                  vehicleId: listVehicle.vehicleId,
                  latestTime: listVehicle.latestTime || listVehicle.timestampGNSS || listVehicle.formatted_time
                };
                
                // 添加到最近搜索
                addToRecentVehicleSearches({ vehicleId: vehicleData.value.vehicleId });
              } else {
                // 未找到车辆
                showNoVehicleFound.value = true;
                console.log('未找到车辆:', vehicleId.value);
              }
            } catch (listError) {
              console.error('列表API调用失败:', listError);
              showNoVehicleFound.value = true;
            }
          }
        } else {
          // API返回失败状态
          console.warn('API查询返回失败状态');
          
          // 尝试从列表API获取
          try {
            console.log('尝试从列表API获取车辆信息');
            const listResponse = await axios.get('/menu/vehicle/list', {
              params: {
                vehicleId: vehicleId.value
              }
            });
            
            if (listResponse.data.status === 'success' && listResponse.data.rows && listResponse.data.rows.length > 0) {
              // 使用列表API返回的车辆信息
              const listVehicle = listResponse.data.rows[0];
              vehicleData.value = {
                vehicleId: listVehicle.vehicleId,
                latestTime: listVehicle.latestTime || listVehicle.timestampGNSS || listVehicle.formatted_time
              };
              
              // 添加到最近搜索
              addToRecentVehicleSearches({ vehicleId: vehicleData.value.vehicleId });
            } else {
              showNoVehicleFound.value = true;
            }
          } catch (listError) {
            console.error('列表API调用也失败:', listError);
            showNoVehicleFound.value = true;
          }
        }
      } catch (error) {
        console.error('查询车辆失败:', error);
        
        // 尝试从列表API获取
        try {
          console.log('尝试从列表API获取车辆信息');
          const listResponse = await axios.get('/menu/vehicle/list', {
            params: {
              vehicleId: vehicleId.value
            }
          });
          
          if (listResponse.data.status === 'success' && listResponse.data.rows && listResponse.data.rows.length > 0) {
            // 使用列表API返回的车辆信息
            const listVehicle = listResponse.data.rows[0];
            vehicleData.value = {
              vehicleId: listVehicle.vehicleId,
              latestTime: listVehicle.latestTime || listVehicle.timestampGNSS || listVehicle.formatted_time
            };
            
            // 添加到最近搜索
            addToRecentVehicleSearches({ vehicleId: vehicleData.value.vehicleId });
          } else {
            showNoVehicleFound.value = true;
          }
        } catch (listError) {
          console.error('列表API调用也失败:', listError);
          showNoVehicleFound.value = true;
        }
      }
    }
    
    // 进入定位模式
    function startLocateMode(vehicle) {
      // 退出列表模式
      if (isInListMode.value) {
        isInListMode.value = false;
      }
      
      // 重置定位和轨迹状态（保留当前的轨迹显示状态）
      isLocated.value = false;
      
      // 设置要定位的车辆
      locatedVehicle.value = vehicle;
      
      console.log('进入定位模式, 车辆数据:', locatedVehicle.value);
      isInLocateMode.value = true;
      
      // 添加到最近搜索
      addToRecentVehicleSearches({ vehicleId: locatedVehicle.value.vehicleId });
    }
    
    // 退出定位模式
    function exitLocateMode() {
      if (isInLocateMode.value) {
        // 清除定位和轨迹状态
        if (isLocated.value) {
          exitVehicleLocation();
        }
        
        if (isTrackShown.value) {
          clearVehicleTrajectory();
        }
        
        isInLocateMode.value = false;
        locatedVehicle.value = null;
      }
    }
    
    // 清除车辆定位
    function exitVehicleLocation() {
      console.log('清除车辆定位');
      // 使用车辆特定的清除事件
      window.dispatchEvent(new CustomEvent('clear-vehicle-located', {
        detail: { source: 'vehicle-search' }
      }));
      isLocated.value = false;
    }
    
    // 清除车辆轨迹 - 修改为使用正确的事件
      function clearVehicleTrajectory() {
        if (isTrackShown.value) {
          console.log('清除车辆轨迹');
          // 直接触发clear-trajectory事件
          window.dispatchEvent(new CustomEvent('clear-trajectory'));
          isTrackShown.value = false;
        }
      }
    
    // 在地图上居中定位车辆
    function centerOnVehicle() {
      if (!locatedVehicle.value) {
        console.error('没有要定位的车辆数据');
        return;
      }
      
      console.log('准备定位车辆:', locatedVehicle.value);
      
      // 检查是否有经纬度数据
      if (!locatedVehicle.value.longitude || !locatedVehicle.value.latitude) {
        console.error('车辆缺少经纬度信息，无法定位');
        
        // 尝试从API重新获取车辆位置信息
        axios.get('/menu/vehicle', {
          params: {
            vehicleId: locatedVehicle.value.vehicleId
          }
        }).then(response => {
          if (response.data.status === 'success' && response.data.vehicle) {
            // 处理vehicle可能是数组的情况
            const vehicleInfo = Array.isArray(response.data.vehicle) 
              ? response.data.vehicle[0] 
              : response.data.vehicle;
              
            // 更新车辆信息
            locatedVehicle.value = {
              ...locatedVehicle.value,
              ...vehicleInfo
            };
            
            if (locatedVehicle.value.longitude && locatedVehicle.value.latitude) {
              console.log('已获取到经纬度信息，执行定位');
              executeVehicleLocation();
            } else {
              console.error('无法获取到车辆经纬度信息');
            }
          } else {
            console.error('获取车辆详情失败');
          }
        }).catch(error => {
          console.error('API调用失败:', error);
        });
        
        return;
      }
      
      // 执行定位
      executeVehicleLocation();
    }
    
    // 执行车辆定位
    function executeVehicleLocation() {
      // 确保经纬度是数值
      const longitude = parseFloat(locatedVehicle.value.longitude);
      const latitude = parseFloat(locatedVehicle.value.latitude);
      
      // 检查坐标是否有效
      if (isNaN(longitude) || isNaN(latitude)) {
        console.error('无效的车辆坐标:', longitude, latitude);
        return;
      }
      
      console.log('触发车辆定位事件, 坐标:', [longitude, latitude]);
      
      // 触发定位事件，使用美化后的定位标记
      window.dispatchEvent(new CustomEvent('locate-vehicle', {
        detail: {
          source: 'vehicle-search', // 明确标识来源
          id: locatedVehicle.value.vehicleId,
          coordinates: [longitude, latitude],
          title: locatedVehicle.value.vehicleId,
          location: '当前位置',
          status: '正常',
          latestTime: locatedVehicle.value.latestTime,
          useEnhancedMarker: true // 标记使用美化标识
        }
      }));
      
      // 更新定位状态
      isLocated.value = true;
    }
    
    // 显示车辆轨迹
    function showVehicleTrajectory() {
      if (locatedVehicle.value) {
        console.log('显示车辆轨迹, 轨迹点数:', trackPoints.value.length);
        
        if (trackPoints.value && trackPoints.value.length >= 2) {
          // 使用API返回的轨迹点数据
          const points = trackPoints.value.map(point => [
            parseFloat(point.longitude),
            parseFloat(point.latitude)
          ]);
          
          // 获取起点和终点
          const startPoint = points[0];
          const endPoint = points[points.length - 1];
          
          console.log('使用轨迹点, 起点:', startPoint, '终点:', endPoint);
          
          // 触发显示轨迹事件，使用美化的起点和终点标记
          window.dispatchEvent(new CustomEvent('show-trajectory', {
            detail: {
              vehicleId: locatedVehicle.value.vehicleId,
              startPoint: startPoint,
              endPoint: endPoint,
              points: points, // 传递完整轨迹点
              useEnhancedMarkers: true, // 使用美化的起点终点标记
              startTime: trackPoints.value[0].timestampGNSS || trackPoints.value[0].latestTime,
              endTime: trackPoints.value[trackPoints.value.length - 1].timestampGNSS || 
                       trackPoints.value[trackPoints.value.length - 1].latestTime
            }
          }));
          
          // 更新轨迹显示状态
          isTrackShown.value = true;
        } else {
          // 尝试再次获取车辆轨迹
          console.log('没有足够的轨迹点，尝试获取轨迹');
          
          axios.get('/menu/vehicle', {
            params: {
              vehicleId: locatedVehicle.value.vehicleId
            }
          }).then(response => {
            if (response.data.status === 'success' && response.data.trackPoints && response.data.trackPoints.length >= 2) {
              // 更新轨迹点
              trackPoints.value = response.data.trackPoints;
              
              // 使用API返回的轨迹点数据
              const points = trackPoints.value.map(point => [
                parseFloat(point.longitude),
                parseFloat(point.latitude)
              ]);
              
              // 获取起点和终点
              const startPoint = points[0];
              const endPoint = points[points.length - 1];
              
              console.log('重新获取轨迹点成功, 起点:', startPoint, '终点:', endPoint);
              
              // 触发显示轨迹事件，使用美化的起点和终点标记
              window.dispatchEvent(new CustomEvent('show-trajectory', {
                detail: {
                  vehicleId: locatedVehicle.value.vehicleId,
                  startPoint: startPoint,
                  endPoint: endPoint,
                  points: points, // 传递完整轨迹点
                  useEnhancedMarkers: true, // 使用美化的起点终点标记
                  startTime: trackPoints.value[0].timestampGNSS || trackPoints.value[0].latestTime,
                  endTime: trackPoints.value[trackPoints.value.length - 1].timestampGNSS || 
                           trackPoints.value[trackPoints.value.length - 1].latestTime
                }
              }));
              
              // 更新轨迹显示状态
              isTrackShown.value = true;
            } else {
              console.error('无法获取足够的轨迹点，轨迹无法显示');
            }
          }).catch(error => {
            console.error('获取轨迹失败:', error);
          });
        }
      }
    }
    
    // 从搜索结果显示车辆轨迹
    function showVehicleTrajectoryFromResult(vehicle) {
      // 先进入定位模式
      startLocateMode(vehicle);
      
      // 然后显示轨迹
      setTimeout(() => {
        showVehicleTrajectory();
      }, 300);
    }
    
    // 清空历史记录
    function clearVehicleHistory() {
      recentVehicleSearches.value = [];
      localStorage.removeItem('recentVehicleSearches');
    }
    
    // 显示所有车辆列表
    function showAllVehicles() {
      if (isInLocateMode.value) {
        exitLocateMode();
      }
      
      vehicleData.value = null;
      trackPoints.value = [];
      showNoVehicleFound.value = false;
      
      isInListMode.value = true;
      pageNum.value = 1; // 重置为第一页
      
      fetchVehicleList();
      
      emit('search-active'); // 通知父组件搜索已激活
    }
    
    // 退出列表模式
    function exitListMode() {
      isInListMode.value = false;
      vehicleList.value = [];
    }
    
    // 获取车辆列表数据
    async function fetchVehicleList() {
      if (loadingList.value) return;
      loadingList.value = true;
      
      try {
        console.log('获取车辆列表, 页码:', pageNum.value, '每页条数:', pageSize.value);
        
        // 调用API接口
        const response = await axios.get('/menu/vehicle/list', {
          params: {
            pageNum: pageNum.value,
            pageSize: pageSize.value
          }
        });
        
        console.log('获取车辆列表返回结果:', response.data);
        
        if (response.data && response.data.status === 'success') {
          // 使用返回的数据更新列表和总数
          total.value = response.data.total || 0;
          vehicleList.value = response.data.rows || [];
        } else {
          console.error('获取车辆列表失败:', response.data);
          vehicleList.value = [];
          total.value = 0;
        }
      } catch (error) {
        console.error('获取车辆列表API调用失败:', error);
        vehicleList.value = [];
        total.value = 0;
      } finally {
        loadingList.value = false;
      }
    }
    
    // 刷新车辆列表
    function refreshVehicleList() {
      fetchVehicleList();
    }
    
    // 切换页码
    function changePage(newPage) {
      if (newPage < 1 || newPage > Math.ceil(total.value / pageSize.value) || newPage === pageNum.value) {
        return;
      }
      
      pageNum.value = newPage;
      fetchVehicleList();
    }
    
    // 生命周期钩子
    onMounted(() => {
      // 从本地存储加载最近搜索
      const savedVehicleSearches = localStorage.getItem('recentVehicleSearches');
      if (savedVehicleSearches) {
        try {
          recentVehicleSearches.value = JSON.parse(savedVehicleSearches);
        } catch (e) {
          console.error('Failed to parse recent vehicle searches', e);
        }
      }
      
      // 点击页面其他地方时隐藏搜索建议
      const handleDocumentClick = (event) => {
        if (searchInput.value && !searchInput.value.contains(event.target)) {
          showSearchSuggestions.value = false;
        }
      };
      
      document.addEventListener('click', handleDocumentClick);
      
      // 确保清理事件监听器
      onUnmounted(() => {
        document.removeEventListener('click', handleDocumentClick);
      });
    });
    
    return {
      searchInput,
      vehicleId,
      vehicleData,
      trackPoints,
      recentVehicleSearches,
      isInLocateMode,
      locatedVehicle,
      showNoVehicleFound,
      lastSearchedVehicleId,
      showSearchSuggestions,
      filteredSuggestions,
      showSearchResults,
      isLocated,
      isTrackShown,
      
      // 列表模式相关
      isInListMode,
      vehicleList,
      total,
      pageNum,
      pageSize,
      displayedPages,
      
      formatTime,
      formatTimeForDisplay,
      formatRelativeTime,
      searchVehicle,
      selectRecentVehicleSearch,
      startLocateMode,
      exitLocateMode,
      centerOnVehicle,
      exitVehicleLocation,
      showVehicleTrajectory,
      clearVehicleTrajectory,
      showVehicleTrajectoryFromResult,
      handleSearchInput,
      selectSuggestion,
      clearVehicleHistory,
      clearVehicleSearch,
      
      // 列表相关方法
      showAllVehicles,
      exitListMode,
      fetchVehicleList,
      refreshVehicleList,
      changePage,
      selectVehicleFromList
    };
  }
}
</script>

<style scoped>
.vehicle-search-container {
  width: 100%;
  position: relative;
  z-index: 20;
}

.search-section {
  position: relative;
  margin-bottom: 6px;
}

.search-form {
  display: flex;
  width: 100%;
}

.search-input {
  flex: 1;
  background-color: rgba(4, 34, 53, 0.6);
  border: 1px solid rgba(14, 89, 134, 0.6);
  color: white;
  padding: 4px 8px;
  border-radius: 3px 0 0 3px;
  outline: none;
  font-size: 12px;
  height: 30px;
}

.search-button {
  width: 32px;
  background-color: rgba(0, 145, 234, 0.6);
  border: none;
  color: white;
  padding: 4px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 30px;
}

.list-button {
  width: 32px;
  background-color: rgba(0, 210, 91, 0.6);
  border: none;
  color: white;
  padding: 4px;
  border-radius: 0 3px 3px 0;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 30px;
}

.search-button {
  border-radius: 0;
}

.search-button:hover {
  background-color: rgba(0, 145, 234, 0.8);
}

.list-button:hover {
  background-color: rgba(0, 210, 91, 0.8);
}

/* 搜索建议下拉菜单 */
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background-color: rgba(4, 34, 53, 0.95);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 0 0 3px 3px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  z-index: 25;
  margin-top: -1px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover {
  background-color: rgba(0, 145, 234, 0.2);
}

.suggestion-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 6px;
  font-size: 12px;
}

.suggestion-text {
  font-size: 12px;
  color: white;
}

/* 搜索结果面板样式 */
.search-results-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  width: 100%;
  background-color: rgba(4, 34, 53, 0.95);
  border: 1px solid rgba(14, 89, 134, 0.8);
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
  z-index: 20;
  height: 64vh;
  overflow-y: auto;
  padding: 8px;
}

/* 以下是从BottomDataPanel复制过来的样式 */
.vehicle-content-wrapper {
  display: flex;
  flex-direction: row;
  height: 220px;
  gap: 6px;
  overflow: hidden;
}

/* 新增：垂直布局样式 */
.vehicle-content-wrapper.vehicle-vertical-layout {
  display: flex;
  flex-direction: column;
  height: auto;
  max-height: 450px;
  gap: 8px;
  overflow-y: auto;
}

.vehicle-content-wrapper.empty-state {
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.3);
  border-radius: 3px;
}

.vehicle-result-section {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  padding: 6px;
}

.vehicle-details-section {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  padding: 6px;
}

.vehicle-history-section {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
}

.vehicle-example-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 定位模式内容 */
.locate-mode-content {
  flex: 1;
  overflow: auto;
}

.located-vehicle-info {
  background-color: rgba(0, 145, 234, 0.2);
  border: 1px solid rgba(0, 145, 234, 0.4);
  border-radius: 3px;
  padding: 6px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.located-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.vehicle-icon, .device-icon {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 145, 234, 0.3);
  border-radius: 50%;
  margin-right: 8px;
  font-size: 14px;
  flex-shrink: 0;
}

.vehicle-info-main {
  flex: 1;
  min-width: 0;
}

.plate-number {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vehicle-status {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 2px;
  padding: 1px 0;
}

.located-position {
  display: flex;
  align-items: center;
  padding: 4px 6px;
  background-color: rgba(4, 34, 53, 0.4);
  border-radius: 3px;
  margin-bottom: 6px;
}

.position-icon {
  font-size: 12px;
  margin-right: 6px;
  color: #00ffff;
}

.position-text {
  flex: 1;
  font-size: 12px;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 按钮样式 */
.locate-actions {
  display: flex;
  gap: 6px;
  margin-top: auto;
}

.action-btn {
  flex: 1;
  height: 28px;
  padding: 0 8px;
  font-size: 12px;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.action-icon {
  margin-right: 8px;
  font-size: 12px;
}

.center-btn {
  background-color: rgba(0, 145, 234, 0.8);
  color: white;
}

.center-btn:hover {
  background-color: rgba(0, 145, 234, 1);
}

.track-btn {
  background-color: rgba(0, 210, 91, 0.8);
  color: white;
}

.track-btn:hover {
  background-color: rgba(0, 210, 91, 1);
}

.exit-btn {
  background-color: rgba(255, 77, 79, 0.8);
  color: white;
}

.exit-btn:hover {
  background-color: rgba(255, 77, 79, 1);
}

.clear-btn {
  background-color: rgba(255, 155, 0, 0.8);
  color: white;
}

.clear-btn:hover {
  background-color: rgba(255, 155, 0, 1);
}

/* 搜索结果卡片 */
.vehicle-result-card {
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.vehicle-card-header {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  background-color: rgba(4, 34, 53, 0.5);
  min-height: 28px;
}

.vehicle-item-icon {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 145, 234, 0.3);
  border-radius: 50%;
  margin-right: 8px;
  font-size: 14px;
  flex-shrink: 0;
}

.vehicle-item-info {
  flex: 1;
  min-width: 0;
}

.vehicle-item-plate {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
  line-height: 1.3;
}

.vehicle-item-detail {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
  line-height: 1.3;
}

.vehicle-item-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  white-space: nowrap;
  margin-left: 6px;
  flex-shrink: 0;
}

.vehicle-card-body {
  padding: 6px 8px;
  border-top: 1px solid rgba(14, 89, 134, 0.3);
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 新增：垂直布局中的详细信息区域 */
.vehicle-info-section {
  border: 1px solid rgba(14, 89, 134, 0.3);
  border-radius: 3px;
  padding: 6px;
  background-color: rgba(4, 34, 53, 0.2);
}

.vehicle-info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
  line-height: 1.4;
}

.vehicle-info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  color: rgba(255, 255, 255, 0.7);
  width: 30%;
}

.info-value {
  color: #fff;
  text-align: right;
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drive-status {
  display: inline-block;
  padding: 0 4px;
  border-radius: 2px;
  background-color: rgba(0, 210, 91, 0.3);
}

/* 底部按钮行样式 */
.vehicle-card-footer {
  display: flex;
  border-top: 1px solid rgba(14, 89, 134, 0.3);
  height: 32px;
}

.vehicle-action-btn {
  flex: 1;
  border: none;
  padding: 0 8px;
  background-color: transparent;
  color: white;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.locate-btn {
  background-color: rgba(0, 145, 234, 0.2);
  border-right: 1px solid rgba(14, 89, 134, 0.3);
}

.locate-btn:hover {
  background-color: rgba(0, 145, 234, 0.4);
}

.history-btn {
  background-color: rgba(101, 78, 163, 0.2);
  border-right: 1px solid rgba(14, 89, 134, 0.3);
}

.history-btn:hover {
  background-color: rgba(101, 78, 163, 0.4);
}

.clear-btn {
  background-color: rgba(255, 77, 79, 0.2);
}

.clear-btn:hover {
  background-color: rgba(255, 77, 79, 0.4);
}

/* 最近搜索样式 */
.recent-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recent-title-icon {
  margin-right: 4px;
  font-size: 12px;
}

.clear-history-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 0;
  font-size: 12px;
  transition: color 0.2s;
}

.clear-history-btn:hover {
  color: rgba(255, 255, 255, 0.8);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-item {
  display: flex;
  align-items: center;
  background-color: rgba(4, 34, 53, 0.6);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  padding: 4px 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.recent-item:hover {
  background-color: rgba(0, 145, 234, 0.2);
  transform: translateY(-1px);
}

.recent-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 6px;
  background-color: rgba(4, 34, 53, 0.4);
  border-radius: 50%;
  flex-shrink: 0;
}

.recent-text {
  font-size: 12px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: auto;
  white-space: nowrap;
  padding-left: 4px;
}

/* 空状态提示 */
.empty-search {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.6);
  padding: 10px;
}

.empty-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 13px;
  text-align: center;
  margin-bottom: 12px;
}

.empty-examples {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  margin-bottom: 12px;
}

.example-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.example-item {
  font-size: 12px;
  color: rgba(0, 145, 234, 0.8);
  background-color: rgba(0, 145, 234, 0.1);
  padding: 4px 8px;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(0, 145, 234, 0.2);
}

.example-item:hover {
  background-color: rgba(0, 145, 234, 0.2);
  color: #fff;
  border: 1px solid rgba(0, 145, 234, 0.4);
}

/* 列出所有车辆按钮 */
.list-all-vehicles {
  margin-top: 6px;
}

.list-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 210, 91, 0.2);
  border: 1px solid rgba(0, 210, 91, 0.4);
  color: white;
  border-radius: 3px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.list-all-btn:hover {
  background-color: rgba(0, 210, 91, 0.4);
  transform: translateY(-1px);
}

.list-all-icon {
  margin-right: 6px;
}

/* 新增: 信息段落容器 */
.info-segment {
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  overflow: hidden;
  flex: 1;
}

.info-segment-title {
  font-size: 12px;
  color: #fff;
  background-color: rgba(4, 34, 53, 0.5);
  padding: 3px 6px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.4);
}

.info-segment-content {
  padding: 6px;
}

/* 新增: 清除搜索按钮 */
.clear-search-button {
  margin-top: 8px;
  background-color: rgba(255, 77, 79, 0.2);
  border: 1px solid rgba(255, 77, 79, 0.4);
  color: rgba(255, 255, 255, 0.8);
  border-radius: 3px;
  padding: 4px 0;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-search-button:hover {
  background-color: rgba(255, 77, 79, 0.4);
}

/* 新增: 未找到结果样式 */
.no-result-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.no-result-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background-color: rgba(4, 34, 53, 0.4);
  border: 1px solid rgba(14, 89, 134, 0.5);
  border-radius: 4px;
  width: 80%;
}

.no-result-icon {
  font-size: 32px;
  margin-bottom: 10px;
  color: rgba(255, 77, 79, 0.8);
}

.no-result-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #fff;
}

.no-result-message {
  font-size: 13px;
  margin-bottom: 6px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
}

.no-result-suggestion {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 16px;
  text-align: center;
}

.no-result-examples {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.example-buttons {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.example-btn {
  background-color: rgba(0, 145, 234, 0.2);
  border: 1px solid rgba(0, 145, 234, 0.4);
  color: #fff;
  border-radius: 3px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.example-btn:hover {
  background-color: rgba(0, 145, 234, 0.4);
  transform: translateY(-1px);
}

/* 图标样式 */
.icon-car::after {
  content: '🚗';
  font-size: 13px;
}

/* 状态样式 */
.status-normal {
  background-color: rgb(0, 210, 91);
  color: white;
}

/* 新增: 车辆列表模式样式 - 修改为占满整个面板 */
.vehicle-content-wrapper.vehicle-list-mode {
  display: flex;
  flex-direction: column;
  height: 100%; /* 修改：使用100%高度 */
  min-height: 500px; /* 添加：确保最小高度 */
  gap: 8px;
  overflow: hidden;
}

.vehicle-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background-color: rgba(4, 34, 53, 0.5);
  border-radius: 3px;
  border: 1px solid rgba(14, 89, 134, 0.5);
}

.list-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.list-title-icon {
  margin-right: 6px;
}

.list-actions {
  display: flex;
  gap: 8px;
}

.refresh-list-btn, .exit-list-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.refresh-list-btn {
  background-color: rgba(0, 145, 234, 0.3);
  color: white;
}

.refresh-list-btn:hover {
  background-color: rgba(0, 145, 234, 0.5);
}

.exit-list-btn {
  background-color: rgba(255, 77, 79, 0.3);
  color: white;
}

.exit-list-btn:hover {
  background-color: rgba(255, 77, 79, 0.5);
}

.vehicle-list-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
}

.vehicle-table-header {
  display: flex;
  background-color: rgba(4, 34, 53, 0.5);
  padding: 6px 8px;
  font-size: 12px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(14, 89, 134, 0.5);
}

.vehicle-table-body {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 145, 234, 0.6) rgba(4, 34, 53, 0.4);
}

.vehicle-table-body::-webkit-scrollbar {
  width: 4px;
}

.vehicle-table-body::-webkit-scrollbar-track {
  background: rgba(4, 34, 53, 0.4);
}

.vehicle-table-body::-webkit-scrollbar-thumb {
  background-color: rgba(0, 145, 234, 0.6);
  border-radius: 4px;
}

.header-cell, .vehicle-cell {
  padding: 0 8px;
}

.id-cell {
  width: 25%;
  flex-shrink: 0;
}

.speed-cell {
  width: 25%;
  flex-shrink: 0;
}

.time-cell {
  width: 35%;
  flex-shrink: 0;
}

.action-cell {
  width: 15%;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.vehicle-row {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  font-size: 12px;
  color: white;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
  transition: background-color 0.2s;
  cursor: pointer;
}

.vehicle-row:hover {
  background-color: rgba(0, 145, 234, 0.1);
}

.vehicle-row:last-child {
  border-bottom: none;
}

.vehicle-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vehicle-cell.id-cell {
  display: flex;
  align-items: center;
  font-weight: bold;
}

.vehicle-row-icon {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 145, 234, 0.2);
  border-radius: 50%;
  margin-right: 6px;
  font-size: 12px;
  flex-shrink: 0;
}

.row-action-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 3px;
  margin: 0 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.details-action {
  background-color: rgba(0, 145, 234, 0.3);
  color: white;
  width: 85px;      /* 增大宽度 */
}

.details-action:hover {
  background-color: rgba(0, 145, 234, 0.5);
}

.no-vehicle-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  color: rgba(255, 255, 255, 0.7);
}

.no-data-icon {
  font-size: 24px;
  margin-bottom: 10px;
}

.no-data-text {
  font-size: 14px;
}

/* 分页控件样式 */
.pagination-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background-color: rgba(4, 34, 53, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.pagination-info {
  white-space: nowrap;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  width: 28px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(4, 34, 53, 0.4);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.page-btn:hover:not(:disabled) {
  background-color: rgba(0, 145, 234, 0.3);
  border-color: rgba(0, 145, 234, 0.5);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.active-page {
  background-color: rgba(0, 145, 234, 0.5);
  border-color: rgba(0, 145, 234, 0.7);
  font-weight: bold;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

/* 修改搜索结果面板高度 */
.search-results-panel {
  max-height: calc(100vh - 80px); /* 增加高度以允许更多内容显示 */
}

/* 响应式调整 */
@media (max-width: 1400px) {
  .vehicle-content-wrapper {
    height: 200px;
  }
  
  .vehicle-content-wrapper.vehicle-list-mode {
    height: 100%;
    min-height: 450px;
  }
}

@media (max-width: 992px) {
  .vehicle-content-wrapper {
    flex-direction: column;
    height: auto;
    max-height: 400px;
  }
  
  .vehicle-result-section,
  .vehicle-details-section {
    width: 100%;
  }
  
  .vehicle-content-wrapper.vehicle-list-mode {
    height: 100%;
    min-height: 400px;
  }
  
  .id-cell {
    width: 30%;
  }
  
  .speed-cell {
    width: 25%;
  }
  
  .time-cell {
    width: 30%;
  }
  
  .action-cell {
    width: 15%;
  }
}
</style>