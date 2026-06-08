import { Notification, NotificationType, UnreadCount, PageResult } from '@/types/notification'

export const notificationTypes: NotificationType[] = [
  { id: 1, name: '系统通知', icon: '🔔', color: '#165DFF' },
  { id: 2, name: '审核结果', icon: '📋', color: '#FF7D00' },
  { id: 3, name: '计划提醒', icon: '⏰', color: '#00B42A' },
  { id: 4, name: '小组动态', icon: '👥', color: '#722ED1' }
]

export const mockUnreadCount: UnreadCount = {
  0: 5,
  1: 2,
  2: 1,
  3: 1,
  4: 1
}

const baseNotifications: Notification[] = [
  {
    id: 1,
    type: 1,
    typeName: '系统通知',
    title: '系统升级维护通知',
    content: '为提升用户体验，系统将于本周六凌晨2:00-4:00进行升级维护，届时将暂停服务，请提前做好准备。感谢您的理解与支持！',
    extraData: '',
    isRead: 0,
    createdAt: '2026-06-08 10:30:00'
  },
  {
    id: 2,
    type: 2,
    typeName: '审核结果',
    title: '书籍审核通过',
    content: '您上传的书籍《深入理解计算机系统》已通过审核，现在可以开始阅读了！',
    extraData: JSON.stringify({ page: 'reader', bookId: 123 }),
    isRead: 0,
    createdAt: '2026-06-07 15:20:00'
  },
  {
    id: 3,
    type: 3,
    typeName: '计划提醒',
    title: '阅读计划提醒',
    content: '您今天的阅读计划还未完成，目标阅读时长30分钟，快去阅读吧！坚持打卡赢积分~',
    extraData: JSON.stringify({ page: 'reader', bookId: 456 }),
    isRead: 0,
    createdAt: '2026-06-08 09:00:00'
  },
  {
    id: 4,
    type: 4,
    typeName: '小组动态',
    title: '小组有新动态',
    content: '书友「小明」在「经典文学共读」小组发布了新笔记：《百年孤独》读后感分享，快来看看吧！',
    extraData: JSON.stringify({ page: 'group', groupId: 789 }),
    isRead: 0,
    createdAt: '2026-06-07 20:15:00'
  },
  {
    id: 5,
    type: 1,
    typeName: '系统通知',
    title: '新功能上线',
    content: '阅读笔记导出功能上线啦！您现在可以将读书笔记导出为PDF格式，方便整理和分享。',
    extraData: '',
    isRead: 1,
    createdAt: '2026-06-06 14:00:00',
    readAt: '2026-06-06 15:30:00'
  },
  {
    id: 6,
    type: 2,
    typeName: '审核结果',
    title: '书籍审核未通过',
    content: '您上传的书籍《xxx》未通过审核，原因：内容涉及版权问题，请确认版权后重新上传。',
    extraData: JSON.stringify({ page: 'bookshelf' }),
    isRead: 1,
    createdAt: '2026-06-05 11:00:00',
    readAt: '2026-06-05 12:00:00'
  }
]

export const getMockNotifications = (type?: number, page: number = 1, size: number = 10): PageResult<Notification> => {
  let data = [...baseNotifications]
  if (type) {
    data = data.filter(n => n.type === type)
  }
  
  const start = (page - 1) * size
  const end = start + size
  const records = data.slice(start, end)
  
  return {
    records,
    total: data.length,
    size,
    current: page,
    pages: Math.ceil(data.length / size)
  }
}

export const getMockNotificationDetail = (id: number): Notification | undefined => {
  return baseNotifications.find(n => n.id === id)
}
