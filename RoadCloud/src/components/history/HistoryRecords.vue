<template>
  <div class="panel-box history-records">
    <div class="chart-title">
      <div class="title-text">
        <i class="history-icon"></i>历史记录
      </div>
      <span class="refresh-icon" @click="refreshHistoryRecords" title="刷新历史记录">↻</span>
    </div>
    <div class="panel-content">
      <!-- 时间选择器部分 -->
      <div class="time-selector">
        <div class="time-group">
          <div class="time-label">开始时间</div>
          <div class="time-input-wrapper" @click="showStartTimeModal = true">
            <div class="time-display">{{ formattedStartTime }}</div>
            <div class="time-icon">
              <i class="calendar-icon"></i>
            </div>
          </div>
        </div>
        <div class="time-group">
          <div class="time-label">结束时间</div>
          <div class="time-input-wrapper" @click="showEndTimeModal = true">
            <div class="time-display">{{ formattedEndTime }}</div>
            <div class="time-icon">
              <i class="calendar-icon"></i>
            </div>
          </div>
        </div>
        <!-- 添加测试按钮 -->
        <div class="debug-controls" v-if="isDebugMode">
          <button class="debug-btn" @click="addTestRecord">添加测试记录</button>
          <button class="debug-btn" @click="clearAndAddTestRecords">重置并添加测试记录</button>
        </div>
      </div>

      <!-- 新增: 导出和回放记录显示区域 -->
      <div class="history-records-list">
        <div class="records-header">
          <div class="records-title">历史操作记录 ({{ historyRecords.length }})</div>
          <div class="records-actions">
            <button class="clear-btn" @click="clearHistoryRecords" title="清空记录">
              <i class="clear-icon"></i>清空
            </button>
          </div>
        </div>
        <div class="records-container">
          <div v-if="historyRecords.length === 0" class="no-records-message">
            暂无历史操作记录
          </div>
          <div v-else v-for="(record, index) in displayedRecords" :key="index" class="record-item" :class="record.type">
            <div class="record-icon" :class="record.type + '-icon'"></div>
            <div class="record-content">
              <div class="record-title">{{ record.title }}</div>
              <div class="record-time-range">
                {{ formatShortDateTime(record.startTime) }} 至 {{ formatShortDateTime(record.endTime) }}
              </div>
            </div>
            <div class="record-actions">
              <button class="record-action-btn repeat" @click="repeatOperation(record)" title="重复此操作">
                <i class="repeat-icon"></i>
              </button>
            </div>
          </div>
        </div>
        <div class="records-footer" v-if="historyRecords.length > maxDisplayRecords">
          <button class="show-more-btn" @click="toggleShowAllRecords">
            {{ showAllRecords ? '收起' : '显示更多...' }}
          </button>
        </div>
      </div>

      <!-- 控制按钮部分 -->
      <div class="history-controls">
        <button class="history-btn play-btn" @click="startHistoryPlayback" :disabled="isPlaybackActive" :class="{ 'disabled': isPlaybackActive }">
          <i class="btn-icon play-icon"></i>
          <span>回放</span>
        </button>
        <button class="history-btn import-btn" @click="openFileImport">
          <i class="btn-icon import-icon"></i>
          <span>导入</span>
        </button>
        <button class="history-btn export-btn" @click="showExportOptions = true">
          <i class="btn-icon export-icon"></i>
          <span>导出</span>
        </button>
      </div>
      <div class="history-status" v-if="historyStatus">
        <div class="status-indicator"></div>
        <span>{{ historyStatus }}</span>
      </div>
      <div class="playback-progress" v-if="isPlaybackActive">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: playbackProgress + '%' }"></div>
          <div class="progress-handle" :style="{ left: playbackProgress + '%' }"></div>
        </div>
        <div class="progress-time">{{ formatPlaybackTime(currentPlaybackTime) }}</div>
      </div>
      <!-- 隐藏的文件输入元素 -->
      <input 
        type="file" 
        ref="fileInput" 
        @change="handleFileImport" 
        accept=".glb" 
        style="display:none;"
      />
      <!-- 添加回放文件选择器 -->
      <input 
        type="file" 
        ref="playbackFileInput" 
        @change="handlePlaybackFileSelect" 
        accept=".glb" 
        style="display:none;"
      />
    </div>
  </div>

  <!-- 开始时间选择弹窗 -->
  <div v-if="showStartTimeModal" class="modal-overlay" @click.self="showStartTimeModal = false">
    <div class="modal-content datetime-modal">
      <div class="modal-header">
        <h3>选择开始时间</h3>
        <div class="close-btn" @click="showStartTimeModal = false">&times;</div>
      </div>
      <div class="modal-body">
        <input 
          class="time-input-modal" 
          type="datetime-local" 
          v-model="tempStartTime" 
          step="1"
        />
      </div>
      <div class="modal-footer">
        <button class="modal-btn cancel" @click="showStartTimeModal = false">取消</button>
        <button class="modal-btn confirm" @click="confirmStartTime">确认</button>
      </div>
    </div>
  </div>

  <!-- 结束时间选择弹窗 -->
  <div v-if="showEndTimeModal" class="modal-overlay" @click.self="showEndTimeModal = false">
    <div class="modal-content datetime-modal">
      <div class="modal-header">
        <h3>选择结束时间</h3>
        <div class="close-btn" @click="showEndTimeModal = false">&times;</div>
      </div>
      <div class="modal-body">
        <input 
          class="time-input-modal" 
          type="datetime-local" 
          v-model="tempEndTime" 
          step="1"
        />
      </div>
      <div class="modal-footer">
        <button class="modal-btn cancel" @click="showEndTimeModal = false">取消</button>
        <button class="modal-btn confirm" @click="confirmEndTime">确认</button>
      </div>
    </div>
  </div>

  <!-- 导出选项弹窗 -->
  <div v-if="showExportOptions" class="modal-overlay" @click.self="showExportOptions = false">
    <div class="modal-content export-modal">
      <div class="modal-header">
        <h3>选择导出格式</h3>
        <div class="close-btn" @click="showExportOptions = false">&times;</div>
      </div>
      <div class="modal-body">
        <div class="export-options">
          <div class="export-option" @click="exportHistoryData('glb')">
            <div class="export-icon model-icon"></div>
            <div class="export-info">
              <div class="export-title">3D模型文件(GLB)</div>
              <div class="export-desc">导出所选时间段内的3D模型文件，用于后续分析或展示</div>
            </div>
          </div>
          <div class="export-option" @click="exportHistoryData('excel')">
            <div class="export-icon excel-icon"></div>
            <div class="export-info">
              <div class="export-title">Excel数据报表</div>
              <div class="export-desc">导出所选时间段内的交通数据统计报表，含车流量、行人数量等</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 加载中弹窗 -->
  <div v-if="showLoadingModal" class="modal-overlay">
    <div class="loading-modal">
      <div class="loading-spinner"></div>
      <div class="loading-text">{{ loadingText }}</div>
    </div>
  </div>

  <!-- 操作结果提示弹窗 -->
  <div v-if="showResultToast" class="result-toast" :class="{'success': resultIsSuccess, 'error': !resultIsSuccess}">
    <div class="toast-icon" :class="{'success-icon': resultIsSuccess, 'error-icon': !resultIsSuccess}"></div>
    <span>{{ resultMessage }}</span>
  </div>

  <!-- 回放确认弹窗 -->
  <div v-if="showPlaybackConfirm" class="modal-overlay" @click.self="showPlaybackConfirm = false">
    <div class="modal-content playback-confirm-modal">
      <div class="modal-header">
        <h3>确认回放</h3>
        <div class="close-btn" @click="showPlaybackConfirm = false">&times;</div>
      </div>
      <div class="modal-body">
        <p>模型文件已下载到您的计算机。请选择刚刚下载的GLB文件进行回放。</p>
        <div class="file-selection-area">
          <button class="file-select-btn" @click="selectPlaybackFile">
            <i class="btn-icon file-icon"></i> 选择GLB文件
          </button>
          <div class="selected-file" v-if="selectedPlaybackFile">
            已选择: {{ selectedPlaybackFile }}
          </div>
        </div>
        <div class="model-info">
          <div class="model-info-item">
            <span class="model-info-label">开始时间:</span>
            <span class="model-info-value">{{ formattedStartTime }}</span>
          </div>
          <div class="model-info-item">
            <span class="model-info-label">结束时间:</span>
            <span class="model-info-value">{{ formattedEndTime }}</span>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="modal-btn cancel" @click="showPlaybackConfirm = false">取消</button>
        <button class="modal-btn confirm" @click="confirmStartPlayback" :disabled="!selectedPlaybackFile">开始回放</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'

