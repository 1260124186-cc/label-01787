<template>
  <div class="page-container">
    <div class="page-header">
      <h2>版权申诉</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 160px" @change="fetchData">
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已下架" :value="2" />
          <el-option label="已驳回" :value="3" />
        </el-select>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无申诉工单">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="complainantName" label="申诉人" min-width="100" />
        <el-table-column prop="complainantContact" label="联系方式" min-width="140" />
        <el-table-column prop="bookTitle" label="书籍名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reason" label="申诉原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0 || row.status === 1">
              <el-button type="danger" size="small" @click="handleAction(row, 2)">下架</el-button>
              <el-button size="small" @click="handleAction(row, 3)">驳回</el-button>
              <el-button type="primary" size="small" @click="handleAction(row, 1)" v-if="row.status === 0">处理</el-button>
            </template>
            <span v-else class="handled-text">已处理</span>
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
import { getComplaintList, handleComplaint, requestConfirmToken } from '@/api/admin'
import { ElMessageBox, ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusText = (status) => {
  const map = { 0: '待处理', 1: '处理中', 2: '已下架', 3: '已驳回' }
  return map[status] ?? '未知'
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'info', 2: 'danger', 3: 'success' }
  return map[status] ?? ''
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    const res = await getComplaintList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const handleAction = async (row, newStatus) => {
  const actionLabel = statusText(newStatus)
  try {
    await ElMessageBox.prompt(
      `确定将此申诉标记为"${actionLabel}"？请填写处理结果：`,
      '处理申诉',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入处理结果',
        inputValidator: (val) => val ? true : '处理结果不能为空'
      }
    ).then(async ({ value }) => {
      try {
        const { data } = await requestConfirmToken('handle_complaint')
        await handleComplaint(row.id, {
          status: newStatus,
          handleResult: value,
          confirmToken: data.confirmToken
        })
        ElMessage.success('处理成功')
        fetchData()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '处理失败')
      }
    })
  } catch {
    // cancelled
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
.handled-text {
  color: #999;
  font-size: 13px;
}
</style>
