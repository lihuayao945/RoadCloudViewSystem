<template>
    <div class="pie-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  import * as echarts from 'echarts'
  import { generateRandomValue } from '../../utils/chartUtils'
  
  export default {
    name: 'PieChart',
    setup() {
      const chartContainer = ref(null)
      let chart = null
      
      onMounted(() => {
        initChart()
        window.addEventListener('resize', resizeChart)
      })
      
      onBeforeUnmount(() => {
        if (chart) {
          chart.dispose()
        }
        window.removeEventListener('resize', resizeChart)
      })
      
      function initChart() {
        chart = echarts.init(chartContainer.value)
        refreshData()
      }
      
      function getChartOption() {
        return {
          backgroundColor: 'transparent',
          color: ['#00ffff', '#0091ea', '#8e72ff', '#cf77fc', '#ffd600'],
          series: [
            {
              name: '区域统计',
              type: 'pie',
              radius: ['40%', '60%'],
              center: ['50%', '50%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#042235',
                borderWidth: 2
              },
              label: {
                show: true,
                position: 'outside',
                formatter: '{a|{c}%}\n{b|{b}}',
                rich: {
                  a: {
                    color: '#00ffff',
                    fontSize: 14,
                    fontWeight: 'bold',
                    lineHeight: 20
                  },
                  b: {
                    color: '#fff',
                    fontSize: 12,
                    lineHeight: 20
                  }
                }
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 16,
                  fontWeight: 'bold'
                },
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              },
              labelLine: {
                show: true
              },
              data: [
                { value: generateRandomValue(20, 30), name: '商业区' },
                { value: generateRandomValue(15, 25), name: '住宅区' },
                { value: generateRandomValue(15, 25), name: '教育区' },
                { value: generateRandomValue(15, 25), name: '工业区' },
                { value: generateRandomValue(5, 15), name: '绿化区' }
              ]
            }
          ],
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c}%'
          },
          animationDuration: 1000,
          animationEasing: 'cubicOut'
        }
      }
      
      function refreshData() {
        if (chart) {
          chart.setOption(getChartOption())
        }
      }
      
      function resizeChart() {
        if (chart) {
          chart.resize()
        }
      }
      
      return {
        chartContainer,
        refreshData
      }
    }
  }
  </script>
  
  <style scoped>
  .pie-chart {
    height: calc(100% - 120px);
    width: 100%;
  }
  </style>