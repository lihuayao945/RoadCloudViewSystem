<template>
  <div class="bottom-panel panel">
    <div class="panel-header">
      <div class="chart-title main-title">系统数据统计</div>
      <div class="header-controls">
        <div class="panel-badge" :class="{ active: activeRequestCount > 0 }">
          <span v-if="activeRequestCount > 0">{{ activeRequestCount }}</span>
          <span v-else>0</span>
        </div>
      </div>
    </div>
    
    <div class="panel-content">
      <div class="charts-container">
        <!-- 速度分布饼图 -->
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-card-title">车辆速度分布</span>
            <div class="chart-header-value">{{ totalVehicles }} <small>辆</small></div>
          </div>
          <div class="chart-body">
            <div class="pie-chart-container" ref="speedChartContainer"></div>
          </div>
        </div>
        
        <!-- 车辆类型分布饼图 -->
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-card-title">车辆类型占比</span>
            <div class="chart-header-value">{{ totalTypes }} <small>类</small></div>
          </div>
          <div class="chart-body">
            <div class="pie-chart-container" ref="typeChartContainer"></div>
          </div>
        </div>
        
        <!-- 驾驶类型分布饼图 -->
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-card-title">驾驶模式占比</span>
            <div class="chart-header-value">4.90 <small>分</small></div>
          </div>
          <div class="chart-body">
            <div class="pie-chart-container" ref="drivingChartContainer"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import axios from 'axios'

// 注册必要的组件
echarts.use([TitleComponent, TooltipComponent, LegendComponent, PieChart, CanvasRenderer])

