<template>
  <div class="pattern-chart" ref="chartContainer"></div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import * as echarts from 'echarts';

export default {
  name: 'PatternChart',
  props: {
    activeTab: {
      type: String,
      default: 'minute'
    }
  },
  setup(props) {
    const chartContainer = ref(null);
    let chart = null;
    
    // 存储API返回的数据
    const trafficData = ref({
      carTypeAndCount: {},
      status: '',
      requestTime: null // 记录请求时间
    });
    
    // 获取交通数据
    const fetchTrafficData = async () => {
      try {
        // 记录当前请求的时间
        const currentTime = new Date();
        trafficData.value.requestTime = currentTime;
        
        // 结束时间是当前时间的时间戳（毫秒）
        const endTime = currentTime.getTime().toString();
        
        let startTime;
        if (props.activeTab === 'minute') {
          // 开始时间是1分钟前的时间戳（毫秒）
          startTime = (currentTime.getTime() - 60 * 1000).toString();
        } else if (props.activeTab === 'hour') {
          // 开始时间是1小时前的时间戳（毫秒）
          startTime = (currentTime.getTime() - 60 * 60 * 1000).toString();
        } else {
          // 天视图：开始时间是1天前的时间戳（毫秒）
          startTime = (currentTime.getTime() - 24 * 60 * 60 * 1000).toString();
        }
        
        // 发起API请求获取车辆类型数量数据
        const response = await fetch(`/menu/vehicle/carType/count?startTime=${startTime}&endTime=${endTime}`);
        const data = await response.json();
        
        // 保存API返回的数据和请求时间
        trafficData.value = {
          ...data,
          requestTime: currentTime
        };
        
        // 根据当前激活的标签页重新渲染图表
        if (chart) {
          renderPattern();
        }
      } catch (error) {
        console.error('获取交通数据失败:', error);
        trafficData.value = { 
          status: 'fail', 
          requestTime: new Date()
        };
        
        if (chart) {
          renderErrorChart('数据加载失败，请稍后再试');
        }
      }
    };
    
    const initChart = () => {
      if (!chartContainer.value) return;
      
      // 设置全局霓虹灯效果
      echarts.registerTheme('neon', {
        backgroundColor: 'rgba(13, 17, 23, 0.9)',
        textStyle: {
          color: 'rgba(255, 255, 255, 0.85)'
        }
      });
      
      chart = echarts.init(chartContainer.value, 'neon');
      
      // 获取数据并初始化图表
      fetchTrafficData();
    };
    
    const switchTab = (tab) => {
      if (!chart) return;
      
      // 切换标签时重新获取对应时间范围的数据
      fetchTrafficData();
    };
    
    // 刷新数据
    const refreshData = () => {
      fetchTrafficData();
    };
    
    // 渲染交通模式图表
    const renderPattern = () => {
      if (trafficData.value.status !== 'success') {
        renderErrorChart('数据加载失败，请稍后再试');
        return;
      }
      
      const { carTypeAndCount } = trafficData.value;
      const currentTime = trafficData.value.requestTime || new Date();
      
      // 获取数据点数量 - 以第一个车辆类型的数据长度为准
      const firstVehicleType = Object.keys(carTypeAndCount)[0];
      const dataPointCount = firstVehicleType ? carTypeAndCount[firstVehicleType].length : 0;
      
      // 根据当前的activeTab和实际数据点数量生成相应的时间标签
      const timeLabels = generateTimeLabels(currentTime, props.activeTab, dataPointCount);
      
      const option = {
        backgroundColor: 'rgba(13, 17, 23, 0.9)',
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'line',
            lineStyle: {
              color: 'rgba(0, 255, 255, 0.7)',
              width: 2,
              type: 'dashed'
            }
          },
          backgroundColor: 'rgba(0, 0, 20, 0.8)',
          borderColor: 'rgba(0, 255, 255, 0.4)',
          textStyle: {
            color: '#fff'
          },
          formatter: function(params) {
            let result = params[0].axisValue + '<br/>';
            params.forEach(param => {
              result += `${param.marker} ${param.seriesName}: ${param.value} 辆<br/>`;
            });
            return result;
          }
        },
        legend: {
          data: Object.keys(carTypeAndCount),
          textStyle: {
            color: 'rgba(255, 255, 255, 0.9)',
            fontSize: 10
          },
          right: 10,
          top: 0
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '22%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: timeLabels,
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.9)',
              fontSize: 10,
              // 根据数据点数量自动调整间隔
              interval: Math.floor(dataPointCount / 10) - 1,
              formatter: function(value) {
                return value;
              }
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(0, 255, 255, 0.4)',
                width: 2
              }
            },
            splitLine: {
              show: false
            }
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '车辆数量',
            nameTextStyle: {
              color: 'rgba(255, 255, 255, 0.9)',
              fontSize: 10,
              padding: [0, 0, 0, 0]
            },
            axisLabel: {
              color: 'rgba(255, 255, 255, 0.9)',
              fontSize: 10
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(0, 255, 255, 0.4)',
                width: 2
              }
            },
            splitLine: {
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.1)',
                type: 'dashed'
              }
            }
          }
        ],
        series: []
      };
      
      // 霓虹灯效果的颜色映射
      const colorMap = {
        '乘用车': '#ff00ff',  // 品红
        '自行车': '#00ffff',  // 青色
        '卡车': '#ffff00',    // 黄色
        '公交车': '#00ff00',  // 绿色
        '摩托车': '#ff8800'   // 橙色
      };
      
      // 动态生成系列数据
      Object.keys(carTypeAndCount).forEach((vehicleType, index) => {
        const color = colorMap[vehicleType] || `hsl(${index * 60}, 100%, 60%)`;
        
        // 获取当前车辆类型的数据
        const data = [...carTypeAndCount[vehicleType]];
        
        option.series.push({
          name: vehicleType,
          type: 'line',
          smooth: true,
          data: data,
          symbolSize: 6,
          symbol: 'circle',
          itemStyle: {
            color: color,
            shadowColor: color,
            shadowBlur: 5
          },
          lineStyle: {
            width: 2,
            shadowColor: color,
            shadowBlur: 5
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: `${color.replace(')', ', 0.5)')}`
                },
                {
                  offset: 1,
                  color: `${color.replace(')', ', 0.1)')}`
                }
              ]
            }
          },
          emphasis: {
            scale: true,
            focus: 'series',
            itemStyle: {
              borderWidth: 3,
              borderColor: '#fff',
              shadowColor: color,
              shadowBlur: 5
            },
            lineStyle: {
              width: 6,
              shadowColor: color,
              shadowBlur: 5
            }
          }
        });
      });
      
      chart.setOption(option, true);
    };
    
    // 根据当前激活的标签页和数据点数量生成时间标签
    const generateTimeLabels = (currentTime, activeTab, dataPointCount) => {
      if (!dataPointCount) return [];
      
      const timeLabels = [];
      
      if (activeTab === 'minute') {
        // 生成过去1分钟内的时间点
        const timeInterval = 60 / dataPointCount; // 每个点的秒数间隔
        
        for (let i = dataPointCount - 1; i >= 0; i--) {
          const time = new Date(currentTime);
          time.setMilliseconds(currentTime.getMilliseconds() - i * timeInterval * 1000);
          
          // 格式化为 MM:SS.ms
          const seconds = time.getSeconds();
          const milliseconds = Math.floor(time.getMilliseconds() / 100);
          timeLabels.push(`${seconds}.${milliseconds}`);
        }
      } else if (activeTab === 'hour') {
        // 生成过去1小时内的时间点
        const timeInterval = 3600 / dataPointCount; // 每个点的秒数间隔
        
        for (let i = dataPointCount - 1; i >= 0; i--) {
          const time = new Date(currentTime);
          time.setSeconds(currentTime.getSeconds() - i * timeInterval);
          
          // 格式化为 HH:MM:SS
          const hours = time.getHours().toString().padStart(2, '0');
          const minutes = time.getMinutes().toString().padStart(2, '0');
          const seconds = time.getSeconds().toString().padStart(2, '0');
          timeLabels.push(`${hours}:${minutes}:${seconds}`);
        }
      } else {
        // 生成过去1天内的时间点
        const timeInterval = 1440 / dataPointCount; // 每个点的分钟间隔
        
        for (let i = dataPointCount - 1; i >= 0; i--) {
          const time = new Date(currentTime);
          time.setMinutes(currentTime.getMinutes() - i * timeInterval);
          
          // 格式化为 HH:MM
          const hours = time.getHours().toString().padStart(2, '0');
          const minutes = time.getMinutes().toString().padStart(2, '0');
          timeLabels.push(`${hours}:${minutes}`);
        }
      }
      
      return timeLabels;
    };
    
    // 渲染错误信息
    const renderErrorChart = (message) => {
      if (!chart) return;
      
      const option = {
        backgroundColor: 'rgba(13, 17, 23, 0.9)',
        title: {
          text: message,
          textStyle: {
            color: 'rgba(255, 0, 0, 0.8)',
            fontSize: 14,
            textShadow: '0 0 10px rgba(255, 0, 0, 0.5)'
          },
          left: 'center',
          top: 'center'
        }
      };
      
      chart.setOption(option);
    };
    
    // 监听tab变化
    watch(() => props.activeTab, (newTab) => {
      switchTab(newTab);
    });
    
    onMounted(() => {
      initChart();
      window.addEventListener('resize', handleResize);
    });
    
    onBeforeUnmount(() => {
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
      switchTab,
      refreshData
    };
  }
}
</script>

<style scoped>
.pattern-chart {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(10, 14, 35, 0.95) 0%, rgba(15, 23, 42, 0.95) 100%);
  border-radius: 8px;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3), 0 0 30px rgba(0, 255, 255, 0.2);
  overflow: hidden;
}
</style>