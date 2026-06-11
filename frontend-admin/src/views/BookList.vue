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
      <div class="toolbar">
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
        <div class="batch-bar" v-if="selectedIds.length > 0">
          <span class="batch-info">
            已选择 <el-tag type="primary">{{ selectedIds.length }}</el-tag> 本
          </span>
          <el-button size="small" type="warning" :icon="BottomRight" :loading="batchLoading" @click="handleBatchTakeDown">
            批量下架
          </el-button>
          <el-button size="small" type="danger" :icon="Delete" :loading="batchLoading" @click="handleBatchDelete">
            批量删除
          </el-button>
          <el-button size="small" text @click="clearSelection">取消选择</el-button>
        </div>
      </div>

      <el-table
        :data="tableData"
        stripe
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无书籍数据"
        ref="tableRef"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" :selectable="row => row.status === 1" />
        <el-table-column prop="title" label="书名" min-width="220" show-overflow-tooltip />
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
            <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else-if="row.status === 2" type="warning" size="small">已下架</el-tag>
            <el-tag v-else type="danger" size="small">已删除</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" min-width="170" />
        <el-table-column label="索引状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="indexStatusMap[row.id]" :type="getIndexTagType(indexStatusMap[row.id].status)" size="small">
              {{ indexStatusMap[row.id].statusText }}
            </el-tag>
            <el-tag v-else type="info" size="small">未索引</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.bookFormat === 'pdf' && row.status === 1"
              size="small"
              type="success"
              link
              :loading="previewLoadingMap[row.id]"
              @click="handlePreview(row)"
            >
              预览PDF
            </el-button>
            <el-button size="small" type="primary" link @click="handleViewUploader(row)">上传用户</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" link @click="handleTakeDown(row)">下架</el-button>
            <el-button size="small" text @click="handleRebuildIndex(row)">重建索引</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="previewVisible" title="PDF在线预览" width="900px" destroy-on-close top="5vh">
      <div v-if="previewData" class="preview-container">
        <div class="preview-header">
          <div>
            <strong>{{ previewData.title }}</strong>
            <span class="preview-meta">
              共 {{ previewData.totalPages }} 页，当前预览前 {{ previewData.previewPages }} 页
            </span>
          </div>
          <div class="preview-page-count">
            <span>预览页数：</span>
            <el-select v-model="previewPageCount" size="small" style="width: 110px" @change="refreshPreview">
              <el-option v-for="n in [1,2,3,5,10]" :key="n" :label="`前 ${n} 页`" :value="n" />
            </el-select>
          </div>
        </div>
        <div class="preview-pages" v-loading="previewLoading">
          <div v-for="page in previewData.pages" :key="page.page" class="preview-page-item">
            <div class="page-label">第 {{ page.page }} 页</div>
            <img :src="page.image" class="page-image" :alt="`第${page.page}页`" />
          </div>
          <el-empty v-if="!previewLoading && previewData.pages.length === 0" description="没有可预览的页面" />
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="uploaderVisible" title="上传用户信息" width="480px" destroy-on-close>
      <div v-if="uploaderData" class="uploader-card">
        <el-avatar :size="72" :src="uploaderData.avatar" shape="square">
          {{ uploaderData.nickname?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="uploader-info">
          <el-descriptions :column="1" border size="small" style="margin-top: 16px; width: 100%;">
            <el-descriptions-item label="书籍名称">{{ uploaderData.bookTitle }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ uploaderData.userId }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ uploaderData.nickname }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="uploaderData.userStatus === 1 ? 'success' : 'danger'" size="small">
                {{ uploaderData.userStatus === 1 ? '正常' : '已禁用' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, BottomRight } from '@element-plus/icons-vue'
import {
  getBookList, getBookFormatStats, batchDeleteBooks, batchTakeDownBooks,
  getBookUploader, previewBookPdf
} from '@/api/admin'
import { getIndexStatus, rebuildIndex } from '@/api/search'

const tableRef = ref(null)
const tableData = ref([])
const loading = ref(false)
const batchLoading = ref(false)
const keyword = ref('')
const formatFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const formatStats = ref(null)
const indexStatusMap = ref({})
const selectedIds = ref([])
const previewLoadingMap = ref({})

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)
const previewPageCount = ref(3)
const previewingBookId = ref(null)

const uploaderVisible = ref(false)
const uploaderData = ref(null)

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
    clearSelection()
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

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(r => r.id)
}

const clearSelection = () => {
  selectedIds.value = []
  tableRef.value?.clearSelection()
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

const handleTakeDown = async (row) => {
  try {
    await ElMessageBox.confirm(`确定下架《${row.title}》吗？`, '下架确认', { type: 'warning' })
    await batchTakeDownBooks([row.id])
    ElMessage.success('下架成功')
    fetchData()
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '下架失败')
  }
}

const handleBatchTakeDown = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定批量下架选中的 ${selectedIds.value.length} 本书吗？此操作需二次确认。`,
      '批量下架确认',
      { type: 'warning' }
    )
    batchLoading.value = true
    const res = await batchTakeDownBooks(selectedIds.value)
    ElMessage.success(`批量下架完成：成功 ${res.data.successCount} 本，失败 ${res.data.failedCount} 本`)
    fetchData()
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '操作失败')
  } finally {
    batchLoading.value = false
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定批量删除选中的 ${selectedIds.value.length} 本书吗？此操作不可撤销，且需要二次确认！`,
      '批量删除确认',
      { type: 'error', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
    )
    batchLoading.value = true
    const res = await batchDeleteBooks(selectedIds.value)
    ElMessage.success(`批量删除完成：成功 ${res.data.successCount} 本，失败 ${res.data.failedCount} 本`)
    fetchData()
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '操作失败')
  } finally {
    batchLoading.value = false
  }
}

const handleViewUploader = async (row) => {
  try {
    const res = await getBookUploader(row.id)
    uploaderData.value = res.data
    uploaderVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取上传用户信息失败')
  }
}

const handlePreview = async (row) => {
  previewLoadingMap.value[row.id] = true
  try {
    previewingBookId.value = row.id
    previewPageCount.value = 3
    await loadPreview()
    previewVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '预览失败')
  } finally {
    previewLoadingMap.value[row.id] = false
  }
}

const refreshPreview = async () => {
  await loadPreview()
}

const loadPreview = async () => {
  if (!previewingBookId.value) return
  previewLoading.value = true
  try {
    const res = await previewBookPdf(previewingBookId.value, previewPageCount.value)
    previewData.value = res.data
  } finally {
    previewLoading.value = false
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
.toolbar {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.search-bar {
  display: flex;
  align-items: center;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #ecf5ff;
  border-radius: 6px;
  border: 1px solid #d9ecff;
}
.batch-info {
  font-size: 13px;
  color: #606266;
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

.preview-container {
  padding: 0 4px;
}
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
}
.preview-meta {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
  font-weight: normal;
}
.preview-page-count {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}
.preview-pages {
  max-height: 70vh;
  overflow: auto;
  background: #f0f2f5;
  padding: 20px;
  border-radius: 6px;
}
.preview-page-item {
  margin-bottom: 20px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border-radius: 4px;
  padding: 12px;
}
.page-label {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}
.page-image {
  display: block;
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  border: 1px solid #e4e7ed;
}

.uploader-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 4px;
}
.uploader-info {
  width: 100%;
}
</style>
