<template>
    <div class="event-type-chart" ref="chartContainer">
      <div class="chart-title">事件类型分布</div>
    </div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'EventTypeChart',
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
        
        const data = [
          { value: Math.floor(Math.random() * 30) + 20, name: '交通拥堵' },
          { value: Math.floor(Math.random() * 20) + 10, name: '设备故障' },
          { value: Math.floor(Math.random() * 15) + 5, name: '交通事故' },
          { value: Math.floor(Math.random() * 10) + 5, name: '信号异常' },
          { value: Math.floor(Math.random() * 8) + 2, name: '其他' }
        ];
        
        const option = {
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
          },
          color: ['#00c1de', '#ffb400', '#ff5733', '#41b883', '#8378ea'],
          legend: {
            orient: 'vertical',
            right: 10,
            top: 'center',
            itemWidth: 10,
            itemHeight: 10,
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            }
          },
          series: [
            {
              name: '事件类型',
              type: 'pie',
              radius: ['40%', '65%'],
              center: ['35%', '50%'],
              avoidLabelOverlap: false,
              label: {
                show: false
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: '10',
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: data
            }
          ]
        };
        
        chart.setOption(option)
      }
      
      return {
        chartContainer,
        refreshData
      }
    }
  }
  </script>
  
  <style scoped>
  .event-type-chart {
    width: 100%;
    height: 100%;
    position: relative;
  }
  
  .chart-title {
    position: absolute;
    top: 2px;
    left: 10px;
    font-size: 11px;
    color: #00ffff;
  }
  </style>