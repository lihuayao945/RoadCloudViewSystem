<template>
    <div class="flow-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'FlowAnalysisChart',
    setup() {
      const chartContainer = ref(null)
      let chart = null
      
      // 监听窗口大小变化
      const handleResize = () => {
        if (chart) {
          chart.resize()
        }
      }
      
      onMounted(() => {
        window.addEventListener('resize', handleResize)
        initChart()
      })
      
      onBeforeUnmount(() => {
        window.removeEventListener('resize', handleResize)
        if (chart) {
          chart.dispose()
          chart = null
        }
      })
      
      // 初始化图表
      const initChart = () => {
        if (!chartContainer.value) return
        
        chart = echarts.init(chartContainer.value)
        refreshData()
      }
      
      // 刷新数据
      const refreshData = () => {
        if (!chart) return
        
        const now = new Date()
        const hours = now.getHours()
        const minutes = now.getMinutes()
        
        // 获取过去24个时间点的数据
        const data = generateFlowData(hours, minutes)
        
        const option = {
          animation: true,
          grid: {
            left: '3%',
            right: '4%',
            bottom: '10%',
            top: '10%',
            containLabel: true
          },
          tooltip: {
            trigger: 'axis',
            formatter: function(params) {
              return `${params[0].name}<br/>车流量: ${params[0].value}辆/分钟`
            },
            axisPointer: {
              type: 'line',
              lineStyle: {
                color: 'rgba(0, 255, 255, 0.3)',
                width: 1,
                type: 'solid'
              }
            }
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: data.times,
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9,
              interval: 2
            },
            splitLine: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            },
            axisLine: {
              show: false
            }
          },
          series: [{
            name: '车流量',
            type: 'line',
            smooth: true,
            symbol: 'none',
            sampling: 'average',
            itemStyle: {
              color: '#00c1de'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgba(0, 193, 222, 0.8)'
                },
                {
                  offset: 1,
                  color: 'rgba(0, 193, 222, 0)'
                }
              ])
            },
            data: data.values
          }]
        }
        
        chart.setOption(option)
      }
      
      // 生成流量数据
      const generateFlowData = (currentHour, currentMinute) => {
        const times = []
        const values = []
        
        // 生成过去12个点的时间
        for (let i = 11; i >= 0; i--) {
          let hour = currentHour
          let minute = currentMinute - (i * 10)
          
          // 处理分钟为负的情况
          while (minute < 0) {
            minute += 60
            hour--
          }
          
          // 处理小时为负的情况
          if (hour < 0) {
            hour += 24
          }
          
          const timeStr = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
          times.push(timeStr)
          
          // 生成流量数据
          // 根据时间生成不同的流量数据
          let baseValue
          
          // 早高峰 (7-9点)
          if (hour >= 7 && hour < 9) {
            baseValue = 50 + Math.random() * 25
          }
          // 晚高峰 (17-19点)
          else if (hour >= 17 && hour < 19) {
            baseValue = 55 + Math.random() * 25
          }
          // 中午 (11-13点)
          else if (hour >= 11 && hour < 13) {
            baseValue = 35 + Math.random() * 15
          }
          // 其他时间
          else if (hour >= 6 && hour < 22) {
            baseValue = 20 + Math.random() * 20
          }
          // 深夜/凌晨
          else {
            baseValue = 5 + Math.random() * 10
          }
          
          // 添加一些随机性
          const value = Math.round(baseValue + (Math.random() - 0.5) * 10)
          values.push(value)
        }
        
        return { times, values }
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
  .flow-chart {
    width: 100%;
    height: 100%;
  }
  </style>