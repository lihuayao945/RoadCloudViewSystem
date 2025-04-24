<template>
  <div class="chart-container" ref="chartContainer">
    <!-- 左上角放大按钮 -->
    <div class="expand-button" @click="expandTopology" title="查看大图">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M15 3H21V9" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M9 21H3V15" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M21 3L14 10" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M3 21L10 14" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div>
    
    <!-- 弹窗组件 -->
    <teleport to="body">
      <div class="topology-modal" v-if="showExpandedView" @click.self="closeExpandedView">
        <div class="modal-content">
          <div class="modal-header">
            <div class="header-left">
              <h3>{{ internalViewType === 'hierarchy' ? '设备层级拓扑' : '设备区域分布' }}</h3>
              <div class="tag-controls">
                <div class="tag" :class="{ 'active': internalViewType === 'hierarchy' }" @click="switchViewInModal('hierarchy')">层级视图</div>
                <div class="tag" :class="{ 'active': internalViewType === 'district' }" @click="switchViewInModal('district')">区域分布</div>
              </div>
            </div>
            <div class="header-right">
              <!-- 修改：更新控制工具图标和功能 -->
              <div class="chart-controls">
                <button class="control-btn" @click="zoomIn" title="放大">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="11" cy="11" r="8" stroke="#00FFFF" stroke-width="2"/>
                    <line x1="19" y1="19" x2="16" y2="16" stroke="#00FFFF" stroke-width="2" stroke-linecap="round"/>
                    <line x1="11" y1="8" x2="11" y2="14" stroke="#00FFFF" stroke-width="2" stroke-linecap="round"/>
                    <line x1="8" y1="11" x2="14" y2="11" stroke="#00FFFF" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </button>
                <button class="control-btn" @click="zoomOut" title="缩小">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="11" cy="11" r="8" stroke="#00FFFF" stroke-width="2"/>
                    <line x1="19" y1="19" x2="16" y2="16" stroke="#00FFFF" stroke-width="2" stroke-linecap="round"/>
                    <line x1="8" y1="11" x2="14" y2="11" stroke="#00FFFF" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </button>
                <button class="control-btn" @click="resetChart" title="重置">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M21 12C21 16.97 16.97 21 12 21C7.03 21 3 16.97 3 12C3 7.03 7.03 3 12 3" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M16 3L12 7L8 3" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <button class="control-btn" @click="saveImage" title="保存图片">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 19V5C3 3.89543 3.89543 3 5 3H19C20.1046 3 21 3.89543 21 5V19C21 20.1046 20.1046 21 19 21H5C3.89543 21 3 20.1046 3 19Z" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M8.5 10C9.32843 10 10 9.32843 10 8.5C10 7.67157 9.32843 7 8.5 7C7.67157 7 7 7.67157 7 8.5C7 9.32843 7.67157 10 8.5 10Z" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M21 15L16 10L5 21" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <!-- 修改：显隐节点标签按钮图标 -->
                <button class="control-btn" @click="toggleLabels" :class="{ active: showLabels }" title="显示/隐藏标签">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M17 3H7C5.89543 3 5 3.89543 5 5V19C5 20.1046 5.89543 21 7 21H17C18.1046 21 19 20.1046 19 19V5C19 3.89543 18.1046 3 17 3Z" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M9 7H15" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M9 11H15" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M9 15H13" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
              <button class="close-btn" @click="closeExpandedView">×</button>
            </div>
          </div>
          <div class="modal-body">
            <!-- 新增：搜索与过滤组件 - 只在区域分布视图显示 -->
            <div class="expanded-chart-toolbar" v-if="internalViewType === 'district'">
              <div class="search-filter">
                <input 
                  type="text" 
                  class="search-input" 
                  v-model="searchKeyword" 
                  placeholder="搜索设备ID" 
                  @input="handleSearch"
                  @keydown.enter="applySearch"
                />
                <button class="search-btn" @click="applySearch">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M11 19C15.4183 19 19 15.4183 19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19Z" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M21 21L16.65 16.65" stroke="#00FFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
              <div class="filters">
                <div 
                  v-for="(filter, idx) in categoryFilters" 
                  :key="idx" 
                  class="filter-tag" 
                  :class="{ active: filter.active }"
                  @click="toggleFilter(idx)"
                >
                  {{ filter.name }}
                </div>
              </div>
            </div>
            
            <div class="expanded-chart-wrapper" ref="expandedChartContainer"></div>
            
            <!-- 新增：悬浮节点信息栏 -->
            <div class="node-info-panel" v-if="selectedNode">
              <div class="info-header">
                <h4>{{ selectedNode.name }}</h4>
                <button class="close-info" @click="selectedNode = null">×</button>
              </div>
              <div class="info-content">
                <div class="info-item" v-if="selectedNode.category !== undefined">
                  <span class="label">类型:</span>
                  <span class="value">{{ getCategoryName(selectedNode.category) }}</span>
                </div>
                <div class="info-item" v-if="selectedNode.lng && selectedNode.lat">
                  <span class="label">经纬度:</span>
                  <span class="value">{{ selectedNode.lng }}, {{ selectedNode.lat }}</span>
                </div>
                <div class="info-item" v-if="selectedNode.value !== undefined">
                  <span class="label">设备数:</span>
                  <span class="value">{{ selectedNode.value || '0' }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 新增：状态栏 -->
          <div class="modal-footer">
            <div class="status-info">
              <span class="zoom-level">缩放比例: {{ Math.round(zoomLevel * 100) }}%</span>
              <span class="nodes-count" v-if="nodes.length">节点数: {{ nodes.length }}</span>
            </div>
            <div class="legend-container">
              <!-- 图例将由echarts自动渲染 -->
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

export default {
  name: 'DeviceTopologyChart',
  props: {
    viewType: {
      type: String,
      default: 'hierarchy'
    }
  },
  emits: ['update:viewType'],
  setup(props, { emit }) {
    const chartContainer = ref(null)
    const expandedChartContainer = ref(null)
    let chart = null
    let expandedChart = null
    const topologyData = ref(null)
    const showExpandedView = ref(false) // 控制弹窗显示
    const isChartInitialized = ref(false) // 跟踪图表是否已初始化
    let initializationAttempts = 0 // 跟踪初始化尝试次数
    let isInitializing = false // 防止重复初始化
    const showLabels = ref(true) // 控制节点标签显示
    const zoomLevel = ref(1) // 当前缩放级别
    const searchKeyword = ref('') // 搜索关键词
    const selectedNode = ref(null) // 选中的节点信息
    const nodes = ref([]) // 保存所有节点数据
    
    // 新增：内部视图类型状态，用于独立管理模态框的视图类型
    const internalViewType = ref(props.viewType)
    
    // 过滤器设置
    const categoryFilters = ref([
      { name: '省级', active: true },
      { name: '区县级', active: true },
      { name: '地标', active: true },
      { name: 'RCU设备', active: true }
    ])
    
    // 监听视图类型变化
    watch(() => props.viewType, (newType) => {
      console.log('外部视图类型变化:', newType)
      // 同步更新内部视图类型
      internalViewType.value = newType
      
      if (chart) {
        // 确保有数据，然后切换视图
        if (!topologyData.value) {
          fetchTopologyData().then(() => {
            switchView(newType)
          }).catch(err => {
            console.error('获取拓扑数据出错:', err)
            // 显示错误提示
            showToast('获取拓扑数据失败，使用默认数据', 'error')
            useDefaultTopologyData()
            switchView(newType)
          })
        } else {
          switchView(newType)
        }
      }
    })
    
    // 监听内部视图类型变化
    watch(() => internalViewType.value, (newType) => {
      console.log('内部视图类型变化:', newType)
      // 当模态框打开时，根据内部视图类型更新模态框中的图表
      if (showExpandedView.value && expandedChart) {
        if (newType === 'hierarchy') {
          renderExpandedHierarchyView()
        } else {
          renderExpandedDistrictView()
        }
      }
    })
    
    onMounted(() => {
      console.log('拓扑图组件已挂载')
      window.addEventListener('resize', handleResize)
      // 确保组件挂载后初始化图表
      initChart()
      fetchTopologyData().then(() => {
        switchView(props.viewType)
      }).catch(err => {
        console.error('获取拓扑数据出错:', err)
        // 显示错误提示
        showToast('获取拓扑数据失败，使用默认数据', 'error')
        useDefaultTopologyData()
        switchView(props.viewType)
      })
    })
    
    onBeforeUnmount(() => {
      if (chart) {
        chart.dispose()
        chart = null
      }
      if (expandedChart) {
        expandedChart.dispose()
        expandedChart = null
      }
      window.removeEventListener('resize', handleResize)
      
      // 确保关闭弹窗
      if (showExpandedView.value) {
        closeExpandedView()
      }
    })
    
    function showToast(message, type = 'info', duration = 2000) {
      // 创建自定义事件来显示提示
      window.dispatchEvent(new CustomEvent('show-toast', {
        detail: { message, type, duration }
      }))
    }
    
    // 获取分类名称
    function getCategoryName(categoryIndex) {
      const categories = ['省级', '区县级', '地标', 'RCU设备']
      return categories[categoryIndex] || '未知'
    }
    
    // 切换过滤器
    function toggleFilter(idx) {
      categoryFilters.value[idx].active = !categoryFilters.value[idx].active
      applyFiltersAndSearch()
      
      // 显示状态提示
      const filterName = categoryFilters.value[idx].name
      const status = categoryFilters.value[idx].active ? '显示' : '隐藏'
      showToast(`${status} ${filterName} 节点`, 'info')
    }
    
    // 处理搜索输入
    function handleSearch() {
      // 可以在此处添加输入防抖逻辑
    }
    
    // 应用搜索和过滤条件
    function applyFiltersAndSearch() {
      if (!expandedChart) return
      
      // 重新渲染当前视图类型的图表，应用过滤和搜索
      if (internalViewType.value === 'hierarchy') {
        renderExpandedHierarchyView(true) // 传入应用过滤参数
      } else {
        renderExpandedDistrictView(true) // 传入应用过滤参数
      }
    }
    
    // 应用搜索
    function applySearch() {
      applyFiltersAndSearch()
      
      // 如果有搜索词，显示提示
      if (searchKeyword.value.trim()) {
        showToast(`搜索: ${searchKeyword.value}`, 'info')
      }
    }
    
    // 显示/隐藏标签
    function toggleLabels() {
      showLabels.value = !showLabels.value
      
      if (expandedChart) {
        const option = expandedChart.getOption()
        
        // 更新图表系列的label显示
        option.series.forEach(series => {
          series.label = {
            ...series.label,
            show: showLabels.value
          }
        })
        
        expandedChart.setOption(option)
      }
    }
    
    // 放大图表 - 修复实现
    function zoomIn() {
      if (!expandedChart) return
      
      zoomLevel.value = Math.min(zoomLevel.value + 0.2, 3) // 限制最大放大3倍
      
      const option = expandedChart.getOption()
      if (internalViewType.value === 'hierarchy') {
        // 树图缩放
        option.series[0].zoom = zoomLevel.value
      } else {
        // 关系图缩放
        option.series[0].zoom = zoomLevel.value
      }
      
      expandedChart.setOption(option)
      showToast(`放大至 ${Math.round(zoomLevel.value * 100)}%`, 'info')
    }
    
    // 缩小图表 - 修复实现
    function zoomOut() {
      if (!expandedChart) return
      
      zoomLevel.value = Math.max(zoomLevel.value - 0.2, 0.5) // 限制最小缩小0.5倍
      
      const option = expandedChart.getOption()
      if (internalViewType.value === 'hierarchy') {
        // 树图缩放
        option.series[0].zoom = zoomLevel.value
      } else {
        // 关系图缩放
        option.series[0].zoom = zoomLevel.value
      }
      
      expandedChart.setOption(option)
      showToast(`缩小至 ${Math.round(zoomLevel.value * 100)}%`, 'info')
    }
    
    // 重置图表 - 修复实现
    function resetChart() {
      if (!expandedChart) return
      
      zoomLevel.value = 1 // 重置缩放级别
      
      // 清空搜索和重置过滤器
      searchKeyword.value = ''
      categoryFilters.value.forEach(filter => {
        filter.active = true
      })
      
      // 清除选中状态
      selectedNode.value = null
      
      // 重新渲染
      if (internalViewType.value === 'hierarchy') {
        renderExpandedHierarchyView()
      } else {
        renderExpandedDistrictView()
      }
      
      showToast('图表已重置', 'info')
    }
    
    // 保存图片 - 确保功能正确
    function saveImage() {
      if (!expandedChart) return
      
      try {
        const url = expandedChart.getDataURL({
          type: 'png',
          pixelRatio: 2,
          backgroundColor: 'rgba(4, 12, 32, 0.95)'
        })
        
        // 创建下载链接
        const link = document.createElement('a')
        link.download = `设备拓扑图_${new Date().toLocaleDateString()}.png`
        link.href = url
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        
        showToast('图片已保存', 'success')
      } catch (error) {
        console.error('保存图片失败:', error)
        showToast('保存图片失败', 'error')
      }
    }
    
    // 展开拓扑图到弹窗
    function expandTopology() {
      console.log('展开拓扑图按钮被点击')
      
      // 防止重复点击
      if (isInitializing) {
        console.log('初始化正在进行中，忽略重复点击')
        return
      }
      
      isInitializing = true
      initializationAttempts = 0 // 重置尝试次数
      
      // 重置缩放级别
      zoomLevel.value = 1
      
      // 清空搜索关键词
      searchKeyword.value = ''
      
      // 重置过滤器
      categoryFilters.value.forEach(filter => {
        filter.active = true
      })
      
      // 重置选中节点
      selectedNode.value = null
      
      // 同步内部视图类型与当前显示的视图类型
      internalViewType.value = props.viewType
      
      // 显示成功提示
      showToast('正在加载拓扑大图...', 'info', 2000)
      
      // 先设置标志，然后等DOM更新后初始化图表
      showExpandedView.value = true
      
      // 使用nextTick确保DOM已更新
      nextTick(() => {
        // 给更长时间让DOM渲染完成
        setTimeout(() => {
          try {
            initExpandedChart()
          } catch (error) {
            console.error('初始化扩展图表失败:', error)
            showToast('加载拓扑大图失败，请重试', 'error', 3000)
            closeExpandedView()
          } finally {
            isInitializing = false
          }
        }, 300) // 稍微减少延迟时间
      })
    }
    
    // 关闭弹窗
    function closeExpandedView() {
      if (expandedChart) {
        try {
          expandedChart.dispose()
        } catch (error) {
          console.error('销毁扩展图表失败:', error)
        }
        expandedChart = null
      }
      
      showExpandedView.value = false
      isInitializing = false
      selectedNode.value = null
    }
    
    // 在模态框中切换视图
    function switchViewInModal(viewType) {
      // 更新内部视图类型
      internalViewType.value = viewType
      
      // 同步更新父组件的viewType
      emit('update:viewType', viewType)
      
      // 重置搜索和选中状态
      searchKeyword.value = ''
      selectedNode.value = null
      zoomLevel.value = 1
      
      // 显示提示
      showToast(`切换至${viewType === 'hierarchy' ? '层级视图' : '区域分布视图'}`, 'info')
    }
    
    // 初始化扩展图表
    function initExpandedChart() {
      if (!expandedChartContainer.value) {
        console.error('扩展图表容器未找到')
        showToast('加载拓扑大图失败，未找到容器', 'error')
        closeExpandedView()
        return
      }

      // 检查容器尺寸
      const containerWidth = expandedChartContainer.value.clientWidth
      const containerHeight = expandedChartContainer.value.clientHeight
      
      console.log('尝试初始化扩展图表，容器尺寸:', containerWidth, containerHeight)
      
      if (containerWidth <= 0 || containerHeight <= 0) {
        initializationAttempts++;
        console.warn(`扩展图表容器尺寸无效，尝试重新计算 ${containerWidth} ${containerHeight}，尝试次数: ${initializationAttempts}`)
        
        if (initializationAttempts > 3) { // 减少最大尝试次数
          console.error('多次尝试后仍无法获取有效容器尺寸，放弃初始化')
          showToast('加载拓扑大图失败，请重试', 'error', 3000)
          closeExpandedView()
          return
        }
        
        // 强制设置一个尺寸
        expandedChartContainer.value.style.width = '100%'
        expandedChartContainer.value.style.height = '600px'
        expandedChartContainer.value.style.minHeight = '600px'
        
        // 再次尝试初始化，给额外延迟
        setTimeout(() => {
          initExpandedChart()
        }, 300)
        return
      }
      
      console.log('成功初始化扩展图表，容器尺寸:', containerWidth, containerHeight)
      
      try {
        if (expandedChart) {
          expandedChart.dispose()
        }
        
        expandedChart = echarts.init(expandedChartContainer.value)
        
        // 添加点击事件监听器
        expandedChart.on('click', function(params) {
          if (params.dataType === 'node') {
            selectedNode.value = params.data
            console.log('选中节点:', selectedNode.value)
          } else {
            // 点击空白区域，清除选中
            selectedNode.value = null
          }
        })
        
        // 先设置一个简单的加载动画
        expandedChart.showLoading({
          text: '加载中...',
          color: '#00ffff',
          textColor: '#00ffff',
          maskColor: 'rgba(4, 12, 32, 0.8)',
          zlevel: 0
        });
        
        // 根据当前内部视图类型渲染扩展图表
        if (internalViewType.value === 'hierarchy') {
          renderExpandedHierarchyView()
        } else {
          renderExpandedDistrictView()
        }
        
        // 渲染完成后隐藏加载动画
        expandedChart.hideLoading();
        
        // 添加窗口调整大小事件监听器
        window.addEventListener('resize', handleExpandedResize)
      } catch (error) {
        console.error('初始化扩展图表时出错:', error)
        showToast('加载拓扑大图失败，请重试', 'error')
        closeExpandedView()
      }
    }
    
    // 处理扩展图表的窗口调整大小
    function handleExpandedResize() {
      if (expandedChart && showExpandedView.value) {
        expandedChart.resize()
      }
    }
    
    async function fetchTopologyData() {
      try {
        console.log('正在获取拓扑数据...')
        const response = await axios.get('/menu/top/list', {
          params: {
            provincename: 'chongqing'
          }
        })
        
        if (response.data && response.data.status === 'success') {
          topologyData.value = response.data
          console.log('拓扑数据获取成功:', topologyData.value)
          
          // 如果图表已初始化，则更新视图
          if (chart) {
            switchView(props.viewType)
          } else {
            // 确保图表已初始化
            initChart()
          }
          return topologyData.value
        } else {
          console.error('获取拓扑数据失败:', response.data)
          // 使用默认数据
          return useDefaultTopologyData()
        }
      } catch (error) {
        console.error('获取拓扑数据出错:', error)
        // 使用默认数据
        return useDefaultTopologyData()
      }
    }
    
    function useDefaultTopologyData() {
      console.log('使用默认拓扑数据')
      // 设置默认拓扑数据
      topologyData.value = {
        provicename: "重庆市",
        data: {
          provicename: "重庆市",
          lng: 106.551557,
          lat: 29.563009,
          children: [
            {
              city: "渝中区",
              lng: 106.571746,
              lat: 29.557404,
              children: [
                {
                  landmark: "解放碑",
                  lng: 106.573347,
                  lat: 29.558964,
                  children: [
                    {
                      rcuId: "U-WZ000R",
                      lng: 106.5736,
                      lat: 29.5592
                    },
                    {
                      rcuId: "U-WZ000S",
                      lng: 106.5737,
                      lat: 29.5593
                    }
                  ]
                },
                {
                  landmark: "临江",
                  lng: 106.578185,
                  lat: 29.562458,
                  children: [
                    {
                      rcuId: "U-WZ0012",
                      lng: 106.5784,
                      lat: 29.5627
                    },
                    {
                      rcuId: "U-WZ0013",
                      lng: 106.5786,
                      lat: 29.5629
                    },
                    {
                      rcuId: "U-WZ0015",
                      lng: 106.5783,
                      lat: 29.5625
                    }
                  ]
                }
              ]
            },
            {
              city: "沙坪坝区",
              lng: 106.463656,
              lat: 29.555125,
              children: [
                {
                  landmark: "三峡广场",
                  lng: 106.463021,
                  lat: 29.555384,
                  children: [
                    {
                      rcuId: "U-WZ0019",
                      lng: 106.4633,
                      lat: 29.55555
                    },
                    {
                      rcuId: "U-WZ001B",
                      lng: 106.4634,
                      lat: 29.5556
                    }
                  ]
                },
                {
                  landmark: "谢家湾",
                  lng: 106.46244,
                  lat: 29.552685,
                  children: [
                    {
                      rcuId: "U-WZ004K",
                      lng: 106.4627,
                      lat: 29.55295
                    }
                  ]
                }
              ]
            },
            {
              city: "九龙坡区",
              lng: 106.530658,
              lat: 29.487746,
              children: [
                {
                  landmark: "石坪桥",
                  lng: 106.53084,
                  lat: 29.48806,
                  children: [
                    {
                      rcuId: "U-ZS000U",
                      lng: 106.5311,
                      lat: 29.4883
                    },
                    {
                      rcuId: "U-ZS0013",
                      lng: 106.5312,
                      lat: 29.4884
                    }
                  ]
                }
              ]
            }
          ]
        },
        status: "success"
      }
      
      // 如果图表已初始化，则更新视图
      if (chart) {
        switchView(props.viewType)
      } else {
        // 确保图表已初始化
        initChart()
      }
      
      return topologyData.value
    }
    
    function handleResize() {
      if (chart) {
        chart.resize()
      }
    }
    
    function initChart() {
      console.log('初始化图表')
      if (chartContainer.value) {
        if (chart) {
          chart.dispose()
        }
        
        try {
          chart = echarts.init(chartContainer.value)
          
          // 添加图表点击事件监听器
          chart.on('click', function(params) {
            console.log('图表点击事件:', params)
          });
          
          // 标记图表已初始化
          isChartInitialized.value = true;
          
          // 初始化时如果已有数据则渲染图表
          if (topologyData.value) {
            switchView(props.viewType)
          } else {
            // 否则先获取数据
            fetchTopologyData()
          }
        } catch (error) {
          console.error('初始化图表失败:', error)
          isChartInitialized.value = false;
          showToast('初始化图表失败，请刷新页面', 'error')
        }
      } else {
        console.error('图表容器未找到')
      }
    }
    
    function switchView(viewType) {
      console.log('切换视图:', viewType)
      if (!chart) {
        console.error('图表未初始化，无法切换视图')
        initChart()
        return
      }
      
      if (!topologyData.value) {
        console.warn('拓扑数据未加载，先获取数据')
        fetchTopologyData().then(() => {
          if (viewType === 'hierarchy') {
            renderHierarchyView()
          } else {
            renderDistrictView()
          }
        }).catch(err => {
          console.error('获取拓扑数据出错:', err)
          useDefaultTopologyData()
          if (viewType === 'hierarchy') {
            renderHierarchyView()
          } else {
            renderDistrictView()
          }
        })
        return
      }
      
      // 确保有清晰的视图切换日志
      console.log(`准备渲染视图: ${viewType}`, topologyData.value)
      
      try {
        if (viewType === 'hierarchy') {
          renderHierarchyView()
        } else {
          renderDistrictView()
        }
        
        // 如果扩展视图正在显示，并且内部视图类型与切换的视图类型不同，则同步内部视图类型
        if (showExpandedView.value && expandedChart && internalViewType.value !== viewType) {
          internalViewType.value = viewType
        }
      } catch (error) {
        console.error('切换视图失败:', error)
        showToast('切换视图失败，请重试', 'error')
      }
    }

    // 层级视图 - 树形结构展示 (显示具体设备)
    function renderHierarchyView() {
      console.log('渲染层级视图')
      if (!topologyData.value || !topologyData.value.data) {
        console.warn('无法渲染层级视图，数据不完整')
        return
      }
      
      // 转换API数据为树形结构
      const apiData = topologyData.value.data
      
      // 创建层级数据 - 确保包含所有的RCU设备
      const data = {
        name: apiData.provicename || '重庆市',
        children: []
      }
      
      // 添加区县级别
      if (apiData.children && apiData.children.length > 0) {
        apiData.children.forEach(district => {
          const districtNode = {
            name: district.city,
            value: 0,
            children: []
          }
          
          // 添加地标级别
          if (district.children && district.children.length > 0) {
            district.children.forEach(landmark => {
              const landmarkNode = {
                name: landmark.landmark,
                value: 0,
                children: []
              }
              
              // 添加RCU设备
              if (landmark.children && landmark.children.length > 0) {
                landmarkNode.value = landmark.children.length
                landmark.children.forEach(rcu => {
                  landmarkNode.children.push({
                    name: rcu.rcuId,
                    value: 1
                  })
                })
              }
              
              districtNode.value += landmarkNode.value
              districtNode.children.push(landmarkNode)
            })
          }
          
          data.children.push(districtNode)
        })
      }
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: function (params) {
            const value = params.data.value
            if (value) {
              return `${params.name}: ${value} 台设备`
            }
            return params.name
          },
          backgroundColor: 'rgba(8, 24, 44, 0.9)',
          borderColor: 'rgba(0, 181, 255, 0.5)',
          borderWidth: 1,
          padding: [8, 10],
          textStyle: {
            color: '#ffffff'
          }
        },
        series: [
          {
            type: 'tree',
            data: [data],
            top: '5%',
            left: '20%',
            bottom: '5%',
            right: '15%',
            zoom: 0.8,
            roam: true,
            // 修改symbolSize，避免使用treePathInfo
            symbolSize: function(value, params) {
              // 根据数据的深度确定节点大小
              // 深度从0开始：0=根节点，1=区县，2=地标，3=设备
              const depth = params.data.depth || 0;
              const sizes = [16, 14, 12, 10];
              return sizes[Math.min(depth, sizes.length - 1)];
            },
            // 修改itemStyle，避免使用treePathInfo
            itemStyle: {
              color: function(params) {
                // 根据数据的深度确定节点颜色
                const depth = params.data.depth || 0;
                const colors = ['#1A73E8', '#00B0FF', '#7EFFED', '#00FFAA'];
                return colors[Math.min(depth, colors.length - 1)];
              },
              borderColor: 'rgba(255, 255, 255, 0.5)',
              borderWidth: 1,
              shadowColor: 'rgba(0, 181, 255, 0.5)',
              shadowBlur: 8
            },
            label: {
              position: 'left',
              verticalAlign: 'middle',
              align: 'right',
              fontSize: 12,
              color: '#fff',
              distance: 5,
              backgroundColor: 'rgba(8, 24, 44, 0.6)',
              padding: [3, 5],
              borderRadius: 3
            },
            leaves: {
              label: {
                position: 'right',
                verticalAlign: 'middle',
                align: 'left'
              }
            },
            emphasis: {
              focus: 'descendant',
              itemStyle: {
                borderWidth: 2,
                shadowBlur: 15,
                shadowColor: 'rgba(0, 255, 255, 0.8)'
              },
              label: {
                color: '#00ffff',
                fontWeight: 'bold'
              }
            },
            initialTreeDepth: 3, // 默认展开3级，显示具体设备
            lineStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: 'rgba(0, 176, 255, 0.7)' },
                  { offset: 1, color: 'rgba(0, 210, 255, 0.7)' }
                ]
              },
              width: 1.5,
              curveness: 0.5,
              shadowColor: 'rgba(0, 181, 255, 0.3)',
              shadowBlur: 5
            },
            expandAndCollapse: true,
            animationDuration: 550,
            animationDurationUpdate: 750
          }
        ]
      }
      
      // 在将数据传递给ECharts前，为每个节点添加深度信息
      function addDepthInfo(node, depth = 0) {
        node.depth = depth;
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => addDepthInfo(child, depth + 1));
        }
        return node;
      }
      
      // 添加深度信息
      addDepthInfo(data);
      
      chart.setOption(option, true);
      console.log('层级视图渲染完成')
    }
    
    // 扩展层级视图 - 增强版树形结构展示
    function renderExpandedHierarchyView(applyFilters = false) {
      console.log('渲染扩展层级视图，应用过滤:', applyFilters)
      if (!topologyData.value || !topologyData.value.data || !expandedChart) {
        console.warn('无法渲染扩展层级视图，数据不完整或图表未初始化')
        return
      }
      
      // 复用相同的数据处理逻辑
      const apiData = topologyData.value.data
      
      // 创建层级数据
      const data = {
        name: apiData.provicename || '重庆市',
        children: [],
        category: 0, // 为根节点添加分类信息
        visible: true // 控制节点显示与否
      }
      
      // 根节点显示控制 - 受省级过滤器控制
      if (applyFilters) {
        data.visible = categoryFilters.value[0].active
      }
      
      // 搜索函数 - 检查节点是否匹配搜索关键词
      const matchesSearch = (node) => {
        if (!searchKeyword.value.trim()) return true
        return node.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
      }
      
      // 添加区县级别
      if (apiData.children && apiData.children.length > 0) {
        apiData.children.forEach(district => {
          const districtNode = {
            name: district.city,
            value: 0,
            children: [],
            category: 1, // 为节点添加分类信息
            lng: district.lng,
            lat: district.lat,
            visible: true // 默认显示
          }
          
          // 应用区县级过滤
          if (applyFilters) {
            districtNode.visible = categoryFilters.value[1].active
          }
          
          // 检查搜索匹配
          const districtMatchesSearch = matchesSearch(districtNode)
          
          // 添加地标级别
          if (district.children && district.children.length > 0) {
            district.children.forEach(landmark => {
              const landmarkNode = {
                name: landmark.landmark,
                value: 0,
                children: [],
                category: 2, // 为节点添加分类信息
                lng: landmark.lng,
                lat: landmark.lat,
                visible: true // 默认显示
              }
              
              // 应用地标级过滤
              if (applyFilters) {
                landmarkNode.visible = categoryFilters.value[2].active
              }
              
              // 检查搜索匹配
              const landmarkMatchesSearch = matchesSearch(landmarkNode)
              
              // 添加RCU设备
              if (landmark.children && landmark.children.length > 0) {
                landmark.children.forEach(rcu => {
                  const rcuNode = {
                    name: rcu.rcuId,
                    value: 1,
                    category: 3, // 为节点添加分类信息
                    lng: rcu.lng,
                    lat: rcu.lat,
                    visible: true // 默认显示
                  }
                  
                  // 应用RCU设备级过滤
                  if (applyFilters) {
                    rcuNode.visible = categoryFilters.value[3].active
                  }
                  
                  // 检查搜索匹配
                  const rcuMatchesSearch = matchesSearch(rcuNode)
                  
                  // 如果RCU节点匹配搜索或搜索为空，添加此节点
                  if ((rcuMatchesSearch || !searchKeyword.value.trim()) && (rcuNode.visible || !applyFilters)) {
                    landmarkNode.children.push(rcuNode)
                    // 只计算可见子节点
                    if (rcuNode.visible || !applyFilters) {
                      landmarkNode.value++
                    }
                  }
                })
              }
              
              // 检查是否应该添加此地标节点
              const shouldAddLandmark = (landmarkMatchesSearch || 
                                        !searchKeyword.value.trim() || 
                                        landmarkNode.children.length > 0) &&
                                        (landmarkNode.visible || !applyFilters);
              
              if (shouldAddLandmark) {
                districtNode.children.push(landmarkNode)
                // 只计算可见子节点的值
                if (landmarkNode.visible || !applyFilters) {
                  districtNode.value += landmarkNode.value
                }
              }
            })
          }
          
          // 检查是否应该添加此区县节点
          const shouldAddDistrict = (districtMatchesSearch || 
                                    !searchKeyword.value.trim() || 
                                    districtNode.children.length > 0) &&
                                    (districtNode.visible || !applyFilters);
          
          if (shouldAddDistrict) {
            data.children.push(districtNode)
          }
        })
      }
      
      // 保存节点信息以供UI展示
      nodes.value = countAllVisibleNodes(data)
      
      // 重要：创建所有可能的树结构，然后通过节点显隐来控制过滤效果
      function prepareNodesForFiltering(treeData) {
        // 递归处理每个节点
        function processNode(node) {
          // 处理当前节点的子节点
          if (node.children && node.children.length > 0) {
            node.children.forEach(child => {
              processNode(child)
            })
          }
        }
        
        processNode(treeData)
        return treeData
      }
      
      const processedData = prepareNodesForFiltering(data)
      
      // 扩展视图配置
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: function (params) {
            const data = params.data
            if (data.value) {
              let content = `<div style="font-weight:bold;color:#00ffff;margin-bottom:5px;">${params.name}</div>`
              content += `设备数量: ${data.value} 台<br>`
              
              if (data.lng && data.lat) {
                content += `位置: ${data.lng.toFixed(4)}, ${data.lat.toFixed(4)}<br>`
              }
              
              if (data.category !== undefined) {
                content += `类型: ${getCategoryName(data.category)}`
              }
              
              return content
            }
            return params.name
          },
          backgroundColor: 'rgba(8, 24, 44, 0.9)',
          borderColor: 'rgba(0, 181, 255, 0.5)',
          borderWidth: 1,
          padding: [8, 10],
          textStyle: {
            color: '#ffffff'
          }
        },
        legend: {
          data: categoryFilters.value.map(filter => filter.name),
          textStyle: {
            color: '#fff',
            fontSize: 12
          },
          bottom: 10,
          left: 'center',
          itemWidth: 15,
          itemHeight: 10,
          itemGap: 20,
          padding: [8, 10],
          backgroundColor: 'rgba(8, 24, 44, 0.5)',
          borderColor: 'rgba(0, 181, 255, 0.3)',
          borderWidth: 1,
          borderRadius: 4,
          selectedMode: false // 禁用图例切换，使用自定义过滤器
        },
        series: [
          {
            type: 'tree',
            data: [processedData],
            top: '5%',
            left: '15%',
            bottom: '10%', // 留出底部空间给图例
            right: '15%',
            zoom: zoomLevel.value, // 使用保存的缩放级别
            roam: true,
            symbolSize: function(value, params) {
              const depth = params.data.depth || 0;
              const sizes = [20, 16, 14, 12]; // 增大一些节点尺寸
              return sizes[Math.min(depth, sizes.length - 1)];
            },
            itemStyle: {
              color: function(params) {
                // 根据数据的分类确定节点颜色
                const category = params.data.category !== undefined ? params.data.category : params.data.depth || 0;
                const colors = ['#1A73E8', '#00B0FF', '#7EFFED', '#00FFAA'];
                return colors[Math.min(category, colors.length - 1)];
              },
              borderColor: 'rgba(255, 255, 255, 0.5)',
              borderWidth: 1,
              shadowColor: 'rgba(0, 181, 255, 0.5)',
              shadowBlur: 8
            },
            label: {
              show: showLabels.value,
              position: 'left',
              verticalAlign: 'middle',
              align: 'right',
              fontSize: 14, // 增大字体
              color: '#fff',
              distance: 5,
              backgroundColor: 'rgba(8, 24, 44, 0.6)',
              padding: [3, 5],
              borderRadius: 3
            },
            leaves: {
              label: {
                position: 'right',
                verticalAlign: 'middle',
                align: 'left'
              }
            },
            emphasis: {
              focus: 'descendant',
              itemStyle: {
                borderWidth: 2,
                shadowBlur: 15,
                shadowColor: 'rgba(0, 255, 255, 0.8)'
              },
              label: {
                color: '#00ffff',
                fontWeight: 'bold',
                fontSize: 16
              }
            },
            initialTreeDepth: searchKeyword.value.trim() ? 5 : 2, // 如果在搜索则展开所有层级
            lineStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: 'rgba(0, 176, 255, 0.7)' },
                  { offset: 1, color: 'rgba(0, 210, 255, 0.7)' }
                ]
              },
              width: 2, // 稍微加粗线条
              curveness: 0.5,
              shadowColor: 'rgba(0, 181, 255, 0.3)',
              shadowBlur: 5
            },
            expandAndCollapse: true,
            animationDuration: 550,
            animationDurationUpdate: 750
          }
        ],
        backgroundColor: 'transparent' // 使用透明背景
      }
      
      // 在将数据传递给ECharts前，为每个节点添加深度信息
      function addDepthInfo(node, depth = 0) {
        node.depth = depth;
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => addDepthInfo(child, depth + 1));
        }
        return node;
      }
      
      // 添加深度信息
      addDepthInfo(processedData);
      
      expandedChart.setOption(option, true);
      console.log('扩展层级视图渲染完成，节点数:', nodes.value.length)
    }
    
    // 计算所有可见节点的辅助函数
    function countAllVisibleNodes(node) {
      let nodesList = []
      
      function traverse(node) {
        if (!node) return
        
        // 只统计可见节点
        if (node.visible !== false) {
          nodesList.push(node)
        }
        
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => traverse(child))
        }
      }
      
      traverse(node)
      return nodesList
    }
    
    // 区域分布视图 - 以区域为中心的关系图
    function renderDistrictView() {
      console.log('渲染区域分布视图')
      if (!topologyData.value || !topologyData.value.data) {
        console.warn('无法渲染区域分布视图，数据不完整')
        return
      }
      
      const apiData = topologyData.value.data
      
      // 分类 - 使用更有区分度的颜色
      const categories = [
        { name: '省级', itemStyle: { color: '#1A73E8' } },
        { name: '区县级', itemStyle: { color: '#00B0FF' } },
        { name: '地标', itemStyle: { color: '#00DDFF' } },
        { name: 'RCU设备', itemStyle: { color: '#00FFAA' } }
      ]
      
      const nodes = []
      const links = []
      
      // 添加省级节点
      nodes.push({
        id: '0',
        name: apiData.provicename,
        symbolSize: 40,
        category: 0,
        x: 0,
        y: 0,
        // 使用真实经纬度作为定位参考
        lng: apiData.lng,
        lat: apiData.lat,
        symbol: 'circle',
        itemStyle: {
          shadowColor: 'rgba(0, 115, 232, 0.9)',
          shadowBlur: 20
        }
      })
      
      // 添加区县节点、地标节点和RCU节点
      if (apiData.children && apiData.children.length > 0) {
        apiData.children.forEach((district, dIndex) => {
          const districtId = `d${dIndex}`
          
          // 添加区县节点
          nodes.push({
            id: districtId,
            name: district.city,
            symbolSize: 30,
            category: 1,
            lng: district.lng,
            lat: district.lat,
            symbol: 'circle'
          })
          
          // 连接省级到区县
          links.push({
            source: '0',
            target: districtId,
            lineStyle: {
              width: 2,
              curveness: 0.2
            }
          })
          
          // 添加地标节点
          if (district.children && district.children.length > 0) {
            district.children.forEach((landmark, lIndex) => {
              const landmarkId = `${districtId}_l${lIndex}`
              
              // 添加地标节点
              nodes.push({
                id: landmarkId,
                name: landmark.landmark,
                symbolSize: 20,
                category: 2,
                lng: landmark.lng,
                lat: landmark.lat,
                symbol: 'roundRect'
              })
              
              // 连接区县到地标
              links.push({
                source: districtId,
                target: landmarkId,
                lineStyle: {
                  width: 1.5,
                  curveness: 0.1
                }
              })
              
              // 添加RCU设备节点
              if (landmark.children && landmark.children.length > 0) {
                landmark.children.forEach((rcu, rIndex) => {
                  const rcuId = `${landmarkId}_r${rIndex}`
                  
                  // 添加RCU设备节点
                  nodes.push({
                    id: rcuId,
                    name: rcu.rcuId,
                    symbolSize: 12,
                    category: 3,
                    lng: rcu.lng,
                    lat: rcu.lat,
                    symbol: 'diamond'
                  })
                  
                  // 连接地标到RCU设备
                  links.push({
                    source: landmarkId,
                    target: rcuId,
                    lineStyle: {
                      width: 1,
                      curveness: 0.05
                    }
                  })
                })
              }
            })
          }
        })
      }
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            if (params.dataType === 'node') {
              const data = params.data
              if (data.category === 3) {
                return `<div style="font-weight:bold;color:#00ffff;margin-bottom:5px;">${data.name}</div>
                         经度: ${data.lng}<br>纬度: ${data.lat}`
              }
              return `<div style="font-weight:bold;color:#00ffff;margin-bottom:5px;">${data.name}</div>`
            }
            return params.name
          },
          backgroundColor: 'rgba(8, 24, 44, 0.9)',
          borderColor: 'rgba(0, 181, 255, 0.5)',
          borderWidth: 1,
          padding: [8, 10],
          textStyle: {
            color: '#ffffff'
          }
        },
        legend: {
          data: categories.map(a => a.name),
          textStyle: {
            color: '#fff',
            fontSize: 12
          },
          top: 10,
          right: 10,
          orient: 'vertical',
          itemWidth: 14,
          itemHeight: 14,
          itemGap: 10,
          padding: 8,
          backgroundColor: 'rgba(8, 24, 44, 0.5)',
          borderColor: 'rgba(0, 181, 255, 0.3)',
          borderWidth: 1,
          borderRadius: 4,
          formatter: function(name) {
            // 截断太长的文字
            if (name.length > 6) {
              return name.substring(0, 6) + '...'
            }
            return name
          }
        },
        series: [
          {
            type: 'graph',
            layout: 'force',
            animation: true,
            label: {
              show: true,
              position: 'right',
              formatter: '{b}',
              fontSize: 12,
              color: '#fff',
              backgroundColor: 'rgba(8, 24, 44, 0.6)',
              padding: [3, 5],
              borderRadius: 3
            },
            roam: true,
            zoom: 0.8, // 使用缩放级别
            draggable: true,
            data: nodes,
            links: links,
            categories: categories,
            force: {
              edgeLength: [80, 150],
              repulsion: 250, // 调整斥力，使节点更分散
              gravity: 0.1,
              friction: 0.2
            },
            edgeSymbol: ['none', 'arrow'],
            edgeSymbolSize: [0, 8],
            lineStyle: {
              opacity: 0.7,
              width: 1.5,
              curveness: 0.2,
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: 'rgba(26, 115, 232, 0.7)' },
                  { offset: 1, color: 'rgba(0, 210, 255, 0.7)' }
                ]
              }
            },
            itemStyle: {
              borderColor: 'rgba(255, 255, 255, 0.8)',
              borderWidth: 1,
              shadowColor: 'rgba(0, 181, 255, 0.4)',
              shadowBlur: 10
            },
            emphasis: {
              lineStyle: {
                width: 3,
                color: 'rgba(0, 255, 255, 0.9)',
                shadowColor: 'rgba(0, 255, 255, 0.5)',
                shadowBlur: 15
              },
              itemStyle: {
                borderWidth: 2,
                borderColor: '#ffffff',
                shadowColor: 'rgba(0, 255, 255, 0.8)',
                shadowBlur: 15
              },
              label: {
                color: '#00ffff',
                fontWeight: 'bold'
              }
            }
          }
        ],
        backgroundColor: 'transparent'
      }
      
      chart.setOption(option, true);
      console.log('区域分布视图渲染完成')
    }
    
    // 扩展区域分布视图
    function renderExpandedDistrictView(applyFilters = false) {
      console.log('渲染扩展区域分布视图，应用过滤:', applyFilters)
      if (!topologyData.value || !topologyData.value.data || !expandedChart) {
        console.warn('无法渲染扩展区域分布视图，数据不完整或图表未初始化')
        return
      }
      
      const apiData = topologyData.value.data
      
      // 分类 - 使用更有区分度的颜色
      const categories = [
        { name: '省级', itemStyle: { color: '#1A73E8' } },
        { name: '区县级', itemStyle: { color: '#00B0FF' } },
        { name: '地标', itemStyle: { color: '#00DDFF' } },
        { name: 'RCU设备', itemStyle: { color: '#00FFAA' } }
      ]
      
      const nodes = []
      const links = []
      
      // 搜索函数 - 检查节点是否匹配搜索关键词
      const matchesSearch = (node) => {
        if (!searchKeyword.value.trim()) return true
        return node.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
      }
      
      // 添加省级节点 - 省级节点受省级过滤器控制
      if (!applyFilters || categoryFilters.value[0].active) {
        const rootNode = {
          id: '0',
          name: apiData.provicename,
          symbolSize: 60, // 扩展视图增大节点尺寸
          category: 0,
          x: 0,
          y: 0,
          lng: apiData.lng,
          lat: apiData.lat,
          symbol: 'circle',
          itemStyle: {
            shadowColor: 'rgba(0, 115, 232, 0.9)',
            shadowBlur: 20
          }
        }
        
        // 检查搜索条件
        if (matchesSearch(rootNode) || !searchKeyword.value.trim()) {
          nodes.push(rootNode)
        }
      }
      
      // 添加区县节点、地标节点和RCU节点
      if (apiData.children && apiData.children.length > 0) {
        apiData.children.forEach((district, dIndex) => {
          const districtId = `d${dIndex}`
          const districtNode = {
            id: districtId,
            name: district.city,
            symbolSize: 40, // 扩展视图增大节点尺寸
            category: 1,
            lng: district.lng,
            lat: district.lat,
            symbol: 'circle'
          }
          
          // 检查区县是否匹配搜索
          const districtMatchesSearch = matchesSearch(districtNode)
          
          // 确定是否应该添加区县节点 - 如果区县级过滤器关闭则不添加
          const shouldAddDistrict = (districtMatchesSearch || !searchKeyword.value.trim()) && 
                                   (!applyFilters || categoryFilters.value[1].active);
          
          if (shouldAddDistrict) {
            nodes.push(districtNode)
            
            // 只有当省级节点显示时才添加连接
            if (nodes.find(node => node.id === '0')) {
              links.push({
                source: '0',
                target: districtId,
                lineStyle: {
                  width: 3, // 扩展视图加粗线条
                  curveness: 0.2
                }
              })
            }
          }
          
          // 添加地标节点
          if (district.children && district.children.length > 0) {
            district.children.forEach((landmark, lIndex) => {
              const landmarkId = `${districtId}_l${lIndex}`
              const landmarkNode = {
                id: landmarkId,
                name: landmark.landmark,
                symbolSize: 24, // 扩展视图增大节点尺寸
                category: 2,
                lng: landmark.lng,
                lat: landmark.lat,
                symbol: 'roundRect'
              }
              
              // 检查地标是否匹配搜索
              const landmarkMatchesSearch = matchesSearch(landmarkNode)
              
              // 确定是否应该添加地标节点 - 如果地标级过滤器关闭则不添加
              const shouldAddLandmark = (landmarkMatchesSearch || !searchKeyword.value.trim()) && 
                                       (!applyFilters || categoryFilters.value[2].active);
              
              if (shouldAddLandmark) {
                nodes.push(landmarkNode)
                
                // 只有当区县节点显示时才添加连接
                if (nodes.find(node => node.id === districtId)) {
                  links.push({
                    source: districtId,
                    target: landmarkId,
                    lineStyle: {
                      width: 2, // 扩展视图加粗线条
                      curveness: 0.1
                    }
                  })
                }
              }
              
              // 添加RCU设备节点
              if (landmark.children && landmark.children.length > 0) {
                landmark.children.forEach((rcu, rIndex) => {
                  const rcuId = `${landmarkId}_r${rIndex}`
                  const rcuNode = {
                    id: rcuId,
                    name: rcu.rcuId,
                    symbolSize: 16, // 扩展视图增大节点尺寸
                    category: 3,
                    lng: rcu.lng,
                    lat: rcu.lat,
                    symbol: 'diamond'
                  }
                  
                  // 检查RCU是否匹配搜索
                  const rcuMatchesSearch = matchesSearch(rcuNode)
                  
                  // 确定是否应该添加RCU节点 - 如果RCU设备级过滤器关闭则不添加
                  const shouldAddRcu = (rcuMatchesSearch || !searchKeyword.value.trim()) && 
                                      (!applyFilters || categoryFilters.value[3].active);
                  
                  if (shouldAddRcu) {
                    nodes.push(rcuNode)
                    
                    // 只有当地标节点显示时才添加连接
                    if (nodes.find(node => node.id === landmarkId)) {
                      links.push({
                        source: landmarkId,
                        target: rcuId,
                        lineStyle: {
                          width: 1.5, // 扩展视图加粗线条
                          curveness: 0.05
                        }
                      })
                    }
                  }
                })
              }
            })
          }
        })
      }
      
      // 保存节点信息以供UI展示
      nodes.value = nodes
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            if (params.dataType === 'node') {
              const data = params.data
              let content = `<div style="font-weight:bold;color:#00ffff;margin-bottom:5px;">${data.name}</div>`
              
              if (data.lng && data.lat) {
                content += `位置: ${data.lng.toFixed(4)}, ${data.lat.toFixed(4)}<br>`
              }
              
              if (data.category !== undefined) {
                content += `类型: ${getCategoryName(data.category)}`
              }
              
              return content
            } else if (params.dataType === 'edge') {
              return `<div style="color:#8af2ff;">连接</div>${params.data.source} → ${params.data.target}`
            }
            return params.name
          },
          backgroundColor: 'rgba(8, 24, 44, 0.9)',
          borderColor: 'rgba(0, 181, 255, 0.5)',
          borderWidth: 1,
          padding: [8, 10],
          textStyle: {
            color: '#ffffff'
          }
        },
        legend: {
          data: categories.map(a => a.name),
          textStyle: {
            color: '#fff',
            fontSize: 12
          },
          bottom: 10,
          left: 'center',
          itemWidth: 15,
          itemHeight: 10,
          itemGap: 20,
          padding: [8, 10],
          backgroundColor: 'rgba(8, 24, 44, 0.5)',
          borderColor: 'rgba(0, 181, 255, 0.3)',
          borderWidth: 1,
          borderRadius: 4,
          selectedMode: false // 禁用图例切换，使用自定义过滤器
        },
        series: [
          {
            type: 'graph',
            layout: 'force',
            animation: true,
            label: {
              show: showLabels.value,
              position: 'right',
              formatter: '{b}',
              fontSize: 14, // 扩展视图增大字体
              color: '#fff',
              backgroundColor: 'rgba(8, 24, 44, 0.6)',
              padding: [3, 5],
              borderRadius: 3
            },
            roam: true,
            zoom: zoomLevel.value, // 使用当前缩放级别
            draggable: true,
            data: nodes,
            links: links,
            categories: categories,
            force: {
              edgeLength: [100, 200], // 扩展视图增大边长
              repulsion: 350, // 扩展视图增大斥力
              gravity: 0.1,
              friction: 0.2
            },
            edgeSymbol: ['none', 'arrow'],
            edgeSymbolSize: [0, 10], // 扩展视图增大箭头
            lineStyle: {
              opacity: 0.8,
              width: 2,
              curveness: 0.2,
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: 'rgba(26, 115, 232, 0.7)' },
                  { offset: 1, color: 'rgba(0, 210, 255, 0.7)' }
                ]
              }
            },
            itemStyle: {
              borderColor: 'rgba(255, 255, 255, 0.8)',
              borderWidth: 2,
              shadowColor: 'rgba(0, 181, 255, 0.4)',
              shadowBlur: 15
            },
            emphasis: {
              lineStyle: {
                width: 4,
                color: 'rgba(0, 255, 255, 0.9)',
                shadowColor: 'rgba(0, 255, 255, 0.5)',
                shadowBlur: 15
              },
              itemStyle: {
                borderWidth: 3,
                borderColor: '#ffffff',
                shadowColor: 'rgba(0, 255, 255, 0.8)',
                shadowBlur: 20
              },
              label: {
                color: '#00ffff',
                fontWeight: 'bold',
                fontSize: 16
              }
            }
          }
        ],
        backgroundColor: 'transparent' // 使用透明背景
      }
      
      expandedChart.setOption(option, true);
      console.log('扩展区域分布视图渲染完成，节点数:', nodes.length)
    }
    
    return {
      chartContainer,
      expandedChartContainer,
      initChart,
      switchView,
      expandTopology,
      closeExpandedView,
      showExpandedView,
      switchViewInModal,
      isChartInitialized,
      showLabels,
      toggleLabels,
      zoomIn,
      zoomOut,
      resetChart,
      saveImage,
      searchKeyword,
      handleSearch,
      applySearch,
      categoryFilters,
      toggleFilter,
      selectedNode,
      zoomLevel,
      nodes,
      getCategoryName,
      internalViewType // 导出内部视图类型供模板使用
    }
  }
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden; /* 确保内容不溢出 */
}

