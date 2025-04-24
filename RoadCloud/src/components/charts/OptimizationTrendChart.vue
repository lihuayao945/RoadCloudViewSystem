<template>
    <div class="optimization-trend-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'OptimizationTrendChart',
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
        
        // 生成最近7天的日期标签
        const dateLabels = [];
        const today = new Date();
        
        for (let i = 6; i >= 0; i--) {
          const date = new Date();
          date.setDate(today.getDate() - i);
          const day = date.getDate();
          const month = date.getMonth() + 1;
          dateLabels.push(`${month}/${day}`);
        }
        
        // 生成优化前的数据 - 高拥堵指数
        const beforeOptimization = [7.8, 7.5, 8.2, 7.9, 7.3, 6.9, 6.5];
        
        // 生成优化后的数据 - 优化后拥堵降低
        const afterOptimization = [7.8, 7.5, 8.2, 7.9, 5.2, 4.8, 4.5];
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: dateLabels,
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
            name: '拥堵指数',
            min: 0,
            max: 10,
            nameTextStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9
            },
            axisLine: {
              show: false
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            }
          },
          series: [
            {
              name: '优化前',
              type: 'line',
              data: beforeOptimization,
              symbolSize: 0,
              lineStyle: {
                width: 3,
                color: 'rgba(255, 165, 0, 0.7)'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(255, 165, 0, 0.4)' },
                  { offset: 1, color: 'rgba(255, 165, 0, 0)' }
                ])
              }
            },
            {
              name: '优化后',
              type: 'line',
              data: afterOptimization,
              symbolSize: 0,
              lineStyle: {
                width: 3,
                color: 'rgba(0, 210, 91, 0.7)'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(0, 210, 91, 0.4)' },
                  { offset: 1, color: 'rgba(0, 210, 91, 0)' }
                ])
              }
            }
          ]
        };
        
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
  .optimization-trend-chart {
    width: 100%;
    height: 100%;
  }
  </style>