<template>
    <div class="district-chart" ref="chartContainer"></div>
  </template>
  
  <script>
  import { ref, onMounted, onUnmounted } from 'vue';
  import * as echarts from 'echarts';
  import { generateRandomValue } from '../../utils/chartUtils';
  
  export default {
    name: 'DistrictChart',
    setup() {
      const chartContainer = ref(null);
      let chart = null;
      
      const districts = [
        '渝中区', '江北区', '南岸区', '渝北区', '沙坪坝区', 
        '九龙坡区', '大渡口区', '巴南区', '北碚区', '两江新区'
      ];
      
      // 初始化数据
      const generateChartData = () => {
        return districts.map(district => ({
          name: district,
          congestionIndex: +(generateRandomValue(1, 8).toFixed(1)),
          speedRate: generateRandomValue(30, 90),
          trafficVolume: generateRandomValue(500, 3000)
        }));
      };
      
      const initChart = () => {
        if (!chartContainer.value) return;
        
        const data = generateChartData();
        
        // 根据拥堵指数排序
        data.sort((a, b) => b.congestionIndex - a.congestionIndex);
        
        // 初始化图表
        chart = echarts.init(chartContainer.value);
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            },
            formatter: function(params) {
              const district = params[0].name;
              const congestionIndex = params[0].value;
              const speedRate = data.find(item => item.name === district).speedRate;
              const trafficVolume = data.find(item => item.name === district).trafficVolume;
              
              return `<div style="font-size:14px;color:#fff;font-weight:bold;margin-bottom:4px">${district}</div>
                      <div style="font-size:12px;color:#fff;margin-bottom:3px">
                        <span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:#5470c6;"></span>
                        拥堵指数: ${congestionIndex}
                      </div>
                      <div style="font-size:12px;color:#fff;margin-bottom:3px">
                        <span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:#91cc75;"></span>
                        平均车速: ${speedRate} km/h
                      </div>
                      <div style="font-size:12px;color:#fff;">
                        <span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:#fac858;"></span>
                        流量: ${trafficVolume} 辆/小时
                      </div>`;
            }
          },
          grid: {
            left: '3%',
            right: '8%',
            bottom: '3%',
            top: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'value',
            name: '拥堵指数',
            nameLocation: 'end',
            nameTextStyle: {
              color: 'rgba(255, 255, 255, 0.6)',
              padding: [0, 0, 0, 0]
            },
            max: 10,
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.6)',
              formatter: '{value}'
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            }
          },
          yAxis: {
            type: 'category',
            data: data.map(item => item.name),
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.8)'
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)'
              }
            }
          },
          series: [
            {
              name: '拥堵指数',
              type: 'bar',
              data: data.map(item => item.congestionIndex),
              itemStyle: {
                color: function(params) {
                  // 根据拥堵指数变化颜色
                  const value = params.value;
                  if (value < 3) return '#52c41a'; // 低拥堵，绿色
                  if (value < 5) return '#fadb14'; // 中低拥堵，黄色
                  if (value < 7) return '#fa8c16'; // 中高拥堵，橙色
                  return '#f5222d'; // 高拥堵，红色
                },
                borderRadius: [0, 4, 4, 0]
              },
              label: {
                show: true,
                position: 'right',
                color: 'rgba(255, 255, 255, 0.7)'
              }
            }
          ],
          // 视觉映射
          visualMap: {
            show: false,
            min: 0,
            max: 10,
            dimension: 0,
            inRange: {
              color: ['#52c41a', '#fadb14', '#fa8c16', '#f5222d']
            }
          }
        };
        
        chart.setOption(option);
      };
      
      const refreshData = () => {
        if (!chart) {
          initChart();
          return;
        }
        
        const data = generateChartData();
        
        // 根据拥堵指数排序
        data.sort((a, b) => b.congestionIndex - a.congestionIndex);
        
        const option = {
          yAxis: {
            data: data.map(item => item.name)
          },
          series: [
            {
              data: data.map(item => item.congestionIndex)
            }
          ]
        };
        
        chart.setOption(option);
      };
      
      onMounted(() => {
        window.addEventListener('resize', handleResize);
      });
      
      onUnmounted(() => {
        window.removeEventListener('resize', handleResize);
        if (chart) {
          chart.dispose();
          chart = null;
        }
      });
      
      const handleResize = () => {
        if (chart) {
          chart.resize();
        }
      };
      
      return {
        chartContainer,
        initChart,
        refreshData
      };
    }
  }
  </script>
  
  <style scoped>
  .district-chart {
    width: 100%;
    height: 100%;
  }
  </style>