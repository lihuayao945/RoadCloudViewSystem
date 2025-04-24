<template>
  <div class="roles-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索角色..."
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

    <el-table
      :data="roleList"
      style="width: 100%"
      class="role-table"
      v-loading="loading"
      element-loading-text="加载中..."
      element-loading-background="rgba(0, 0, 0, 0.7)"
    >
      <el-table-column prop="roleid" label="ID" width="160" />
      <el-table-column prop="rolename" label="角色名称" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- 编辑角色弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      title="编辑角色信息" 
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="edit-form">
        <el-form :model="editForm" label-width="100px">
          <el-form-item label="角色ID">
            <el-input v-model="editForm.roleid" disabled />
          </el-form-item>
          <el-form-item label="角色名称">
            <el-input v-model="editForm.rolename" placeholder="请输入角色名称" />
          </el-form-item>
          <el-form-item label="角色权限">
            <el-tree
              ref="permissionTree"
              :data="permissionOptions"
              show-checkbox
              node-key="id"
              :default-expand-all="false"
              :props="{ label: 'name', children: 'children' }"
              @check="handlePermissionCheck"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEdit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增角色弹窗 -->
    <el-dialog 
      v-model="addDialogVisible" 
      title="新增角色" 
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="add-form">
        <el-form :model="addForm" label-width="100px" :rules="addFormRules" ref="addFormRef">
          <el-form-item label="角色英文名" prop="rolecode">
            <el-input v-model="addForm.rolecode" placeholder="请输入角色英文名" />
          </el-form-item>
          <el-form-item label="角色中文名" prop="rolename">
            <el-input v-model="addForm.rolename" placeholder="请输入角色中文名" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAdd" :loading="submitLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// 角色列表数据
