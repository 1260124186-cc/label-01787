import Taro from '@tarojs/taro'
import { ensureLoggedIn } from './user'
import type { ContinueReadingItem, ReadingTimelineDay, ReadingSummary } from '@/types/reading'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Taro.request.Option = {}): Promise<T> => {
  try {
    await ensureLoggedIn()
  } catch (err) {
    console.error('[Reading API] 登录失败', err)
    throw err
  }

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
      Taro.removeStorageSync('userInfo')
      throw new Error('登录已过期')
    } else {
      throw new Error(data.message || '请求失败')
    }
  } catch (err) {
    console.error('[Reading API] 请求失败', url, err)
    throw err
  }
}

export const getContinueReadingList = async (limit: number = 5): Promise<ContinueReadingItem[]> => {
  try {
    const data = await request<ContinueReadingItem[]>(`/reading/continue-list?limit=${limit}`)
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ReadingService] getContinueReadingList failed', err)
    return []
  }
}

export const getReadingTimeline = async (period: string = 'month'): Promise<ReadingTimelineDay[]> => {
  try {
    const data = await request<ReadingTimelineDay[]>(`/reading/timeline?period=${period}`)
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ReadingService] getReadingTimeline failed', err)
    return []
  }
}

export const getReadingSummary = async (period: string = 'week'): Promise<ReadingSummary | null> => {
  try {
    return await request<ReadingSummary>(`/reading/summary?period=${period}`)
  } catch (err) {
    console.error('[ReadingService] getReadingSummary failed', err)
    return null
  }
}

export const startReading = async (bookId: number): Promise<{ id: number } | null> => {
  try {
    return await request<{ id: number }>('/reading/start', {
      method: 'POST',
      data: { bookId }
    })
  } catch (err) {
    console.error('[ReadingService] startReading failed', err)
    return null
  }
}

export const endReading = async (recordId: number, lastPage: number): Promise<void> => {
  try {
    await request<void>('/reading/end', {
      method: 'POST',
      data: { recordId, lastPage }
    })
  } catch (err) {
    console.error('[ReadingService] endReading failed', err)
  }
}

export const updateBookProgress = async (bookId: number, lastPage: number): Promise<void> => {
  try {
    await request<void>(`/books/${bookId}/progress`, {
      method: 'PUT',
      data: { lastPage }
    })
  } catch (err) {
    console.error('[ReadingService] updateBookProgress failed', err)
  }
}
