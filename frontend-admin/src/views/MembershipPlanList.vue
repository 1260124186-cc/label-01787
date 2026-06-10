<template>
  <div class="page-container">
    <div class="page-header">
      <h2>套餐配置</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-button type="primary" @click="openDialog()">新建套餐</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无套餐数据">
        <el-table-column prop="code" label="套餐代码" min-width="120" />
        <el-table-column prop="name" label="套餐名称" min-width="120" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">
            ¥{{ (row.price / 100).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="100">
          <template #default="{ row }">
            {{ row.durationDays === 0 ? '永久' : row.durationDays + '天' }}
          </template>
        </el-table-column>
        <el-table-column label="最大书籍数" width="110">
          <template #default="{ row }">
            {{ row.maxBooks === 0 ? '无限' : row.maxBooks }}
          </template>
        </el-table-column>
        <el-table-column label="存储空间" width="100">
          <template #default="{ row }">
            {{ formatStorage(row.maxStorage) }}
          </template>
        </el-table-column>
        <el-table-column label="AI每日限额" width="110">
          <template #default="{ row }">
            {{ row.aiDailyLimit === 0 ? '无限' : row.aiDailyLimit }}
          </template>
        </el-table-column>
        <el-table-column label="优先队列" width="90">
          <template #default="{ row }">
            {{ row.priorityQueue ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="高级统计" width="90">
          <template #default="{ row }">
            {{ row.advancedStats ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑套餐' : '新建套餐'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="套餐代码">
          <el-input v-model="form.code" :disabled="!!editingId" placeholder="如 free、vip" />
        </el-form-item>
        <el-form-item label="套餐名称">
          <el-input v-model="form.name" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="价格(分)">
          <el-input-number v-model="form.price" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="有效期(天)">
          <el-input-number v-model="form.durationDays" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="最大书籍数">
          <el-input-number v-model="form.maxBooks" :min="0" :step="1" placeholder="0表示无限" />
        </el-form-item>
        <el-form-item label="存储空间(GB)">
          <el-input-number v-model="form.maxStorageGB" :min="0" :step="1" :precision="1" />
        </el-form-item>
        <el-form-item label="AI每日限额">
          <el-input-number v-model="form.aiDailyLimit" :min="0" :step="1" placeholder="0表示无限" />
        </el-form-item>
        <el-form-item label="优先队列">
          <el-switch v-model="form.priorityQueue" />
        </el-form-item>
        <el-form-item label="高级统计">
          <el-switch v-model="form.advancedStats" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlanList, createPlan, updatePlan } from '@/api/membership'

const BYTES_PER_GB = 1024 * 1024 * 1024

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const editingId = ref(null)
const form = ref({
  code: '',
  name: '',
  price: 0,
  durationDays: 30,
  maxBooks: 0,
  maxStorageGB: 0,
  aiDailyLimit: 0,
  priorityQueue: false,
  advancedStats: false
})

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const res = await getPlanList()
    tableData.value = res.data || []
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const formatStorage = (bytes) => {
  if (!bytes) return '0 GB'
  const gb = bytes / BYTES_PER_GB
  if (gb >= 1) return gb.toFixed(1) + ' GB'
  const mb = bytes / (1024 * 1024)
  return mb.toFixed(0) + ' MB'
}

const openDialog = (row) => {
  if (row) {
    editingId.value = row.id
    form.value = {
      ...row,
      maxStorageGB: Number((row.maxStorage / BYTES_PER_GB).toFixed(1)),
      priorityQueue: !!row.priorityQueue,
      advancedStats: !!row.advancedStats
    }
  } else {
    editingId.value = null
    form.value = {
      code: '',
      name: '',
      price: 0,
      durationDays: 30,
      maxBooks: 0,
      maxStorageGB: 0,
      aiDailyLimit: 0,
      priorityQueue: false,
      advancedStats: false
    }
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('套餐代码和名称不能为空')
    return
  }
  submitLoading.value = true
  try {
    const payload = {
      ...form.value,
      maxStorage: Math.round(form.value.maxStorageGB * BYTES_PER_GB),
      priorityQueue: form.value.priorityQueue ? 1 : 0,
      advancedStats: form.value.advancedStats ? 1 : 0
    }
    delete payload.maxStorageGB
    if (editingId.value) {
      await updatePlan(editingId.value, payload)
      ElMessage.success('套餐更新成功')
    } else {
      await createPlan(payload)
      ElMessage.success('套餐创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
}
</style>
