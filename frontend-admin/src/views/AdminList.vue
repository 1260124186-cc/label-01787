<template>
  <div class="page-container">
    <div class="page-header">
      <h2>管理员管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建管理员</el-button>
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
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag v-if="row.role" :type="getRoleTagType(row.role.code)" size="small">
              {{ row.role.name }}
            </el-tag>
            <el-button v-else type="primary" link size="small" @click="openEditRole(row)">分配角色</el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.statusSwitch" :active-value="1" :inactive-value="0"
              :loading="statusLoadingMap[row.id]" @change="(val) => handleToggleStatus(row, val)"
              :disabled="isLastSuperAdmin(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openEditNickname(row)">修改昵称</el-button>
            <el-button size="small" text @click="openEditRole(row)">分配角色</el-button>
            <el-button type="warning" size="small" text @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-if="row.status === 1" type="danger" size="small" text :disabled="isLastSuperAdmin(row)"
              @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="创建管理员" width="500px" destroy-on-close>
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="字母/数字/下划线，3-50个字符" maxlength="50" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="选填，默认为用户名" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="createForm.roleId" placeholder="请选择角色" style="width: 100%" clearable>
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码" prop="initPassword">
          <el-input v-model="createForm.initPassword" placeholder="留空则自动生成12位随机密码"
            :type="createShowPwd ? 'text' : 'password'" show-password maxlength="50" clearable>
            <template #append>
              <el-button @click="generateRandomPwd">生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <div class="pwd-policy-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>密码规则：长度≥8位；自动生成的密码包含大小写字母、数字、特殊符号</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="createLoading">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordResultVisible" title="管理员创建成功" width="460px"
      :close-on-click-modal="false" :close-on-press-escape="false">
      <el-result icon="success" title="管理员创建成功" sub-title="请妥善保管以下初始密码信息：">
        <template #extra>
          <div class="pwd-result-card">
            <div class="pwd-result-row">
              <span class="pwd-label">用户名：</span>
              <span class="pwd-value">{{ createdUser?.username }}</span>
              <el-button size="small" link type="primary"
                @click="copyText(createdUser?.username)">复制</el-button>
            </div>
            <div class="pwd-result-row">
              <span class="pwd-label">昵称：</span>
              <span class="pwd-value">{{ createdUser?.nickname }}</span>
            </div>
            <div class="pwd-result-row highlight">
              <span class="pwd-label">初始密码：</span>
              <span class="pwd-value pwd-text">{{ createdUser?.initPassword }}</span>
              <el-button size="small" link type="primary"
                @click="copyText(createdUser?.initPassword)">复制密码</el-button>
            </div>
            <div v-if="createdUser?.isRandomPassword" class="pwd-note">
              <el-icon><WarningFilled /></el-icon>
              <span>此密码为系统随机生成，仅显示一次，请立即告知用户修改</span>
            </div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="passwordResultVisible = false">我已保存</el-button>
      </template>
    </el-dialog>

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

    <el-dialog v-model="roleDialogVisible" title="分配/修改角色" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <span>{{ editAdmin?.username }}</span>
        </el-form-item>
        <el-form-item label="选择角色">
          <el-select v-model="editRoleId" placeholder="请选择角色" style="width: 100%" clearable>
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-alert v-if="isCurrentSuperAdmin(editAdmin)" type="warning" show-icon :closable="false"
          title="此操作将修改超级管理员的角色，请谨慎操作（需二次确认）" style="margin-top: 12px;" />
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole" :loading="roleLoading">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="420px">
      <el-result icon="warning" title="确认重置密码" sub-title="系统将生成新的随机密码，请确认操作：">
        <template #extra>
          <div class="reset-pwd-info">
            <p>用户名：<strong>{{ editAdmin?.username }}</strong>（{{ editAdmin?.nickname }}）</p>
            <p class="warning-tip">
              <el-icon><WarningFilled /></el-icon>
              此操作需要二次确认，新密码将仅在重置后显示一次
            </p>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResetPwd" :loading="resetPwdLoading">确认重置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="newPwdDialogVisible" title="密码重置成功" width="420px"
      :close-on-click-modal="false" :close-on-press-escape="false">
      <el-result icon="success" title="密码已重置">
        <template #extra>
          <div class="pwd-result-card">
            <div class="pwd-result-row highlight">
              <span class="pwd-label">新密码：</span>
              <span class="pwd-value pwd-text">{{ newPassword }}</span>
              <el-button size="small" link type="primary" @click="copyText(newPassword)">复制</el-button>
            </div>
            <div class="pwd-note">
              <el-icon><WarningFilled /></el-icon>
              <span>此密码仅显示一次，请立即妥善保管并告知用户</span>
            </div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="newPwdDialogVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import {
  getAdminList, updateAdminNickname, deleteAdmin, getRoleList,
  createAdmin, updateAdminRole, resetAdminPassword, toggleAdminStatus
} from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const roleOptions = ref([])
const roleMap = ref({})
const statusLoadingMap = ref({})

