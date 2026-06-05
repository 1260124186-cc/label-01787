import request from '@/utils/request'

// 登录
export function login(data) {
  return request.post('/admin/login', data)
}

// 仪表盘
export function getDashboard() {
  return request.get('/admin/dashboard')
}

// 用户列表
export function getUserList(params) {
  return request.get('/admin/users', { params })
}

// 书籍列表
export function getBookList(params) {
  return request.get('/admin/books', { params })
}

// 操作日志
export function getLogList(params) {
  return request.get('/admin/logs', { params })
}

// 管理员列表
export function getAdminList(params) {
  return request.get('/admin/admins', { params })
}

// 修改管理员昵称
export function updateAdminNickname(id, nickname) {
  return request.put(`/admin/admins/${id}/nickname`, { nickname })
}
