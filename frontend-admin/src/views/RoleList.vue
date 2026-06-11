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
            <el-table-column label="角色编码" min-width="130">
              <template #default="{ row }">
                <code class="role-code">{{ row.code }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-if="selectedRole">
          <template #header>
            <div class="role-header">
              <div class="role-header-left">
                <span>{{ selectedRole.name }} — 权限配置</span>
                <el-tag v-if="selectedRole.code === 'SUPER_ADMIN'" type="danger" size="small" style="margin-left: 8px;">
                  超级管理员
                </el-tag>
                <el-tag v-else-if="selectedRole.code" size="small" style="margin-left: 8px;">
                  {{ selectedRole.code }}
                </el-tag>
              </div>
              <div class="role-header-right">
                <el-button size="small" @click="expandAll(true)" :icon="ArrowsExpand">全部展开</el-button>
                <el-button size="small" @click="expandAll(false)" :icon="Fold">全部折叠</el-button>
                <el-button type="primary" size="small" :loading="saveLoading" @click="handleSave" :icon="Check">
                  保存权限
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="selectedRole.code === 'SUPER_ADMIN'" class="super-admin-tip">
            <el-alert type="info" show-icon :closable="false">
              <template #title>
                超级管理员默认拥有所有权限，不需要在此配置。
              </template>
            </el-alert>
          </div>

          <div v-else>
            <div class="tree-stats-bar">
              <span class="stat-item">
                <el-icon><Menu /></el-icon>
                菜单权限：已选 <strong>{{ menuCheckedCount }}</strong>
                <span v-if="menuHalfCheckedCount > 0">（半选 {{ menuHalfCheckedCount }}）</span>
                 / 共 {{ menuTotalCount }}
              </span>
              <span class="stat-item">
                <el-icon><Key /></el-icon>
                接口权限：已选 <strong>{{ apiCheckedCount }}</strong>
                <span v-if="apiHalfCheckedCount > 0">（半选 {{ apiHalfCheckedCount }}）</span>
                 / 共 {{ apiTotalCount }}
              </span>
            </div>

            <el-tree
              ref="treeRef"
              :data="permissionTree"
              show-checkbox
              node-key="id"
              :default-expanded-keys="defaultExpandedKeys"
              :default-checked-keys="checkedKeys"
              :props="{ label: 'name', children: 'children' }"
              :expand-on-click-node="false"
              @check="handleCheckChange"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <span class="node-icon" :title="data.type === 1 ? '菜单权限' : '接口权限'">
                    <el-icon v-if="data.type === 1" class="menu-icon"><Menu /></el-icon>
                    <el-icon v-else class="api-icon"><Key /></el-icon>
                  </span>
                  <span class="node-name" :title="data.name">{{ data.name }}</span>
                  <code class="node-code" :title="data.code">{{ data.code }}</code>
                </div>
              </template>
            </el-tree>
          </div>
        </el-card>

        <el-card v-else>
          <el-empty description="请从左侧选择一个角色进行权限配置" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, ArrowsExpand, Fold, Menu, Key } from '@element-plus/icons-vue'
import { getRoleList, getRoleDetail, getPermissionList, updateRolePermissions } from '@/api/admin'

const Check = markRaw(Check)
const ArrowsExpand = markRaw(ArrowsExpand)
const Fold = markRaw(Fold)
const Menu = markRaw(Menu)
const Key = markRaw(Key)

const roles = ref([])
const roleLoading = ref(false)
const selectedRole = ref(null)
const permissions = ref([])
const checkedKeys = ref([])
const saveLoading = ref(false)
const treeRef = ref(null)
const defaultExpandedKeys = ref([])

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

const menuPermissions = computed(() => permissions.value.filter(p => p.type === 1))
const apiPermissions = computed(() => permissions.value.filter(p => p.type === 2))
const menuTotalCount = computed(() => menuPermissions.value.length)
const apiTotalCount = computed(() => apiPermissions.value.length)

const treeState = computed(() => {
  if (!treeRef.value) return { checked: [], half: [] }
  return {
    checked: treeRef.value.getCheckedKeys(false) || [],
    half: treeRef.value.getHalfCheckedKeys() || []
  }
})

const menuCheckedCount = computed(() => {
  const ids = new Set(treeState.value.checked)
  return menuPermissions.value.filter(p => ids.has(p.id)).length
})

const menuHalfCheckedCount = computed(() => {
  const ids = new Set(treeState.value.half)
  return menuPermissions.value.filter(p => ids.has(p.id)).length
})

const apiCheckedCount = computed(() => {
  const ids = new Set(treeState.value.checked)
  return apiPermissions.value.filter(p => ids.has(p.id)).length
})

const apiHalfCheckedCount = computed(() => {
  const ids = new Set(treeState.value.half)
  return apiPermissions.value.filter(p => ids.has(p.id)).length
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
  checkedKeys.value = []
  defaultExpandedKeys.value = []
  await nextTick()
  if (row.code === 'SUPER_ADMIN') return
  try {
    const res = await getRoleDetail(row.id)
    const role = res.data
    const permIds = (role.permissions || []).map(p => p.id)
    checkedKeys.value = permIds
    await nextTick()
    defaultExpandedKeys.value = permissionTree.value
      .filter(node => !node.parentId)
      .map(node => node.id)
  } catch {
    checkedKeys.value = []
  }
}

const handleCheckChange = () => {
  // 触发响应式更新 treeState
}

const expandAll = (expand) => {
  if (!treeRef.value) return
  const nodes = treeRef.value.store.nodesMap
  for (const key in nodes) {
    if (nodes[key].expanded !== undefined) {
      nodes[key].expanded = expand
    }
  }
}

const handleSave = async () => {
  if (!selectedRole.value) return
  if (selectedRole.value.code === 'SUPER_ADMIN') {
    ElMessage.info('超级管理员拥有所有权限，无需保存')
    return
  }
  const checkedNodes = treeRef.value.getCheckedNodes(false, true)
  const permissionIds = [...new Set(checkedNodes.map(n => n.id))]
  const count = permissionIds.length
  try {
    await ElMessageBox.confirm(
      `将为「${selectedRole.value.name}」配置 ${count} 个权限。此操作将覆盖当前权限配置，${
        selectedRole.value.code === 'SUPER_ADMIN' ? '' : '修改角色权限需要二次确认。'
      }是否继续？`,
      '保存确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  saveLoading.value = true
  try {
    await updateRolePermissions(selectedRole.value.id, permissionIds)
    ElMessage.success(`权限保存成功，共配置 ${count} 个权限`)
    handleRoleSelect(selectedRole.value)
  } catch (e) {
    if (e.message && e.message.includes('用户取消操作')) return
    ElMessage.error(e.message || '保存失败')
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
  gap: 12px;
  flex-wrap: wrap;
}
.role-header-left {
  display: flex;
  align-items: center;
}
.role-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.role-code {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  font-family: 'SF Mono', 'Consolas', monospace;
}
.super-admin-tip {
  margin-bottom: 12px;
}
.tree-stats-bar {
  display: flex;
  gap: 24px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #606266;
  .stat-item {
    display: flex;
    align-items: center;
    gap: 6px;
    strong {
      color: #409eff;
      font-weight: 600;
    }
  }
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
  font-size: 13px;
}
.node-icon {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  .menu-icon {
    color: #409eff;
    font-size: 14px;
  }
  .api-icon {
    color: #67c23a;
    font-size: 14px;
  }
}
.node-name {
  color: #303133;
}
.node-code {
  background: #f0f2f5;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: #909399;
  font-family: 'SF Mono', 'Consolas', monospace;
  margin-left: 4px;
}
</style>
