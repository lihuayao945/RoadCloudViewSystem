<template>
  <div class="vehicle-stream-chart" ref="chartContainer"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

// 创建一个配置了baseURL的axios实例
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json; charset=utf-8'
  }
})

const props = defineProps({
  rcuId: {
    type: String,
    default: 'U-WZ000R'
  },
  timeRange: {
    type: Object,
    default: () => ({
      startTime: Date.now() - 24 * 60 * 60 * 1000, // 默认24小时前
      endTime: Date.now()
    })
  },
  timeMode: {
    type: String,
    default: 'day' // 'day'或'hour'或'minute'
  }
})

const chartContainer = ref(null)
let chart = null
const chartData = ref([])
const loading = ref(false)

// 监听属性变化，重新获取数据
watch(() => props.rcuId, fetchData)
watch(() => props.timeRange, fetchData)
watch(() => props.timeMode, () => {
  // 时间模式改变时，重新绘制图表
  if (chart) {
    updateChart()
  }
})

// 初始化图表
function initChart() {
  if (!chartContainer.value) return
  
  chart = echarts.init(chartContainer.value)
  updateChart()
  
  // 添加窗口大小变化监听
  window.addEventListener('resize', handleResize)
}

// 处理窗口大小变化
function handleResize() {
  if (chart) {
    chart.resize()
  }
}

// 更新图表
function updateChart() {
  if (!chart) return
  
  const xAxisData = []
  const seriesData = []
  
  // 根据时间模式选择不同的数据处理方式
  if (props.timeMode === 'day') {
    // 按天模式：显示24小时
    updateChartByDay(xAxisData, seriesData)
  } else if (props.timeMode === 'hour') {
    // 按时模式：显示1小时
    updateChartByHour(xAxisData, seriesData)
  } else {
    // 按分模式：显示1分钟
    updateChartByMinute(xAxisData, seriesData)
  }
  
  // 设置图表配置
  const option = {
    backgroundColor: 'transparent',
    grid: {
      left: '5%',
      right: '5%',
      top: '15%',
      bottom: '15%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 15, 40, 0.8)',
      borderColor: 'rgba(0, 255, 255, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#fff',
        fontSize: 12
      },
      formatter: function(params) {
        if (props.timeMode === 'day') {
          // 按天模式的提示格式
          const date = new Date(params[0].axisValue)
          if (isNaN(date.getTime())) {
            // 如果无法解析时间，则使用原始格式
            return `${params[0].axisValue}<br/><span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:#00ffff;"></span>车流量: <span style="color:#00ffff;font-weight:bold">${params[0].value}</span>`
          }
          return `${params[0].axisValue}<br/><span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:#00ffff;"></span>车流量: <span style="color:#00ffff;font-weight:bold">${params[0].value}</span>`
        } else {
          // 按小时和按分钟模式的提示格式
          return `${params[0].axisValue}<br/><span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:#00ffff;"></span>车流量: <span style="color:#00ffff;font-weight:bold">${params[0].value}</span>`
        }
      },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: 'rgba(0, 255, 255, 0.5)',
          width: 1,
          type: 'dashed'
        }
      }
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLine: {
        lineStyle: {
          color: '#0e5986',
          width: 2
        }
      },
      axisLabel: {
        color: '#fff',
        fontSize: 10,
        rotate: 45,
        margin: 15,
        // 按不同模式有不同的显示逻辑
        formatter: function(value, index) {
          if (props.timeMode === 'day') {
            // 按天模式：显示部分标签
            return index % Math.ceil(xAxisData.length / 4) === 0 ? value : ''
          } else if (props.timeMode === 'hour') {
            // 按小时模式：适当间隔显示
            return index % Math.ceil(xAxisData.length / 8) === 0 ? value : ''
          } else {
            // 按分钟模式：更密集地显示
            return index % Math.ceil(xAxisData.length / 4) === 0 ? value : ''
          }
        }
      },
      axisTick: {
        alignWithLabel: true,
        lineStyle: {
          color: '#0e5986'
        }
      },
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      name: '车流量',
      nameLocation: 'end',
      nameGap: 15,
      nameRotate: 0,
      nameTextStyle: {
        color: '#fff',
        fontSize: 12,
        padding: [0, 0, 0, 0]
      },
      min: 0,
      // 计算合适的Y轴最大值，确保数据可视化效果良好
      max: function(value) {
        return value.max === 0 ? 5 : Math.ceil(value.max * 1.2);
      },
      axisLine: {
        lineStyle: {
          color: '#0e5986',
          width: 2
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(14, 89, 134, 0.3)',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#fff',
        fontSize: 10,
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '车流量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: props.timeMode === 'minute' ? 8 : 6,
        showSymbol: props.timeMode !== 'day', // 按天模式不显示数据点，其他模式显示
        data: seriesData,
        lineStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            {
              offset: 0,
              color: '#00bfff'
            },
            {
              offset: 1,
              color: '#00ffff'
            }
          ]),
          width: 3,
          shadowColor: 'rgba(0, 255, 255, 0.5)',
          shadowBlur: 10
        },
        itemStyle: {
          color: function(params) {
            return new echarts.graphic.LinearGradient(0, 0, 1, 1, [
              {
                offset: 0,
                color: '#00bfff'
              },
              {
                offset: 1,
                color: '#00ffff'
              }
            ]);
          },
          borderColor: '#fff',
          borderWidth: 2,
          shadowColor: 'rgba(0, 255, 255, 0.8)',
          shadowBlur: 10
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(0, 255, 255, 0.6)'
            },
            {
              offset: 0.5,
              color: 'rgba(0, 191, 255, 0.3)'
            },
            {
              offset: 1,
              color: 'rgba(0, 127, 255, 0.1)'
            }
          ]),
          shadowColor: 'rgba(0, 255, 255, 0.2)',
          shadowBlur: 20
        },
        emphasis: {
          itemStyle: {
            borderWidth: 3,
            shadowBlur: 20
          }
        },
        z: 10
      }
    ],
    animationDuration: 1500,
    animationEasing: 'elasticOut'
  }
  
  // 设置图表加载状态
  loading.value ? chart.showLoading({
    text: '数据加载中...',
    color: '#00ffff',
    textColor: '#fff',
    maskColor: 'rgba(0, 0, 0, 0.5)'
  }) : chart.hideLoading()
  
  // 应用配置
  chart.setOption(option)
}

