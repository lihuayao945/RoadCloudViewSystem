<template>
  <div class="rcu-objects-container">
    <div class="filter-row">
      <div class="filter-item search-area">
        <el-input
          v-model="rcuId"
          placeholder="输入RCU ID进行搜索..."
          class="filter-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-button type="primary" size="default" @click="fetchData">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button type="warning" size="default" @click="fetchAllRcus" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="action-row">
      <div class="table-actions">
        <el-button 
          type="success" 
          size="small" 
          @click="handleExport" 
          :loading="exporting" 
          :disabled="!multipleSelection.length && !rcuId"
        >
          <el-icon><Download /></el-icon>
          导出{{ multipleSelection.length ? ' (' + multipleSelection.length + ')' : '' }}
        </el-button>
        <el-button 
          type="danger" 
          size="small" 
          @click="handleBatchDelete" 
          :disabled="!multipleSelection.length"
        >
          <el-icon><Delete /></el-icon>
          删除{{ multipleSelection.length ? ' (' + multipleSelection.length + ')' : '' }}
        </el-button>
      </div>
    </div>

    <!-- RCU设备列表 -->
    <el-table
      :data="filteredDevices"
      style="width: 100%"
      class="data-table"
      v-loading="loading"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="rcuId" label="RCU ID" width="150" />
      <el-table-column prop="deviceType" label="设备类型" width="100">
        <template #default="{ row }">
          {{ getDeviceTypeName(row.deviceType || 0) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'online' ? 'success' : 'danger'">
            {{ row.status === 'online' ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timestampOfDetOut" label="最后更新时间">
        <template #default="{ row }">
          {{ formatTimestamp(row.timestampOfDetOut) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button size="small" @click="handleViewMessages(row)">
              <el-icon><View /></el-icon>
              查看消息
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="rcuDevices.length === 0 && !loading" class="empty-data">
      <el-empty description="暂无RCU设备数据" />
    </div>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 优化: RCU消息列表弹窗 -->
    <el-dialog
      v-model="messagesDialogVisible"
      :title="`RCU消息列表 - ${currentRcuId}`"
      width="90%"
      destroy-on-close
      class="custom-dialog"
    >
      <div class="dialog-filter-row">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="x"
          :default-time="defaultTime"
          :shortcuts="timeShortcuts"
          @change="fetchRcuMessages"
          style="width: 100%;"
        />
        <el-button type="primary" @click="fetchRcuMessages" :loading="messagesLoading">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <div class="dialog-content">
        <el-table
          :data="rcuMessages"
          style="width: 100%"
          v-loading="messagesLoading"
          class="data-table message-table"
          @selection-change="handleMessageSelectionChange"
          height="400px"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" width="55" fixed="left" />
          <el-table-column prop="objsflag" label="消息标识" min-width="220" show-overflow-tooltip fixed="left" />
          <el-table-column prop="rcuId" label="RCU ID" min-width="120" />
          <el-table-column prop="deviceType" label="设备类型" min-width="120">
            <template #default="{ row }">
              {{ getDeviceTypeName(row.deviceType || 0) }}
            </template>
          </el-table-column>
          <el-table-column prop="timestampOfDetOut" label="感知时间" min-width="180">
            <template #default="{ row }">
              {{ formatTimestamp(row.timestampOfDetOut) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" @click="handleViewMessageObjects(row)">
                  <el-icon><View /></el-icon>
                  查看对象列表
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="rcuMessages.length === 0 && !messagesLoading" class="empty-data">
          <el-empty description="暂无消息数据" />
        </div>

        <div class="dialog-pagination">
          <el-pagination
            v-model:current-page="messagesPageNum"
            v-model:page-size="messagesPageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="messagesTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="handleMessagesSizeChange"
            @current-change="handleMessagesCurrentChange"
          />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button 
            type="success" 
            @click="handleExportMessages" 
            :disabled="!selectedMessages.length"
          >
            导出选中{{ selectedMessages.length ? ' (' + selectedMessages.length + ')' : '' }}
          </el-button>
          <el-button 
            type="danger" 
            @click="handleBatchDeleteMessages" 
            :disabled="!selectedMessages.length"
          >
            批量删除{{ selectedMessages.length ? ' (' + selectedMessages.length + ')' : '' }}
          </el-button>
          <el-button @click="messagesDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 优化: 消息对象列表弹窗 -->
    <el-dialog
      v-model="objectsListDialogVisible"
      :title="`消息对象列表 - ${currentMessageId}`"
      width="90%"
      destroy-on-close
      append-to-body
      class="custom-dialog objects-dialog"
    >
      <div class="dialog-filter-row">
        <el-input
          v-model="objectSearchQuery"
          placeholder="搜索对象..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="warning" @click="fetchMessageObjects" :loading="objectsLoading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>

      <div class="dialog-content">
        <el-table
          :data="filteredObjects"
          style="width: 100%"
          v-loading="objectsLoading"
          class="data-table objects-table"
          @selection-change="handleObjectSelectionChange"
          height="400px"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" width="55" fixed="left" />
          <el-table-column prop="objId" label="对象ID" min-width="80" fixed="left">
            <template #default="{ row }">
              {{ row.objId !== undefined ? row.objId : '未知ID' }}
            </template>
          </el-table-column>
          <el-table-column prop="type" label="对象类型" min-width="120">
            <template #default="{ row }">
              {{ getObjectTypeName(row.type || '0') }}
            </template>
          </el-table-column>
          <el-table-column prop="speed" label="速度" min-width="80" />
          <el-table-column prop="longitude" label="经度" min-width="120" />
          <el-table-column prop="latitude" label="纬度" min-width="120" />
          <el-table-column prop="plateNo" label="车牌号" min-width="120">
            <template #default="{ row }">
              {{ row.plateNo || '无' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" @click="handleViewObjectDetails(row)">
                  <el-icon><View /></el-icon>
                  查看详情
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="messageObjects.length === 0 && !objectsLoading" class="empty-data">
          <el-empty description="暂无对象数据" />
        </div>

        <div class="dialog-pagination">
          <el-pagination
            v-model:current-page="objectsPageNum"
            v-model:page-size="objectsPageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="objectsTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="handleObjectsSizeChange"
            @current-change="handleObjectsCurrentChange"
          />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button 
            type="success" 
            @click="handleExportObjects" 
            :disabled="!selectedObjects.length"
          >
            导出选中{{ selectedObjects.length ? ' (' + selectedObjects.length + ')' : '' }}
          </el-button>
          <el-button 
            type="danger" 
            @click="handleBatchDeleteObjects" 
            :disabled="!selectedObjects.length"
          >
            批量删除{{ selectedObjects.length ? ' (' + selectedObjects.length + ')' : '' }}
          </el-button>
          <el-button @click="objectsListDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 具体对象详情弹窗 -->
    <el-dialog
      v-model="objectDetailDialogVisible"
      :title="`对象详细信息 - ${currentObjectId}`"
      width="50%"
      append-to-body
      destroy-on-close
      class="object-details-dialog"
    >
      <div v-if="currentObject" class="object-details">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="UUID">{{ currentObject.uuid || '-' }}</el-descriptions-item>
          <el-descriptions-item label="消息标识">{{ currentObject.objsflag || currentMessageId }}</el-descriptions-item>
          <el-descriptions-item label="对象ID">{{ currentObject.objId !== undefined ? currentObject.objId : '未知ID' }}</el-descriptions-item>
          <el-descriptions-item label="对象类型">{{ getObjectTypeName(currentObject.type || '0') }}</el-descriptions-item>
          <el-descriptions-item label="速度">{{ currentObject.speed || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ currentObject.longitude || '-' }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ currentObject.latitude || '-' }}</el-descriptions-item>
          <el-descriptions-item label="海拔">{{ currentObject.elevation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="朝向">{{ currentObject.heading || '-' }}</el-descriptions-item>
          <el-descriptions-item label="车牌号">{{ currentObject.plateNo || '无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="objectDetailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { 
  Search, 
  Download, 
  Refresh, 
  View,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

// 配置axios
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || ''; // 从环境变量获取基础URL
axios.defaults.timeout = 60000; // 60秒超时

// 查询参数
const rcuId = ref('')
const timeRange = ref([])
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
]
const timeShortcuts = [
  {
    text: '最近1小时',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000)
      return [start, end]
    },
  },
  {
    text: '最近6小时',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 6)
      return [start, end]
    },
  },
  {
    text: '最近24小时',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24)
      return [start, end]
    },
  },
]

// 表格数据 - 第一层：RCU设备列表
const rcuDevices = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const exporting = ref(false)
const multipleSelection = ref([])

// 第二层：RCU消息列表
const messagesDialogVisible = ref(false)
const rcuMessages = ref([])
const messagesLoading = ref(false)
const messagesPageNum = ref(1)
const messagesPageSize = ref(10)
const messagesTotal = ref(0)
const currentRcuId = ref('')
const selectedMessages = ref([])

// 第三层：消息对象列表
const objectsListDialogVisible = ref(false)
const messageObjects = ref([])
const objectsLoading = ref(false)
const objectsPageNum = ref(1)
const objectsPageSize = ref(10)
const objectsTotal = ref(0)
const currentMessageId = ref('')
const selectedObjects = ref([])
const objectSearchQuery = ref('')

// 第四层：单个对象详情
const objectDetailDialogVisible = ref(false)
const currentObject = ref(null)
const currentObjectId = ref('')

// 过滤RCU设备列表
const filteredDevices = computed(() => {
  return rcuDevices.value
})

// 过滤对象列表
const filteredObjects = computed(() => {
  if (!objectSearchQuery.value) return messageObjects.value;
  const query = objectSearchQuery.value.toLowerCase();
  return messageObjects.value.filter(obj => 
    (obj.objId !== undefined && obj.objId.toString().includes(query)) ||
    (obj.plateNo && obj.plateNo.toLowerCase().includes(query)) ||
    (getObjectTypeName(obj.type || '0').toLowerCase().includes(query))
  );
})

// 设备类型映射
const getDeviceTypeName = (type) => {
  const typeMap = {
    0: '未知',
    1: '融合设备',
    2: '摄像头',
    3: '毫米波雷达',
    4: '激光雷达',
    // 5～255预留
  }
  return typeMap[type] || `预留(${type})`
}

// 对象类型映射
const getObjectTypeName = (type) => {
  const typeMap = {
    1: '小型车',
    2: '大型车',
    3: '公交车',
    4: '卡车',
    5: '特种车辆',
    6: '摩托车',
    7: '行人',
    8: '非机动车',
    // 可以继续添加其他类型
  }
  return typeMap[type] || `未知(${type})`
}

// 时间戳格式化
const formatTimestamp = (timestamp) => {
  if (!timestamp) return ''
  try {
    const date = new Date(Number(timestamp))
    if (isNaN(date.getTime())) return timestamp // 如果无法解析为日期，则返回原始字符串
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).replace(/\//g, '-')
  } catch (error) {
    return timestamp // 发生错误返回原始字符串
  }
}

// 初始化
onMounted(() => {
  // 设置默认时间范围为最近24小时
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 3600 * 1000 * 24)
  timeRange.value = [start.getTime(), end.getTime()]
  
  // 首先获取全部RCU设备列表
  fetchAllRcus()
})

// 获取全部RCU设备列表 - 使用 /system/rcu/list 接口
const fetchAllRcus = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    const response = await axios.get('/system/rcu/list', { params })
    
    if (response.data.status === 'success') {
      rcuDevices.value = response.data.rows || []
      // 为设备添加默认的在线状态
      rcuDevices.value = rcuDevices.value.map(device => ({
        ...device,
        status: 'online' // 默认设置为在线
      }))
      total.value = response.data.total || 0
    } else {
      ElMessage.warning('获取RCU设备列表失败: ' + (response.data.msg || '未知错误'))
      rcuDevices.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取数据出错详情:', error)
    ElMessage.error('获取数据出错: ' + (error.message || '未知错误'))
    rcuDevices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 获取rcuId设备数据 - 使用 /system/rcu/list 接口带rcuId参数
const fetchData = async () => {
  if (!rcuId.value) {
    ElMessage.warning('请输入RCU ID')
    return
  }
  
  loading.value = true
  try {
    const params = {
      rcuId: rcuId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    const response = await axios.get('/system/rcu/list', { params })
    
    if (response.data.status === 'success') {
      rcuDevices.value = response.data.rows || []
      // 为设备添加默认的在线状态
      rcuDevices.value = rcuDevices.value.map(device => ({
        ...device,
        status: 'online' // 默认设置为在线
      }))
      total.value = response.data.total || 0
      
      if (rcuDevices.value.length === 0) {
        ElMessage.info('未找到符合条件的数据')
      }
    } else {
      ElMessage.warning('获取数据失败: ' + (response.data.msg || '未知错误'))
      rcuDevices.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取数据出错详情:', error)
    ElMessage.error('获取数据出错: ' + (error.message || '未知错误'))
    rcuDevices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 获取特定RCU的消息列表
const fetchRcuMessages = async () => {
  if (!currentRcuId.value) return
  
  messagesLoading.value = true
  try {
    // 确保时间范围合理
    let startTime = timeRange.value && timeRange.value[0] ? timeRange.value[0] : (Date.now() - 24 * 60 * 60 * 1000)
    let endTime = timeRange.value && timeRange.value[1] ? timeRange.value[1] : Date.now()
    
    const params = {
      rcuId: currentRcuId.value,
      startTime: startTime,
      endTime: endTime,
      pageNum: messagesPageNum.value,
      pageSize: messagesPageSize.value
    }
    
    const response = await axios.get('/system/rcu/info_list', { params })
    
    if (response.data.status === 'success') {
      if (Array.isArray(response.data.rows)) {
        rcuMessages.value = response.data.rows
        messagesTotal.value = response.data.total || rcuMessages.value.length
        
        if (rcuMessages.value.length === 0) {
          ElMessage.info(`未找到设备 "${currentRcuId.value}" 在指定时间范围内的消息数据`)
        }
      } else {
        ElMessage.warning('API返回了无效的数据格式')
        rcuMessages.value = []
        messagesTotal.value = 0
      }
    } else {
      ElMessage.warning('获取RCU消息列表失败: ' + (response.data.msg || '未知错误'))
      rcuMessages.value = []
      messagesTotal.value = 0
    }
  } catch (error) {
    console.error('获取RCU消息列表出错详情:', error)
    ElMessage.error('获取RCU消息列表出错: ' + (error.message || '未知错误'))
    rcuMessages.value = []
    messagesTotal.value = 0
  } finally {
    messagesLoading.value = false
  }
}

// 获取消息对象列表数据
const fetchMessageObjects = async () => {
  if (!currentMessageId.value) return
  
  objectsLoading.value = true
  try {
    const params = {
      objsflag: currentMessageId.value,
      pageNum: objectsPageNum.value,
      pageSize: objectsPageSize.value
    }
    
    const response = await axios.get('/system/rcu/obj_list', { params })
    
    if (response.data.status === 'success') {
      if (Array.isArray(response.data.rows)) {
        // 添加数据转换步骤，确保objId字段存在
        messageObjects.value = response.data.rows.map(obj => {
          return {
            ...obj,
            // 确保objId存在且为字符串格式，如果不存在则提供默认值
            objId: obj.objId !== undefined ? obj.objId.toString() : '未知ID'
          }
        });
        
        objectsTotal.value = response.data.total || messageObjects.value.length
        
        if (messageObjects.value.length === 0) {
          ElMessage.info('此消息没有包含任何对象数据')
        }
      } else {
        ElMessage.warning('API返回了无效的对象数据格式')
        messageObjects.value = []
        objectsTotal.value = 0
      }
    } else {
      ElMessage.error('获取对象列表失败: ' + (response.data.msg || '未知错误'))
      messageObjects.value = []
      objectsTotal.value = 0
    }
  } catch (error) {
    console.error('获取对象列表出错详情:', error)
    ElMessage.error('获取对象列表出错: ' + (error.message || '未知错误') + '，请尝试缩小查询范围')
    messageObjects.value = []
    objectsTotal.value = 0
  } finally {
    objectsLoading.value = false
  }
}

// 处理主表格选择变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 处理消息表格选择变化
const handleMessageSelectionChange = (selection) => {
  selectedMessages.value = selection
}

// 处理对象表格选择变化
const handleObjectSelectionChange = (selection) => {
  selectedObjects.value = selection
}

// 导出RCU设备数据
const handleExport = async () => {
  if (multipleSelection.value.length === 0 && !rcuId.value) {
    ElMessage.warning('请先选择要导出的RCU设备或输入RCU ID')
    return
  }
  
  exporting.value = true
  try {
    let params = {}
    
    if (multipleSelection.value.length > 0) {
      // 导出选中的RCU设备数据 - 使用逗号分隔的字符串
      params.rcuIds = multipleSelection.value.map(item => item.rcuId).join(',')
    } else {
      // 导出查询条件下的所有数据
      params.rcuIds = rcuId.value
    }
    
    // 设置默认时间范围 - 24小时
    const end = new Date()
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24)
    params.startTime = start.getTime()
    params.endTime = end.getTime()
    
    // 步骤1: 发送请求获取filePath
    const response = await axios.get('/system/rcu/exportByRcuId', {
      params: params
    })
    
    // 步骤2: 检查响应状态并使用filePath构建下载链接
    if (response.data && response.data.status === 'success') {
      // 获取文件路径
      const filePath = response.data.filepath
      
      // 使用硬编码的基础URL (根据您的环境调整)
      const baseUrl = '/api' // 或者使用实际的服务器地址
      const downloadUrl = baseUrl + filePath
      
      // 创建下载链接并触发点击
      const link = document.createElement('a')
      link.href = downloadUrl
      link.target = '_blank'
      link.setAttribute('download', 'RCU设备数据_' + new Date().getTime() + '.xlsx')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('导出成功，正在下载文件')
    } else {
      ElMessage.error('导出失败：' + (response.data?.msg || '未知错误'))
    }
  } catch (error) {
    console.error('导出错误详情:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

// 导出消息数据
const handleExportMessages = async () => {
  if (selectedMessages.value.length === 0) {
    ElMessage.warning('请先选择要导出的消息数据')
    return
  }
  
  try {
    // 导出选中的消息数据 - 使用逗号分隔的字符串
    const params = {
      objsflags: selectedMessages.value.map(item => item.objsflag).join(',')
    }
    
    // 添加时间范围
    if (timeRange.value && timeRange.value.length === 2) {
      params.startTime = timeRange.value[0]
      params.endTime = timeRange.value[1]
    } else {
      // 设置默认时间范围
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24) // 默认24小时
      params.startTime = start.getTime()
      params.endTime = end.getTime()
    }
    
    // 步骤1: 发送请求获取filePath
    const response = await axios.get('/system/rcu/exportByobjsflag', {
      params: params
    })
    
    // 步骤2: 检查响应状态并使用filePath构建下载链接
    if (response.data && response.data.status === 'success') {
      // 获取文件路径
      const filePath = response.data.filepath
      
      // 使用硬编码的基础URL (根据您的环境调整)
      const baseUrl = '/api' // 或者使用实际的服务器地址
      const downloadUrl = baseUrl + filePath
      
      // 创建下载链接并触发点击
      const link = document.createElement('a')
      link.href = downloadUrl
      link.target = '_blank'
      link.setAttribute('download', '消息数据_' + currentRcuId.value + '_' + new Date().getTime() + '.xlsx')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('导出成功，正在下载文件')
    } else {
      ElMessage.error('导出失败：' + (response.data?.msg || '未知错误'))
    }
  } catch (error) {
    console.error('导出消息错误详情:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  }
}

// 导出对象数据
const handleExportObjects = async () => {
  if (selectedObjects.value.length === 0) {
    ElMessage.warning('请先选择要导出的对象数据')
    return
  }
  
  try {
    // 导出选中的对象数据
    const params = {
      objIds: selectedObjects.value.map(item => item.objId).join(','),
      objsflag: currentMessageId.value
    }
    
    // 步骤1: 发送请求获取filePath
    const response = await axios.get('/system/rcu/export_objects', {
      params: params
    })
    
    // 步骤2: 检查响应状态并使用filePath构建下载链接
    if (response.data && response.data.status === 'success') {
      // 获取文件路径
      const filePath = response.data.filepath
      
      // 使用硬编码的基础URL (根据您的环境调整)
      const baseUrl = '/api' // 或者使用实际的服务器地址
      const downloadUrl = baseUrl + filePath
      
      // 创建下载链接并触发点击
      const link = document.createElement('a')
      link.href = downloadUrl
      link.target = '_blank'
      link.setAttribute('download', '对象数据_' + currentMessageId.value + '_' + new Date().getTime() + '.xlsx')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('导出成功，正在下载文件')
    } else {
      ElMessage.error('导出失败：' + (response.data?.msg || '未知错误'))
    }
  } catch (error) {
    console.error('导出对象错误详情:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  }
}

// 批量删除选中的RCU设备
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请先选择要删除的RCU设备')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${multipleSelection.value.length} 个RCU设备及其所有数据吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    loading.value = true
    // 依次删除选中的RCU设备
    const errors = []
    for (const item of multipleSelection.value) {
      try {
        const params = {
          rcuId: item.rcuId
        }
        
        const response = await axios.delete('/system/rcu', { params })
        
        if (response.data.status !== 'success') {
          errors.push(item.rcuId)
        }
      } catch (error) {
        errors.push(item.rcuId)
      }
    }
    
    // 根据处理结果显示提示
    if (errors.length === 0) {
      ElMessage.success('所有RCU设备删除成功')
    } else {
      ElMessage.warning(`部分RCU设备删除失败: ${errors.length}/${multipleSelection.value.length}`)
    }
    
    // 刷新列表
    fetchAllRcus()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 批量删除选中的消息
const handleBatchDeleteMessages = () => {
  if (selectedMessages.value.length === 0) {
    ElMessage.warning('请先选择要删除的消息')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedMessages.value.length} 条消息及其所有对象数据吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    messagesLoading.value = true
    // 依次删除选中的消息
    const errors = []
    for (const item of selectedMessages.value) {
      try {
        const params = {
          objsflag: item.objsflag
        }
        
        const response = await axios.delete('/system/rcu/objs', { params })
        
        if (response.data.status !== 'success') {
          errors.push(item.objsflag)
        }
      } catch (error) {
        errors.push(item.objsflag)
      }
    }
    
    // 根据处理结果显示提示
    if (errors.length === 0) {
      ElMessage.success('所有消息删除成功')
    } else {
      ElMessage.warning(`部分消息删除失败: ${errors.length}/${selectedMessages.value.length}`)
    }
    
    // 刷新消息列表
    fetchRcuMessages()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 批量删除选中的对象
const handleBatchDeleteObjects = () => {
  if (selectedObjects.value.length === 0) {
    ElMessage.warning('请先选择要删除的对象')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedObjects.value.length} 个对象吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    objectsLoading.value = true
    // 依次删除选中的对象
    const errors = []
    for (const item of selectedObjects.value) {
      try {
        const params = {
          objsflag: currentMessageId.value,
          objId: item.objId
        }
        
        const response = await axios.delete('/system/rcu/obj', { params })
        
        if (response.data.status !== 'success') {
          errors.push(item.objId)
        }
      } catch (error) {
        errors.push(item.objId)
      }
    }
    
    // 根据处理结果显示提示
    if (errors.length === 0) {
      ElMessage.success('所有对象删除成功')
    } else {
      ElMessage.warning(`部分对象删除失败: ${errors.length}/${selectedObjects.value.length}`)
    }
    
    // 刷新对象列表
    fetchMessageObjects()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 主表分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  if (rcuId.value) {
    fetchData()
  } else {
    fetchAllRcus()
  }
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  if (rcuId.value) {
    fetchData()
  } else {
    fetchAllRcus()
  }
}

// 消息列表分页变化
const handleMessagesSizeChange = (size) => {
  messagesPageSize.value = size
  fetchRcuMessages()
}

const handleMessagesCurrentChange = (page) => {
  messagesPageNum.value = page
  fetchRcuMessages()
}

// 对象列表分页变化
const handleObjectsSizeChange = (size) => {
  objectsPageSize.value = size
  fetchMessageObjects()
}

const handleObjectsCurrentChange = (page) => {
  objectsPageNum.value = page
  fetchMessageObjects()
}

// 处理查看RCU消息列表
const handleViewMessages = (rcu) => {
  currentRcuId.value = rcu.rcuId
  messagesPageNum.value = 1
  messagesPageSize.value = 10
  selectedMessages.value = []
  
  // 设置默认时间范围为最近24小时
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 3600 * 1000 * 24)
  timeRange.value = [start.getTime(), end.getTime()]
  
  messagesDialogVisible.value = true
  fetchRcuMessages()
}

// 处理查看消息对象列表
const handleViewMessageObjects = (message) => {
  currentMessageId.value = message.objsflag
  objectsPageNum.value = 1
  objectsPageSize.value = 10
  selectedObjects.value = []
  objectSearchQuery.value = ''
  
  objectsListDialogVisible.value = true
  fetchMessageObjects()
}

// 处理查看对象详情
const handleViewObjectDetails = (object) => {
  currentObject.value = object
  currentObjectId.value = object.objId
  
  objectDetailDialogVisible.value = true
}

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
}
</script>

<style scoped>
.rcu-objects-container {
  padding: 24px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 12px;
  border: 1px solid #0e5986;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(5px);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.filter-item {
  flex: 1;
  min-width: 200px;
}

.search-area {
  max-width: 350px;
}

.filter-input {
  width: 100%;
}

.filter-input :deep(.el-input__wrapper),
:deep(.el-date-editor .el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.filter-input :deep(.el-input__wrapper:hover),
:deep(.el-date-editor .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.filter-input :deep(.el-input__wrapper.is-focus),
:deep(.el-date-editor .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

.filter-input :deep(.el-input__inner),
:deep(.el-date-editor .el-input__inner) {
  color: white;
}

:deep(.el-date-editor) {
  --el-fill-color-blank: rgba(0, 20, 40, 0.7);
  --el-text-color-regular: #e1f2ff;
  --el-border-color: rgba(14, 89, 134, 0.7);
  border-radius: 6px;
  overflow: hidden;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.table-actions {
  display: flex;
  gap: 12px;
}

.data-table {
  --el-table-border-color: rgba(14, 89, 134, 0.5);
  --el-table-header-bg-color: rgba(0, 40, 80, 0.6);
  --el-table-row-hover-bg-color: rgba(0, 145, 234, 0.2);
  --el-table-header-text-color: #ffffff;
  --el-table-text-color: #e1f2ff;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 20px;
  background-color: rgba(0, 20, 40, 0.4);
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
  cursor: pointer;
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

.data-table :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.data-table :deep(.el-table--border),
.data-table :deep(.el-table--group) {
  border: 1px solid #0e5986;
  border-radius: 8px;
  overflow: hidden;
}

.data-table :deep(.el-table__fixed-right),
.data-table :deep(.el-table__fixed-left) {
  height: 100% !important;
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

/* 新增表格选择框样式 */
.data-table :deep(.el-table-column--selection .el-checkbox) {
  margin-right: 0;
}

.data-table :deep(.el-checkbox__inner) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
}

.data-table :deep(.el-checkbox__inner:hover) {
  border-color: #00e0ff;
}

.data-table :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #00a1ff;
  border-color: #00a1ff;
}

/* 操作按钮垂直居中对齐 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pagination, .dialog-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 10px;
  background-color: rgba(0, 30, 60, 0.3);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.pagination :deep(.el-pagination),
.dialog-pagination :deep(.el-pagination) {
  padding: 0;
  margin: 0;
  font-weight: normal;
  color: #a0cfff;
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: rgba(0, 40, 80, 0.6);
  --el-pagination-hover-color: #00ffff;
  --el-pagination-font-size: 14px;
}

.pagination :deep(.el-pagination .el-pagination__total),
.dialog-pagination :deep(.el-pagination .el-pagination__total) {
  font-size: 14px;
  color: #a0cfff;
}

.pagination :deep(.el-pagination .el-pagination__jump),
.dialog-pagination :deep(.el-pagination .el-pagination__jump) {
  color: #a0cfff;
}

.pagination :deep(.el-pagination .el-input__inner),
.dialog-pagination :deep(.el-pagination .el-input__inner) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: #e1f2ff;
  border-radius: 4px;
}

.pagination :deep(.el-pagination .el-select .el-input),
.dialog-pagination :deep(.el-pagination .el-select .el-input) {
  width: 110px;
}

.pagination :deep(.el-pagination .el-select-dropdown__item),
.dialog-pagination :deep(.el-pagination .el-select-dropdown__item) {
  color: #a0cfff;
}

.pagination :deep(.el-pagination .btn-prev),
.pagination :deep(.el-pagination .btn-next),
.dialog-pagination :deep(.el-pagination .btn-prev),
.dialog-pagination :deep(.el-pagination .btn-next) {
  background-color: rgba(0, 50, 100, 0.5);
  border-radius: 4px;
  color: #a0cfff;
  transition: all 0.3s ease;
}

.pagination :deep(.el-pagination .btn-prev:hover),
.pagination :deep(.el-pagination .btn-next:hover),
.dialog-pagination :deep(.el-pagination .btn-prev:hover),
.dialog-pagination :deep(.el-pagination .btn-next:hover) {
  background-color: rgba(0, 90, 180, 0.6);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

.pagination :deep(.el-pagination .el-pager li),
.dialog-pagination :deep(.el-pagination .el-pager li) {
  background-color: rgba(0, 50, 100, 0.5);
  color: #a0cfff;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.pagination :deep(.el-pagination .el-pager li:hover),
.dialog-pagination :deep(.el-pagination .el-pager li:hover) {
  background-color: rgba(0, 90, 180, 0.6);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

.pagination :deep(.el-pagination .el-pager li.is-active),
.dialog-pagination :deep(.el-pagination .el-pager li.is-active) {
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

/* tag样式 */
:deep(.el-tag--success) {
  background: linear-gradient(135deg, rgba(0, 196, 140, 0.8), rgba(0, 161, 112, 0.8));
  border: 1px solid #00a170;
  box-shadow: 0 2px 6px rgba(0, 196, 140, 0.3);
  color: white;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, rgba(255, 82, 82, 0.8), rgba(230, 57, 57, 0.8));
  border: 1px solid #e63939;
  box-shadow: 0 2px 6px rgba(255, 82, 82, 0.3);
  color: white;
}

/* 自定义对话框样式 */
:deep(.el-dialog) {
  background-color: rgba(0, 20, 40, 0.95);
  border: 1px solid #0e5986;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(10px);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(90deg, rgba(0, 60, 120, 0.8), rgba(0, 40, 80, 0.8));
  color: #e1f2ff;
  border-bottom: 1px solid rgba(0, 183, 255, 0.3);
  padding: 20px 24px;
  margin: 0 !important;
}

:deep(.el-dialog__title) {
  color: #e1f2ff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.5);
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
}

.dialog-filter-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.dialog-filter-row .el-date-picker {
  width: auto;
  flex: 2;
  min-width: 300px;
}

.dialog-filter-row .search-input {
  width: auto;
  flex: 1;
  min-width: 200px;
}

.dialog-content {
  max-height: unset;
}

.message-table, .objects-table {
  --el-table-header-bg-color: rgba(0, 40, 80, 0.7);
  --el-table-row-hover-bg-color: rgba(0, 145, 234, 0.3);
  --el-table-border-color: #0e5986;
  --el-table-header-text-color: #ffffff;
  --el-table-text-color: #e1f2ff;
}

.objects-dialog :deep(.el-table__cell) {
  padding: 8px 0;
}

.objects-dialog :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(0, 40, 80, 0.4);
}

.dialog-content .data-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.dialog-content .data-table :deep(.el-scrollbar__wrap) {
  overflow-x: auto !important;
}

/* 更好的空数据状态可视性 */
.empty-data {
  padding: 40px;
  text-align: center;
  color: #8f9bb3;
  font-size: 16px;
  background: rgba(0, 30, 60, 0.3);
  border-radius: 10px;
  margin: 20px 0;
  border: 1px dashed rgba(14, 89, 134, 0.7);
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  width: 100%;
  margin-top: 10px;
}

/* 修改对象详情弹窗样式，提高可读性 */
.object-details {
  padding: 20px;
  background-color: rgba(0, 25, 50, 0.7);
  border-radius: 8px;
  margin-top: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3), 0 0 20px rgba(0, 100, 234, 0.1);
  border: 1px solid rgba(14, 89, 134, 0.5);
}

.object-details-dialog :deep(.el-descriptions) {
  --el-descriptions-item-bordered-label-background: rgba(15, 60, 110, 0.8);
  --el-descriptions-item-bordered-content-background: rgba(0, 30, 60, 0.7);
  --el-descriptions-border-color: #1c7db6;
}

.object-details-dialog :deep(.el-descriptions__label) {
  color: #ffffff;
  font-weight: bold;
  padding: 12px 10px;
  text-shadow: 0 0 5px rgba(0, 183, 255, 0.3);
}

.object-details-dialog :deep(.el-descriptions__content) {
  color: #ffffff;
  padding: 12px;
}

.message-info {
  color: #e1f2ff;
  font-size: 14px;
  margin-bottom: 15px;
  text-shadow: 0 0 5px rgba(0, 183, 255, 0.3);
  line-height: 1.6;
}

.object-navigation {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 16px;
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

.dialog-content {
  animation: fadeIn 0.3s ease-out;
}

.object-details {
  animation: fadeIn 0.3s ease-out;
}
</style>