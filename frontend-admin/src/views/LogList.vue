<template>
  <div class="page-container">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-form :inline="true" :model="filters" @submit.prevent>
          <el-form-item label="操作类型">
            <el-input v-model="filters.action" placeholder="搜索操作类型" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="IP地址">
            <el-input v-model="filters.ip" placeholder="搜索IP" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="操作人">
            <el-select v-model="filters.userType" placeholder="全部" clearable style="width: 120px">
              <el-option label="管理员" :value="1" />
              <el-option label="普通用户" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作时间">
            <el-date-picker
              v-model="filters.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 360px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="fetchData">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="success" :icon="Download" :loading="exportLoading" @click="handleExport">
              导出CSV
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无日志记录">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="action" label="操作" min-width="160" />
        <el-table-column prop="target" label="目标" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作人" width="120">
          <template #default="{ row }">
            <el-tag :type="row.userType === 1 ? '' : 'success'" size="small">
              {{ row.userType === 1 ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人ID" width="100">
          <template #default="{ row }">{{ row.userId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" min-width="140" />
        <el-table-column prop="createdAt" label="操作时间" min-width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="680px" destroy-on-close>
      <div v-if="detailData" class="detail-container">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="日志ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detailData.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ detailData.action }}</el-descriptions-item>
          <el-descriptions-item label="目标对象">{{ detailData.target || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人类型">
            <el-tag :type="detailData.userType === 1 ? '' : 'success'" size="small">
              {{ detailData.userType === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作人ID">{{ detailData.userId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP地址" :span="2">{{ detailData.ip || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="detail-section-header">
            <span>操作详情 (detail JSON)</span>
            <div class="detail-actions">
              <el-button size="small" @click="formatJson" :icon="MagicStick">格式化</el-button>
              <el-button size="small" type="primary" @click="copyDetail" :icon="DocumentCopy">复制</el-button>
            </div>
          </div>
          <div class="json-wrapper">
            <pre ref="jsonPreRef" class="json-content">{{ formattedDetail }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, MagicStick, DocumentCopy } from '@element-plus/icons-vue'
import { getLogList, getLogDetail, exportLogsCsv } from '@/api/admin'
import request from '@/utils/request'

const Download = markRaw(Download)
const MagicStick = markRaw(MagicStick)
const DocumentCopy = markRaw(DocumentCopy)

const tableData = ref([])
const loading = ref(false)
const exportLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = reactive({
  action: '',
  ip: '',
  userType: null,
  dateRange: []
})

const detailVisible = ref(false)
const detailData = ref(null)
const formattedDetail = ref('')
const jsonPreRef = ref(null)

const formatJsonDetail = (detail) => {
  if (!detail) return '{}'
  try {
    const parsed = JSON.parse(detail)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return detail
  }
}

const formatJson = () => {
  if (!detailData.value?.detail) return
  try {
    const parsed = JSON.parse(detailData.value.detail)
    formattedDetail.value = JSON.stringify(parsed, null, 2)
    ElMessage.success('已格式化')
  } catch {
    ElMessage.warning('不是合法的JSON，无法格式化')
  }
}

const copyDetail = async () => {
  if (!formattedDetail.value) return
  try {
    await navigator.clipboard.writeText(formattedDetail.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

const openDetail = async (row) => {
  try {
    const res = await getLogDetail(row.id)
    detailData.value = res.data
    formattedDetail.value = formatJsonDetail(res.data.detail)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const buildParams = () => {
  const params = {
    page: currentPage.value,
    size: pageSize.value
  }
  if (filters.action?.trim()) params.action = filters.action.trim()
  if (filters.ip?.trim()) params.ip = filters.ip.trim()
  if (filters.userType != null) params.userType = filters.userType
  if (filters.dateRange?.length === 2) {
    params.startTime = filters.dateRange[0]
    params.endTime = filters.dateRange[1]
  }
  return params
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const res = await getLogList(buildParams())
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const resetFilters = () => {
  filters.action = ''
  filters.ip = ''
  filters.userType = null
  filters.dateRange = []
  currentPage.value = 1
  fetchData()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const params = buildParams()
    delete params.page
    delete params.size
    const res = await request.get('/admin/logs/export', {
      params,
      responseType: 'blob'
    })
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `operation_logs_${Date.now()}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.filter-bar {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.detail-container {
  padding: 4px;
}
.detail-section {
  margin-top: 20px;
}
.detail-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  color: #303133;
}
.detail-actions {
  display: flex;
  gap: 8px;
}
.json-wrapper {
  background: #1e1e1e;
  border-radius: 6px;
  padding: 16px;
  max-height: 420px;
  overflow: auto;
}
.json-content {
  margin: 0;
  color: #d4d4d4;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
