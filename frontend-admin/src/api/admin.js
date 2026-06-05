import request from '@/utils/request'

export function login(data) {
  return request.post('/admin/login', data)
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getUserList(params) {
  return request.get('/admin/users', { params })
}

export function disableUser(id) {
  return request.put(`/admin/users/${id}/disable`, null, {
    headers: { 'X-Sensitive-Operation': 'disable_user' }
  })
}

export function getBookList(params) {
  return request.get('/admin/books', { params })
}

export function deleteBook(id) {
  return request.delete(`/admin/books/${id}`, {
    headers: { 'X-Sensitive-Operation': 'delete_book' }
  })
}

export function getLogList(params) {
  return request.get('/admin/logs', { params })
}

export function getAdminList(params) {
  return request.get('/admin/admins', { params })
}

export function updateAdminNickname(id, nickname) {
  return request.put(`/admin/admins/${id}/nickname`, { nickname })
}

export function deleteAdmin(id) {
  return request.delete(`/admin/admins/${id}`, {
    headers: { 'X-Sensitive-Operation': 'delete_admin' }
  })
}

export function getRoleList() {
  return request.get('/admin/roles')
}

export function getRoleDetail(id) {
  return request.get(`/admin/roles/${id}`)
}

export function updateRolePermissions(id, permissionIds) {
  return request.put(`/admin/roles/${id}/permissions`, { permissionIds }, {
    headers: { 'X-Sensitive-Operation': 'update_role_permissions' }
  })
}

export function getPermissionList() {
  return request.get('/admin/permissions')
}

export function getMyPermissions() {
  return request.get('/admin/permissions/mine')
}

export function requestConfirmToken(operation) {
  return request.post('/admin/sensitive/confirm-token', { operation })
}

export function getComplaintList(params) {
  return request.get('/admin/complaints', { params })
}

export function handleComplaint(id, data) {
  return request.put(`/admin/complaints/${id}/handle`, data, {
    headers: { 'X-Sensitive-Operation': 'handle_complaint' }
  })
}

export function getAuditList(params) {
  return request.get('/admin/audits', { params })
}

export function getComplianceReport(params) {
  return request.get('/admin/compliance/report', { params })
}
