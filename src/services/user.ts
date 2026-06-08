import Taro from '@tarojs/taro'

const BASE_URL = 'http://localhost:8080/api/mp'

export interface LoginResult {
  userId: number
  nickname: string
  avatar: string
  token: string
}

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
      Taro.removeStorageSync('userInfo')
      throw new Error('登录已过期')
    } else {
      throw new Error(data.message || '请求失败')
    }
  } catch (err) {
    console.error('[User API] 请求失败', url, err)
    if ((err as any).errMsg?.includes('timeout')) {
      Taro.showToast({ title: '网络超时', icon: 'none' })
    }
    throw err
  }
}

export const mpLogin = async (code: string): Promise<LoginResult> => {
  return request<LoginResult>('/login', {
    method: 'POST',
    data: { code }
  })
}

export const ensureLoggedIn = async (): Promise<string> => {
  let token = Taro.getStorageSync('token')
  if (token) {
    return token
  }

  try {
    const result = await mpLogin('test_code_001')
    Taro.setStorageSync('token', result.token)
    Taro.setStorageSync('userInfo', {
      userId: result.userId,
      nickname: result.nickname,
      avatar: result.avatar
    })
    return result.token
  } catch (err) {
    console.error('[User] 自动登录失败', err)
    throw err
  }
}

export const getUserInfo = () => {
  return Taro.getStorageSync('userInfo') || null
}

export const logout = () => {
  Taro.removeStorageSync('token')
  Taro.removeStorageSync('userInfo')
}
