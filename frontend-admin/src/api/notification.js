import request from '@/utils/request'

export function getNotificationList(params) {
  return request.get('/admin/notifications', { params })
}

export function sendAnnouncement(data) {
  return request.post('/admin/notifications/send', data)
}

export function getTemplateList(params) {
  return request.get('/admin/notification-templates', { params })
}

export function getEnabledTemplates() {
  return request.get('/admin/notification-templates/enabled')
}

export function getTemplate(id) {
  return request.get(`/admin/notification-templates/${id}`)
}

export function createTemplate(data) {
  return request.post('/admin/notification-templates', data)
}

export function updateTemplate(id, data) {
  return request.put(`/admin/notification-templates/${id}`, data)
}

export function deleteTemplate(id) {
  return request.delete(`/admin/notification-templates/${id}`)
}

export function toggleTemplateStatus(id) {
  return request.put(`/admin/notification-templates/${id}/toggle-status`)
}
