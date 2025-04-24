<template>
    <div class="event-trend-chart" ref="chartContainer">
      <div class="chart-title">24小时事件趋势</div>
    </div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'EventTrendChart',
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
      
      // 生成数据
      const generateTrendData = () => {
        const hours = [];
        const trafficJamData = [];
        const accidentData = [];
        const deviceIssueData = [];
        
        // 获取当前小时
        const currentHour = new Date().getHours();
        
        // 生成过去24小时的标签
        for (let i = 0; i < 24; i++) {
          // 从24小时前到现在
          let hour = (currentHour - 23 + i + 24) % 24;
          hours.push(hour + ':00');
          
          // 模拟不同的趋势
          // 交通拥堵在早晚高峰最高
          if (hour >= 7 && hour <= 9 || hour >= 17 && hour <= 19) {
            trafficJamData.push(Math.floor(Math.random() * 10) + 15);
          } else if (hour >= 11 && hour <= 13) {
            trafficJamData.push(Math.floor(Math.random() * 8) + 8);
          } else if (hour >= 22 || hour < 6) {
            trafficJamData.push(Math.floor(Math.random() * 4));
          } else {
            trafficJamData.push(Math.floor(Math.random() * 5) + 5);
          }
          
          // 事故在早晚高峰和周末午后较高
          if (hour >= 7 && hour <= 9 || hour >= 17 && hour <= 19) {
            accidentData.push(Math.floor(Math.random() * 5) + 3);
          } else if (hour >= 13 && hour <= 15) {
            accidentData.push(Math.floor(Math.random() * 4) + 2);
          } else if (hour >= 22 || hour < 6) {
            accidentData.push(Math.floor(Math.random() * 2));
          } else {
            accidentData.push(Math.floor(Math.random() * 3) + 1);
          }
          
          // 设备问题分布更均匀，但夜间略低
          if (hour >= 22 || hour < 6) {
            deviceIssueData.push(Math.floor(Math.random() * 3) + 1);
          } else {
            deviceIssueData.push(Math.floor(Math.random() * 4) + 3);
          }
        }
        
        return {
          hours,
          trafficJamData,
          accidentData,
          deviceIssueData
        };
      };
      
      // 刷新数据
      const refreshData = () => {
        if (!chart) return
        
        const data = generateTrendData();
        
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
            top: '25%',
            containLabel: true
          },
          legend: {
            data: ['交通拥堵', '交通事故', '设备问题'],
            top: 0,
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            },
            itemWidth: 10,
            itemHeight: 10
          },
          xAxis: {
            type: 'category',
            data: data.hours,
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.3)'
              }
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9,
              interval: 3
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
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
              name: '交通拥堵',
              type: 'line',
              data: data.trafficJamData,
              smooth: true,
              symbol: 'none',
              itemStyle: {
                color: '#00c1de'
              }
            },
            {
              name: '交通事故',
              type: 'line',
              data: data.accidentData,
              smooth: true,
              symbol: 'none',
              itemStyle: {
                color: '#ffb400'
              }
            },
            {
              name: '设备问题',
              type: 'line',
              data: data.deviceIssueData,
              smooth: true,
              symbol: 'none',
              itemStyle: {
                color: '#8378ea'
              }
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
  .event-trend-chart {
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