const roleList = ref([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 弹窗相关变量
const dialogVisible = ref(false)
const editForm = ref({
  roleid: '',
  rolename: '',
  permissions: []
})

// 存储原始权限，用于比较
const originalPermissions = ref([])

// 新增角色弹窗相关变量
const addDialogVisible = ref(false)
const addForm = ref({
  rolecode: '',
  rolename: ''
})
const addFormRef = ref(null)
const submitLoading = ref(false)

// 权限树引用
const permissionTree = ref(null)

// 模拟权限数据
const permissionOptions = ref([
  {
    id: 'home',
    name: '首页',
    children: [
      { id: 37, name: '设备总览' },
      { id: 38, name: '设备健康分布' },
      { id: 39, name: '查询异常设备' },
      { id: 40, name: '根据Rcuid查询' },
      { id: 41, name: '根据车辆id查询' },
      { id: 42, name: '拓扑图填充' },
      { id: 43, name: '首页流量分析' },
      { id: 44, name: '根据时间范围统计车辆' },
      { id: 45, name: '获取交通事件' },
      { id: 46, name: '统计人、车数量和差值' },
      { id: 47, name: '地标路段实时流量排名' },
      { id: 48, name: '分段统计车辆类型和数量' },
      { id: 49, name: '统计车辆速度分布区间和数量' },
      { id: 50, name: '查询设备列表' },
      { id: 51, name: '分页查询车辆列表' }
    ]
  },
  {
    id: 'detail',
    name: '详细页',
    children: [
      { id: 52, name: '详细页流量分布曲线图' },
      { id: 53, name: '首页流量分析条形图' },
      { id: 54, name: '查询可建模数据' },
      { id: 55, name: '导出建模数据' },
      { id: 56, name: '导出模型文件' },
      { id: 57, name: '根据Rcuid路口信息' }
    ]
  },
  {
    id: 'admin',
    name: '后台管理页',
    children: [
      { 
        id: 'admin-user', 
        name: '用户管理',
        children: [
          { 
            id: 'admin-user-info', 
            name: '用户信息',
            children: [
              { id: 2, name: '查询用户列表' },
              { id: 3, name: '查询用户详细' },
              { id: 4, name: '修改用户' },
              { id: 5, name: '删除用户' },
              { id: 6, name: '新建用户' },
              { id: 7, name: '用户数据导出' }
            ] 
          },
          { 
            id: 'admin-user-role', 
            name: '角色管理',
            children: [
              { id: 58, name: '查询所有角色' },
              { id: 59, name: '创建角色' },
              { id: 60, name: '删除角色' },
              { id: 61, name: '查询所有权限' },
              { id: 62, name: '增加角色权限' },
              { id: 63, name: '删除角色权限' }
            ]
          }
        ]
      },
      { 
        id: 'admin-data', 
        name: '数据管理',
        children: [
          { 
            id: 'admin-data-vehicles', 
            name: '车辆信息',
            children: [
              { id: 15, name: '查询车辆列表' },
              { id: 16, name: '模糊查询车辆详细' },
              { id: 17, name: '车辆信息总览' },
              { id: 18, name: '导出车辆信息' },
              { id: 19, name: '删除车辆信息通过主键' },
              { id: 20, name: '删除车辆信息通过id' },
              { id: 21, name: '查询某车辆具体时间段信息' }
            ]
          },
          { 
            id: 'admin-data-rcu', 
            name: 'RCU感知对象',
            children: [
              { id: 22, name: '查询rcu设备信息' },
              { id: 23, name: '根据rcuid查询' },
              { id: 24, name: '根据objsflag查询' },
              { id: 25, name: '根据rcuid删除' },
              { id: 26, name: '根据objsflag删除' },
              { id: 27, name: '根据objsflag和objid删除' },
              { id: 28, name: '根据rcuid和时间导出' },
              { id: 29, name: '根据objsflags导出' }
            ]
          }
        ]
      },
      { 
        id: 'admin-system', 
        name: '系统管理',
        children: [
          { 
            id: 'admin-system-params', 
            name: '参数调整',
            children: [
              { id: 33, name:'获取参数列表'},
              { id: 34, name: '删除参数数据' },
              { id: 35, name: '修改参数' },
              { id: 36, name: '新增参数' }
            ]
          },
          { 
            id: 'admin-system-topics', 
            name: '主题订阅',
            children: [
              { id: 8, name: '获取Mqtt配置信息' },
              { id: 9, name: '设置Mqtt配置信息' },
              { id: 10, name: '开始订阅' },
              { id: 11, name: '停止订阅' },
              { id: 12, name: '分页查询' },
              { id: 13, name: '测试连接是否成功' },
              { id: 14, name: '导出消息列表' }
            ]
          },
          { 
            id: 'admin-system-logs', 
            name: '日志管理',
            children: [
              { id: 30, name: '查询用户日志' },
              { id: 31, name: '删除用户日志' },
              { id: 32, name: '导出数据库日志' },
              { id: 64, name: '清空日志' }
            ]
          }
        ]
      }
    ]
  }
])

// 模拟角色-权限关联数据
const mockRolePermissions = {
  1: ['home', 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 'detail', 52, 53, 54, 55, 56, 57, 'admin', 'admin-user', 'admin-user-info', 2, 3, 4, 5, 6, 7, 'admin-user-role', 58, 59, 60, 61, 62, 63, 'admin-data', 'admin-data-vehicles', 15, 16, 17, 18, 19, 20, 21, 'admin-data-rcu', 22, 23, 24, 25, 26, 27, 28, 29, 'admin-system', 'admin-system-params', 34, 35, 36, 'admin-system-topics', 8, 9, 10, 11, 12, 13, 14, 'admin-system-logs', 30, 31, 32], // 系统管理员拥有所有权限
  2: ['home', 37, 38, 46, 'detail', 52, 53, 'admin-user-info', 2, 3, 'admin-user-role', 58, 61, 'admin-data-vehicles', 15, 16, 17], // 普通用户拥有部分权限
  3: ['home', 37, 'detail', 52], // 访客只有查看权限
  4: ['home', 37, 38, 43, 46, 'detail', 52, 53, 54, 'admin-data-vehicles', 15, 16, 17, 'admin-data-rcu', 22, 23, 24, 'admin-system-logs', 30], // 数据分析师
  5: ['home', 51, 'detail', 52, 'admin-data-vehicles', 15, 16, 17, 18] // 车辆管理员
}

// 新增角色表单校验规则
const addFormRules = {
  rolecode: [
    { required: true, message: '请输入角色英文名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  rolename: [
    { required: true, message: '请输入角色中文名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
}

// 获取角色列表
const fetchRoleList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value.toString(),
      pageSize: pageSize.value.toString()
    }
    
    // 如果有搜索关键词，添加到请求参数
    if (searchQuery.value) {
      params.rolename = searchQuery.value
    }
    
    const response = await axios.get('/system/role', { params })
    
    if (response.data.status === 'success') {
      roleList.value = response.data.rows || []
      total.value = response.data.total || 0
    } else {
      ElMessage.error('获取角色列表失败')
      roleList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取角色列表出错:', error)
    ElMessage.error('网络错误，请稍后重试')
    roleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchRoleList()
}

// 处理每页记录数变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1 // 重置为第一页
  fetchRoleList()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1 // 重置为第一页
  fetchRoleList()
}

// 处理新增角色
const handleAdd = () => {
  addDialogVisible.value = true
  // 重置表单
  addForm.value = {
    rolecode: '',
    rolename: ''
  }
}

// 处理编辑角色
const handleEdit = async (row) => {
  console.log('编辑角色:', row)
  editForm.value = {
    roleid: row.roleid,
    rolename: row.rolename,
    permissions: []
  }
  
  // 打开弹窗
  dialogVisible.value = true
  
  try {
    // 从API获取角色权限
    const response = await axios.get('/system/role/permission', {
      params: { roleId: row.roleid }
    })
    
    if (response.data.status === 'success') {
      // 提取权限ID列表并转换为数字
      const rolePermissions = response.data.rows.map(item => {
        // 将字符串ID转换为数字
        const permId = parseInt(item.permissionid, 10)
        return isNaN(permId) ? item.permissionid : permId
      })
      console.log('获取到的角色权限:', rolePermissions)
      
      // 保存原始权限用于比较
      originalPermissions.value = [...rolePermissions]
      
      // 设置权限树的选中状态
      setTimeout(() => {
        if (permissionTree.value) {
          // 清除已有选择
          permissionTree.value.setCheckedKeys([])
          
          // 设置选中状态
          permissionTree.value.setCheckedKeys(rolePermissions)
          
          // 更新表单数据中的权限
          editForm.value.permissions = rolePermissions
        }
      }, 100)
    } else {
      // ElMessage.error('获取角色权限失败')
      // 失败时使用空数组
      originalPermissions.value = []
      editForm.value.permissions = []
    }
  } catch (error) {
    console.error('获取角色权限出错:', error)
    ElMessage.error('网络错误，请稍后重试')
    originalPermissions.value = []
    editForm.value.permissions = []
  }
}

// 处理权限选择变化
const handlePermissionCheck = (node, checkedInfo) => {
  // 只获取叶子节点（数字ID）
  const checkedNodes = permissionTree.value.getCheckedNodes(true) // true表示只获取叶子节点
  editForm.value.permissions = checkedNodes.map(node => node.id).filter(id => typeof id === 'number')
  console.log('选择的权限:', editForm.value.permissions)
}

// 提交编辑
const submitEdit = async () => {
  try {
    // 获取当前权限和原始权限
    const currentPermissions = editForm.value.permissions.filter(id => typeof id === 'number')
    
    // 过滤原始权限，只保留数字ID
    const originalNumericPermissions = originalPermissions.value.filter(id => typeof id === 'number' || (typeof id === 'string' && !isNaN(parseInt(id, 10))))
      .map(id => typeof id === 'string' ? parseInt(id, 10) : id)
    
    console.log('过滤后的原始权限:', originalNumericPermissions)
    
    // 找出新增的权限 (确保类型一致性比较)
    const addedPermissions = currentPermissions.filter(id => !originalNumericPermissions.includes(id))
    
    // 找出移除的权限 (确保类型一致性比较)
    const removedPermissions = originalNumericPermissions.filter(id => !currentPermissions.includes(id))
    
    console.log('角色ID:', editForm.value.roleid)
    console.log('角色名称:', editForm.value.rolename)
    console.log('原始权限(数字ID):', originalNumericPermissions)
    console.log('当前权限(数字ID):', currentPermissions)
    console.log('新增权限:', addedPermissions)
    console.log('移除权限:', removedPermissions)
    
    let successFlag = true
    let errorMsg = ''
    
    // 如果有新增的权限，发送POST请求
    if (addedPermissions.length > 0) {
      try {
        // 将新增权限转换为逗号分隔的字符串
        const permissionData = addedPermissions.join(',')
        
        // 创建表单数据
        const formData = new URLSearchParams()
        formData.append('roleId', editForm.value.roleid)
        formData.append('permissiondata', permissionData)
        
        // 发送请求
        const addResponse = await axios.post('/system/role/permission', formData, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        })
        
        if (addResponse.data.status !== 'success') {
          successFlag = false
          errorMsg = '添加权限失败'
        }
      } catch (error) {
        console.error('添加权限出错:', error)
        successFlag = false
        errorMsg = '添加权限时发生错误'
      }
    }
    
    // 如果有移除的权限，发送DELETE请求
    if (removedPermissions.length > 0 && successFlag) {
      try {
        // 将移除权限转换为逗号分隔的字符串
        const permissionData = removedPermissions.join(',')
        
        // 发送请求
        const deleteResponse = await axios.delete('/system/role/permission', {
          params: {
            roleId: editForm.value.roleid,
            permissiondata: permissionData
          }
        })
        
        if (deleteResponse.data.status !== 'success') {
          successFlag = false
          errorMsg = '移除权限失败'
        }
      } catch (error) {
        console.error('移除权限出错:', error)
        successFlag = false
        errorMsg = '移除权限时发生错误'
      }
    }
    
    // 角色名称发生变化，更新名称
    if (editForm.value.rolename !== roleList.value.find(role => role.roleid === editForm.value.roleid)?.rolename) {
      try {
        // 发送请求更新角色名称
        const updateResponse = await axios.put('/system/role', {
          roleid: editForm.value.roleid,
          rolename: editForm.value.rolename
        })
        
        if (updateResponse.data.status !== 'success') {
          successFlag = false
          errorMsg = '更新角色名称失败'
        }
      } catch (error) {
        console.error('更新角色名称出错:', error)
        successFlag = false
        errorMsg = '更新角色名称时发生错误'
      }
    }
    
    // 根据操作结果显示提示信息
    if (successFlag) {
      ElMessage.success('角色信息更新成功')
      // 关闭弹窗
      dialogVisible.value = false
      // 重新获取角色列表
      fetchRoleList()
    } else {
      ElMessage.error(errorMsg || '保存失败，请重试')
    }
  } catch (error) {
    console.error('保存角色信息出错:', error)
    ElMessage.error('保存失败，请重试')
  }
}

// 处理删除角色
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除角色"${row.rolename}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      draggable: true,
    }
  ).then(async () => {
    try {
      const response = await axios.delete('/system/role', {
        params: { roleId: row.roleid }
      })
      
      if (response.data.status === 'success') {
        ElMessage.success('删除角色成功')
        // 重新获取角色列表
        fetchRoleList()
      } else {
        ElMessage.error('删除角色失败')
      }
    } catch (error) {
      console.error('删除角色出错:', error)
      ElMessage.error('删除失败，请重试')
    }
  }).catch(() => {
    // 用户取消删除操作
    ElMessage.info('已取消删除')
  })
}