// 历史记录相关数据
// const historyStartTime = ref(new Date(Date.now() - 3600000).toISOString().slice(0, 19)) // 默认一小时前
// const historyEndTime = ref(new Date().toISOString().slice(0, 19)) // 默认当前时间
const historyStartTime = ref('2025-04-01T00:00:01') // 2025年4月1日0点00分01秒
const historyEndTime = ref('2025-04-01T00:00:08') // 2025年4月1日0点00分08秒
const historyStatus = ref('')
const isPlaybackActive = ref(false)
const playbackProgress = ref(0)
const currentPlaybackTime = ref(null)
const playbackInterval = ref(null)
const fileInput = ref(null) // 添加文件输入引用

// 调试模式标志
const isDebugMode = ref(false) // 设置为true启用调试功能

// 新增: 历史记录列表数据
const historyRecords = ref([])
const showAllRecords = ref(false)
const maxDisplayRecords = 2 // 修改为2条记录

// 调试函数: 添加测试记录
const addTestRecord = () => {
  const now = new Date();
  const oneHourAgo = new Date(now.getTime() - 3600000);
  
  // 随机选择一种记录类型
  const recordTypes = ['export-glb', 'export-excel', 'playback'];
  const randomType = recordTypes[Math.floor(Math.random() * recordTypes.length)];
  
  // 添加测试记录
  addHistoryRecord(
    randomType, 
    oneHourAgo.toISOString(), 
    now.toISOString()
  );
  
  showToast('已添加测试记录', true);
};

// 调试函数: 清空并添加多条测试记录
const clearAndAddTestRecords = () => {
  // 清空记录
  historyRecords.value = [];
  
  // 添加多条测试记录
  const now = new Date();
  
  // 添加3种不同类型的记录
  addHistoryRecord('export-glb', new Date(now.getTime() - 3600000).toISOString(), now.toISOString());
  addHistoryRecord('export-excel', new Date(now.getTime() - 7200000).toISOString(), new Date(now.getTime() - 3600000).toISOString());
  addHistoryRecord('playback', new Date(now.getTime() - 10800000).toISOString(), new Date(now.getTime() - 7200000).toISOString());
  
  // 添加额外记录以测试"显示更多"功能
  for (let i = 0; i < 3; i++) {
    const startTime = new Date(now.getTime() - (i + 4) * 3600000);
    const endTime = new Date(startTime.getTime() + 3600000);
    const types = ['export-glb', 'export-excel', 'playback'];
    addHistoryRecord(types[i % 3], startTime.toISOString(), endTime.toISOString());
  }
  
  showToast('已重置并添加测试记录', true);
};

// 显示的记录条数（受showAllRecords控制）
const displayedRecords = computed(() => {
  return historyRecords.value;
})

// 时间选择器和导出选项弹窗相关
const showStartTimeModal = ref(false)
const showEndTimeModal = ref(false)
const showExportOptions = ref(false)
const showPlaybackConfirm = ref(false) // 回放确认弹窗
const tempStartTime = ref(historyStartTime.value)
const tempEndTime = ref(historyEndTime.value)

// 回放临时存储数据
const pendingPlaybackData = ref(null)

// 加载和结果提示相关
const showLoadingModal = ref(false) 
const loadingText = ref('正在处理请求...')
const showResultToast = ref(false)
const resultMessage = ref('')
const resultTimeout = ref(null)
const resultIsSuccess = ref(true)

// 添加文件选择相关状态
const playbackFileInput = ref(null) // 回放文件输入引用
const selectedPlaybackFile = ref('') // 已选择的回放文件名

// 可用的模型数据
const availableModels = [
  { time: '2023-05-01T08:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T09:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T10:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T11:00:00', file: '/car_animation.glb' },
  { time: '2023-05-01T12:00:00', file: '/car_animation.glb' }
]

// 格式化日期时间显示
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '';
  
  const date = new Date(dateTimeString);
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

// 新增: 更简短的日期时间格式
const formatShortDateTime = (dateTimeString) => {
  if (!dateTimeString) return '';
  
  const date = new Date(dateTimeString);
  
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  
  return `${month}-${day} ${hours}:${minutes}`;
}

// 格式化时间显示
const formatPlaybackTime = (timestamp) => {
  if (!timestamp) return '--:--:--';
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour12: false });
};

// 确认开始时间选择
const confirmStartTime = () => {
  historyStartTime.value = tempStartTime.value;
  showStartTimeModal.value = false;
}

// 确认结束时间选择
const confirmEndTime = () => {
  historyEndTime.value = tempEndTime.value;
  showEndTimeModal.value = false;
}

// 新增: 切换显示全部或部分记录
const toggleShowAllRecords = () => {
  showAllRecords.value = !showAllRecords.value;
}

