<template>
  <div class="page-container">
    <div class="page-header">
      <h2>备份任务监控</h2>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="任务状态" clearable style="width: 140px; margin-right: 12px;" @change="fetchData">
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="失败" :value="3" />
        </el-select>
        <el-button type="primary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon success">
              <el-icon><Check /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.completed }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon warning">
              <el-icon><Loading /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.processing }}</div>
              <div class="stat-label">处理中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon info">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.pending }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon danger">
              <el-icon><Close /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.failed }}</div>
              <div class="stat-label">失败</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无备份任务">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="userNickname" label="用户" min-width="120" />
        <el-table-column label="任务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.taskType === 1 ? 'primary' : 'success'" size="small">
              {{ row.taskTypeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="140">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="row.status === 3 ? 'exception' : (row.status === 2 ? 'success' : '')" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileSizeText" label="文件大小" width="120" />
        <el-table-column label="数据统计" min-width="240">
          <template #default="{ row }">
            <div class="data-stats">
              <span class="stat-item"><el-icon><Collection /></el-icon> {{ row.bookCount || 0 }}</span>
              <span class="stat-item"><el-icon><EditPen /></el-icon> {{ row.annotationCount || 0 }}</span>
              <span class="stat-item"><el-icon><Timer /></el-icon> {{ row.recordCount || 0 }}</span>
              <span class="stat-item"><el-icon><Folder /></el-icon> {{ row.categoryCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)" :disabled="row.status === 1">
              详情
            </el-button>
            <el-button link type="success" size="small" @click="download(row)" :disabled="row.status !== 2 || !row.fileName">
              下载
            </el-button>
            <el-button link type="danger" size="small" @click="deleteTask(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="任务详情" width="600px">
      <el-descriptions :column="2" border v-if="currentTask">
        <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentTask.userNickname }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ currentTask.taskTypeText }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentTask.status)">{{ currentTask.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件名" :span="2">{{ currentTask.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ currentTask.fileSizeText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ currentTask.progress }}%</el-descriptions-item>
        <el-descriptions-item label="书籍数量">{{ currentTask.bookCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="批注数量">{{ currentTask.annotationCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="阅读记录">{{ currentTask.recordCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="分类数量">{{ currentTask.categoryCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTask.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentTask.updatedAt }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ currentTask.expiredAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          <span v-if="currentTask.errorMessage" style="color: #f56c6c;">{{ currentTask.errorMessage }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="download(currentTask)" :disabled="currentTask?.status !== 2">
          下载文件
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Check, Loading, Clock, Close, Collection, EditPen, Timer, Folder } from '@element-plus/icons-vue'
import { getBackupList, getBackupDetail, downloadBackup, deleteBackup } from '@/api/backup'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref(null)
const detailVisible = ref(false)
const currentTask = ref(null)

const stats = reactive({
  completed: 0,
  processing: 0,
  pending: 0,
  failed: 0
})

const getStatusType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'warning'
    case 2: return 'success'
    case 3: return 'danger'
    default: return 'info'
  }
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (statusFilter.value !== null) {
      params.status = statusFilter.value
    }
    const res = await getBackupList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0

    stats.completed = tableData.value.filter(t => t.status === 2).length
    stats.processing = tableData.value.filter(t => t.status === 1).length
    stats.pending = tableData.value.filter(t => t.status === 0).length
    stats.failed = tableData.value.filter(t => t.status === 3).length
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const viewDetail = async (row) => {
  try {
    const res = await getBackupDetail(row.id)
    currentTask.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const download = async (row) => {
  try {
    const res = await downloadBackup(row.id)
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName || 'backup.zip'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const deleteTask = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除此备份任务吗？', '确认删除', {
      type: 'warning'
    })
    await deleteBackup(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  align-items: center;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;

  &.success {
    background: linear-gradient(135deg, #67c23a, #85ce61);
  }
  &.warning {
    background: linear-gradient(135deg, #e6a23c, #f0c78a);
  }
  &.info {
    background: linear-gradient(135deg, #409eff, #66b1ff);
  }
  &.danger {
    background: linear-gradient(135deg, #f56c6c, #f89898);
  }
}

.stat-content {
  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 4px;
  }
}

.data-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #606266;
  }
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
