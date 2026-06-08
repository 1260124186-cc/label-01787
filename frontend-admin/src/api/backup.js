import request from '@/utils/request.js'

export function getBackupList(params) {
  return request({
    url: '/api/admin/backups',
    method: 'get',
    params
  })
}

export function getBackupDetail(id) {
  return request({
    url: `/api/admin/backups/${id}`,
    method: 'get'
  })
}

export function downloadBackup(id) {
  return request({
    url: `/api/admin/backups/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

export function deleteBackup(id) {
  return request({
    url: `/api/admin/backups/${id}`,
    method: 'delete'
  })
}

export function getStorageStats() {
  return request({
    url: '/api/admin/storage',
    method: 'get'
  })
}
