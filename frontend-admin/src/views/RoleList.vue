<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色权限管理</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>角色列表</span>
          </template>
          <el-table :data="roles" stripe v-loading="roleLoading" highlight-current-row
            @current-change="handleRoleSelect" style="width: 100%">
            <el-table-column prop="name" label="角色名称" min-width="120" />
            <el-table-column prop="code" label="编码" min-width="130" />
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-if="selectedRole">
          <template #header>
            <div class="role-header">
              <span>{{ selectedRole.name }} — 权限配置</span>
              <el-button type="primary" size="small" :loading="saveLoading" @click="handleSave">
                保存权限
              </el-button>
            </div>
          </template>

          <el-tree ref="treeRef" :data="permissionTree" show-checkbox node-key="id"
            :default-checked-keys="checkedKeys" :props="{ label: 'name', children: 'children' }"
            default-expand-all />
        </el-card>

        <el-card v-else>
          <el-empty description="请从左侧选择一个角色" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoleList, getRoleDetail, getPermissionList, updateRolePermissions } from '@/api/admin'

const roles = ref([])
const roleLoading = ref(false)
const selectedRole = ref(null)
const permissions = ref([])
const checkedKeys = ref([])
const saveLoading = ref(false)
const treeRef = ref(null)

const permissionTree = computed(() => {
  const map = {}
  const roots = []
  permissions.value.forEach(p => {
    map[p.id] = { ...p, children: [] }
  })
  permissions.value.forEach(p => {
    if (p.parentId && map[p.parentId]) {
      map[p.parentId].children.push(map[p.id])
    } else {
      roots.push(map[p.id])
    }
  })
  return roots
})

const fetchRoles = async () => {
  roleLoading.value = true
  try {
    const res = await getRoleList()
    roles.value = res.data || []
  } finally {
    roleLoading.value = false
  }
}

const fetchPermissions = async () => {
  try {
    const res = await getPermissionList()
    permissions.value = res.data || []
  } catch {}
}

const handleRoleSelect = async (row) => {
  if (!row) return
  selectedRole.value = row
  try {
    const res = await getRoleDetail(row.id)
    const role = res.data
    checkedKeys.value = (role.permissions || [])
      .filter(p => p.type === 2)
      .map(p => p.id)
  } catch {
    checkedKeys.value = []
  }
}

const handleSave = async () => {
  if (!selectedRole.value) return
  saveLoading.value = true
  try {
    const checkedNodes = treeRef.value.getCheckedNodes(false, true)
    const permissionIds = [...new Set(checkedNodes.map(n => n.id))]
    await updateRolePermissions(selectedRole.value.id, permissionIds)
    ElMessage.success('权限保存成功')
    handleRoleSelect(selectedRole.value)
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})
</script>

<style lang="scss" scoped>
.role-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
