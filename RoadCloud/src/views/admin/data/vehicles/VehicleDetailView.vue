<template>
  <div class="vehicle-detail-container">
    <div class="header">
      <div class="title">
        <h2>车辆详情 - {{ vehicleId }}</h2>
      </div>
      <div class="actions">
        <el-button type="danger" size="small" :disabled="!multipleSelection.length" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          删除数据{{ multipleSelection.length ? ' (' + multipleSelection.length + ')' : '' }}
        </el-button>
        <el-button type="success" size="small" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
        <el-button type="primary" size="small" @click="goBack">
          <el-icon><Back /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <el-card class="info-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>位置数据</span>
          <div class="filter-actions">
            <div class="time-filter">
              <el-date-picker
                v-model="filterStartTime"
                type="datetime"
                placeholder="开始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="x"
                size="small"
                @change="handleTimeChange"
              />
              <span class="time-separator">至</span>
              <el-date-picker
                v-model="filterEndTime"
                type="datetime"
                placeholder="结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="x"
                size="small"
                @change="handleTimeChange"
              />
              <el-button type="info" size="small" class="reset-button" @click="resetTimeFilter">
                <el-icon><RefreshRight /></el-icon>
                重置
              </el-button>
            </div>
            <el-pagination
              small
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

      <div v-if="locationData.length === 0 && !loading" class="empty-data">
        暂无位置数据
      </div>
      <el-table 
        v-else 
        :data="locationData" 
        style="width: 100%" 
        class="data-table"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="stateId" label="状态ID" />
        <el-table-column prop="timestampGNSS" label="时间">
          <template #default="{ row }">
            {{ formatTimestamp(row.timestampGNSS) }}
          </template>
        </el-table-column>
        <el-table-column prop="longitude" label="经度" />
        <el-table-column prop="latitude" label="纬度" />
        <el-table-column prop="elevation" label="海拔(m)" />
        <el-table-column prop="heading" label="方向" />
        <el-table-column prop="velocityGNSS" label="速度" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Delete, Download, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

// 获取URL参数中的vehicleId
const vehicleId = ref(route.query.vehicleId || route.params.id || '')

// 数据状态
const locationData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const multipleSelection = ref([])

// 时间筛选相关
const filterStartTime = ref('')
const filterEndTime = ref('')

// 导出相关
const exporting = ref(false)

// 处理时间筛选变化
const handleTimeChange = () => {
  if (filterEndTime.value && filterStartTime.value && filterEndTime.value < filterStartTime.value) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  currentPage.value = 1 // 重置页码
  fetchLocationData()
}

// 重置时间筛选
const resetTimeFilter = () => {
  filterStartTime.value = ''
  filterEndTime.value = ''
  currentPage.value = 1 // 重置页码
  fetchLocationData()
}

