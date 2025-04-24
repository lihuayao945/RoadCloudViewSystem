<template>
    <div class="city-selector">
      <div class="selector-container">
        <div class="current-city" @click="toggleDropdown">
          {{ currentCity.name }} <i class="arrow-down"></i>
        </div>
        <div class="dropdown" v-if="showDropdown">
          <div class="city-group" v-for="(cities, province) in cityGroups" :key="province">
            <div class="province-title">{{ province }}</div>
            <div class="cities-list">
              <div 
                class="city-item" 
                v-for="city in cities" 
                :key="city.name"
                :class="{ 'active': currentCity.name === city.name }"
                @click="selectCity(city)"
              >
                {{ city.name }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'CitySelector',
    props: {
      currentCity: {
        type: Object,
        default: () => ({
          name: '重庆市',
          center: [106.32554720015287, 29.51776878783989]
        })
      }
    },
    data() {
      return {
        showDropdown: false,
        cityGroups: {
          '直辖市': [
            { name: '北京市', center: [116.407526, 39.90403] },
            { name: '上海市', center: [121.473701, 31.230416] },
            { name: '天津市', center: [117.190182, 39.125596] },
            { name: '重庆市', center: [106.32554720015287, 29.51776878783989] }
          ],
          '华东地区': [
            { name: '杭州市', center: [120.209947, 30.245853] },
            { name: '南京市', center: [118.767413, 32.041544] },
            { name: '合肥市', center: [117.227239, 31.820587] },
            { name: '济南市', center: [117.120098, 36.651216] },
            { name: '青岛市', center: [120.382639, 36.067082] },
            { name: '福州市', center: [119.306239, 26.075302] }
          ],
          '华南地区': [
            { name: '广州市', center: [113.264434, 23.129162] },
            { name: '深圳市', center: [114.085947, 22.547] },
            { name: '南宁市', center: [108.320004, 22.82402] },
            { name: '海口市', center: [110.198293, 20.044001] }
          ],
          '华中地区': [
            { name: '武汉市', center: [114.305392, 30.593098] },
            { name: '长沙市', center: [112.982279, 28.19409] },
            { name: '郑州市', center: [113.665412, 34.757975] }
          ],
          '华北地区': [
            { name: '太原市', center: [112.549248, 37.857014] },
            { name: '石家庄市', center: [114.502461, 38.045474] },
            { name: '呼和浩特市', center: [111.670801, 40.818311] }
          ],
          '西北地区': [
            { name: '西安市', center: [108.948024, 34.263161] },
            { name: '兰州市', center: [103.823557, 36.058039] },
            { name: '西宁市', center: [101.778916, 36.623178] },
            { name: '乌鲁木齐市', center: [87.617733, 43.792818] }
          ],
          '西南地区': [
            { name: '成都市', center: [104.065735, 30.659462] },
            { name: '贵阳市', center: [106.713478, 26.578343] },
            { name: '昆明市', center: [102.833, 24.88] },
            { name: '拉萨市', center: [91.132212, 29.660361] }
          ],
          '东北地区': [
            { name: '沈阳市', center: [123.429096, 41.796767] },
            { name: '长春市', center: [125.3245, 43.886841] },
            { name: '哈尔滨市', center: [126.642464, 45.756967] }
          ]
        }
      }
    },
    methods: {
      toggleDropdown() {
        this.showDropdown = !this.showDropdown;
      },
      selectCity(city) {
        if (city.name !== this.currentCity.name) {
          this.$emit('city-change', city);
        }
        this.showDropdown = false;
      }
    },
    mounted() {
      // 点击组件外部关闭下拉菜单
      document.addEventListener('click', (event) => {
        if (!this.$el.contains(event.target)) {
          this.showDropdown = false;
        }
      });
    },
    beforeUnmount() {
      document.removeEventListener('click', this.closeDropdown);
    }
  }
  </script>
  
  <style scoped>
  .city-selector {
    display: inline-flex;
    align-items: center;
    z-index: 1100; /* 确保下拉框在其他元素之上 */
  }
  
  .selector-container {
    position: relative;
  }
  
  .current-city {
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 3px 8px;
    background-color: rgba(0, 145, 234, 0.2);
    border: 1px solid rgba(0, 145, 234, 0.6);
    border-radius: 4px;
    font-size: 14px;
    min-width: 80px;
    text-align: center;
    transition: all 0.3s;
  }
  
  .current-city:hover {
    background-color: rgba(0, 145, 234, 0.4);
    box-shadow: 0 0 8px rgba(0, 145, 234, 0.6);
  }
  
  .arrow-down {
    display: inline-block;
    width: 0;
    height: 0;
    margin-left: 6px;
    border-left: 4px solid transparent;
    border-right: 4px solid transparent;
    border-top: 4px solid white;
  }
  
  .dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    margin-top: 5px;
    background-color: rgba(0, 36, 61, 0.95);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
    border: 1px solid #0e5986;
    border-radius: 4px;
    max-height: 400px;
    overflow-y: auto;
    min-width: 300px;
    z-index: 1100;
    color: white;
  }
  
  .city-group {
    margin-bottom: 10px;
    padding: 0 10px;
  }
  
  .province-title {
    font-weight: bold;
    padding: 10px 5px 5px;
    border-bottom: 1px solid rgba(14, 89, 134, 0.6);
    color: #00ffff;
  }
  
  .cities-list {
    display: flex;
    flex-wrap: wrap;
    padding: 5px 0;
  }
  
  .city-item {
    padding: 5px 10px;
    margin: 3px;
    cursor: pointer;
    border-radius: 3px;
    transition: background-color 0.2s;
  }
  
  .city-item:hover {
    background-color: rgba(0, 145, 234, 0.4);
  }
  
  .city-item.active {
    background-color: rgba(0, 145, 234, 0.6);
    box-shadow: 0 0 8px rgba(0, 145, 234, 0.6);
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .current-city {
      font-size: 13px;
      padding: 2px 6px;
      min-width: 70px;
    }
    
    .dropdown {
      min-width: 250px;
    }
    
    .city-item {
      padding: 3px 8px;
      font-size: 13px;
    }
  }
  
  @media (max-width: 480px) {
    .current-city {
      font-size: 12px;
      padding: 2px 4px;
      min-width: 60px;
    }
    
    .dropdown {
      min-width: 200px;
      max-height: 300px;
    }
    
    .city-item {
      padding: 3px 6px;
      font-size: 12px;
    }
  }
  </style>