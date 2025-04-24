<template>
  <div class="users-container">
    <div class="page-header">
      <h2>用户信息</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>
    </div>

    <div class="action-row">
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索用户..."
          class="search-input"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="small" @click="handleSearch" style="margin-left: 8px">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
      <div class="table-actions">
        <el-button type="success" size="small" @click="handleExportSelected">
          <el-icon><Download /></el-icon>
          {{ selectedIds.length === 0 ? '全部导出' : `导出 ${selectedIds.length > 0 ? `(${selectedIds.length})` : ''}` }}
        </el-button>
        <el-button type="warning" size="small" @click="fetchUserList">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
        <el-button type="danger" size="small" :disabled="selectedIds.length === 0" @click="handleDeleteSelected">
          <el-icon><Delete /></el-icon>
          删除 <span v-if="selectedIds.length > 0">({{ selectedIds.length }})</span>
        </el-button>
      </div>
    </div>

    <div class="table-container">
      <el-table
        :data="userList"
        style="width: 100%"
        class="user-table"
        :max-height="tableHeight"
        v-loading="loading"
        element-loading-text="加载中..."
        element-loading-background="rgba(0, 0, 0, 0.7)"
        @selection-change="handleSelectionChange"
        ref="tableRef"
        row-key="uid"
        :reserve-selection="true"
        @rendered="onTableRendered"
      >
        <el-table-column type="selection" width="55" :reserve-selection="true" :selectable="rowSelectable" />
        <el-table-column prop="uid" label="ID" width="180" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column label="权限" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getAuthorityTagType(row.authority)">
              {{ getAuthorityLabel(row.authority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
                编辑
              </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="30%"
      class="delete-dialog"
    >
      <div>
        <p>确定要删除选中的 {{ selectedIds.length }} 条记录吗？此操作不可恢复！</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmDelete">确定删除</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 新增用户对话框 -->
    <el-dialog
      v-model="addUserDialogVisible"
      title="新增用户"
      width="40%"
      class="user-dialog"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userFormRules"
        label-width="80px"
        status-icon
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="userForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="权限" prop="authorityLabel">
          <el-select v-model="userForm.authorityLabel" placeholder="请选择权限">
            <el-option label="普通用户" value="普通用户" />
            <el-option label="管理员" value="管理员" />
            <el-option label="超级管理员" value="超级管理员" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addUserDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUserForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="editUserDialogVisible"
      title="编辑用户"
      width="40%"
      class="user-dialog"
    >
      <el-form
        ref="editUserFormRef"
        :model="editUserForm"
        :rules="editUserFormRules"
        label-width="80px"
        status-icon
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editUserForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="editUserForm.password" type="password" placeholder="不修改请留空" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="editUserForm.confirmPassword" type="password" placeholder="不修改请留空" show-password></el-input>
        </el-form-item>
        <el-form-item label="权限" prop="authorityLabel">
          <el-select v-model="editUserForm.authorityLabel" placeholder="请选择权限">
            <el-option label="普通用户" value="普通用户" />
            <el-option label="管理员" value="管理员" />
            <el-option label="超级管理员" value="超级管理员" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editUserDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEditUserForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive, nextTick, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search as IconSearch, 
  Download as IconDownload, 
  RefreshRight as IconRefresh, 
  Plus as IconPlus, 
  Edit as IconEdit, 
  Delete as IconDelete
} from '@element-plus/icons-vue'

// 导入xlsx库，使用try-catch处理导入失败的情况
let XLSX = null
// 在组件初始化时不立即加载xlsx库，仅在需要时动态导入

const userList = ref([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableHeight = ref('400px')
const loading = ref(false)
const selectedRows = ref([])
const selectedIds = ref([])
const deleteDialogVisible = ref(false)
const addUserDialogVisible = ref(false)
const editUserDialogVisible = ref(false)
const userFormRef = ref(null)
const editUserFormRef = ref(null)
const tableRef = ref(null)

// 计算当前有多少选中项，用于UI显示
const selectedCount = computed(() => selectedIds.value.length)

// 用户表单数据
const userForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  authorityLabel: ''
})

// 编辑用户表单数据
const editUserForm = reactive({
  uid: '',
  username: '',
  password: '',
  confirmPassword: '',
  authorityLabel: ''
})

// 表单校验规则
const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== userForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  authorityLabel: [
    { required: true, message: '请选择权限', trigger: 'change' }
  ]
}

// 编辑用户表单校验规则
const editUserFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3到20个字符', trigger: 'blur' }
  ],
  password: [
    // 编辑时密码可以为空，表示不修改
  ],
  confirmPassword: [
    { 
      validator: (rule, value, callback) => {
        if (editUserForm.password && value !== editUserForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  authorityLabel: [
    { required: true, message: '请选择权限', trigger: 'change' }
  ]
}

// 权限标签映射到数字
const authorityMap = {
  '普通用户': '1',
  '管理员': '2',
  '超级管理员': '3'
}

// 获取权限标签文本
const getAuthorityLabel = (authority) => {
  switch (String(authority)) {
    case '1': return '普通用户'
    case '2': return '管理员'
    case '3': return '超级管理员'
    default: return '未知权限'
  }
}

// 获取权限标签样式类型
const getAuthorityTagType = (authority) => {
  switch (String(authority)) {
    case '1': return 'info'
    case '2': return 'success'
    case '3': return 'danger'
    default: return 'warning'
  }
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  // 直接保存选中的行
  selectedRows.value = selection
  // 保存选中行的ID
  selectedIds.value = selection.map(row => row.uid)
  
  console.log('当前选中项数量:', selectedIds.value.length)

  // 每次选择变化后，刷新表头复选框状态
  updateAllSelected()
}

// 判断行是否可选，始终返回true允许选择
const rowSelectable = (row) => {
  return true
}

// 刷新表头复选框状态，使其只反映当前页面选择状态
const updateAllSelected = () => {
  if (!tableRef.value) return
  
  try {
    // 获取当前页面所有行的ID
    const currentPageRowIds = userList.value.map(row => row.uid)
    
    // 计算当前页面选中的行数
    const currentPageSelectedCount = selectedIds.value.filter(id => 
      currentPageRowIds.includes(id)
    ).length

    // 获取表头复选框元素
    const headerCheckbox = tableRef.value.$el.querySelector('.el-table__header-wrapper .el-checkbox')
    const headerCheckboxInput = tableRef.value.$el.querySelector('.el-table__header-wrapper .el-checkbox input')
    
    // 确保元素存在后再操作
    if (headerCheckbox && headerCheckboxInput) {
      if (currentPageSelectedCount === 0) {
        // 当前页没有选中行，清除表头全选状态
        headerCheckboxInput.indeterminate = false
        headerCheckbox.classList.remove('is-checked')
      } else if (currentPageSelectedCount === userList.value.length) {
        // 当前页全选，设置表头全选状态
        headerCheckboxInput.indeterminate = false
        headerCheckbox.classList.add('is-checked')
      } else {
        // 当前页部分选中，设置表头半选状态
        headerCheckboxInput.indeterminate = true
      }
    }
  } catch (error) {
    console.error('更新表头复选框状态失败:', error)
  }
}

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    
    // 如果有搜索关键词，添加到请求参数
    if (searchQuery.value) {
      params.username = searchQuery.value
    }
    
    const response = await axios.get('/system/user/list', { params })
    
    if (response.data.status === 'success') {
      userList.value = response.data.rows
      total.value = response.data.total
    } else {
      ElMessage.error('获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUserList()
  
  // 页码变化后也需要更新表头复选框状态
  nextTick(() => {
    updateAllSelected()
  })
}

// 处理每页记录数变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1 // 重置为第一页
  fetchUserList()
  
  // 每页条数变化后也需要更新表头复选框状态
  nextTick(() => {
    updateAllSelected()
  })
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1 // 重置为第一页
  fetchUserList()
}

// 处理导出选中记录
const handleExportSelected = async () => {
  // 如果没有选中记录，调用全部导出接口
  if (selectedIds.value.length === 0) {
    try {
      loading.value = true
      ElMessage.info('正在请求导出所有用户数据，请稍候...')
      
      // 使用axios发送GET请求获取Excel文件
      const response = await axios.get('/system/user/export', {
        responseType: 'blob', // 重要：设置响应类型为blob
      })
      
      // 检查是否成功获取了Excel文件
      if (response.status === 200 && response.data) {
        const blob = new Blob([response.data], { 
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
        })
        
        // 创建下载链接并触发下载
        const downloadLink = document.createElement('a')
        const url = window.URL.createObjectURL(blob)
        
        // 从响应头获取文件名，如果没有则使用默认文件名
        const contentDisposition = response.headers['content-disposition']
        let filename = 'users.xlsx'
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename=(.+)/)
          if (filenameMatch && filenameMatch.length > 1) {
            filename = filenameMatch[1]
          }
        }
        
        downloadLink.href = url
        downloadLink.download = filename
        document.body.appendChild(downloadLink)
        downloadLink.click()
        document.body.removeChild(downloadLink)
        window.URL.revokeObjectURL(url)
        
        ElMessage.success('成功导出全部用户数据')
      } else {
        throw new Error('导出请求成功但未收到有效的文件数据')
      }
    } catch (error) {
      console.error('导出全部用户失败:', error)
      ElMessage.error('导出失败，请重试')
    } finally {
      loading.value = false
    }
    return
  }
  
  // 以下是选中记录的导出逻辑
  try {
    // 动态导入XLSX库
    if (!XLSX) {
      try {
        XLSX = await import('xlsx')
      } catch (e) {
        console.error('xlsx库加载失败，导出功能将不可用:', e)
        ElMessage.error('导出功能不可用，请确保已安装xlsx库')
        return
      }
    }
    
    loading.value = true
    
    // 获取当前页面上可见的选中用户数据
    const visibleSelectedUsers = userList.value.filter(user => 
      selectedIds.value.includes(user.uid)
    )
    
    // 如果有不在当前页面的选中用户，需要单独请求
    const invisibleCount = selectedIds.value.length - visibleSelectedUsers.length
    
    // 所有导出用户数据
    let allExportUsers = [...visibleSelectedUsers]
    
    // 如果有不在当前页面的用户，需要从服务器获取
    if (invisibleCount > 0) {
      ElMessage.info(`正在获取${invisibleCount}条不在当前页面的记录...`)
      
      // 找出不在当前页的用户ID
      const invisibleUserIds = selectedIds.value.filter(id => 
        !visibleSelectedUsers.some(user => user.uid === id)
      )
      
      // 这里理想情况应该有一个批量获取用户信息的API
      // 但如果没有，可以单独请求每个用户信息
      try {
        const fetchPromises = invisibleUserIds.map(async (uid) => {
          try {
            // 这里可能需要修改为实际的API端点
            const response = await axios.get(`/system/user?uid=${uid}`)
            if (response.data && response.data.status === 'success') {
              return response.data.user || response.data.data || null
            }
            return null
          } catch (err) {
            console.error(`获取用户ID=${uid}的信息失败:`, err)
            return null
          }
        })
        
        const fetchedUsers = await Promise.all(fetchPromises)
        const validFetchedUsers = fetchedUsers.filter(user => user !== null)
        
        // 合并所有用户数据
        allExportUsers = [...allExportUsers, ...validFetchedUsers]
      } catch (err) {
        console.error('获取额外用户信息失败:', err)
        ElMessage.warning(`只能导出 ${visibleSelectedUsers.length} 条可见记录，${invisibleCount} 条不可见记录无法获取`)
      }
    }
    
    if (allExportUsers.length === 0) {
      ElMessage.error('没有可导出的数据')
      return
    }
    
    // 格式化数据用于导出
    const exportData = allExportUsers.map(user => {
      return {
        '用户ID': user.uid,
        '用户名': user.username,
        '用户密码': user.password || '******', // 尝试使用实际密码，如果不存在则显示掩码
        '用户角色': user.authority // 直接使用数字值(1,2,3)而不是文本描述
      }
    })
    
    // 创建工作簿和工作表
    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '用户列表')
    
    // 设置列宽
    const columnWidths = [
      { wch: 20 }, // 用户ID列宽
      { wch: 15 }, // 用户名列宽
      { wch: 15 }, // 用户密码列宽
      { wch: 10 }  // 用户角色列宽
    ]
    worksheet['!cols'] = columnWidths
    
    // 生成并下载Excel文件
    const fileName = `用户数据_${new Date().toISOString().split('T')[0]}.xlsx`
    XLSX.writeFile(workbook, fileName)
    
    ElMessage.success(`成功导出 ${allExportUsers.length} 条记录`)
  } catch (error) {
    console.error('导出Excel失败:', error)
    ElMessage.error('导出Excel失败，请重试')
  } finally {
    loading.value = false
  }
}

