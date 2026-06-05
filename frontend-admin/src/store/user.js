import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

function getSavedNickname() {
  const saved = localStorage.getItem('admin_nickname')
  if (!saved) return '管理员'
  try {
    const decoded = decodeURIComponent(saved)
    return decoded || '管理员'
  } catch {
    return saved || '管理员'
  }
}

function getSavedPermissions() {
  try {
    const saved = localStorage.getItem('admin_permissions')
    return saved ? JSON.parse(saved) : []
  } catch {
    return []
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const nickname = ref(getSavedNickname())
  const roleId = ref(localStorage.getItem('admin_role_id') || '')
  const roleCode = ref(localStorage.getItem('admin_role_code') || '')
  const permissions = ref(getSavedPermissions())

  const isSuperAdmin = computed(() => roleCode.value === 'SUPER_ADMIN')

  function setLogin(data) {
    token.value = data.token
    nickname.value = data.nickname || data.username || '管理员'
    roleId.value = data.roleId || ''
    roleCode.value = data.roleCode || ''
    permissions.value = data.permissions || []
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_nickname', encodeURIComponent(nickname.value))
    localStorage.setItem('admin_role_id', String(roleId.value))
    localStorage.setItem('admin_role_code', roleCode.value)
    localStorage.setItem('admin_permissions', JSON.stringify(permissions.value))
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    roleId.value = ''
    roleCode.value = ''
    permissions.value = []
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_nickname')
    localStorage.removeItem('admin_role_id')
    localStorage.removeItem('admin_role_code')
    localStorage.removeItem('admin_permissions')
  }

  const isLoggedIn = () => !!token.value

  function hasPermission(code) {
    if (isSuperAdmin.value) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(...codes) {
    if (isSuperAdmin.value) return true
    return codes.some(code => permissions.value.includes(code))
  }

  return {
    token, nickname, roleId, roleCode, permissions, isSuperAdmin,
    setLogin, logout, isLoggedIn, hasPermission, hasAnyPermission
  }
})
