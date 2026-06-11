<template>
  <div class="page-container">
    <div class="page-header">
      <h2>书摘举报管理</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无举报">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="excerptId" label="书摘ID" width="90" />
        <el-table-column prop="reporterId" label="举报人ID" width="100" />
        <el-table-column prop="reason" label="举报原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="detail" label="详细描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="举报时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewExcerpt(row)">查看书摘</el-button>
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="handleReport(row, 1)">处理</el-button>
              <el-button type="info" size="small" @click="handleReport(row, 2)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="excerptDialogVisible" title="被举报书摘详情" width="600px">
      <div v-if="currentExcerpt" class="detail-content">
        <div class="detail-item">
          <span class="label">书名：</span>
          <span>《{{ currentExcerpt.bookTitle }}》</span>
        </div>
        <div class="detail-item">
          <span class="label">作者：</span>
          <span>{{ currentExcerpt.bookAuthor || '佚名' }}</span>
        </div>
        <div class="detail-item excerpt-box">
          <span class="label">书摘原文：</span>
          <div class="excerpt-text">"{{ currentExcerpt.excerptText }}"</div>
        </div>
        <div class="detail-item" v-if="currentExcerpt.commentText">
          <span class="label">评语：</span>
          <div class="comment-text">{{ currentExcerpt.commentText }}</div>
        </div>
        <div class="detail-stats">
          <span>❤ {{ currentExcerpt.likes || 0 }}</span>
          <span>★ {{ currentExcerpt.favorites || 0 }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="excerptDialogVisible = false">关闭</el-button>
        <el-button type="danger" @click="removeAndHandle">下架书摘</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPlazaReportList, handlePlazaReport, getPlazaExcerptList } from '@/api/admin'
import { ElMessageBox, ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const excerptDialogVisible = ref(false)
const currentExcerpt = ref(null)
const currentReport = ref(null)

const statusText = (status) => {
  const map = { 0: '待处理', 1: '已处理', 2: '已驳回' }
  return map[status] ?? '未知'
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] ?? ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    const res = await getPlazaReportList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    loading.value = false
  }
}

const viewExcerpt = async (row) => {
  currentReport.value = row
  try {
    const res = await getPlazaExcerptList({ page: 1, size: 1 })
    const list = res.data.records || []
    const excerpt = list.find(item => item.id === row.excerptId)
    if (excerpt) {
      currentExcerpt.value = excerpt
    } else {
      currentExcerpt.value = {
        id: row.excerptId,
        bookTitle: '（已删除）',
        bookAuthor: '',
        excerptText: '',
        commentText: '',
        likes: 0,
        favorites: 0
      }
    }
    excerptDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取书摘详情失败')
  }
}

const handleReport = async (row, newStatus) => {
  const actionLabel = statusText(newStatus)
  try {
    const { value } = await ElMessageBox.prompt(
      `确定将此举报标记为"${actionLabel}"？请填写处理结果：`,
      '处理举报',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入处理结果',
        type: newStatus === 1 ? 'warning' : 'info',
        inputValidator: (val) => val ? true : '处理结果不能为空'
      }
    )
    await handlePlazaReport(row.id, { status: newStatus, handleResult: value })
    ElMessage.success('处理成功')
    fetchData()
  } catch {
  }
}

const removeAndHandle = async () => {
  if (!currentReport.value) return
  try {
    const { value } = await ElMessageBox.prompt(
      `确定下架该书摘并处理举报？请填写处理结果：`,
      '下架并处理',
      {
        confirmButtonText: '确定下架',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入处理结果',
        type: 'warning',
        inputValidator: (val) => val ? true : '处理结果不能为空'
      }
    )
    await handlePlazaReport(currentReport.value.id, { status: 1, handleResult: value })
    ElMessage.success('已下架并处理')
    excerptDialogVisible.value = false
    fetchData()
  } catch {
  }
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

.detail-content {
  .detail-item {
    margin-bottom: 16px;
    display: flex;
    gap: 8px;
    line-height: 1.6;
    
    .label {
      color: #999;
      flex-shrink: 0;
    }
  }
  
  .excerpt-box {
    flex-direction: column;
    gap: 8px;
  }
  
  .excerpt-text {
    background: #f5f7fa;
    padding: 12px 16px;
    border-radius: 4px;
    border-left: 3px solid #d4a574;
    color: #666;
    line-height: 1.8;
  }
  
  .comment-text {
    background: #ecf5ff;
    padding: 12px 16px;
    border-radius: 4px;
    color: #333;
    line-height: 1.8;
  }
  
  .detail-stats {
    display: flex;
    gap: 20px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
    color: #666;
    font-size: 14px;
  }
}
</style>