// 处理批量删除
const handleDeleteSelected = () => {
  deleteDialogVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  try {
    // 获取所有选中的用户ID
    const selectedUserIds = [...selectedIds.value]
    console.log('删除以下ID的记录:', selectedUserIds)
    
    // 显示加载状态
    loading.value = true
    
    // 批量删除结果计数
    let successCount = 0
    let failCount = 0
    
    // 逐个删除选中的用户
    for (const uid of selectedUserIds) {
      try {
        // 调用删除API
        const response = await axios({
          method: 'DELETE',
          url: `/system/user?uid=${uid}`,
          redirect: 'follow'
        })
        
        if (response.data && response.data.status === 'success') {
          successCount++
        } else {
          failCount++
          console.error('删除失败:', response.data?.msg || '未知错误')
        }
      } catch (err) {
        failCount++
        console.error(`删除ID为${uid}的用户失败:`, err)
      }
    }
    
    // 关闭对话框
    deleteDialogVisible.value = false
    
    // 显示结果消息
    if (successCount > 0 && failCount === 0) {
      ElMessage.success(`成功删除 ${successCount} 条记录`)
    } else if (successCount > 0 && failCount > 0) {
      ElMessage.warning(`成功删除 ${successCount} 条记录，${failCount} 条记录删除失败`)
    } else {
      ElMessage.error('删除失败，请重试')
    }
    
    // 强制清空所有选择状态
    if (tableRef.value) {
      tableRef.value.clearSelection()
    }
    
    // 清空选择数组
    selectedIds.value = []
    selectedRows.value = []
    
    // 重新加载数据
    await fetchUserList()
    
    console.log('已重置选择状态，当前选中数量: 0')
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('删除失败，请重试')
  } finally {
    loading.value = false
  }
}

