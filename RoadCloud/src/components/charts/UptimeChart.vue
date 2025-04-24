<template>
    <div class="uptime-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'UptimeChart',
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
        
        // 生成过去12个月的标签
        const months = [];
        const now = new Date();
        for (let i = 11; i >= 0; i--) {
          const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
          const month = date.getMonth() + 1;
          const year = date.getFullYear();
          months.push(`${year}-${month}`);
        }
        
        // 生成可用性数据 - 一般都很高
        const availabilityData = generateUptimeData(99.5, 100);
        
        const option = {
          tooltip: {
            trigger: 'axis',
            formatter: '{b}<br/>可用性: {c}%'
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
            data: months,
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
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
            min: 99,
            max: 100,
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
              data: availabilityData,
              type: 'line',
              smooth: true,
              symbol: 'emptyCircle',
              symbolSize: 4,
              lineStyle: {
                width: 2,
                color: '#00ffff'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(0, 255, 255, 0.3)' },
                  { offset: 1, color: 'rgba(0, 255, 255, 0)' }
                ])
              },
              markLine: {
                silent: true,
                lineStyle: {
                  color: '#ff9800',
                  type: 'dashed'
                },
                data: [
                  { yAxis: 99.9, name: 'SLA标准' }
                ],
                label: {
                  formatter: 'SLA: 99.9%',
                  position: 'start',
                  color: '#ff9800',
                  fontSize: 10
                }
              }
            }
          ]
        };
        
        chart.setOption(option)
      }
      
      function generateUptimeData(min, max) {
        const data = [];
        
        for (let i = 0; i < 12; i++) {
          // 生成高可用性数据，但偶尔会有小下降
          let value;
          if (Math.random() > 0.8) {
            // 20%的概率生成较低的可用性数据
            value = min + Math.random() * 0.3;
          } else {
            // 80%的概率生成较高的可用性数据
            value = min + 0.3 + Math.random() * (max - min - 0.3);
          }
          
          data.push(Math.round(value * 100) / 100);
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
  .uptime-chart {
    width: 100%;
    height: 100%;
  }
  </style>