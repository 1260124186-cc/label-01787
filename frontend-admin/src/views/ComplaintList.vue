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
        <el-table-column label="关联书籍" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.bookId" type="primary" size="small">已关联 #{{ row.bookId }}</el-tag>
            <el-tag v-else type="info" size="small">未关联</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申诉原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0 || row.status === 1">
              <el-button type="danger" size="small" @click="handleTakeDown(row)">下架</el-button>
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

    <el-dialog v-model="bookSelectDialogVisible" title="选择关联书籍" width="600px">
      <el-input v-model="bookSearchKeyword" placeholder="搜索书籍" style="margin-bottom: 16px" clearable @input="loadBooks" />
      <el-table :data="allBooks" v-loading="booksLoading" height="300px" @row-click="onBookRowClick"
        highlight-current-row>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" min-width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="bookSelectDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedBookForComplaint" @click="confirmTakeDown">确定下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBooks, getComplaintList, handleComplaint, requestConfirmToken } from '@/api/admin'
import { ElMessageBox, ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const bookSelectDialogVisible = ref(false)
const allBooks = ref([])
const booksLoading = ref(false)
const bookSearchKeyword = ref('')
const selectedBookForComplaint = ref(null)
const currentComplaint = ref(null)
const currentHandleResult = ref('')

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

const loadBooks = async () => {
  booksLoading.value = true
  try {
    const res = await getBooks({ page: 1, size: 200, keyword: bookSearchKeyword.value || undefined })
    allBooks.value = res.data.records || []
  } finally {
    booksLoading.value = false
  }
}

const onBookRowClick = (row) => {
  selectedBookForComplaint.value = row
}

const handleTakeDown = async (row) => {
  if (row.bookId) {
    try {
      await ElMessageBox.prompt(
        `确定下架书籍「${row.bookTitle}」？请填写处理结果：`,
        '下架书籍',
        {
          confirmButtonText: '确定下架',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入处理结果',
          type: 'warning',
          inputValidator: (val) => val ? true : '处理结果不能为空'
        }
      ).then(async ({ value }) => {
        try {
          const { data } = await requestConfirmToken('handle_complaint')
          await handleComplaint(row.id, {
            status: 2,
            handleResult: value,
            bookId: row.bookId,
            confirmToken: data.confirmToken
          })
          ElMessage.success('下架成功')
          fetchData()
        } catch (e) {
          ElMessage.error(e.response?.data?.message || '处理失败')
        }
      })
    } catch {
      // cancelled
    }
  } else {
    currentComplaint.value = row
    selectedBookForComplaint.value = null
    bookSearchKeyword.value = row.bookTitle || ''
    bookSelectDialogVisible.value = true
    loadBooks()
  }
}

const confirmTakeDown = async () => {
  if (!selectedBookForComplaint.value) {
    ElMessage.warning('请先选择要下架的书籍')
    return
  }
  try {
    await ElMessageBox.prompt(
      `确定将申诉关联到书籍「${selectedBookForComplaint.value.title}」并下架？请填写处理结果：`,
      '关联并下架',
      {
        confirmButtonText: '确定下架',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入处理结果',
        type: 'warning',
        inputValidator: (val) => val ? true : '处理结果不能为空'
      }
    ).then(async ({ value }) => {
      try {
        const { data } = await requestConfirmToken('handle_complaint')
        await handleComplaint(currentComplaint.value.id, {
          status: 2,
          handleResult: value,
          bookId: selectedBookForComplaint.value.id,
          confirmToken: data.confirmToken
        })
        ElMessage.success('下架成功')
        bookSelectDialogVisible.value = false
        fetchData()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '处理失败')
      }
    })
  } catch {
    // cancelled
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