// 按天模式更新图表数据
function updateChartByDay(xAxisData, seriesData) {
  // 创建24小时的时间刻度作为固定横坐标范围
  const endDate = new Date()
  const startDate = new Date(endDate.getTime() - 24 * 60 * 60 * 1000) // 24小时前
  
  // 生成24个小时刻度
  for (let i = 0; i <= 24; i++) {
    const time = new Date(startDate.getTime() + i * 60 * 60 * 1000)
    const hour = time.getHours().toString().padStart(2, '0')
    xAxisData.push(`${hour}:00`)
  }
  
  // 根据后端数据创建数据点
  if (chartData.value && chartData.value.length > 0) {
    // 初始化值为0的数据数组（对应每个小时）
    const hourlyData = Array(25).fill(0)
    
    // 将数据点分配到对应的小时区间
    chartData.value.forEach(item => {
      const itemDate = new Date(item.start_time)
      // 计算数据点距离起始时间的小时数
      const hourDiff = Math.floor((itemDate - startDate) / (60 * 60 * 1000))
      // 如果在有效范围内，累加数据值
      if (hourDiff >= 0 && hourDiff < hourlyData.length) {
        hourlyData[hourDiff] += item.count
      }
    })
    
    // 设置数据点
    seriesData.push(...hourlyData)
  } else {
    // 如果没有数据，填充0
    seriesData.push(...Array(25).fill(0))
  }
}

// 按小时模式更新图表数据
function updateChartByHour(xAxisData, seriesData) {
  if (chartData.value && chartData.value.length > 0) {
    // 按时间排序数据
    const sortedData = [...chartData.value].sort((a, b) => 
      new Date(a.start_time) - new Date(b.start_time)
    )
    
    // 遍历每个数据点
    sortedData.forEach(item => {
      const date = new Date(item.start_time)
      // 格式化时间为 HH:MM:SS
      const formattedTime = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
      xAxisData.push(formattedTime)
      seriesData.push(item.count)
    })
  } else {
    // 如果没有数据，生成一些参考刻度
    const now = new Date()
    const hourAgo = new Date(now.getTime() - 60 * 60 * 1000)
    
    // 生成10个空的数据点
    for (let i = 0; i < 10; i++) {
      const time = new Date(hourAgo.getTime() + (i * 6 * 60 * 1000)) // 每6分钟一个点
      const hour = time.getHours().toString().padStart(2, '0')
      const minute = time.getMinutes().toString().padStart(2, '0')
      xAxisData.push(`${hour}:${minute}`)
      seriesData.push(0)
    }
  }
}

