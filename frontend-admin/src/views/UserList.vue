<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="小程序用户" name="user" />
        <el-tab-pane label="管理员" name="admin" />
      </el-tabs>

      <div class="search-bar">
        <el-input v-model="keyword" :placeholder="activeTab === 'user' ? '搜索用户昵称' : '搜索管理员'" 
          clearable style="width: 280px" @keyup.enter="fetchData" prefix-icon="Search" />
        <el-button type="primary" :loading="loading" @click="fetchData">搜索</el-button>
      </div>

      <!-- 小程序用户表格 -->
      <el-table v-if="activeTab === 'user'" :data="tableData" stripe v-loading="loading" 
        style="width: 100%" empty-text="暂无用户数据">
        <el-table-column prop="nickname" label="昵称" min-width="160" />
        <el-table-column prop="createdAt" label="注册时间" min-width="180" />
      </el-table>

      <!-- 管理员表格 -->
      <el-table v-else :data="tableData" stripe v-loading="loading" 
        style="width: 100%" empty-text="暂无管理员数据">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="160" />
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openEditNickname(row)">
              修改昵称
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 修改昵称弹窗 -->
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
import { getUserList, getAdminList, updateAdminNickname } from '@/api/admin'

const activeTab = ref('user')
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

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value, keyword: keyword.value }
    const res = activeTab.value === 'user' 
      ? await getUserList(params) 
      : await getAdminList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const handleTabChange = () => {
  keyword.value = ''
  currentPage.value = 1
  fetchData()
}

onMounted(fetchData)

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