// 导出车辆数据
const handleExport = async () => {
  // 获取当前筛选的时间范围
  const startTime = filterStartTime.value
  const endTime = filterEndTime.value
  
  // 验证时间选择（如果两个都有值时才验证）
  if (startTime && endTime && endTime <= startTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  exporting.value = true
  
  try {
    // 导出参数始终使用车辆ID
    const vehicleIdValue = vehicleId.value
    
    // 构建请求参数（只添加有值的参数）
    const requestParams = {
      savedata: vehicleIdValue // 车辆ID必传
    }
    
    // 只有在有值时才添加时间参数
    if (startTime) {
      requestParams.starttime = startTime
    }
    
    if (endTime) {
      requestParams.endtime = endTime
    }
    
    // 步骤1: 发送请求获取filePath
    const response = await axios.get('/system/vehicle/export', {
      params: requestParams
    })
    
    // 步骤2: 检查响应状态并使用filePath构建下载链接
    if (response.data && response.data.status === 'success') {
      // 获取文件路径
      const filePath = response.data.filepath
      
      // 构建完整下载URL (确保包含协议和主机名)
      const downloadUrl = '/api' + filePath
      
      // 创建下载链接并触发点击
      const link = document.createElement('a')
      link.href = downloadUrl
      link.target = '_blank'
      link.setAttribute('download', `车辆数据_${vehicleId.value}.xlsx`)
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

// 获取车辆位置数据
const fetchLocationData = async () => {
  if (!vehicleId.value) {
    ElMessage.warning('未指定车辆ID')
    return
  }

  loading.value = true
  try {
    // 使用新的接口和参数
    const response = await axios.get('/system/vehicle/list/vehicleIdAndTime', {
      params: {
        vehicleId: vehicleId.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        starttime: filterStartTime.value || undefined, // 如果为空则不传
        endtime: filterEndTime.value || undefined    // 如果为空则不传
      }
    })
    
    if (response.data.status === 'success') {
      locationData.value = response.data.rows
      total.value = response.data.total
    } else {
      // 当状态为fail时，设置为无数据
      locationData.value = []
      total.value = 0
      ElMessage.error('该时间段无车辆位置数据')
    }
  } catch (error) {
    console.error('获取车辆位置数据出错:', error)
    ElMessage.error('网络错误，请稍后重试')
    // 发生错误时也设置为无数据
    locationData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 删除单个位置数据
const deleteStateData = async (stateId) => {
  try {
    const response = await axios.delete('/system/vehicle/delete/stateId?stateId=' + stateId)
    
    if (response.data.status === 'success') {
      ElMessage.success('删除数据成功')
      return true
    } else {
      ElMessage.error('删除数据失败')
      return false
    }
  } catch (error) {
    console.error('删除数据出错:', error)
    ElMessage.error('网络错误，请稍后重试')
    return false
  }
}

// 处理批量删除
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请选择要删除的数据')
    return
  }

  ElMessageBox.confirm(
    '是否确认删除选中的 ' + multipleSelection.value.length + ' 条位置数据?',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.value = true
    // 依次删除选中的数据
    const errors = []
    for (const item of multipleSelection.value) {
      try {
        const response = await axios.delete('/system/vehicle/delete/stateId?stateId=' + item.stateId)
        if (response.data.status !== 'success') {
          errors.push(item.stateId)
        }
      } catch (error) {
        errors.push(item.stateId)
      }
    }
    
    // 根据处理结果显示相应提示
    if (errors.length === 0) {
      ElMessage.success('所有数据删除成功')
    } else {
      ElMessage.warning('部分数据删除失败: ' + errors.join(', '))
    }
    
    // 刷新列表
    fetchLocationData()
    loading.value = false
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
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

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 监听分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchLocationData()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchLocationData()
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
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

// 监听路由参数变化
watch(() => [route.params.id, route.query.vehicleId], ([newId, newQuery]) => {
  vehicleId.value = newQuery || newId || ''
  if (vehicleId.value) {
    currentPage.value = 1 // 重置页码
    fetchLocationData()
  }
})

// 组件挂载时获取数据
onMounted(() => {
  fetchLocationData()
})
</script>

<style scoped>
.vehicle-detail-container {
  padding: 24px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 12px;
  border: 1px solid #0e5986;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.2);
  backdrop-filter: blur(5px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(14, 89, 134, 0.3);
}

.title h2 {
  color: #00ffff;
  margin: 0;
  font-size: 22px;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  letter-spacing: 1px;
  position: relative;
  display: inline-block;
}

.title h2::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(to right, rgba(0, 255, 255, 0.8), transparent);
  border-radius: 2px;
}

.actions {
  display: flex;
  gap: 12px;
}

.info-card {
  margin-bottom: 20px;
  background-color: rgba(0, 20, 40, 0.5);
  border: 1px solid #0e5986;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3), 0 0 20px rgba(0, 100, 234, 0.1);
  transition: all 0.3s ease;
  border-radius: 10px;
  overflow: hidden;
}

.info-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3), 0 0 30px rgba(0, 145, 234, 0.15);
}

.info-card :deep(.el-card__header) {
  background-color: rgba(0, 40, 80, 0.7);
  background-image: linear-gradient(to right, rgba(0, 60, 120, 0.7), rgba(0, 40, 80, 0.7));
  padding: 16px 20px;
  border-bottom: 1px solid #0e5986;
}

.info-card :deep(.el-card__body) {
  padding: 20px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header span {
  color: #e1f2ff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.5);
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding-top: 8px;
}

.time-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.time-separator {
  color: #e1f2ff;
  text-shadow: 0 0 5px rgba(0, 183, 255, 0.3);
}

.time-filter :deep(.el-input__wrapper) {
  background-color: rgba(0, 20, 40, 0.7);
  border-color: rgba(14, 89, 134, 0.7);
  box-shadow: 0 0 0 1px rgba(14, 89, 134, 0.7);
  transition: all 0.3s ease;
}

.time-filter :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 183, 255, 0.7);
}

.time-filter :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00b7ff, 0 0 10px rgba(0, 183, 255, 0.5);
}

.time-filter :deep(.el-input__inner) {
  color: white;
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

/* 增强状态ID列的样式 */
.data-table :deep([prop="stateId"] .cell) {
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

.data-table :deep([prop="stateId"] .cell::before) {
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

/* 增强时间列的样式 */
.data-table :deep([prop="timestampGNSS"] .cell) {
  color: #a0e7ff;
  font-weight: 500;
  font-size: 14px;
  letter-spacing: 0.3px;
  text-shadow: 0 0 3px rgba(0, 183, 255, 0.2);
  transition: all 0.3s ease;
}

/* 增强经纬度列的样式 */
.data-table :deep([prop="longitude"] .cell),
.data-table :deep([prop="latitude"] .cell) {
  color: #84e1ff;
  font-weight: 500;
  font-size: 14px;
  font-family: 'Consolas', monospace;
  letter-spacing: 0.3px;
  transition: all 0.3s ease;
}

/* 增强海拔、方向和速度列的样式 */
.data-table :deep([prop="elevation"] .cell),
.data-table :deep([prop="heading"] .cell),
.data-table :deep([prop="velocityGNSS"] .cell) {
  color: #7adeff;
  font-weight: 500;
  font-family: 'Consolas', monospace;
  font-size: 14px;
  transition: all 0.3s ease;
}

/* 确保hover状态下字体更明显 */
.data-table :deep(.el-table__row:hover [prop="stateId"] .cell) {
  color: #00ffff;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.6);
  transform: translateX(2px);
  font-weight: 600;
}

.data-table :deep(.el-table__row:hover [prop="stateId"] .cell::before) {
  opacity: 0.8;
  border-left: 3px solid #00ffff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.4);
}

.data-table :deep(.el-table__row:hover [prop="timestampGNSS"] .cell),
.data-table :deep(.el-table__row:hover [prop="longitude"] .cell),
.data-table :deep(.el-table__row:hover [prop="latitude"] .cell),
.data-table :deep(.el-table__row:hover [prop="elevation"] .cell),
.data-table :deep(.el-table__row:hover [prop="heading"] .cell),
.data-table :deep(.el-table__row:hover [prop="velocityGNSS"] .cell) {
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

.empty-data {
  padding: 40px;
  text-align: center;
  color: #8f9bb3;
  font-size: 16px;
  background: rgba(0, 30, 60, 0.3);
  border-radius: 10px;
  margin: 20px 0;
  border: 1px dashed rgba(14, 89, 134, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #a0cfff;
  min-height: 180px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.empty-data::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at center, rgba(0, 145, 234, 0.1) 0%, transparent 70%);
  opacity: 0.7;
  z-index: 0;
}

.empty-data::after {
  content: '无数据';
  position: absolute;
  font-size: 40px;
  font-weight: 300;
  opacity: 0.1;
  color: #00e0ff;
  z-index: 1;
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

/* 重置按钮的特殊样式 */
.reset-button {
  background: linear-gradient(135deg, #3498db, #2980b9) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 4px 10px rgba(52, 152, 219, 0.4) !important;
  transition: all 0.3s ease;
  margin-left: 8px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.reset-button:hover {
  background: linear-gradient(135deg, #00b7ff, #007bff) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 183, 255, 0.5) !important;
}

.reset-button:active {
  transform: translateY(1px);
  box-shadow: 0 2px 5px rgba(0, 183, 255, 0.4) !important;
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

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: rgba(0, 40, 80, 0.6);
  --el-pagination-hover-color: #00ffff;
  --el-pagination-font-size: 14px;
}

:deep(.el-pagination .el-pagination__total) {
  font-size: 14px;
  color: #a0cfff;
}

:deep(.el-pagination .el-pagination__jump) {
  color: #a0cfff;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background-color: rgba(0, 50, 100, 0.5);
  border-radius: 4px;
  color: #a0cfff;
  transition: all 0.3s ease;
}

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background-color: rgba(0, 90, 180, 0.6);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

:deep(.el-pagination .el-pager li) {
  background-color: rgba(0, 50, 100, 0.5);
  color: #a0cfff;
  border-radius: 4px;
  transition: all 0.3s ease;
}

:deep(.el-pagination .el-pager li:hover) {
  background-color: rgba(0, 90, 180, 0.6);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

:deep(.el-pagination .el-pager li.is-active) {
  background-color: rgba(0, 183, 255, 0.7);
  color: white;
  font-weight: bold;
  box-shadow: 0 2px 10px rgba(0, 183, 255, 0.4);
}

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

:deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(14, 89, 134, 0.5);
  background: rgba(0, 20, 40, 0.5);
}

:deep(.el-form-item__label) {
  color: #a0cfff;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(0, 183, 255, 0.3);
}

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

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  width: 100%;
  margin-top: 10px;
}
</style> 