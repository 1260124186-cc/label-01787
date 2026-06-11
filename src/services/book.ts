import Taro from '@tarojs/taro'
import { ensureLoggedIn } from './user'
import type { BookItem } from '@/types/book'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Taro.request.Option = {}): Promise<T> => {
  try {
    await ensureLoggedIn()
  } catch (err) {
    console.error('[Book API] 登录失败', err)
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
    console.error('[Book API] 请求失败', url, err)
    throw err
  }
}

export const getBookList = async (): Promise<BookItem[]> => {
  try {
    const data = await request<{ records: BookItem[] }>('/books?page=1&size=100')
    return Array.isArray(data?.records) ? data.records : Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[BookService] getBookList failed', err)
    return []
  }
}
