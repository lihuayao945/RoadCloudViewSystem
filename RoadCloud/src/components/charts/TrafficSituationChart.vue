<template>
    <div class="traffic-situation-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import * as echarts from 'echarts'
  import { ref, onMounted, onBeforeUnmount } from 'vue'
  
  export default {
    name: 'TrafficSituationChart',
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
        
        const hours = [];
        const congestionData = [];
        const speedData = [];
        
        // 获取当前小时和分钟
        const now = new Date();
        const currentHour = now.getHours();
        const currentMinute = now.getMinutes();
        
        // 生成过去12小时的数据
        for (let i = 11; i >= 0; i--) {
          let hour = (currentHour - i);
          if (hour < 0) hour += 24;
          
          let timeLabel;
          if (i === 0) {
            // 当前时间点显示小时和分钟
            timeLabel = `${hour}:${currentMinute.toString().padStart(2, '0')}`;
          } else {
            timeLabel = `${hour}:00`;
          }
          
          hours.push(timeLabel);
          
          // 根据时间生成拥堵指数和速度
          // 早高峰 7-9点
          if (hour >= 7 && hour < 9) {
            congestionData.push((Math.random() * 2) + 7);  // 拥堵指数 7-9
            speedData.push((Math.random() * 10) + 20);     // 速度 20-30
          }
          // 晚高峰 17-19点
          else if (hour >= 17 && hour < 19) {
            congestionData.push((Math.random() * 2) + 6.5); // 拥堵指数 6.5-8.5
            speedData.push((Math.random() * 15) + 25);      // 速度 25-40
          }
          // 中午 11-13点
          else if (hour >= 11 && hour < 13) {
            congestionData.push((Math.random() * 2) + 5);   // 拥堵指数 5-7
            speedData.push((Math.random() * 15) + 35);      // 速度 35-50
          }
          // 其他白天时间
          else if (hour >= 6 && hour < 22) {
            congestionData.push((Math.random() * 3) + 3);   // 拥堵指数 3-6
            speedData.push((Math.random() * 20) + 40);      // 速度 40-60
          }
          // 夜间
          else {
            congestionData.push((Math.random() * 2) + 1);   // 拥堵指数 1-3
            speedData.push((Math.random() * 30) + 50);      // 速度 50-80
          }
        }
        
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
            bottom: '10%',
            top: '15%',
            containLabel: true
          },
          legend: {
            data: ['拥堵指数', '平均速度'],
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 10
            },
            right: 10,
            top: 0
          },
          xAxis: [
            {
              type: 'category',
              data: hours,
              axisPointer: {
                type: 'shadow'
              },
              axisLine: {
                lineStyle: {
                  color: 'rgba(255, 255, 255, 0.3)'
                }
              },
              axisLabel: {
                color: 'rgba(255, 255, 255, 0.7)',
                fontSize: 10,
                interval: 1
              },
              axisTick: {
                show: false
              }
            }
          ],
          yAxis: [
            {
              type: 'value',
              name: '拥堵指数',
              nameTextStyle: {
                color: 'rgba(255, 165, 0, 0.7)',
                fontSize: 10
              },
              min: 0,
              max: 10,
              interval: 2,
              axisLabel: {
                formatter: '{value}',
                color: 'rgba(255, 165, 0, 0.7)',
                fontSize: 10
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
            {
              type: 'value',
              name: '速度 km/h',
              nameTextStyle: {
                color: 'rgba(0, 193, 222, 0.7)',
                fontSize: 10
              },
              min: 0,
              max: 80,
              interval: 20,
              axisLabel: {
                formatter: '{value}',
                color: 'rgba(0, 193, 222, 0.7)',
                fontSize: 10
              },
              axisLine: {
                show: false
              },
              splitLine: {
                lineStyle: {
                  color: 'rgba(255, 255, 255, 0.1)'
                }
              }
            }
          ],
          series: [
            {
              name: '拥堵指数',
              type: 'bar',
              barWidth: 6,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(255, 165, 0, 0.9)' },
                  { offset: 1, color: 'rgba(255, 165, 0, 0.3)' }
                ])
              },
              data: congestionData
            },
            {
              name: '平均速度',
              type: 'line',
              yAxisIndex: 1,
              symbol: 'none',
              smooth: true,
              lineStyle: {
                width: 3,
                color: 'rgba(0, 193, 222, 0.8)'
              },
              data: speedData
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
  .traffic-situation-chart {
    width: 100%;
    height: 100%;
  }
  </style>