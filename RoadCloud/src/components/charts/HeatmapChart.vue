<template>
    <div class="heatmap-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
  
  export default {
    name: 'HeatmapChart',
    props: {
      activeTab: {
        type: String,
        default: 'real'
      }
    },
    setup(props) {
      const chartContainer = ref(null)
      let chart = null
      
      // 监听活动标签变化
      watch(() => props.activeTab, (newTab) => {
        if (chart) {
          updateChartData(newTab)
        }
      })
      
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
        updateChartData(props.activeTab)
        
        // 设置自动调整大小
        window.addEventListener('resize', () => {
          if (chart) chart.resize()
        })
      }
      
      // 更新图表数据
      const updateChartData = (tabType) => {
        if (!chart) return
        
        // 根据活动标签类型生成不同的数据
        const data = generateHeatmapData(tabType)
        
        const option = {
          animation: true,
          tooltip: {
            position: 'top',
            formatter: function (params) {
              return `${params.data[1]}-${params.data[0]}<br>车流量: ${params.data[2]}辆`
            }
          },
          grid: {
            top: '10%',
            left: '3%',
            right: '3%',
            bottom: '10%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: ['0', '3', '6', '9', '12', '15', '18', '21', '24'],
            splitArea: {
              show: true
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            },
            splitLine: {
              show: false
            }
          },
          yAxis: {
            type: 'category',
            data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
            splitArea: {
              show: true
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            },
            splitLine: {
              show: false
            }
          },
          visualMap: {
            min: 0,
            max: 10,
            calculable: false,
            orient: 'horizontal',
            left: 'center',
            bottom: '0%',
            show: false,
            inRange: {
              color: [
                'rgba(0, 193, 222, 0.2)',
                'rgba(0, 193, 222, 0.5)',
                'rgba(0, 255, 255, 0.8)',
                'rgba(255, 255, 0, 0.8)',
                'rgba(255, 0, 0, 0.8)'
              ]
            }
          },
          series: [{
            name: '车流量',
            type: 'heatmap',
            data: data,
            label: {
              show: false
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 255, 255, 0.5)'
              }
            }
          }]
        }
        
        chart.setOption(option)
      }
      
      // 生成热力图数据
      const generateHeatmapData = (type) => {
        const days = 7
        const hours = 9 // 使用0, 3, 6...24小时制，共9个点
        const data = []
        
        // 不同类型的数据生成略有不同
        const isPredict = type === 'predict'
        
        // 对于每一天
        for (let d = 0; d < days; d++) {
          // 对于每个小时点
          for (let h = 0; h < hours; h++) {
            // 基础车流量模式：工作日早晚高峰，周末中午高峰
            let baseValue
            
            // 周一至周五
            if (d < 5) {
              // 早高峰 (6-9点)
              if (h === 2 || h === 3) {
                baseValue = 8 + Math.random() * 2
              }
              // 晚高峰 (18-21点)
              else if (h === 6 || h === 7) {
                baseValue = 9 + Math.random() * 1
              }
              // 中午 (12-15点)
              else if (h === 4 || h === 5) {
                baseValue = 5 + Math.random() * 2
              }
              // 其他时间
              else {
                baseValue = 2 + Math.random() * 3
              }
            }
            // 周末
            else {
              // 中午高峰 (12-15点)
              if (h === 4 || h === 5) {
                baseValue = 7 + Math.random() * 3
              }
              // 晚高峰 (18-21点)
              else if (h === 6 || h === 7) {
                baseValue = 8 + Math.random() * 2
              }
              // 其他时间
              else {
                baseValue = 3 + Math.random() * 4
              }
            }
            
            // 对于预测数据，模拟更平滑的模式并略微高于实时数据
            if (isPredict) {
              // 增加一点点平滑度和预测性
              baseValue = Math.min(10, baseValue * (1 + Math.random() * 0.1))
              
              // 预测数据更平滑，减少极端值
              baseValue = Math.max(2, Math.min(10, baseValue))
            }
            
            // 添加数据点 [x, y, value]
            data.push([h.toString() * 3, d.toString(), Math.round(baseValue * 10) / 10])
          }
        }
        
        return data
      }
      
      return {
        chartContainer,
        initChart,
        updateChartData
      }
    }
  }
  </script>
  
  <style scoped>
  .heatmap-chart {
    width: 100%;
    height: 100%;
  }
  </style>