// 重置用户表单
const resetUserForm = () => {
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
  userForm.username = ''
  userForm.password = ''
  userForm.confirmPassword = ''
  userForm.authorityLabel = ''
}

// 处理新增用户
const handleAdd = () => {
  resetUserForm()
  addUserDialogVisible.value = true
}

// 提交用户表单
const submitUserForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        loading.value = true
        
        // 将权限标签转换为数字
        const authority = authorityMap[userForm.authorityLabel]
        
        const userData = {
          username: userForm.username,
          password: userForm.password,
          authority: authority
        }
        
        const response = await axios.post('/system/user', userData, {
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (response.data.status === 'success') {
          ElMessage.success(response.data.msg || '新建用户成功')
          addUserDialogVisible.value = false
          fetchUserList() // 刷新用户列表
        } else {
          ElMessage.error(response.data.msg || '新建用户失败')
        }
      } catch (error) {
        console.error('新增用户出错:', error)
        ElMessage.error('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    } else {
      console.log('表单验证失败', fields)
    }
  })
}

// 处理编辑用户
const handleEdit = (row) => {
  // 重置表单
  resetEditUserForm()
  
  // 填充表单数据
  editUserForm.uid = row.uid
  editUserForm.username = row.username
  editUserForm.authorityLabel = getAuthorityLabel(row.authority)
  
  // 显示编辑对话框
  editUserDialogVisible.value = true
}

