<template>
  <div class="page-container">
    <div class="page-header">
      <h2>AI摘要记录</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 160px"
          @keyup.enter="fetchData" />
        <el-button type="primary" :loading="loading" @click="fetchData" style="margin-left: 12px">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="bookTitle" label="书籍" min-width="180" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
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
import { getAiSummaryList } from '@/api/membership'

const tableData = ref([])
const loading = ref(false)
const userIdFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusText = (status) => {
  const map = { 0: '失败', 1: '成功', 2: '生成中' }
  return map[status] ?? status
}

const statusTagType = (status) => {
  const map = { 0: 'danger', 1: 'success', 2: 'warning' }
  return map[status] ?? ''
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (userIdFilter.value) params.userId = userIdFilter.value
    const res = await getAiSummaryList(params)
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

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
