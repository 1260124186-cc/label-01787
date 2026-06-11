<template>
  <div class="page-container">
    <div class="page-header">
      <h2>书籍管理</h2>
    </div>

    <el-card v-if="formatStats && formatStats.formats && formatStats.formats.length">
      <div class="format-stats-bar">
        <span class="format-stats-title">格式分布</span>
        <div class="format-stats-items">
          <span v-for="item in formatStats.formats" :key="item.format" class="format-stats-item">
            <el-tag :type="getFormatTagType(item.format)" size="small">{{ item.format.toUpperCase() }}</el-tag>
            <span class="format-stats-count">{{ item.count }} 本 ({{ item.percentage }}%)</span>
          </span>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索书籍名称" clearable style="width: 240px"
          @keyup.enter="fetchData" prefix-icon="Search" />
        <el-select v-model="formatFilter" placeholder="全部格式" clearable style="width: 140px; margin-left: 12px;" @change="fetchData">
          <el-option label="PDF" value="pdf" />
          <el-option label="EPUB" value="epub" />
          <el-option label="MOBI" value="mobi" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="fetchData" style="margin-left: 12px;">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无书籍数据">
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" min-width="120" />
        <el-table-column label="格式" width="90">
          <template #default="{ row }">
            <el-tag :type="getFormatTagType(row.bookFormat)" size="small">{{ (row.bookFormat || 'pdf').toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="页数/章节" width="100">
          <template #default="{ row }">
            {{ row.bookFormat === 'epub' ? (row.chapterCount || 0) + '章' : (row.pageCount || 0) + '页' }}
          </template>
        </el-table-column>
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
        <el-table-column label="索引状态" width="140">
          <template #default="{ row }">
            <el-tag v-if="indexStatusMap[row.id]" :type="getIndexTagType(indexStatusMap[row.id].status)" size="small">
              {{ indexStatusMap[row.id].statusText }}
            </el-tag>
            <el-tag v-else type="info" size="small">未索引</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleRebuildIndex(row)">重建索引</el-button>
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
import { getBookList, getBookFormatStats } from '@/api/admin'
import { getIndexStatus, rebuildIndex } from '@/api/search'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const formatFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const formatStats = ref(null)
const indexStatusMap = ref({})

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value, keyword: keyword.value }
    if (formatFilter.value) {
      params.format = formatFilter.value
    }
    const res = await getBookList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
    fetchIndexStatus()
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const fetchIndexStatus = async () => {
  try {
    const res = await getIndexStatus({ page: 1, size: 500 })
    const statusList = res.data.records || []
    const map = {}
    statusList.forEach(item => {
      map[item.bookId] = item
    })
    indexStatusMap.value = map
  } catch (e) {
    console.error('加载索引状态失败', e)
  }
}

const handleRebuildIndex = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要重建《${row.title}》的全文索引吗？`, '提示', { type: 'warning' })
    await rebuildIndex(row.id)
    ElMessage.success('重建任务已提交')
    fetchIndexStatus()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('重建索引失败', e)
    }
  }
}

const getIndexTagType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger' }
  return map[status] ?? 'info'
}

const fetchFormatStats = async () => {
  try {
    const res = await getBookFormatStats()
    formatStats.value = res.data
  } catch (e) {
    // ignore
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const getFormatTagType = (format) => {
  switch (format) {
    case 'epub': return 'warning'
    case 'mobi': return 'info'
    default: return ''
  }
}

onMounted(() => {
  fetchData()
  fetchFormatStats()
})
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
.format-stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
}
.format-stats-title {
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}
.format-stats-items {
  display: flex;
  gap: 20px;
}
.format-stats-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.format-stats-count {
  font-size: 13px;
  color: #606266;
}
</style>
