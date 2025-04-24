<template>
    <div class="chart-container" ref="chartContainer"></div>
  </template>
  
  <script>
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  import * as echarts from 'echarts'
  import { generateRandomValue } from '../../utils/chartUtils'
  
  export default {
    name: 'DeviceAnomalyChart',
    setup() {
      const chartContainer = ref(null)
      let chart = null
      
      onMounted(() => {
        window.addEventListener('resize', handleResize)
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
      
      function refreshData() {
        // 异常类型数据
        const anomalyTypes = [
          '信号丢失', 
          '信号干扰', 
          '电源故障', 
          '存储故障', 
          '传感器异常',
          '温度过高'
        ]
        
        // 为每种设备类型生成数据
        const cameraData = anomalyTypes.map(() => generateRandomValue(5, 20))
        const trafficLightData = anomalyTypes.map(() => generateRandomValue(3, 15))
        const policeData = anomalyTypes.map(() => generateRandomValue(2, 10))
        const sensorData = anomalyTypes.map(() => generateRandomValue(1, 8))
        
        const option = {
          color: ['#00d2ff', '#7777eb', '#37a2da', '#32c5e9'],
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          legend: {
            data: ['监控摄像头', '交通信号灯', '电子警察', '路况检测器'],
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
            top: '25%',
            containLabel: true
          },
          xAxis: {
            type: 'value',
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              fontSize: 11,
              color: '#fff'
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            }
          },
          yAxis: {
            type: 'category',
            data: anomalyTypes,
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              fontSize: 11,
              color: '#fff'
            }
          },
          series: [
            {
              name: '监控摄像头',
              type: 'bar',
              stack: '异常',
              emphasis: {
                focus: 'series'
              },
              data: cameraData
            },
            {
              name: '交通信号灯',
              type: 'bar',
              stack: '异常',
              emphasis: {
                focus: 'series'
              },
              data: trafficLightData
            },
            {
              name: '电子警察',
              type: 'bar',
              stack: '异常',
              emphasis: {
                focus: 'series'
              },
              data: policeData
            },
            {
              name: '路况检测器',
              type: 'bar',
              stack: '异常',
              emphasis: {
                focus: 'series'
              },
              data: sensorData
            }
          ]
        }
        
        chart.setOption(option)
      }
      
      return {
        chartContainer,
        initChart,
        refreshData
      }
    }
  }
  </script>
  
  <style scoped>
  .chart-container {
    width: 100%;
    height: 100%;
  }
  </style>