export default {
  name: 'BottomDataPanel',
  setup() {
    const activeRequestCount = ref(0);
    
    // 图表容器引用
    const speedChartContainer = ref(null);
    const typeChartContainer = ref(null);
    const drivingChartContainer = ref(null);
    
    // 图表实例
    let speedChart = null;
    let typeChart = null;
    let drivingChart = null;
    
    // 统计数据
    const totalVehicles = ref(0);
    const totalTypes = ref(0);
    
    // 车辆速度分布数据 - 如果API不可用，使用模拟数据
    const speedData = ref([
      { value: 45, name: '0-30 km/h' },
      { value: 25, name: '30-60 km/h' },
      { value: 20, name: '60-90 km/h' },
      { value: 10, name: '>90 km/h' }
    ]);
    
    // 车辆类型分布数据 - 将从API获取
    const typeData = ref([]);
    
    // 驾驶类型分布数据
    const drivingData = [
      { value: 72, name: '人工驾驶' },
      { value: 18, name: '辅助驾驶' },
      { value: 10, name: '自动驾驶' }
    ];
    
    // 获取车辆速度分布数据
    async function fetchVehicleSpeedData() {
      try {
        activeRequestCount.value++;
        
        // 尝试从API获取速度数据
        const response = await axios.get('/menu/vehicle/speed');
        
        if (response.data && response.data.status === 'success' && response.data.vehicleSpeedRange) {
          // 转换API返回的数据格式为echarts所需格式
          speedData.value = response.data.vehicleSpeedRange.map(item => ({
            value: item.vehicle_count,
            name: item.speed_range
          }));
        }
        
        // 计算总车辆数
        totalVehicles.value = speedData.value.reduce((sum, item) => sum + item.value, 0);
        
        // 更新图表
        updateVehicleSpeedChart();
      } catch (error) {
        console.error('获取车辆速度数据失败:', error);
        // 如果API请求失败，保留默认模拟数据并更新总数
        totalVehicles.value = speedData.value.reduce((sum, item) => sum + item.value, 0);
        updateVehicleSpeedChart();
      } finally {
        activeRequestCount.value--;
      }
    }
    
    // 获取车辆类型分布数据
    async function fetchVehicleTypeData() {
      try {
        activeRequestCount.value++;
        
        // 获取当前时间的前24小时作为时间范围
        const endTime = new Date().getTime();
        const startTime = endTime - 24 * 60 * 60 * 1000; // 24小时前
        
        const response = await axios.get('/menu/vehicle/kindnum/all', {
          params: {
            startTime: startTime.toString(),
            endTime: endTime.toString()
          }
        });
        
        console.log("API 响应数据:", response.data);
        
        if (response.data && response.data.status === 'success' && response.data.data && Array.isArray(response.data.data)) {
          if (response.data.data.length > 0) {
            // 修改这里以匹配实际API返回的字段名
            typeData.value = response.data.data.map(item => ({
              value: item.record_count || 0, // 使用record_count作为value
              name: item.typeName || '未知类型', // 使用typeName作为name
              percentage: item.percentage || 0
            }));
            
            console.log("处理后的类型数据:", typeData.value);
            
            // 更新车辆类型总数
            totalTypes.value = typeData.value.length;
          } else {
            console.log("API 返回了空数组");
            useDefaultTypeData();
          }
        } else {
          console.log("API 数据结构不符合预期");
          useDefaultTypeData();
        }
        
        // 更新图表
        updateVehicleTypeChart();
      } catch (error) {
        console.error('获取车辆类型数据失败:', error);
        useDefaultTypeData();
        updateVehicleTypeChart();
      } finally {
        activeRequestCount.value--;
      }
    }
    
    // A使用默认车辆类型数据
    function useDefaultTypeData() {
      typeData.value = [
        { value: 350, name: '摩托车', percentage: 0.58 },
        { value: 120, name: '自行车', percentage: 0.20 },
        { value: 80, name: '小型车', percentage: 0.13 },
        { value: 50, name: '大型车', percentage: 0.09 }
      ];
      totalTypes.value = typeData.value.length;
    }
    
    // 更新车辆速度图表
    function updateVehicleSpeedChart() {
      if (speedChartContainer.value && speedData.value.length > 0) {
        if (!speedChart) {
          speedChart = echarts.init(speedChartContainer.value);
        }
        
        const mainPercentage = calculateMainPercentage(speedData.value);
        speedChart.setOption(getPieChartOption(speedData.value, '车速分布', mainPercentage, [
          ['#FF318C', '#d949ff'], // 粉紫色渐变
          ['#21D4FD', '#2876f9'], // 蓝色渐变
          ['#43E97B', '#38f9d7'], // 绿色渐变
          ['#F97794', '#623AA2'], // 粉紫色渐变
          ['#FACC15', '#FA9E1B']  // 黄色渐变
        ]));
      }
    }
    
    // 更新车辆类型图表
    function updateVehicleTypeChart() {
      if (typeChartContainer.value && typeData.value.length > 0) {
        if (!typeChart) {
          typeChart = echarts.init(typeChartContainer.value);
        }
        
        // 确保每个数据项都有有效的value
        const validData = typeData.value.filter(item => !isNaN(item.value) && item.value > 0);
        
        if (validData.length > 0) {
          const mainPercentage = calculateMainPercentage(validData);
          typeChart.setOption(getPieChartOption(validData, '车型分布', mainPercentage, [
            ['#FF318C', '#d949ff'], // 粉紫色渐变
            ['#21D4FD', '#2876f9'], // 蓝色渐变
            ['#43E97B', '#38f9d7'], // 绿色渐变
            ['#F97794', '#623AA2'], // 粉紫色渐变
            ['#FACC15', '#FA9E1B']  // 黄色渐变
          ]));
        } else {
          // 如果没有有效数据，显示提示信息
          typeChart.setOption({
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center',
              textStyle: {
                fontSize: 14,
                color: '#fff'
              }
            },
            series: [
              {
                type: 'pie',
                radius: ['65%', '85%'],
                center: ['50%', '50%'],
                data: [{ value: 1, name: '暂无数据', itemStyle: { color: '#333' } }],
                label: { show: false },
                emphasis: { scale: false }
              }
            ]
          });
        }
      }
    }
    
    // 计算主要部分的百分比用于中心显示
    function calculateMainPercentage(data) {
      if (!data || data.length === 0) return 0;
      
      // 计算所有value的总和
      const total = data.reduce((sum, item) => {
        // 确保value是数字
        const value = Number(item.value);
        return sum + (isNaN(value) ? 0 : value);
      }, 0);
      
      // 如果总和为0，返回0%
      if (total === 0) return 0;
      
      // 找出最大的项
      const maxItem = data.reduce((prev, current) => {
        const prevValue = Number(prev.value);
        const currentValue = Number(current.value);
        return (prevValue > currentValue) ? prev : current;
      }, data[0]);
      
      // 计算百分比
      const maxValue = Number(maxItem.value);
      const percentage = isNaN(maxValue) ? 0 : Math.round((maxValue / total) * 100);
      
      return percentage;
    }
    
    // 初始化图表
    function initCharts() {
      // 初始化速度分布图表
      updateVehicleSpeedChart();
      
      // 初始化车辆类型图表
      updateVehicleTypeChart();
      
      // 初始化驾驶类型图表
      if (drivingChartContainer.value) {
        drivingChart = echarts.init(drivingChartContainer.value);
        const mainPercentage = calculateMainPercentage(drivingData);
        drivingChart.setOption(getPieChartOption(drivingData, '驾驶模式', mainPercentage, [
          ['#FF318C', '#d949ff'], // 粉紫色渐变
          ['#21D4FD', '#2876f9'], // 蓝色渐变
          ['#43E97B', '#38f9d7']  // 绿色渐变
        ]));
      }
    }
    
    // 生成饼图配置 - 修改为渐变色彩的现代饼图
    function getPieChartOption(data, title, centerPercentage, colorPalette) {
      // 为每个数据项设置渐变色
      const processedData = data.map((item, index) => {
        const colors = colorPalette[index % colorPalette.length];
        return {
          ...item,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: colors[0] },
              { offset: 1, color: colors[1] }
            ])
          }
        };
      });
      
      return {
        backgroundColor: 'transparent',
        title: {
          text: centerPercentage + '%',
          left: 'center',
          top: 'center',
          textStyle: {
            fontSize: 24,
            fontWeight: 'bold',
            color: '#fff',
            fontFamily: 'Arial'
          }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {d}%',
          backgroundColor: 'rgba(4, 34, 53, 0.9)',
          borderColor: 'rgba(14, 89, 134, 0.6)',
          textStyle: {
            color: '#fff'
          }
        },
        series: [
          {
            name: title,
            type: 'pie',
            radius: ['65%', '85%'],
            center: ['50%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 6,
              borderColor: 'rgba(0, 0, 0, 0.05)',
              borderWidth: 1
            },
            label: {
              show: false
            },
            emphasis: {
              scale: true,
              scaleSize: 10,
              label: {
                show: false
              }
            },
            labelLine: {
              show: false
            },
            data: processedData
          }
        ]
      };
    }
    
    // 处理窗口大小变化，重新调整图表大小
    function handleResize() {
      if (speedChart) speedChart.resize();
      if (typeChart) typeChart.resize();
      if (drivingChart) drivingChart.resize();
    }
    
    // 定时刷新数据
    let refreshTimer = null;
    
    function startDataRefresh() {
      // 每5分钟刷新一次数据
      refreshTimer = setInterval(() => {
        fetchVehicleSpeedData();
        fetchVehicleTypeData();
      }, 5 * 60 * 1000);
    }
    
    onMounted(() => {
      // 首次加载获取数据
      fetchVehicleSpeedData();
      fetchVehicleTypeData();
      
      // 初始化图表
      nextTick(() => {
        // 使用nextTick确保DOM已更新
        initCharts();
      });
      
      // 添加窗口大小变化监听
      window.addEventListener('resize', handleResize);
      
      // 启动定时刷新
      startDataRefresh();
    });
    
    onUnmounted(() => {
      // 移除窗口大小变化监听
      window.removeEventListener('resize', handleResize);
      
      // 销毁图表实例
      if (speedChart) speedChart.dispose();
      if (typeChart) typeChart.dispose();
      if (drivingChart) drivingChart.dispose();
      
      // 清除定时器
      if (refreshTimer) {
        clearInterval(refreshTimer);
      }
    });
    
    // 监听数据变化，更新图表
    watch([speedData, typeData], () => {
      nextTick(() => {
        updateVehicleSpeedChart();
        updateVehicleTypeChart();
      });
    });
    
    return {
      activeRequestCount,
      speedChartContainer,
      typeChartContainer,
      drivingChartContainer,
      totalVehicles,
      totalTypes
    };
  }
}
</script>

