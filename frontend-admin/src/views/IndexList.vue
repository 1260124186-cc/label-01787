<template>
  <div class="page-container">
    <div class="page-header">
      <h2>全文搜索索引</h2>
      <div class="header-actions">
        <el-button type="primary" :loading="rebuildingAll" @click="handleRebuildAll">
          <el-icon><Refresh /></el-icon>
          批量重建索引
        </el-button>
      </div>
    </div>

    <el-card v-if="failedAlerts.length > 0" class="alert-card">
      <template #header>
        <div class="card-header">
          <div class="alert-title">
            <el-icon :size="18" color="#F56C6C"><Warning /></el-icon>
            <span>索引失败告警 ({{ failedAlerts.length }})</span>
          </div>
          <el-button size="small" type="danger" plain @click="fetchFailedAlerts">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      <el-table :data="failedAlerts" size="small" stripe>
        <el-table-column prop="bookTitle" label="书籍" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userNickname" label="用户" width="120" />
        <el-table-column prop="progress" label="进度" width="100">
          <template #default="{ row }">
            {{ row.indexedPages }}/{{ row.totalPages }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleRebuild(row.bookId)">重建</el-button>
            <el-button size="small" type="warning" @click="handleRetry(row.id)">重试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="索引状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="全部" :value="null" />
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="失败" :value="3" />
        </el-select>
        <el-input v-model="bookTitleFilter" placeholder="书籍名称" clearable style="width: 200px; margin-left: 12px"
          @keyup.enter="fetchData" />
        <el-button type="primary" :loading="loading" @click="fetchData" style="margin-left: 12px">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button :loading="loading" @click="resetFilters" style="margin-left: 8px">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="bookTitle" label="书籍名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="userNickname" label="用户昵称" width="120" />
        <el-table-column label="进度" min-width="200">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="row.progress || 0"
                :status="row.status === 3 ? 'exception' : row.status === 2 ? 'success' : ''" />
              <span class="progress-text">{{ row.indexedPages }}/{{ row.totalPages }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="row.priority >= 10 ? 'success' : 'info'" size="small">
              {{ row.priority >= 10 ? 'VIP' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMessage" :title="row.errorMessage">{{ row.errorMessage }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="180">
          <template #default="{ row }">
            <span>{{ row.startedAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="finishedAt" label="完成时间" width="180">
          <template #default="{ row }">
            <span>{{ row.finishedAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleRebuild(row.bookId)">重建</el-button>
            <el-button v-if="row.status === 3" size="small" type="warning" @click="handleRetry(row.id)">重试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning, Refresh, Search } from '@element-plus/icons-vue'
import {
  getIndexStatus,
  getFailedAlerts,
  rebuildIndex,
  rebuildAllIndex,
  retryIndexTask
} from '@/api/search'

const tableData = ref([])
const failedAlerts = ref([])
const loading = ref(false)
const rebuildingAll = ref(false)
const statusFilter = ref(null)
const bookTitleFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger' }
  return map[status] ?? ''
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const res = await getIndexStatus(params)
    let records = res.data.records || []
    if (bookTitleFilter.value) {
      const keyword = bookTitleFilter.value.toLowerCase()
      records = records.filter(r => (r.bookTitle || '').toLowerCase().includes(keyword))
    }
    tableData.value = records
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const fetchFailedAlerts = async () => {
  try {
    const res = await getFailedAlerts()
    failedAlerts.value = res.data || []
  } catch (e) {
    console.error('加载失败告警失败', e)
  }
}

const resetFilters = () => {
  statusFilter.value = null
  bookTitleFilter.value = ''
  currentPage.value = 1
  fetchData()
}

const handleRebuild = async (bookId) => {
  try {
    await ElMessageBox.confirm('确定要重建该书籍的全文索引吗？', '提示', { type: 'warning' })
    await rebuildIndex(bookId)
    ElMessage.success('重建任务已提交')
    fetchData()
    fetchFailedAlerts()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('重建索引失败', e)
    }
  }
}

const handleRebuildAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要批量重建所有书籍的全文索引吗？此操作可能耗时较长。',
      '确认批量重建',
      { type: 'warning' }
    )
    rebuildingAll.value = true
    await rebuildAllIndex()
    ElMessage.success('批量重建任务已启动')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量重建失败', e)
      ElMessage.error('批量重建失败')
    }
  } finally {
    rebuildingAll.value = false
  }
}

const handleRetry = async (taskId) => {
  try {
    await retryIndexTask(taskId)
    ElMessage.success('重试任务已提交')
    fetchData()
    fetchFailedAlerts()
  } catch (e) {
    console.error('重试失败', e)
    ElMessage.error('重试失败')
  }
}

onMounted(() => {
  fetchData()
  fetchFailedAlerts()
})
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #333;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #F56C6C;
}

.alert-card {
  border: 1px solid #fbc4c4;
  background: #fef0f0;

  :deep(.el-card__header) {
    background: #fef0f0;
    border-bottom: 1px solid #fbc4c4;
  }
}

.search-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .progress-text {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
  }
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
