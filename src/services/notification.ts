import Taro from '@tarojs/taro'
import { Notification, UnreadCount, PageResult } from '@/types/notification'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Taro.request.Option = {}): Promise<T> => {
  const token = Taro.getStorageSync('token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  try {
    const res = await Taro.request({
      url: `${BASE_URL}${url}`,
      header: headers,
      timeout: 10000,
      ...options
    })
    
    const data = res.data as any
    if (data.code === 200) {
      return data.data
    } else if (data.code === 401) {
      Taro.removeStorageSync('token')
      Taro.navigateTo({ url: '/pages/login/index' })
      throw new Error('登录已过期')
    } else {
      Taro.showToast({ title: data.message || '请求失败', icon: 'none' })
      throw new Error(data.message || '请求失败')
    }
  } catch (err) {
    console.error('[Notification API] 请求失败', url, err)
    if ((err as any).errMsg?.includes('timeout')) {
      Taro.showToast({ title: '网络超时', icon: 'none' })
    }
    throw err
  }
}

export const getNotifications = (type?: number, page: number = 1, size: number = 10) => {
  const params = new URLSearchParams()
  if (type) params.append('type', String(type))
  params.append('page', String(page))
  params.append('size', String(size))
  return request<PageResult<Notification>>(`/notifications?${params.toString()}`)
}

export const getUnreadCount = () => {
  return request<UnreadCount>('/notifications/unread-count')
}

export const getNotificationDetail = (id: number) => {
  return request<Notification>(`/notifications/${id}`)
}

export const markAsRead = (id: number) => {
  return request<void>(`/notifications/${id}/read`, { method: 'PUT' })
}

export const markAllAsRead = (type?: number) => {
  const params = type ? `?type=${type}` : ''
  return request<void>(`/notifications/read-all${params}`, { method: 'PUT' })
}