/* 左上角放大按钮样式 */
.expand-button {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 30px;
  height: 30px;
  background-color: rgba(8, 24, 44, 0.8);
  border: 1px solid rgba(0, 181, 255, 0.5);
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  transition: all 0.3s ease;
  box-shadow: 0 0 5px rgba(0, 181, 255, 0.3);
}

.expand-button:hover {
  background-color: rgba(0, 160, 255, 0.3);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.6);
  transform: scale(1.1);
}

.expand-button:active {
  transform: scale(0.95);
}

/* 模态框样式 */
.topology-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000; /* 提高z-index确保在最前面 */
  backdrop-filter: blur(3px);
}

.modal-content {
  width: 92%;
  height: 92%;
  background-color: rgba(4, 12, 32, 0.95);
  border: 2px solid rgba(0, 181, 255, 0.5);
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 210, 255, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: rgba(8, 24, 44, 0.9);
  border-bottom: 1px solid rgba(0, 181, 255, 0.3);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.modal-header h3 {
  color: #00ffff;
  margin: 0;
  font-size: 18px;
  white-space: nowrap;
}

/* 新增：图表控制工具 */
.chart-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  background-color: rgba(0, 70, 120, 0.3);
  border: 1px solid rgba(0, 181, 255, 0.4);
  border-radius: 4px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(0, 255, 255, 0.9);
  transition: all 0.2s;
}