// 重置编辑用户表单
const resetEditUserForm = () => {
  if (editUserFormRef.value) {
    editUserFormRef.value.resetFields()
  }
  editUserForm.uid = ''
  editUserForm.username = ''
  editUserForm.password = ''
  editUserForm.confirmPassword = ''
  editUserForm.authorityLabel = ''
}

// 提交编辑用户表单
const submitEditUserForm = async () => {
  if (!editUserFormRef.value) return
  
  await editUserFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        loading.value = true
        
        // 将权限标签转换为数字
        const authority = authorityMap[editUserForm.authorityLabel]
        
        // 构建请求数据对象
        const userData = {
          uid: editUserForm.uid,
          username: editUserForm.username,
          authority: authority
        }
        
        // 如果用户输入了密码，则添加到请求数据中
        if (editUserForm.password) {
          userData.password = editUserForm.password
        }
        
        console.log('准备提交修改的用户数据:', userData)
        
        // 使用fetch API完全匹配示例
        const myHeaders = new Headers();
        myHeaders.append("Content-Type", "application/json");
        
        const requestOptions = {
          method: "PUT",
          headers: myHeaders,
          body: JSON.stringify(userData),
          redirect: "follow"
        };
        
        const response = await fetch("/system/user", requestOptions);
        const resultText = await response.text();
        let result;
        
        try {
          // 尝试解析JSON响应
          result = JSON.parse(resultText);
        } catch (e) {
          console.error('响应不是有效的JSON:', resultText);
          result = { status: 'fail', msg: '服务器响应格式错误' };
        }
        
        console.log('服务器响应:', result);
        
        if (result && result.status === 'success') {
          ElMessage.success(result.msg || '修改用户成功');
          editUserDialogVisible.value = false;
          
          // 强制清空所有选择状态
          if (tableRef.value) {
            tableRef.value.clearSelection();
          }
          
          // 清空选择数组
          selectedIds.value = [];
          selectedRows.value = [];
          
          // 刷新用户列表
          fetchUserList();
        } else {
          ElMessage.error(result?.msg || '修改用户失败');
        }
      } catch (error) {
        console.error('编辑用户出错:', error);
        ElMessage.error('网络错误，请稍后重试');
      } finally {
        loading.value = false;
      }
    } else {
      console.log('表单验证失败', fields);
    }
  });
}

