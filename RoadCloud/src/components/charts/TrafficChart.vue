<template>
  <div class="traffic-chart" ref="chartContainer">
    <div v-if="!trafficData || trafficData.length === 0" class="no-data-message">暂无数据</div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch, toRefs } from 'vue'
import * as echarts from 'echarts'

export default {
  name: 'TrafficChart',
  props: {
    activeTab: {
      type: String,
      default: 'minute'
    },
    trafficData: {
      type: Array,
      default: () => []
    },
    startTime: {
      type: String,
      default: ''
    },
    endTime: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    const { activeTab, trafficData } = toRefs(props)
    const chartContainer = ref(null)
    let chart = null
    
    onMounted(() => {
      setTimeout(() => {
        initChart()
        window.addEventListener('resize', resizeChart)
      }, 0) // 使用nextTick确保DOM已渲染
    })
    
    onBeforeUnmount(() => {
      if (chart) {
        chart.dispose()
      }
      window.removeEventListener('resize', resizeChart)
    })
    
    // 监听属性变化，刷新图表
    watch([activeTab, trafficData], () => {
      console.log(`数据变化: ${activeTab.value}, ${trafficData.value?.length || 0}条记录`)
      
      // 确保图表存在
      if (!chart && chartContainer.value) {
        initChart()
      } else {
        refreshData()
      }
    }, { deep: true, immediate: true })
    
    // 初始化图表
    function initChart() {
      if (chartContainer.value) {
        try {
          if (chart) {
            chart.dispose()
          }
          chart = echarts.init(chartContainer.value)
          refreshData()
          
          // 如果没有数据，显示一个空的图表框架
          if (!trafficData.value || trafficData.value.length === 0) {
            setEmptyChart()
          }
        } catch (error) {
          console.error('初始化图表时出错:', error)
        }
      }
    }
    
    // 设置空图表（只有坐标轴，没有数据）
    function setEmptyChart() {
      if (!chart) return
      
      const option = {
        backgroundColor: 'transparent',
        grid: {
          left: '40px',
          right: '20px',
          top: '20px',
          bottom: '40px',
          containLabel: false
        },
        xAxis: {
          type: 'category',
          data: [],
          boundaryGap: false,
          axisLine: {
            lineStyle: {
              color: '#0e5986'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        yAxis: {
          type: 'value',
          name: '车流量',
          nameTextStyle: {
            color: '#fff',
            padding: [0, 30, 0, 0]
          },
          min: 0,
          max: 10,
          interval: 2,
          axisLine: {
            show: true,
            lineStyle: {
              color: '#0e5986'
            }
          },
          splitLine: {
            show: true,
            lineStyle: {
              color: 'rgba(14, 89, 134, 0.3)'
            }
          },
          axisLabel: {
            show: true,
            color: '#fff',
            formatter: '{value}',
            margin: 10
          }
        },
        series: [
          {
            data: [],
            type: 'line',
            smooth: true
          }
        ]
      }
      
      chart.setOption(option)
    }
    
    // 根据选项卡格式化时间
    function formatTimeByTab(date, tab) {
      const hours = date.getHours();
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      switch(tab) {
        case 'minute':
          return `${hours}:${minutes}:${seconds}`;
        case 'hour':
          return `${hours}:${minutes}`;
        case 'day':
          return `${hours}:00`;
        default:
          return `${hours}:${minutes}:${seconds}`;
      }
    }
    
    // 获取图表配置
    function getChartOption() {
      let xAxisData = [];
      let seriesData = [];
      
      if (trafficData.value && trafficData.value.length > 0) {
        // 按时间排序数据
        const sortedData = [...trafficData.value].sort((a, b) => {
          // 使用start_time字段进行排序，可能是ISO字符串或时间戳
          const timeA = typeof a.start_time === 'string' ? new Date(a.start_time).getTime() : a.start_time;
          const timeB = typeof b.start_time === 'string' ? new Date(b.start_time).getTime() : b.start_time;
          return timeA - timeB;
        });
        
        // 提取数据
        sortedData.forEach(item => {
          // 处理可能是时间戳或字符串的start_time
          const timestamp = typeof item.start_time === 'string' ? 
                           new Date(item.start_time).getTime() : 
                           (typeof item.start_time === 'number' ? 
                             item.start_time : 
                             (item.seg_start || 0));
          
          const date = new Date(timestamp);
          const formattedTime = formatTimeByTab(date, activeTab.value);
          
          xAxisData.push(formattedTime);
          seriesData.push(item.count || 0);
        });
        
        // 记录数据范围
        if (sortedData.length > 0) {
          const firstTime = new Date(typeof sortedData[0].start_time === 'string' ? 
                                   sortedData[0].start_time : 
                                   (sortedData[0].seg_start || 0));
          
          const lastTime = new Date(typeof sortedData[sortedData.length-1].start_time === 'string' ? 
                                  sortedData[sortedData.length-1].start_time : 
                                  (sortedData[sortedData.length-1].seg_start || 0));
          
          console.log(`数据范围: ${firstTime.toLocaleString()} 到 ${lastTime.toLocaleString()}`);
        }
      }
      
      // 计算Y轴最大值
      const maxValue = Math.max(...seriesData, 10);
      const yAxisMax = Math.ceil(maxValue * 1.2);
      
      // 计算Y轴间隔
      const yAxisInterval = Math.max(Math.ceil(yAxisMax / 5), 1);
      
      return {
        backgroundColor: 'transparent',
        grid: {
          left: '40px',
          right: '20px',
          top: '20%',
          bottom: '20%',
          containLabel: false
        },
        xAxis: {
          type: 'category',
          data: xAxisData.length > 0 ? xAxisData : ['暂无数据'],
          boundaryGap: false,
          axisLine: {
            lineStyle: {
              color: '#0e5986'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10,
            rotate: 45,
            interval: function(index) {
              // 根据不同标签页和数据长度动态显示标签
              const dataLength = xAxisData.length;
              
              if (dataLength <= 1) return true;
              
              // 始终显示第一个和最后一个
              if (index === 0 || index === dataLength - 1) {
                return true;
              }
              
              if (activeTab.value === 'minute') {
                // 分钟视图每6个点显示一个标签
                return index % 6 === 0;
              } else if (activeTab.value === 'hour') {
                // 小时视图每5个点显示一个标签
                return index % 5 === 0;
              } else if (activeTab.value === 'day') {
                // 天视图每4个点显示一个标签
                return index % 4 === 0;
              }
              
              return index % Math.max(Math.floor(dataLength / 10), 1) === 0;
            },
            margin: 8
          }
        },
        yAxis: {
          type: 'value',
          name: '车流量',
          nameTextStyle: {
            color: '#fff',
            padding: [0, 30, 0, 0]
          },
          min: 0,
          max: yAxisMax || 10,
          interval: yAxisInterval || 2,
          axisLine: {
            show: true,
            lineStyle: {
              color: '#0e5986'
            }
          },
          splitLine: {
            show: true,
            lineStyle: {
              color: 'rgba(14, 89, 134, 0.3)'
            }
          },
          axisLabel: {
            show: true,
            color: '#fff',
            formatter: '{value}',
            margin: 10
          }
        },
        series: [
          {
            data: seriesData.length > 0 ? seriesData : [],
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            showSymbol: false,
            lineStyle: {
              color: '#00ffff',
              width: 3
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgba(0, 255, 255, 0.5)'
                },
                {
                  offset: 1,
                  color: 'rgba(0, 255, 255, 0.1)'
                }
              ])
            }
          }
        ],
        tooltip: {
          show: true,
          trigger: 'axis',
          formatter: function(params) {
            if (!params[0].value && params[0].value !== 0) {
              return '暂无数据';
            }
            return params[0].name + '<br/>' + 
                  '车流量: ' + params[0].value;
          },
          axisPointer: {
            type: 'line',
            lineStyle: {
              color: 'rgba(0, 255, 255, 0.5)',
              width: 1
            }
          }
        },
        animationDuration: 500
      }
    }
    
    // 刷新图表数据
    function refreshData() {
      if (!chart) {
        // 如果图表不存在，等待DOM更新后再尝试初始化
        setTimeout(() => {
          initChart()
        }, 0)
        return
      }
      
      try {
        if (trafficData.value && trafficData.value.length > 0) {
          const option = getChartOption()
          chart.setOption(option, true)
        } else {
          // 如果没有数据，显示空图表
          setEmptyChart()
        }
      } catch (error) {
        console.error('刷新图表出错:', error)
      }
    }
    
    // 调整图表大小
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
.traffic-chart {
  height: 180px;
  width: 100%;
  display: block;
  position: relative;
  overflow: visible;
}

.no-data-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(0, 255, 255, 0.7);
  font-size: 14px;
}
</style>