// 新增: 清空历史记录
const clearHistoryRecords = () => {
  if (confirm('确定要清空所有历史记录吗？此操作不可撤销。')) {
    console.log('清空历史记录');
    historyRecords.value = [];
    localStorage.setItem('historyOperationRecords', JSON.stringify([]));
    showToast('历史记录已清空', true);
  }
};

// 新增: 重复之前的操作
const repeatOperation = (record) => {
  historyStartTime.value = record.startTime;
  historyEndTime.value = record.endTime;
  
  // 根据记录类型执行不同操作
  if (record.type === 'export-glb') {
    exportHistoryData('glb');
  } else if (record.type === 'export-excel') {
    exportHistoryData('excel');
  } else if (record.type === 'playback') {
    startHistoryPlayback();
  }
}

// 新增: 添加记录到历史记录中
const addHistoryRecord = (type, startTime, endTime) => {
  // 创建记录对象
  const typeMap = {
    'export-glb': '3D模型导出',
    'export-excel': 'Excel导出',
    'playback': '数据回放'
  };
  
  const record = {
    type,
    title: typeMap[type] || '操作记录',
    startTime,
    endTime,
    timestamp: new Date().toISOString()
  };
  
  console.log('添加新记录:', record);
  
  // 添加到记录列表前面
  historyRecords.value.unshift(record);
  
  // 限制记录数量，最多保留20条
  if (historyRecords.value.length > 20) {
    historyRecords.value = historyRecords.value.slice(0, 20);
  }
  
  // 保存到本地存储
  localStorage.setItem('historyOperationRecords', JSON.stringify(historyRecords.value));
  console.log('记录已保存到localStorage, 当前共有', historyRecords.value.length, '条记录');
}

// 新增: 通用Toast提示函数
const showToast = (message, isSuccess = true) => {
  resultMessage.value = message;
  resultIsSuccess.value = isSuccess;
  showResultToast.value = true;
  
  // 清除之前的定时器
  if (resultTimeout.value) {
    clearTimeout(resultTimeout.value);
  }
  
  // 设置新定时器，3秒后自动关闭
  resultTimeout.value = setTimeout(() => {
    showResultToast.value = false;
  }, 3000);
}

