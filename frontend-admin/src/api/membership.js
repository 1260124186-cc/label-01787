import request from '@/utils/request'

export function getPlanList() {
  return request.get('/admin/membership/plans')
}

export function createPlan(data) {
  return request.post('/admin/membership/plans', data)
}

export function updatePlan(id, data) {
  return request.put(`/admin/membership/plans/${id}`, data)
}

export function getOrderList(params) {
  return request.get('/admin/membership/orders', { params })
}

export function getMemberList(params) {
  return request.get('/admin/membership/members', { params })
}

export function getPointsRules() {
  return request.get('/admin/membership/points-rules')
}

export function updatePointsRule(id, data) {
  return request.put(`/admin/membership/points-rules/${id}`, data)
}

export function adjustPoints(data) {
  return request.post('/admin/membership/points-adjust', data)
}

export function getConvertTaskList(params) {
  return request.get('/admin/convert/tasks', { params })
}

export function getConvertTask(id) {
  return request.get(`/admin/convert/task/${id}`)
}

export function getAiSummaryList(params) {
  return request.get('/admin/ai/summaries', { params })
}

export function getAiSummary(id) {
  return request.get(`/admin/ai/summary/${id}`)
}
