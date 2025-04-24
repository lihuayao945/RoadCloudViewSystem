<template>
  <div class="logs-container">
    <div class="action-row">
      <div class="filter-group">
        <el-select v-model="logLevel" placeholder="状态" class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="成功" value="0" />
          <el-option label="失败" value="1" />
        </el-select>
        <el-select v-model="logModule" placeholder="业务类型" class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="其他" value="OTHER" />
          <el-option label="新增" value="INSERT" />
          <el-option label="修改" value="UPDATE" />
          <el-option label="查询" value="QUERY" />
          <el-option label="删除" value="DELETE" />
          <el-option label="更新状态" value="STATUS" />
          <el-option label="授权" value="ASSIGN" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="date-picker"
        />
      </div>
      <div class="table-actions">
        <el-button type="primary" size="small" @click="handleSearch" :loading="loading">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <!-- <el-button type="success" size="small" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出
        </el-button> -->
        <el-button type="danger" size="small" @click="handleClear" :loading="clearing">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
      </div>
    </div>

    <el-table
      :data="logs"
      style="width: 100%"
      class="data-table"
      v-loading="loading"
      @row-click="handleRowClick"
    >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="操作" width="100" />
      <el-table-column prop="business_type" label="业务类型" width="90">
        <template #default="{ row }">
          <el-tag :type="getBusinessTypeTag(row.businessType)" effect="light">
            {{ getBusinessTypeName(row.businessType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="requestMethod" label="请求方式" width="90" />
      <el-table-column prop="operName" label="操作人" width="90" />
      <el-table-column prop="operUrl" label="请求地址" min-width="120" show-overflow-tooltip />
      <el-table-column prop="operIp" label="IP地址" width="130" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" effect="light">
            {{ row.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operTime" label="操作时间" width="160">
        <template #default="{ row }">
          {{ formatTimestamp(row.operTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="executeTime" label="耗时(ms)" width="100" sortable />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <div class="table-actions-column">
            <el-button size="small" @click="handleDetail(row)">
              <el-icon><Document /></el-icon>
              详情
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" :loading="row.deleting">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="70%"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="日志编号">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ currentLog.title }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ getBusinessTypeName(currentLog.businessType) }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ currentLog.operName }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ currentLog.operIp }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ currentLog.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="操作方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="currentLog.status === 0 ? 'success' : 'danger'" effect="light">
            {{ currentLog.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTimestamp(currentLog.operTime) }}</el-descriptions-item>
        <el-descriptions-item label="消耗时间">{{ currentLog.executeTime }}毫秒</el-descriptions-item>
      </el-descriptions>

      <el-tabs class="detail-tabs">
        <el-tab-pane label="请求参数">
          <el-code-highlight language="json" style="max-height: 300px; overflow-y: auto;">
            {{formatJson(currentLog.oper_param || '{}')}}
          </el-code-highlight>
        </el-tab-pane>
        <el-tab-pane label="返回结果">
          <el-code-highlight language="json" style="max-height: 300px; overflow-y: auto;">
            {{formatJson(currentLog.json_result || '{}')}}
          </el-code-highlight>
        </el-tab-pane>
        <el-tab-pane label="异常信息" v-if="currentLog.error_msg">
          <pre class="error-msg">{{ currentLog.error_msg }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { 
  Search, 
  Download, 
  Delete,
  Document
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

// 配置axios
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';
axios.defaults.timeout = 15000;

// 状态变量
const logs = ref([])
const logLevel = ref('')
const logModule = ref('')
const dateRange = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const clearing = ref(false)

// 详情对话框
const detailVisible = ref(false)
const currentLog = ref({})

// 初始化数据
onMounted(() => {
  fetchLogs()
})

// 获取日志数据
const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    // 添加筛选条件
    if (logLevel.value !== '') {
      params.status = logLevel.value
    }
    
    if (logModule.value) {
      params.businessType = logModule.value
    }
    
    if (dateRange.value && dateRange.value.length === 2) {
      params.beginTime = formatDate(dateRange.value[0]).getTime()
      params.endTime = formatDate(dateRange.value[1]).getTime()
      // 转为时间戳

    }
    
    // 发送请求
    const response = await axios.get('/system/log/list', { params })
    
    // 打印响应数据，便于开发分析
    console.log('API响应数据:', response.data)
    console.log('日志列表数据:', response.data.rows)
    
    // 处理响应
    if (response.data.status === 'success') {
      logs.value = response.data.rows
      total.value = parseInt(response.data.total) || 0
    } else {
      ElMessage.error('获取日志失败: ' + (response.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('获取日志出错:', error)
    ElMessage.error('获取日志失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 格式化时间戳为日期时间
const formatTimestamp = (timestamp) => {
  if (!timestamp) return ''
  
  // 判断是否为毫秒级时间戳（13位），如果是10位的秒级时间戳则转换为毫秒级
  const ts = String(timestamp).length === 10 ? timestamp * 1000 : timestamp
  
  const date = new Date(Number(ts))
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 获取业务类型名称
const getBusinessTypeName = (type) => {
  // 添加调试日志
  console.log('业务类型值:', type, '类型:', typeof type);
  
  const types = {
    'OTHER': '其他',
    'INSERT': '新增',
    'UPDATE': '修改',
    'QUERY': '查询',
    'DELETE': '删除',
    'STATUS': '更新状态',
    'ASSIGN': '授权'
  }
  
  // 如果类型是数字，尝试转换为字符串
  if (typeof type === 'number') {
    type = type.toString();
  }
  
  // 如果类型是字符串但包含空格，去除空格
  if (typeof type === 'string') {
    type = type.trim();
  }
  
  const result = types[type] || '未知';
  console.log('业务类型映射结果:', result);
  return result;
}

// 获取业务类型标签样式
const getBusinessTypeTag = (type) => {
  // 如果类型是数字，尝试转换为字符串
  if (typeof type === 'number') {
    type = type.toString();
  }
  
  // 如果类型是字符串但包含空格，去除空格
  if (typeof type === 'string') {
    type = type.trim();
  }
  
  const tags = {
    'OTHER': 'info',
    'INSERT': 'success',
    'UPDATE': 'warning',
    'QUERY': '',
    'DELETE': 'danger',
    'STATUS': 'warning',
    'ASSIGN': 'success'
  }
  return tags[type] || ''
}

// 格式化JSON
const formatJson = (jsonString) => {
  try {
    const obj = JSON.parse(jsonString)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return jsonString
  }
}

// 处理查询
const handleSearch = () => {
  pageNum.value = 1 // 重置为第一页
  fetchLogs()
}

// 处理分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchLogs()
}

// 处理当前页变化
const handleCurrentChange = (page) => {
  pageNum.value = page
  fetchLogs()
}

// 处理导出
const handleExport = async () => {
  exporting.value = true
  try {
    // 构建导出的参数，按照接口文档规范调整参数名称
    const params = {}
    
    if (dateRange.value && dateRange.value.length === 2) {
      params.starttime = formatDate(dateRange.value[0])
      params.endtime = formatDate(dateRange.value[1])
    }
    
    // 添加其他可能的过滤条件
    if (logLevel.value) {
      params.status = logLevel.value
    }
    
    if (logModule.value) {
      params.businessType = logModule.value
    }
    
    // 第一步：发送普通请求获取文件路径
    const response = await axios.get('/system/log/export', { params })
    
    // 打印响应数据，便于调试
    console.log('导出响应:', response.data)
    
    if (response.data.status === 'success') {
      // 从响应中获取文件路径
      const filePath = response.data.filepath
      
      // 输出第二次请求的完整路径
      console.log('开始第二次请求，下载文件路径:', filePath)
      
      // 创建完整的URL（使用服务器URL）
      const fullUrl = `/api${filePath}`
      console.log('完整下载URL:', fullUrl)
      
      try {
        // 使用window.open直接在新窗口打开下载链接
        // 这样可以避免CORS问题，因为浏览器直接处理下载
        window.open(fullUrl, '_blank')
        
        ElMessage.success('文件下载已启动，请检查浏览器下载')
      } catch (downloadError) {
        console.error('文件下载出错:', downloadError)
        
        // 备选方案：尝试使用表单提交方式下载
        const form = document.createElement('form')
        form.method = 'GET'
        form.action = fullUrl
        form.target = '_blank'
        document.body.appendChild(form)
        form.submit()
        document.body.removeChild(form)
        
        ElMessage.success('正在尝试备选下载方式')
      }
      
      /* 
      // 原先的下载方式可能存在跨域问题，先注释掉保留
      // 第二步：使用axios通过代理下载文件
      const downloadResponse = await axios.get(filePath, { 
        responseType: 'blob',  // 指定响应类型为二进制数据
      })
      
      // 输出下载响应的信息
      console.log('文件下载响应状态:', downloadResponse.status)
      console.log('文件下载响应头:', downloadResponse.headers)
      console.log('文件下载响应类型:', typeof downloadResponse.data)
      console.log('文件下载响应大小:', downloadResponse.data.size, 'bytes')
      
      // 检查内容类型是否正确
      const contentType = downloadResponse.headers['content-type']
      if (contentType && contentType.includes('text/html')) {
        console.error('警告：服务器返回的是HTML而不是Excel文件')
        ElMessage.error('下载失败：服务器返回了错误的文件类型')
        return
      }
      
      // 创建Blob URL
      const blob = new Blob([downloadResponse.data], { 
        type: contentType || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
      })
      const blobUrl = URL.createObjectURL(blob)
      
      // 创建下载链接
      const link = document.createElement('a')
      link.href = blobUrl
      
      // 从文件路径中提取文件名
      const fileName = filePath.split('/').pop() || '操作日志.xlsx'
      link.download = fileName
      
      // 模拟点击下载
      document.body.appendChild(link)
      link.click()
      
      // 清理
      document.body.removeChild(link)
      URL.revokeObjectURL(blobUrl)
      */
      
      ElMessage.success('日志导出成功')
    } else {
      ElMessage.error('导出日志失败: ' + (response.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('导出日志出错:', error)
    // 详细输出错误信息
    if (error.response) {
      // 服务器返回了错误状态码
      console.error('错误状态码:', error.response.status)
      console.error('错误响应头:', error.response.headers)
      console.error('错误响应数据:', error.response.data)
      
      if (error.response.status === 403) {
        console.error('权限错误(403): 您可能没有导出日志的权限')
        ElMessage.error('权限错误: 您可能没有导出日志的权限')
      } else {
        ElMessage.error('导出日志失败: ' + (error.response.data?.message || error.message || '未知错误'))
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      console.error('请求已发出，但没有收到响应:', error.request)
      ElMessage.error('导出日志失败: 服务器无响应')
    } else {
      // 请求设置时出错
      console.error('请求设置错误:', error.message)
      ElMessage.error('导出日志失败: ' + error.message)
    }
  } finally {
    exporting.value = false
  }
}

// 处理清空
const handleClear = () => {
  ElMessageBox.confirm('确定要清空所有日志吗？此操作不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    clearing.value = true
    try {
      const response = await axios.delete('/system/log/clean')
      
      if (response.data.status === 'success') {
        ElMessage.success('日志已清空')
        // 重新加载日志
        pageNum.value = 1
        fetchLogs()
      } else {
        ElMessage.error('清空日志失败: ' + (response.data.message || '未知错误'))
      }
    } catch (error) {
      console.error('清空日志出错:', error)
      ElMessage.error('清空日志失败: ' + (error.message || '未知错误'))
    } finally {
      clearing.value = false
    }
  }).catch(() => {
    ElMessage.info('已取消清空操作')
  })
}

// 处理查看详情
const handleDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

// 处理删除单条日志
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除ID为 ${row.id} 的日志记录吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // 设置删除中状态
    row.deleting = true
    
    try {
      const response = await axios.delete('/system/log/delete', { 
        params: { id: row.id } 
      })
      
      if (response.data.status === 'success') {
        ElMessage.success('日志删除成功')
        // 重新加载日志列表
        fetchLogs()
      } else {
        ElMessage.error('删除日志失败: ' + (response.data.message || '未知错误'))
      }
    } catch (error) {
      console.error('删除日志出错:', error)
      ElMessage.error('删除日志失败: ' + (error.message || '未知错误'))
    } finally {
      // 清除删除中状态
      row.deleting = false
    }
  }).catch(() => {
    ElMessage.info('已取消删除操作')
  })
}

// 处理表格行点击
const handleRowClick = (row, column, event) => {
  // 检查点击的不是按钮
  if (event.target.tagName === 'BUTTON' || 
      event.target.closest('button')) {
    return
  }
  
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
.logs-container {
  padding: 24px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 12px;
  border: 1px solid #0e5986;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(5px);
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  flex: 1;
}

.filter-select {
  width: 130px;
}

.filter-select :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.filter-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.filter-select :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

.filter-select :deep(.el-input__inner) {
  color: white;
}

.filter-select :deep(.el-select__popper) {
  background-color: rgba(0, 30, 60, 0.95);
  border: 1px solid rgba(14, 89, 134, 0.7);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
}

.filter-select :deep(.el-select-dropdown__item) {
  color: #e1f2ff;
}

.filter-select :deep(.el-select-dropdown__item.hover) {
  background-color: rgba(0, 145, 234, 0.2);
}

.filter-select :deep(.el-select-dropdown__item.selected) {
  background-color: rgba(0, 145, 234, 0.3);
  color: #ffffff;
  font-weight: bold;
}

.date-picker {
  width: 320px;
}

.date-picker :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.date-picker :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.date-picker :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

.date-picker :deep(.el-input__inner), .date-picker :deep(.el-range-input) {
  color: white;
}

.date-picker :deep(.el-range-separator) {
  color: rgba(255, 255, 255, 0.7);
}

.table-actions {
  display: flex;
  gap: 12px;
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

.table-actions-column {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.table-actions-column :deep(.el-button) {
  padding: 4px 8px;
  min-height: 28px;
  font-size: 12px;
}

.table-actions-column :deep(.el-button .el-icon) {
  font-size: 14px;
  margin-right: 2px;
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
  animation: fadeIn 0.3s ease-out;
}

/* 修复表格右侧过大边距的问题 */
.data-table :deep(.el-table__fixed-right) {
  height: 100% !important;
  box-shadow: -3px 0 10px rgba(0, 0, 0, 0.12);
}

.data-table :deep(.el-table__fixed-right::before) {
  display: none;
}

.data-table :deep(.el-table__body-wrapper) {
  overflow-x: auto !important;
}

.data-table :deep(.el-table__header-wrapper, .el-table__body-wrapper) {
  margin-right: 0 !important;
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

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 4px;
  padding: 0 10px;
  height: 26px;
  line-height: 26px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  font-weight: 500;
  letter-spacing: 0.5px;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, rgba(0, 196, 140, 0.9), rgba(0, 161, 112, 0.9));
  border-color: rgba(0, 196, 140, 0.8);
  color: white;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, rgba(255, 82, 82, 0.9), rgba(230, 57, 57, 0.9));
  border-color: rgba(255, 82, 82, 0.8);
  color: white;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.9), rgba(230, 126, 0, 0.9));
  border-color: rgba(255, 149, 0, 0.8);
  color: white;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, rgba(144, 147, 153, 0.9), rgba(107, 109, 113, 0.9));
  border-color: rgba(144, 147, 153, 0.8);
  color: white;
}

:deep(.el-tag:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.25);
}

/* 对话框样式 */
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
}

:deep(.el-dialog__close) {
  color: #e1f2ff;
}

:deep(.el-dialog__close:hover) {
  background-color: rgba(0, 145, 234, 0.2);
  color: #ffffff;
  border-radius: 50%;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
  background-color: rgba(0, 30, 60, 0.3);
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(14, 89, 134, 0.5);
}

:deep(.el-descriptions__header) {
  margin-bottom: 0;
}

:deep(.el-descriptions__title) {
  color: #e1f2ff;
  font-weight: bold;
}

:deep(.el-descriptions__label) {
  background-color: rgba(0, 40, 80, 0.6);
  color: #e1f2ff;
  padding: 14px 12px;
  font-weight: 500;
}

:deep(.el-descriptions__content) {
  background-color: rgba(0, 30, 60, 0.3);
  color: #a0cfff;
  padding: 14px 12px;
}

:deep(.el-descriptions .el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
  border-color: rgba(14, 89, 134, 0.5);
}

.detail-tabs {
  margin-top: 20px;
}

:deep(.el-tabs__header) {
  background-color: rgba(0, 40, 80, 0.6);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(14, 89, 134, 0.5);
  padding: 0 16px;
}

:deep(.el-tabs__nav) {
  border: none;
}

:deep(.el-tabs__item) {
  color: #a0cfff;
  height: 50px;
  font-size: 15px;
  transition: all 0.3s ease;
}

:deep(.el-tabs__item:hover) {
  color: #e1f2ff;
}

:deep(.el-tabs__item.is-active) {
  color: #00e0ff;
  font-weight: bold;
  text-shadow: 0 0 10px rgba(0, 224, 255, 0.3);
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(to right, #0091ff, #00e0ff);
  height: 3px;
  border-radius: 3px;
}

:deep(.el-tabs__content) {
  background-color: rgba(0, 30, 60, 0.3);
  border-radius: 0 0 8px 8px;
  border: 1px solid rgba(14, 89, 134, 0.5);
  border-top: none;
  padding: 16px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.error-msg {
  color: #ff6b6b;
  background-color: rgba(255, 82, 82, 0.1);
  padding: 16px;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
  border: 1px dashed rgba(255, 82, 82, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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

@media (max-width: 768px) {
  .logs-container {
    padding: 16px;
  }
  
  .action-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-group {
    flex-direction: column;
    width: 100%;
  }
  
  .filter-select, .date-picker {
    width: 100%;
  }
  
  .table-actions {
    margin-top: 12px;
    width: 100%;
    justify-content: flex-end;
  }
}
</style> 