// 修改: 导出历史数据函数，增加记录添加
const exportHistoryData = (format) =>   {
  // 关闭导出选项弹窗
  showExportOptions.value = false;
  
  
    console.log('执行导出操作:', format);
    const startDate = new Date(historyStartTime.value);
    const endDate = new Date(historyEndTime.value);
    
    if (startDate >= endDate) {
      historyStatus.value = '错误：开始时间必须早于结束时间';
      setTimeout(() => {
        historyStatus.value = '';
      }, 3000);
      return;
    }
    
    // 验证时间差值不超过30分钟
    const timeDifferenceMs = endDate.getTime() - startDate.getTime();
    const timeDifferenceMinutes = timeDifferenceMs / (1000 * 60);
    
    if (timeDifferenceMinutes > 30) {
      historyStatus.value = '错误：时间范围不能超过30分钟';
      setTimeout(() => {
        historyStatus.value = '';
      }, 3000);
      return;
    }
    
    console.log('验证通过，准备添加导出记录');
    
    // 添加到历史记录 - 将这一步移到最前面，确保记录被添加
    const recordType = format === 'glb' ? 'export-glb' : 'export-excel';
    addHistoryRecord(recordType, historyStartTime.value, historyEndTime.value);
    console.log(`已添加${recordType}记录`);

    // 显示加载中弹窗
    loadingText.value = `正在准备${format === 'glb' ? '3D模型' : 'Excel报表'}数据...`;
    showLoadingModal.value = true;
    
    // 准备 API 请求参数，将日期转换为时间戳
    const startTimestamp = Math.floor(new Date(historyStartTime.value).getTime() / 1000);
    const endTimestamp = Math.floor(new Date(historyEndTime.value).getTime() / 1000);
    
    // 记录原始和修正后的时间戳，便于调试
    console.log('原始时间戳:', {
      start: new Date(historyStartTime.value).getTime(),
      end: new Date(historyEndTime.value).getTime()
    });
    console.log('修正后的时间戳:', {
      start: startTimestamp,
      end: endTimestamp
    });
    
    if (format === 'glb') {
    // 记录导出GLB的请求信息
    const requestUrl = `/system/creat/exportglb?starttime=${startTimestamp}&endtime=${endTimestamp}`;
    console.log('========== 导出GLB模型请求 ==========');
    console.log('请求URL:', requestUrl);
    console.log('请求参数:', {
      starttime: startTimestamp,
      endtime: endTimestamp,
      formattedStart: new Date(startTimestamp * 1000).toLocaleString('zh-CN'),
      formattedEnd: new Date(endTimestamp * 1000).toLocaleString('zh-CN')
    });
    
    // GLB 文件导出 - 调用后端接口
    // 第一个请求 - 调用创建导出GLB文件接口
    fetch(requestUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    .then(response => {
      console.log('响应状态:', response.status, response.statusText);
      return response.json();
    })
    .then(data => {
      console.log('第一次请求响应数据:', data);
      
      if (data.status === 'success' && (data.filepath || data.url)) {
        // 更新加载文字
        loadingText.value = "正在准备下载3D模型文件...";
        
        // 创建日期格式作为文件名
        const startDate = new Date(historyStartTime.value);
        const endDate = new Date(historyEndTime.value);
        const formattedStartDate = formatDateForFileName(startDate);
        const formattedEndDate = formatDateForFileName(endDate);
        
        // 设置文件名
        const fileName = `交通数据_${formattedStartDate}_至_${formattedEndDate}.glb`;
        
        // 第二次请求 - 下载文件
        const filePath = data.filepath || data.url;
        const filePathWithSlash = filePath.startsWith('/') ? filePath : `/${filePath}`;
        const downloadUrl = `/download${filePathWithSlash}`;
        
        console.log('========== 下载GLB文件请求 ==========');
        console.log('原始文件URL:', downloadUrl);
        console.log('文件路径:', filePathWithSlash);
        
        // 解决CORS问题：尝试使用后端代理下载文件
        fetch(downloadUrl, {
          method: 'GET'
        })
        .then(response => {
          console.log('下载响应状态:', response.status, response.statusText);
          console.log('下载响应头:', Object.fromEntries([...response.headers.entries()]));
          
          if (!response.ok) {
            throw new Error(`下载失败: ${response.status}`);
          }
          return response.blob();
        })
        .then(blob => {
          console.log('下载文件类型:', blob.type);
          console.log('下载文件大小:', `${(blob.size / 1024).toFixed(2)} KB`);
          
          // 下载GLB文件
          downloadBlob(blob, fileName);
          
          // 关闭加载弹窗
          showLoadingModal.value = false;
          
          // 显示成功消息
          showToast("3D模型文件导出成功");
          
          console.log('文件下载完成:', fileName);
        })
        .catch(error => {
          console.error("下载文件时出错:", error);
          
          // 更新加载提示
          loadingText.value = "正在尝试替代下载方式...";
          
          // 备选方案：直接使用window.open
          historyStatus.value = "正在尝试替代下载方式...";
          const backupFilePath = data.filepath || data.url;
          const backupFilePathWithSlash = backupFilePath.startsWith('/') ? backupFilePath : `/${backupFilePath}`;
          const fileUrl = `/download${backupFilePathWithSlash}`;
          console.log('尝试备选下载方式:', fileUrl);
          window.open(fileUrl, '_blank');
          
          setTimeout(() => {
            // 关闭加载弹窗
            showLoadingModal.value = false;
            
            // 显示消息
            showToast("如果文件没有自动下载，请检查浏览器是否阻止了弹出窗口");
          }, 3000);
        });
      } else {
        // 检查实际返回结构并记录
        console.log('服务器返回数据结构:', Object.keys(data));
        if (data.status === 'success') {
          console.log('服务器返回了成功状态，但缺少预期的文件路径信息');
          showToast("导出格式不匹配，请联系管理员");
        } else {
          console.error('导出GLB文件失败:', data);
          showToast("导出失败，请稍后重试");
        }
        
        // 关闭加载弹窗
        showLoadingModal.value = false;
      }
    })
    .catch(error => {
      console.error("请求导出GLB时出错:", error);
      showToast("导出请求失败，请检查网络连接");
      
      // 关闭加载弹窗
      showLoadingModal.value = false;
    });
  } else {
    // 处理 Excel 报表导出 - 调用后端接口
    // 记录导出Excel的请求信息
    const requestUrl = `/system/creat/export?starttime=${startTimestamp}&endtime=${endTimestamp}`;
    console.log('========== 导出Excel报表请求 ==========');
    console.log('请求URL:', requestUrl);
    console.log('请求参数:', {
      starttime: startTimestamp,
      endtime: endTimestamp,
      formattedStart: new Date(startTimestamp * 100).toLocaleString('zh-CN'),
      formattedEnd: new Date(endTimestamp * 100).toLocaleString('zh-CN')
    });
    
    // 第一个请求 - 调用创建导出文件接口
    fetch(requestUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    .then(response => {
      console.log('响应状态:', response.status, response.statusText);
      return response.json();
    })
    .then(data => {
      console.log('第一次请求响应数据:', data);
      
      if (data.status === 'success' && (data.filepath || data.url)) {
        // 更新加载文字
        loadingText.value = "正在准备下载文件...";
        
        // 创建日期格式作为文件名
        const startDate = new Date(historyStartTime.value);
        const endDate = new Date(historyEndTime.value);
        const formattedStartDate = formatDateForFileName(startDate);
        const formattedEndDate = formatDateForFileName(endDate);
        
        // 设置文件名
        const fileName = `交通数据_${formattedStartDate}_至_${formattedEndDate}.csv`;
        
        // 第二次请求 - 下载文件
        const filePath = data.filepath || data.url;
        const excelPathWithSlash = filePath.startsWith('/') ? filePath : `/${filePath}`;
        const downloadUrl = `/api${excelPathWithSlash}`;
        
        console.log('========== 下载Excel文件请求 ==========');
        console.log('原始文件URL:', downloadUrl);
        console.log('文件路径:', excelPathWithSlash);
        
        // 解决CORS问题：使用后端代理下载文件
        // 方法1：如果后端提供了代理接口，使用后端代理
        // 使用本地后端服务作为中介，避免直接请求外部服务器
        fetch(downloadUrl, {
          method: 'GET'
        })
        .then(response => {
          console.log('下载响应状态:', response.status, response.statusText);
          console.log('下载响应头:', Object.fromEntries([...response.headers.entries()]));
          
          if (!response.ok) {
            throw new Error(`下载失败: ${response.status}`);
          }
          return response.blob();
        })
        .then(blob => {
          console.log('下载文件类型:', blob.type);
          console.log('下载文件大小:', `${(blob.size / 1024).toFixed(2)} KB`);
          
          // 下载CSV文件
          downloadBlob(blob, fileName);
          
          // 关闭加载弹窗
          showLoadingModal.value = false;
          
          // 显示成功消息
          showToast("Excel报表导出成功");
          
          console.log('文件下载完成:', fileName);
        })
        .catch(error => {
          console.error("下载文件时出错:", error);
          
          // 方法2：尝试直接使用window.open作为备选方案
          // 通过在新窗口打开URL的方式来下载文件
          // 注意：这种方法可能仍受到CORS限制，但在某些情况下有效
          showToast("正在尝试替代下载方式...");
          const backupFilePath = data.filepath || data.url;
          const backupFilePathWithSlash = backupFilePath.startsWith('/') ? backupFilePath : `/${backupFilePath}`;
          const fileUrl = `/api${backupFilePathWithSlash}`;
          console.log('尝试备选下载方式:', fileUrl);
          window.open(fileUrl, '_blank');
          
          setTimeout(() => {
            // 关闭加载弹窗
            showLoadingModal.value = false;
            
            // 显示消息
            showToast("如果文件没有自动下载，请检查浏览器是否阻止了弹出窗口");
          }, 3000);
        });
      } else {
        // 检查实际返回结构并记录
        console.log('服务器返回数据结构:', Object.keys(data));
        if (data.status === 'success') {
          console.log('服务器返回了成功状态，但缺少预期的文件路径信息');
          showToast("导出格式不匹配，请联系管理员");
        } else {
          console.error('导出Excel文件失败:', data);
          showToast("导出失败，请稍后重试");
        }
        
        // 关闭加载弹窗
        showLoadingModal.value = false;
      }
    })
    .catch(error => {
      console.error("请求导出时出错:", error);
      showToast("导出请求失败，请检查网络连接");
      
      // 关闭加载弹窗
      showLoadingModal.value = false;
    });
  }
  
};

// 将日期格式化为文件名友好的格式
const formatDateForFileName = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  
  return `${year}${month}${day}_${hours}${minutes}`;
};

// 修改: 回放函数，增加记录添加
const startHistoryPlayback = () => {
  if (isPlaybackActive.value) return;
  
  console.log('========== 初始化回放过程 ==========');
  
  try {
    console.log('执行回放操作');
    const startDate = new Date(historyStartTime.value);
    const endDate = new Date(historyEndTime.value);
    
    if (startDate >= endDate) {
      historyStatus.value = '错误：开始时间必须早于结束时间';
      setTimeout(() => {
        historyStatus.value = '';
      }, 3000);
      return;
    }
    
    // 验证时间差值不超过30分钟
    const timeDifferenceMs = endDate.getTime() - startDate.getTime();
    const timeDifferenceMinutes = timeDifferenceMs / (1000 * 60);
    
    if (timeDifferenceMinutes > 30) {
      historyStatus.value = '错误：时间范围不能超过30分钟';
      setTimeout(() => {
        historyStatus.value = '';
      }, 3000);
      return;
    }
    
    console.log('验证通过，准备添加回放记录');
    
    // 添加到历史记录 - 将这一步移到最前面，确保记录被添加
    addHistoryRecord('playback', historyStartTime.value, historyEndTime.value);
    console.log('已添加回放记录');

    // 显示加载中弹窗
    loadingText.value = '正在准备回放数据...';
    showLoadingModal.value = true;
    
    // 准备 API 请求参数，将日期转换为时间戳
    const startTimestamp = Math.floor(new Date(historyStartTime.value).getTime() / 1000);
    const endTimestamp = Math.floor(new Date(historyEndTime.value).getTime() / 1000);
    
    // 记录原始和修正后的时间戳，便于调试
    console.log('回放操作 - 原始时间戳:', {
      start: new Date(historyStartTime.value).getTime(),
      end: new Date(historyEndTime.value).getTime()
    });
    console.log('回放操作 - 修正后的时间戳:', {
      start: startTimestamp,
      end: endTimestamp
    });
    
    // 和导出GLB文件相同的API请求
    const requestUrl = `/system/creat/exportglb?starttime=${startTimestamp}&endtime=${endTimestamp}`;
    console.log('========== 回放模型请求 ==========');
    console.log('请求URL:', requestUrl);
    console.log('请求参数:', {
      starttime: startTimestamp,
      endtime: endTimestamp,
      formattedStart: new Date(startTimestamp * 1000).toLocaleString('zh-CN'),
      formattedEnd: new Date(endTimestamp * 1000).toLocaleString('zh-CN')
    });
    
    // 第一个请求 - 调用创建导出GLB文件接口
    fetch(requestUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    .then(response => {
      console.log('响应状态:', response.status, response.statusText);
      return response.json();
    })
    .then(data => {
      console.log('第一次请求响应数据:', data);
      
      if (data.status === 'success' && (data.filepath || data.url)) {
        // 创建日期格式作为文件名提示
        const startDate = new Date(historyStartTime.value);
        const endDate = new Date(historyEndTime.value);
        const formattedStartDate = formatDateForFileName(startDate);
        const formattedEndDate = formatDateForFileName(endDate);
        const suggestedFileName = `交通数据_${formattedStartDate}_至_${formattedEndDate}.glb`;
        
        // 更新加载文字
        loadingText.value = "正在准备下载3D模型文件...";
        
        // 第二次请求 - 下载文件
        const filePath = data.filepath || data.url;
        const filePathWithSlash = filePath.startsWith('/') ? filePath : `/${filePath}`;
        const downloadUrl = `/download${filePathWithSlash}`;
        
        console.log('========== 加载模型文件请求 ==========');
        console.log('模型文件URL:', downloadUrl);
        console.log('文件路径:', filePathWithSlash);
        console.log('建议文件名:', suggestedFileName);
        
        // 直接使用window.open打开URL，避免跨域问题
        historyStatus.value = "正在下载模型文件...";
        console.log('使用window.open打开模型文件:', downloadUrl);
        
        // 打开文件下载
        window.open(downloadUrl, '_blank');
        
        // 设置定时器，稍等片刻后关闭加载弹窗并显示确认弹窗
        setTimeout(() => {
          // 关闭加载弹窗
          showLoadingModal.value = false;
          
          // 重置文件选择
          selectedPlaybackFile.value = '';
          
          // 存储回放信息
          pendingPlaybackData.value = {
            startDate: startDate,
            endDate: endDate,
            suggestedFileName: suggestedFileName
          };
          
          console.log('回放数据已准备:', pendingPlaybackData.value);
          
          // 显示确认回放弹窗
          showPlaybackConfirm.value = true;
          
          // 显示提示消息
          showToast(`请等待文件下载完成，然后选择下载路径下的"${suggestedFileName}"文件`, true);
          
        }, 2000); // 2秒后显示确认弹窗
      } else {
        // 检查实际返回结构并记录
        console.log('服务器返回数据结构:', Object.keys(data));
        if (data.status === 'success') {
          console.log('服务器返回了成功状态，但缺少预期的文件路径信息');
          showToast("导出格式不匹配，请联系管理员", false);
        } else {
          console.error('加载模型文件失败:', data);
          showToast("加载失败，请稍后重试", false);
        }
        
        // 关闭加载弹窗
        showLoadingModal.value = false;
      }
    })
    .catch(error => {
      console.error("请求模型数据时出错:", error);
      showToast("回放请求失败，请检查网络连接", false);
      
      // 关闭加载弹窗
      showLoadingModal.value = false;
    });
  } catch (error) {
    console.error('初始化回放过程出错:', error);
    showToast("回放初始化失败，请查看控制台", false);
    showLoadingModal.value = false;
  }
};

// 新增: 在组件挂载时加载历史记录
onMounted(() => {
  // 从localStorage加载历史记录
  const savedRecords = localStorage.getItem('historyOperationRecords');
  if (savedRecords) {
    try {
      historyRecords.value = JSON.parse(savedRecords);
      console.log('从localStorage加载历史记录:', historyRecords.value.length, '条记录');
    } catch (e) {
      console.error('解析历史记录出错:', e);
      historyRecords.value = [];
    }
  } else {
    console.log('localStorage中没有找到历史记录');
    // 初始化空数组
    historyRecords.value = [];
    localStorage.setItem('historyOperationRecords', JSON.stringify([]));
  }
  
  // 为测试添加一些示例记录
  if (historyRecords.value.length === 0) {
    console.log('添加示例记录以测试显示');
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 3600000);
    
    // 添加测试记录
    addHistoryRecord('export-glb', oneHourAgo.toISOString(), now.toISOString());
    addHistoryRecord('export-excel', oneHourAgo.toISOString(), now.toISOString());
    addHistoryRecord('playback', oneHourAgo.toISOString(), now.toISOString());
  }
});

// 刷新历史记录
const refreshHistoryRecords = () => {
  historyStatus.value = '历史记录已刷新';
  
  // 实际加载localStorage中的记录
  const savedRecords = localStorage.getItem('historyOperationRecords');
  if (savedRecords) {
    try {
      historyRecords.value = JSON.parse(savedRecords);
      console.log('刷新历史记录成功，加载了', historyRecords.value.length, '条记录');
    } catch (e) {
      console.error('刷新时解析历史记录出错:', e);
      historyRecords.value = [];
    }
  }
  
  // 提示用户
  setTimeout(() => {
    historyStatus.value = '';
  }, 2000);
};

// 打开文件导入对话框
const openFileImport = () => {
  fileInput.value.click();
};

// 处理文件导入事件
const handleFileImport = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 确保是GLB文件
  if (!file.name.toLowerCase().endsWith('.glb')) {
    historyStatus.value = "错误：只支持GLB格式的3D模型文件";
    setTimeout(() => {
      historyStatus.value = '';
    }, 3000);
    return;
  }
  
  historyStatus.value = `正在导入模型: ${file.name}`;
  
  // 创建文件的URL
  const fileURL = URL.createObjectURL(file);
  
  // 通过emit发送加载模型的事件，传递null作为标题，fileURL作为文件路径
  emit('loadModel', null, fileURL);
  
  // 清理状态
  setTimeout(() => {
    historyStatus.value = `模型导入成功: ${file.name}`;
    setTimeout(() => {
      historyStatus.value = '';
    }, 3000);
    
    // 重置文件输入，以便可以选择同一个文件
    event.target.value = '';
  }, 1000);
};

