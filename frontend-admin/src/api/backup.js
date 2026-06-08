import request from '@/utils/request'

export function getBackupList(params) {
  return request.get('/admin/backups', { params })
}

export function getBackupDetail(id) {
  return request.get(`/admin/backups/${id}`)
}

export function downloadBackup(id) {
  return request.get(`/admin/backups/${id}/download`, { responseType: 'blob' })
}

export function deleteBackup(id) {
  return request.delete(`/admin/backups/${id}`)
}

export function getStorageStats() {
  return request.get('/admin/storage')
}