const nicknameDialogVisible = ref(false)
const editNickname = ref('')
const editAdminId = ref(null)
const editAdmin = ref(null)
const nicknameLoading = ref(false)

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createLoading = ref(false)
const createShowPwd = ref(false)
const createForm = ref({
  username: '',
  nickname: '',
  roleId: null,
  initPassword: ''
})
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度需在3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }
  ]
}
const createdUser = ref(null)
const passwordResultVisible = ref(false)

const roleDialogVisible = ref(false)
const editRoleId = ref(null)
const roleLoading = ref(false)

const resetPwdDialogVisible = ref(false)
const newPwdDialogVisible = ref(false)
const newPassword = ref('')
const resetPwdLoading = ref(false)

const roleTypeMap = {
  SUPER_ADMIN: { tag: 'danger' },
  OPERATOR: { tag: 'warning' },
  AUDITOR: { tag: 'info' }
}

const fetchRoles = async () => {
  try {
    const res = await getRoleList()
    roleOptions.value = res.data || []
    const map = {}
    roleOptions.value.forEach(r => { map[r.id] = r })
    roleMap.value = map
  } catch {}
}

const getRoleTagType = (code) => {
  return roleTypeMap[code]?.tag || ''
}

const isLastSuperAdmin = (row) => {
  if (!row.role || row.role.code !== 'SUPER_ADMIN' || row.status !== 1) return false
  const superAdminCount = tableData.value.filter(r => r.role?.code === 'SUPER_ADMIN' && r.status === 1).length
  return superAdminCount <= 1
}

const isCurrentSuperAdmin = (row) => {
  return row?.role?.code === 'SUPER_ADMIN'
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = { page: currentPage.value, size: pageSize.value, keyword: keyword.value }
    const res = await getAdminList(params)
    const records = (res.data.records || []).map(r => ({
      ...r,
      statusSwitch: r.status
    }))
    tableData.value = records
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const openCreateDialog = () => {
  createForm.value = { username: '', nickname: '', roleId: null, initPassword: '' }
  createShowPwd.value = false
  createDialogVisible.value = true
}

const generateRandomPwd = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%'
  let pwd = ''
  for (let i = 0; i < 12; i++) {
    pwd += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  createForm.value.initPassword = pwd
  createShowPwd.value = true
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    const pwd = createForm.value.initPassword
    if (pwd && pwd.length < 8) {
      ElMessage.warning('密码长度不能少于8位')
      return
    }
    createLoading.value = true
    try {
      const res = await createAdmin(createForm.value)
      createdUser.value = res.data
      createDialogVisible.value = false
      passwordResultVisible.value = true
      fetchData()
    } finally {
      createLoading.value = false
    }
  })
}

const copyText = async (text) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

const openEditNickname = (row) => {
  editAdminId.value = row.id
  editAdmin.value = row
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

const openEditRole = (row) => {
  editAdmin.value = row
  editRoleId.value = row.roleId
  roleDialogVisible.value = true
}

const submitRole = async () => {
  if (editRoleId.value === editAdmin.value.roleId) {
    ElMessage.info('角色未变更')
    roleDialogVisible.value = false
    return
  }
  roleLoading.value = true
  try {
    await updateAdminRole(editAdmin.value.id, editRoleId.value)
    ElMessage.success('角色更新成功')
    roleDialogVisible.value = false
    fetchData()
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '更新失败')
  } finally {
    roleLoading.value = false
  }
}

const handleResetPassword = (row) => {
  editAdmin.value = row
  resetPwdDialogVisible.value = true
}

const confirmResetPwd = async () => {
  resetPwdLoading.value = true
  try {
    const res = await resetAdminPassword(editAdmin.value.id)
    newPassword.value = res.data.newPassword
    resetPwdDialogVisible.value = false
    newPwdDialogVisible.value = true
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '重置失败')
  } finally {
    resetPwdLoading.value = false
  }
}

const handleToggleStatus = async (row, val) => {
  if (val === 0 && isLastSuperAdmin(row)) {
    row.statusSwitch = 1
    ElMessage.warning('不能禁用最后一个超级管理员')
    return
  }
  statusLoadingMap.value[row.id] = true
  try {
    await toggleAdminStatus(row.id, val)
    ElMessage.success(val === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch (e) {
    row.statusSwitch = row.status
    ElMessage.error(e.message || '操作失败')
  } finally {
    statusLoadingMap.value[row.id] = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除管理员「${row.username}」？`, '删除确认', { type: 'warning' })
  try {
    await deleteAdmin(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '删除失败')
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
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.pwd-policy-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}
.pwd-result-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  text-align: left;
  max-width: 380px;
  margin: 0 auto;
}
.pwd-result-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
  &.highlight {
    background: #ecf5ff;
    border-radius: 6px;
    padding: 10px 12px;
    margin: 8px -12px;
  }
}
.pwd-label {
  color: #606266;
  width: 90px;
  flex-shrink: 0;
}
.pwd-value {
  color: #303133;
  font-weight: 500;
  flex: 1;
}
.pwd-text {
  font-family: monospace;
  letter-spacing: 1px;
  color: #409eff;
  font-size: 15px;
}
.pwd-note {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.5;
}
.reset-pwd-info {
  text-align: left;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  p { margin: 6px 0; }
}
.warning-tip {
  color: #e6a23c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.text-muted {
  color: #999;
  font-size: 13px;
}
</style>
