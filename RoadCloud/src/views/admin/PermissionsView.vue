<template>
  <div class="permissions-container">
    <div class="page-header">
      <h2>权限管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleAdd">
          <i class="fas fa-plus"></i>
          新增权限
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索权限..."
        class="search-input"
        clearable
      >
        <template #prefix>
          <i class="fas fa-search"></i>
        </template>
      </el-input>
    </div>

    <el-table
      :data="filteredPermissions"
      style="width: 100%"
      class="permission-table"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="权限名称" />
      <el-table-column prop="code" label="权限编码" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="type" label="类型">
        <template #default="{ row }">
          <el-tag :type="row.type === 'menu' ? 'success' : 'warning'">
            {{ row.type === 'menu' ? '菜单' : '操作' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button-group>
            <el-button size="small" @click="handleEdit(row)">
              <i class="fas fa-edit"></i>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              <i class="fas fa-trash"></i>
              删除
            </el-button>
          </el-button-group>
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
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// 模拟数据
const permissions = ref([
  { 
    id: 1, 
    name: '用户管理', 
    code: 'user_manage',
    description: '管理用户信息的权限',
    type: 'menu'
  },
  { 
    id: 2, 
    name: '角色管理', 
    code: 'role_manage',
    description: '管理角色信息的权限',
    type: 'menu'
  },
  { 
    id: 3, 
    name: '数据查看', 
    code: 'data_view',
    description: '查看系统数据的权限',
    type: 'operation'
  },
  { 
    id: 4, 
    name: '报表导出', 
    code: 'report_export',
    description: '导出报表的权限',
    type: 'operation'
  }
])

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

// 过滤权限列表
const filteredPermissions = computed(() => {
  if (!searchQuery.value) return permissions.value
  return permissions.value.filter(permission => 
    permission.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    permission.code.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    permission.description.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 处理新增权限
const handleAdd = () => {
  console.log('新增权限')
}

// 处理编辑权限
const handleEdit = (row) => {
  console.log('编辑权限:', row)
}

// 处理删除权限
const handleDelete = (row) => {
  console.log('删除权限:', row)
}
</script>

<style scoped>
.permissions-container {
  padding: 20px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 5px;
  border: 1px solid #0e5986;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  color: #00ffff;
  font-size: 20px;
  margin: 0;
}

.search-bar {
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.permission-table {
  margin-bottom: 20px;
  background-color: transparent;
}

.permission-table :deep(.el-table__header) {
  background-color: rgba(0, 30, 60, 0.5);
}

.permission-table :deep(.el-table__row) {
  background-color: rgba(0, 30, 60, 0.3);
}

.permission-table :deep(.el-table__row:hover) {
  background-color: rgba(0, 145, 234, 0.2);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.el-button) {
  display: flex;
  align-items: center;
  gap: 5px;
}

:deep(.el-button i) {
  font-size: 14px;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-hover-color: #00ffff;
}
</style> 