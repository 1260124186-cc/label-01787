const { request } = require('./request')

const notificationTypes = [
  { id: 1, name: '系统通知', icon: '🔔', color: '#165DFF' },
  { id: 2, name: '审核结果', icon: '📋', color: '#FF7D00' },
  { id: 3, name: '计划提醒', icon: '⏰', color: '#00B42A' },
  { id: 4, name: '小组动态', icon: '👥', color: '#722ED1' }
]

function getTypeName(type) {
  const item = notificationTypes.find(t => t.id === type)
  return item ? item.name : '系统通知'
}

function getTypeIcon(type) {
  const item = notificationTypes.find(t => t.id === type)
  return item ? item.icon : '🔔'
}

function getTypeColor(type) {
  const item = notificationTypes.find(t => t.id === type)
  return item ? item.color : '#165DFF'
}

function getNotifications(type, page, size) {
  const params = []
  if (type) params.push(`type=${type}`)
  params.push(`page=${page || 1}`)
  params.push(`size=${size || 10}`)
  return request({
    url: `/notifications?${params.join('&')}`
  })
}

function getUnreadCount() {
  return request({
    url: '/notifications/unread-count'
  })
}

function getNotificationDetail(id) {
  return request({
    url: `/notifications/${id}`
  })
}

function markAsRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'PUT'
  })
}

function markAllAsRead(type) {
  const url = type ? `/notifications/read-all?type=${type}` : '/notifications/read-all'
  return request({
    url,
    method: 'PUT'
  })
}

module.exports = {
  notificationTypes,
  getTypeName,
  getTypeIcon,
  getTypeColor,
  getNotifications,
  getUnreadCount,
  getNotificationDetail,
  markAsRead,
  markAllAsRead
}