// 提交新增角色
const submitAdd = async () => {
  submitLoading.value = true
  try {
    // 验证表单
    await addFormRef.value.validate()
    
    // 调用API保存数据
    const response = await axios.post('/system/role', {
      rolecode: addForm.value.rolecode,
      rolename: addForm.value.rolename
    })
    
    if (response.data.status === 'success') {
      ElMessage.success('新增角色成功')
      // 关闭弹窗
      addDialogVisible.value = false
      // 重新获取角色列表
      fetchRoleList()
    } else {
      ElMessage.error('新增角色失败')
    }
  } catch (error) {
    console.error('新增角色出错:', error)
    ElMessage.error('新增失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  fetchRoleList()
})
</script>

<style scoped>
.roles-container {
  padding: 24px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 12px;
  border: 1px solid #0e5986;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(5px);
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

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
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

.role-table {
  margin-bottom: 20px;
  background-color: transparent;
  flex: 1;
}

/* 表格头部样式 */
.role-table :deep(.el-table__header) {
  background-color: rgba(0, 40, 80, 0.7);
  background-image: linear-gradient(to right, rgba(0, 60, 120, 0.7), rgba(0, 40, 80, 0.7));
}

.role-table :deep(.el-table__header th) {
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
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

/* 设置表格背景为透明 */
.role-table :deep(.el-table),
.role-table :deep(.el-table__inner-wrapper),
.role-table :deep(.el-table__body),
.role-table :deep(.el-table__footer) {
  background-color: transparent !important;
}

/* 修改表格行基础样式 */
.role-table :deep(.el-table__row) {
  background-color: rgba(0, 30, 60, 0.3);
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
}

/* 偶数行背景色 */
.role-table :deep(.el-table__row:nth-child(even)) {
  background-color: rgba(0, 35, 70, 0.3);
}

/* 修改悬停行样式，保持深色调 */
.role-table :deep(.el-table__row:hover) {
  background-color: rgba(30, 70, 110, 0.5) !important;
  transform: none !important;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

/* 覆盖Element Plus表格悬停单元格样式 */
.role-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: transparent !important;
}

.role-table :deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: transparent !important;
}

/* 覆盖单元格背景和文字颜色 */
.role-table :deep(.el-table__cell) {
  background-color: transparent !important;
  color: #e1f2ff;
  border-bottom: 1px solid rgba(14, 89, 134, 0.2);
  padding: 16px 12px;
  text-align: center;
}

/* 覆盖选中行样式 */
.role-table :deep(.el-table__row.current-row) {
  background-color: rgba(30, 80, 130, 0.6) !important;
}

/* 覆盖选中行的单元格样式 */
.role-table :deep(.el-table__row.current-row td.el-table__cell) {
  background-color: transparent !important;
}

/* 确保表格边框也是深色的 */
.role-table :deep(.el-table__border-left),
.role-table :deep(.el-table__border-right),
.role-table :deep(.el-table__border-top),
.role-table :deep(.el-table__border-bottom) {
  background-color: rgba(14, 89, 134, 0.3) !important;
}

/* 美化表格外观 */
.role-table :deep(.el-table) {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(14, 89, 134, 0.4);
}

/* 美化按钮样式 */
:deep(.el-button) {
  border-radius: 6px;
  padding: 10px 16px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #0091ff, #006be6);
  border-color: #0080e0;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #00a0ff, #0080ff);
  box-shadow: 0 4px 12px rgba(0, 145, 234, 0.4);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff4d4f, #d9363e);
  border-color: #d9363e;
}

:deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #ff6b6b, #f02d22);
  box-shadow: 0 4px 12px rgba(255, 76, 76, 0.4);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  border-color: #389e0d;
}

:deep(.el-button--success:hover) {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #73d13d, #52c41a);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.4);
}

:deep(.el-button--default) {
  background: rgba(30, 41, 59, 0.8);
  border-color: #334155;
  color: #f1f5f9;
}

:deep(.el-button--default:hover) {
  background: rgba(51, 65, 85, 0.9);
  border-color: #475569;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 按钮内的图标样式 */
:deep(.el-button .el-icon) {
  margin-right: 6px;
  font-size: 16px;
  vertical-align: middle;
}

/* 按钮组样式 */
:deep(.el-button-group) {
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

:deep(.el-button-group .el-button) {
  box-shadow: none;
}

:deep(.el-button-group .el-button:not(:first-child):not(:last-child)) {
  border-radius: 0;
}

:deep(.el-button-group .el-button:first-child) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

:deep(.el-button-group .el-button:last-child) {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/* 美化分页器 */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

:deep(.el-pagination) {
  border-radius: 8px;
  padding: 10px 16px;
  background: rgba(30, 41, 59, 0.6);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(14, 89, 134, 0.4);
}

:deep(.el-pagination .el-pagination__total) {
  color: #e1f2ff;
  font-weight: 500;
  margin-right: 10px;
}

:deep(.el-pagination .el-pagination__sizes) {
  margin-right: 15px;
}

:deep(.el-pagination .el-pagination__sizes .el-select .el-input) {
  border-radius: 4px;
}

:deep(.el-pagination .el-pagination__sizes .el-select .el-input__inner) {
  background-color: rgba(15, 23, 42, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: #e1f2ff;
}

:deep(.el-pagination .el-pagination__sizes .el-select:hover .el-input__inner) {
  border-color: #0091ff;
}

:deep(.el-pagination .el-pager) {
  background: transparent;
}

:deep(.el-pagination .el-pager li) {
  background: rgba(30, 41, 59, 0.6);
  color: #e1f2ff;
  margin: 0 2px;
  border-radius: 4px;
  font-weight: 500;
  min-width: 32px;
  height: 32px;
  line-height: 32px;
  transition: all 0.3s ease;
  border: 1px solid rgba(14, 89, 134, 0.3);
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #0091ff, #006be6);
  color: white;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0, 130, 255, 0.3);
  border-color: #0080e0;
}

:deep(.el-pagination .el-pager li:hover:not(.is-active)) {
  background: rgba(30, 68, 108, 0.8);
  color: #fff;
  border-color: rgba(0, 145, 234, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

:deep(.el-pagination .btn-prev, .el-pagination .btn-next) {
  background: rgba(30, 41, 59, 0.6);
  color: #e1f2ff;
  margin: 0 2px;
  border-radius: 4px;
  font-weight: 500;
  min-width: 32px;
  height: 32px;
  line-height: 32px;
  transition: all 0.3s ease;
  border: 1px solid rgba(14, 89, 134, 0.3);
}

:deep(.el-pagination .btn-prev:hover, .el-pagination .btn-next:hover) {
  background: rgba(30, 68, 108, 0.8);
  color: #fff;
  border-color: rgba(0, 145, 234, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

:deep(.el-pagination .btn-prev:disabled, .el-pagination .btn-next:disabled, .el-pagination .el-pager li.is-disabled) {
  background: rgba(15, 23, 42, 0.3);
  color: rgba(225, 242, 255, 0.3);
  cursor: not-allowed;
  border-color: rgba(14, 89, 134, 0.1);
}

:deep(.el-pagination .el-pagination__jump) {
  color: #e1f2ff;
  margin-left: 10px;
}

:deep(.el-pagination .el-pagination__jump .el-input__inner) {
  background-color: rgba(15, 23, 42, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  color: #e1f2ff;
}

:deep(.el-pagination .el-pagination__jump .el-input:hover .el-input__inner) {
  border-color: #0091ff;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .roles-container {
    padding: 15px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .search-bar {
    width: 100%;
  }
  
  .search-input {
    flex: 1;
  }
  
  .pagination {
    justify-content: center;
  }
}

/* 美化弹窗样式 */
:deep(.el-dialog) {
  background: linear-gradient(135deg, rgba(12, 37, 70, 0.95), rgba(8, 20, 40, 0.95));
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 30px rgba(0, 145, 234, 0.3);
  border: 1px solid rgba(14, 89, 134, 0.5);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(90deg, rgba(10, 50, 100, 0.8), rgba(5, 25, 50, 0.8));
  margin: 0;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.4);
}

:deep(.el-dialog__title) {
  color: #00dffc;
  font-weight: bold;
  font-size: 18px;
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.5);
}

:deep(.el-dialog__headerbtn) {
  top: 16px;
  right: 20px;
}

:deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #00ffff;
  transform: rotate(90deg);
  transition: all 0.3s;
}

:deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #e1f2ff;
  transition: all 0.3s;
}

:deep(.el-dialog__body) {
  padding: 24px;
  color: #e1f2ff;
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid rgba(14, 89, 134, 0.4);
  padding: 16px 20px;
  background: rgba(10, 30, 60, 0.4);
}

/* 弹窗底部按钮居中 */
.dialog-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
}

.dialog-footer :deep(.el-button) {
  min-width: 100px;
  font-weight: 500;
  letter-spacing: 1px;
}

/* 美化表单样式 */
:deep(.el-form-item__label) {
  color: #e1f2ff;
  font-weight: 500;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__wrapper) {
  background-color: rgba(0, 20, 40, 0.6);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
  border-radius: 6px;
}

:deep(.el-input__wrapper:hover),
:deep(.el-textarea__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
  border-color: rgba(0, 183, 255, 0.7);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
  border-color: #00b7ff;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  color: #e1f2ff;
  background-color: transparent;
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: rgba(225, 242, 255, 0.5);
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(71, 85, 105, 0.3);
  background-color: rgba(30, 41, 59, 0.3);
}

:deep(.el-input.is-disabled .el-input__inner) {
  color: rgba(225, 242, 255, 0.5);
}

/* 美化树形控件 */
:deep(.el-tree) {
  background-color: rgba(0, 20, 40, 0.4);
  border: 1px solid rgba(14, 89, 134, 0.5);
  border-radius: 8px;
  padding: 10px;
  color: #e1f2ff;
  max-height: 300px;
  overflow-y: auto;
}

:deep(.el-tree-node__content) {
  padding: 8px 10px;
  border-radius: 4px;
  transition: all 0.3s;
}

:deep(.el-tree-node__content:hover) {
  background-color: rgba(0, 145, 234, 0.15);
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: rgba(0, 145, 234, 0.25) !important;
}

:deep(.el-tree-node__expand-icon) {
  color: #00b7ff;
}

:deep(.el-tree-node__expand-icon.expanded) {
  transform: rotate(90deg);
}

:deep(.el-tree-node__expand-icon.is-leaf) {
  color: rgba(225, 242, 255, 0.5);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #0091ff;
  border-color: #0091ff;
}

:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  background-color: rgba(0, 145, 234, 0.6);
  border-color: #0091ff;
}

:deep(.el-checkbox__inner) {
  background-color: rgba(0, 20, 40, 0.6);
  border-color: rgba(14, 89, 134, 0.7);
}

:deep(.el-checkbox__inner:hover) {
  border-color: #0091ff;
}

/* 自定义滚动条 */
:deep(.el-dialog__body::-webkit-scrollbar),
:deep(.el-tree::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb),
:deep(.el-tree::-webkit-scrollbar-thumb) {
  background-color: rgba(0, 145, 234, 0.5);
  border-radius: 4px;
}

:deep(.el-dialog__body::-webkit-scrollbar-track),
:deep(.el-tree::-webkit-scrollbar-track) {
  background-color: rgba(30, 41, 59, 0.2);
  border-radius: 4px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb:hover),
:deep(.el-tree::-webkit-scrollbar-thumb:hover) {
  background-color: rgba(0, 145, 234, 0.7);
}

/* 响应式弹窗 */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 10px auto !important;
  }
  
  :deep(.el-dialog__body) {
    padding: 15px;
  }
  
  :deep(.el-form-item__label) {
    float: none;
    display: block;
    text-align: left;
    padding: 0 0 8px;
  }
  
  :deep(.el-tree) {
    max-height: 200px;
  }
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.action-buttons :deep(.el-button) {
  flex: 1;
  max-width: 84px;
}
</style> 