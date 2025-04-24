<template>
    <div class="device-search-container">
      <div class="search-section">
        <div class="search-form">
          <input 
            type="text" 
            v-model="rcuId" 
            placeholder="输入设备ID查询 (例如: U-WZ0012)" 
            class="search-input"
            @keyup.enter="searchDevice"
            @input="handleSearchInput"
            ref="searchInput"
          />
          <button @click="searchDevice" class="search-button" title="搜索">
            <i class="search-icon">🔍</i>
          </button>
          <button @click="showAllDevices" class="list-button" title="显示所有设备">
            <i class="list-icon">📋</i>
          </button>
        </div>
        
        <!-- 搜索建议下拉菜单 -->
        <div v-if="showSearchSuggestions && filteredSuggestions.length > 0" class="search-suggestions">
          <div 
            v-for="suggestion in filteredSuggestions" 
            :key="suggestion.rcuId"
            class="suggestion-item"
            @click="selectSuggestion(suggestion)"
          >
            <div class="suggestion-icon icon-device"></div>
            <div class="suggestion-text">{{ suggestion.rcuId }}</div>
          </div>
        </div>
      </div>
      
      <!-- 搜索结果面板 (当有搜索结果时覆盖右侧面板的内容) -->
      <div v-if="showSearchResults" class="search-results-panel">
        <!-- 设备列表模式 -->
        <div v-if="isInListMode" class="device-content-wrapper device-list-mode">
          <div class="device-list-header">
            <div class="list-title">
              <i class="list-title-icon">📡</i> 设备列表
            </div>
            <div class="list-actions">
              <button @click="refreshDeviceList" class="refresh-list-btn" title="刷新">
                <i class="refresh-icon">↻</i>
              </button>
              <button @click="exitListMode" class="exit-list-btn" title="关闭">
                <i class="exit-icon">✕</i>
              </button>
            </div>
          </div>
          
          <div class="device-list-container">
            <div class="device-table-header">
              <div class="header-cell id-cell">设备ID</div>
              <div class="header-cell type-cell">设备类型</div>
              <div class="header-cell time-cell">最后更新时间</div>
              <div class="header-cell action-cell">操作</div>
            </div>
            
            <div class="device-table-body">
              <div 
                v-for="device in deviceList" 
                :key="device.rcuId"
                class="device-row"
                @click="selectDeviceFromList(device)"
              >
                <div class="device-cell id-cell">
                  <div class="device-row-icon icon-device"></div>
                  <div class="device-id">{{ device.rcuId }}</div>
                </div>
                <div class="device-cell type-cell">
                  {{ formatDeviceType(device.deviceType) }}
                </div>
                <div class="device-cell time-cell">
                  {{ formatTimeForDisplay(device.receiveTime) }}
                </div>
                <div class="device-cell action-cell">
                  <button @click.stop="selectDeviceFromList(device)" class="row-action-btn details-action">
                    <i class="action-icon">📋</i>详细信息
                  </button>
                </div>
              </div>
              
              <!-- 没有数据时显示 -->
              <div v-if="deviceList.length === 0" class="no-device-data">
                <div class="no-data-icon">🔍</div>
                <div class="no-data-text">暂无设备数据</div>
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
        
        <!-- 设备定位模式 -->
        <div v-else-if="isInLocateMode" class="device-content-wrapper device-vertical-layout">
          <div class="locate-mode-content">
            <div class="located-device-info">
              <div class="located-header">
                <div class="device-icon icon-device"></div>
                <div class="device-info-main">
                  <div class="device-number">{{ locatedDevice.rcuId }}</div>
                  <div class="device-status" :class="getStatusClass(locatedDevice.deviceStatus)">
                    {{ locatedDevice.deviceStatus || '状态未知' }} · 更新时间: {{ formatTime(locatedDevice.receiveTime) }}
                  </div>
                </div>
              </div>
              
              <div class="located-position">
                <i class="position-icon">📍</i>
                <span class="position-text">经度: {{ parseFloat(locatedDevice.longitude || 0).toFixed(6) }}, 纬度: {{ parseFloat(locatedDevice.latitude || 0).toFixed(6) }}</span>
              </div>
              
              <div class="info-segment">
                <div class="info-segment-title">设备信息</div>
                <div class="info-segment-content">
                  <div class="info-row">
                    <span class="info-label">设备类型:</span>
                    <span class="info-value">{{ formatDeviceType(locatedDevice.deviceType) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">设备状态:</span>
                    <span class="info-value" :class="getStatusClass(locatedDevice.deviceStatus)">{{ locatedDevice.deviceStatus || '未知' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">所在位置:</span>
                    <span class="info-value">{{ locatedDevice.longitude ? parseFloat(locatedDevice.longitude).toFixed(4) + ', ' + parseFloat(locatedDevice.latitude).toFixed(4) : '未知' }}</span>
                  </div>
                </div>
              </div>
              
              <div class="locate-actions">
                <button 
                  @click="isLocated ? exitDeviceLocation() : centerOnDevice()" 
                  :class="['action-btn', isLocated ? 'exit-btn' : 'center-btn']"
                >
                  <i class="action-icon">{{ isLocated ? '✕' : '🎯' }}</i> 
                  {{ isLocated ? '退出定位' : '立即定位' }}
                </button>
                <button @click="clearDeviceSearch" class="action-btn exit-btn">
                  <i class="action-icon">✕</i> 退出
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 设备搜索结果 -->
        <div v-else-if="deviceData" class="device-content-wrapper device-vertical-layout">
          <div class="device-result-card">
            <div class="device-card-header">
              <div class="device-item-icon icon-device"></div>
              <div class="device-item-info">
                <div class="device-item-id">{{ deviceData.rcuId }}</div>
                <div class="device-item-detail">更新时间: {{ formatTimeForDisplay(deviceData.receiveTime) }}</div>
              </div>
              <div class="device-item-status" :class="getStatusClass(deviceData.deviceStatus)">
                {{ deviceData.deviceStatus || '状态未知' }}
              </div>
            </div>
            
            <div class="device-card-body">
              <div class="device-info-section">
                <div class="device-info-row">
                  <span class="info-label">设备ID:</span>
                  <span class="info-value">{{ deviceData.rcuId }}</span>
                </div>
                <div class="device-info-row">
                  <span class="info-label">设备类型:</span>
                  <span class="info-value">{{ formatDeviceType(deviceData.deviceType) }}</span>
                </div>
                <div class="device-info-row">
                  <span class="info-label">更新时间:</span>
                  <span class="info-value">{{ formatTime(deviceData.receiveTime) }}</span>
                </div>
              </div>
              
              <div class="info-segment">
                <div class="info-segment-title">位置数据</div>
                <div class="info-segment-content">
                  <div class="info-row">
                    <span class="info-label">经度:</span>
                    <span class="info-value">{{ deviceData.longitude ? parseFloat(deviceData.longitude).toFixed(6) : '未知' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">纬度:</span>
                    <span class="info-value">{{ deviceData.latitude ? parseFloat(deviceData.latitude).toFixed(6) : '未知' }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="device-card-footer">
              <button @click="startLocateMode(deviceData)" class="device-action-btn locate-btn">
                <i class="action-icon">📍</i> 设备定位
              </button>
              <button @click="clearDeviceSearch" class="device-action-btn clear-btn">
                <i class="clear-icon">✕</i> 清除搜索
              </button>
            </div>
          </div>
        </div>
        
        <!-- 新增：显示搜索不到结果的提示 -->
        <div v-else-if="showNoDeviceFound" class="device-content-wrapper">
          <div class="no-result-wrapper">
            <div class="no-result-content">
              <div class="no-result-icon">❓</div>
              <div class="no-result-title">未找到设备</div>
              <div class="no-result-message">没有找到ID为 "{{ lastSearchedRcuId }}" 的设备信息</div>
              <div class="no-result-suggestion">请检查设备ID是否正确，或尝试其他ID</div>
              <button @click="clearDeviceSearch" class="device-action-btn clear-btn" style="margin-top: 10px; width: 120px;">
                <i class="clear-icon">✕</i> 返回
              </button>
            </div>
          </div>
        </div>
        
        <!-- 设备搜索历史和示例 -->
        <div v-else class="device-content-wrapper empty-state">
          <div class="device-history-section" v-if="recentDeviceSearches.length > 0">
            <div class="recent-title">
              <i class="recent-title-icon">🕒</i> 最近搜索记录
              <button class="clear-history-btn" @click="clearDeviceHistory" title="清空记录">
                <i class="clear-icon">🗑️</i>
              </button>
            </div>
            <div class="recent-list">
              <div 
                v-for="(item, index) in recentDeviceSearches" 
                :key="index"
                class="recent-item"
                @click="selectRecentDeviceSearch(item)"
              >
                <div class="recent-icon icon-device"></div>
                <div class="recent-text">{{ item.rcuId }}</div>
                <div class="recent-time">{{ formatRelativeTime(item.timestamp) }}</div>
              </div>
            </div>
          </div>
          
          <div class="device-example-section">
            <div class="empty-search">
              <div class="empty-icon">📡</div>
              <div class="empty-text">请输入设备ID进行查询</div>
              <div class="list-all-devices">
                <button @click="showAllDevices" class="list-all-btn">
                  <i class="list-all-icon">📋</i> 查看所有设备
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
    
  <script>
  import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
  import axios from 'axios'


  export default {
    name: 'DeviceSearch',
    emits: ['search-active', 'search-cleared'],
    props: { onZoomTo: Function },
    setup(props, { emit }) {
      const searchInput = ref(null);
      const rcuId = ref('');
      const deviceData = ref(null);
      const recentDeviceSearches = ref([]);
      const isInLocateMode = ref(false);
      const locatedDevice = ref(null);
      // 用于处理未找到设备的情况
      const showNoDeviceFound = ref(false);
      const lastSearchedRcuId = ref('');
      // 搜索建议相关
      const showSearchSuggestions = ref(false);
      const filteredSuggestions = ref([]);
      
      // 定位状态
      const isLocated = ref(false);
      
      // 设备列表模式相关
      const isInListMode = ref(false);
      const deviceList = ref([]);
      const total = ref(0);
      const pageNum = ref(1);
      const pageSize = ref(15); // 每页15条
      const loadingList = ref(false);
      
    
      
      // 计算属性：是否显示搜索结果（覆盖右侧面板）
      const showSearchResults = computed(() => {
        return deviceData.value || isInLocateMode.value || showNoDeviceFound.value || isInListMode.value || (rcuId.value && rcuId.value.trim().length > 0);
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
          // 确保彻底清除搜索状态后，再恢复右侧面板显示
          nextTick(() => {
            emit('search-cleared');
          });
        }
      });
      
      // 搜索建议过滤
      function handleSearchInput() {
        if (rcuId.value.trim().length > 0) {
          // 从后端获取匹配的设备ID
          axios.get('/menu/device/list', {
            params: {
              pageNum: 1,
              pageSize: 5,
              rcuId: rcuId.value.trim()
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
      function selectSuggestion(device) {
        rcuId.value = device.rcuId;
        showSearchSuggestions.value = false;
        searchDevice();
      }
      
      // 清空设备搜索 - 修改以确保彻底清除所有状态并恢复右侧面板
      function clearDeviceSearch() {
        console.log('欲儿测到了清除列表');

        // 先保存当前状态，以便决定是否需要触发search-cleared事件
        const wasSearchActive = showSearchResults.value;
        
        // 首先清除地图上的定位标记（无论是否处于定位模式）
        if (isLocated.value) {
          window.dispatchEvent(new CustomEvent('clear-located-device'));
          isLocated.value = false;
        }
        
        // 然后退出任何特殊模式
        isInLocateMode.value = false;
        locatedDevice.value = null;
        
        if (isInListMode.value) {
          isInListMode.value = false;
          deviceList.value = [];
        }
        
        //然后清除所有其他状态
        deviceData.value = null;
        showNoDeviceFound.value = false;
        lastSearchedRcuId.value = '';
        rcuId.value = '';
        showSearchSuggestions.value = false;
        filteredSuggestions.value = [];
        
        // // 只有在搜索之前是激活状态时才发送清除事件
        // if (wasSearchActive) {
        //   // 确保通知父组件搜索已清除，右侧面板可以恢复显示
        //   emit('search-cleared', { source: 'device-search' });
          
        //   // 使用setTimeout确保DOM更新后再尝试刷新右侧面板
        //   setTimeout(() => {
        //     window.dispatchEvent(new CustomEvent('refresh-right-panel', {
        //       detail: { source: 'device-search' }
        //     }));
        //   }, 100);
        // }
      }
      
      // 选择最近搜索记录
      function selectRecentDeviceSearch(device) {
        rcuId.value = device.rcuId;
        searchDevice();
      }
      
      // 添加到最近搜索
      function addToRecentDeviceSearches(device) {
        const exists = recentDeviceSearches.value.findIndex(item => item.rcuId === device.rcuId);
        if (exists !== -1) {
          recentDeviceSearches.value.splice(exists, 1);
        }
        
        recentDeviceSearches.value.unshift({
          rcuId: device.rcuId,
          timestamp: Date.now()
        });
        
        if (recentDeviceSearches.value.length > 5) {
          recentDeviceSearches.value = recentDeviceSearches.value.slice(0, 5);
        }
        
        localStorage.setItem('recentDeviceSearches', JSON.stringify(recentDeviceSearches.value));
      }
      
      // 格式化设备类型
      function formatDeviceType(type) {
        if (!type) return '未知';
        
        // 如果是数字类型的设备类型，做一个映射转换
        const typeMap = {
          '0': '融合设备',
          '1': '激光雷达',
          '2': '摄像头',
          '3': '毫米波雷达'
        };
        
        return typeMap[type] || type;
      }
      
      // 获取状态样式类
      function getStatusClass(status) {
        if (!status) return 'status-unknown';
        
        if (status === '在线') return 'status-online';
        if (status === '离线') return 'status-offline';
        if (status === '异常') return 'status-error';
        
        return 'status-unknown';
      }
      
      // 格式化时间戳
      function formatTime(timestamp) {
        if (!timestamp) return '-';
        
        try {
          // 如果是ISO格式时间字符串
          if (typeof timestamp === 'string' && timestamp.includes('T')) {
            const date = new Date(timestamp);
            if (isNaN(date.getTime())) return '-';
            
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
          }
          
          // 如果是时间戳
          const timestamp_num = parseInt(timestamp);
          if (isNaN(timestamp_num)) return '-';
          
          const date = new Date(timestamp_num);
          if (isNaN(date.getTime())) return '-';
          
          const year = date.getFullYear();
          const month = String(date.getMonth() + 1).padStart(2, '0');
          const day = String(date.getDate()).padStart(2, '0');
          const hours = String(date.getHours()).padStart(2, '0');
          const minutes = String(date.getMinutes()).padStart(2, '0');
          const seconds = String(date.getSeconds()).padStart(2, '0');
          
          return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        } catch (error) {
          console.error('格式化时间出错:', error);
          return '-';
        }
      }
      
      // 格式化时间显示 - 用于列表中的简短时间显示
      function formatTimeForDisplay(timestamp) {
        if (!timestamp) return '-';
        
        try {
          // 如果是ISO格式时间字符串
          if (typeof timestamp === 'string' && timestamp.includes('T')) {
            const date = new Date(timestamp);
            if (isNaN(date.getTime())) return '-';
            
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            return `${hours}:${minutes}:${seconds}`;
          }
          
          // 如果是时间戳
          const timestamp_num = parseInt(timestamp);
          if (isNaN(timestamp_num)) return '-';
          
          const date = new Date(timestamp_num);
          if (isNaN(date.getTime())) return '-';
          
          const hours = String(date.getHours()).padStart(2, '0');
          const minutes = String(date.getMinutes()).padStart(2, '0');
          const seconds = String(date.getSeconds()).padStart(2, '0');
          
          return `${hours}:${minutes}:${seconds}`;
        } catch (error) {
          console.error('格式化显示时间出错:', error);
          return '-';
        }
      }
      
      // 格式化相对时间
      function formatRelativeTime(timestamp) {
        if (!timestamp) return '';
        
        try {
          const now = Date.now();
          const diff = now - timestamp;
          
          if (diff < 60 * 1000) return '刚刚';
          if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`;
          if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`;
          if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`;
          
          const date = new Date(timestamp);
          if (isNaN(date.getTime())) return '';
          
          return `${date.getMonth() + 1}月${date.getDate()}日`;
        } catch (error) {
          console.error('格式化相对时间出错:', error);
          return '';
        }
      }
      
      // 从列表中选择设备，使用设备详情API
      async function selectDeviceFromList(device) {
        // 退出列表模式
        isInListMode.value = false;
        
        // 重置状态
        deviceData.value = null;
        showNoDeviceFound.value = false;
        
        // 修复: 同步设置 rcuId.value，确保状态一致
        rcuId.value = device.rcuId;
        
        console.log('正在请求设备详情:', device.rcuId);
        
        try {
          // 使用设备ID调用详情API
          const response = await axios.get('/menu/device', {
            params: {
              rcuId: device.rcuId
            }
          });
          
          console.log('获取设备详情返回结果:', response.data);
          
          if (response.data.status === 'success') {
            if (response.data.device) {
              console.log('API返回了设备数据');
                
              // 设置设备数据
              deviceData.value = {
                ...response.data.device,
                deviceStatus: response.data.deviceStatus
              };
              
              // 添加到最近搜索
              addToRecentDeviceSearches({ rcuId: deviceData.value.rcuId });
            } else {
              console.warn('API返回成功但没有设备数据，尝试使用列表中的数据');
              // 如果API返回成功但没有设备数据，使用列表中的设备数据
              deviceData.value = {
                rcuId: device.rcuId,
                deviceType: device.deviceType,
                receiveTime: device.receiveTime,
                longitude: device.longitude,
                latitude: device.latitude
              };
              
              // 添加到最近搜索
              addToRecentDeviceSearches({ rcuId: device.rcuId });
            }
          } else {
            // API返回失败状态时，也使用列表中的数据
            console.warn('API返回失败状态，使用列表中的数据');
            deviceData.value = {
              rcuId: device.rcuId,
              deviceType: device.deviceType,
              receiveTime: device.receiveTime,
              longitude: device.longitude,
              latitude: device.latitude
            };
            
            // 添加到最近搜索
            addToRecentDeviceSearches({ rcuId: device.rcuId });
          }
        } catch (error) {
          console.error('获取设备详情失败:', error);
          
          // 出错时也使用列表中的数据
          deviceData.value = {
            rcuId: device.rcuId,
            deviceType: device.deviceType,
            receiveTime: device.receiveTime,
            longitude: device.longitude,
            latitude: device.latitude
          };
          
          // 添加到最近搜索
          addToRecentDeviceSearches({ rcuId: device.rcuId });
        }
      }
      
      // 查询设备 - 使用API接口
      async function searchDevice() {
        if (!rcuId.value.trim()) {
          return;
        }
        
        if (isInLocateMode.value) {
          exitLocateMode();
        }
        
        if (isInListMode.value) {
          exitListMode();
        }
        
        // 重置状态
        deviceData.value = null;
        showNoDeviceFound.value = false;
        lastSearchedRcuId.value = rcuId.value.trim();
        showSearchSuggestions.value = false;
        
        emit('search-active');
        
        console.log('正在搜索设备:', rcuId.value);
        
        try {
          // 调用API接口
          const response = await axios.get('/menu/device', {
            params: {
              rcuId: rcuId.value.trim()
            }
          });
          
          console.log('搜索设备返回结果:', response.data);
          
          if (response.data.status === 'success') {
            if (response.data.device) {
              console.log('API返回了设备数据:', response.data.device);
              
              // 设置设备数据
              deviceData.value = {
                ...response.data.device,
                deviceStatus: response.data.deviceStatus
              };
              
              // 添加到最近搜索
              addToRecentDeviceSearches({ rcuId: deviceData.value.rcuId });
            } else {
              // 尝试从列表API获取基本信息
              console.log('API没有返回设备数据，尝试从列表API获取');
              
              try {
                const listResponse = await axios.get('/menu/device/list', {
                  params: {
                    pageNum: 1,
                    pageSize: 1,
                    rcuId: rcuId.value.trim()
                  }
                });
                
                if (listResponse.data.status === 'success' && listResponse.data.rows && listResponse.data.rows.length > 0) {
                  // 使用列表API返回的设备信息
                  const listDevice = listResponse.data.rows[0];
                  deviceData.value = {
                    rcuId: listDevice.rcuId,
                    deviceType: listDevice.deviceType,
                    receiveTime: listDevice.receiveTime,
                    longitude: listDevice.longitude,
                    latitude: listDevice.latitude
                  };
                  
                  // 添加到最近搜索
                  addToRecentDeviceSearches({ rcuId: deviceData.value.rcuId });
                } else {
                  // 未找到设备
                  showNoDeviceFound.value = true;
                  console.log('未找到设备:', rcuId.value);
                }
              } catch (listError) {
                console.error('列表API调用失败:', listError);
                showNoDeviceFound.value = true;
              }
            }
          } else {
            // API返回失败状态
            console.warn('API查询返回失败状态');
            
            // 尝试从列表API获取
            try {
              console.log('尝试从列表API获取设备信息');
              const listResponse = await axios.get('/menu/device/list', {
                params: {
                  pageNum: 1,
                  pageSize: 1,
                  rcuId: rcuId.value.trim()
                }
              });
              
              if (listResponse.data.status === 'success' && listResponse.data.rows && listResponse.data.rows.length > 0) {
                // 使用列表API返回的设备信息
                const listDevice = listResponse.data.rows[0];
                deviceData.value = {
                  rcuId: listDevice.rcuId,
                  deviceType: listDevice.deviceType,
                  receiveTime: listDevice.receiveTime,
                  longitude: listDevice.longitude,
                  latitude: listDevice.latitude
                };
                
                // 添加到最近搜索
                addToRecentDeviceSearches({ rcuId: deviceData.value.rcuId });
              } else {
                showNoDeviceFound.value = true;
              }
            } catch (listError) {
              console.error('列表API调用也失败:', listError);
              showNoDeviceFound.value = true;
            }
          }
        } catch (error) {
          console.error('查询设备失败:', error);
          
          // 尝试从列表API获取
          try {
            console.log('尝试从列表API获取设备信息');
            const listResponse = await axios.get('/menu/device/list', {
              params: {
                pageNum: 1,
                pageSize: 1,
                rcuId: rcuId.value.trim()
              }
            });
            
            if (listResponse.data.status === 'success' && listResponse.data.rows && listResponse.data.rows.length > 0) {
              // 使用列表API返回的设备信息
              const listDevice = listResponse.data.rows[0];
              deviceData.value = {
                rcuId: listDevice.rcuId,
                deviceType: listDevice.deviceType,
                receiveTime: listDevice.receiveTime,
                longitude: listDevice.longitude,
                latitude: listDevice.latitude
              };
              
              // 添加到最近搜索
              addToRecentDeviceSearches({ rcuId: deviceData.value.rcuId });
            } else {
              showNoDeviceFound.value = true;
            }
          } catch (listError) {
            console.error('列表API调用也失败:', listError);
            showNoDeviceFound.value = true;
          }
        }
      }
      
      // 进入定位模式
      function startLocateMode(device) {
        // 退出列表模式
        if (isInListMode.value) {
          isInListMode.value = false;
        }
        
        // // 重置定位状态
        isLocated.value = false;
        
        // // 设置要定位的设备
        locatedDevice.value = device;
        console.log('欲儿永远讨厌bug');
        
        // console.log('进入定位模式, 设备数据:', locatedDevice.value);
        isInLocateMode.value = true;
        
        // // 添加到最近搜索
        //addToRecentDeviceSearches({ rcuId: locatedDevice.value.rcuId });
      }
      
      // 退出定位模式
      function exitLocateMode() {
        if (isInLocateMode.value) {
          // 清除定位状态
          if (isLocated.value) {
            exitDeviceLocation();
          }
          
          isInLocateMode.value = false;
          locatedDevice.value = null;
          isLocated.value = false;
        }
      }
      
      // 清除设备定位 - 确保清除定位状态并发送事件
      function exitDeviceLocation() {
        console.log('清除设备定位');
        const longitude = parseFloat(locatedDevice.value.longitude);
        const latitude = parseFloat(locatedDevice.value.latitude);
        props.onZoomTo(longitude, latitude,12);
        
        // 显式发送事件来清除地图上的定位标记
        //window.dispatchEvent(new CustomEvent('clear-located-device'));
        // 确保定位状态被重置
        isLocated.value = false;
      }
      
      // 在地图上居中定位设备
      function centerOnDevice() {
        console.log('欲儿测立即定位');
        if (!locatedDevice.value) {
          console.error('没有要定位的设备数据');
          return;
        }
        
        console.log('准备定位设备:', locatedDevice.value);
        
        // 检查是否有经纬度数据
        if (!locatedDevice.value.longitude || !locatedDevice.value.latitude) {
          console.error('设备缺少经纬度信息，无法定位');
          
          // 尝试从API重新获取设备位置信息
          axios.get('/menu/device', {
            params: {
              rcuId: locatedDevice.value.rcuId
            }
          }).then(response => {
            if (response.data.status === 'success' && response.data.device) {
              // 更新设备信息
              locatedDevice.value = {
                ...locatedDevice.value,
                ...response.data.device,
                deviceStatus: response.data.deviceStatus
              };
              
              if (locatedDevice.value.longitude && locatedDevice.value.latitude) {
                console.log('已获取到经纬度信息，执行定位');
                executeDeviceLocation();
              } else {
                console.error('无法获取到设备经纬度信息');
              }
            } else {
              console.error('获取设备详情失败');
            }
          }).catch(error => {
            console.error('API调用失败:', error);
          });
          
          return;
        }
        
        // 执行定位
        executeDeviceLocation();
      }
      
      // 执行设备定位
      function executeDeviceLocation() {
        console.log('欲儿lastone定位');
        // 确保经纬度是数值
        const longitude = parseFloat(locatedDevice.value.longitude);
        const latitude = parseFloat(locatedDevice.value.latitude);
        
        // 检查坐标是否有效
        if (isNaN(longitude) || isNaN(latitude)) {
          console.error('无效的设备坐标:', longitude, latitude);
          return;
        }
        
        console.log('触发设备定位事件, 坐标:', [longitude, latitude]);
        
        // 触发定位事件，使用美化后的定位标记
        console.log('欲儿马上撒花');
        // window.dispatchEvent(new CustomEvent('locate-device', {
        //   detail: {
        //     id: locatedDevice.value.rcuId,
        //     coordinates: [longitude, latitude],
        //     title: locatedDevice.value.rcuId,
        //     deviceType: formatDeviceType(locatedDevice.value.deviceType),
        //     status: locatedDevice.value.deviceStatus || '状态未知',
        //     latestTime: locatedDevice.value.receiveTime,
        //     useEnhancedMarker: false // 标记使用美化标识
        //   }
        // }));

        // zoomToLocation(longitude, latitude); // 上海位置，放大到默认18级
        // console.log('欲儿定位成功');

        console.log("yuerdingwei:",longitude,latitude);
        props.onZoomTo(longitude, latitude,15);



        // 更新定位状态
        isLocated.value = true;
      }
      
      // 显示所有设备列表
      function showAllDevices() {
        console.log('欲儿来咯右上角');
        // if (isInLocateMode.value) {
        //   exitLocateMode();
        // }
        
        deviceData.value = null;
        showNoDeviceFound.value = false;
        
        isInListMode.value = true;
        pageNum.value = 1; // 重置为第一页
        
        fetchDeviceList();
        
        emit('search-active'); // 通知父组件搜索已激活
      }
      
      // 退出列表模式
      function exitListMode() {
        isInListMode.value = false;
        deviceList.value = [];
        
        // 在退出列表模式时，如果没有其他搜索内容，则清除搜索
        if (!deviceData.value && !isInLocateMode.value && !showNoDeviceFound.value && rcuId.value.trim() === '') {
          clearDeviceSearch();
        } else {
          // 如果有搜索内容，触发搜索操作
          if (rcuId.value.trim() !== '') {
            searchDevice();
          }
        }
      }
      
      // 获取设备列表数据
      async function fetchDeviceList() {
        if (loadingList.value) return;
        
        loadingList.value = true;
        
        try {
          console.log('获取设备列表, 页码:', pageNum.value, '每页条数:', pageSize.value);
          
          // 调用API接口
          const response = await axios.get('/menu/device/list', {
            params: {
              pageNum: pageNum.value,
              pageSize: pageSize.value
            }
          });
          
          console.log('获取设备列表返回结果:', response.data);
          
          if (response.data && response.data.status === 'success') {
            // 使用返回的数据更新列表和总数
            total.value = response.data.total || 0;
            deviceList.value = response.data.rows || [];
          } else {
            console.error('获取设备列表失败:', response.data);
            deviceList.value = [];
            total.value = 0;
          }
        } catch (error) {
          console.error('获取设备列表API调用失败:', error);
          deviceList.value = [];
          total.value = 0;
        } finally {
          loadingList.value = false;
        }
      }
      
      // 刷新设备列表
      function refreshDeviceList() {
        fetchDeviceList();
      }
      
      // 切换页码
      function changePage(newPage) {
        if (newPage < 1 || newPage > Math.ceil(total.value / pageSize.value) || newPage === pageNum.value) {
          return;
        }
        
        pageNum.value = newPage;
        fetchDeviceList();
      }
      
      // 清空历史记录
      function clearDeviceHistory() {
        recentDeviceSearches.value = [];
        localStorage.removeItem('recentDeviceSearches');
      }
      
      // 生命周期钩子
      onMounted(() => {
        // 从本地存储加载最近搜索
        const savedDeviceSearches = localStorage.getItem('recentDeviceSearches');
        if (savedDeviceSearches) {
          try {
            recentDeviceSearches.value = JSON.parse(savedDeviceSearches);
          } catch (e) {
            console.error('Failed to parse recent device searches', e);
          }
        }
        
        // 点击页面其他地方时隐藏搜索建议
        const handleDocumentClick = (event) => {
          if (searchInput.value && !searchInput.value.contains(event.target)) {
            showSearchSuggestions.value = false;
          }
        };
        
        document.addEventListener('click', handleDocumentClick);
        
        // 当组件挂载时，如果URL中包含设备ID参数，自动搜索
        const urlParams = new URLSearchParams(window.location.search);
        const deviceIdParam = urlParams.get('deviceId');
        if (deviceIdParam) {
          rcuId.value = deviceIdParam;
          searchDevice();
        }
        
        // 确保清理事件监听器
        onUnmounted(() => {
          document.removeEventListener('click', handleDocumentClick);
        });
      });
      
      return {
        searchInput,
        rcuId,
        deviceData,
        recentDeviceSearches,
        isInLocateMode,
        locatedDevice,
        showNoDeviceFound,
        lastSearchedRcuId,
        showSearchSuggestions,
        filteredSuggestions,
        showSearchResults,
        isLocated,
        
        // 列表模式相关
        isInListMode,
        deviceList,
        total,
        pageNum,
        pageSize,
        displayedPages,
        
        formatTime,
        formatTimeForDisplay,
        formatRelativeTime,
        formatDeviceType,
        getStatusClass,
        searchDevice,
        selectRecentDeviceSearch,
        startLocateMode,
        exitLocateMode,
        centerOnDevice,
        exitDeviceLocation,
        handleSearchInput,
        selectSuggestion,
        clearDeviceHistory,
        clearDeviceSearch,
        
        // 列表相关方法
        showAllDevices,
        exitListMode,
        fetchDeviceList,
        refreshDeviceList,
        changePage,
        selectDeviceFromList
      };
    }
  }
  </script>
    
  <style scoped>
  .device-search-container {
    position: absolute;
    top: 0.8%;
    right: 0.5%;
    width: 24%;
    z-index: 20; /* 提高z-index，确保在RightPanel之上 */
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
    background-color: rgba(108, 99, 255, 0.6);
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
    background-color: rgba(108, 99, 255, 0.8);
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
    background-color: rgba(108, 99, 255, 0.2);
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
  
  /* 搜索结果面板样式 - 修改透明度和层级 */
  .search-results-panel {
    position: absolute;
    top: calc(100% + 6px);
    left: 0;
    width: 100%;
    background-color: rgba(4, 34, 53, 0.95); /* 提高背景透明度以更好地覆盖下方内容 */
    border: 1px solid rgba(14, 89, 134, 0.8); /* 提高边框颜色强度 */
    border-radius: 4px;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.5); /* 增强阴影效果 */
    z-index: 20; /* 确保在RightPanel之上 */
    height: 72vh;
    overflow-y: auto;
    padding: 8px;
    backdrop-filter: blur(3px); /* 添加背景模糊效果 */
  }
  
  /* 以下是从BottomDataPanel复制并修改的样式 */
  .device-content-wrapper {
    display: flex;
    flex-direction: row;
    height: 80%;
    gap: 6px;
    overflow: hidden;
  }
  
  /* 垂直布局样式 */
  .device-content-wrapper.device-vertical-layout {
    display: flex;
    flex-direction: column;
    height: auto;
    max-height: 800px;
    gap: 8px;
    overflow-y: auto;
  }
  
  .device-content-wrapper.empty-state {
    background-color: rgba(4, 34, 53, 0.7); /* 提高空状态背景不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.5);
    border-radius: 3px;
  }
  
  .device-result-section {
    flex: 1;
    overflow-y: auto;
    min-width: 0;
    background-color: rgba(4, 34, 53, 0.7); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
    padding: 6px;
  }
  
  .device-history-section {
    flex: 1;
    overflow-y: auto;
    padding: 6px;
    background-color: rgba(4, 34, 53, 0.7); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
  }
  
  .device-example-section {
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
  
  .located-device-info {
    background-color: rgba(108, 99, 255, 0.3); /* 提高不透明度 */
    border: 1px solid rgba(108, 99, 255, 0.6);
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
  
  .device-icon, .device-icon {
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(108, 99, 255, 0.3);
    border-radius: 50%;
    margin-right: 8px;
    font-size: 14px;
    flex-shrink: 0;
  }
  
  .device-info-main {
    flex: 1;
    min-width: 0;
  }
  
  .device-number {
    font-size: 14px;
    font-weight: bold;
    color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .device-status {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.9);
    margin-top: 2px;
    padding: 1px 0;
  }
  
  .located-position {
    display: flex;
    align-items: center;
    padding: 4px 6px;
    background-color: rgba(4, 34, 53, 0.8); /* 提高不透明度 */
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
    background-color: rgba(108, 99, 255, 0.8);
    color: white;
  }
  
  .center-btn:hover {
    background-color: rgba(108, 99, 255, 1);
  }
  
  .exit-btn {
    background-color: rgba(255, 77, 79, 0.8);
    color: white;
  }
  
  .exit-btn:hover {
    background-color: rgba(255, 77, 79, 1);
  }
  
  /* 搜索结果卡片 */
  .device-result-card {
    background-color: rgba(4, 34, 53, 0.8); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
    overflow: hidden;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  
  .device-card-header {
    display: flex;
    align-items: center;
    padding: 6px 8px;
    background-color: rgba(4, 34, 53, 0.9); /* 提高不透明度 */
    min-height: 28px;
  }
  
  .device-item-icon {
    width: 26px;
    height: 26px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(108, 99, 255, 0.3);
    border-radius: 50%;
    margin-right: 8px;
    font-size: 14px;
    flex-shrink: 0;
  }
  
  .device-item-info {
    flex: 1;
    min-width: 0;
  }
  
  .device-item-id {
    font-size: 14px;
    font-weight: bold;
    color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin: 0;
    line-height: 1.3;
  }
  
  .device-item-detail {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.7);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin: 0;
    line-height: 1.3;
  }
  
  .device-item-status {
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 10px;
    white-space: nowrap;
    margin-left: 6px;
    flex-shrink: 0;
  }
  
  .device-card-body {
    padding: 6px 8px;
    border-top: 1px solid rgba(14, 89, 134, 0.3);
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  
  /* 新增：垂直布局中的详细信息区域 */
  .device-info-section {
    border: 1px solid rgba(14, 89, 134, 0.5);
    border-radius: 3px;
    padding: 6px;
    background-color: rgba(4, 34, 53, 0.6); /* 提高不透明度 */
  }
  
  .device-info-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
    font-size: 12px;
    line-height: 1.4;
  }
  
  .device-info-row:last-child {
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
  
  /* 底部按钮行样式 */
  .device-card-footer {
    display: flex;
    border-top: 1px solid rgba(14, 89, 134, 0.5);
    height: 32px;
  }
  
  .device-action-btn {
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
    background-color: rgba(108, 99, 255, 0.4); /* 提高不透明度 */
    border-right: 1px solid rgba(14, 89, 134, 0.5);
  }
  
  .locate-btn:hover {
    background-color: rgba(108, 99, 255, 0.6);
  }
  
  .clear-btn {
    background-color: rgba(255, 77, 79, 0.4); /* 提高不透明度 */
  }
  
  .clear-btn:hover {
    background-color: rgba(255, 77, 79, 0.6);
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
    background-color: rgba(4, 34, 53, 0.8); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
    padding: 4px 6px;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .recent-item:hover {
    background-color: rgba(108, 99, 255, 0.3);
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
    background-color: rgba(4, 34, 53, 0.6);
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
    color: rgba(255, 255, 255, 0.8); /* 提高文字对比度 */
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
  
  /* 列出所有设备按钮 */
  .list-all-devices {
    margin-top: 6px;
  }
  
  .list-all-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(0, 210, 91, 0.4); /* 提高不透明度 */
    border: 1px solid rgba(0, 210, 91, 0.6);
    color: white;
    border-radius: 3px;
    padding: 6px 12px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.3s;
  }
  
  .list-all-btn:hover {
    background-color: rgba(0, 210, 91, 0.6);
    transform: translateY(-1px);
  }
  
  .list-all-icon {
    margin-right: 6px;
  }
  
  /* 信息段落容器 */
  .info-segment {
    background-color: rgba(4, 34, 53, 0.6); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
    overflow: hidden;
    flex: 1;
  }
  
  .info-segment-title {
    font-size: 12px;
    color: #fff;
    background-color: rgba(4, 34, 53, 0.8); /* 提高不透明度 */
    padding: 3px 6px;
    border-bottom: 1px solid rgba(14, 89, 134, 0.6);
  }
  
  .info-segment-content {
    padding: 6px;
  }
  
  /* 未找到结果样式 */
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
    background-color: rgba(4, 34, 53, 0.9); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.7);
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
  
  /* 图标样式 */
  .icon-device::after {
    content: '📡';
    font-size: 13px;
  }
  
  /* 状态样式 */
  .status-online {
    background-color: rgb(0, 210, 91);
    color: white;
  }
  
  .status-offline {
    background-color: rgb(255, 77, 79);
    color: white;
  }
  
  .status-error {
    background-color: rgb(255, 152, 0);
    color: white;
  }
  
  .status-unknown {
    background-color: rgb(108, 117, 125);
    color: white;
  }
  
  /* 设备列表模式样式 */
  .device-content-wrapper.device-list-mode {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 500px;
    gap: 8px;
    overflow: hidden;
  }
  
  .device-list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 10px;
    background-color: rgba(4, 34, 53, 0.9); /* 提高不透明度 */
    border-radius: 3px;
    border: 1px solid rgba(14, 89, 134, 0.7);
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
    background-color: rgba(108, 99, 255, 0.5); /* 提高不透明度 */
    color: white;
  }
  
  .refresh-list-btn:hover {
    background-color: rgba(108, 99, 255, 0.7);
  }
  
  .exit-list-btn {
    background-color: rgba(255, 77, 79, 0.5); /* 提高不透明度 */
    color: white;
  }
  
  .exit-list-btn:hover {
    background-color: rgba(255, 77, 79, 0.7);
  }
  
  .device-list-container {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    background-color: rgba(4, 34, 53, 0.8); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
  }
  
  .device-table-header {
    display: flex;
    background-color: rgba(4, 34, 53, 0.9); /* 提高不透明度 */
    padding: 6px 8px;
    font-size: 12px;
    font-weight: bold;
    color: rgba(255, 255, 255, 0.9);
    border-bottom: 1px solid rgba(14, 89, 134, 0.7);
  }
  
  .device-table-body {
    flex: 1;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: rgba(108, 99, 255, 0.6) rgba(4, 34, 53, 0.4);
  }
  
  .device-table-body::-webkit-scrollbar {
    width: 4px;
  }
  
  .device-table-body::-webkit-scrollbar-track {
    background: rgba(4, 34, 53, 0.4);
  }
  
  .device-table-body::-webkit-scrollbar-thumb {
    background-color: rgba(108, 99, 255, 0.6);
    border-radius: 4px;
  }
  
  .header-cell, .device-cell {
    padding: 0 8px;
  }
  
  .id-cell {
    width: 25%;
    flex-shrink: 0;
  }
  
  .type-cell {
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
  
  .device-row {
    display: flex;
    align-items: center;
    padding: 6px 8px;
    font-size: 12px;
    color: white;
    border-bottom: 1px solid rgba(14, 89, 134, 0.5);
    transition: background-color 0.2s;
    cursor: pointer;
  }
  
  .device-row:hover {
    background-color: rgba(108, 99, 255, 0.2);
  }
  
  .device-row:last-child {
    border-bottom: none;
  }
  
  .device-cell {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .device-cell.id-cell {
    display: flex;
    align-items: center;
    font-weight: bold;
  }
  
  .device-row-icon {
    width: 22px;
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(108, 99, 255, 0.3);
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
    background-color: rgba(108, 99, 255, 0.5); /* 提高不透明度 */
    color: white;
    width: 85px;
  }
  
  .details-action:hover {
    background-color: rgba(108, 99, 255, 0.7);
  }
  
  .no-device-data {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding: 20px;
    color: rgba(255, 255, 255, 0.8); /* 提高文字对比度 */
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
    background-color: rgba(4, 34, 53, 0.8); /* 提高不透明度 */
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.9); /* 提高文字对比度 */
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
    background-color: rgba(4, 34, 53, 0.7);
    border: 1px solid rgba(14, 89, 134, 0.6);
    border-radius: 3px;
    color: white;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 12px;
  }
  
  .page-btn:hover:not(:disabled) {
    background-color: rgba(108, 99, 255, 0.5);
    border-color: rgba(108, 99, 255, 0.7);
  }
  
  .page-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  .active-page {
    background-color: rgba(108, 99, 255, 0.7); /* 提高不透明度 */
    border-color: rgba(108, 99, 255, 0.9);
    font-weight: bold;
  }
  
  .page-numbers {
    display: flex;
    gap: 4px;
  }
  
  /* 修改搜索结果面板高度和滚动条样式 */
  .search-results-panel {
    max-height: calc(100vh - 120px); /* 限制最大高度 */
    scrollbar-width: thin;
    scrollbar-color: rgba(108, 99, 255, 0.6) rgba(4, 34, 53, 0.4);
  }
  
  .search-results-panel::-webkit-scrollbar {
    width: 4px;
  }
  
  .search-results-panel::-webkit-scrollbar-track {
    background: rgba(4, 34, 53, 0.4);
  }
  
  .search-results-panel::-webkit-scrollbar-thumb {
    background-color: rgba(108, 99, 255, 0.6);
    border-radius: 4px;
  }
  
  /* 响应式调整 */
  @media (max-width: 1400px) {
    .device-content-wrapper {
      height: 200px;
    }
    
    .device-content-wrapper.device-list-mode {
      height: 100%;
      min-height: 450px;
    }
  }
  
  @media (max-width: 992px) {
    .device-content-wrapper {
      flex-direction: column;
      height: auto;
      max-height: 400px;
    }
    
    .device-result-section,
    .device-details-section {
      width: 100%;
    }
    
    .device-content-wrapper.device-list-mode {
      height: 100%;
      min-height: 400px;
    }
    
    .id-cell {
      width: 30%;
    }
    
    .type-cell {
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