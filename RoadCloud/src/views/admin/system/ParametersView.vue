<template>
  <div class="parameters-container">
    <div class="action-row">
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索参数..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="table-actions">
        <el-button type="success" size="default" @click="handleAddParameter">
          <el-icon><Plus /></el-icon>
          添加参数
        </el-button>
        <el-button v-if="activeTab === 'style'" type="danger" size="default" @click="handleBatchDelete" :disabled="!multipleSelectionStyle.length">
          <el-icon><Delete /></el-icon>
          删除{{ multipleSelectionStyle.length ? ' (' + multipleSelectionStyle.length + ')' : '' }}
        </el-button>
        <el-button v-else type="danger" size="default" @click="handleBatchDelete" :disabled="!multipleSelectionSystem.length">
          <el-icon><Delete /></el-icon>
          删除{{ multipleSelectionSystem.length ? ' (' + multipleSelectionSystem.length + ')' : '' }}
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="parameter-tabs">
      <el-tab-pane label="样式参数" name="style">
    <el-table
          :data="filteredStyleParameters"
          v-loading="loading"
          :highlight-current-row="false"
      class="data-table"
          @row-click="handleRowClick"
          @selection-change="handleStyleSelectionChange"
    >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="50" />
      <el-table-column prop="name" label="参数名称" />
      <el-table-column prop="key" label="参数键" />
      <el-table-column prop="value" label="参数值">
        <template #default="{ row }">
              <div v-if="row.type === '颜色'" class="color-value-display">
                <div class="color-block" :style="{ backgroundColor: row.value }"></div>
                <span class="color-text">{{ row.value }}</span>
              </div>
              <div v-else-if="row.type === '布尔值'" class="boolean-value-display">
                <el-tag :type="row.value === 'true' ? 'success' : 'info'" size="small" effect="dark">
                  {{ row.value === 'true' ? '是' : '否' }}
                </el-tag>
              </div>
              <div v-else-if="row.type === '数字'" class="number-value-display">
                <span>{{ row.value }}</span>
              </div>
              <div v-else class="text-value-display">
                <span>{{ row.value }}</span>
              </div>
        </template>
      </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" type="primary" class="edit-button" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
              </div>
            </template>
          </el-table-column>
    </el-table>
      </el-tab-pane>
      <el-tab-pane label="系统参数" name="system">
        <el-table
          :data="filteredSystemParameters"
          v-loading="loading"
          :highlight-current-row="false"
          class="data-table" 
          @row-click="handleRowClick"
          @selection-change="handleSystemSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="50" />
          <el-table-column prop="name" label="参数名称" />
          <el-table-column prop="key" label="参数键" />
          <el-table-column prop="value" label="参数值">
            <template #default="{ row }">
              <div v-if="row.type === '颜色'" class="color-value-display">
                <div class="color-block" :style="{ backgroundColor: row.value }"></div>
                <span class="color-text">{{ row.value }}</span>
              </div>
              <div v-else-if="row.type === '布尔值'" class="boolean-value-display">
                <el-tag :type="row.value === 'true' ? 'success' : 'info'" size="small" effect="dark">
                  {{ row.value === 'true' ? '是' : '否' }}
                </el-tag>
              </div>
              <div v-else-if="row.type === '数字'" class="number-value-display">
                <span>{{ row.value }}</span>
              </div>
              <div v-else class="text-value-display">
                <span>{{ row.value }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" type="primary" class="edit-button" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        background
      />
    </div>

    <!-- 添加参数对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="添加参数"
      width="80%"
      destroy-on-close
      center
      class="edit-parameter-dialog"
    >
      <el-form :model="newParameter" label-width="120px" :rules="rules" ref="paramForm" class="parameter-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="参数组" prop="group">
              <el-select v-model="newParameter.group" placeholder="请选择参数组" style="width: 100%">
                <el-option label="样式参数" value="style" />
                <el-option label="系统参数" value="system" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数类型" prop="type">
              <el-select v-model="newParameter.type" placeholder="请选择参数类型" style="width: 100%">
                <el-option label="字符串" value="字符串" />
                <el-option label="数字" value="数字" />
                <el-option label="布尔值" value="布尔值" />
                <el-option label="颜色" value="颜色" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="参数名称" prop="name">
              <el-input v-model="newParameter.name" placeholder="请输入参数名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数键" prop="key">
              <el-input v-model="newParameter.key" placeholder="请输入参数键" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="参数值" prop="value">
          <el-color-picker 
            v-if="newParameter.type === '颜色'" 
            v-model="newParameter.value" 
            show-alpha 
            size="large"
            color-format="hex"
            style="margin-right: 10px;"
          />
          <span v-if="newParameter.type === '颜色'" style="color: #e1f2ff; margin-left: 10px;">
            选择的颜色值: {{ newParameter.value }}
          </span>
          <el-switch 
            v-else-if="newParameter.type === '布尔值'" 
            v-model="newParameter.boolValue" 
            active-text="开启"
            inactive-text="关闭"
            inline-prompt
          />
          <el-input-number 
            v-else-if="newParameter.type === '数字'" 
            v-model="newParameter.numValue" 
            :min="0" 
            :max="9999" 
            controls-position="right"
            class="number-input"
            style="width: 100%;"
          />
          <el-input 
            v-else 
            v-model="newParameter.value" 
            placeholder="请输入参数值" 
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="newParameter.description" 
            type="textarea" 
            placeholder="请输入参数描述" 
            rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="dialogVisible = false" plain>取消</el-button>
          <el-button size="large" type="primary" @click="submitForm" :loading="saving">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑参数对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑参数"
      width="80%"
      destroy-on-close
      center
      class="edit-parameter-dialog"
    >
      <el-form :model="editingParameter" label-width="120px" :rules="rules" ref="editForm" class="parameter-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="参数组" prop="group">
              <el-select v-model="editingParameter.group" placeholder="请选择参数组" style="width: 100%">
                <el-option label="样式参数" value="style" />
                <el-option label="系统参数" value="system" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数类型" prop="type">
              <el-select v-model="editingParameter.type" placeholder="请选择参数类型" style="width: 100%">
                <el-option label="字符串" value="字符串" />
                <el-option label="数字" value="数字" />
                <el-option label="布尔值" value="布尔值" />
                <el-option label="颜色" value="颜色" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="参数名称" prop="name">
              <el-input v-model="editingParameter.name" placeholder="请输入参数名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数键" prop="key">
              <el-input v-model="editingParameter.key" placeholder="请输入参数键" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="参数值" prop="value">
          <el-color-picker 
            v-if="editingParameter.type === '颜色'" 
            v-model="editingParameter.value" 
            show-alpha 
            size="large"
            color-format="hex"
            style="margin-right: 10px;"
          />
          <span v-if="editingParameter.type === '颜色'" style="color: #e1f2ff; margin-left: 10px;">
            选择的颜色值: {{ editingParameter.value }}
          </span>
          <el-switch 
            v-else-if="editingParameter.type === '布尔值'" 
            v-model="editingParameter.boolValue" 
            active-text="开启"
            inactive-text="关闭"
            inline-prompt
          />
          <el-input-number 
            v-else-if="editingParameter.type === '数字'" 
            v-model="editingParameter.numValue" 
            :min="0" 
            :max="9999" 
            controls-position="right"
            class="number-input"
            style="width: 100%;"
          />
          <el-input 
            v-else 
            v-model="editingParameter.value" 
            placeholder="请输入参数值" 
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="editingParameter.description" 
            type="textarea" 
            placeholder="请输入参数描述" 
            rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="editDialogVisible = false" plain>取消</el-button>
          <el-button size="large" type="primary" @click="submitEditForm" :loading="saving">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch, inject } from 'vue'