// 计算表格高度
const calculateTableHeight = () => {
  const windowHeight = window.innerHeight
  const headerHeight = 60 // 页面头部高度
  const pageHeaderHeight = 70 // 页头高度
  const actionRowHeight = 60 // 操作栏高度
  const paginationHeight = 60 // 分页高度
  const padding = 80 // 内边距总和
  
  const availableHeight = windowHeight - headerHeight - pageHeaderHeight - actionRowHeight - paginationHeight - padding
  tableHeight.value = `${Math.max(availableHeight, 300)}px`
}

// 监听窗口大小变化以调整表格高度
onMounted(() => {
  calculateTableHeight()
  window.addEventListener('resize', calculateTableHeight)
  window.addEventListener('app-resize', calculateTableHeight)
  
  // 初始加载用户列表
  fetchUserList()
})

onUnmounted(() => {
  window.removeEventListener('resize', calculateTableHeight)
  window.removeEventListener('app-resize', calculateTableHeight)
})

// 添加表格更新完成时的回调函数，确保状态同步
const onTableRendered = () => {
  // 表格渲染完成后，恢复选中状态
  if (tableRef.value) {
    // 如果选中项为空，确保清除所有选择
    if (selectedIds.value.length === 0) {
      tableRef.value.clearSelection()
      updateAllSelected()
      return
    }
    
    // 先清除当前选择状态
    tableRef.value.clearSelection()
    
    // 根据selectedIds选中对应行
    userList.value.forEach(row => {
      if (selectedIds.value.includes(row.uid)) {
        tableRef.value.toggleRowSelection(row, true)
      }
    })
    
    // 更新表头复选框状态
    updateAllSelected()
  }
}
</script>

<style scoped>
.users-container {
  padding: 24px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 12px;
  border: 1px solid #0e5986;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(5px);
  border-radius: 5px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.page-header h2 {
  color: #00ffff;
  font-size: 22px;
  margin: 0;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  letter-spacing: 1px;
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
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
  max-width: 300px;
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

.table-container {
  flex: 1;
  overflow: hidden;
  margin-bottom: 20px;
  min-height: 0; /* 确保flex布局正确计算高度 */
}

.user-table {
  --el-table-border-color: rgba(14, 89, 134, 0.5);
  --el-table-header-bg-color: rgba(0, 40, 80, 0.6);
  --el-table-row-hover-bg-color: rgba(0, 145, 234, 0.2);
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 10px;
  background-color: rgba(0, 20, 40, 0.4);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3), 0 0 20px rgba(0, 100, 234, 0.1);
  transition: all 0.3s ease;
}

.user-table:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.15);
}