<style scoped>
.bottom-panel {
  position: absolute;
  bottom: 0.5%;
  left: 50%;
  transform: translateX(-50%);
  width: 48%;
  height: 25%; 
  display: flex;
  flex-direction: column;
  background-color: rgb(8, 24, 44);
  border: 1px solid rgba(0, 145, 234, 0.6);
  border-radius: 4px;
  padding: 6px;
  /* box-shadow: 0 0 10px rgba(0, 145, 234, 0.6); */
  z-index: 10;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  height: 28px;
  padding: 0 4px;
}

.main-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 0;
  color: #00e4ff;
  display: flex;
  align-items: center;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-badge {
  background-color: rgba(4, 34, 53, 0.6);
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  border: 1px solid rgba(14, 89, 134, 0.4);
  min-width: 24px;
  text-align: center;
}

.panel-badge.active {
  background-color: rgba(0, 145, 234, 0.3);
  color: #fff;
  border: 1px solid rgba(0, 145, 234, 0.6);
}

/* 图表内容布局 */
.panel-content {
  height: calc(100% - 28px);
  overflow: hidden;
}

.charts-container {
  display: flex;
  height: 100%;
  width: 100%;
  gap: 8px;
  padding: 0 2px;
}

/* 图表卡片样式 */
.chart-card {
  flex: 1;
  background-color: rgba(4, 34, 53, 0.6);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 3px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-header {
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
  height: 36px;
}

.chart-card-title {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
}

.chart-header-value {
  font-size: 16px;
  font-weight: bold;
  color: #00e4ff;
}

.chart-header-value small {
  font-size: 11px;
  font-weight: normal;
  color: rgba(255, 255, 255, 0.6);
  margin-left: 2px;
}

.chart-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.pie-chart-container {
  width: 100%;
  height: 100%;
}

/* 响应式调整 */
@media (max-width: 1400px) {
  .bottom-panel {
    width: 60%;
  }
}

@media (max-width: 992px) {
  .bottom-panel {
    width: 90%;
  }
  
  .charts-container {
    flex-direction: column;
    overflow-y: auto;
  }
  
  .chart-card {
    height: 160px;
  }
}

@media (max-width: 768px) {
  .bottom-panel {
    width: 95%;
  }
  
  .chart-header-value {
    font-size: 14px;
  }
}
</style>