import { 
  Search, 
  Check, 
  RefreshLeft,
  Plus,
  Delete,
  Edit
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { resolveImagePath } from '@/utils/imageUtils'

// 配置axios
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';
axios.defaults.timeout = 15000;

// 注入全局状态更新函数
const updateGlobalState = inject('updateGlobalState', null)

// 参数状态
const styleParameters = ref([])
const systemParameters = ref([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('style')
const originalParams = ref({}) // 用于存储原始参数值，以便重置
const multipleSelectionStyle = ref([]) // 保存样式参数的多选数据
const multipleSelectionSystem = ref([]) // 保存系统参数的多选数据

// 添加参数对话框
const dialogVisible = ref(false)
const paramForm = ref(null)
const newParameter = reactive({
  group: 'style',
  name: '',
  key: '',
  type: '字符串',
  value: '',
  boolValue: false,
  numValue: 0,
  description: ''
})

// 编辑参数对话框
const editDialogVisible = ref(false)
const editForm = ref(null)
const editingParameter = reactive({
  id: 0,
  group: '',
  name: '',
  key: '',
  type: '',
  value: '',
  boolValue: false,
  numValue: 0,
  description: ''
})

// 表单验证规则
const rules = {
  group: [{ required: true, message: '请选择参数组', trigger: 'change' }],
  name: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  key: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  type: [{ required: true, message: '请选择参数类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
  description: [{ required: true, message: '请输入参数描述', trigger: 'blur' }]
}

// 初始化参数数据
onMounted(() => {
  fetchParameters()
})

// 获取参数数据
const fetchParameters = async () => {
  loading.value = true
  try {
    // 构建请求参数
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    
    // 如果有活动标签，添加组别参数
    if (activeTab.value) {
      params.group = activeTab.value
    }
    
    // 发送请求获取数据
    const response = await axios.get('/system/parameter/list', { params })
    console.log('API响应:', response.data)
    
    // 检查状态 - 注意API可能返回"sucess"或"success"
    if (response.data.status === 'success' || response.data.status === 'sucess') {
      // 处理参数数据，添加UI所需的额外属性
      const processedParams = response.data.rows.map(param => {
        const processedParam = { ...param }
        
        // 为布尔值和数字类型的参数添加特殊处理
        if (processedParam.type === '布尔值') {
          processedParam.boolValue = processedParam.value === 'true'
        } else if (processedParam.type === '数字') {
          processedParam.numValue = parseFloat(processedParam.value)
        }
        
        // 为每个参数添加一个changed标记
        processedParam.changed = false
        return processedParam
      })
      
      console.log('处理后的参数:', processedParams)
      
      // 清空当前参数列表，避免累加
      styleParameters.value = []
      systemParameters.value = []
      
      // 根据组别分配参数到不同的数组
      processedParams.forEach(param => {
        console.log(`参数 ${param.name} 的group值:`, param.group)
        if (param.group === 'style') {
          styleParameters.value.push(param)
        } else if (param.group === 'system') {
          systemParameters.value.push(param)
        } else {
          console.warn(`参数 ${param.name} 的group值无效:`, param.group)
        }
      })
      
      console.log('样式参数:', styleParameters.value)
      console.log('系统参数:', systemParameters.value)
      
      // 更新总数
      total.value = response.data.total
      
      // 保存原始参数，用于重置
      saveOriginalParams()
      
      // 应用系统参数值到应用程序
      applySystemParameters()
    } else {
      ElMessage.error('获取参数列表失败：' + (response.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('获取参数列表出错:', error)
    ElMessage.error('获取参数列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 应用系统参数到应用程序
const applySystemParameters = () => {
  // 应用颜色参数到CSS变量
  systemParameters.value.forEach(param => {
    if (param.type === '颜色') {
      if (param.key === 'system.color.primary') {
        document.documentElement.style.setProperty('--el-color-primary', param.value)
      } else if (param.key === 'system.color.success') {
        document.documentElement.style.setProperty('--el-color-success', param.value)
      } else if (param.key === 'system.color.warning') {
        document.documentElement.style.setProperty('--el-color-warning', param.value)
      } else if (param.key === 'system.color.danger') {
        document.documentElement.style.setProperty('--el-color-danger', param.value)
      } else if (param.key === 'system.color.info') {
        document.documentElement.style.setProperty('--el-color-info', param.value)
      } else if (param.key === 'system.color.border') {
        document.documentElement.style.setProperty('--el-border-color', param.value)
      }
    }
  })
}

// 保存原始参数值
const saveOriginalParams = () => {
  originalParams.value = {}
  
  for (const param of styleParameters.value) {
    originalParams.value[param.id] = { ...param }
  }
  
  for (const param of systemParameters.value) {
    originalParams.value[param.id] = { ...param }
  }
}

// 监听标签页变化
watch(activeTab, () => {
  currentPage.value = 1 // 切换标签时重置为第一页
  fetchParameters() // 重新获取数据
})

// 过滤样式参数列表
const filteredStyleParameters = computed(() => {
  if (!searchQuery.value) {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return styleParameters.value.slice(start, end)
  }
  
  const filtered = styleParameters.value.filter(param => 
    param.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    param.key.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    param.description.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
  
  return filtered
})

// 过滤系统参数列表
const filteredSystemParameters = computed(() => {
  if (!searchQuery.value) {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return systemParameters.value.slice(start, end)
  }
  
  const filtered = systemParameters.value.filter(param => 
    param.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    param.key.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    param.description.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
  
  return filtered
})

// 处理参数值变更
const handleValueChange = (row) => {
  row.changed = true
  
  // 如果是数字类型，同时更新numValue
  if (row.type === '数字') {
    row.numValue = parseInt(row.value) || 0
  }
  
  // 如果是布尔类型，同时更新boolValue
  if (row.type === '布尔值') {
    row.boolValue = row.value === 'true'
  }
  
  // 如果是颜色类型，并且是特定的系统色彩，更新CSS变量
  if (row.type === '颜色') {
    // 处理颜色实时预览
    if (row.key === 'system.color.primary') {
      document.documentElement.style.setProperty('--el-color-primary', row.value)
    } else if (row.key === 'system.color.success') {
      document.documentElement.style.setProperty('--el-color-success', row.value)
    } else if (row.key === 'system.color.warning') {
      document.documentElement.style.setProperty('--el-color-warning', row.value)
    } else if (row.key === 'system.color.danger') {
      document.documentElement.style.setProperty('--el-color-danger', row.value)
    } else if (row.key === 'system.color.info') {
      document.documentElement.style.setProperty('--el-color-info', row.value)
    } else if (row.key === 'system.color.border') {
      document.documentElement.style.setProperty('--el-border-color', row.value)
    }
  }
}

// 处理布尔值变化
const handleBoolChange = (row) => {
  row.changed = true
  row.value = row.boolValue.toString()
}

// 处理数字值变化
const handleNumChange = (row) => {
  row.changed = true
  row.value = row.numValue.toString()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1 // 重置为第一页
  fetchParameters()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchParameters()
}

// 保存单个参数
const handleSave = async (row) => {
  if (!row.changed) return
  
  saving.value = true
  try {
    // 直接使用参数对象，不再包装在parameter字段中
    const paramData = { ...row }
    
    // 打印详细的请求参数
    console.log('准备发送保存参数请求:', {
      url: '/system/parameter',
      method: 'PUT',
      data: JSON.stringify(paramData)
    })
    
    // 发送保存参数的请求 - 使用PUT方法、正确的URL和明确的Content-Type
    const response = await axios.put('/system/parameter', 
      JSON.stringify(paramData), 
      { 
        headers: { 
          'Content-Type': 'application/json'
        }
      }
    )
    
    // 打印完整的响应信息
    console.log('参数保存响应:', {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
      data: response.data
    })
    
    // 同时接受'success'和'sucess'（拼写错误的成功状态）
    if (response.data.status === 'success' || response.data.status === 'sucess') {
      console.log('保存参数成功:', row)
  row.changed = false
      
      // 如果是系统名称，更新全局状态
      if (row.key === 'system.name' && updateGlobalState) {
        updateGlobalState({ systemName: row.value })
        ElMessage.success(`系统名称已更新为 ${row.value}`)
      }
      // 如果是系统LOGO，也更新全局状态
      else if (row.key === 'system.logo.url' && updateGlobalState) {
        // 使用图片工具函数处理路径
        updateGlobalState({ logoUrl: row.value })
        ElMessage.success(`系统LOGO已更新，新路径: ${row.value}`)
      } else {
  ElMessage.success(`参数 ${row.name} 保存成功`)
      }
    } else {
      console.error('保存参数失败:', response.data)
      ElMessage.error('保存参数失败：' + (response.data.message || '未知错误'))
    }
  } catch (error) {
    // 打印更详细的错误信息
    console.error('保存参数出错:', error)
    
    if (error.response) {
      // 服务器响应了，但状态码超出了2xx范围
      console.error('服务器响应:', {
        status: error.response.status,
        statusText: error.response.statusText,
        headers: error.response.headers,
        data: error.response.data
      })
    } else if (error.request) {
      // 请求已发送但没有收到响应
      console.error('未收到响应，请求详情:', error.request)
    } else {
      // 设置请求时发生了一些事情，触发了错误
      console.error('请求配置错误:', error.config)
    }
    
    ElMessage.error('保存参数失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 保存所有参数
const handleSaveAll = async () => {
  const changedStyleParams = styleParameters.value.filter(param => param.changed)
  const changedSystemParams = systemParameters.value.filter(param => param.changed)
  const changedParams = [...changedStyleParams, ...changedSystemParams]
  
  if (changedParams.length === 0) {
    ElMessage.info('没有需要保存的参数')
    return
  }
  
  console.log('准备批量保存的参数:', changedParams)
  
  saving.value = true
  try {
    // 构建所有请求 - 直接使用参数对象
    const savePromises = changedParams.map(param => {
      const paramData = { ...param }
      
      console.log(`准备发送参数 ${param.name} (id=${param.id}) 的保存请求:`, {
        url: '/system/parameter',
        method: 'PUT',
        data: JSON.stringify(paramData)
      })
      
      return axios.put(
        '/system/parameter', 
        JSON.stringify(paramData), 
        { 
          headers: { 
            'Content-Type': 'application/json'
          }
        }
      )
    })
    
    console.log(`开始发送 ${savePromises.length} 个保存请求`)
    
    const responses = await Promise.all(savePromises)
    
    // 记录每个响应
    responses.forEach((res, index) => {
      const param = changedParams[index]
      console.log(`参数 ${param.name} (id=${param.id}) 的保存响应:`, {
        status: res.status,
        data: res.data
      })
    })
    
    // 检查所有响应 - 接受两种拼写
    const allSuccess = responses.every(res => res.data.status === 'success' || res.data.status === 'sucess')
    const successCount = responses.filter(res => res.data.status === 'success' || res.data.status === 'sucess').length
    
    console.log(`保存结果: ${successCount}/${responses.length} 成功, 全部成功: ${allSuccess}`)
    
    if (allSuccess) {
      console.log('批量保存参数成功:', changedParams)
      
      // 检查是否有系统名称或LOGO参数需要更新全局状态
      if (updateGlobalState) {
        const systemNameParam = changedParams.find(p => p.key === 'system.name')
        const logoUrlParam = changedParams.find(p => p.key === 'system.logo.url')
        
        const updates = {}
        if (systemNameParam) {
          updates.systemName = systemNameParam.value
        }
        
        if (logoUrlParam) {
          // 直接使用用户输入的路径，工具函数会在全局状态中处理
          updates.logoUrl = logoUrlParam.value
        }
        
        if (Object.keys(updates).length > 0) {
          console.log('准备更新全局状态:', updates)
          updateGlobalState(updates)
          
          // 提示更新的参数
          if (systemNameParam && logoUrlParam) {
            ElMessage.success('系统名称和LOGO路径已更新')
          } else if (systemNameParam) {
            ElMessage.success(`系统名称已更新为 ${systemNameParam.value}`)
          } else if (logoUrlParam) {
            ElMessage.success(`系统LOGO已更新，新路径: ${logoUrlParam.value}`)
          }
        }
      }
      
      // 标记所有参数为未更改
    changedParams.forEach(param => param.changed = false)
    ElMessage.success(`成功保存 ${changedParams.length} 个参数`)
  } else {
      // 找出失败的响应
      const failedResponses = responses.filter(res => res.data.status !== 'success' && res.data.status !== 'sucess')
      const failedParams = failedResponses.map((res, idx) => {
        return {
          parameter: changedParams[idx],
          response: res.data
        }
      })
      
      console.error('部分参数保存失败:', failedParams)
      ElMessage.error(`部分参数保存失败，共 ${failedResponses.length} 个失败`)
    }
  } catch (error) {
    console.error('批量保存参数出错:', error)
    
    if (error.response) {
      console.error('服务器响应:', {
        status: error.response.status,
        statusText: error.response.statusText,
        headers: error.response.headers,
        data: error.response.data
      })
    } else if (error.request) {
      console.error('未收到响应，请求详情:', error.request)
    } else {
      console.error('请求配置错误:', error.config)
    }
    
    ElMessage.error('保存参数失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 重置参数
const handleReset = () => {
  ElMessageBox.confirm(
    '确定要重置所有修改过的参数吗？',
    '重置确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    // 从原始数据恢复参数
    styleParameters.value = styleParameters.value.map(param => {
      if (param.changed && originalParams.value[param.id]) {
        return { ...originalParams.value[param.id] }
      }
      return param
    })
    
    systemParameters.value = systemParameters.value.map(param => {
      if (param.changed && originalParams.value[param.id]) {
        return { ...originalParams.value[param.id] }
      }
      return param
    })
    
  ElMessage.warning('已重置所有参数修改')
  }).catch(() => {
    ElMessage.info('已取消重置')
  })
}

// 添加参数
const handleAddParameter = () => {
  // 重置表单
  newParameter.name = ''
  newParameter.key = ''
  newParameter.type = '字符串'
  newParameter.value = ''
  newParameter.boolValue = false
  newParameter.numValue = 0
  newParameter.description = ''
  
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  if (!paramForm.value) return
  
  await paramForm.value.validate(async (valid, fields) => {
    if (valid) {
      saving.value = true
      try {
        // 根据参数类型处理值
        if (newParameter.type === '布尔值') {
          newParameter.value = newParameter.boolValue.toString()
        } else if (newParameter.type === '数字') {
          newParameter.value = newParameter.numValue.toString()
        }
        
        // 创建新参数对象 - 按照API要求的格式
        const paramData = {
          name: newParameter.name,
          key: newParameter.key,
          value: newParameter.value,
          description: newParameter.description,
          group: newParameter.group
        }
        
        // 打印详细的请求参数
        console.log('准备发送添加参数请求:', {
          url: '/system/parameter',
          method: 'POST',
          data: JSON.stringify(paramData)
        })
        
        // 发送添加参数的请求
        const response = await axios.post(
          '/system/parameter', 
          JSON.stringify(paramData), 
          { 
            headers: { 
              'Content-Type': 'application/json'
            }
          }
        )
        
        // 打印完整的响应信息
        console.log('添加参数响应:', {
          status: response.status,
          statusText: response.statusText,
          headers: response.headers,
          data: response.data
        })
        
        // 检查响应状态 - 同时接受两种拼写
        if (response.data.status === 'success' || response.data.status === 'sucess') {
          console.log('添加参数成功:', response.data)
          
          // 获取新参数的ID，如果返回数据有ID就使用，否则使用临时ID
          const newParamId = response.data.data?.id || Date.now()
          
          // 完整的新参数对象
          const param = {
            id: newParamId,
            name: newParameter.name,
            key: newParameter.key,
            value: newParameter.value,
            type: newParameter.type,
            description: newParameter.description,
            changed: false,
            group: newParameter.group
          }
          
          // 添加特殊字段
          if (newParameter.type === '布尔值') {
            param.boolValue = newParameter.boolValue
          } else if (newParameter.type === '数字') {
            param.numValue = newParameter.numValue
            param.min = 0
            param.max = 9999
          }
          
          // 添加到对应的参数列表
          if (param.group === 'style') {
            styleParameters.value.push(param)
          } else {
            systemParameters.value.push(param)
          }
          
          // 保存原始参数
          originalParams.value[param.id] = { ...param }
          
          // 更新总数
          total.value++
          
          // 如果是系统名称或LOGO，更新全局状态
          if (param.key === 'system.name' && updateGlobalState) {
            updateGlobalState({ systemName: param.value })
            ElMessage.success(`系统名称已设置为 ${param.value}`)
          } else if (param.key === 'system.logo.url' && updateGlobalState) {
            updateGlobalState({ logoUrl: param.value })
            ElMessage.success(`系统LOGO已设置，路径: ${param.value}`)
          } else {
            ElMessage.success(`参数 ${param.name} 添加成功`)
          }
          
          dialogVisible.value = false
        } else {
          console.error('添加参数失败:', response.data)
          ElMessage.error('添加参数失败：' + (response.data.message || '未知错误'))
        }
      } catch (error) {
        console.error('添加参数出错:', error)
        
        if (error.response) {
          console.error('服务器响应:', {
            status: error.response.status,
            statusText: error.response.statusText,
            headers: error.response.headers,
            data: error.response.data
          })
        } else if (error.request) {
          console.error('未收到响应，请求详情:', error.request)
        } else {
          console.error('请求配置错误:', error.config)
        }
        
        ElMessage.error('添加参数失败: ' + (error.message || '未知错误'))
      } finally {
        saving.value = false
      }
    } else {
      console.log('表单验证失败:', fields)
    }
  })
}

// 处理样式参数表格选择变化
const handleStyleSelectionChange = (selection) => {
  multipleSelectionStyle.value = selection
}

// 处理系统参数表格选择变化
const handleSystemSelectionChange = (selection) => {
  multipleSelectionSystem.value = selection
}

// 处理批量删除
const handleBatchDelete = () => {
  // 获取当前活动标签页的选择项
  const selection = activeTab.value === 'style' ? multipleSelectionStyle.value : multipleSelectionSystem.value
  
  if (selection.length === 0) {
    ElMessage.warning('请选择要删除的参数')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selection.length} 个参数吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 创建所有删除请求
      const deletePromises = selection.map(param => {
        console.log(`准备删除参数 ${param.name} (id=${param.id}):`, {
          url: '/system/parameter',
          method: 'DELETE',
          params: { id: param.id }
        })
        
        return axios.delete('/system/parameter', {
          params: { id: param.id }
        })
      })
      
      console.log(`开始发送 ${deletePromises.length} 个删除请求`)
      
      // 执行所有请求
      const responses = await Promise.all(deletePromises)
      
      // 记录每个响应
      responses.forEach((res, index) => {
        const param = selection[index]
        console.log(`参数 ${param.name} (id=${param.id}) 的删除响应:`, {
          status: res.status,
          data: res.data
        })
      })
      
      // 检查所有响应 - 同时接受两种拼写
      const allSuccess = responses.every(res => res.data.status === 'success' || res.data.status === 'sucess')
      const successCount = responses.filter(res => res.data.status === 'success' || res.data.status === 'sucess').length
      
      console.log(`删除结果: ${successCount}/${responses.length} 成功, 全部成功: ${allSuccess}`)
      
      if (allSuccess) {
        // 从列表中移除
        if (activeTab.value === 'style') {
          styleParameters.value = styleParameters.value.filter(p => !selection.some(s => s.id === p.id))
          multipleSelectionStyle.value = [] // 清空选择
        } else {
          systemParameters.value = systemParameters.value.filter(p => !selection.some(s => s.id === p.id))
          multipleSelectionSystem.value = [] // 清空选择
        }
        
        // 从原始参数中移除
        selection.forEach(param => {
          delete originalParams.value[param.id]
        })
        
        // 更新总数
        total.value -= selection.length
        
        ElMessage.success(`成功删除 ${selection.length} 个参数`)
      } else {
        // 部分成功的处理
        if (successCount > 0) {
          // 找出成功的请求对应的参数
          const successParams = [];
          responses.forEach((res, idx) => {
            if (res.data.status === 'success' || res.data.status === 'sucess') {
              successParams.push(selection[idx]);
            }
          });
          
          // 从列表中移除成功删除的参数
          if (activeTab.value === 'style') {
            styleParameters.value = styleParameters.value.filter(p => !successParams.some(s => s.id === p.id))
            multipleSelectionStyle.value = multipleSelectionStyle.value.filter(p => !successParams.some(s => s.id === p.id))
          } else {
            systemParameters.value = systemParameters.value.filter(p => !successParams.some(s => s.id === p.id))
            multipleSelectionSystem.value = multipleSelectionSystem.value.filter(p => !successParams.some(s => s.id === p.id))
          }
          
          // 从原始参数中移除
          successParams.forEach(param => {
            delete originalParams.value[param.id]
          })
          
          // 更新总数
          total.value -= successCount
          
          ElMessage.warning(`部分删除成功：${successCount}/${selection.length} 个参数已删除，其余删除失败`)
        } else {
          ElMessage.error('所有参数删除失败')
        }
      }
    } catch (error) {
      console.error('批量删除参数出错:', error)
      
      if (error.response) {
        console.error('服务器响应:', {
          status: error.response.status,
          statusText: error.response.statusText,
          headers: error.response.headers,
          data: error.response.data
        })
      } else if (error.request) {
        console.error('未收到响应，请求详情:', error.request)
      } else {
        console.error('请求配置错误:', error.config)
      }
      
      ElMessage.error('删除参数失败: ' + (error.message || '未知错误'))
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 处理表格行点击
const handleRowClick = (row, column, event) => {
  // 检查点击的不是按钮
  if (event.target.tagName === 'BUTTON' || 
      event.target.closest('button') ||
      event.target.classList.contains('el-switch') ||
      event.target.closest('.el-switch') ||
      event.target.classList.contains('el-color-picker') ||
      event.target.closest('.el-color-picker') ||
      event.target.classList.contains('el-input-number') ||
      event.target.closest('.el-input-number')) {
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

// 处理编辑参数
const handleEdit = (row) => {
  // 复制参数到编辑对象
  editingParameter.id = row.id
  editingParameter.group = row.group
  editingParameter.name = row.name
  editingParameter.key = row.key
  editingParameter.type = row.type
  editingParameter.value = row.value
  editingParameter.description = row.description
  
  // 特殊类型处理
  if (row.type === '布尔值') {
    editingParameter.boolValue = row.value === 'true'
  } else if (row.type === '数字') {
    editingParameter.numValue = parseFloat(row.value) || 0
  }
  
  // 显示编辑对话框
  editDialogVisible.value = true
}

// 提交编辑表单
const submitEditForm = async () => {
  if (!editForm.value) return
  
  await editForm.value.validate(async (valid, fields) => {
    if (valid) {
      saving.value = true
      try {
        // 根据参数类型处理值
        if (editingParameter.type === '布尔值') {
          editingParameter.value = editingParameter.boolValue.toString()
        } else if (editingParameter.type === '数字') {
          editingParameter.value = editingParameter.numValue.toString()
        }
        
        // 准备更新的参数对象
        const updatedParam = {
          id: editingParameter.id,
          name: editingParameter.name,
          key: editingParameter.key,
          value: editingParameter.value,
          type: editingParameter.type,
          description: editingParameter.description,
          group: editingParameter.group
        }
        
        // 打印详细的请求参数
        console.log('准备发送保存参数请求:', {
          url: '/system/parameter',
          method: 'PUT',
          data: JSON.stringify(updatedParam)
        })
        
        // 发送保存参数的请求
        const response = await axios.put('/system/parameter', 
          JSON.stringify(updatedParam), 
          { 
            headers: { 
              'Content-Type': 'application/json'
            }
          }
        )
        
        // 打印完整的响应信息
        console.log('参数保存响应:', {
          status: response.status,
          statusText: response.statusText,
          headers: response.headers,
          data: response.data
        })
        
        // 检查响应状态
        if (response.data.status === 'success' || response.data.status === 'sucess') {
          console.log('保存参数成功:', updatedParam)
          
          // 更新列表中的参数
          const updateInArray = (arr) => {
            const index = arr.findIndex(item => item.id === updatedParam.id)
            if (index !== -1) {
              // 特殊类型处理
              if (updatedParam.type === '布尔值') {
                updatedParam.boolValue = updatedParam.value === 'true'
              } else if (updatedParam.type === '数字') {
                updatedParam.numValue = parseFloat(updatedParam.value)
              }
              
              // 更新参数
              updatedParam.changed = false
              arr[index] = { ...updatedParam }
              
              // 更新原始参数
              originalParams.value[updatedParam.id] = { ...updatedParam }
              
              return true
            }
            return false
          }
          
          // 在样式参数和系统参数列表中查找并更新
          const updatedInStyle = updateInArray(styleParameters.value)
          const updatedInSystem = !updatedInStyle && updateInArray(systemParameters.value)
          
          // 如果参数组发生变化，可能需要移动参数
          if (!updatedInStyle && !updatedInSystem) {
            if (updatedParam.group === 'style') {
              styleParameters.value.push({ ...updatedParam })
              // 从系统参数中移除
              const index = systemParameters.value.findIndex(item => item.id === updatedParam.id)
              if (index !== -1) {
                systemParameters.value.splice(index, 1)
              }
            } else {
              systemParameters.value.push({ ...updatedParam })
              // 从样式参数中移除
              const index = styleParameters.value.findIndex(item => item.id === updatedParam.id)
              if (index !== -1) {
                styleParameters.value.splice(index, 1)
              }
            }
          }
          
          // 如果是系统名称或LOGO，更新全局状态
          if (updatedParam.key === 'system.name' && updateGlobalState) {
            updateGlobalState({ systemName: updatedParam.value })
            ElMessage.success(`系统名称已更新为 ${updatedParam.value}`)
          } else if (updatedParam.key === 'system.logo.url' && updateGlobalState) {
            updateGlobalState({ logoUrl: updatedParam.value })
            ElMessage.success(`系统LOGO已更新，新路径: ${updatedParam.value}`)
          } else {
            ElMessage.success(`参数 ${updatedParam.name} 保存成功`)
          }
          
          // 应用系统参数
          applySystemParameters()
          
          // 关闭对话框
          editDialogVisible.value = false
        } else {
          console.error('保存参数失败:', response.data)
          ElMessage.error('保存参数失败：' + (response.data.message || '未知错误'))
        }
      } catch (error) {
        console.error('编辑参数出错:', error)
        
        if (error.response) {
          console.error('服务器响应:', {
            status: error.response.status,
            statusText: error.response.statusText,
            headers: error.response.headers,
            data: error.response.data
          })
        } else if (error.request) {
          console.error('未收到响应，请求详情:', error.request)
        } else {
          console.error('请求配置错误:', error.config)
        }
        
        ElMessage.error('保存参数失败: ' + (error.message || '未知错误'))
      } finally {
        saving.value = false
      }
    } else {
      console.log('表单验证失败:', fields)
    }
  })
}

// 处理删除参数
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除参数 "${row.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 打印详细的请求参数
      console.log('准备发送删除参数请求:', {
        url: '/system/parameter',
        method: 'DELETE',
        params: { id: row.id }
      })
      
      // 发送删除请求，使用params传递id参数
      const response = await axios.delete('/system/parameter', {
        params: { id: row.id }
      })
      
      // 打印完整的响应信息
      console.log('删除参数响应:', {
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
        data: response.data
      })
      
      // 检查响应状态 - 同时接受两种拼写
      if (response.data.status === 'success' || response.data.status === 'sucess') {
        // 从列表中移除
        if (row.group === 'style') {
          styleParameters.value = styleParameters.value.filter(p => p.id !== row.id)
          multipleSelectionStyle.value = multipleSelectionStyle.value.filter(p => p.id !== row.id)
        } else {
          systemParameters.value = systemParameters.value.filter(p => p.id !== row.id)
          multipleSelectionSystem.value = multipleSelectionSystem.value.filter(p => p.id !== row.id)
        }
        
        // 从原始参数中移除
        delete originalParams.value[row.id]
        
        // 更新总数
        total.value--
        
        ElMessage.success(`成功删除参数 "${row.name}"`)
      } else {
        ElMessage.error('删除参数失败：' + (response.data.message || '未知错误'))
      }
    } catch (error) {
      console.error('删除参数出错:', error)
      
      if (error.response) {
        console.error('服务器响应:', {
          status: error.response.status,
          statusText: error.response.statusText,
          headers: error.response.headers,
          data: error.response.data
        })
      } else if (error.request) {
        console.error('未收到响应，请求详情:', error.request)
      } else {
        console.error('请求配置错误:', error.config)
      }
      
      ElMessage.error('删除参数失败: ' + (error.message || '未知错误'))
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}
</script>

<style scoped>
.parameters-container {
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

.search-bar {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

.search-input :deep(.el-input__inner) {
  color: white;
}

.table-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.table-actions :deep(.el-button) {
  min-width: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.table-actions :deep(.el-button--success) {
  background: linear-gradient(135deg, #28c76f, #16a75c);
  border: none;
  box-shadow: 0 2px 8px rgba(40, 199, 111, 0.4);
}

.table-actions :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #33d683, #1fc569);
  box-shadow: 0 4px 12px rgba(40, 199, 111, 0.6);
  transform: translateY(-2px);
}

.table-actions :deep(.el-button--danger) {
  background: linear-gradient(135deg, #ea5455, #c53f40);
  border: none;
  box-shadow: 0 2px 8px rgba(234, 84, 85, 0.4);
}

.table-actions :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #f26364, #d84a4b);
  box-shadow: 0 4px 12px rgba(234, 84, 85, 0.6);
  transform: translateY(-2px);
}

.table-actions :deep(.el-button--danger:disabled) {
  opacity: 0.6;
  background: linear-gradient(135deg, #9e5455, #8a3f40);
  box-shadow: none;
  cursor: not-allowed;
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

/* 标签页样式 */
.parameter-tabs {
  margin-bottom: 20px;
}

:deep(.el-tabs__item) {
  color: #a0cfff;
  font-size: 16px;
  padding: 0 24px;
  transition: all 0.3s ease;
  height: 50px;
  line-height: 50px;
}

:deep(.el-tabs__item:hover) {
  color: #54d8ff;
}

:deep(.el-tabs__item.is-active) {
  color: #00e0ff;
  font-weight: bold;
  text-shadow: 0 0 10px rgba(0, 224, 255, 0.5);
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #0091ff, #00e0ff);
  height: 3px;
  border-radius: 3px;
  box-shadow: 0 0 10px rgba(0, 224, 255, 0.5);
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(14, 89, 134, 0.3);
  height: 1px;
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
  border: none;
  box-shadow: 0 4px 10px rgba(0, 130, 255, 0.3);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #00a1ff, #0077ff);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 145, 255, 0.4);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #ff9500, #e67e00);
  border: none;
  box-shadow: 0 4px 10px rgba(255, 149, 0, 0.3);
}

:deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, #ffa520, #ff8800);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 149, 0, 0.4);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #00c48c, #00a170);
  border: none;
  box-shadow: 0 4px 10px rgba(0, 196, 140, 0.3);
}

:deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #00d49a, #00b180);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 196, 140, 0.4);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff5252, #e63939);
  border: none;
  box-shadow: 0 4px 10px rgba(255, 82, 82, 0.3);
}

:deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #ff6b6b, #ff4747);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 82, 82, 0.4);
}

/* 表格样式 */
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
  background-color: transparent !important;
  border-left: none !important;
  transform: none !important;
  box-shadow: none !important;
}

/* 点击表格行时的动画效果 */
@keyframes rowPulse {
  0% {
    box-shadow: 0 0 0 rgba(0, 224, 255, 0);
    background-color: rgba(0, 145, 234, 0.1);
  }
  50% {
    box-shadow: 0 0 15px rgba(0, 224, 255, 0.5);
    background-color: rgba(0, 145, 234, 0.2);
  }
  100% {
    box-shadow: 0 0 0 rgba(0, 224, 255, 0);
    background-color: rgba(0, 30, 60, 0.3);
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

/* 颜色选择器样式 */
:deep(.el-color-picker__trigger) {
  border-color: rgba(14, 89, 134, 0.7);
  width: 42px;
  height: 42px;
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
}

:deep(.el-color-picker__trigger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 5px 12px rgba(0, 0, 0, 0.3);
  border-color: rgba(0, 183, 255, 0.7);
}

:deep(.custom-color-picker) {
  margin: 0 auto;
}

/* 开关样式 */
:deep(.el-switch__core) {
  border-color: rgba(14, 89, 134, 0.7);
  background-color: rgba(0, 30, 60, 0.5);
}

:deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(90deg, #0091ff, #00b7ff);
  box-shadow: 0 0 10px rgba(0, 145, 255, 0.3);
}

:deep(.el-switch__action) {
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}

:deep(.el-switch__inner) {
  color: #e1f2ff;
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.5);
}

/* 数字输入框样式 */
.number-input {
  width: 100%;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background-color: rgba(0, 40, 80, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: #e1f2ff;
  transition: all 0.2s ease;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  background-color: rgba(0, 60, 120, 0.8);
  color: white;
}

/* 对话框样式 */
:deep(.el-dialog) {
  background-color: rgba(0, 20, 40, 0.95);
  border: 1px solid #0e5986;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(10px);
  overflow: visible;
  margin: 5vh auto !important;
  display: flex;
  flex-direction: column;
  position: relative;
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

/* 表单样式 */
.parameter-form {
  max-width: 600px;
  margin: 0 auto;
}

:deep(.el-form-item__label) {
  color: #e1f2ff;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.3);
}

:deep(.el-select .el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
}

:deep(.el-select-dropdown__item) {
  color: #e1f2ff;
}

:deep(.el-select-dropdown__item.selected) {
  color: #00e0ff;
  font-weight: bold;
}

:deep(.el-select-dropdown__item:hover) {
  background-color: rgba(0, 145, 234, 0.2);
}

:deep(.el-textarea__inner) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: white;
  border-radius: 6px;
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

/* 对话框按钮样式 */
.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  width: 100%;
  margin-top: 10px;
}

.dialog-footer :deep(.el-button) {
  min-width: 120px;
  font-size: 16px;
  padding: 12px 24px;
  border-radius: 6px;
  transition: all 0.3s ease;
  letter-spacing: 1px;
}

.logo-input-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.path-suggestions {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
  background: rgba(0, 30, 60, 0.3);
  padding: 8px;
  border-radius: 6px;
  border-left: 3px solid rgba(0, 183, 255, 0.5);
}

.path-suggestions span {
  color: #a0cfff;
  white-space: nowrap;
  text-shadow: 0 0 5px rgba(0, 183, 255, 0.3);
}

@media (max-width: 768px) {
  .parameters-container {
    padding: 16px;
  }
  
  .action-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar {
    max-width: 100%;
  }
  
  .table-actions {
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

.parameter-form {
  animation: fadeIn 0.3s ease-out;
}

.data-table :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: linear-gradient(135deg, #00a1ff, #0077ff);
  border-color: #0091ff;
  box-shadow: 0 0 8px rgba(0, 145, 255, 0.5);
  transform: scale(1.1);
  transition: all 0.3s ease;
}

.data-table :deep(.el-checkbox__inner) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  transition: all 0.2s ease;
}

.data-table :deep(.el-checkbox__inner:hover) {
  border-color: #00e0ff;
}

/* 编辑参数对话框样式 */
:deep(.edit-parameter-dialog .el-dialog__body) {
  padding: 30px;
  overflow: visible;
  max-height: none;
}

:deep(.edit-parameter-dialog) {
  margin-top: 8vh !important;
}

:deep(.edit-parameter-dialog .el-dialog__header) {
  padding: 20px 30px;
}

:deep(.edit-parameter-dialog .el-dialog__footer) {
  padding: 20px 30px 30px;
}

:deep(.edit-parameter-dialog .el-form-item) {
  margin-bottom: 22px;
}

:deep(.edit-parameter-dialog .el-input__wrapper),
:deep(.edit-parameter-dialog .el-textarea__inner) {
  background-color: rgba(0, 20, 40, 0.8);
  border-color: rgba(14, 89, 134, 0.8);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.8);
}

:deep(.edit-parameter-dialog .el-input__inner) {
  height: 40px;
  color: #e1f2ff;
  font-size: 14px;
}

:deep(.edit-parameter-dialog .el-textarea__inner) {
  color: #e1f2ff;
  font-size: 14px;
}

:deep(.edit-parameter-dialog .el-form-item__label) {
  font-size: 15px;
  color: #e1f2ff;
  font-weight: 500;
}

:deep(.edit-parameter-dialog .dialog-footer .el-button) {
  height: 44px;
  font-size: 16px;
}

.color-value-display {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.color-block {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.color-text {
  color: #e1f2ff;
  font-size: 14px;
  background: rgba(0, 30, 60, 0.5);
  padding: 3px 8px;
  border-radius: 4px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.boolean-value-display {
  display: flex;
  justify-content: center;
}

.boolean-value-display :deep(.el-tag) {
  min-width: 60px;
  display: flex;
  justify-content: center;
  font-weight: 500;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.number-value-display {
  display: flex;
  justify-content: center;
  color: #00e0ff;
  font-weight: 500;
  font-size: 15px;
  text-shadow: 0 0 5px rgba(0, 224, 255, 0.3);
}

.text-value-display {
  display: flex;
  justify-content: center;
  color: #e1f2ff;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0 auto;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.edit-button {
  background: linear-gradient(135deg, #0091ff, #0055d4);
  border: none;
  box-shadow: 0 2px 8px rgba(0, 145, 255, 0.4);
  transition: all 0.3s ease;
}

.edit-button:hover {
  background: linear-gradient(135deg, #00a9ff, #0066ff);
  box-shadow: 0 4px 12px rgba(0, 145, 255, 0.6);
  transform: translateY(-2px);
}

.edit-button:active {
  box-shadow: 0 1px 4px rgba(0, 145, 255, 0.3);
  transform: translateY(0);
}
</style> 