.control-btn:hover {
  background-color: rgba(0, 181, 255, 0.4);
  transform: scale(1.05);
}

.control-btn:active {
  transform: scale(0.95);
}

.control-btn.active {
  background-color: rgba(0, 181, 255, 0.6);
  border-color: rgba(0, 255, 255, 0.8);
}

/* 标签控件样式 */
.tag-controls {
  display: flex;
  gap: 10px;
}

.tag {
  padding: 4px 12px;
  font-size: 13px;
  background-color: rgba(4, 34, 53, 0.6);
  border: 1px solid rgba(0, 181, 255, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
  white-space: nowrap;
}

.tag.active {
  background-color: rgba(0, 181, 255, 0.6);
  border-color: rgba(0, 181, 255, 0.8);
  color: white;
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-btn:hover {
  color: #00ffff;
  background-color: rgba(255, 255, 255, 0.1);
  transform: scale(1.1);
}

.modal-body {
  flex: 1;
  width: 100%;
  position: relative;
  overflow: hidden;
}

/* 新增：搜索和过滤工具栏 */
.expanded-chart-toolbar {
  position: absolute;
  top: 10px;
  left: 20px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 300px;
  max-width: 90%;
}

.search-filter {
  display: flex;
  width: 100%;
}

.search-input {
  flex: 1;
  background-color: rgba(8, 24, 44, 0.8);
  border: 1px solid rgba(0, 181, 255, 0.5);
  border-radius: 4px 0 0 4px;
  color: white;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.search-input:focus {
  border-color: rgba(0, 255, 255, 0.8);
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.4);
}

.search-btn {
  background-color: rgba(0, 70, 120, 0.8);
  border: 1px solid rgba(0, 181, 255, 0.5);
  border-left: none;
  border-radius: 0 4px 4px 0;
  width: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  background-color: rgba(0, 181, 255, 0.6);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-tag {
  padding: 3px 8px;
  font-size: 12px;
  background-color: rgba(8, 24, 44, 0.7);
  border: 1px solid rgba(0, 181, 255, 0.3);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  color: rgba(255, 255, 255, 0.8);
}

.filter-tag.active {
  background-color: rgba(0, 181, 255, 0.5);
  border-color: rgba(0, 181, 255, 0.8);
  color: white;
}

.expanded-chart-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  min-height: 600px;
}

/* 新增：悬浮节点信息面板 */
.node-info-panel {
  position: absolute;
  bottom: 10px;
  right: 20px;
  width: 280px;
  background-color: rgba(8, 24, 44, 0.9);
  border: 1px solid rgba(0, 181, 255, 0.5);
  border-radius: 6px;
  box-shadow: 0 0 15px rgba(0, 181, 255, 0.2);
  z-index: 10;
  color: white;
  overflow: hidden;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: rgba(0, 70, 120, 0.6);
  border-bottom: 1px solid rgba(0, 181, 255, 0.3);
}

.info-header h4 {
  margin: 0;
  font-size: 15px;
  color: #00ffff;
}

.close-info {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-info:hover {
  color: #00ffff;
  background-color: rgba(255, 255, 255, 0.1);
}

.info-content {
  padding: 10px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
}

.info-item .label {
  width: 80px;
  color: rgba(255, 255, 255, 0.7);
}

.info-item .value {
  flex: 1;
  color: rgba(0, 255, 255, 0.9);
}

/* 新增：底部状态栏 */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 20px;
  background-color: rgba(8, 24, 44, 0.9);
  border-top: 1px solid rgba(0, 181, 255, 0.3);
  height: 36px;
}

.status-info {
  display: flex;
  gap: 20px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.zoom-level, .nodes-count {
  display: flex;
  align-items: center;
}

.legend-container {
  /* 图例将由echarts自动填充 */
  flex: 1;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .tag-controls {
    margin-left: 0;
  }
  
  .chart-controls {
    display: none; /* 在小屏幕上隐藏部分控件 */
  }
  
  .node-info-panel {
    width: 240px;
  }
}
</style>