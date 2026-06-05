<template>
  <div class="page-container">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <el-card>
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无日志记录">
        <el-table-column prop="action" label="操作" min-width="160" />
        <el-table-column prop="target" label="目标" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作人类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.userType === 1 ? '' : 'success'" size="small">
              {{ row.userType === 1 ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" min-width="140" />
        <el-table-column prop="createdAt" label="操作时间" min-width="180" />
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
import { getLogList } from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const res = await getLogList({ page: currentPage.value, size: pageSize.value })
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
