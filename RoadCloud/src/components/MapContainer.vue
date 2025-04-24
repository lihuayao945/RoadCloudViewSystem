<template>
  <div class="map-container" ref="mapContainer">
    <!-- 添加地图控制和状态指示器 -->
    <div class="map-status-indicator" v-if="isLocating">
      <div class="status-spinner"></div>
      <span>定位中...</span>
    </div>
    
    <!-- 移除全局定位控制按钮，仅通过事件控制 -->
    
    <!-- 添加拓扑映射控制 -->
    <div class="topology-mapping-controls" v-if="topologyMappingActive">
      <div class="topology-info">已显示 {{ topologyMarkers.length }} 个设备</div>
      <button class="clear-topology-btn" @click="clearTopologyMapping">
        清除设备映射
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MapContainer',
  props: {
    currentMapType: {
      type: String,
      default: '标准'
    },
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
      map: null,
      satelliteLayer: null,
      trafficLayer: null,  // 初始化 trafficLayer
      currentLayers: [],    // 用于跟踪当前激活的图层
      markers: [], // 地图标注集合
      infoWindow: null, // 信息窗口
      locatedItemMarker: null, // 用于标记被定位的车辆或设备
      locatedItemId: null, // 当前定位的项目ID
      isLocating: false, // 是否正在进行定位操作
      defaultZoom: 11, // 默认缩放级别
      previousZoom: 11, // 用于存储定位前的缩放级别
      previousCenter: null, // 用于存储定位前的中心点
      showingTrajectory: false, // 是否显示轨迹
      trajectoryLine: null, // 轨迹线对象
      trajectoryPoints: [], // 轨迹点集合
      trajectoryMarkers: [], // 轨迹起终点标记
      layersPreloaded: false, // 预加载标记
      mapInitialized: false, // 地图初始化标记
      currentMapStyle: 'amap://styles/fresh', // 当前地图样式
      activeMapType: '标准', // 当前激活的地图类型
      currentInternalMapType: 'standard', // 内部使用的地图类型
      lastTypeChangeTime: 0, // 记录最后一次切换时间
      
      // 新增拓扑映射相关数据
      topologyMappingActive: false, // 拓扑映射是否激活
      topologyMarkers: [], // 拓扑设备标记集合
      topologyLines: [], // 拓扑连线集合
      topologyDevices: [], // 存储拓扑设备数据
      districtClusterMarkers: {}, // 区域聚合标记
      connectingLinesVisible: true, // 连线是否可见,
      
      // 设备数据相关
      devices: [], // 存储设备数据
      isLoadingDevices: false, // 是否正在加载设备数据
      fitDevicesViewPadding: [100, 100, 100, 100], // 调整视图边距
      
      // 新增: 阻止初始自动聚焦
      initialLoadComplete: false, // 用于标记初始加载完成
      disableAutoFocus: true, // 禁用自动聚焦
      
      // 新增: 存储地图控件引用
      mapControls: {
        toolBar: null,
        scale: null
      }
    }
  },
  watch: {
    // 监听 currentMapType 属性的变化
    currentMapType: {
      handler(newType) {
        console.log('MapContainer: currentMapType changed to', newType);
        if (this.mapInitialized) {
          this.activeMapType = newType; // 确保同步 activeMapType
          this.updateMapType(newType);
        }
      },
      immediate: true // 确保首次加载时也会触发
    },
    // 监听当前城市变化
    currentCity: {
      handler(newCity) {
        console.log('MapContainer: currentCity changed to', newCity.name);
        if (this.mapInitialized) {
          this.changeMapCenter(newCity.center);
        }
      },
      deep: true
    }
  },
  mounted() {
    this.initAMap();
    console.log('MapContainer mounted, currentMapType:', this.currentMapType);
    console.log('MapContainer mounted, currentCity:', this.currentCity.name);
    
    // 监听定位车辆事件
    window.addEventListener('locate-vehicle', this.handleLocateVehicle);
    window.addEventListener('clear-vehicle-located', this.clearVehicleLocated); // 新增专用事件
    // 监听定位设备事件
    window.addEventListener('locate-device', this.handleLocateDevice);
    window.addEventListener('clear-device-located', this.clearDeviceLocated); // 新增专用事件
    // 监听清除定位事件
    window.addEventListener('clear-located-device', this.clearLocatedItem);
    // 监听显示轨迹事件
    window.addEventListener('show-trajectory', this.handleShowTrajectory);
    // 监听清除轨迹事件
    window.addEventListener('clear-trajectory', this.clearTrajectory);
    
    // 新增: 监听拓扑映射事件
    window.addEventListener('show-topology-on-map', this.handleShowTopologyOnMap);
    
    // 新增: 监听在地图上显示设备事件
    window.addEventListener('show-device-on-map', this.handleShowDeviceOnMap);
    
    // 新增: 监听定位地图到设备事件
    window.addEventListener('center-map-on-device', this.handleCenterMapOnDevice);
  },
  beforeUnmount() {
    // 移除事件监听
    window.removeEventListener('locate-vehicle', this.handleLocateVehicle);
    window.removeEventListener('locate-device', (event) => {
      this.locateItemOnMap(event.detail, 'device');
    });
    window.removeEventListener('clear-located-device', this.clearLocatedItem);
    window.removeEventListener('show-trajectory', this.handleShowTrajectory);
    window.removeEventListener('clear-trajectory', this.clearTrajectory);
    window.removeEventListener('show-topology-on-map', this.handleShowTopologyOnMap);
    window.removeEventListener('show-device-on-map', this.handleShowDeviceOnMap);
    window.removeEventListener('center-map-on-device', this.handleCenterMapOnDevice);
    
    // 销毁地图实例
    if (this.map) {
      this.map.destroy();
    }
  },
  methods: {
    initAMap() {
      // 确保AMap已加载
      if (window.AMap) {
        console.log('AMap loaded, initializing map...');
        
        // 创建地图实例，优化加载速度
        this.map = new AMap.Map(this.$refs.mapContainer, {
          zoom: this.defaultZoom,
          center: this.currentCity.center,
          viewMode: '2D',
          mapStyle: this.currentMapStyle,
          resizeEnable: true,
          fadeOnZoom: false, // 禁用缩放时的渐变效果以提高性能
          animateEnable: false, // 初始化时禁用动画以提高加载速度
          preloadMode: true, // 预加载模式
          zooms: [3, 20], // 设置地图缩放级别范围
          defaultCursor: 'default' // 设置默认鼠标样式
        });

        // 优化地图加载
        this.map.on('complete', () => {
          console.log('Map loading complete');
          // 地图加载完成后再启用动画
          this.map.setStatus({animateEnable: true});
          
          // 添加控件并保存引用
          this.addMapControls();
          
          // 预加载图层
          this.preloadLayers();
          
          // 初始化地图类型
          this.mapInitialized = true;
          this.activeMapType = this.currentMapType; // 设置当前激活的地图类型
          this.updateMapType(this.currentMapType);
          console.log('Map initialized with type:', this.currentMapType);
          
          // 保存初始缩放级别
          this.previousZoom = this.map.getZoom();
          this.previousCenter = this.map.getCenter();

          // 创建信息窗口，但不显示
          this.infoWindow = new AMap.InfoWindow({
            offset: new AMap.Pixel(0, -30),
            closeWhenClickMap: true // 点击地图其他位置时关闭
          });
          
          // 添加地图缩放结束事件
          this.map.on('zoomend', this.handleZoomEnd);
          
          // 标记初始加载完成
          this.initialLoadComplete = true;
          
          // 在地图初始化完成后加载所有设备，但禁用自动聚焦
          this.loadAllDevices(true); // 传入true表示禁用自动聚焦
        });
      } else {
        console.error('AMap is not loaded');
      }
    },

    // 处理旧的事件（为了兼容性）
    handleLegacyEvent(event) {
      console.log('Legacy event received, use specific events instead', event);
      // 可以根据事件的detail决定调用哪个清除方法
      if (event.detail && event.detail.source === 'vehicle-search') {
        this.clearVehicleLocated(event);
      } else {
        this.clearDeviceLocated(event);
      }
    },
    
    // 新增: 添加地图控件并保存引用以便后续恢复
    addMapControls() {
      this.map.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
        // 添加工具条控件
        this.mapControls.toolBar = new AMap.ToolBar({
          position: 'RB', // 右下角
          offset: new AMap.Pixel(10, 10)
        });
        this.map.addControl(this.mapControls.toolBar);
        
        // 添加比例尺控件
        this.mapControls.scale = new AMap.Scale({
          position: 'LB', // 左下角
          offset: new AMap.Pixel(10, 10)
        });
        this.map.addControl(this.mapControls.scale);
        
        console.log('Map controls added successfully');
      });
    },
    
    // 新增: 确保控件可见
    ensureControlsVisible() {
      // 检查当前地图上是否有控件，如果没有则重新添加
      const hasToolBar = this.map.getAllOverlays('control').some(
        control => control instanceof AMap.ToolBar
      );
      
      const hasScale = this.map.getAllOverlays('control').some(
        control => control instanceof AMap.Scale
      );
      
      if (!hasToolBar && this.mapControls.toolBar) {
        this.map.addControl(this.mapControls.toolBar);
        console.log('ToolBar control restored');
      }
      
      if (!hasScale && this.mapControls.scale) {
        this.map.addControl(this.mapControls.scale);
        console.log('Scale control restored');
      }
      
      if (!hasToolBar || !hasScale) {
        console.log('Some controls were restored');
      }
    },
    
    // 加载所有设备，添加参数控制是否禁用自动聚焦
    loadAllDevices(disableAutoFocus = false) {
      this.isLoadingDevices = true;
      
      // 使用API接口获取设备数据
      // 设置一个较大的pageSize以获取所有设备
      fetch('/menu/device/list?pageNum=1&pageSize=1000')
        .then(response => response.json())
        .then(data => {
          if (data.status === 'success') {
            console.log('成功加载设备数据:', data.total, '个设备');
            this.devices = data.rows;
            
            // 在地图上显示所有设备，传入禁用自动聚焦参数
            this.displayDevicesOnMap(this.devices, false, disableAutoFocus);
          } else {
            console.error('加载设备数据失败');
            // 显示错误提示
            window.dispatchEvent(new CustomEvent('show-toast', {
              detail: { 
                message: '加载设备数据失败', 
                type: 'error', 
                duration: 3000 
              }
            }));
          }
        })
        .catch(error => {
          console.error('获取设备数据出错:', error);
          // 显示错误提示
          window.dispatchEvent(new CustomEvent('show-toast', {
            detail: { 
              message: '网络错误，无法获取设备数据', 
              type: 'error', 
              duration: 3000 
            }
          }));
        })
        .finally(() => {
          this.isLoadingDevices = false;
        });
    },
    
    // 在地图上显示所有设备，添加参数控制是否调整视图和禁用自动聚焦
    displayDevicesOnMap(devices, adjustView = false, disableAutoFocus = false) {
      // 清除现有标记（如果需要）
      if (this.markers.length > 0) {
        this.map.remove(this.markers);
        this.markers = [];
      }
      
      // 为每个设备创建标记
      devices.forEach(device => {
        // 检查设备是否有有效的坐标
        if (!device.longitude || !device.latitude) {
          console.warn('设备缺少坐标信息:', device.rcuId);
          return;
        }
        
        // 创建坐标数组
        const coordinates = [parseFloat(device.longitude), parseFloat(device.latitude)];
        
        // 根据设备类型选择不同的图标
        let icon;
        switch (device.deviceType) {
          case '1': // 激光雷达
            icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png';
            break;
          case '2': // 摄像头
            icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png';
            break;
          case '3': // 毫米波雷达
            icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_y.png';
            break;
          case '4': // 融合设备
            icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png';
            break;
          default:
            icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png';
        }
        
        // 创建设备标记
        const marker = new AMap.Marker({
          position: coordinates,
          title: device.rcuId,
          icon: icon,
          offset: new AMap.Pixel(-12, -12),
          zIndex: 90,
          extData: {
            id: device.rcuId,
            deviceType: device.deviceType,
            receiveTime: device.receiveTime,
            coordinates: coordinates
          }
        });
        
        // 添加点击事件
        marker.on('click', () => {
          this.showDeviceInfo(device, marker);
        });
        
        // 添加鼠标悬停效果
        marker.on('mouseover', () => {
          marker.setAnimation('AMAP_ANIMATION_BOUNCE');
        });
        
        marker.on('mouseout', () => {
          marker.setAnimation(null);
        });
        
        // 添加到标记集合
        this.markers.push(marker);
      });
      
      // 将所有标记添加到地图
      if (this.markers.length > 0) {
        this.map.add(this.markers);
        console.log('在地图上显示了', this.markers.length, '个设备');
        
        // 只有当明确要求调整视图且未禁用自动聚焦时才执行
        if (adjustView && !disableAutoFocus) {
          this.fitAllMarkersView();
        } else {
          console.log('保持当前地图视图 - 视图调整已禁用');
        }
      } else {
        console.warn('没有可显示的设备');
      }
    },
    
    // 完全禁用视图调整的方法
    fitAllMarkersView() {
      console.log('保持当前地图视图 - 视图调整已禁用');
      // 不执行任何视图调整操作
    },
    
    // 显示设备信息
    showDeviceInfo(device, marker) {
      // 将时间戳转换为可读时间
      let formattedTime = '未知';
      if (device.receiveTime) {
        const timestamp = parseInt(device.receiveTime);
        if (!isNaN(timestamp)) {
          const date = new Date(timestamp);
          formattedTime = date.toLocaleString();
        }
      }
      
      // 获取设备类型文本
      let deviceTypeText = '未知设备';
      switch (device.deviceType) {
        case '1':
          deviceTypeText = '激光雷达';
          break;
        case '2':
          deviceTypeText = '摄像头';
          break;
        case '3':
          deviceTypeText = '毫米波雷达';
          break;
        case '4':
          deviceTypeText = '融合设备';
          break;
      }
      
      const content = `
        <div style="width: 220px;">
          <h4 style="margin-bottom: 8px; color: #1a73e8;">${device.rcuId}</h4>
          <p style="margin: 4px 0;">类型: <span style="color: #0091ea; font-weight: 500;">${deviceTypeText}</span></p>
          <p style="margin: 4px 0;">更新时间: ${formattedTime}</p>
          <p style="margin: 4px 0;">坐标: [${parseFloat(device.longitude).toFixed(4)}, ${parseFloat(device.latitude).toFixed(4)}]</p>
          <div style="display: flex; justify-content: space-between; margin-top: 12px;">
            <button id="locate-button" style="padding: 6px 12px; background-color: #1a73e8; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; margin-right: 8px; font-weight: 500;">
              精确定位
            </button>
            <button id="detail-button" style="padding: 6px 12px; background-color: #34a853; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; font-weight: 500;">
              详细信息
            </button>
          </div>
        </div>
      `;
      
      this.infoWindow.setContent(content);
      this.infoWindow.open(this.map, marker.getPosition());
      
      // 监听按钮点击事件
      setTimeout(() => {
        const locateButton = document.getElementById('locate-button');
        const detailButton = document.getElementById('detail-button');
        
        if (locateButton) {
          locateButton.addEventListener('click', () => {
            console.log('欲儿触发精确定位');
            // 触发设备定位
            this.locateItemOnMap({
              id: device.rcuId,
              coordinates: [parseFloat(device.longitude), parseFloat(device.latitude)],
              title: device.rcuId,
              type: deviceTypeText,
              status: '在线',
              location: `经度: ${device.longitude}, 纬度: ${device.latitude}`
            }, 'device');
          });
        }
        
        if (detailButton) {
          detailButton.addEventListener('click', () => {
            // 使用window.location.href直接跳转，避免路由问题
            window.location.href = `/detail?rcuId=${device.rcuId}`;
          });
        }
      }, 10);
    },
    
    // 预加载图层，提高切换速度
    preloadLayers() {
      if (window.AMap && !this.layersPreloaded) {
        try {
          // 不预创建图层，而是在需要时再创建
          this.layersPreloaded = true;
          console.log('Map layer preload setting completed');
        } catch (e) {
          console.error('Error setting up map layer preload:', e);
        }
      }
    },
    
    // 处理地图缩放结束事件
    handleZoomEnd() {
      // 保存当前缩放级别（仅当不在定位状态时）
      if (!this.isLocating && !this.locatedItemMarker) {
        this.previousZoom = this.map.getZoom();
        this.previousCenter = this.map.getCenter();
        console.log('Saved map state: zoom =', this.previousZoom);
      }
      
      // 根据缩放级别自动调整拓扑设备的聚合状态
      if (this.topologyMappingActive) {
        this.updateTopologyByZoom();
      }
    },
    
    // 根据缩放级别更新拓扑设备显示
    updateTopologyByZoom() {
      const currentZoom = this.map.getZoom();
      
      // 高缩放级别显示单个设备，低缩放级别显示聚合
      if (currentZoom >= 13) {
        // 显示所有单独设备，隐藏聚合标记
        this.showAllIndividualDevices();
      } else {
        // 显示聚合标记，隐藏单独设备
        this.showClusteredDevices();
      }
      
      // 控制连线显示
      // 只在适当的缩放级别显示连线
      this.toggleConnectingLines(currentZoom >= 12);
    },
    
    // 显示所有单独的设备标记
    showAllIndividualDevices() {
      // 确保所有设备标记都可见
      this.topologyMarkers.forEach(marker => {
        if (!marker.getMap()) {
          this.map.add(marker);
        }
      });
      
      // 隐藏聚合标记
      Object.values(this.districtClusterMarkers).forEach(marker => {
        this.map.remove(marker);
      });
    },
    
    // 显示聚合的设备标记
    showClusteredDevices() {
      // 隐藏单独设备标记
      this.topologyMarkers.forEach(marker => {
        this.map.remove(marker);
      });
      
      // 确保聚合标记可见
      Object.values(this.districtClusterMarkers).forEach(marker => {
        if (!marker.getMap()) {
          this.map.add(marker);
        }
      });
    },
    
    // 切换连接线的显示状态
    toggleConnectingLines(visible) {
      if (this.connectingLinesVisible !== visible) {
        this.connectingLinesVisible = visible;
        
        if (visible) {
          // 显示连线
          this.topologyLines.forEach(line => {
            if (!line.getMap()) {
              this.map.add(line);
            }
          });
        } else {
          // 隐藏连线
          this.topologyLines.forEach(line => {
            this.map.remove(line);
          });
        }
      }
    },
    
    // 显示拓扑设备信息窗口
    showTopologyDeviceInfo(device, marker) {
      const content = `
        <div style="width: 220px;">
          <h4 style="margin-bottom: 8px; color: #1a73e8;">${device.id}</h4>
          <p style="margin: 4px 0;">类型: <span style="color: #0091ea; font-weight: 500;">${device.type}</span></p>
          <p style="margin: 4px 0;">区域: ${device.district}</p>
          <p style="margin: 4px 0;">地标: ${device.landmark}</p>
          <p style="margin: 4px 0;">坐标: [${device.coordinates[0].toFixed(4)}, ${device.coordinates[1].toFixed(4)}]</p>
          <div style="display: flex; justify-content: center; margin-top: 12px;">
            <button id="locate-button" style="padding: 6px 12px; background-color: #1a73e8; color: white; border: none; border-radius: 4px; cursor: pointer; width: 80%; font-weight: 500;">
              精确定位
            </button>
          </div>
        </div>
      `;
      
      this.infoWindow.setContent(content);
      this.infoWindow.open(this.map, marker.getPosition());
      
      // 监听按钮点击事件
      setTimeout(() => {
        const locateButton = document.getElementById('locate-button');
        
        if (locateButton) {
          locateButton.addEventListener('click', () => {
            // 触发设备定位
            this.locateItemOnMap({
              id: device.id,
              coordinates: device.coordinates,
              title: device.id,
              location: `${device.landmark}, ${device.district}`,
              status: '在线'
            }, 'device');
          });
        }
      }, 10);
    },
    
    // 显示车辆信息窗口
    showVehicleInfo(vehicle, marker) {
      const content = `
        <div style="width: 220px;">
          <h4 style="margin-bottom: 8px; color: #1a73e8;">${vehicle.plateNumber}</h4>
          <p style="margin: 4px 0;">类型: <span style="color: #0091ea; font-weight: 500;">${vehicle.type}</span></p>
          <p style="margin: 4px 0;">状态: ${vehicle.status}</p>
          <p style="margin: 4px 0;">速度: ${vehicle.speed} km/h</p>
          <div style="display: flex; justify-content: center; margin-top: 12px;">
            <button id="detail-button" style="padding: 6px 12px; background-color: #34a853; color: white; border: none; border-radius: 4px; cursor: pointer; width: 80%; font-weight: 500;">
              查看详细
            </button>
          </div>
        </div>
      `;
      
      this.infoWindow.setContent(content);
      this.infoWindow.open(this.map, marker.getPosition());
      
      // 监听按钮点击事件
      setTimeout(() => {
        const detailButton = document.getElementById('detail-button');
        
        if (detailButton) {
          detailButton.addEventListener('click', () => {
            // 使用window.location.href直接跳转，避免路由问题
           // window.location.href = `/detail?rcuId=${vehicle.id}`;
            window.location.reload = `/detail?rcuId=${vehicle.id}`;
          });
        }
      }, 10);
    },

    // 跳转到 /detail 页面
    goToDetailPage(id) {
      // 使用window.location.href直接跳转，避免路由问题
      // window.location.href = `/detail?rcuId=${id}`;
      window.location.reload = `/detail?rcuId=${vehicle.id}`;
    },

    // 切换地图中心点
    changeMapCenter(center) {
      if (this.map) {
        // 平滑移动到新的中心点
        this.map.setCenter(center, true);
        console.log('Map center changed to:', center);
      }
    },
    
    // 处理定位车辆事件
    handleLocateVehicle(event) {
      const vehicleData = event.detail;
      // 保存当前地图状态，准备还原
      this.saveMapState();
      this.locateItemOnMap(vehicleData, 'vehicle');
    },
    
    // 处理定位设备事件
    handleLocateDevice(event) {
      const deviceData = event.detail;
      // 保存当前地图状态，准备还原
      this.saveMapState();
      this.locateItemOnMap(deviceData, 'device');
    },
    
    // 保存地图状态以便后续还原
    saveMapState() {
      // 仅在第一次定位时保存状态
      if (!this.locatedItemMarker) {
        this.previousZoom = this.map.getZoom();
        this.previousCenter = this.map.getCenter();
        console.log('Saved map state: zoom =', this.previousZoom);
      }
    },
    
    // 还原地图状态
    restoreMapState() {
      if (this.previousZoom && this.previousCenter) {
        console.log('Restoring map to previous state: zoom =', this.previousZoom);
        this.map.setZoom(this.previousZoom, false); // 不使用动画
        this.map.setCenter(this.previousCenter, false);
      } else {
        // 如果没有保存的状态，还原到默认值
        console.log('Restoring map to default state');
        this.map.setZoom(this.defaultZoom, false);
        this.map.setCenter(this.currentCity.center, false);
      }
    },
    
    // 处理显示轨迹事件
    handleShowTrajectory(event) {
      const trajectoryData = event.detail;
      // 保存当前地图状态，准备还原
      this.saveMapState();
      this.showTrajectory(trajectoryData);
    },
    
    // 处理在地图上显示拓扑结构的事件
    handleShowTopologyOnMap(event) {
      const data = event.detail;
      
      if (data.active) {
        // 激活映射
        this.showTopologyOnMap(data.devices);
      } else {
        // 清除映射
        this.clearTopologyMapping();
      }
    },
    
    // 在地图上显示拓扑结构
    showTopologyOnMap(devices) {
      console.log('在地图上显示拓扑结构:', devices.length, '个设备');
      
      // 清除之前的映射
      this.clearTopologyMapping();
      
      // 存储设备数据
      this.topologyDevices = devices;
      this.topologyMappingActive = true;
      
      // 创建设备标记
      this.createDeviceMarkers();
      
      // 创建设备间连线
      this.createDeviceConnections();
      
      // 创建区域聚合标记
      this.createDistrictClusters();
      
      // 根据当前缩放级别决定显示模式
      this.updateTopologyByZoom();
      
      // 调整地图视图以显示所有设备
      this.adjustViewToShowAllDevices();
    },
    
    // 创建设备标记
    createDeviceMarkers() {
      // 星轨效果的标记样式
      const deviceIcon = {
        // 自定义设备图标
        size: new AMap.Size(24, 24),
        imageSize: new AMap.Size(24, 24),
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
      };
      
      this.topologyDevices.forEach((device, index) => {
        // 创建设备标记
        const marker = new AMap.Marker({
          position: device.coordinates,
          title: device.id,
          icon: new AMap.Icon(deviceIcon),
          offset: new AMap.Pixel(-12, -12),
          zIndex: 90,
          extData: {
            id: device.id,
            district: device.district,
            landmark: device.landmark,
            type: device.type
          }
        });
        
        // 添加点击事件
        marker.on('click', () => {
          this.showTopologyDeviceInfo(device, marker);
        });
        
        // 添加鼠标悬停效果
        marker.on('mouseover', () => {
          marker.setAnimation('AMAP_ANIMATION_BOUNCE');
          // 放大标记
          marker.setIcon(new AMap.Icon({
            ...deviceIcon,
            size: new AMap.Size(32, 32),
            imageSize: new AMap.Size(32, 32)
          }));
        });
        
        marker.on('mouseout', () => {
          marker.setAnimation(null);
          // 恢复原始大小
          marker.setIcon(new AMap.Icon(deviceIcon));
        });
        
        // 添加到标记集合
        this.topologyMarkers.push(marker);
      });
      
      // 将标记添加到地图
      this.map.add(this.topologyMarkers);
    },
    
    // 创建设备之间的连接线
    createDeviceConnections() {
      // 按地标和区域分组的设备
      const districtGroups = {};
      
      // 分组设备
      this.topologyDevices.forEach(device => {
        const district = device.district || '未知区域';
        const landmark = device.landmark || '未知地标';
        const key = `${district}-${landmark}`;
        
        if (!districtGroups[key]) {
          districtGroups[key] = [];
        }
        
        districtGroups[key].push(device);
      });
      
      // 为每个地标组创建连接线
      Object.keys(districtGroups).forEach(key => {
        const devices = districtGroups[key];
        
        if (devices.length > 1) {
          // 计算地标中心点
          const centerCoords = this.calculateCenterPoint(devices.map(d => d.coordinates));
          
          // 为每个设备创建到中心点的连线
          devices.forEach(device => {
            const line = new AMap.Polyline({
              path: [device.coordinates, centerCoords],
              strokeColor: 'rgba(0, 210, 255, 0.6)',
              strokeWeight: 2,
              strokeStyle: 'dashed',
              strokeDasharray: [5, 5],
              lineJoin: 'round',
              lineCap: 'round',
              zIndex: 50
            });
            
            // 添加到连线集合
            this.topologyLines.push(line);
          });
        }
      });
      
      // 将连线添加到地图
      this.map.add(this.topologyLines);
    },
    
    // 创建区域聚合标记
    createDistrictClusters() {
      // 按区域分组设备
      const districtGroups = {};
      
      // 分组设备
      this.topologyDevices.forEach(device => {
        const district = device.district || '未知区域';
        
        if (!districtGroups[district]) {
          districtGroups[district] = [];
        }
        
        districtGroups[district].push(device);
      });
      
      // 为每个区域创建聚合标记
      Object.keys(districtGroups).forEach(district => {
        const devices = districtGroups[district];
        
        // 计算区域中心点
        const centerCoords = this.calculateCenterPoint(devices.map(d => d.coordinates));
        
        // 创建聚合标记
        const clusterMarker = new AMap.Marker({
          position: centerCoords,
          content: `<div class="cluster-marker">${district}<span>${devices.length}</span></div>`,
          offset: new AMap.Pixel(-40, -40),
          zIndex: 100,
          extData: {
            district: district,
            count: devices.length
          }
        });
        
        // 添加点击事件 - 点击后缩放到合适级别查看区域设备
        clusterMarker.on('click', () => {
          // 找到属于这个区域的所有设备标记
          const districtMarkers = this.topologyMarkers.filter(
            marker => marker.getExtData().district === district
          );
          
          // 调整视图以显示该区域所有设备
          if (districtMarkers.length > 0) {
            this.map.setFitView(districtMarkers, false, [50, 50, 50, 50]);
            
            // 强制更新显示模式
            setTimeout(() => {
              this.updateTopologyByZoom();
            }, 100);
          }
          
          // 显示区域信息
          this.showDistrictInfo(district, devices, clusterMarker);
        });
        
        // 存储聚合标记
        this.districtClusterMarkers[district] = clusterMarker;
      });
    },
    
    // 显示区域信息
    showDistrictInfo(district, devices, marker) {
      const content = `
        <div style="width: 220px;">
          <h4 style="margin-bottom: 8px; color: #1a73e8;">${district}</h4>
          <p style="margin: 4px 0;">设备总数: <span style="color: #0091ea; font-weight: 500;">${devices.length}</span></p>
          <p style="margin: 4px 0; color: #5f6368;">点击查看区域内所有设备</p>
        </div>
      `;
      
      this.infoWindow.setContent(content);
      this.infoWindow.open(this.map, marker.getPosition());
    },
    
    // 调整视图以显示所有设备 - 改为可选择是否调整视图
    adjustViewToShowAllDevices() {
      if (this.topologyMarkers.length > 0) {
        // 获取所有标记的中心点
        const positions = this.topologyMarkers.map(marker => marker.getPosition());
        const bounds = new AMap.Bounds();
        
        // 计算包含所有标记的边界
        positions.forEach(position => {
          bounds.extend(position);
        });
        
        // 获取边界中心点
        const center = bounds.getCenter();
        
        // 仅移动到中心点，不改变缩放级别
        this.map.setCenter(center);
      }
    },
    
    // 计算中心点
    calculateCenterPoint(coordinates) {
      if (coordinates.length === 0) return [0, 0];
      
      let sumLng = 0;
      let sumLat = 0;
      
      coordinates.forEach(coord => {
        sumLng += coord[0];
        sumLat += coord[1];
      });
      
      return [sumLng / coordinates.length, sumLat / coordinates.length];
    },
    
    // 清除拓扑映射
    clearTopologyMapping() {
      // 重置拓扑映射状态
      this.topologyMappingActive = false;
      
      // 移除设备标记
      if (this.topologyMarkers.length > 0) {
        this.map.remove(this.topologyMarkers);
        this.topologyMarkers = [];
      }
      
      // 移除连接线
      if (this.topologyLines.length > 0) {
        this.map.remove(this.topologyLines);
        this.topologyLines = [];
      }
      
      // 移除聚合标记
      if (Object.keys(this.districtClusterMarkers).length > 0) {
        this.map.remove(Object.values(this.districtClusterMarkers));
        this.districtClusterMarkers = {};
      }
      
      // 清空设备数据
      this.topologyDevices = [];
      
      // 关闭信息窗口
      if (this.infoWindow && this.infoWindow.getIsOpen()) {
        this.infoWindow.close();
      }
      
      // 显示操作提示
      window.dispatchEvent(new CustomEvent('show-toast', {
        detail: { 
          message: '已清除地图上的设备拓扑映射', 
          type: 'info', 
          duration: 2000 
        }
      }));
    },
    
    // 显示轨迹方法 - 不缩放地图，直接在当前视图上展示轨迹
    showTrajectory(data) {
      // 清除已有轨迹但保持定位标记
      this.clearExistingTrajectory();
      
      // 设置显示轨迹状态
      this.showingTrajectory = true;
      
      // 生成轨迹点
      this.generateTrajectoryPoints(data);
      
      // 直接创建完整轨迹并添加到地图
      this.createTrajectoryLine();
      
      // 不调整地图视图，直接在当前视图上展示轨迹
      console.log('Trajectory displayed on current map view');
      
      // 如果有定位标记，需要更新信息窗口以显示轨迹清除按钮
      if (this.locatedItemMarker) {
        this.updateLocatedItemInfoWindow();
      }
    },
    
    // 更新定位项目的信息窗口，添加轨迹清除按钮
    updateLocatedItemInfoWindow() {
      // 获取当前定位的项目信息
      if (this.locatedItemMarker && this.locatedItemId) {
        // 查找设备或车辆数据
        const deviceData = this.devices.find(device => device.rcuId === this.locatedItemId) || {
          rcuId: this.locatedItemId,
          deviceType: '2' // 默认为摄像头
        };
        
        // 获取类型
        let type = 'device';
        
        // 重新打开信息窗口以显示轨迹清除按钮
        this.showLocatedItemInfo({
          id: this.locatedItemId,
          title: this.locatedItemId,
          type: deviceData.deviceType === '1' ? '激光雷达' : 
                deviceData.deviceType === '3' ? '毫米波雷达' : 
                deviceData.deviceType === '4' ? '融合设备' : '摄像头',
          status: '在线',
          coordinates: this.locatedItemMarker.getPosition(),
          location: `经度: ${this.locatedItemMarker.getPosition()[0].toFixed(6)}, 纬度: ${this.locatedItemMarker.getPosition()[1].toFixed(6)}`
        }, type);
      }
    },
    
    // 只清除现有轨迹，不清除定位标记和不还原地图状态
    clearExistingTrajectory() {
      if (this.trajectoryLine) {
        this.map.remove(this.trajectoryLine);
        this.trajectoryLine = null;
      }
      
      if (this.trajectoryMarkers && this.trajectoryMarkers.length > 0) {
        this.map.remove(this.trajectoryMarkers);
        this.trajectoryMarkers = [];
      }
      
      this.trajectoryPoints = [];
    },
    
    // 生成轨迹点
    generateTrajectoryPoints(data) {
      const { startPoint, endPoint, vehicleId } = data;
      
      // 清除现有轨迹点
      this.trajectoryPoints = [];
      
      // 如果有具体的轨迹点数据，则使用
      if (data.points && data.points.length > 0) {
        this.trajectoryPoints = data.points;
      } else {
        // 否则生成模拟轨迹点
        // 为简化示例，这里生成5个点的路径
        const startLng = parseFloat(startPoint[0]);
        const startLat = parseFloat(startPoint[1]);
        const endLng = parseFloat(endPoint[0]);
        const endLat = parseFloat(endPoint[1]);
        
        // 添加起点
        this.trajectoryPoints.push([startLng, startLat]);
        
        // 生成中间点
        const pointCount = 5;
        for (let i = 1; i < pointCount - 1; i++) {
          const ratio = i / pointCount;
          
          // 添加一些随机偏移，模拟真实轨迹
          const offsetLng = (Math.random() - 0.5) * 0.01;
          const offsetLat = (Math.random() - 0.5) * 0.01;
          
          const lng = startLng + (endLng - startLng) * ratio + offsetLng;
          const lat = startLat + (endLat - startLat) * ratio + offsetLat;
          
          this.trajectoryPoints.push([lng, lat]);
        }
        
        // 添加终点
        this.trajectoryPoints.push([endLng, endLat]);
      }
    },
    
    // 创建轨迹线
    createTrajectoryLine() {
      if (this.trajectoryPoints.length < 2) return;
      
      // 确保移除先前的轨迹标记
      if (this.trajectoryMarkers.length > 0) {
        this.map.remove(this.trajectoryMarkers);
        this.trajectoryMarkers = [];
      }
      
      // 创建折线
      this.trajectoryLine = new AMap.Polyline({
        path: this.trajectoryPoints,
        isOutline: true,
        outlineColor: '#FFD54F',
        borderWeight: 2,
        strokeColor: '#0091EA',
        strokeWeight: 5,
        strokeStyle: 'solid',
        strokeOpacity: 0.8,
        lineJoin: 'round',
        lineCap: 'round',
        zIndex: 50
      });
      
      // 添加到地图
      this.map.add(this.trajectoryLine);
      
      // 添加起点标记
      const startMarker = new AMap.Marker({
        position: this.trajectoryPoints[0],
        icon: new AMap.Icon({
          size: new AMap.Size(25, 34),
          image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
          imageSize: new AMap.Size(25, 34)
        }),
        offset: new AMap.Pixel(-12, -34),
        zIndex: 101
      });
      
      // 添加终点标记
      const endMarker = new AMap.Marker({
        position: this.trajectoryPoints[this.trajectoryPoints.length - 1],
        icon: new AMap.Icon({
          size: new AMap.Size(25, 34),
          image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
          imageSize: new AMap.Size(25, 34)
        }),
        offset: new AMap.Pixel(-12, -34),
        zIndex: 101
      });
      
      this.map.add([startMarker, endMarker]);
      
      // 保存起终点标记，方便清除
      this.trajectoryMarkers = [startMarker, endMarker];
    },
    
    // 清除轨迹 - 修改为可选是否还原地图状态，但不清除定位标记
    clearTrajectory(restoreMapState = true) {
      this.showingTrajectory = false;
      this.clearExistingTrajectory();
      
      // 如果需要还原地图状态
      if (restoreMapState) {
        this.restoreMapState();
      }
      
      // 通知其他组件轨迹已清除
      window.dispatchEvent(new CustomEvent('trajectory-cleared'));
      
      // 如果有定位标记，更新信息窗口
      if (this.locatedItemMarker) {
        this.updateLocatedItemInfoWindow();
      }
    },
    
    // 在地图上定位项目（车辆或设备）- 修改版，不清除轨迹
    locateItemOnMap(item, type) {// 设置定位状态
      this.isLocating = true;
      
      // 保存当前项目类型 - 新增
      this.locatedItemType = type;
      
      // 如果定位的是相同的项目，则只聚焦不创建新标记
      if (this.locatedItemId === item.id && this.locatedItemType === type) {
        this.focusOnLocatedItem();
        this.isLocating = false;
        return;
      }
      
      // 保存目前是否正在显示轨迹
      const hasTrajectory = this.showingTrajectory;
      const currentTrajectoryLine = this.trajectoryLine;
      const currentTrajectoryMarkers = [...this.trajectoryMarkers];
      const currentTrajectoryPoints = [...this.trajectoryPoints];
      
      // 清除已有的定位标记，但不触发事件，也不清除轨迹
      this.clearLocatedItemOnly();
      
      // 保存当前定位项目ID
      this.locatedItemId = item.id;
      
      // 直接设置缩放级别和中心点（无动画）
      this.map.setZoom(15, false);
      this.map.setCenter(item.coordinates, false);
      
      // 立即完成定位过程
      this.createLocatedMarker(item, type);
      
      // 如果之前有轨迹，恢复轨迹显示
      if (hasTrajectory && currentTrajectoryLine) {
        this.showingTrajectory = true;
        this.trajectoryLine = currentTrajectoryLine;
        this.trajectoryMarkers = currentTrajectoryMarkers;
        this.trajectoryPoints = currentTrajectoryPoints;
        
        // 确保轨迹重新添加到地图上
        this.map.add(this.trajectoryLine);
        this.map.add(this.trajectoryMarkers);
        
        // 更新信息窗口以显示轨迹清除按钮
        this.updateLocatedItemInfoWindow();
      }
    },
    
    // 只清除定位标记，不清除轨迹和不还原地图状态
    clearLocatedItemOnly() {
      if (this.locatedItemMarker) {
        // 关闭信息窗口
        if (this.infoWindow && this.infoWindow.getIsOpen()) {
          this.infoWindow.close();
        }
        
        // 移除定位标记
        this.map.remove(this.locatedItemMarker);
        this.locatedItemMarker = null;
        
        // 移除定位效果
        if (this.locateEffects && this.locateEffects.length > 0) {
          this.map.remove(this.locateEffects);
          this.locateEffects = [];
        }
        
        // 重置定位ID
        this.locatedItemId = null;
      }
    },
    
    // 创建定位标记和效果
    createLocatedMarker(item, type) {
      // 选择合适的图标颜色
      let iconUrl;
      
      switch(type) {
        case 'vehicle':
          iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_rs.png';
          break;
        case 'device':
          // 根据设备类型选择不同颜色
          if (item.type === '激光雷达') {
            iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_rs.png';
          } else if (item.type === '毫米波雷达') {
            iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_ys.png';
          } else if (item.type === '融合设备') {
            iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_gs.png';
          } else {
            iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_bs.png';
          }
          break;
        default:
          iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_bs.png';
      }
      
      // 添加定位光圈效果
      this.addLocateEffect(item.coordinates);
      
      // 创建定位标记
      this.locatedItemMarker = new AMap.Marker({
        position: item.coordinates,
        map: this.map,
        title: item.title || item.id,
        icon: iconUrl,
        animation: 'AMAP_ANIMATION_DROP', // 使用下落动画
        offset: new AMap.Pixel(-13, -30),
        zIndex: 120 // 确保在其他标记之上
      });
      
      // 显示信息窗口
      this.showLocatedItemInfo(item, type);
      
      // 取消定位状态
      this.isLocating = false;
      
      // 通知其他组件项目已定位
      window.dispatchEvent(new CustomEvent('device-located', {
        detail: {
          id: item.id,
          type: type
        }
      }));
    },
    
    // 显示已定位项目的信息窗口 - 修改为可控制是否显示轨迹清除按钮
    showLocatedItemInfo(item, type) {
      // 获取状态样式
      const statusColor = this.getStatusColor(item.status || '在线');
      
      // 创建信息窗口内容，添加条件判断是否显示轨迹清除按钮
      const content = `
        <div style="width: 240px;">
          <div style="display: flex; align-items: center; margin-bottom: 10px;">
            <h4 style="margin: 0; flex: 1; color: #1a73e8;">${item.title || item.id}</h4>
            ${item.status ? `<span style="padding: 2px 8px; border-radius: 12px; font-size: 12px; background-color: ${statusColor}; color: white;">${item.status}</span>` : ''}
          </div>
          ${item.type ? `<p style="margin: 4px 0;">类型: <span style="color: #0091ea; font-weight: 500;">${item.type}</span></p>` : ''}
          ${item.location ? `<p style="margin: 4px 0;">位置: ${item.location}</p>` : ''}
          <p style="margin: 8px 0; color: #34a853; font-weight: 500;">✓ 已成功定位到地图中心</p>
          <div style="display: flex; justify-content: space-between; margin-top: 12px;">
            <button id="exit-locate-button" style="padding: 6px 12px; background-color: #ea4335; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; margin-right: 8px; font-weight: 500;">
              退出定位
            </button>
            ${this.showingTrajectory ? `
            <button id="clear-trajectory-button" style="padding: 6px 12px; background-color: #fbbc05; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; font-weight: 500;">
              清除轨迹
            </button>
            ` : ''}
            ${type === 'device' && !this.showingTrajectory ? `
            <button id="detail-button" style="padding: 6px 12px; background-color: #34a853; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; font-weight: 500;">
              查看详细
            </button>
            ` : ''}
          </div>
        </div>
      `;
      
      // 设置信息窗口内容并打开
      this.infoWindow.setContent(content);
      this.infoWindow.open(this.map, this.locatedItemMarker.getPosition());
      
      // 监听按钮点击事件
      setTimeout(() => {
        const exitLocateButton = document.getElementById('exit-locate-button');
        const clearTrajectoryButton = document.getElementById('clear-trajectory-button');
        const detailButton = document.getElementById('detail-button');
        
        if (exitLocateButton) {
          exitLocateButton.addEventListener('click', () => {
            this.clearLocatedItem(true, true); // 清除定位和轨迹，并还原地图状态
          });
        }
        
        if (clearTrajectoryButton) {
          clearTrajectoryButton.addEventListener('click', () => {
            this.clearExistingTrajectory(); // 只清除轨迹，不清除定位
            this.showingTrajectory = false;
            
            // 刷新信息窗口内容，移除轨迹清除按钮
            this.showLocatedItemInfo(item, type);
            
            // 显示提示信息
            window.dispatchEvent(new CustomEvent('show-toast', {
              detail: { 
                message: '已清除轨迹', 
                type: 'info', 
                duration: 2000 
              }
            }));
          });
        }
        
        if (detailButton) {
          detailButton.addEventListener('click', () => {
            // 使用window.location.href直接跳转到详情页
            window.location.href = `/detail?rcuId=${item.id}`;
          });
        }
      }, 10);
    },
    
    // 获取状态对应的颜色
    getStatusColor(status) {
      switch(status) {
        case '在线': return '#34a853'; // 绿色
        case '维护中': return '#fbbc05'; // 黄色
        case '故障': 
        case '告警': return '#ea4335'; // 红色
        case '离线': return '#9aa0a6'; // 灰色
        default: return '#1a73e8'; // 蓝色
      }
    },
    
    // 添加定位效果 - 优化版
    addLocateEffect(position) {
      // 清除之前的效果
      if (this.locateEffects && this.locateEffects.length > 0) {
        this.map.remove(this.locateEffects);
        this.locateEffects = [];
      }
      
      // 创建一个大圆形作为底层
      const outerCircle = new AMap.Circle({
        center: position,
        radius: 80,
        fillColor: 'rgba(26, 115, 232, 0.1)',
        strokeColor: 'rgba(26, 115, 232, 0.3)',
        strokeWeight: 1,
        zIndex: 90,
        map: this.map
      });
      
      // 创建一个中等圆形
      const middleCircle = new AMap.Circle({
        center: position,
        radius: 50,
        fillColor: 'rgba(26, 115, 232, 0.15)',
        strokeColor: 'rgba(26, 115, 232, 0.5)',
        strokeWeight: 1,
        zIndex: 91,
        map: this.map
      });
      
      // 创建一个内部圆形
      const innerCircle = new AMap.Circle({
        center: position,
        radius: 20,
        fillColor: 'rgba(26, 115, 232, 0.25)',
        strokeColor: 'rgba(26, 115, 232, 0.8)',
        strokeWeight: 2,
        zIndex: 92,
        map: this.map
      });
      
      // 添加到效果集合
      this.locateEffects = [outerCircle, middleCircle, innerCircle];
      
      // 创建脉动动画效果
      this.animateLocateEffect();
    },
    
    // 定位效果的动画
    animateLocateEffect() {
      if (!this.locateEffects || this.locateEffects.length === 0) return;
      
      // 获取三个圆形
      const [outerCircle, middleCircle, innerCircle] = this.locateEffects;
      
      // 定义动画参数
      const duration = 2000; // 动画周期时长
      const maxOuterRadius = 100;
      const minOuterRadius = 80;
      const maxMiddleRadius = 65;
      const minMiddleRadius = 40;
      const maxInnerRadius = 30;
      const minInnerRadius = 15;
      
      // 启动动画
      let startTime = null;
      
      const animate = (timestamp) => {
        if (!startTime) startTime = timestamp;
        const elapsed = (timestamp - startTime) % duration;
        const progress = elapsed / duration;
        
        // 正弦函数实现平滑的脉动效果
        const wave = Math.sin(progress * Math.PI * 2);
        const normalizedWave = (wave + 1) / 2; // 将 -1~1 转换为 0~1
        
        // 计算当前半径
        const outerRadius = minOuterRadius + (maxOuterRadius - minOuterRadius) * normalizedWave;
        const middleRadius = minMiddleRadius + (maxMiddleRadius - minMiddleRadius) * normalizedWave;
        const innerRadius = minInnerRadius + (maxInnerRadius - minInnerRadius) * normalizedWave;
        
        // 设置圆形半径
        if (outerCircle && outerCircle.setRadius) outerCircle.setRadius(outerRadius);
        if (middleCircle && middleCircle.setRadius) middleCircle.setRadius(middleRadius);
        if (innerCircle && innerCircle.setRadius) innerCircle.setRadius(innerRadius);
        
        // 如果定位标记还存在，继续动画
        if (this.locatedItemMarker) {
          requestAnimationFrame(animate);
        }
      };
      
      // 启动动画
      requestAnimationFrame(animate);
    },
    
    // 聚焦到已定位的标记
    focusOnLocatedItem() {
      if (this.locatedItemMarker) {
        // 设置地图中心点
        this.map.setCenter(this.locatedItemMarker.getPosition(), false);
        
        // 如果信息窗口已关闭，则重新打开
        if (!this.infoWindow.getIsOpen()) {
          this.infoWindow.open(this.map, this.locatedItemMarker.getPosition());
        }
        
        // 刷新定位标记的动画
        this.locatedItemMarker.setAnimation('AMAP_ANIMATION_BOUNCE');
        setTimeout(() => {
          if (this.locatedItemMarker) {
            this.locatedItemMarker.setAnimation(null);
          }
        }, 2000);
      }
    },
    
    // 清除定位标记和效果，并可选择性地清除轨迹和还原地图状态
    clearLocatedItem(triggerEvent = true, clearTrajectoryAlso = true) {
      if (this.locatedItemMarker) {
        // 关闭信息窗口
        if (this.infoWindow && this.infoWindow.getIsOpen()) {
          this.infoWindow.close();
        }
        
        // 移除定位标记
        this.map.remove(this.locatedItemMarker);
        this.locatedItemMarker = null;
        
        // 移除定位效果
        if (this.locateEffects && this.locateEffects.length > 0) {
          this.map.remove(this.locateEffects);
          this.locateEffects = [];
        }
        
        // 重置定位ID
        this.locatedItemId = null;
        
        // 还原地图状态
        this.restoreMapState();
        
        // 触发事件通知其他组件
        if (triggerEvent) {
          window.dispatchEvent(new CustomEvent('device-location-cleared'));
          
          // 显示提示信息
          window.dispatchEvent(new CustomEvent('show-toast', {
            detail: { 
              message: '已退出定位模式', 
              type: 'info', 
              duration: 2000 
            }
          }));
        }
      }
      
      // 同时清除轨迹
      if (clearTrajectoryAlso && this.showingTrajectory) {
        this.clearTrajectory(false); // 不重复还原地图状态
      }
    },

    // 车辆特定的清除方法
    clearVehicleLocated(event) {
      // 保存当前类型
      const currentType = 'vehicle';
      
      if (this.locatedItemMarker && this.locatedItemType === currentType) {
        // 关闭信息窗口
        if (this.infoWindow && this.infoWindow.getIsOpen()) {
          this.infoWindow.close();
        }
        
        // 移除定位标记
        this.map.remove(this.locatedItemMarker);
        this.locatedItemMarker = null;
        
        // 移除定位效果
        if (this.locateEffects && this.locateEffects.length > 0) {
          this.map.remove(this.locateEffects);
          this.locateEffects = [];
        }
        
        // 重置定位ID
        this.locatedItemId = null;
        
        // 还原地图状态
        this.restoreMapState();
        
        // 触发事件通知其他组件
        if (triggerEvent) {
          window.dispatchEvent(new CustomEvent('device-location-cleared'));
          
          // 显示提示信息
          window.dispatchEvent(new CustomEvent('show-toast', {
            detail: { 
              message: '已退出定位模式', 
              type: 'info', 
              duration: 2000 
            }
          }));
        }
        window.dispatchEvent(new CustomEvent('vehicle-location-cleared'));
      }
    },

    // 设备特定的清除方法
    clearDeviceLocated(event) {
      // 保存当前类型
      const currentType = 'device';
      
      if (this.locatedItemMarker && this.locatedItemType === currentType) {
        /// 关闭信息窗口
        if (this.infoWindow && this.infoWindow.getIsOpen()) {
          this.infoWindow.close();
        }
        
        // 移除定位标记
        this.map.remove(this.locatedItemMarker);
        this.locatedItemMarker = null;
        
        // 移除定位效果
        if (this.locateEffects && this.locateEffects.length > 0) {
          this.map.remove(this.locateEffects);
          this.locateEffects = [];
        }
        
        // 重置定位ID
        this.locatedItemId = null;
        
        // 还原地图状态
        this.restoreMapState();
        
        // 触发事件通知其他组件
        if (triggerEvent) {
          window.dispatchEvent(new CustomEvent('device-location-cleared'));
          
          // 显示提示信息
          window.dispatchEvent(new CustomEvent('show-toast', {
            detail: { 
              message: '已退出定位模式', 
              type: 'info', 
              duration: 2000 
            }
          }));
        }
        window.dispatchEvent(new CustomEvent('device-location-cleared'));
      }
    },

    // 重写的地图类型更新方法 - 关键修改，确保始终强制刷新
    updateMapType(displayType) {
      if (!this.map) {
        console.error('Map not initialized');
        return;
      }
      
      console.log('Updating map type to:', displayType, 'from current type:', this.activeMapType);
      
      // 添加节流控制，防止频繁切换
      const now = Date.now();
      if (now - this.lastTypeChangeTime < 300) {
        console.log('Ignoring rapid type change request');
        return;
      }
      this.lastTypeChangeTime = now;
      
      // 记录当前活动类型
      this.activeMapType = displayType;
      
      // 转换显示类型为内部类型
      let mapType;
      switch (displayType) {
        case '卫星':
          mapType = 'satellite';
          break;
        case '交通':
          mapType = 'traffic';
          break;
        default:
          mapType = 'standard';
      }
      
      // 记录新的内部地图类型
      this.currentInternalMapType = mapType;
      
      // 应用地图类型，始终强制刷新
      this.applyMapType(mapType, true);
      
      // 向父组件发送 map-type-changed 事件通知更新
      this.$emit('map-type-changed', displayType);
      
      console.log('Map type update completed to:', displayType);
    },
    
    // 完全重写的清除所有图层方法
    clearAllLayers() {
      // 记录当前地图上的所有覆盖物
      console.log('Clearing all map layers...');
      
      try {
        // 1. 先销毁已有图层
        if (this.satelliteLayer) {
          this.map.remove(this.satelliteLayer);
          this.satelliteLayer = null;
          console.log('Removed satellite layer');
        }
        
        if (this.trafficLayer) {
          this.map.remove(this.trafficLayer);
          this.trafficLayer = null;
          console.log('Removed traffic layer');
        }
        
        // 2. 移除当前图层列表中的所有图层
        if (this.currentLayers.length > 0) {
          this.map.remove(this.currentLayers);
          this.currentLayers = [];
          console.log('Removed all tracked layers');
        }
        
        // 3. 过滤出需要移除的图层 (排除基础图层和控件)
        const allLayers = this.map.getLayers();
        console.log('Current map layers:', allLayers.length);
        
        const layersToRemove = allLayers.filter(layer => {
          // 只移除卫星和交通图层，保留其他图层
          if (layer instanceof AMap.TileLayer.Satellite || 
              layer instanceof AMap.TileLayer.Traffic) {
            return true;
          }
          return false;
        });
        
        if (layersToRemove.length > 0) {
          console.log('Removing specific layers:', layersToRemove.length);
          this.map.remove(layersToRemove);
        }
      } catch (e) {
        console.error('Error removing map layers:', e);
      }
      
      console.log('All layers cleared');
    },
    
    // 重写的应用地图类型方法，确保强制刷新
    applyMapType(type, forceRefresh = true) {
      console.log('Applying map type:', type, 'Force refresh:', forceRefresh);

      // 首先清除所有现有图层，保留控件
      this.clearAllLayers();
      
      // 重置当前图层列表
      this.currentLayers = [];
      
      // 根据类型添加对应图层
      switch (type) {
        case 'satellite':
          try {
            // 重新创建卫星图层
            this.satelliteLayer = new AMap.TileLayer.Satellite({
              zIndex: 5
            });
            
            // 显式添加图层到地图
            this.map.add(this.satelliteLayer);
            this.currentLayers.push(this.satelliteLayer);
            
            // 强制更新视图模式
            this.map.setMapStyle('normal');  // 首先重置样式
            this.map.setViewMode('3D', false, 0); 
            console.log('Added satellite layer and set to 3D view');
            
            // 检查是否成功添加
            const layers = this.map.getLayers();
            const hasSatellite = layers.some(layer => layer instanceof AMap.TileLayer.Satellite);
            console.log('Satellite layer added successfully:', hasSatellite);
            
            // 强制刷新地图
            this.forceRefreshMap();
          } catch (e) {
            console.error('Error switching to satellite view:', e);
          }
          break;

        case 'traffic':
          try {
            // 重新创建交通图层
            this.trafficLayer = new AMap.TileLayer.Traffic({
              zIndex: 10
            });
            
            // 显式添加图层到地图
            this.map.add(this.trafficLayer);
            this.currentLayers.push(this.trafficLayer);
            
            // 设置适合交通图层的样式
            this.map.setMapStyle('normal');
            this.map.setViewMode('2D', false, 0);
            console.log('Added traffic layer');
            
            // 检查是否成功添加
            const layers = this.map.getLayers();
            const hasTraffic = layers.some(layer => layer instanceof AMap.TileLayer.Traffic);
            console.log('Traffic layer added successfully:', hasTraffic);
            
            // 强制刷新地图
            this.forceRefreshMap();
          } catch (e) {
            console.error('Error switching to traffic view:', e);
          }
          break;

        case 'standard':
          try {
            // 显式设置标准地图样式
            this.map.setMapStyle('amap://styles/fresh');
            this.map.setViewMode('2D', false, 0);
            console.log('Set to standard 2D view');
            
            // 即使是标准地图也强制刷新
            this.forceRefreshMap();
          } catch (e) {
            console.error('Error switching to standard view:', e);
          }
          break;
          
        default:
          console.error('Unknown map type:', type);
          // 默认回到标准视图
          this.map.setMapStyle('amap://styles/fresh');
          this.map.setViewMode('2D', false, 0);
          this.forceRefreshMap();
      }
      
      // 确保控件在切换后仍然可见
      this.ensureControlsVisible();
    },
    
    // 强制刷新地图的方法
    forceRefreshMap() {
      // 暂时禁用地图交互
      this.map.setStatus({
        dragEnable: false,
      });
      
      // 延时重新启用交互并刷新
      setTimeout(() => {
        // 重新启用地图交互
        this.map.setStatus({
          dragEnable: true,
        });
        
        // 强制resize地图以刷新显示
        this.map.resize();
        
        // 确保控件在刷新后仍然可见
        this.ensureControlsVisible();
        
        console.log('Map forcefully refreshed');
      }, 100);
    },
    
    // 添加 resizeMap 方法，供父组件调用
    resizeMap() {
      if (this.map) {
        this.map.resize();
        
        // 确保控件在调整大小后仍然可见
        this.ensureControlsVisible();
        
        console.log('Map resized');
      }
    },
    
    // 添加缩放方法，供父组件调用
    zoomIn() {
      if (this.map) {
        const zoom = this.map.getZoom();
        this.map.setZoom(zoom + 1, false, 0); // 立即缩放，不使用动画
        console.log('Zoomed in to:', zoom + 1);
      }
    },
    
    zoomOut() {
      if (this.map) {
        const zoom = this.map.getZoom();
        this.map.setZoom(Math.max(1, zoom - 1), false, 0); // 立即缩放，不使用动画
        console.log('Zoomed out to:', Math.max(1, zoom - 1));
      }
    },
    
    // 新增: 处理在地图上显示设备的事件
    handleShowDeviceOnMap(event) {
      const deviceData = event.detail;
      
      // 显式禁用自动聚焦
      const noAutoFocus = deviceData.noAutoFocus || this.disableAutoFocus;
      
      // 检查是否已经有该设备的标记
      const existingMarker = this.markers.find(marker => {
        const extData = marker.getExtData();
        return extData && extData.id === deviceData.id;
      });
      
      if (existingMarker) {
        // 如果标记已存在且未禁用自动聚焦，则聚焦到该标记
        if (!noAutoFocus && this.initialLoadComplete) {
          this.handleCenterMapOnDevice({
            detail: {
              id: deviceData.id,
              coordinates: existingMarker.getPosition()
            }
          });
        }
        return;
      }
      
      // 创建设备图标基于类型
      let icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png';
      
      if (deviceData.deviceType === '1') {
        icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png'; // 激光雷达
      } else if (deviceData.deviceType === '3') {
        icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_y.png'; // 毫米波雷达
      } else if (deviceData.deviceType === '4') {
        icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png'; // 融合设备
      }
      
      // 为设备创建新标记
      const marker = new AMap.Marker({
        position: deviceData.coordinates,
        map: this.map,
        title: deviceData.title || deviceData.id,
        icon: icon,
        extData: { 
          id: deviceData.id, 
          type: 'device',
          deviceType: deviceData.deviceType 
        }
      });
      
      // 添加点击事件显示信息窗口
      marker.on('click', () => {
        this.showDeviceInfoFromMarker(deviceData, marker);
      });
      
      // 添加到标记集合
      this.markers.push(marker);
      
      // 只有在未禁用自动聚焦且初始加载完成后才聚焦
      if (!noAutoFocus && this.initialLoadComplete) {
        this.handleCenterMapOnDevice({
          detail: {
            id: deviceData.id,
            coordinates: deviceData.coordinates
          }
        });
      }
    },
    
    // 显示设备标记的信息窗口
    showDeviceInfoFromMarker(deviceData, marker) {
      // 获取设备类型文本
      let deviceTypeText = '未知设备';
      
      if (deviceData.deviceType === '1') {
        deviceTypeText = '激光雷达';
      } else if (deviceData.deviceType === '2') {
        deviceTypeText = '摄像头';
      } else if (deviceData.deviceType === '3') {
        deviceTypeText = '毫米波雷达';
      } else if (deviceData.deviceType === '4') {
        deviceTypeText = '融合设备';
      } else if (deviceData.type) {
        deviceTypeText = deviceData.type;
      }
      
      // 创建信息窗口内容
      const content = `
        <div style="width: 220px;">
          <h4 style="margin-bottom: 8px; color: #1a73e8;">${deviceData.id || deviceData.rcuId}</h4>
          <p style="margin: 4px 0;">类型: <span style="color: #0091ea; font-weight: 500;">${deviceTypeText}</span></p>
          <div style="display: flex; justify-content: space-between; margin-top: 12px;">
            <button id="locate-btn" style="padding: 6px 12px; background-color: #1a73e8; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; margin-right: 8px; font-weight: 500;">
              精确定位
            </button>
            <button id="detail-btn" style="padding: 6px 12px; background-color: #34a853; color: white; border: none; border-radius: 4px; cursor: pointer; flex: 1; font-weight: 500;">
              详细信息
            </button>
          </div>
        </div>
      `;
      
      this.infoWindow.setContent(content);
      this.infoWindow.open(this.map, marker.getPosition());
      
      // 添加按钮事件监听
      setTimeout(() => {
        const locateBtn = document.getElementById('locate-btn');
        const detailBtn = document.getElementById('detail-btn');
        
        if (locateBtn) {
          locateBtn.addEventListener('click', () => {
            const deviceId = deviceData.id || deviceData.rcuId;
            this.handleCenterMapOnDevice({
              detail: {
                id: deviceId,
                coordinates: marker.getPosition()
              }
            });
          });
        }
        
        if (detailBtn) {
          detailBtn.addEventListener('click', () => {
            const deviceId = deviceData.id || deviceData.rcuId;
            // 使用window.location.href直接跳转，避免路由问题
            window.location.href = `/detail?rcuId=${deviceId}`;
          });
        }
      }, 10);
    },
    
    // 触发设备搜索
    triggerDeviceSearch(deviceId) {
      // 创建自定义事件触发DeviceSearchBar组件中的设备搜索
      window.dispatchEvent(new CustomEvent('trigger-device-search', {
        detail: { deviceId: deviceId }
      }));
    },
    
    // 处理将地图中心定位到设备
    handleCenterMapOnDevice(event) {
      const { id, coordinates } = event.detail;
      
      // 查找设备的已有标记
      const marker = this.markers.find(marker => {
        const extData = marker.getExtData();
        return extData && extData.id === id;
      });
      
      if (marker) {
        // 查找对应的设备数据
        const deviceData = this.devices.find(device => device.rcuId === id) || {
          rcuId: id,
          deviceType: marker.getExtData().deviceType || '2'
        };
        
        // 在定位之前关闭现有的信息窗口
        if (this.infoWindow && this.infoWindow.getIsOpen()) {
          this.infoWindow.close();
        }
        
        // 保存当前地图状态
        this.saveMapState();
        
        // 将地图中心定位到该标记(带动画)
        this.map.setCenter(coordinates);
        this.map.setZoom(16);
        
        // 高亮标记(添加弹跳动画)
        marker.setAnimation('AMAP_ANIMATION_BOUNCE');
        
        // 打开该标记的信息窗口
        this.showDeviceInfoFromMarker(deviceData, marker);
        
        // 3秒后停止动画
        setTimeout(() => {
          if (marker) {
            marker.setAnimation(null);
          }
        }, 3000);
      } else {
        console.warn('没有找到设备标记:', id);
      }
    }
  }
}
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
  position: relative;
  min-height: 500px;
}

/* 地图状态指示器 */
.map-status-indicator {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 15px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.status-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #fff;
  border-radius: 50%;
  margin-right: 8px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 拓扑映射控制 */
.topology-mapping-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 10px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 90;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.topology-info {
  margin-bottom: 8px;
  font-size: 14px;
}

.clear-topology-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s;
  width: 100%;
}

.clear-topology-btn:hover {
  background-color: #1967d2;
}

/* 拓扑设备聚合样式 */
:deep(.cluster-marker) {
  width: 80px;
  height: 80px;
  background-color: rgba(26, 115, 232, 0.7);
  border: 2px solid rgba(255, 255, 255, 0.8);
  color: white;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  font-weight: bold;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
}

:deep(.cluster-marker span) {
  font-size: 18px;
  margin-top: 4px;
}

:deep(.cluster-marker:hover) {
  transform: scale(1.05);
  background-color: rgba(26, 115, 232, 0.9);
  box-shadow: 0 2px 15px rgba(0, 181, 255, 0.5);
}
</style>