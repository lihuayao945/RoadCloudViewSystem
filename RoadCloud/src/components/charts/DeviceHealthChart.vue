<template>
  <div class="chart-container" ref="chartContainer">
    <!-- Tech-style loading indicator -->
    <div class="tech-loading" v-if="isLoading">
      <div class="tech-loading-circle"></div>
      <div class="tech-loading-text">加载数据中...</div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, defineProps, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { generateRandomValue } from '../../utils/chartUtils'

export default {
  name: 'DeviceHealthChart',
  props: {
    healthData: {
      type: Object,
      default: () => ({})
    }
  },
  setup(props) {
    const chartContainer = ref(null)
    let chart = null
    const isLoading = ref(true)
    
    onMounted(() => {
      // 延迟初始化以确保DOM已准备好
      setTimeout(() => {
        try {
          window.addEventListener('resize', handleResize)
          initChart()
          setTimeout(() => {
            isLoading.value = false
          }, 800)
        } catch (error) {
          console.error('Error initializing chart:', error)
          isLoading.value = false
        }
      }, 100)
    })
    
    onBeforeUnmount(() => {
      try {
        if (chart) {
          chart.dispose()
          chart = null
        }
        window.removeEventListener('resize', handleResize)
      } catch (error) {
        console.error('Error during component unmount:', error)
      }
    })
    
    // 添加对props.healthData的监听
    watch(() => props.healthData, (newData) => {
      console.log('DeviceHealthChart - healthData变化:', JSON.stringify(newData))
      if (!chartContainer.value) {
        console.warn('Chart container not ready for update')
        return
      }
      
      if (newData && Object.keys(newData).length > 0) {
        isLoading.value = true
        
        // 使用nextTick确保DOM更新后再更新图表
        nextTick(() => {
          setTimeout(() => {
            try {
              updateChart(newData)
            } catch (error) {
              console.error('Error updating chart:', error)
              // 出错时使用随机数据作为备选
              useRandomData()
            } finally {
              isLoading.value = false
            }
          }, 400)
        })
      }
    }, { deep: true })
    
    function handleResize() {
      try {
        if (chart) {
          chart.resize()
        }
      } catch (error) {
        console.error('Error resizing chart:', error)
      }
    }
    
    function initChart() {
      // 添加检查，确保容器存在
      if (!chartContainer.value) {
        console.warn('Chart container is not available yet')
        setTimeout(initChart, 100) // 延迟重试
        return
      }
      
      try {
        if (chart) {
          chart.dispose()
        }
        
        chart = echarts.init(chartContainer.value)
        
        // 如果已有数据，直接使用；否则使用模拟数据
        if (props.healthData && Object.keys(props.healthData).length > 0) {
          console.log('DeviceHealthChart - 初始化使用传入数据:', JSON.stringify(props.healthData))
          updateChart(props.healthData)
        } else {
          console.log('DeviceHealthChart - 初始化使用随机数据')
          useRandomData()
        }
      } catch (error) {
        console.error('Error initializing chart:', error)
      }
    }
    
    // 使用API数据更新图表 - 修正版本
    function updateChart(data) {
      try {
        if (!chartContainer.value || !chart) {
          console.warn('Chart container or chart instance is not available for update')
          return
        }
        
        // 详细记录输入数据
        console.log('DeviceHealthChart - 更新图表的输入数据:', JSON.stringify(data))
        
        // 检查数据是否存在
        if (!data || Object.keys(data).length === 0) {
          console.warn('没有收到有效的设备健康数据，使用模拟数据')
          useRandomData()
          return
        }
        
        // 提取设备类型和对应的状态数据
        const deviceTypes = Object.keys(data)
        const healthyData = []
        const warningData = []
        const offlineData = []
        
        // 计算每个设备的总数，用于后续计算
        const totals = []
        
        deviceTypes.forEach(type => {
          const statusData = data[type]
          console.log(`设备 ${type} 的原始数据:`, JSON.stringify(statusData))
          
          // 简化数据验证 - 直接检查是否为数组且长度为3
          if (Array.isArray(statusData) && statusData.length >= 3) {
            // 确保转换为数字，避免可能的字符串转换问题
            const healthy = Number(statusData[0]) || 0
            const warning = Number(statusData[1]) || 0
            const offline = Number(statusData[2]) || 0
            
            healthyData.push(healthy)
            warningData.push(warning)
            offlineData.push(offline)
            
            // 计算该设备的总数
            const total = healthy + warning + offline
            totals.push(total)
            
            console.log(`设备 ${type} 状态数据处理后: 正常=${healthy}, 告警=${warning}, 离线=${offline}, 总数=${total}`)
          } else {
            console.warn(`设备 ${type} 的数据格式不正确:`, statusData, 
                        '预期格式: [正常数量, 告警数量, 离线数量]')
            // 添加0值作为占位符
            healthyData.push(0)
            warningData.push(0)
            offlineData.push(0)
            totals.push(0)
          }
        })
        
        // 打印最终使用的数据
        console.log('设备健康状态最终数据:', {
          deviceTypes,
          healthyData,
          warningData,
          offlineData,
          totals
        })
        
        // 找出所有数据中的最大值，用于设置雷达图的最大值
        const allValues = [...healthyData, ...warningData, ...offlineData]
        const maxValue = Math.max(...allValues, 10) // 确保最小为10
        
        // 构建雷达图的指示器 - 修改最大值设置
        const indicators = deviceTypes.map((type) => {
          // 使用全局最大值的1.5倍，确保所有数据都能显示
          const indicatorMax = Math.ceil(maxValue * 1.5)
          return {
            name: type,
            max: indicatorMax
          }
        })
        
        // 美化版本 - 使用高科技雷达图和状态卡片组合
        const option = {
          backgroundColor: 'transparent',
          title: {
            text: '设备健康状态',
            textStyle: {
              fontSize: 14,
              fontWeight: 'normal',
              color: 'rgba(135, 221, 255, 0.9)'
            },
            left: 'center',
            top: 0
          },
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(18, 35, 69, 0.95)',
            borderColor: 'rgba(32, 186, 238, 0.4)',
            borderWidth: 1,
            textStyle: {
              color: '#fff'
            },
            formatter: function (params) {
              const deviceIndex = deviceTypes.indexOf(params.name);
              if (deviceIndex === -1) return '';

              const total = totals[deviceIndex];
              const healthy = healthyData[deviceIndex];
              const warning = warningData[deviceIndex];
              const offline = offlineData[deviceIndex];

              const healthyPercent = total > 0 ? Math.round((healthy / total) * 100) : 0;
              const warningPercent = total > 0 ? Math.round((warning / total) * 100) : 0;
              const offlinePercent = total > 0 ? Math.round((offline / total) * 100) : 0;

              return `<div style="font-weight:bold;color:#87DDFF">${params.name}</div>
                      <div style="margin:5px 0;display:flex;justify-content:space-between">
                        <span style="padding-right:15px">总数量:</span><span style="color:#fff;font-weight:bold">${total}</span>
                      </div>
                      <div style="display:flex;justify-content:space-between">
                        <span style="color:#41E9C3;padding-right:15px">正常:</span><span style="color:#41E9C3;font-weight:bold">${healthy} (${healthyPercent}%)</span>
                      </div>
                      <div style="display:flex;justify-content:space-between">
                        <span style="color:#FFB545;padding-right:15px">告警:</span><span style="color:#FFB545;font-weight:bold">${warning} (${warningPercent}%)</span>
                      </div>
                      <div style="display:flex;justify-content:space-between">
                        <span style="color:#FF5C9B;padding-right:15px">离线:</span><span style="color:#FF5C9B;font-weight:bold">${offline} (${offlinePercent}%)</span>
                      </div>`;
            }
          },
          legend: {
            show: true,
            top: '5%', // 将图例放置在右上角
            right: '5%',
            itemWidth: 12,
            itemHeight: 8,
            icon: 'roundRect',
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 11
            },
            data: ['正常', '告警', '离线']
          },
          radar: {
            center: ['50%', '55%'],
            radius: '60%',
            splitNumber: 4,
            shape: 'polygon',
            axisName: {
              color: 'rgb(238, 197, 102)',
              formatter: (name, indicator) => {
                const index = deviceTypes.indexOf(name);
                if (index === -1) return name;
                return `{a|${name}}\n{b|${totals[index]}}`;
              },
              rich: {
                a: {
                  color: 'rgba(135, 221, 255, 0.9)',
                  fontSize: 11,
                  fontWeight: 'bold',
                  padding: [0, 0, 0, 0]
                },
                b: {
                  color: '#fff',
                  fontSize: 11,
                  padding: [3, 0, 0, 0],
                  align: 'center'
                }
              }
            },
            indicator: deviceTypes.map((type, index) => ({
              name: type,
              max: Math.ceil((totals[index] || 10) * 1.5) // 动态调整最大值
            })),
            splitArea: {
              show: true,
              areaStyle: {
                color: ['rgba(38, 72, 120, 0.1)', 'rgba(38, 72, 120, 0.2)', 'rgba(38, 72, 120, 0.3)', 'rgba(38, 72, 120, 0.4)'],
                shadowColor: 'rgba(0, 180, 220, 0.3)',
                shadowBlur: 10
              }
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(135, 221, 255, 0.3)'
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(135, 221, 255, 0.3)',
                width: 1,
                type: 'dashed'
              }
            }
          },
          series: [
            {
              name: '设备状态',
              type: 'radar',
              symbol: 'circle',
              symbolSize: 6,
              emphasis: {
                lineStyle: {
                  width: 3
                }
              },
              data: [
                {
                  value: healthyData,
                  name: '正常',
                  itemStyle: {
                    color: 'rgba(65, 233, 195, 1)'
                  },
                  lineStyle: {
                    color: 'rgba(65, 233, 195, 0.8)',
                    width: 2,
                    shadowColor: 'rgba(65, 233, 195, 0.8)',
                    shadowBlur: 10
                  },
                  areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: 'rgba(65, 233, 195, 0.6)' },
                      { offset: 1, color: 'rgba(65, 233, 195, 0.1)' }
                    ])
                  }
                },
                {
                  value: warningData,
                  name: '告警',
                  itemStyle: {
                    color: 'rgba(255, 181, 69, 1)'
                  },
                  lineStyle: {
                    color: 'rgba(255, 181, 69, 0.8)',
                    width: 2,
                    shadowColor: 'rgba(255, 181, 69, 0.8)',
                    shadowBlur: 10
                  },
                  areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: 'rgba(255, 181, 69, 0.6)' },
                      { offset: 1, color: 'rgba(255, 181, 69, 0.1)' }
                    ])
                  }
                },
                {
                  value: offlineData,
                  name: '离线',
                  itemStyle: {
                    color: 'rgba(255, 92, 155, 1)'
                  },
                  lineStyle: {
                    color: 'rgba(255, 92, 155, 0.8)',
                    width: 2,
                    shadowColor: 'rgba(255, 92, 155, 0.8)',
                    shadowBlur: 10
                  },
                  areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: 'rgba(255, 92, 155, 0.6)' },
                      { offset: 1, color: 'rgba(255, 92, 155, 0.1)' }
                    ])
                  }
                }
              ]
            }
          ]
        }
        
        // 设置图表选项
        chart.setOption(option, true) // 添加true参数强制重绘
        
        // 添加闪烁动画效果
        nextTick(() => {
          addPulseEffect()
        })
      } catch (error) {
        console.error('Error updating chart:', error, error.stack)
        // 发生错误时尝试使用随机数据
        useRandomData()
      }
    }
    
    function addPulseEffect() {
      // (保持不变)...
    }
    
    // 使用随机数据（无API数据时的备选方案）- 需要相应更新
    function useRandomData() {
      try {
        if (!chartContainer.value) {
          console.warn('Chart container is not available for random data')
          return
        }
        
        if (!chart) {
          chart = echarts.init(chartContainer.value)
        }
        
        const deviceTypes = ['摄像头', '激光雷达', '毫秒波雷达', '融合设备']
        
        // 生成随机数据
        const healthyData = []
        const warningData = []
        const offlineData = []
        const totals = []
        
        deviceTypes.forEach(() => {
          const total = generateRandomValue(50, 200)
          const healthyPercent = generateRandomValue(60, 90)
          const warningPercent = generateRandomValue(3, 15)
          const offlinePercent = 100 - healthyPercent - warningPercent
          
          const healthy = Math.round(total * healthyPercent / 100)
          const warning = Math.round(total * warningPercent / 100)
          const offline = Math.round(total * offlinePercent / 100)
          
          healthyData.push(healthy)
          warningData.push(warning)
          offlineData.push(offline)
          totals.push(total)
        })
        
        console.log('DeviceHealthChart - 使用随机数据:', {
          deviceTypes,
          healthyData,
          warningData,
          offlineData,
          totals
        })
        
        // 找出所有数据中的最大值
        const allValues = [...healthyData, ...warningData, ...offlineData]
        const maxValue = Math.max(...allValues, 10) // 确保最小为10
        
        // 构建雷达图的指示器 - 使用统一的最大值
        const indicators = deviceTypes.map((type) => {
          return {
            name: type,
            max: Math.ceil(maxValue * 1.5)
          }
        })
        
        // 雷达图配置
        const option = {
          backgroundColor: 'transparent',
          title: {
            text: '设备健康状态',
            textStyle: {
              fontSize: 14,
              fontWeight: 'normal',
              color: 'rgba(135, 221, 255, 0.9)'
            },
            left: 'center',
            top: 0
          },
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(18, 35, 69, 0.95)',
            borderColor: 'rgba(32, 186, 238, 0.4)',
            borderWidth: 1,
            textStyle: {
              color: '#fff'
            },
            formatter: function(params) {
              if (params.seriesType === 'radar') {
                const deviceIndex = deviceTypes.indexOf(params.name)
                if (deviceIndex === -1) return ''
                
                const total = totals[deviceIndex]
                const healthy = healthyData[deviceIndex]
                const warning = warningData[deviceIndex]
                const offline = offlineData[deviceIndex]
                
                // 计算百分比，避免除以0
                const healthyPercent = total > 0 ? Math.round(healthy/total*100) : 0
                const warningPercent = total > 0 ? Math.round(warning/total*100) : 0
                const offlinePercent = total > 0 ? Math.round(offline/total*100) : 0
                
                return `<div style="font-weight:bold;color:#87DDFF">${params.name}</div>
                       <div style="margin:5px 0;display:flex;justify-content:space-between">
                         <span style="padding-right:15px">总数量:</span><span style="color:#fff;font-weight:bold">${total}</span>
                       </div>
                       <div style="display:flex;justify-content:space-between">
                         <span style="color:#41E9C3;padding-right:15px">正常:</span><span style="color:#41E9C3;font-weight:bold">${healthy} (${healthyPercent}%)</span>
                       </div>
                       <div style="display:flex;justify-content:space-between">
                         <span style="color:#FFB545;padding-right:15px">告警:</span><span style="color:#FFB545;font-weight:bold">${warning} (${warningPercent}%)</span>
                       </div>
                       <div style="display:flex;justify-content:space-between">
                         <span style="color:#FF5C9B;padding-right:15px">离线:</span><span style="color:#FF5C9B;font-weight:bold">${offline} (${offlinePercent}%)</span>
                       </div>`
              }
              return ''
            }
          },
          legend: {
            show: true,
            top: '5%', // 将图例放置在右上角
            right: '5%',
            itemWidth: 12,
            itemHeight: 8,
            icon: 'roundRect',
            textStyle: {
              color: 'rgba(255, 255, 255, 0.7)',
              fontSize: 11
            },
            data: ['正常', '告警', '离线']
          },
          radar: {
            center: ['50%', '55%'],
            radius: '60%',
            splitNumber: 4,
            shape: 'polygon',
            axisName: {
              color: 'rgb(238, 197, 102)',
              formatter: (name, indicator) => {
                const index = deviceTypes.indexOf(name)
                return `{a|${name}}\n{b|${totals[index]}}`
              },
              rich: {
                a: {
                  color: 'rgba(135, 221, 255, 0.9)',
                  fontSize: 11,
                  fontWeight: 'bold',
                  padding: [0, 0, 0, 0]
                },
                b: {
                  color: '#fff',
                  fontSize: 11,
                  padding: [3, 0, 0, 0],
                  align: 'center'
                }
              }
            },
            indicator: indicators,
            splitArea: {
              show: true,
              areaStyle: {
                color: ['rgba(38, 72, 120, 0.1)', 'rgba(38, 72, 120, 0.2)', 'rgba(38, 72, 120, 0.3)', 'rgba(38, 72, 120, 0.4)'],
                shadowColor: 'rgba(0, 180, 220, 0.3)',
                shadowBlur: 10
              }
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(135, 221, 255, 0.3)'
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(135, 221, 255, 0.3)',
                width: 1,
                type: 'dashed'
              }
            },
            scale: false,
            axisLabel: {
              show: false
            },
            silent: true,
            triggerEvent: false
          },
          series: [
            {
              name: '设备状态',
              type: 'radar',
              symbol: 'circle',
              symbolSize: 6,
              emphasis: {
                lineStyle: {
                  width: 3
                }
              },
              data: [
                {
                  value: healthyData,
                  name: '正常',
                  itemStyle: {
                    color: 'rgba(65, 233, 195, 1)'
                  },
                  lineStyle: {
                    color: 'rgba(65, 233, 195, 0.8)',
                    width: 2,
                    shadowColor: 'rgba(65, 233, 195, 0.8)',
                    shadowBlur: 10
                  },
                  areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: 'rgba(65, 233, 195, 0.6)' },
                      { offset: 1, color: 'rgba(65, 233, 195, 0.1)' }
                    ])
                  }
                },
                {
                  value: warningData,
                  name: '告警',
                  itemStyle: {
                    color: 'rgba(255, 181, 69, 1)'
                  },
                  lineStyle: {
                    color: 'rgba(255, 181, 69, 0.8)',
                    width: 2,
                    shadowColor: 'rgba(255, 181, 69, 0.8)',
                    shadowBlur: 10
                  },
                  areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: 'rgba(255, 181, 69, 0.6)' },
                      { offset: 1, color: 'rgba(255, 181, 69, 0.1)' }
                    ])
                  }
                },
                {
                  value: offlineData,
                  name: '离线',
                  itemStyle: {
                    color: 'rgba(255, 92, 155, 1)'
                  },
                  lineStyle: {
                    color: 'rgba(255, 92, 155, 0.8)',
                    width: 2,
                    shadowColor: 'rgba(255, 92, 155, 0.8)',
                    shadowBlur: 10
                  },
                  areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: 'rgba(255, 92, 155, 0.6)' },
                      { offset: 1, color: 'rgba(255, 92, 155, 0.1)' }
                    ])
                  }
                }
              ]
            }
          ]
        }
        
        chart.setOption(option, true) // 添加true参数强制重绘
        
        // 添加闪烁动画效果
        nextTick(() => {
          addPulseEffect()
        })
      } catch (error) {
        console.error('Error using random data:', error)
      }
    }
    
    return {
      chartContainer,
      initChart,
      updateChart,
      isLoading
    }
  }
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, rgba(10, 35, 65, 0.7) 0%, rgba(16, 45, 85, 0.7) 100%);
  box-shadow: 0 0 20px rgba(0, 125, 255, 0.1) inset, 0 0 5px rgba(0, 181, 255, 0.2);
  border: 1px solid rgba(32, 186, 238, 0.4);
}

