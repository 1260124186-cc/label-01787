<template>
  <div class="page-container">
    <div class="page-header">
      <h2>订单列表</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="订单状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="全部" :value="null" />
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已退款" :value="3" />
        </el-select>
        <el-input v-model="orderNo" placeholder="搜索订单号" clearable style="width: 240px; margin-left: 12px"
          @keyup.enter="fetchData" prefix-icon="Search" />
        <el-button type="primary" :loading="loading" @click="fetchData" style="margin-left: 12px">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无订单数据">
        <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column label="订单类型" width="110">
          <template #default="{ row }">
            {{ row.orderType === 1 ? '会员购买' : row.orderType === 2 ? '存储包' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            ¥{{ (row.amount / 100).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="wxTransactionId" label="微信交易号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="paidAt" label="支付时间" min-width="180" />
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
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
import { getOrderList } from '@/api/membership'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref(null)
const orderNo = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusText = (status) => {
  const map = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款' }
  return map[status] ?? status
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] ?? ''
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    if (orderNo.value) params.orderNo = orderNo.value
    const res = await getOrderList(params)
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
</style>
