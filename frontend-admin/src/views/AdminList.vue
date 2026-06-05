<template>
  <div class="page-container">
    <div class="page-header">
      <h2>管理员管理</h2>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索管理员" clearable style="width: 280px"
          @keyup.enter="fetchData" prefix-icon="Search" />
        <el-button type="primary" :loading="loading" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无管理员数据">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="roleMap[row.roleId]" :type="roleMap[row.roleId].type" size="small">
              {{ roleMap[row.roleId].name }}
            </el-tag>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openEditNickname(row)">修改昵称</el-button>
            <el-button v-if="row.status === 1" type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="nicknameDialogVisible" title="修改昵称" width="400px">
        <el-form @submit.prevent="submitNickname">
          <el-form-item label="昵称">
            <el-input v-model="editNickname" placeholder="请输入新昵称" maxlength="50" clearable />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="nicknameDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitNickname" :loading="nicknameLoading">确定</el-button>
        </template>
      </el-dialog>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminList, updateAdminNickname, deleteAdmin, getRoleList } from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const nicknameDialogVisible = ref(false)
const editNickname = ref('')
const editAdminId = ref(null)
const nicknameLoading = ref(false)
const roleMap = ref({})

const roleTypeMap = {
  SUPER_ADMIN: { name: '超级管理员', type: 'danger' },
  OPERATOR: { name: '运营', type: 'warning' },
  AUDITOR: { name: '只读审计', type: 'info' }
}

const fetchRoles = async () => {
  try {
    const res = await getRoleList()
    const roles = res.data || []
    const map = {}
    roles.forEach(r => {
      map[r.id] = roleTypeMap[r.code] || { name: r.name, type: '' }
    })
    roleMap.value = map
  } catch {}
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value, keyword: keyword.value }
    const res = await getAdminList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const openEditNickname = (row) => {
  editAdminId.value = row.id
  editNickname.value = row.nickname || ''
  nicknameDialogVisible.value = true
}

const submitNickname = async () => {
  if (!editNickname.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  nicknameLoading.value = true
  try {
    await updateAdminNickname(editAdminId.value, editNickname.value.trim())
    ElMessage.success('昵称修改成功')
    nicknameDialogVisible.value = false
    fetchData()
  } finally {
    nicknameLoading.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除管理员「${row.username}」？此操作需要二次确认。`, '删除确认', { type: 'warning' })
  try {
    await deleteAdmin(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e.message !== '用户取消操作') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchRoles()
  fetchData()
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
.text-muted {
  color: #999;
  font-size: 13px;
}
</style>