// 打开文件选择对话框选择回放文件
const selectPlaybackFile = () => {
  console.log('打开文件选择对话框...');
  playbackFileInput.value.click();
};

// 处理回放文件选择
const handlePlaybackFileSelect = (event) => {
  const file = event.target.files[0];
  console.log('选择的回放文件:', file);
  
  if (!file) return;
  
  // 确保是GLB文件
  if (!file.name.toLowerCase().endsWith('.glb')) {
    showToast("请选择GLB格式的3D模型文件");
    selectedPlaybackFile.value = '';
    return;
  }
  
  // 设置选中的文件名
  selectedPlaybackFile.value = file.name;
  console.log('已设置选中的回放文件:', selectedPlaybackFile.value);
};

// 确认开始回放的函数
const confirmStartPlayback = () => {
  console.log('========== 开始回放调试信息 ==========');
  console.log('1. 确认回放函数被调用');
  
  try {
    // 关闭确认弹窗
    showPlaybackConfirm.value = false;
    
    // 确保有待处理的回放数据
    if (!pendingPlaybackData.value) {
      console.error('错误: pendingPlaybackData为空，没有可用的回放数据');
      showToast("回放数据无效，请重试");
      return;
    }
    
    // 确保已选择文件
    if (!selectedPlaybackFile.value) {
      console.error('错误: 未选择回放文件');
      showToast("请先选择GLB模型文件");
      return;
    }
    
    console.log('2. 待处理回放数据:', pendingPlaybackData.value);
    console.log('3. 选择的文件名:', selectedPlaybackFile.value);
    
    const { startDate, endDate } = pendingPlaybackData.value;
    
    // 获取选择的文件
    const file = playbackFileInput.value.files[0];
    
    if (!file) {
      console.error('错误: 文件对象为空');
      showToast("文件选择出错，请重试");
      return;
    }
    
    // 创建文件URL对象
    const fileURL = URL.createObjectURL(file);
    console.log('已创建本地文件URL:', fileURL);
    
    // 启动回放控制逻辑
    isPlaybackActive.value = true;
    playbackProgress.value = 0;
    currentPlaybackTime.value = startDate.getTime();
    historyStatus.value = '模型加载成功，回放开始...';
    
    console.log('4. 准备调用loadModel事件，参数:', {
      title: null,
      fileURL: fileURL
    });
    
    try {
      // 使用本地文件URL发送加载模型的事件
      emit('loadModel', null, fileURL);
      console.log('5. loadModel事件已触发');
    } catch (emitError) {
      console.error('触发loadModel事件时出错:', emitError);
      showToast("加载模型失败，请检查控制台错误");
      return;
    }
    
    // 计算总时间跨度
    const totalDuration = endDate.getTime() - startDate.getTime();
    const playbackSpeed = 50; // 每50毫秒更新一次
    
    console.log('6. 回放控制参数:', {
      totalDuration: `${totalDuration}毫秒 (${totalDuration/1000}秒)`,
      playbackSpeed: `${playbackSpeed}毫秒/更新`,
      加速倍数: '100倍'
    });
    
    // 创建回放循环
    playbackInterval.value = setInterval(() => {
      // 更新当前时间
      currentPlaybackTime.value += playbackSpeed * 100; // 加速回放，100倍速
      
      // 计算进度
      const elapsed = currentPlaybackTime.value - startDate.getTime();
      playbackProgress.value = Math.min((elapsed / totalDuration) * 100, 100);
      
      // 检查是否完成
      if (currentPlaybackTime.value >= endDate.getTime()) {
        stopHistoryPlayback();
        historyStatus.value = '回放完成';
        console.log('7. 回放已完成');
        setTimeout(() => {
          historyStatus.value = '';
        }, 3000);
      }
    }, playbackSpeed);
    
    console.log('回放定时器已设置 ID:', playbackInterval.value);
    
    // 清理临时数据
    pendingPlaybackData.value = null;
  } catch (error) {
    console.error('回放过程中发生错误:', error);
    showToast("回放过程中出现错误，请查看控制台");
  }
};