/* 添加表格内部线条 */
.user-table :deep(.el-table__inner-wrapper) {
  border: 1px solid #0e5986;
  border-radius: 10px;
  overflow: hidden;
}

.user-table :deep(.el-table__cell) {
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
  padding: 12px 8px;
}

.user-table :deep(.el-table__column-filter-trigger) {
  margin-left: 8px;
}

.user-table :deep(.el-table__header) {
  background-color: rgba(0, 40, 80, 0.7);
  background-image: linear-gradient(to right, rgba(0, 60, 120, 0.7), rgba(0, 40, 80, 0.7));
}

.user-table :deep(.el-table__header th) {
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

.user-table :deep(.el-table__header th::after) {
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

.user-table :deep(.el-table__header th:hover::after) {
  transform: translateY(0);
}

.user-table :deep(.el-table__header th:hover) {
  background-image: linear-gradient(to bottom, rgba(0, 80, 160, 0.9), rgba(0, 50, 100, 0.7));
  color: #ffffff;
}

.user-table :deep(.el-table__header th .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  font-size: 15px;
  text-shadow: 0 0 5px rgba(0, 224, 255, 0.3);
}

.user-table :deep(.el-table__header-wrapper) {
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
  position: sticky;
  top: 0;
  z-index: 10;
}

.user-table :deep(.el-table__row) {
  background-color: rgba(0, 30, 60, 0.3);
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
}

.user-table :deep(.el-table__row td) {
  border-right: 1px solid rgba(14, 89, 134, 0.2);
  text-align: center;
  padding: 14px 12px;
  height: 55px;
  color: #e1f5ff;
  font-weight: 400;
}

.user-table :deep(.el-table__row:hover td) {
  color: #ffffff;
}

/* 增强ID和用户名列的样式 */
.user-table :deep([prop="uid"] .cell),
.user-table :deep([prop="username"] .cell) {
  color: #ffffff;
  font-weight: 500;
  font-size: 15px;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 6px;
}

.user-table :deep([prop="uid"] .cell::before),
.user-table :deep([prop="username"] .cell::before) {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: linear-gradient(to right, rgba(0, 183, 255, 0.1), transparent);
  border-left: 2px solid #00a1ff;
  border-radius: 4px;
  opacity: 0.5;
  transition: all 0.3s ease;
}

/* 确保hover状态下字体更明显 */
.user-table :deep(.el-table__row:hover [prop="uid"] .cell),
.user-table :deep(.el-table__row:hover [prop="username"] .cell) {
  color: #00ffff;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.6);
  transform: translateX(2px);
  font-weight: 600;
}

.user-table :deep(.el-table__row:hover [prop="uid"] .cell::before),
.user-table :deep(.el-table__row:hover [prop="username"] .cell::before) {
  opacity: 0.8;
  border-left: 3px solid #00ffff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.4);
}

.user-table :deep(.el-table__row td .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.user-table :deep(.el-table__row:nth-child(even)) {
  background-color: rgba(0, 35, 70, 0.3);
}

.user-table :deep(.el-table__row:hover) {
  background-color: rgba(0, 145, 234, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.user-table :deep(.el-table__row.current-row) {
  background-color: rgba(0, 145, 234, 0.3);
  box-shadow: 0 0 15px rgba(0, 145, 234, 0.2);
}

.user-table :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.user-table :deep(.el-table--border),
.user-table :deep(.el-table--group) {
  border: 1px solid #0e5986;
  border-radius: 8px;
  overflow: hidden;
}

.user-table :deep(.el-table__fixed-right),
.user-table :deep(.el-table__fixed-left) {
  height: 100% !important;
  background-color: rgba(0, 30, 60, 0.4);
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
}

.user-table :deep(.el-table__fixed-right::before),
.user-table :deep(.el-table__fixed::before) {
  background-color: rgba(14, 89, 134, 0.5);
}

/* 表格空数据样式 */
.user-table :deep(.el-table__empty-block) {
  background: rgba(0, 30, 60, 0.3);
  min-height: 150px;
}

.user-table :deep(.el-table__empty-text) {
  color: #a0cfff;
  font-size: 15px;
  background: rgba(0, 40, 80, 0.5);
  padding: 15px 30px;
  border-radius: 8px;
  border: 1px dashed rgba(14, 89, 134, 0.7);
}

/* 表格加载动画样式 */
.user-table :deep(.el-loading-mask) {
  background-color: rgba(0, 20, 40, 0.7);
  backdrop-filter: blur(5px);
}

.user-table :deep(.el-loading-spinner .path) {
  stroke: #00e0ff;
  stroke-width: 3;
}

.user-table :deep(.el-loading-text) {
  color: #00e0ff;
  font-size: 15px;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 224, 255, 0.5);
  margin-top: 10px;
}

/* 新增表格选择框样式 */
.user-table :deep(.el-table-column--selection .el-checkbox) {
  margin-right: 0;
}

.user-table :deep(.el-checkbox__inner) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
}

.user-table :deep(.el-checkbox__inner:hover) {
  border-color: #00e0ff;
}

.user-table :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #00a1ff;
  border-color: #00a1ff;
}

/* 表格内标签样式 */
.user-table :deep(.el-tag) {
  border-radius: 4px;
  padding: 0 8px;
  height: 28px;
  line-height: 28px;
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  font-size: 14px;
  color: #ffffff;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

/* 添加不同权限标签的特定样式 */
.user-table :deep(.el-tag--info) {
  background-color: #4e5969;
  border-color: #4e5969;
  color: #ffffff;
}

.user-table :deep(.el-tag--success) {
  background-color: #28bd8b;
  border-color: #28bd8b;
  color: #ffffff;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.user-table :deep(.el-tag--danger) {
  background-color: #ff6b6b;
  border-color: #ff6b6b;
  color: #ffffff;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.user-table :deep(.el-tag--warning) {
  background-color: #e6a23c;
  border-color: #e6a23c;
  color: #ffffff;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 10px;
  background-color: rgba(0, 30, 60, 0.3);
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

/* 按钮组样式优化 */
.action-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.action-buttons .el-button {
  background-color: rgba(20, 60, 100, 0.6);
  border-color: rgba(30, 110, 160, 0.7);
  color: #e1f2ff;
  transition: all 0.3s;
  border-radius: 6px;
  padding: 8px 16px;
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.action-buttons .el-button:hover {
  background-color: rgba(32, 120, 190, 0.45);
  border-color: #00a1ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 145, 234, 0.3);
}

.action-buttons .el-button--primary {
  background-color: #00a1ff;
  border-color: #00a1ff;
  color: white;
}

.action-buttons .el-button--primary:hover {
  background-color: #0088e0;
  border-color: #0088e0;
}

.action-buttons .el-button--danger {
  background-color: rgba(220, 60, 60, 0.7);
  border-color: rgba(200, 40, 40, 0.7);
}

.action-buttons .el-button--danger:hover {
  background-color: rgba(240, 70, 70, 0.8);
  border-color: rgba(220, 40, 40, 0.8);
}

.action-buttons .el-button--success {
  background-color: rgba(60, 180, 100, 0.7);
  border-color: rgba(50, 160, 80, 0.7);
}

.action-buttons .el-button--success:hover {
  background-color: rgba(70, 200, 120, 0.8);
  border-color: rgba(60, 180, 100, 0.8);
}

.action-buttons .el-button--warning {
  background-color: rgba(220, 160, 40, 0.7);
  border-color: rgba(200, 140, 20, 0.7);
}

.action-buttons .el-button--warning:hover {
  background-color: rgba(240, 180, 60, 0.8);
  border-color: rgba(220, 160, 40, 0.8);
}

.action-buttons .el-button [class*=" el-icon-"],
.action-buttons .el-button [class^=el-icon-] {
  margin-right: 6px;
  vertical-align: middle;
}

/* 对话框样式 */
.delete-dialog :deep(.el-dialog),
.user-dialog :deep(.el-dialog) {
  background-color: rgba(0, 20, 40, 0.95);
  border: 1px solid #0e5986;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(10px);
  overflow: hidden;
}

.delete-dialog :deep(.el-dialog__header),
.user-dialog :deep(.el-dialog__header) {
  background: linear-gradient(90deg, rgba(0, 60, 120, 0.8), rgba(0, 40, 80, 0.8));
  color: #e1f2ff;
  border-bottom: 1px solid rgba(0, 183, 255, 0.3);
  padding: 20px 24px;
  margin: 0 !important;
  position: relative;
}

.delete-dialog :deep(.el-dialog__title),
.user-dialog :deep(.el-dialog__title) {
  color: #e1f2ff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.5);
}

.delete-dialog :deep(.el-dialog__headerbtn),
.user-dialog :deep(.el-dialog__headerbtn) {
  top: 18px;
  right: 20px;
}

.delete-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close),
.user-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #00ffff;
  transform: rotate(90deg);
  transition: all 0.3s ease;
}

.delete-dialog :deep(.el-dialog__body),
.user-dialog :deep(.el-dialog__body) {
  padding: 24px 24px 16px;
  color: #a0cfff;
}

.delete-dialog :deep(.el-dialog__footer),
.user-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(14, 89, 134, 0.5);
  background: rgba(0, 20, 40, 0.5);
}

.user-dialog :deep(.el-form-item__label) {
  color: #a0cfff;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.3);
}

.user-dialog :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.user-dialog :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.user-dialog :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

.user-dialog :deep(.el-input__inner),
.user-dialog :deep(.el-select-dropdown__item) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: #fff;
}

.user-dialog :deep(.el-input__inner:focus),
.user-dialog :deep(.el-select:hover .el-input__inner) {
  border-color: #00ffff;
}

.user-dialog :deep(.el-select-dropdown) {
  background-color: rgba(0, 24, 50, 0.95);
  border: 1px solid #0e5986;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3), 0 0 20px rgba(0, 100, 234, 0.2);
}

.user-dialog :deep(.el-select-dropdown__item.selected),
.user-dialog :deep(.el-select-dropdown__item.hover),
.user-dialog :deep(.el-select-dropdown__item:hover) {
  background-color: rgba(0, 140, 255, 0.2);
  color: #00e0ff;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .users-container {
    padding: 15px;
  }
  
  .page-header, .action-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .search-input, .table-actions {
    width: 100%;
    max-width: none;
  }
  
  .table-actions {
    justify-content: flex-end;
  }
  
  .pagination {
    justify-content: center;
  }
}