/* 脉冲动画容器 */
.pulse-effect-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

/* 脉冲动画效果 */
.pulse-effect {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(32, 186, 238, 0.15) 0%, rgba(32, 186, 238, 0) 70%);
  border-radius: 50%;
  opacity: 0;
  animation: pulse 4s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 0.5;
  }
  70% {
    opacity: 0;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.8);
    opacity: 0;
  }
}

/* 高科技风格的加载动画 */
.tech-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(8, 24, 44, 0.85);
  z-index: 10;
  backdrop-filter: blur(2px);
}

.tech-loading-circle {
  width: 40px;
  height: 40px;
  border: 2px solid transparent;
  border-top-color: rgba(65, 233, 195, 0.8);
  border-right-color: rgba(32, 186, 238, 0.8);
  border-bottom-color: rgba(255, 92, 155, 0.8);
  border-left-color: rgba(255, 181, 69, 0.8);
  border-radius: 50%;
  animation: tech-spin 1s linear infinite;
  box-shadow: 0 0 10px rgba(32, 186, 238, 0.5);
}

.tech-loading-text {
  margin-top: 12px;
  color: rgba(135, 221, 255, 0.9);
  font-size: 12px;
  text-shadow: 0 0 5px rgba(32, 186, 238, 0.5);
}

@keyframes tech-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 添加高亮边缘效果 */
.chart-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(32, 186, 238, 0) 0%, rgba(32, 186, 238, 0.8) 50%, rgba(32, 186, 238, 0) 100%);
  box-shadow: 0 0 8px rgba(32, 186, 238, 0.8);
  animation: scanline 4s linear infinite;
  z-index: 1;
}

@keyframes scanline {
  0% {
    top: 0%;
  }
  100% {
    top: 100%;
  }
}
</style>