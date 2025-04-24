<template>
  <div class="chart-container" ref="chartContainer"></div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { generateRandomValue } from '../../utils/chartUtils'

export default {
  name: 'LineChart',
  setup() {
    const chartContainer = ref(null)
    let chart = null
    
    onMounted(() => {
      window.addEventListener('resize', handleResize)
      initChart()
    })
    
    onBeforeUnmount(() => {
      if (chart) {
        chart.dispose()
        chart = null
      }
      window.removeEventListener('resize', handleResize)
    })
    
    function handleResize() {
      if (chart) {
        chart.resize()
      }
    }
    
    function initChart() {
      if (chart) {
        chart.dispose()
      }
      
      chart = echarts.init(chartContainer.value)
      refreshData()
    }
    
    // 默认的交通数据图表
    function refreshData() {
      // 生成X轴数据，最近7天
      const dateLabels = []
      const now = new Date()
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(now.getDate() - i)
        const month = d.getMonth() + 1
        const date = d.getDate()
        dateLabels.push(`${month}/${date}`)
      }
      
      // 生成交通数据
      const congestionData = []
      const flowData = []
      const timeData = []
      
      for (let i = 0; i < 7; i++) {
        congestionData.push(generateRandomValue(2, 5, 1))
        flowData.push(generateRandomValue(20000, 50000))
        timeData.push(generateRandomValue(15, 35, 1))
      }
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['交通拥堵指数', '车流量', '平均通行时间'],
          textStyle: {
            fontSize: 11,
            color: '#fff'
          },
          top: 0,
          itemWidth: 12,
          itemHeight: 8
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%', 
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dateLabels,
          axisLine: {
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.3)'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 11
          }
        },
        yAxis: [
          {
            type: 'value',
            name: '指数',
            position: 'left',
            min: 0,
            max: 6,
            interval: 1,
            axisLine: {
              lineStyle: {
                color: '#91cc75'
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            },
            axisLabel: {
              formatter: '{value}',
              color: '#fff',
              fontSize: 11
            }
          },
          {
            type: 'value',
            name: '流量',
            position: 'right',
            axisLine: {
              lineStyle: {
                color: '#5470c6'
              }
            },
            splitLine: {
              show: false
            },
            axisLabel: {
              formatter: '{value}',
              color: '#fff',
              fontSize: 11
            }
          }
        ],
        series: [
          {
            name: '交通拥堵指数',
            type: 'line',
            data: congestionData,
            yAxisIndex: 0,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#91cc75'
            },
            lineStyle: {
              width: 2
            }
          },
          {
            name: '车流量',
            type: 'line',
            data: flowData,
            yAxisIndex: 1,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#5470c6'
            },
            lineStyle: {
              width: 2
            }
          },
          {
            name: '平均通行时间',
            type: 'line',
            data: timeData,
            yAxisIndex: 0,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#fac858'
            },
            lineStyle: {
              width: 2
            }
          }
        ]
      }
      
      chart.setOption(option)
    }
    
    // 新增的设备状态图表
    function initChartForDeviceStatus() {
      if (chart) {
        chart.dispose()
      }
      
      chart = echarts.init(chartContainer.value)
      refreshDataForDeviceStatus()
    }
    
    function refreshDataForDeviceStatus() {
      // 生成X轴数据，最近7天
      const dateLabels = []
      const now = new Date()
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(now.getDate() - i)
        const month = d.getMonth() + 1
        const date = d.getDate()
        dateLabels.push(`${month}/${date}`)
      }
      
      // 生成设备状态数据
      const onlineRateData = []
      const responseTimeData = []
      const anomalyCountData = []
      
      for (let i = 0; i < 7; i++) {
        onlineRateData.push(generateRandomValue(85, 99, 1))
        responseTimeData.push(generateRandomValue(50, 300))
        anomalyCountData.push(generateRandomValue(2, 12))
      }
      
      const option = {
        color: ['#00d2ff', '#e690ed', '#ff8c71'],
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['在线率 (%)', '响应时间 (ms)', '异常事件 (次)'],
          textStyle: {
            fontSize: 11,
            color: '#fff'
          },
          top: 0,
          itemWidth: 12,
          itemHeight: 8
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dateLabels,
          axisLine: {
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.3)'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 11
          }
        },
        yAxis: [
          {
            type: 'value',
            name: '百分比',
            min: 80,
            max: 100,
            position: 'left',
            axisLine: {
              lineStyle: {
                color: '#00d2ff'
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            },
            axisLabel: {
              formatter: '{value}%',
              color: '#fff',
              fontSize: 11
            }
          },
          {
            type: 'value',
            name: '时间/次数',
            position: 'right',
            axisLine: {
              lineStyle: {
                color: '#e690ed'
              }
            },
            splitLine: {
              show: false
            },
            axisLabel: {
              formatter: '{value}',
              color: '#fff',
              fontSize: 11
            }
          }
        ],
        series: [
          {
            name: '在线率 (%)',
            type: 'line',
            data: onlineRateData,
            yAxisIndex: 0,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2
            },
            areaStyle: {
              opacity: 0.1
            }
          },
          {
            name: '响应时间 (ms)',
            type: 'line',
            data: responseTimeData,
            yAxisIndex: 1,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2
            }
          },
          {
            name: '异常事件 (次)',
            type: 'bar',
            data: anomalyCountData,
            yAxisIndex: 1,
            barWidth: 10
          }
        ]
      }
      
      chart.setOption(option)
    }
    
    return {
      chartContainer,
      initChart,
      refreshData,
      initChartForDeviceStatus,
      refreshDataForDeviceStatus
    }
  }
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 190px;
}
</style>