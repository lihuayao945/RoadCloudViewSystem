<template>
    <div class="gauge-chart" :id="chartId"></div>
  </template>
  
  <script>
  import { onMounted, onBeforeUnmount, ref, watch } from 'vue';
  import * as echarts from 'echarts';
  import { generateRandomValue } from '../../utils/chartUtils';
  
  export default {
    name: 'GaugeChart',
    props: {
      chartId: {
        type: String,
        required: true
      },
      chartIndex: {
        type: Number,
        default: 0
      }
    },
    setup(props) {
      let chart = null;
      const chartData = ref({
        value: 50,
        name: '性能',
        threshold: [30, 70, 100]
      });
      
      onMounted(() => {
        initChart();
        window.addEventListener('resize', handleResize);
      });
      
      onBeforeUnmount(() => {
        if (chart) {
          chart.dispose();
          chart = null;
        }
        window.removeEventListener('resize', handleResize);
      });
      
      const handleResize = () => {
        if (chart) {
          chart.resize();
        }
      };
      
      const initChart = () => {
        const chartDom = document.getElementById(props.chartId);
        if (!chartDom) return;
        
        chart = echarts.init(chartDom);
        
        updateChart();
      };
      
      const updateChart = () => {
        if (!chart) return;
        
        // 根据阈值决定颜色
        const getGaugeColor = (value, threshold) => {
          if (value <= threshold[0]) return '#52c41a'; // 良好状态 - 绿色
          if (value <= threshold[1]) return '#fadb14'; // 中等状态 - 黄色
          return '#f5222d'; // 较差状态 - 红色
        };
        
        const option = {
          series: [
            {
              type: 'gauge',
              radius: '100%',
              startAngle: 180,
              endAngle: 0,
              min: 0,
              max: 100,
              splitNumber: 10,
              axisLine: {
                lineStyle: {
                  width: 6,
                  color: [
                    [chartData.value.threshold[0] / 100, '#52c41a'],
                    [chartData.value.threshold[1] / 100, '#fadb14'],
                    [1, '#f5222d']
                  ]
                }
              },
              pointer: {
                icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
                length: '12%',
                width: 5,
                offsetCenter: [0, '-60%'],
                itemStyle: {
                  color: getGaugeColor(chartData.value.value, chartData.value.threshold)
                }
              },
              axisTick: {
                length: 6,
                lineStyle: {
                  color: 'auto',
                  width: 1
                }
              },
              splitLine: {
                length: 10,
                lineStyle: {
                  color: 'auto',
                  width: 2
                }
              },
              axisLabel: {
                color: 'rgba(255, 255, 255, 0.7)',
                fontSize: 10,
                distance: -30,
                formatter: function(value) {
                  if (value === 0 || value === 100) {
                    return value + '%';
                  }
                  return '';
                }
              },
              title: {
                offsetCenter: [0, '-20%'],
                fontSize: 12,
                color: '#fff'
              },
              detail: {
                fontSize: 26,
                offsetCenter: [0, '30%'],
                valueAnimation: true,
                formatter: function(value) {
                  return Math.round(value) + '%';
                },
                color: getGaugeColor(chartData.value.value, chartData.value.threshold)
              },
              data: [
                {
                  value: chartData.value.value,
                  name: chartData.value.name
                }
              ]
            }
          ],
          animation: true
        };
        
        chart.setOption(option);
      };
      
      const updateChartData = (data) => {
        chartData.value = data;
        updateChart();
      };
      
      // 监听数据变化
      watch(chartData, () => {
        updateChart();
      });
      
      return {
        updateChartData
      };
    }
  }
  </script>
  
  <style scoped>
  .gauge-chart {
    width: 100%;
    height: calc(100% - 16px);
  }
  </style>