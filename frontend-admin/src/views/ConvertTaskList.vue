<template>
  <div class="page-container">
    <div class="page-header">
      <h2>转图任务</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="任务状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="全部" :value="null" />
          <el-option label="等待中" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="失败" :value="3" />
        </el-select>
        <el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 160px; margin-left: 12px"
          @keyup.enter="fetchData" />
        <el-button type="primary" :loading="loading" @click="fetchData" style="margin-left: 12px">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="bookTitle" label="书籍标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="row.priority >= 10 ? 'success' : 'info'" size="small">
              {{ row.priority >= 10 ? 'VIP' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="200">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="progressPercent(row)" :status="row.status === 3 ? 'exception' : ''" />
              <span class="progress-text">{{ row.convertedPages }}/{{ row.totalPages }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column prop="finishedAt" label="完成时间" min-width="180" />
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
import { getConvertTaskList } from '@/api/membership'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref(null)
const userIdFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusText = (status) => {
  const map = { 0: '等待中', 1: '处理中', 2: '已完成', 3: '失败' }
  return map[status] ?? status
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger' }
  return map[status] ?? ''
}

const progressPercent = (row) => {
  if (!row.totalPages || row.totalPages === 0) return 0
  return Math.round((row.convertedPages / row.totalPages) * 100)
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    if (userIdFilter.value) params.userId = userIdFilter.value
    const res = await getConvertTaskList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
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
