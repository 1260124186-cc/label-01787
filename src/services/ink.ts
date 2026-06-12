import Taro from '@tarojs/taro'
import type {
  InkStroke,
  InkStrokeDTO,
  InkBatchSyncRequest,
  InkBatchSyncResult
} from '@/types/ink'
import { strokeToDTO, dtoToStroke } from '@/types/ink'

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
      timeout: 15000,
      ...options
    })

    const data = res.data as any
    if (data.code === 200) {
      return data.data
    } else if (data.code === 401) {
      Taro.removeStorageSync('token')
      throw new Error('登录已过期')
    } else {
      throw new Error(data.message || '请求失败')
    }
  } catch (err) {
    console.error('[Ink API] 请求失败', url, err)
    throw err
  }
}

export const saveInkStroke = async (dto: InkStrokeDTO): Promise<InkStroke> => {
  try {
    const result = await request<any>('/ink/stroke', {
      method: 'POST',
      data: dto
    })
    return dtoToStroke(result)
  } catch (err) {
    console.error('[InkService] saveInkStroke failed', err)
    throw err
  }
}

export const batchSyncInk = async (
  bookId: number,
  pageNum: number,
  strokes: InkStroke[],
  deletedStrokeIds: string[] = []
): Promise<InkBatchSyncResult> => {
  try {
    const payload: InkBatchSyncRequest = {
      bookId,
      pageNum,
      strokes: strokes.map(s => strokeToDTO(s, bookId, pageNum)),
      deletedStrokeIds
    }
    const result = await request<any>('/ink/batch', {
      method: 'POST',
      data: payload
    })
    return {
      saved: result.saved,
      deleted: result.deleted,
      strokes: Array.isArray(result.strokes) ? result.strokes.map(dtoToStroke) : []
    }
  } catch (err) {
    console.error('[InkService] batchSyncInk failed', err)
    throw err
  }
}

export const getInkByPage = async (bookId: number, pageNum: number): Promise<InkStroke[]> => {
  try {
    const result = await request<any[]>(`/ink/page/${bookId}/${pageNum}`)
    return Array.isArray(result) ? result.map(dtoToStroke) : []
  } catch (err) {
    console.error('[InkService] getInkByPage failed', err)
    return []
  }
}

export const getInkByBook = async (bookId: number): Promise<InkStroke[]> => {
  try {
    const result = await request<any[]>(`/ink/book/${bookId}`)
    return Array.isArray(result) ? result.map(dtoToStroke) : []
  } catch (err) {
    console.error('[InkService] getInkByBook failed', err)
    return []
  }
}

export const getInkByBookAndPages = async (
  bookId: number,
  pageNums: number[]
): Promise<Record<number, InkStroke[]>> => {
  try {
    const result = await request<Record<string, any[]>>('/ink/book/pages', {
      method: 'POST',
      data: { bookId, pageNums }
    })
    const grouped: Record<number, InkStroke[]> = {}
    if (result) {
      for (const key of Object.keys(result)) {
        grouped[Number(key)] = (result[key] || []).map(dtoToStroke)
      }
    }
    return grouped
  } catch (err) {
    console.error('[InkService] getInkByBookAndPages failed', err)
    return {}
  }
}

export const getInkPageStats = async (bookId: number): Promise<Array<{ pageNum: number; strokeCount: number }>> => {
  try {
    const result = await request<any[]>(`/ink/stats/${bookId}`)
    return Array.isArray(result) ? result.map(item => ({
      pageNum: item.page_num || item.pageNum,
      strokeCount: item.stroke_count || item.strokeCount || 0
    })) : []
  } catch (err) {
    console.error('[InkService] getInkPageStats failed', err)
    return []
  }
}

export const deleteInkStroke = async (id: number): Promise<void> => {
  try {
    await request<void>(`/ink/stroke/${id}`, {
      method: 'DELETE'
    })
  } catch (err) {
    console.error('[InkService] deleteInkStroke failed', err)
    throw err
  }
}

export const deleteInkByStrokeId = async (bookId: number, strokeId: string): Promise<void> => {
  try {
    await request<void>(`/ink/stroke?bookId=${bookId}&strokeId=${encodeURIComponent(strokeId)}`, {
      method: 'DELETE'
    })
  } catch (err) {
    console.error('[InkService] deleteInkByStrokeId failed', err)
    throw err
  }
}

export const clearInkPage = async (bookId: number, pageNum: number): Promise<void> => {
  try {
    await request<void>(`/ink/page/${bookId}/${pageNum}`, {
      method: 'DELETE'
    })
  } catch (err) {
    console.error('[InkService] clearInkPage failed', err)
    throw err
  }
}

