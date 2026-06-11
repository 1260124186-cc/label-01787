<template>
  <div class="page-container">
    <div class="page-header">
      <h2>文件下载审计</h2>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-form :inline="true" :model="filterForm" @submit.prevent>
          <el-form-item label="用户类型">
            <el-select v-model="filterForm.userType" placeholder="全部" clearable style="width: 140px">
              <el-option label="管理员" :value="1" />
              <el-option label="小程序用户" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="用户ID">
            <el-input v-model="filterForm.userId" placeholder="请输入用户ID" style="width: 160px" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无下载记录">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="用户类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.userType === 1 ? '' : 'success'" size="small">
              {{ row.userType === 1 ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="filePath" label="文件路径" min-width="220" show-overflow-tooltip />
        <el-table-column prop="fileToken" label="签名令牌" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" min-width="140" />
        <el-table-column prop="referer" label="来源页" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userAgent" label="浏览器UA" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="下载时间" min-width="180" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDownloadLogList } from '@/api/admin'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const filterForm = reactive({
  userType: null,
  userId: ''
})

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterForm.userType !== null && filterForm.userType !== '') {
      params.userType = filterForm.userType
    }
    if (filterForm.userId && filterForm.userId.trim()) {
      const uid = parseInt(filterForm.userId.trim())
      if (!isNaN(uid)) {
        params.userId = uid
      } else {
        ElMessage.warning('用户ID必须是数字')
        loading.value = false
        return
      }
    }
    const res = await getDownloadLogList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handleReset = () => {
  filterForm.userType = null
  filterForm.userId = ''
  currentPage.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.filter-bar {
  margin-bottom: 16px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
