<template>
  <div class="topics-container">
    <!-- 消息历史记录部分 -->
    <div class="message-history">
      <div class="filter-row">
        <el-form :inline="true" :model="messageQuery" class="message-filter">
          <el-form-item label="主题">
        <el-input
              v-model="messageQuery.topic" 
              placeholder="输入主题" 
              clearable
              class="topic-input"
            />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="messageQuery.timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              :default-time="defaultTime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :shortcuts="timeRangeShortcuts"
          clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleMessageSearch">查询</el-button>
            <el-button @click="resetMessageQuery">重置</el-button>
            <el-button 
              type="success" 
              @click="exportMessages" 
              :disabled="!canExport || loading.export"
              :loading="loading.export"
            >
              导出
            </el-button>
          </el-form-item>
        </el-form>
        
        <!-- 订阅控制按钮 -->
        <div class="subscription-controls">
          <el-button 
            type="primary" 
            @click="showConfigDialog" 
            :disabled="loading.any"
          >
            修改订阅
          </el-button>
          <el-button 
            :type="mqttConfig.isRunning ? 'danger' : 'success'" 
            @click="mqttConfig.isRunning ? stopSubscription() : startSubscription()"
            :loading="loading.start || loading.stop"
            :disabled="loading.any"
          >
            {{ mqttConfig.isRunning ? '停止订阅' : '开始订阅' }}
          </el-button>
          <el-button 
            type="info" 
            @click="refreshStatus" 
            :loading="loading.refresh"
            :disabled="loading.any"
          >
            <el-icon><Refresh /></el-icon>
            刷新状态
        </el-button>
          <el-tag :type="mqttConfig.isRunning ? 'success' : 'info'" class="status-tag">
            {{ mqttConfig.isRunning ? '正在订阅' : '未订阅' }}
          </el-tag>
      </div>
    </div>

    <el-table
        :data="messageList"
      style="width: 100%"
      class="data-table"
        v-loading="messageLoading"
        @row-click="handleRowClick"
      >
        <el-table-column prop="messageId" label="消息ID" width="100" />
        <el-table-column prop="topic" label="主题" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip>
        <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="showMessageDetail(row)"
            >
              查看详细内容
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="receivedTime" label="接收时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.receivedTime) }}
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
          v-model:current-page="messageQuery.pageNum"
          v-model:page-size="messageQuery.pageSize"
        :page-sizes="[10, 20, 50, 100]"
          :total="messageTotal"
        layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleMessageSizeChange"
          @current-change="handleMessageCurrentChange"
      />
      </div>
    </div>
    
    <!-- MQTT配置对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="MQTT配置"
      width="60%"
      :close-on-click-modal="false"
      :close-on-press-escape="!loading.save"
      :before-close="handleDialogClose"
    >
      <el-form :model="mqttConfig.currentConfig" label-width="100px" :disabled="mqttConfig.isRunning || loading.save">
        <el-form-item label="Broker URL" required>
          <el-input v-model="mqttConfig.currentConfig.brokerUrl" />
        </el-form-item>
        <el-form-item label="Client ID" required>
          <el-input v-model="mqttConfig.currentConfig.clientId" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="mqttConfig.currentConfig.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="mqttConfig.currentConfig.password" type="password" show-password />
        </el-form-item>
        
        <el-form-item label="主题配置" required>
          <div class="topics-list">
            <div v-for="(item, index) in mqttConfig.currentConfig.topics" :key="index" class="topic-item">
              <el-input v-model="item.topic" placeholder="主题" class="topic-input" />
              <el-select v-model="item.qos" placeholder="QoS" class="qos-select">
                <el-option :value="0" label="QoS 0" />
                <el-option :value="1" label="QoS 1" />
                <el-option :value="2" label="QoS 2" />
              </el-select>
              <el-button 
                type="danger" 
                icon="Delete" 
                circle 
                @click="removeTopic(index)" 
                v-if="mqttConfig.currentConfig.topics.length > 1"
              />
            </div>
            <el-button 
              type="primary" 
              @click="addTopic" 
              class="add-topic-btn"
            >
              添加主题
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <div class="button-group">
            <el-button @click="dialogVisible = false" :disabled="loading.save">取消</el-button>
            <el-button 
              type="primary" 
              @click="saveConfig" 
              :loading="loading.save"
              :disabled="mqttConfig.isRunning"
            >
              保存
            </el-button>
            
            <el-button
              type="info"
              @click="testConnection"
              :loading="loading.test"
              :disabled="loading.any"
            >
              测试连接
            </el-button>
          </div>
          
          <div v-if="mqttConfig.isRunning" class="warning-text">
            <el-alert
              title="当前正在订阅中，需要先停止订阅才能修改配置"
              type="warning"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 消息详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="消息详情"
      width="60%"
      :close-on-click-modal="false"
    >
      <div class="message-detail">
        <div class="detail-item">
          <span class="label">消息ID：</span>
          <span class="value">{{ currentMessage.messageId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">主题：</span>
          <span class="value">{{ currentMessage.topic }}</span>
        </div>
        <div class="detail-item">
          <span class="label">接收时间：</span>
          <span class="value">{{ formatTime(currentMessage.receivedTime) }}</span>
        </div>
        <div class="detail-item content">
          <span class="label">内容：</span>
          <pre class="value json-content">{{ formatJsonContent(currentMessage.content) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: '/', // 使用vite代理
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 在请求头中添加token等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 如果是二进制文件，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    
    const res = response.data
    if (res.status === 'success') {
      return res
    } else {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  error => {
    console.error('响应错误:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// MQTT API方法
const mqttApi = {
  // 获取MQTT配置和状态
  getConfig() {
    return service.get('/system/mqtt')
  },

  // 开始订阅
  startSubscription(config) {
    return service.get('/system/mqtt/start')
  },

  // 停止订阅
  stopSubscription() {
    return service.get('/system/mqtt/stop')
  },

  // 保存配置
  saveConfig(config) {
    return service.post('/system/mqtt', config)
  },

  // 获取消息历史记录
  getMessageHistory(params) {
    return service.get('/system/mqtt/list', { params })
  },
  
  // 测试连接
  testConnection(config) {
    return service.post('/system/mqtt/testConnection', config)
  },
  
  // 导出消息
  exportMessages(params) {
    return service.get('/system/mqtt/export', {
      params,
      responseType: 'blob',
      headers: {
        'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      },
      timeout: 60000 // 导出请求单独设置60秒超时
    })
  }
}

// 主题数据
const topics = ref([])

// MQTT配置和状态
const mqttConfig = reactive({
  isRunning: false,
  currentConfig: {
    brokerUrl: '',
    clientId: '',
    username: '',
    password: '',
    topics: [
      { topic: '', qos: 2 }
    ]
  }
})

// 加载状态
const loading = reactive({
  start: false,
  stop: false,
  save: false,
  refresh: false,
  test: false,
  export: false,
  get any() {
    return this.start || this.stop || this.save || this.refresh || this.test || this.export;
  }
})

// 消息历史记录相关
const messageQuery = ref({
  topic: '',
  timeRange: [],
  pageNum: 1,
  pageSize: 10
})

const defaultTime = [
  new Date(2000, 1, 1, 0, 0, 0),
  new Date(2000, 1, 1, 23, 59, 59),
]

// 时间范围快捷选项
const timeRangeShortcuts = [
  {
    text: '最近24小时',
    value: () => {
      const end = new Date()
      const start = new Date(end.getTime() - 24 * 60 * 60 * 1000)
      return [start, end]
    },
  },
  {
    text: '最近5分钟',
    value: () => {
      const end = new Date()
      const start = new Date(end.getTime() - 5 * 60 * 1000)
      return [start, end]
    },
  },
  {
    text: '最近1小时',
    value: () => {
      const end = new Date()
      const start = new Date(end.getTime() - 60 * 60 * 1000)
      return [start, end]
    },
  },
  {
    text: '今天',
    value: () => {
      const end = new Date()
      const start = new Date(end.setHours(0, 0, 0, 0))
      return [start, new Date()]
    },
  },
]

const messageList = ref([])
const messageTotal = ref(0)
const messageLoading = ref(false)

// 对话框控制
const dialogVisible = ref(false)

// 消息详情对话框控制
const detailDialogVisible = ref(false)
const currentMessage = ref({})

// 显示配置对话框
const showConfigDialog = () => {
  dialogVisible.value = true
}

// 处理对话框关闭
const handleDialogClose = (done) => {
  if (loading.save) {
    ElMessage.warning('正在保存，请稍候')
    return
  }
  done()
}

// 设置默认时间范围为当前24小时内
const setDefaultTimeRange = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 24 * 60 * 60 * 1000) // 24小时前
  
  // 格式化日期为字符串
  const formatDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }
  
  messageQuery.value.timeRange = [formatDate(start), formatDate(end)]
}

// 格式化时间戳
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(Number(timestamp))
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-')
}

// 查询消息历史
const handleMessageSearch = async () => {
  if (!messageQuery.value.timeRange || messageQuery.value.timeRange.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  messageLoading.value = true
  try {
    const params = {
      topic: messageQuery.value.topic,
      startTime: new Date(messageQuery.value.timeRange[0]).getTime(),
      endTime: new Date(messageQuery.value.timeRange[1]).getTime(),
      pageNum: messageQuery.value.pageNum,
      pageSize: messageQuery.value.pageSize
    }

    const res = await mqttApi.getMessageHistory(params)
    messageList.value = res.rows
    messageTotal.value = res.total
  } catch (error) {
    console.error('查询消息历史失败:', error)
  } finally {
    messageLoading.value = false
  }
}

// 重置查询条件
const resetMessageQuery = () => {
  messageQuery.value.topic = ''
  setDefaultTimeRange()
  messageQuery.value.pageNum = 1
  handleMessageSearch()
}

// 处理分页大小变化
const handleMessageSizeChange = (val) => {
  messageQuery.value.pageSize = val
  handleMessageSearch()
}

// 处理页码变化
const handleMessageCurrentChange = (val) => {
  messageQuery.value.pageNum = val
  handleMessageSearch()
}

// 添加主题
const addTopic = () => {
  mqttConfig.currentConfig.topics.push({ topic: '', qos: 2 })
}

// 移除主题
const removeTopic = (index) => {
  mqttConfig.currentConfig.topics.splice(index, 1)
}

// 获取MQTT配置
const fetchMqttConfig = async () => {
  try {
    const res = await mqttApi.getConfig()
    mqttConfig.currentConfig = res.currentConfig
    mqttConfig.isRunning = res.isRunning
    // 更新主题下拉列表
    topics.value = mqttConfig.currentConfig.topics.map(item => ({
      code: item.topic,
      name: item.topic
    }))
  } catch (error) {
    console.error('获取MQTT配置失败:', error)
  }
}

// 开始订阅
const startSubscription = async () => {
  try {
    loading.start = true
    
    // 确认提示
    await ElMessageBox.confirm(
      '确定要开始订阅吗？这将使用当前配置连接到MQTT broker。',
      '开始订阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 调用后端API
    await mqttApi.startSubscription(mqttConfig.currentConfig)
    ElMessage.success('订阅成功')
    mqttConfig.isRunning = true
  } catch (error) {
    if (error !== 'cancel') {
      console.error('开始订阅失败:', error)
    }
  } finally {
    loading.start = false
  }
}

// 停止订阅
const stopSubscription = async () => {
  try {
    loading.stop = true
    
    // 确认提示
    await ElMessageBox.confirm(
      '确定要停止订阅吗？这将断开与MQTT broker的连接。',
      '停止订阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用后端API
    await mqttApi.stopSubscription()
    ElMessage.success('已停止订阅')
    mqttConfig.isRunning = false
  } catch (error) {
    if (error !== 'cancel') {
      console.error('停止订阅失败:', error)
    }
  } finally {
    loading.stop = false
  }
}

// 保存配置
const saveConfig = async () => {
  // 验证配置
  if (!mqttConfig.currentConfig.brokerUrl) {
    ElMessage.warning('请输入Broker URL')
    return
  }
  
  if (!mqttConfig.currentConfig.clientId) {
    ElMessage.warning('请输入Client ID')
    return
  }
  
  if (mqttConfig.currentConfig.topics.some(t => !t.topic)) {
    ElMessage.warning('请填写所有主题')
    return
  }
  
  try {
    loading.save = true
    
    // 确认提示
    await ElMessageBox.confirm(
      '确定要保存配置吗？',
      '保存配置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 调用后端API
    await mqttApi.saveConfig(mqttConfig.currentConfig)
    ElMessage.success('配置已保存')
    
    // 更新主题下拉列表
    topics.value = mqttConfig.currentConfig.topics.map(item => ({
      code: item.topic,
      name: item.topic
    }))
    
    // 关闭对话框
    dialogVisible.value = false
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存配置失败:', error)
    }
  } finally {
    loading.save = false
  }
}

// 刷新状态
const refreshStatus = async () => {
  try {
    loading.refresh = true
    await fetchMqttConfig()
  } catch (error) {
    console.error('刷新状态失败:', error)
    ElMessage.error('刷新状态失败')
  } finally {
    loading.refresh = false
  }
}

// 显示消息详情
const showMessageDetail = (message) => {
  currentMessage.value = message
  detailDialogVisible.value = true
}

// 格式化JSON内容
const formatJsonContent = (content) => {
  if (!content) return ''
  try {
    // 尝试解析JSON
    const parsed = JSON.parse(content)
    // 格式化JSON，使用2个空格缩进
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    // 如果不是JSON格式，直接返回原内容
    return content
  }
}

// 测试连接
const testConnection = async () => {
  // 验证配置
  if (!mqttConfig.currentConfig.brokerUrl) {
    ElMessage.warning('请输入Broker URL')
    return
  }
  
  if (!mqttConfig.currentConfig.clientId) {
    ElMessage.warning('请输入Client ID')
    return
  }
  
  if (mqttConfig.currentConfig.topics.some(t => !t.topic)) {
    ElMessage.warning('请填写所有主题')
    return
  }
  
  try {
    loading.test = true
    // 调用测试连接API
    const res = await mqttApi.testConnection(mqttConfig.currentConfig)
    // 显示测试结果
    if (res.status === 'success') {
      ElMessage.success(res.msg || '连接测试成功')
    } else {
      ElMessage.error(res.msg || '连接测试失败')
    }
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error('测试连接失败：' + (error.message || '未知错误'))
  } finally {
    loading.test = false
  }
}

// 计算属性：是否可以导出
const canExport = computed(() => {
  return messageQuery.value.timeRange && 
         messageQuery.value.timeRange.length === 2 && 
         !messageLoading.value;
})

// 导出消息
const exportMessages = async () => {
  if (!messageQuery.value.timeRange || messageQuery.value.timeRange.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  // 声明在外部，确保在finally中能关闭
  let loadingInstance = null
  
  try {
    loading.export = true
    
    // 确认提示
    await ElMessageBox.confirm(
      '确定要导出消息列表吗？\n一次最多导出一万条数据。',
      '导出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 显示加载中提示
    loadingInstance = ElMessage({
      type: 'info',
      message: '正在导出数据，请耐心等待...',
      duration: 0
    })
    
    const params = {
      topic: messageQuery.value.topic || undefined,
      startTime: new Date(messageQuery.value.timeRange[0]).getTime(),
      endTime: new Date(messageQuery.value.timeRange[1]).getTime()
    }
    
    // 第一次请求：获取文件路径
    const exportResponse = await service.get('/system/mqtt/export', { params })
    
    if (exportResponse.status === 'success' && exportResponse.filepath) {
      // 第二次请求：下载文件
      const downloadResponse = await service.get('/api' + exportResponse.filepath, {
        responseType: 'blob',
        headers: {
          'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }
      })
      
      // 处理文件下载
      const contentType = downloadResponse.headers['content-type']
      const blob = new Blob([downloadResponse.data], { 
        type: contentType || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
      })
      
      // 从文件路径中提取文件名
      const filename = exportResponse.filepath.split('/').pop()
      
      // 创建下载链接
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
      
      ElMessage.success('导出成功')
    } else {
      ElMessage.error(exportResponse.msg || '导出失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('导出失败:', error)
      ElMessage.error('导出失败：' + (error.message || '未知错误'))
    }
  } finally {
    // 无论成功还是失败，都关闭加载提示
    if (loadingInstance) {
      loadingInstance.close()
    }
    loading.export = false
  }
}

// 组件挂载时设置默认时间范围并查询
onMounted(() => {
  fetchMqttConfig()
  setDefaultTimeRange()
  handleMessageSearch()
  
  // 每30秒自动刷新一次状态
  setInterval(refreshStatus, 30000)
})

// 处理表格行点击
const handleRowClick = (row, column, event) => {
  // 找到点击的行元素
  const rowEl = event.target.closest('tr')
  if (rowEl) {
    // 添加动画类
    rowEl.classList.add('row-active')
    // 动画结束后移除类
    setTimeout(() => {
      rowEl.classList.remove('row-active')
    }, 1500) // 与动画时长一致
  }
  
  // 如果是内容列，并且点击的不是按钮，则显示详情
  if (column.property === 'content' && !event.target.closest('button')) {
    showMessageDetail(row)
  }
}
</script>

<style scoped>
.topics-container {
  padding: 24px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 12px;
  border: 1px solid #0e5986;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(5px);
}

.message-history {
  margin-top: 20px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.message-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.subscription-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tag {
  margin-left: 10px;
  padding: 0 12px;
  height: 28px;
  line-height: 28px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 0;
}

.topic-input {
  width: 100%;
}

.topic-input :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.topic-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.topic-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

.topic-input :deep(.el-input__inner) {
  color: white;
}

.data-table {
  --el-table-border-color: rgba(14, 89, 134, 0.5);
  --el-table-header-bg-color: rgba(0, 40, 80, 0.6);
  --el-table-row-hover-bg-color: rgba(0, 145, 234, 0.2);
  --el-table-header-text-color: #ffffff;
  --el-table-text-color: #e1f2ff;
  margin-bottom: 24px;
  background-color: rgba(0, 20, 40, 0.4);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3), 0 0 20px rgba(0, 100, 234, 0.1);
  transition: all 0.3s ease;
}

.data-table:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.15);
}

/* 添加表格内部线条 */
.data-table :deep(.el-table__inner-wrapper) {
  border: 1px solid #0e5986;
  border-radius: 10px;
  overflow: hidden;
}

.data-table :deep(.el-table__cell) {
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
  padding: 12px 8px;
}

.data-table :deep(.el-table__column-filter-trigger) {
  margin-left: 8px;
}

.data-table :deep(.el-table__header) {
  background-color: rgba(0, 40, 80, 0.7);
  background-image: linear-gradient(to right, rgba(0, 60, 120, 0.7), rgba(0, 40, 80, 0.7));
}

.data-table :deep(.el-table__header th) {
  padding: 16px 12px;
  font-weight: bold;
  letter-spacing: 0.5px;
  color: #e1f2ff;
  background: rgba(0, 40, 80, 0.6);
  text-align: center;
  height: 60px;
  position: relative;
  background-image: linear-gradient(to bottom, rgba(0, 70, 140, 0.8), rgba(0, 40, 80, 0.6));
  transition: all 0.3s ease;
  overflow: hidden;
}

.data-table :deep(.el-table__header th::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(to right, transparent, rgba(0, 224, 255, 0.7), transparent);
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.data-table :deep(.el-table__header th:hover::after) {
  transform: translateY(0);
}

.data-table :deep(.el-table__header th:hover) {
  background-image: linear-gradient(to bottom, rgba(0, 80, 160, 0.9), rgba(0, 50, 100, 0.7));
  color: #ffffff;
}

.data-table :deep(.el-table__header th .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  font-size: 15px;
  text-shadow: 0 0 5px rgba(0, 224, 255, 0.3);
}

.data-table :deep(.el-table__header-wrapper) {
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table :deep(.el-table__row) {
  background-color: rgba(0, 30, 60, 0.3);
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
}

.data-table :deep(.el-table__row td) {
  border-right: 1px solid rgba(14, 89, 134, 0.2);
  text-align: center;
  padding: 14px 12px;
  height: 55px;
}

.data-table :deep(.el-table__row td .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.data-table :deep(.el-table__row:nth-child(even)) {
  background-color: rgba(0, 35, 70, 0.3);
}

.data-table :deep(.el-table__row:hover) {
  background-color: rgba(0, 145, 234, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.data-table :deep(.el-table__row.current-row) {
  background-color: rgba(0, 145, 234, 0.3) !important;
  transform: translateY(-2px);
  box-shadow: 0 0 15px rgba(0, 145, 234, 0.3);
  border-left: 3px solid #00e0ff;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
}

.data-table :deep(.el-table__row.current-row::after) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(0, 224, 255, 0.1), transparent);
  pointer-events: none;
  z-index: -1;
}

.data-table :deep(.el-table__row.current-row td) {
  color: #fff;
  text-shadow: 0 0 5px rgba(0, 224, 255, 0.3);
}

/* 增强选中框样式 */
.data-table :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: linear-gradient(135deg, #00a1ff, #0077ff);
  border-color: #0091ff;
  box-shadow: 0 0 8px rgba(0, 145, 255, 0.5);
  transform: scale(1.1);
  transition: all 0.3s ease;
}

/* 点击表格行时的动画效果 */
@keyframes rowPulse {
  0% {
    box-shadow: 0 0 0 rgba(0, 224, 255, 0);
  }
  50% {
    box-shadow: 0 0 15px rgba(0, 224, 255, 0.5);
  }
  100% {
    box-shadow: 0 0 0 rgba(0, 224, 255, 0);
  }
}

.data-table :deep(.el-table__row.row-active) {
  animation: rowPulse 1.5s ease-in-out;
}

.data-table :deep(.el-table__fixed-right),
.data-table :deep(.el-table__fixed-left) {
  background-color: rgba(0, 30, 60, 0.4);
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
}

.data-table :deep(.el-table__fixed-right::before),
.data-table :deep(.el-table__fixed::before) {
  background-color: rgba(14, 89, 134, 0.5);
}

/* 表格空数据样式 */
.data-table :deep(.el-table__empty-block) {
  background: rgba(0, 30, 60, 0.3);
  min-height: 150px;
}

.data-table :deep(.el-table__empty-text) {
  color: #a0cfff;
  font-size: 15px;
  background: rgba(0, 40, 80, 0.5);
  padding: 15px 30px;
  border-radius: 8px;
  border: 1px dashed rgba(14, 89, 134, 0.7);
}

/* 表格加载动画样式 */
.data-table :deep(.el-loading-mask) {
  background-color: rgba(0, 20, 40, 0.7);
  backdrop-filter: blur(5px);
}

.data-table :deep(.el-loading-spinner .path) {
  stroke: #00e0ff;
  stroke-width: 3;
}

.data-table :deep(.el-loading-text) {
  color: #00e0ff;
  font-size: 15px;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 224, 255, 0.5);
  margin-top: 10px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 30px;
  background: rgba(0, 30, 60, 0.3);
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.pagination :deep(.el-pagination) {
  padding: 0;
  margin: 0;
  font-weight: normal;
  color: #a0cfff;
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: rgba(0, 40, 80, 0.6);
  --el-pagination-hover-color: #00ffff;
  --el-pagination-font-size: 14px;
}

.pagination :deep(.el-pagination .el-pagination__total) {
  font-size: 14px;
  color: #a0cfff;
}

.pagination :deep(.el-pagination .el-pagination__jump) {
  color: #a0cfff;
}

.pagination :deep(.el-pagination .el-input__inner) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: #e1f2ff;
  border-radius: 4px;
}

.pagination :deep(.el-pagination .el-select .el-input) {
  width: 110px;
}

.pagination :deep(.el-pagination .el-select-dropdown__item) {
  color: #a0cfff;
}

.pagination :deep(.el-pagination .btn-prev),
.pagination :deep(.el-pagination .btn-next) {
  background-color: rgba(0, 50, 100, 0.5);
  border-radius: 4px;
  color: #a0cfff;
  transition: all 0.3s ease;
}

.pagination :deep(.el-pagination .btn-prev:hover),
.pagination :deep(.el-pagination .btn-next:hover) {
  background-color: rgba(0, 90, 180, 0.6);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

.pagination :deep(.el-pagination .el-pager li) {
  background-color: rgba(0, 50, 100, 0.5);
  color: #a0cfff;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.pagination :deep(.el-pagination .el-pager li:hover) {
  background-color: rgba(0, 90, 180, 0.6);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

.pagination :deep(.el-pagination .el-pager li.is-active) {
  background-color: rgba(0, 183, 255, 0.7);
  color: white;
  font-weight: bold;
  box-shadow: 0 2px 10px rgba(0, 183, 255, 0.4);
}

/* 按钮样式 */
:deep(.el-button) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  border: none;
}

:deep(.el-button)::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, transparent 60%);
  opacity: 0;
  transform: scale(0.5);
  transition: all 0.4s ease;
}

:deep(.el-button:hover)::after {
  opacity: 1;
  transform: scale(1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #0091ff, #006be6);
  box-shadow: 0 4px 10px rgba(0, 130, 255, 0.3);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #00a1ff, #0077ff);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 145, 255, 0.4);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #ff9500, #e67e00);
  box-shadow: 0 4px 10px rgba(255, 149, 0, 0.3);
}

:deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, #ffa520, #ff8800);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 149, 0, 0.4);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #00c48c, #00a170);
  box-shadow: 0 4px 10px rgba(0, 196, 140, 0.3);
}

:deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #00d49a, #00b180);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 196, 140, 0.4);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff5252, #e63939);
  box-shadow: 0 4px 10px rgba(255, 82, 82, 0.3);
}

:deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #ff6b6b, #ff4747);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 82, 82, 0.4);
}

:deep(.el-button--info) {
  background: linear-gradient(135deg, #909399, #6b6d71);
  box-shadow: 0 4px 10px rgba(144, 147, 153, 0.3);
}

:deep(.el-button--info:hover) {
  background: linear-gradient(135deg, #9c9fa6, #777a7f);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(144, 147, 153, 0.4);
}

:deep(.el-button--plain.el-button--primary) {
  background: transparent;
  color: #0091ff;
  border: 1px solid #0091ff;
}

:deep(.el-button--plain.el-button--primary:hover) {
  background: rgba(0, 145, 255, 0.1);
  color: #00b7ff;
  border-color: #00b7ff;
  box-shadow: 0 4px 10px rgba(0, 145, 255, 0.2);
}

:deep(.el-button--link) {
  cursor: pointer;
  padding: 0;
  color: #00b7ff;
  transition: all 0.3s ease;
}

:deep(.el-button--link:hover) {
  color: #54d8ff;
  text-shadow: 0 0 5px rgba(0, 183, 255, 0.5);
  transform: translateY(-1px);
}

/* 日期选择器样式 */
:deep(.el-date-editor) {
  --el-fill-color-blank: rgba(0, 20, 40, 0.7);
  --el-text-color-regular: #e1f2ff;
  --el-border-color: rgba(14, 89, 134, 0.7);
  border-radius: 6px;
  overflow: hidden;
}

:deep(.el-date-editor .el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
}

:deep(.el-date-editor .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

:deep(.el-date-editor .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
}

.topics-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(0, 20, 40, 0.5);
  padding: 16px;
  border-radius: 8px;
  border: 1px solid rgba(14, 89, 134, 0.3);
}

.topic-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qos-select {
  width: 100px;
}

.qos-select :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
}

.qos-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.add-topic-btn {
  margin-top: 12px;
  align-self: flex-start;
}

.warning-text {
  margin-top: 15px;
}

:deep(.el-dialog) {
  background-color: rgba(0, 20, 40, 0.95);
  border: 1px solid #0e5986;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(10px);
  overflow: visible;
  margin: 5vh auto !important;
}

:deep(.el-dialog__header) {
  background: linear-gradient(90deg, rgba(0, 60, 120, 0.8), rgba(0, 40, 80, 0.8));
  color: #e1f2ff;
  border-bottom: 1px solid rgba(0, 183, 255, 0.3);
  padding: 20px 24px;
  margin: 0 !important;
  position: relative;
}

:deep(.el-dialog__title) {
  color: #e1f2ff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.5);
}

:deep(.el-dialog__headerbtn) {
  top: 18px;
  right: 20px;
}

:deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #00ffff;
  transform: rotate(90deg);
  transition: all 0.3s ease;
}

:deep(.el-dialog__body) {
  padding: 24px;
  color: #a0cfff;
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(14, 89, 134, 0.5);
  background: rgba(0, 20, 40, 0.5);
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.message-detail {
  padding: 24px;
  background-color: rgba(0, 20, 40, 0.5);
  border-radius: 8px;
  border: 1px solid rgba(14, 89, 134, 0.5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.detail-item {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.detail-item .label {
  width: 100px;
  color: #00ffff;
  font-weight: bold;
  flex-shrink: 0;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.detail-item .value {
  flex: 1;
  color: #e1f2ff;
  word-break: break-all;
  margin-left: 16px;
}

.detail-item.content {
  flex-direction: column;
}

.detail-item.content .label {
  margin-bottom: 12px;
}

.detail-item.content .value {
  margin-left: 0;
  width: 100%;
}

.json-content {
  margin-top: 12px;
  padding: 20px;
  background-color: rgba(0, 30, 60, 0.5);
  border-radius: 8px;
  white-space: pre-wrap;
  font-family: "Consolas", "Monaco", monospace;
  max-height: 400px;
  overflow-y: auto;
  width: 100%;
  box-sizing: border-box;
  color: #e1f2ff;
  border: 1px solid rgba(14, 89, 134, 0.5);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.3);
  font-size: 14px;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.button-group {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  justify-content: center;
}

/* 表单样式 */
:deep(.el-form-item__label) {
  color: #e1f2ff;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.3);
}

:deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

:deep(.el-input__inner) {
  color: white;
}

@media (max-width: 768px) {
  .topics-container {
    padding: 16px;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .subscription-controls {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}

/* 新增元素进入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.data-table {
  animation: fadeIn 0.3s ease-out;
}

.message-detail {
  animation: fadeIn 0.3s ease-out;
}
</style> 