// 停止历史回放
const stopHistoryPlayback = () => {
  if (playbackInterval.value) {
    clearInterval(playbackInterval.value);
    playbackInterval.value = null;
  }
  isPlaybackActive.value = false;
  playbackProgress.value = 100;
};

// 定义emit
const emit = defineEmits(['loadModel'])

// 计算属性 - 格式化开始和结束时间显示
const formattedStartTime = computed(() => {
  return formatDateTime(historyStartTime.value);
});

const formattedEndTime = computed(() => {
  return formatDateTime(historyEndTime.value);
});

// 生命周期钩子 - 卸载前
onBeforeUnmount(() => {
  // 清理回放定时器
  if (playbackInterval.value) {
    clearInterval(playbackInterval.value);
  }
})
</script>

<style scoped>
.panel-box {
  background-color: rgba(2, 12, 32, 0.8);
  border: 1px solid #0e5986;
  box-shadow: 0 0 20px rgba(0, 145, 234, 0.4);
  border-radius: 8px;
  padding: 15px;
  backdrop-filter: blur(5px);
  transition: all 0.5s ease;
  position: relative;
  overflow: hidden;
  /* 修改：增大高度以容纳历史记录 */
  height: auto;
  min-height: 300px;
  max-height: 600px;
}

.panel-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.7), transparent);
  z-index: 1;
}

