export interface Notification {
  id: number
  type: number
  typeName: string
  title: string
  content: string
  extraData: string
  isRead: number
  createdAt: string
  readAt?: string
}

export interface NotificationType {
  id: number
  name: string
  icon: string
  color: string
}

export interface UnreadCount {
  0: number
  1: number
  2: number
  3: number
  4: number
  [key: number]: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
