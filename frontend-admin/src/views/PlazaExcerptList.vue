<template>
  <div class="page-container">
    <div class="page-header">
      <h2>书摘广场管理</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="正常" :value="1" />
          <el-option label="已撤回" :value="0" />
          <el-option label="已下架" :value="2" />
        </el-select>
        <el-select v-model="auditFilter" placeholder="审核状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="待审核" :value="0" />
          <el-option label="审核通过" :value="1" />
          <el-option label="审核不通过" :value="2" />
        </el-select>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无书摘">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="bookTitle" label="书名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="bookAuthor" label="作者" width="100" show-overflow-tooltip />
        <el-table-column prop="excerptText" label="书摘内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="commentText" label="评语" min-width="150" show-overflow-tooltip />
        <el-table-column prop="likes" label="点赞" width="70" />
        <el-table-column prop="favorites" label="收藏" width="70" />
        <el-table-column prop="reportCount" label="举报" width="70" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" size="small">{{ auditText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" min-width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">查看</el-button>
            <template v-if="row.auditStatus === 0 && row.status === 1">
              <el-button type="success" size="small" @click="auditPass(row)">通过</el-button>
              <el-button type="danger" size="small" @click="auditReject(row)">拒绝</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button type="danger" size="small" @click="removeExcerpt(row)">下架</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="书摘详情" width="600px">
      <div v-if="currentExcerpt" class="detail-content">
        <div class="detail-item">
          <span class="label">书名：</span>
          <span>《{{ currentExcerpt.bookTitle }}》</span>
        </div>
        <div class="detail-item">
          <span class="label">作者：</span>
          <span>{{ currentExcerpt.bookAuthor || '佚名' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">用户ID：</span>
          <span>{{ currentExcerpt.userId }}</span>
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
          <span>👁 {{ currentExcerpt.views || 0 }}</span>
          <span>⚠ {{ currentExcerpt.reportCount || 0 }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPlazaExcerptList, auditPlazaExcerpt, removePlazaExcerpt } from '@/api/admin'
import { ElMessageBox, ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref(null)
const auditFilter = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const currentExcerpt = ref(null)

const statusText = (status) => {
  const map = { 0: '已撤回', 1: '正常', 2: '已下架' }
  return map[status] ?? '未知'
}

const statusTagType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[status] ?? ''
}

const auditText = (status) => {
  const map = { 0: '待审核', 1: '通过', 2: '不通过' }
  return map[status] ?? '未知'
}

const auditTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] ?? ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    if (auditFilter.value !== null && auditFilter.value !== '') {
      params.auditStatus = auditFilter.value
    }
    const res = await getPlazaExcerptList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentExcerpt.value = row
  detailDialogVisible.value = true
}

const auditPass = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定审核通过该书摘吗？`,
      '审核通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
    await auditPlazaExcerpt(row.id, { auditStatus: 1 })
    ElMessage.success('审核通过')
    fetchData()
  } catch {
  }
}

const auditReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写拒绝原因：`,
      '审核拒绝',
      {
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入拒绝原因',
        type: 'warning',
        inputValidator: (val) => val ? true : '拒绝原因不能为空'
      }
    )
    await auditPlazaExcerpt(row.id, { auditStatus: 2, reason: value })
    ElMessage.success('已拒绝')
    fetchData()
  } catch {
  }
}

const removeExcerpt = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `确定下架该书摘吗？请填写作原因：`,
      '下架书摘',
      {
        confirmButtonText: '确定下架',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入下架原因',
        type: 'warning',
        inputValidator: (val) => val ? true : '下架原因不能为空'
      }
    )
    await removePlazaExcerpt(row.id)
    ElMessage.success('已下架')
    fetchData()
  } catch {
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
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
