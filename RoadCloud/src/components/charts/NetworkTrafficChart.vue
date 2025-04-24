<template>
    <div class="network-traffic-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'NetworkTrafficChart',
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
        
        // 生成最近20分钟的时间点
        const timePoints = [];
        const now = new Date();
        for (let i = 19; i >= 0; i--) {
          const time = new Date(now.getTime() - i * 60 * 1000);
          const hours = time.getHours().toString().padStart(2, '0');
          const minutes = time.getMinutes().toString().padStart(2, '0');
          timePoints.push(`${hours}:${minutes}`);
        }
        
        // 生成入站流量数据
        const inboundData = generateNetworkData(10, 80);
        
        // 生成出站流量数据
        const outboundData = generateNetworkData(5, 40);
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            },
            formatter: function(params) {
              let result = params[0].name + '<br/>';
              params.forEach(param => {
                result += param.seriesName + ': ' + param.value + ' MB/s<br/>';
              });
              return result;
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
            data: ['入站', '出站'],
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
              interval: 4
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
            name: 'MB/s',
            nameTextStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 9
            },
            axisLabel: {
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
              name: '入站',
              type: 'bar',
              stack: 'total',
              barWidth: 8,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(0, 210, 91, 0.8)' },
                  { offset: 1, color: 'rgba(0, 210, 91, 0.3)' }
                ])
              },
              data: inboundData
            },
            {
              name: '出站',
              type: 'bar',
              stack: 'total',
              barWidth: 8,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(0, 145, 234, 0.8)' },
                  { offset: 1, color: 'rgba(0, 145, 234, 0.3)' }
                ])
              },
              data: outboundData
            }
          ]
        };
        
        chart.setOption(option)
      }
      
      function generateNetworkData(min, max) {
        const data = [];
        let value = min + Math.random() * (max - min);
        
        for (let i = 0; i < 20; i++) {
          // 添加随机波动
          const change = (Math.random() - 0.5) * 0.4 * (max - min);
          value += change;
          
          // 确保值在范围内
          value = Math.max(min, Math.min(max, value));
          
          // 添加一些突发流量
          if (Math.random() > 0.9) {
            value = Math.min(max, value * (1 + Math.random()));
          }
          
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
  .network-traffic-chart {
    width: 100%;
    height: 100%;
  }
  </style>