<template>
  <div class="rsu-events-container">
    <div class="action-row">
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索事件..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="table-actions">
        <el-button type="success" size="small">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button type="warning" size="small">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-table
      :data="filteredEvents"
      style="width: 100%"
      class="data-table"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="eventType" label="事件类型" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="level" label="等级">
        <template #default="{ row }">
          <el-tag :type="getEventLevelType(row.level)">
            {{ getEventLevelName(row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="time" label="时间" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'danger' : 'success'">
            {{ row.status === 'active' ? '进行中' : '已结束' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button-group>
            <el-button size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button size="small" type="primary" @click="handleProcess(row)">
              <el-icon><Check /></el-icon>
              处理
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
import { 
  Search, 
  Download, 
  Refresh, 
  View, 
  Check 
} from '@element-plus/icons-vue'

// 模拟数据
const rsuEvents = ref([
  { 
    id: 1, 
    eventType: '交通拥堵', 
    location: '东方路与浦建路交叉口',
    level: 'high',
    time: '2024-03-20 10:00:00',
    status: 'active'
  },
  { 
    id: 2, 
    eventType: '闯红灯', 
    location: '世纪大道与张杨路交叉口',
    level: 'medium',
    time: '2024-03-20 09:30:00',
    status: 'active'
  },
  { 
    id: 3, 
    eventType: '交通事故', 
    location: '陆家嘴环路',
    level: 'high',
    time: '2024-03-19 15:20:00',
    status: 'resolved'
  }
])

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

// 过滤事件列表
const filteredEvents = computed(() => {
  if (!searchQuery.value) return rsuEvents.value
  return rsuEvents.value.filter(event => 
    event.eventType.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    event.location.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 获取事件等级样式
const getEventLevelType = (level) => {
  switch(level) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    case 'low': return 'info'
    default: return 'info'
  }
}

// 获取事件等级名称
const getEventLevelName = (level) => {
  switch(level) {
    case 'high': return '高级'
    case 'medium': return '中级'
    case 'low': return '低级'
    default: return '未知'
  }
}

// 处理查看事件详情
const handleDetail = (row) => {
  console.log('查看事件详情:', row)
}

// 处理事件
const handleProcess = (row) => {
  console.log('处理事件:', row)
}
</script>

<style scoped>
.rsu-events-container {
  padding: 20px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 5px;
  border: 1px solid #0e5986;
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.search-bar {
  flex: 1;
  min-width: 200px;
}

.search-input {
  width: 100%;
  max-width: 300px;
}

.table-actions {
  display: flex;
  gap: 10px;
}

.data-table {
  margin-bottom: 20px;
  background-color: transparent;
}

.data-table :deep(.el-table__header) {
  background-color: rgba(0, 30, 60, 0.5);
}

.data-table :deep(.el-table__row) {
  background-color: rgba(0, 30, 60, 0.3);
}

.data-table :deep(.el-table__row:hover) {
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

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-hover-color: #00ffff;
}
</style> 