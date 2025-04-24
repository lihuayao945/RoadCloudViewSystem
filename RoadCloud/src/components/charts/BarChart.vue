<template>
  <div class="bar-chart" ref="chartContainer"></div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

export default {
  name: 'BarChart',
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
    
    async function fetchData() {
      try {
        // Get current date
        const now = new Date()
        const endTime = now.getTime()
        
        // Set start time to 24 hours ago
        const startTime = endTime - 24 * 60 * 60 * 1000
        
        // For demonstration, simulate API call with mock data
        // Replace this with actual API call in production
        /* 
        const response = await axios.get('/menu/vehicle/kindnum/all', {
          params: {
            starttime: startTime.toString(),
            endtime: endTime.toString()
          }
        })
        */
        
        // Mock response for testing
        const mockResponse = {
          status: 'success',
          rcuId: "12345",
          data: [
            { type: "小型轿车", count: 350, percentage: 0.58 },
            { type: "SUV", count: 120, percentage: 0.20 },
            { type: "货车", count: 80, percentage: 0.13 },
            { type: "公交车", count: 30, percentage: 0.05 },
            { type: "其他", count: 25, percentage: 0.04 }
          ]
        };
        
        const response = { data: mockResponse };
        
        if (response.data && response.data.status === 'success') {
          return response.data.data || [];
        } else {
          console.error('Failed to fetch vehicle data')
          return []
        }
      } catch (error) {
        console.error('Error fetching vehicle data:', error)
        return []
      }
    }
    
    function initChart() {
      chart = echarts.init(chartContainer.value)
      refreshData()
    }
    
    function getChartOption(data) {
      // Extract vehicle types and counts
      const types = data.map(item => item.type)
      const counts = data.map(item => item.count)
      
      return {
        backgroundColor: 'transparent',
        grid: {
          left: '5%',
          right: '5%',
          top: '10%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: types,
          axisLine: {
            lineStyle: {
              color: '#0e5986'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10,
            rotate: types.length > 5 ? 30 : 0,
            interval: 0
          }
        },
        yAxis: {
          type: 'value',
          name: '车辆数量',
          nameTextStyle: {
            color: '#fff',
            fontSize: 10
          },
          axisLine: {
            lineStyle: {
              color: '#0e5986'
            }
          },
          splitLine: {
            lineStyle: {
              color: 'rgba(14, 89, 134, 0.3)'
            }
          },
          axisLabel: {
            color: '#fff',
            formatter: '{value}'
          }
        },
        series: [
          {
            name: '车辆数量',
            type: 'bar',
            data: counts,
            barWidth: '40%',
            itemStyle: {
              color: function(params) {
                // Create a gradient color based on the index
                const colorList = [
                  new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#8e72ff' },
                    { offset: 1, color: '#8e72ff80' }
                  ]),
                  new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#cf77fc' },
                    { offset: 1, color: '#cf77fc80' }
                  ]),
                  new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#4eadff' },
                    { offset: 1, color: '#4eadff80' }
                  ]),
                  new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#37d9ff' },
                    { offset: 1, color: '#37d9ff80' }
                  ]),
                  new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#ffaf51' },
                    { offset: 1, color: '#ffaf5180' }
                  ])
                ]
                return colorList[params.dataIndex % colorList.length]
              }
            }
          }
        ],
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: function(params) {
            const item = params[0]
            const index = item.dataIndex
            const vehicleData = data[index] || {}
            const percentage = vehicleData.percentage ? (vehicleData.percentage * 100).toFixed(1) : 0
            
            return `${item.name}<br/>车辆数量: ${item.value}<br/>占比: ${percentage}%`
          }
        },
        animationDuration: 1000
      }
    }
    
    async function refreshData() {
      try {
        const data = await fetchData()
        
        if (chart && data.length > 0) {
          chart.setOption(getChartOption(data))
        } else if (chart) {
          // If no data is returned, show a placeholder chart
          const placeholderData = [
            {type: '暂无数据', count: 0, percentage: 0}
          ]
          chart.setOption(getChartOption(placeholderData))
        }
      } catch (error) {
        console.error('Error refreshing chart data:', error)
        
        if (chart) {
          // Show error state in chart
          const errorData = [
            {type: '数据加载失败', count: 0, percentage: 0}
          ]
          chart.setOption(getChartOption(errorData))
        }
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
.bar-chart {
  height: calc(100% - 30px);
  width: 100%;
}
</style>