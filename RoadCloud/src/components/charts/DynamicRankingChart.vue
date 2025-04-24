<template>
  <div class="dynamic-ranking-chart" ref="chartContainer"></div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import axios from 'axios';

export default {
  name: 'DynamicRankingChart',
  setup() {
    const chartContainer = ref(null);
    let chart = null;
    let currentData = [];
    
    // 高级科技风格渐变颜色配置
    const getGradientColors = (value, index) => {
      // 创建极致科技感的色彩组
      const colorSets = [
        // 乘用车 - 科技紫
        {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#8e24f8' },
            { offset: 0.3, color: '#c239fb' },
            { offset: 0.7, color: '#e54ff8' },
            { offset: 1, color: '#a259ff' }
          ],
          global: false
        },
        // 自行车 - 科技蓝
        {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#00c2ff' },
            { offset: 0.5, color: '#0ae4ff' },
            { offset: 1, color: '#12f1ff' }
          ],
          global: false
        },
        // 卡车 - 科技黄
        {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#ffa726' },
            { offset: 0.4, color: '#ffc547' },
            { offset: 1, color: '#ffdb4d' }
          ],
          global: false
        },
        // 备用 - 科技青
        {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#00dfc8' },
            { offset: 0.5, color: '#00f7d8' },
            { offset: 1, color: '#3fffed' }
          ],
          global: false
        },
        // 备用 - 科技绿
        {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#00c853' },
            { offset: 0.5, color: '#00e676' },
            { offset: 1, color: '#69f0ae' }
          ],
          global: false
        },
        // 备用 - 科技红
        {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#ff1744' },
            { offset: 0.5, color: '#ff4d69' },
            { offset: 1, color: '#ff8a8a' }
          ],
          global: false
        },
      ];
      
      // 根据值大小增强亮度
      const brightnessAdjust = Math.min(1 + (value / 1000) * 0.3, 1.3);
      
      // 选择颜色组
      const colorSet = JSON.parse(JSON.stringify(colorSets[index % colorSets.length]));
      
      // 根据数值增加亮度
      for (let stop of colorSet.colorStops) {
        let color = stop.color;
        let r = parseInt(color.slice(1, 3), 16);
        let g = parseInt(color.slice(3, 5), 16);
        let b = parseInt(color.slice(5, 7), 16);
        
        // 增强主色调
        r = Math.min(255, Math.round(r * brightnessAdjust));
        g = Math.min(255, Math.round(g * brightnessAdjust));
        b = Math.min(255, Math.round(b * brightnessAdjust));
        
        stop.color = `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
      }
      
      return colorSet;
    };
    
    // 获取发光效果颜色
    const getGlowColor = (index) => {
      const glowColors = [
        'rgba(142, 36, 248, 0.25)', // 紫色辉光
        'rgba(0, 194, 255, 0.25)',  // 蓝色辉光
        'rgba(255, 167, 38, 0.25)', // 黄色辉光
        'rgba(0, 223, 200, 0.25)',  // 青色辉光
        'rgba(0, 200, 83, 0.25)',   // 绿色辉光
        'rgba(255, 23, 68, 0.25)',  // 红色辉光
      ];
      return glowColors[index % glowColors.length];
    };
    
    // 初始化图表
    const initChart = async () => {
      if (!chartContainer.value) return;
      
      // 获取实时数据
      await fetchStreamRankData();
      
      chart = echarts.init(chartContainer.value);
      
      // 只在有数据时才渲染图表
      if (currentData.length > 0) {
        const option = {
          backgroundColor: 'rgba(16, 20, 28, 0.98)', // 深邃空间感背景
          grid: {
            top: 25,
            left: 10,
            right: 40,
            bottom: 20,
            containLabel: true
          },
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            },
            backgroundColor: 'rgba(18, 24, 38, 0.9)',
            borderColor: 'rgba(80, 160, 250, 0.4)',
            borderWidth: 1,
            textStyle: {
              color: 'rgba(255, 255, 255, 0.95)',
              fontSize: 12,
              fontFamily: '"Montserrat", "Arial", sans-serif'
            },
            formatter: function(params) {
              const colorStops = params[0].color.colorStops;
              const color = colorStops ? colorStops[0].color : params[0].color;
              return `<div style="font-weight:bold;margin-bottom:5px;font-size:13px;text-shadow:0 0 2px rgba(255,255,255,0.4);">${params[0].name}</div>
                      <div style="display:flex;align-items:center;gap:8px;line-height:1.5;">
                        <span style="display:inline-block;width:8px;height:8px;background:${color};border-radius:50%;box-shadow:0 0 5px ${color};"></span>
                        <span>车流量：<span style="font-weight:bold;color:#fff;">${params[0].value}</span> 辆/小时</span>
                      </div>`;
            }
          },
          xAxis: {
            type: 'value',
            nameTextStyle: {
              color: 'rgba(190, 210, 235, 0.8)',
              fontWeight: 'normal',
              fontSize: 11,
              fontFamily: '"Montserrat", "Arial", sans-serif',
              textShadow: '0 0 2px rgba(120, 180, 240, 0.3)'
            },
            axisLabel: {
              color: 'rgba(190, 210, 235, 0.8)',
              fontSize: 10,
              fontFamily: '"Montserrat", "Arial", sans-serif',
              textShadow: '0 0 1px rgba(120, 180, 240, 0.2)'
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(80, 120, 180, 0.25)',
                width: 1
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(80, 120, 180, 0.15)',
                type: [5, 5],
                width: 1
              }
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'category',
            data: currentData.map(item => item.name),
            inverse: true,
            axisLabel: {
              color: 'rgba(210, 220, 245, 0.95)',
              fontWeight: 'normal',
              fontSize: 12,
              fontFamily: '"Montserrat", "Arial", sans-serif',
              margin: 22,
              textShadow: '0 0 2px rgba(120, 180, 240, 0.4)'
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(80, 120, 180, 0.25)',
                width: 1
              }
            },
            axisTick: {
              show: false
            },
            splitLine: {
              show: false
            }
          },
          series: [
            // 主要数据条
            {
              type: 'bar',
              data: currentData.map((item, index) => ({
                value: item.value,
                itemStyle: {
                  color: getGradientColors(item.value, index)
                }
              })),
              label: {
                show: true,
                position: 'right',
                color: 'rgba(255, 255, 255, 1)',
                fontWeight: 'bold',
                fontSize: 13,
                fontFamily: '"Montserrat", "Arial", sans-serif',
                formatter: '{c}',
                textShadow: '0 0 4px rgba(255, 255, 255, 0.6)'
              },
              barWidth: '70%',
              itemStyle: {
                borderRadius: [2, 4, 4, 2], // 圆角
                shadowColor: 'rgba(0, 0, 0, 0.35)',
                shadowBlur: 10,
                borderWidth: 0.5,
                borderColor: 'rgba(255, 255, 255, 0.25)'
              }
            },
            
            // 内部微光效果
            {
              type: 'bar',
              z: 2,
              data: currentData.map((item, index) => ({
                value: item.value * 0.92,
                itemStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: 'rgba(255, 255, 255, 0.05)' },
                    { offset: 0.5, color: 'rgba(255, 255, 255, 0.3)' },
                    { offset: 1, color: 'rgba(255, 255, 255, 0.05)' }
                  ])
                }
              })),
              barWidth: '30%',
              barGap: '-100%',
              label: {
                show: false
              },
              itemStyle: {
                borderRadius: [1, 4, 4, 1],
              }
            },
            
            // 外部发光效果 - 第一层
            {
              type: 'bar',
              z: -1,
              data: currentData.map((item, index) => ({
                value: item.value * 1.02,
                itemStyle: {
                  color: getGlowColor(index),
                  opacity: 0.9
                }
              })),
              barWidth: '65%',
              barGap: '-100%',
              itemStyle: {
                borderRadius: [3, 8, 8, 3],
                shadowColor: 'rgba(120, 180, 220, 0.25)',
                shadowBlur: 8,
              }
            },
            
            // 外部发光效果 - 第二层（更大范围，更模糊）
            {
              type: 'bar',
              z: -2,
              data: currentData.map((item, index) => ({
                value: item.value * 1.04,
                itemStyle: {
                  color: getGlowColor(index),
                  opacity: 0.5
                }
              })),
              barWidth: '85%',
              barGap: '-100%',
              itemStyle: {
                borderRadius: [4, 10, 10, 4],
                shadowColor: 'rgba(120, 180, 220, 0.15)',
                shadowBlur: 15,
              }
            }
          ],
          animationDuration: 1200,
          animationEasing: 'cubicOut',
          animationDelay: function(idx) {
            return idx * 80 + 150;
          }
        };
        
        chart.setOption(option);
      } else {
        console.warn('No data available for dynamic ranking chart');
      }
    };
    
    // 获取流量排名数据
    const fetchStreamRankData = async () => {
      try {
        // 获取当前时间戳
        const timestamp = Date.now().toString();
        
        // 调用API获取数据
        const response = await axios.get('/menu/vehicle/stream/rank', {
          params: {
            timestamp
          }
        });
        
        if (response.data.status === 'success' && response.data.streams) {
          // 将API返回的数据转换为图表需要的格式
          currentData = response.data.streams.map(item => {
            return {
              name: item.landmark,
              value: item.stream
            };
          });
          
          // 按流量从大到小排序
          currentData.sort((a, b) => b.value - a.value);
          
          // 只保留前10个
          currentData = currentData.slice(0, 10);
        } else {
          console.error('Failed to fetch stream rank data');
          currentData = []; // 没有数据时，清空当前数据
        }
      } catch (error) {
        console.error('Error fetching stream rank data:', error);
        currentData = []; // 出错时，清空当前数据
      }
    };
    
    // 更新图表
    const updateChart = async () => {
      await fetchStreamRankData();
      
      if (!chart) {
        initChart();
        return;
      }
      
      // 只在有数据时更新图表
      if (currentData.length > 0) {
        chart.setOption({
          yAxis: {
            data: currentData.map(item => item.name)
          },
          series: [
            {
              data: currentData.map((item, index) => ({
                value: item.value,
                itemStyle: {
                  color: getGradientColors(item.value, index)
                }
              }))
            },
            {
              data: currentData.map((item, index) => ({
                value: item.value * 0.92,
                itemStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: 'rgba(255, 255, 255, 0.05)' },
                    { offset: 0.5, color: 'rgba(255, 255, 255, 0.3)' },
                    { offset: 1, color: 'rgba(255, 255, 255, 0.05)' }
                  ])
                }
              }))
            },
            {
              data: currentData.map((item, index) => ({
                value: item.value * 1.02,
                itemStyle: {
                  color: getGlowColor(index),
                  opacity: 0.9
                }
              }))
            },
            {
              data: currentData.map((item, index) => ({
                value: item.value * 1.04,
                itemStyle: {
                  color: getGlowColor(index),
                  opacity: 0.5
                }
              }))
            }
          ]
        });
      } else {
        console.warn('No data available to update dynamic ranking chart');
      }
    };
    
    onMounted(() => {
      initChart();
      // 设置定时更新
      const updateInterval = setInterval(updateChart, 30000); // 每30秒更新一次
      
      window.addEventListener('resize', handleResize);
      
      return () => {
        clearInterval(updateInterval);
      }
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
      updateChart,
      initChart
    };
  }
}
</script>

<style scoped>
.dynamic-ranking-chart {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.45), 
              0 0 12px rgba(70, 130, 240, 0.2), 
              0 0 20px rgba(0, 180, 255, 0.1);
  background: linear-gradient(135deg, 
                rgba(25, 32, 45, 0.98), 
                rgba(15, 20, 32, 0.98) 40%,
                rgba(12, 16, 28, 0.98) 80%,
                rgba(10, 15, 25, 0.98));
  border: 1px solid rgba(70, 130, 240, 0.2);
  position: relative;
  transition: all 0.3s ease;
}

/* 科技感网格背景 */
.dynamic-ranking-chart::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(0deg, rgba(70, 130, 240, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(70, 130, 240, 0.03) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
  z-index: -1;
}

/* 顶部边缘光效 */
.dynamic-ranking-chart::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    rgba(80, 120, 255, 0), 
    rgba(0, 180, 255, 0.8), 
    rgba(140, 0, 255, 0.8), 
    rgba(80, 120, 255, 0));
  z-index: 1;
  box-shadow: 0 0 10px rgba(0, 150, 255, 0.5),
              0 0 20px rgba(0, 150, 255, 0.2);
  animation: glowPulse 4s infinite ease-in-out;
}

/* 边缘发光动画 */
@keyframes glowPulse {
  0% {
    opacity: 0.6;
    filter: blur(0.5px);
  }
  50% {
    opacity: 1;
    filter: blur(1px);
  }
  100% {
    opacity: 0.6;
    filter: blur(0.5px);
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .dynamic-ranking-chart {
    border-radius: 6px;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.4), 
                0 0 8px rgba(70, 130, 240, 0.15);
  }
}
</style>