export const clearInkBook = async (bookId: number): Promise<void> => {
  try {
    await request<void>(`/ink/book/${bookId}`, {
      method: 'DELETE'
    })
  } catch (err) {
    console.error('[InkService] clearInkBook failed', err)
    throw err
  }
}

const LOCAL_INK_KEY_PREFIX = 'ink_local_'
const buildLocalKey = (bookId: number, pageNum: number) =>
  `${LOCAL_INK_KEY_PREFIX}${bookId}_${pageNum}`

export const saveLocalInk = (bookId: number, pageNum: number, strokes: InkStroke[]): void => {
  try {
    const key = buildLocalKey(bookId, pageNum)
    Taro.setStorageSync(key, JSON.stringify(strokes))
  } catch (err) {
    console.error('[InkService] saveLocalInk failed', err)
  }
}

export const getLocalInk = (bookId: number, pageNum: number): InkStroke[] => {
  try {
    const key = buildLocalKey(bookId, pageNum)
    const str = Taro.getStorageSync(key)
    if (str) {
      return JSON.parse(str)
    }
  } catch (err) {
    console.error('[InkService] getLocalInk failed', err)
  }
  return []
}

export const removeLocalInk = (bookId: number, pageNum: number): void => {
  try {
    const key = buildLocalKey(bookId, pageNum)
    Taro.removeStorageSync(key)
  } catch (err) {
    console.error('[InkService] removeLocalInk failed', err)
  }
}

export const exportInkPage = async (
  bookId: number,
  pageNum: number,
  format: 'image' | 'pdf' = 'image'
): Promise<string> => {
  try {
    const token = Taro.getStorageSync('token')
    const url = `${BASE_URL}/ink/export/page/${bookId}/${pageNum}?format=${format}`

    if (process.env.TARO_ENV === 'h5') {
      const response = await fetch(url, {
        headers: {
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      const blob = await response.blob()
      return URL.createObjectURL(blob)
    } else {
      return new Promise((resolve, reject) => {
        Taro.downloadFile({
          url,
          header: {
            'Authorization': token ? `Bearer ${token}` : ''
          },
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        })
      })
    }
  } catch (err) {
    console.error('[InkService] exportInkPage failed', err)
    throw err
  }
}

export const exportInkPages = async (
  bookId: number,
  pageNums: number[],
  format: 'image' | 'pdf' = 'pdf',
  overlay: boolean = true
): Promise<string> => {
  try {
    const token = Taro.getStorageSync('token')
    const response = await fetch(`${BASE_URL}/ink/export/pages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({ bookId, pageNums, format, overlay })
    })
    const blob = await response.blob()
    return URL.createObjectURL(blob)
  } catch (err) {
    console.error('[InkService] exportInkPages failed', err)
    throw err
  }
}

export const exportInkBook = async (
  bookId: number,
  overlay: boolean = true
): Promise<string> => {
  try {
    const token = Taro.getStorageSync('token')
    const url = `${BASE_URL}/ink/export/book/${bookId}?overlay=${overlay}`

    if (process.env.TARO_ENV === 'h5') {
      const response = await fetch(url, {
        headers: {
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      const blob = await response.blob()
      return URL.createObjectURL(blob)
    } else {
      return new Promise((resolve, reject) => {
        Taro.downloadFile({
          url,
          header: {
            'Authorization': token ? `Bearer ${token}` : ''
          },
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        })
      })
    }
  } catch (err) {
    console.error('[InkService] exportInkBook failed', err)
    throw err
  }
}

export const exportInkOnly = async (
  bookId: number,
  pageNum: number,
  width: number = 1000,
  height: number = 1400
): Promise<string> => {
  try {
    const token = Taro.getStorageSync('token')
    const url = `${BASE_URL}/ink/export/ink-only/${bookId}/${pageNum}?width=${width}&height=${height}`

    if (process.env.TARO_ENV === 'h5') {
      const response = await fetch(url, {
        headers: {
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      const blob = await response.blob()
      return URL.createObjectURL(blob)
    } else {
      return new Promise((resolve, reject) => {
        Taro.downloadFile({
          url,
          header: {
            'Authorization': token ? `Bearer ${token}` : ''
          },
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        })
      })
    }
  } catch (err) {
    console.error('[InkService] exportInkOnly failed', err)
    throw err
  }
}

export const downloadFile = (url: string, fileName: string): void => {
  if (process.env.TARO_ENV === 'h5') {
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } else {
    Taro.saveImageToPhotosAlbum({
      filePath: url,
      success: () => {
        Taro.showToast({ title: '已保存', icon: 'success' })
      },
      fail: () => {
        Taro.showToast({ title: '保存失败', icon: 'error' })
      }
    })
  }
}