.chart-title {
  font-size: 16px;
  margin-bottom: 15px;
  color: #00ffff;
  border-bottom: 1px solid rgba(14, 89, 134, 0.6);
  padding-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-text {
  font-weight: 500;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, #00ffff, #1e90ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.history-icon {
  display: inline-block;
  width: 18px;
  height: 18px;
  margin-right: 8px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>');
  background-size: contain;
  background-repeat: no-repeat;
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: rgba(0, 255, 255, 0.7);
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: rgba(14, 89, 134, 0.2);
}

.refresh-icon:hover {
  color: rgba(0, 255, 255, 1);
  transform: rotate(180deg);
  background-color: rgba(14, 89, 134, 0.4);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.panel-content {
  height: calc(100% - 50px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}

.time-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.time-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  padding-left: 2px;
}

.time-input-wrapper {
  background: linear-gradient(135deg, rgba(6, 42, 62, 0.7) 0%, rgba(4, 27, 49, 0.8) 100%);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 6px;
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
  height: 38px;
}

.time-input-wrapper:hover {
  border-color: rgba(0, 255, 255, 0.7);
  background: linear-gradient(135deg, rgba(6, 52, 72, 0.7) 0%, rgba(4, 37, 59, 0.8) 100%);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
}

.time-display {
  font-size: 13px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time-icon {
  color: rgba(0, 255, 255, 0.7);
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M9 10H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z"/></svg>');
  background-size: contain;
  background-repeat: no-repeat;
}

.history-controls {
  display: flex;
  gap: 10px;
  margin-bottom: 4px;
}

.history-btn {
  flex: 1;
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.5) 0%, rgba(4, 47, 79, 0.6) 100%);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 6px;
  padding: 8px 0;
  color: #00ffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
}

.history-btn:hover:not(.disabled) {
  background: linear-gradient(135deg, rgba(6, 102, 142, 0.6) 0%, rgba(4, 67, 99, 0.7) 100%);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 145, 234, 0.3);
}

.history-btn:active:not(.disabled) {
  transform: translateY(1px);
}

.history-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  display: inline-block;
  width: 18px;
  height: 18px;
  background-size: contain;
  background-repeat: no-repeat;
}

.play-icon {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M8 5v14l11-7z"/></svg>');
}

.import-icon {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>');
}

.export-icon {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M19 12v7H5v-7H3v7c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-7h-2zm-6 .67l2.59-2.58L17 11.5l-5 5-5-5 1.41-1.41L11 12.67V3h2z"/></svg>');
}

.file-icon {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z"/></svg>');
}

.history-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #00ffff;
  padding: 8px 12px;
  margin-top: 4px;
  border-radius: 6px;
  background-color: rgba(0, 30, 60, 0.5);
  border: 1px solid rgba(14, 89, 134, 0.4);
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #00ffff;
  box-shadow: 0 0 10px #00ffff;
  animation: pulse 1.5s infinite ease-in-out;
}

@keyframes pulse {
  0% { opacity: 0.3; }
  50% { opacity: 1; }
  100% { opacity: 0.3; }
}

.playback-progress {
  margin-top: 8px;
}

.progress-bar {
  height: 6px;
  background-color: rgba(14, 89, 134, 0.3);
  border-radius: 3px;
  overflow: visible;
  position: relative;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00b4ff 0%, #0077ff 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
  position: relative;
}

.progress-handle {
  position: absolute;
  width: 14px;
  height: 14px;
  background: #00ffff;
  border-radius: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.8);
  z-index: 2;
}

.progress-time {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: linear-gradient(135deg, rgba(6, 52, 82, 0.95) 0%, rgba(4, 27, 59, 0.95) 100%);
  border: 1px solid rgba(0, 180, 255, 0.3);
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 0 30px rgba(0, 145, 234, 0.4);
  width: 400px;
  max-width: 90%;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.modal-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.8), transparent);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.5);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  background: linear-gradient(90deg, #00ffff, #1e90ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.close-btn {
  font-size: 24px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s;
}

.close-btn:hover {
  color: #fff;
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.8);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.modal-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.modal-btn.cancel {
  background: rgba(40, 40, 40, 0.6);
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(80, 80, 80, 0.6);
}

.modal-btn.cancel:hover {
  background: rgba(60, 60, 60, 0.7);
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.1);
}

.modal-btn.confirm {
  background: linear-gradient(135deg, rgba(0, 180, 255, 0.6) 0%, rgba(0, 120, 215, 0.7) 100%);
  color: white;
  border: 1px solid rgba(0, 140, 235, 0.6);
}

.modal-btn.confirm:hover {
  background: linear-gradient(135deg, rgba(0, 200, 255, 0.7) 0%, rgba(0, 140, 235, 0.8) 100%);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.4);
  transform: translateY(-2px);
}

.modal-btn.confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.time-input-modal {
  width: 100%;
  background: rgba(0, 30, 60, 0.6);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 6px;
  padding: 10px 12px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.time-input-modal:focus {
  border-color: rgba(0, 255, 255, 0.7);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

/* 导出选项样式 */
.export-options {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.export-option {
  display: flex;
  align-items: center;
  gap: 15px;
  background: rgba(0, 30, 60, 0.5);
  border: 1px solid rgba(14, 89, 134, 0.5);
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.export-option:hover {
  background: rgba(0, 50, 80, 0.6);
  border-color: rgba(0, 180, 255, 0.6);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.3);
  transform: translateY(-3px);
}

.export-icon {
  width: 36px;
  height: 36px;
  background-size: contain;
  background-repeat: no-repeat;
  opacity: 0.9;
}

.model-icon {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M21 16.5c0 .38-.21.71-.53.88l-7.9 4.44c-.16.12-.36.18-.57.18-.21 0-.41-.06-.57-.18l-7.9-4.44A.991.991 0 0 1 3 16.5v-9c0-.38.21-.71.53-.88l7.9-4.44c.16-.12.36-.18.57-.18.21 0 .41.06.57.18l7.9 4.44c.32.17.53.5.53.88v9zM12 4.15L6.04 7.5 12 10.85l5.96-3.35L12 4.15z"/></svg>');
}

.excel-icon {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2300ffff"><path d="M19 3h-9.5L5 7.5V20c0 .55.45 1 1 1h12c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zm-6.5 15h-2v-2h2v2zm0-4h-2v-4h2v4zm5 4h-4v-1.5h4V18zm0-3.5h-4V13h4v1.5zm0-3h-4V9.5h4V11z"/></svg>');
}

.export-info {
  flex: 1;
}

.export-title {
  font-size: 15px;
  font-weight: 600;
  color: #00ffff;
  margin-bottom: 6px;
}

.export-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.4;
}

/* 加载中弹窗样式 */
.loading-modal {
  background: rgba(0, 30, 60, 0.85);
  border: 1px solid rgba(0, 180, 255, 0.3);
  border-radius: 10px;
  padding: 25px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  box-shadow: 0 0 30px rgba(0, 145, 234, 0.4);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 255, 255, 0.2);
  border-top: 3px solid #00ffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #fff;
  font-size: 14px;
  text-align: center;
}

/* 结果提示样式 */
.result-toast {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
  animation: fade-in 0.3s ease-out;
  z-index: 9999;
}

.result-toast.success {
  background-color: rgba(0, 200, 83, 0.9);
  border-left: 4px solid #00c853;
}

.result-toast.error {
  background-color: rgba(255, 82, 82, 0.9);
  border-left: 4px solid #ff5252;
}

.toast-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-size: 60%;
  background-position: center;
  background-repeat: no-repeat;
}

