import request from '@/utils/request'

export function getIndexStatus(params) {
  return request.get('/admin/search/index-status', { params })
}

export function getFailedAlerts() {
  return request.get('/admin/search/alerts')
}

export function rebuildIndex(bookId) {
  return request.post(`/admin/search/rebuild/${bookId}`)
}

export function rebuildAllIndex() {
  return request.post('/admin/search/rebuild-all')
}

export function retryIndexTask(taskId) {
  return request.post(`/admin/search/retry/${taskId}`)
}

export function getBookIndexStatus(bookId) {
  return request.get(`/api/mp/search/index-status/${bookId}`)
}
