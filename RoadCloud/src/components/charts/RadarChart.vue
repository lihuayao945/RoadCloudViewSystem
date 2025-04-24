<template>
    <div :id="chartId" class="radar-chart"></div>
  </template>
  
  <script>
  import { onMounted, onBeforeUnmount, ref } from 'vue'
  import * as echarts from 'echarts'
  
  export default {
    name: 'RadarChart',
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
      const chartInstance = ref(null);
      
      // 为不同的图表定义不同的颜色
      const chartColors = [
        {
          line: '#8a2be2', // 紫色
          area: ['rgba(138, 43, 226, 0.7)', 'rgba(138, 43, 226, 0.1)']
        },
        {
          line: '#00ffff', // 青色
          area: ['rgba(0, 255, 255, 0.7)', 'rgba(0, 255, 255, 0.1)']
        },
        {
          line: '#ff1493', // 粉色
          area: ['rgba(255, 20, 147, 0.7)', 'rgba(255, 20, 147, 0.1)']
        },
        {
          line: '#ffa500', // 橙色
          area: ['rgba(255, 165, 0, 0.7)', 'rgba(255, 165, 0, 0.1)']
        }
      ];
      
      onMounted(() => {
        // 初始化图表
        initChart();
        
        // 窗口大小改变时，重绘图表
        window.addEventListener('resize', resizeChart);
      });
      
      onBeforeUnmount(() => {
        // 组件销毁前，移除事件监听，销毁图表实例
        window.removeEventListener('resize', resizeChart);
        if (chartInstance.value) {
          chartInstance.value.dispose();
        }
      });
      
      function initChart() {
        // 获取DOM元素
        const chartDom = document.getElementById(props.chartId);
        
        // 创建ECharts实例
        chartInstance.value = echarts.init(chartDom);
        
        // 获取当前图表的颜色
        const colorSet = chartColors[props.chartIndex];
        
        // 设置默认配置
        const option = {
          backgroundColor: 'transparent',
          radar: {
            indicator: [
              { name: '加载中...', max: 100 },
              { name: '加载中...', max: 100 },
              { name: '加载中...', max: 100 },
              { name: '加载中...', max: 100 },
              { name: '加载中...', max: 100 }
            ],
            center: ['50%', '60%'], // 更居中
            radius: '60%', // 减小雷达图的半径
            shape: 'polygon',
            splitNumber: 3, // 减少分割线
            axisName: {
              color: '#00ffff',
              fontSize: 9, // 减小标签字体
              padding: [2, 3], // 减小标签内边距
              formatter: function(value) {
                if(value.length > 3) { // 更简短的标签
                  return value.substring(0, 3) + '..';
                }
                return value;
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(14, 89, 134, 0.4)',
                width: 0.8 // 减小线宽
              }
            },
            splitArea: {
              show: true,
              areaStyle: {
                color: ['rgba(4, 34, 53, 0.2)', 'rgba(4, 34, 53, 0.4)']
              }
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(14, 89, 134, 0.6)',
                width: 1 // 减小轴线宽度
              }
            }
          },
          series: [
            {
              type: 'radar',
              data: [
                {
                  value: [0, 0, 0, 0, 0],
                  name: '指标值',
                  areaStyle: {
                    color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                      {
                        offset: 0,
                        color: colorSet.area[0]
                      },
                      {
                        offset: 1,
                        color: colorSet.area[1]
                      }
                    ])
                  },
                  lineStyle: {
                    color: colorSet.line,
                    width: 1.5 // 减小线宽
                  },
                  itemStyle: {
                    color: colorSet.line
                  },
                  symbol: 'circle',
                  symbolSize: 3 // 减小数据点大小
                }
              ]
            }
          ],
          animation: true,
          animationDuration: 600 // 减少动画时间
        };
        
        // 应用配置
        chartInstance.value.setOption(option);
      }
      
      function resizeChart() {
        if (chartInstance.value) {
          chartInstance.value.resize();
        }
      }
      
      function updateChartData(data) {
        if (!chartInstance.value) return;
        
        // 获取当前图表的颜色
        const colorSet = chartColors[props.chartIndex];
        
        // 更新雷达图数据
        chartInstance.value.setOption({
          radar: {
            indicator: data.indicators
          },
          series: [
            {
              data: [
                {
                  value: data.values,
                  areaStyle: {
                    color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                      {
                        offset: 0,
                        color: colorSet.area[0]
                      },
                      {
                        offset: 1,
                        color: colorSet.area[1]
                      }
                    ])
                  },
                  lineStyle: {
                    color: colorSet.line,
                    width: 1.5 // 减小线宽
                  },
                  itemStyle: {
                    color: colorSet.line
                  }
                }
              ]
            }
          ]
        });
      }
      
      return {
        updateChartData
      }
    }
  }
  </script>
  
  <style scoped>
  .radar-chart {
    width: 100%;
    height: calc(100% - 10px); /* 调整高度，留出更小的空间给标题 */
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: -2px; /* 微调上边距，让图表更紧凑 */
  }
  </style>