// 按分钟模式更新图表数据
function updateChartByMinute(xAxisData, seriesData) {
  if (chartData.value && chartData.value.length > 0) {
    // 按时间排序数据
    const sortedData = [...chartData.value].sort((a, b) => 
      new Date(a.start_time) - new Date(b.start_time)
    )
    
    // 遍历每个数据点
    sortedData.forEach(item => {
      const date = new Date(item.start_time)
      // 格式化时间为 HH:MM:SS (不包含毫秒)
      const formattedTime = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
      xAxisData.push(formattedTime)
      seriesData.push(item.count)
    })
  } else {
    // 如果没有数据，生成一些参考刻度
    const now = new Date()
    const minuteAgo = new Date(now.getTime() - 60 * 1000)
    
    // 生成12个空的数据点，每5秒一个
    for (let i = 0; i < 12; i++) {
      const time = new Date(minuteAgo.getTime() + (i * 5 * 1000))
      const hour = time.getHours().toString().padStart(2, '0')
      const minute = time.getMinutes().toString().padStart(2, '0')
      const second = time.getSeconds().toString().padStart(2, '0')
      xAxisData.push(`${hour}:${minute}:${second}`)
      seriesData.push(0)
    }
  }
}

// 从后端获取数据
async function fetchData() {
  loading.value = true
  
  try {
    // 获取时间范围（确保使用毫秒级时间戳）
    const startTime = props.timeRange.startTime
    const endTime = props.timeRange.endTime
    
    console.log('请求时间范围:', {
      模式: props.timeMode,
      开始时间: new Date(startTime).toLocaleString(),
      结束时间: new Date(endTime).toLocaleString(),
      startTime毫秒值: startTime,
      endTime毫秒值: endTime
    })
    
    // 构建完整URL以便在控制台查看
    const relativeUrl = `/menu/vehicle/stream?rcuId=${props.rcuId}&startTime=${startTime}&endTime=${endTime}`
    const fullUrl = `http://47.109.73.14:10010${relativeUrl}`
    console.log('相对请求URL:', relativeUrl)
    console.log('完整请求URL:', fullUrl)
    
    console.log('尝试使用代理方式请求...')
    try {
      // 方式1：尝试使用Vite代理（如果代理工作正常）
      const proxyResponse = await axios.get('/menu/vehicle/stream', {
        params: {
          rcuId: props.rcuId,
          startTime: startTime,
          endTime: endTime
        },
        timeout: 5000
      })
      console.log('代理请求成功:', proxyResponse.status)
      
      // 处理响应数据
      if (proxyResponse.data && proxyResponse.data.data) {
        chartData.value = proxyResponse.data.data
        console.log('车流量数据点数量:', proxyResponse.data.data.length)
      } else {
        throw new Error('数据格式错误')
      }
    } catch (proxyError) {
      console.error('代理请求失败，尝试直接请求:', proxyError.message)
      
      // 方式2：直接请求（绕过代理，可能会有CORS问题）
      const directResponse = await apiClient.get('/menu/vehicle/stream', {
        params: {
          rcuId: props.rcuId,
          startTime: startTime,
          endTime: endTime
        }
      })
      console.log('直接请求成功:', directResponse.status)
      
      // 处理响应数据
      if (directResponse.data && directResponse.data.data) {
        chartData.value = directResponse.data.data
        console.log('车流量数据点数量:', directResponse.data.data.length)
        if (directResponse.data.data.length > 0) {
          console.log('第一个数据点:', directResponse.data.data[0])
          console.log('最后一个数据点:', directResponse.data.data[directResponse.data.data.length - 1])
        }
      } else {
        chartData.value = []
        console.error('数据格式错误', directResponse)
      }
    }
  } catch (error) {
    console.error('获取车流量数据失败', error)
    chartData.value = []
  } finally {
    loading.value = false
    updateChart()
  }
}

// 对外暴露刷新方法
function refreshData() {
  fetchData()
}

// 组件挂载
onMounted(() => {
  initChart()
  fetchData()
})

// 组件卸载前清理
onBeforeUnmount(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  window.removeEventListener('resize', handleResize)
})

// 暴露方法给父组件
defineExpose({
  refreshData
})
</script>

<style scoped>
.vehicle-stream-chart {
  height: 100%;
  min-height: 220px;
  max-height: 400px;
  width: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vehicle-stream-chart::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.5), transparent);
  z-index: 1;
}

.vehicle-stream-chart::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.3), transparent);
  z-index: 1;
}

@media screen and (max-height: 768px) {
  .vehicle-stream-chart {
    min-height: 180px;
    max-height: 300px;
  }
}

@media screen and (min-height: 1080px) {
  .vehicle-stream-chart {
    min-height: 250px;
    max-height: 450px;
  }
}
</style> 