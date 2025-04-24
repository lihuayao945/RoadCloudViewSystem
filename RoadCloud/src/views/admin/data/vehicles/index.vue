<template>
  <div class="vehicles-container">
    <div class="action-row">
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索车辆ID..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="table-actions">
        <el-button type="danger" size="small" :disabled="!multipleSelection.length" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          删除{{ multipleSelection.length ? ' (' + multipleSelection.length + ')' : '' }}
        </el-button>
        <el-button type="success" size="small" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          {{ multipleSelection.length ? '导出 (' + multipleSelection.length + ')' : '全部导出' }}
        </el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="filteredVehicles"
      style="width: 100%"
      class="data-table"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="vehicleId" label="车辆ID" />
      <el-table-column prop="count" label="轨迹点数量" />
      <el-table-column prop="latestTime" label="最后更新时间" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button-group>
            <el-button size="small" type="primary" @click="handleDetail(row)">
              <el-icon><View /></el-icon>
              详细
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
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { 
  Search, 
  Download, 
  Delete,
  View
} from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

// 路由器实例
const router = useRouter()

// 数据状态
const vehicles = ref([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const multipleSelection = ref([])

// 固定的时间戳（2010年和2030年）
const START_TIMESTAMP = '1262275200000' // 2010-01-01
const END_TIMESTAMP = '1893456000000'   // 2030-01-01

// 获取车辆列表数据
const fetchVehicles = async () => {
  loading.value = true
  try {
    const response = await axios.get('/system/vehicle/manage', {
      params: {
        pageNum: currentPage.value,
        pageSize: pageSize.value
      }
    })
    
    if (response.data.status === 'success') {
      total.value = response.data.total
      
      // 格式化时间
      vehicles.value = response.data.rows.map(vehicle => ({
        ...vehicle,
        latestTime: formatTimestamp(vehicle.latestTime)
      }))
    } else {
      ElMessage.error('获取车辆列表失败')
    }
  } catch (error) {
    console.error('获取车辆列表出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 删除单个车辆
const deleteVehicle = async (vehicleId) => {
  try {
    const response = await axios.delete('/system/vehicle/delete/vehicleId?vehicleId=' + vehicleId)
    
    if (response.data.status === 'success') {
      ElMessage.success('删除成功')
      fetchVehicles() // 刷新数据
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    console.error('删除车辆出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 导出车辆数据
const handleExport = async () => {
  exporting.value = true
  
  try {
    // 确定要导出的车辆ID
    let vehicleIds = ''
    
    if (multipleSelection.value.length > 0) {
      // 导出选中的车辆
      vehicleIds = multipleSelection.value.map(v => v.vehicleId).join(',')
    } else {
      // 获取所有车辆ID（如果需要全部导出）
      if (vehicles.value.length < total.value) {
        // 如果当前页面的数据不足总数，需要获取所有数据
        try {
          const allResponse = await axios.get('/system/vehicle/manage', {
            params: {
              pageNum: 1,
              pageSize: total.value // 获取全部数据
            }
          })
          
          if (allResponse.data.status === 'success') {
            vehicleIds = allResponse.data.rows.map(v => v.vehicleId).join(',')
          }
        } catch (error) {
          console.error('获取所有车辆数据出错:', error)
          vehicleIds = vehicles.value.map(v => v.vehicleId).join(',') // 使用当前页面数据
        }
      } else {
        // 当前页面已包含所有数据
        vehicleIds = vehicles.value.map(v => v.vehicleId).join(',')
      }
    }
    
    // 构建请求参数
    const requestParams = {
      starttime: START_TIMESTAMP,
      endtime: END_TIMESTAMP,
      savedata: vehicleIds
    }
    
    // 步骤1: 发送请求获取filePath
    const response = await axios.get('/system/vehicle/export', {
      params: requestParams
    })
    
    // 步骤2: 检查响应状态并使用filePath构建下载链接
    if (response.data && response.data.status === 'success') {
      // 获取文件路径
      const filePath = response.data.filepath
      
      // 构建完整下载URL
      const downloadUrl = '/api' + filePath
      
      // 创建下载链接并触发点击
      const link = document.createElement('a')
      link.href = downloadUrl
      link.target = '_blank'
      link.setAttribute('download', '车辆数据.xlsx')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('导出成功，正在下载文件')
    } else {
      ElMessage.error('导出失败：' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    console.error('导出车辆数据出错:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

// 时间戳格式化函数
const formatTimestamp = (timestamp) => {
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

// 监听分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchVehicles()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchVehicles()
}

// 过滤车辆列表
const filteredVehicles = computed(() => {
  if (!searchQuery.value) return vehicles.value
  return vehicles.value.filter(vehicle => 
    vehicle.vehicleId.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 处理批量删除
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请选择要删除的车辆')
    return
  }

  ElMessageBox.confirm(
    '是否确认删除选中的 ' + multipleSelection.value.length + ' 条车辆数据?',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    // 依次删除选中的车辆
    const errors = []
    for (const vehicle of multipleSelection.value) {
      try {
        const response = await axios.delete('/system/vehicle/delete/vehicleId?vehicleId=' + vehicle.vehicleId)
        if (response.data.status !== 'success') {
          errors.push(vehicle.vehicleId)
        }
      } catch (error) {
        errors.push(vehicle.vehicleId)
      }
    }
    
    // 根据处理结果显示相应提示
    if (errors.length === 0) {
      ElMessage.success('所有车辆删除成功')
    } else {
      ElMessage.warning('部分车辆删除失败: ' + errors.join(', '))
    }
    
    // 刷新列表
    fetchVehicles()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 处理车辆详情
const handleDetail = (row) => {
  // 导航到车辆详细页面
  router.push({
    name: 'VehicleDetail',
    params: { id: row.vehicleId },
    query: { vehicleId: row.vehicleId }
  })
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

// 组件挂载时获取数据
onMounted(() => {
  fetchVehicles()
})
</script>

<style scoped>
.vehicles-container {
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

.data-table {
  --el-table-border-color: rgba(14, 89, 134, 0.5);
  --el-table-header-bg-color: rgba(0, 40, 80, 0.6);
  --el-table-row-hover-bg-color: rgba(0, 145, 234, 0.2);
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
  color: #e1f5ff;
  font-weight: 400;
}

.data-table :deep(.el-table__row:hover td) {
  color: #ffffff;
}

/* 增强车辆ID列的样式 */
.data-table :deep([prop="vehicleId"] .cell) {
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

.data-table :deep([prop="vehicleId"] .cell::before) {
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

/* 增强轨迹点数量和最后更新时间列的样式 */
.data-table :deep([prop="count"] .cell),
.data-table :deep([prop="latestTime"] .cell) {
  color: #e1f5ff;
  font-weight: 500;
  font-size: 14px;
  letter-spacing: 0.3px;
  text-shadow: 0 0 3px rgba(0, 183, 255, 0.2);
  transition: all 0.3s ease;
}

/* 确保hover状态下字体更明显 */
.data-table :deep(.el-table__row:hover [prop="vehicleId"] .cell) {
  color: #00ffff;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.6);
  transform: translateX(2px);
  font-weight: 600;
}

.data-table :deep(.el-table__row:hover [prop="vehicleId"] .cell::before) {
  opacity: 0.8;
  border-left: 3px solid #00ffff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.4);
}

.data-table :deep(.el-table__row:hover [prop="count"] .cell),
.data-table :deep(.el-table__row:hover [prop="latestTime"] .cell) {
  color: #00ffff;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.4);
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
</style> 