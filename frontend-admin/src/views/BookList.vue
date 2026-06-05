<template>
  <div class="page-container">
    <div class="page-header">
      <h2>书籍管理</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索书籍名称" clearable style="width: 280px"
          @keyup.enter="fetchData" prefix-icon="Search" />
        <el-button type="primary" :loading="loading" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无书籍数据">
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" min-width="120" />
        <el-table-column prop="pageCount" label="页数" width="80" />
        <el-table-column label="文件大小" width="110">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="版权声明" width="90">
          <template #default="{ row }">
            <el-tag :type="row.copyrightDeclared === 1 ? 'success' : 'info'" size="small">
              {{ row.copyrightDeclared === 1 ? '已声明' : '未声明' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" min-width="180" />
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
import { getBookList } from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const res = await getBookList({ page: currentPage.value, size: pageSize.value, keyword: keyword.value })
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
