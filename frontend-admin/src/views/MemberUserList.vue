<template>
  <div class="page-container">
    <div class="page-header">
      <h2>会员管理</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索用户昵称" clearable style="width: 280px"
          @keyup.enter="fetchData" prefix-icon="Search" />
        <el-button type="primary" :loading="loading" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无会员数据">
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <el-avatar :size="32" :src="row.avatar" />
          </template>
        </el-table-column>
        <el-table-column label="套餐" width="100">
          <template #default="{ row }">
            <el-tag :type="row.planCode === 'free' ? 'info' : 'warning'" size="small">
              {{ row.planCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planName" label="套餐名称" min-width="120" />
        <el-table-column prop="expireAt" label="到期时间" min-width="180" />
        <el-table-column prop="pointsBalance" label="积分余额" width="100" />
        <el-table-column label="额外存储" width="100">
          <template #default="{ row }">
            {{ formatStorage(row.extraStorage) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openPointsDialog(row)">调整积分</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>积分规则</span>
      </template>
      <el-table :data="rulesData" stripe v-loading="rulesLoading" style="width: 100%" empty-text="暂无积分规则">
        <el-table-column prop="code" label="规则代码" min-width="140" />
        <el-table-column prop="name" label="规则名称" min-width="140" />
        <el-table-column prop="category" label="分类" min-width="100" />
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column label="每日上限" width="100">
          <template #default="{ row }">
            {{ row.dailyLimit === 0 ? '无限' : row.dailyLimit }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openRuleDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="pointsDialogVisible" title="调整积分" width="440px">
      <el-form :model="pointsForm" label-width="80px">
        <el-form-item label="积分变动">
          <el-input-number v-model="pointsForm.points" :step="1" placeholder="正数增加，负数扣除" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="pointsForm.description" type="textarea" :rows="3" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pointsLoading" @click="submitPoints">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ruleDialogVisible" title="编辑积分规则" width="440px">
      <el-form :model="ruleForm" label-width="80px">
        <el-form-item label="积分">
          <el-input-number v-model="ruleForm.points" :step="1" />
        </el-form-item>
        <el-form-item label="每日上限">
          <el-input-number v-model="ruleForm.dailyLimit" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleLoading" @click="submitRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMemberList, adjustPoints, getPointsRules, updatePointsRule } from '@/api/membership'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const rulesData = ref([])
const rulesLoading = ref(false)

const pointsDialogVisible = ref(false)
const pointsLoading = ref(false)
const pointsForm = ref({ userId: null, points: 0, description: '' })

const ruleDialogVisible = ref(false)
const ruleLoading = ref(false)
const ruleForm = ref({ id: null, points: 0, dailyLimit: 0 })

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value, keyword: keyword.value }
    const res = await getMemberList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const fetchRules = async () => {
  rulesLoading.value = true
  try {
    const res = await getPointsRules()
    rulesData.value = res.data || []
  } finally {
    rulesLoading.value = false
  }
}

const formatStorage = (bytes) => {
  if (!bytes) return '0 GB'
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) return gb.toFixed(1) + ' GB'
  const mb = bytes / (1024 * 1024)
  return mb.toFixed(0) + ' MB'
}

const openPointsDialog = (row) => {
  pointsForm.value = { userId: row.userId || row.id, points: 0, description: '' }
  pointsDialogVisible.value = true
}

const submitPoints = async () => {
  if (!pointsForm.value.description.trim()) {
    ElMessage.warning('请填写调整原因')
    return
  }
  pointsLoading.value = true
  try {
    await adjustPoints(pointsForm.value)
    ElMessage.success('积分调整成功')
    pointsDialogVisible.value = false
    fetchData()
  } finally {
    pointsLoading.value = false
  }
}

const openRuleDialog = (row) => {
  ruleForm.value = { id: row.id, points: row.points, dailyLimit: row.dailyLimit }
  ruleDialogVisible.value = true
}

const submitRule = async () => {
  ruleLoading.value = true
  try {
    await updatePointsRule(ruleForm.value.id, {
      points: ruleForm.value.points,
      dailyLimit: ruleForm.value.dailyLimit
    })
    ElMessage.success('规则更新成功')
    ruleDialogVisible.value = false
    fetchRules()
  } finally {
    ruleLoading.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchRules()
})
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
</style>