@media (max-width: 576px) {
  .users-container {
    padding: 10px;
  }
  
  .pagination :deep(.el-pagination__sizes) {
    display: none !important;
  }
  
  :deep(.el-button span) {
    display: none;
  }
  
  :deep(.el-button) {
    padding: 8px 10px;
  }
  
  :deep(.el-table .cell) {
    padding-left: 8px;
    padding-right: 8px;
  }
}

/* 居中弹窗按钮 */
.dialog-footer {
  display: flex;
  justify-content: center;
  width: 100%;
  gap: 20px;
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

.dialog-footer :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0091ff, #006be6);
  border: none;
  box-shadow: 0 5px 15px rgba(0, 145, 255, 0.4);
  position: relative;
  overflow: hidden;
}

.dialog-footer :deep(.el-button--primary)::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: all 0.8s ease;
}

.dialog-footer :deep(.el-button--primary:hover) {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 145, 255, 0.5);
  background: linear-gradient(135deg, #0099ff, #0077ff);
}

.dialog-footer :deep(.el-button--primary:hover)::before {
  left: 100%;
}

.dialog-footer :deep(.el-button--default) {
  border: 1px solid rgba(14, 89, 134, 0.7);
  background: rgba(0, 20, 40, 0.3);
  color: #a0cfff;
  position: relative;
  overflow: hidden;
}

.dialog-footer :deep(.el-button--default)::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: all 0.8s ease;
}

.dialog-footer :deep(.el-button--default:hover) {
  border-color: #00e0ff;
  color: #00e0ff;
  background: rgba(0, 224, 255, 0.1);
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(0, 224, 255, 0.3);
}

.dialog-footer :deep(.el-button--default:hover)::before {
  left: 100%;
}
</style> 