.success-icon {
  background-color: white;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%2300c853'%3E%3Cpath d='M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z'/%3E%3C/svg%3E");
}

.error-icon {
  background-color: white;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ff5252'%3E%3Cpath d='M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z'/%3E%3C/svg%3E");
}

.file-selection-area {
  margin: 15px 0;
}

.file-select-btn {
  background: linear-gradient(135deg, rgba(6, 82, 122, 0.6) 0%, rgba(4, 47, 79, 0.7) 100%);
  border: 1px solid rgba(14, 89, 134, 0.6);
  border-radius: 6px;
  padding: 10px 15px;
  color: #00ffff;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  width: 100%;
  justify-content: center;
}

.file-select-btn:hover {
  background: linear-gradient(135deg, rgba(6, 102, 142, 0.7) 0%, rgba(4, 67, 99, 0.8) 100%);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.3);
  transform: translateY(-2px);
}

.selected-file {
  margin-top: 10px;
  padding: 8px 12px;
  background: rgba(0, 30, 60, 0.5);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  word-break: break-all;
}

.model-info {
  background: rgba(0, 30, 60, 0.5);
  border: 1px solid rgba(14, 89, 134, 0.4);
  border-radius: 6px;
  padding: 12px;
  margin-top: 15px;
}

.model-info-item {
  margin-bottom: 8px;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
}

.model-info-item:last-child {
  margin-bottom: 0;
}

.model-info-label {
  color: rgba(255, 255, 255, 0.8);
}

.model-info-value {
  color: #00ffff;
  font-weight: 500;
}

.datetime-modal {
  max-width: 350px;
}

.export-modal {
  max-width: 450px;
}

.playback-confirm-modal {
  max-width: 450px;
}

/* 添加历史记录列表样式 */
.history-records-list {
  margin: 0px 0;
  background-color: rgba(0, 20, 40, 0.5);
  border-radius: 10px;
  border: 1px solid rgba(0, 183, 255, 0.3);
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 5;
  position: relative;
  width: 100%; /* 确保列表占满父容器宽度 */
}

.records-container {
  max-height: 120px; /* 调整为刚好显示2条记录的高度 */
  overflow-y: auto;
  padding: 8px 0;
  min-height: 80px;
  background-color: rgba(0, 20, 40, 0.3);
  width: 100%;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 183, 255, 0.5) rgba(0, 20, 40, 0.3);
}

.records-container::-webkit-scrollbar {
  width: 6px;
}

.records-container::-webkit-scrollbar-track {
  background: rgba(0, 20, 40, 0.3);
  border-radius: 3px;
}

.records-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 183, 255, 0.5);
  border-radius: 3px;
  transition: all 0.3s;
}

.records-container::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 183, 255, 0.7);
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(to right, rgba(0, 60, 120, 0.7), rgba(0, 40, 80, 0.7));
  border-bottom: 1px solid rgba(0, 183, 255, 0.3);
}

.records-title {
  font-size: 15px;
  font-weight: bold;
  color: #e1f2ff;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.5);
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 77, 79, 0.1);
  border: 1px solid rgba(255, 77, 79, 0.3);
  border-radius: 4px;
  padding: 4px 8px;
  color: #ff7e80;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
}

.clear-btn:hover {
  background: rgba(255, 77, 79, 0.2);
  box-shadow: 0 0 10px rgba(255, 77, 79, 0.3);
}

.clear-icon {
  display: inline-block;
  width: 12px;
  height: 12px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ff7e80'%3E%3Cpath d='M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z'/%3E%3C/svg%3E");
  background-size: contain;
}

.record-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 183, 255, 0.1);
  transition: all 0.3s ease;
  background-color: rgba(0, 30, 60, 0.3);
  width: 100%;
  box-sizing: border-box;
  height: 60px; /* 固定每条记录的高度 */
}

.record-item:last-child {
  border-bottom: none;
}

.record-item:hover {
  background-color: rgba(0, 145, 234, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.record-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 12px;
  background-size: 60%;
  background-position: center;
  background-repeat: no-repeat;
  flex-shrink: 0;
}

.export-glb-icon {
  background-color: rgba(0, 183, 255, 0.2);
  border: 1px solid rgba(0, 183, 255, 0.5);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%2300b7ff'%3E%3Cpath d='M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z'/%3E%3C/svg%3E");
}

.export-excel-icon {
  background-color: rgba(0, 200, 83, 0.2);
  border: 1px solid rgba(0, 200, 83, 0.5);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%2300c853'%3E%3Cpath d='M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z'/%3E%3C/svg%3E");
}

.playback-icon {
  background-color: rgba(255, 193, 7, 0.2);
  border: 1px solid rgba(255, 193, 7, 0.5);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ffc107'%3E%3Cpath d='M8 5v14l11-7z'/%3E%3C/svg%3E");
}

.record-content {
  flex: 1;
  overflow: hidden;
  min-width: 0; /* 关键添加：允许子元素在flex容器内收缩 */
  margin-right: 8px; /* 确保和操作按钮有间距 */
}

.record-title {
  font-size: 14px;
  font-weight: 500;
  color: #e1f2ff;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%; /* 确保不超出父容器 */
}

.record-time-range {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap; /* 防止时间范围换行 */
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0; /* 防止操作按钮被挤压 */
}

.record-action-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: rgba(0, 183, 255, 0.1);
  border: 1px solid rgba(0, 183, 255, 0.3);
}

.record-action-btn:hover {
  background-color: rgba(0, 183, 255, 0.2);
  transform: scale(1.1);
  box-shadow: 0 0 10px rgba(0, 183, 255, 0.4);
}

.repeat-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%2300b7ff'%3E%3Cpath d='M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 10h7V3l-2.35 3.35z'/%3E%3C/svg%3E");
  background-size: contain;
}

.records-footer {
  display: none;
}

.show-more-btn {
  background: transparent;
  border: none;
  color: #00b7ff;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
  transition: all 0.3s;
}

.show-more-btn:hover {
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.5);
  transform: translateY(-1px);
}

/* 添加无记录时的提示样式 */
.no-records-message {
  padding: 15px;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
  font-size: 14px;
}

/* 确保时间选择器和控制按钮区域之间有足够间距 */
.time-selector {
  margin-bottom: 16px;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translate(-50%, 20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

/* 调试按钮样式 */
.debug-controls {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.debug-btn {
  background: linear-gradient(135deg, #6b21a8, #4c1d95);
  border: 1px solid #7e22ce;
  border-radius: 4px;
  color: white;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.debug-btn:hover {
  background: linear-gradient(135deg, #7e22ce, #5b21b6);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(124, 58, 237, 0.5);
}
</style> 