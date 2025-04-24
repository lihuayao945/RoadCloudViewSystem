<template>
    <div class="area-distribution-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'AreaDistributionChart',
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
        
        const areas = ['渝中区', '江北区', '南岸区', '沙坪坝区', '渝北区', '九龙坡区', '大渡口区', '北碚区'];
        const metrics = ['拥堵指数', '车流量', '事件数'];
        
        const data = [
          // 拥堵指数数据
          [0, 0, generateValue(6, 9)],  // 渝中区
          [0, 1, generateValue(5, 8)],  // 江北区
          [0, 2, generateValue(4, 7)],  // 南岸区
          [0, 3, generateValue(4, 6)],  // 沙坪坝区
          [0, 4, generateValue(5, 7)],  // 渝北区
          [0, 5, generateValue(4, 6)],  // 九龙坡区
          [0, 6, generateValue(3, 5)],  // 大渡口区
          [0, 7, generateValue(3, 6)],  // 北碚区
          
          // 车流量数据 (以千辆为单位)
          [1, 0, generateValue(30, 50)], // 渝中区
          [1, 1, generateValue(25, 45)], // 江北区
          [1, 2, generateValue(20, 35)], // 南岸区
          [1, 3, generateValue(15, 30)], // 沙坪坝区
          [1, 4, generateValue(25, 40)], // 渝北区
          [1, 5, generateValue(20, 35)], // 九龙坡区
          [1, 6, generateValue(10, 20)], // 大渡口区
          [1, 7, generateValue(15, 25)], // 北碚区
          
          // 事件数
          [2, 0, generateValue(8, 15)],  // 渝中区
          [2, 1, generateValue(5, 12)],  // 江北区
          [2, 2, generateValue(3, 10)],  // 南岸区
          [2, 3, generateValue(4, 9)],   // 沙坪坝区
          [2, 4, generateValue(6, 12)],  // 渝北区
          [2, 5, generateValue(5, 10)],  // 九龙坡区
          [2, 6, generateValue(2, 7)],   // 大渡口区
          [2, 7, generateValue(3, 8)]    // 北碚区
        ];
        
        const option = {
          tooltip: {
            position: 'top',
            formatter: function(params) {
              return `${areas[params.data[1]]} - ${metrics[params.data[0]]}<br>${params.data[2]}`;
            }
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: '45px',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: metrics,
            axisTick: {
              show: false
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            }
          },
          yAxis: {
            type: 'category',
            data: areas,
            axisTick: {
              show: false
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            }
          },
          visualMap: {
            min: 0,
            max: 50,
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            top: '10px',
            inRange: {
              color: ['rgba(0, 193, 222, 0.5)', 'rgba(255, 165, 0, 0.8)', 'rgba(255, 77, 79, 0.8)']
            },
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            }
          },
          series: [{
            name: '区域交通分析',
            type: 'heatmap',
            data: data,
            label: {
              show: true,
              color: '#fff',
              fontSize: 10
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 193, 222, 0.5)'
              }
            }
          }]
        };
        
        chart.setOption(option)
      }
      
      function generateValue(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
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
  .area-distribution-chart {
    width: 100%;
    height: 100%;
  }
  </style>