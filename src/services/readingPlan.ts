import Taro from '@tarojs/taro'
import { ensureLoggedIn } from './user'
import type {
  ReadingPlan,
  ReadingPlanProgress,
  ReadingPlanBadge,
  ReadingPlanCreateDTO,
  ReadingPlanCheckinDTO,
  ReadingPlanCheckin
} from '@/types/readingPlan'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Taro.request.Option = {}): Promise<T> => {
  try {
    await ensureLoggedIn()
  } catch (err) {
    console.error('[ReadingPlan API] 登录失败', err)
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
    console.error('[ReadingPlan API] 请求失败', url, err)
    throw err
  }
}

export const createReadingPlan = async (dto: ReadingPlanCreateDTO): Promise<ReadingPlan | null> => {
  try {
    return await request<ReadingPlan>('/reading-plan/create', {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ReadingPlanService] createReadingPlan failed', err)
    return null
  }
}

export const getReadingPlanList = async (status?: number): Promise<ReadingPlan[]> => {
  try {
    const url = status !== undefined
      ? `/reading-plan/list?status=${status}`
      : '/reading-plan/list'
    const data = await request<ReadingPlan[]>(url)
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ReadingPlanService] getReadingPlanList failed', err)
    return []
  }
}

export const getReadingPlanProgress = async (planId: number): Promise<ReadingPlanProgress | null> => {
  try {
    return await request<ReadingPlanProgress>(`/reading-plan/${planId}/progress`)
  } catch (err) {
    console.error('[ReadingPlanService] getReadingPlanProgress failed', err)
    return null
  }
}

export const checkinReadingPlan = async (dto: ReadingPlanCheckinDTO): Promise<ReadingPlanCheckin | null> => {
  try {
    return await request<ReadingPlanCheckin>('/reading-plan/checkin', {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ReadingPlanService] checkinReadingPlan failed', err)
    return null
  }
}

export const getCheckinCalendar = async (planId: number): Promise<string[]> => {
  try {
    const data = await request<string[]>(`/reading-plan/${planId}/calendar`)
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ReadingPlanService] getCheckinCalendar failed', err)
    return []
  }
}

export const getReadingPlanBadges = async (): Promise<ReadingPlanBadge[]> => {
  try {
    const data = await request<ReadingPlanBadge[]>('/reading-plan/badges')
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ReadingPlanService] getReadingPlanBadges failed', err)
    return []
  }
}

export const abandonReadingPlan = async (planId: number): Promise<boolean> => {
  try {
    await request<void>(`/reading-plan/${planId}/abandon`, {
      method: 'PUT'
    })
    return true
  } catch (err) {
    console.error('[ReadingPlanService] abandonReadingPlan failed', err)
    return false
  }
}
