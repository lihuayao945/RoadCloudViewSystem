<template>
    <div class="resource-usage-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'ResourceUsageChart',
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
        
        // 生成最近30分钟的时间点
        const timePoints = [];
        const now = new Date();
        for (let i = 29; i >= 0; i--) {
          const time = new Date(now.getTime() - i * 60 * 1000);
          const hours = time.getHours().toString().padStart(2, '0');
          const minutes = time.getMinutes().toString().padStart(2, '0');
          timePoints.push(`${hours}:${minutes}`);
        }
        
        // 生成CPU使用率数据 - 波动较大
        const cpuData = generateResourceData(30, 70, 0.3);
        
        // 生成内存使用率数据 - 相对稳定但有增长趋势
        const memoryData = generateResourceData(50, 80, 0.1, true);
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'cross',
              crossStyle: {
                color: '#999'
              }
            }
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: '20px',
            containLabel: true
          },
          legend: {
            data: ['CPU', '内存'],
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            },
            right: 10,
            top: 0
          },
          xAxis: {
            type: 'category',
            data: timePoints,
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9,
              interval: 5
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
            min: 0,
            max: 100,
            interval: 20,
            axisLabel: {
              formatter: '{value}%',
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9
            },
            axisLine: {
              show: false
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            }
          },
          series: [
            {
              name: 'CPU',
              type: 'line',
              data: cpuData,
              symbol: 'none',
              smooth: true,
              lineStyle: {
                width: 2,
                color: '#00c1de'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(0, 193, 222, 0.4)' },
                  { offset: 1, color: 'rgba(0, 193, 222, 0)' }
                ])
              }
            },
            {
              name: '内存',
              type: 'line',
              data: memoryData,
              symbol: 'none',
              smooth: true,
              lineStyle: {
                width: 2,
                color: '#8378ea'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(131, 120, 234, 0.4)' },
                  { offset: 1, color: 'rgba(131, 120, 234, 0)' }
                ])
              }
            }
          ]
        };
        
        chart.setOption(option)
      }
      
      function generateResourceData(min, max, volatility, hasGradualTrend = false) {
        const data = [];
        let value = min + Math.random() * (max - min);
        
        for (let i = 0; i < 30; i++) {
          // 添加随机波动
          const change = (Math.random() - 0.5) * volatility * (max - min);
          value += change;
          
          // 如果需要渐进趋势，加入一个很小的增长因子
          if (hasGradualTrend) {
            value += 0.2; // 微小增长
          }
          
          // 确保值在范围内
          value = Math.max(min, Math.min(max, value));
          
          data.push(Math.round(value * 10) / 10);
        }
        
        return data;
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
  .resource-usage-chart {
    width: 100%;
    height: 100%;
  }
  </style>