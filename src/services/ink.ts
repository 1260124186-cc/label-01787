import Taro from '@tarojs/taro'
import type {
  InkStroke,
  InkStrokeDTO,
  InkBatchSyncRequest,
  InkBatchSyncResult
} from '@/types/ink'
import { strokeToDTO, dtoToStroke } from '@/types/ink'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Partial<Taro.request.Option> = {}): Promise<T> => {
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
      timeout: 30000,
      ...options
    } as Taro.request.Option)

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

const getToken = (): string => {
  return Taro.getStorageSync('token') || ''
}

const isH5 = (): boolean => {
  return process.env.TARO_ENV === 'h5'
}

const fetchBlobH5 = async (url: string, options: RequestInit = {}): Promise<string> => {
  const token = getToken()
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> || {})
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(url, { ...options, headers })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

const downloadFileWeapp = async (
  url: string,
  method: 'GET' | 'POST' = 'GET',
  data?: any
): Promise<string> => {
  const token = getToken()
  const header: Record<string, string> = {}
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  let downloadUrl = url
  if (method === 'GET' && data) {
    const params = new URLSearchParams()
    for (const key of Object.keys(data)) {
      const val = data[key]
      if (val !== undefined && val !== null) {
        params.append(key, Array.isArray(val) ? val.join(',') : String(val))
      }
    }
    const qs = params.toString()
    if (qs) {
      downloadUrl += (url.includes('?') ? '&' : '?') + qs
    }
  }

  return new Promise((resolve, reject) => {
    Taro.downloadFile({
      url: downloadUrl,
      header,
      timeout: 60000,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error(`下载失败: HTTP ${res.statusCode}`))
        }
      },
      fail: reject
    })
  })
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
  const url = `${BASE_URL}/ink/export/page/${bookId}/${pageNum}?format=${format}`

  if (isH5()) {
    return fetchBlobH5(url)
  } else {
    return downloadFileWeapp(url, 'GET')
  }
}

export const exportInkPages = async (
  bookId: number,
  pageNums: number[],
  format: 'image' | 'pdf' = 'pdf',
  overlay: boolean = true
): Promise<string> => {
  const url = `${BASE_URL}/ink/export/pages`
  const data = { bookId, pageNums, format, overlay }

  if (isH5()) {
    return fetchBlobH5(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
  } else {
    return downloadFileWeapp(url, 'GET', data)
  }
}

export const exportInkBook = async (
  bookId: number,
  overlay: boolean = true
): Promise<string> => {
  const url = `${BASE_URL}/ink/export/book/${bookId}?overlay=${overlay}`

  if (isH5()) {
    return fetchBlobH5(url)
  } else {
    return downloadFileWeapp(url, 'GET')
  }
}

export const exportInkOnly = async (
  bookId: number,
  pageNum: number,
  width: number = 1000,
  height: number = 1400
): Promise<string> => {
  const url = `${BASE_URL}/ink/export/ink-only/${bookId}/${pageNum}?width=${width}&height=${height}`

  if (isH5()) {
    return fetchBlobH5(url)
  } else {
    return downloadFileWeapp(url, 'GET')
  }
}

const triggerDownloadH5 = (url: string, fileName: string): void => {
  const isBlobUrl = url.startsWith('blob:')
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  if (!isBlobUrl) {
    link.target = '_blank'
  }
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  if (isBlobUrl) {
    setTimeout(() => {
      try {
        URL.revokeObjectURL(url)
      } catch (e) {
        console.warn('revokeObjectURL failed', e)
      }
    }, 5000)
  }
}

const saveImageToAlbum = async (filePath: string): Promise<boolean> => {
  return new Promise((resolve) => {
    const doSave = () => {
      Taro.saveImageToPhotosAlbum({
        filePath,
        success: () => {
          Taro.showToast({ title: '已保存到相册', icon: 'success' })
          resolve(true)
        },
        fail: (err: any) => {
          console.error('保存图片失败', err)
          const errMsg = err?.errMsg || ''
          if (errMsg.includes('auth') || errMsg.includes('deny')) {
            Taro.showModal({
              title: '需要相册权限',
              content: '请在设置中开启「保存到相册」权限',
              confirmText: '去设置',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  Taro.openSetting()
                }
                resolve(false)
              }
            })
          } else {
            Taro.showToast({ title: '保存失败', icon: 'error' })
            resolve(false)
          }
        }
      })
    }

    Taro.getSetting({
      success: (res) => {
        const auth = res.authSetting?.['scope.writePhotosAlbum']
        if (auth === false) {
          Taro.showModal({
            title: '需要相册权限',
            content: '请在设置中开启「保存到相册」权限',
            confirmText: '去设置',
            success: (modalRes) => {
              if (modalRes.confirm) {
                Taro.openSetting()
              }
              resolve(false)
            }
          })
        } else if (auth === true) {
          doSave()
        } else {
          Taro.authorize({
            scope: 'scope.writePhotosAlbum',
            success: doSave,
            fail: () => {
              Taro.showModal({
                title: '需要相册权限',
                content: '请授权保存图片到相册',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    Taro.openSetting()
                  }
                  resolve(false)
                }
              })
            }
          })
        }
      },
      fail: () => doSave()
    })
  })
}

const openOrSavePdf = async (filePath: string, fileName: string): Promise<boolean> => {
  return new Promise((resolve) => {
    Taro.showActionSheet({
      itemList: ['打开预览', '保存到本地'],
      success: (res) => {
        if (res.tapIndex === 0) {
          Taro.openDocument({
            filePath,
            fileType: 'pdf',
            showMenu: true,
            success: () => resolve(true),
            fail: (err) => {
              console.error('打开PDF失败', err)
              Taro.showToast({ title: '打开失败', icon: 'error' })
              resolve(false)
            }
          })
        } else {
          Taro.saveFile({
            tempFilePath: filePath,
            success: (saveRes) => {
              Taro.showModal({
                title: '保存成功',
                content: `文件已保存。可在微信「我 - 收藏」或文件管理器中查看。\n保存路径：${saveRes.savedFilePath}`,
                showCancel: false,
                confirmText: '好的'
              })
              resolve(true)
            },
            fail: (err) => {
              console.error('保存PDF失败', err)
              Taro.openDocument({
                filePath,
                fileType: 'pdf',
                showMenu: true,
                success: () => resolve(true),
                fail: () => {
                  Taro.showToast({ title: '保存失败', icon: 'error' })
                  resolve(false)
                }
              })
            }
          })
        }
      },
      fail: () => {
        Taro.openDocument({
          filePath,
          fileType: 'pdf',
          showMenu: true,
          success: () => resolve(true),
          fail: () => resolve(false)
        })
      }
    })
  })
}

export const downloadFile = async (url: string, fileName: string): Promise<boolean> => {
  const lowerName = fileName.toLowerCase()
  const isPdf = lowerName.endsWith('.pdf')
  const isImage = lowerName.endsWith('.png') || lowerName.endsWith('.jpg') || lowerName.endsWith('.jpeg')

  try {
    if (isH5()) {
      triggerDownloadH5(url, fileName)
      Taro.showToast({ title: '下载已开始', icon: 'success' })
      return true
    }

    if (isImage) {
      if (url.startsWith('http') || url.startsWith('blob:')) {
        Taro.showLoading({ title: '下载中...' })
        try {
          const tempPath = await downloadFileWeapp(url, 'GET')
          Taro.hideLoading()
          return saveImageToAlbum(tempPath)
        } catch (err) {
          Taro.hideLoading()
          console.error('下载图片失败', err)
          Taro.showToast({ title: '下载失败', icon: 'error' })
          return false
        }
      } else {
        return saveImageToAlbum(url)
      }
    }

    if (isPdf) {
      if (url.startsWith('http') || url.startsWith('blob:')) {
        Taro.showLoading({ title: '下载中...' })
        try {
          const tempPath = await downloadFileWeapp(url, 'GET')
          Taro.hideLoading()
          return openOrSavePdf(tempPath, fileName)
        } catch (err) {
          Taro.hideLoading()
          console.error('下载PDF失败', err)
          Taro.showToast({ title: '下载失败', icon: 'error' })
          return false
        }
      } else {
        return openOrSavePdf(url, fileName)
      }
    }

    if (url.startsWith('http')) {
      Taro.showLoading({ title: '下载中...' })
      return new Promise((resolve) => {
        Taro.downloadFile({
          url,
          success: (res) => {
            Taro.hideLoading()
            Taro.saveFile({
              tempFilePath: res.tempFilePath,
              success: () => {
                Taro.showToast({ title: '下载成功', icon: 'success' })
                resolve(true)
              },
              fail: () => {
                Taro.showToast({ title: '下载失败', icon: 'error' })
                resolve(false)
              }
            })
          },
          fail: () => {
            Taro.hideLoading()
            Taro.showToast({ title: '下载失败', icon: 'error' })
            resolve(false)
          }
        })
      })
    }

    Taro.showToast({ title: '保存成功', icon: 'success' })
    return true
  } catch (err) {
    console.error('[InkService] downloadFile failed', err)
    Taro.showToast({ title: '操作失败', icon: 'error' })
    return false
  }
}

export const previewPdf = async (url: string, _fileName: string): Promise<boolean> => {
  try {
    if (isH5()) {
      if (url.startsWith('blob:')) {
        const w = window.open()
        if (w) {
          w.location.href = url
        } else {
          window.location.href = url
        }
      } else {
        window.open(url, '_blank')
      }
      return true
    }

    if (url.startsWith('http')) {
      Taro.showLoading({ title: '加载中...' })
      return new Promise((resolve) => {
        Taro.downloadFile({
          url,
          timeout: 60000,
          success: (res) => {
            Taro.hideLoading()
            Taro.openDocument({
              filePath: res.tempFilePath,
              fileType: 'pdf',
              showMenu: true,
              success: () => resolve(true),
              fail: () => {
                Taro.showToast({ title: '打开失败', icon: 'error' })
                resolve(false)
              }
            })
          },
          fail: () => {
            Taro.hideLoading()
            Taro.showToast({ title: '下载失败', icon: 'error' })
            resolve(false)
          }
        })
      })
    }

    return new Promise((resolve) => {
      Taro.openDocument({
        filePath: url,
        fileType: 'pdf',
        showMenu: true,
        success: () => resolve(true),
        fail: () => {
          Taro.showToast({ title: '打开失败', icon: 'error' })
          resolve(false)
        }
      })
    })
  } catch (err) {
    console.error('[InkService] previewPdf failed', err)
    return false
  }
}

export const previewImage = async (url: string): Promise<boolean> => {
  try {
    if (isH5()) {
      window.open(url, '_blank')
      return true
    }

    if (url.startsWith('http')) {
      Taro.showLoading({ title: '加载中...' })
      return new Promise((resolve) => {
        Taro.downloadFile({
          url,
          timeout: 30000,
          success: (res) => {
            Taro.hideLoading()
            Taro.previewImage({
              urls: [res.tempFilePath],
              current: 0,
              success: () => resolve(true),
              fail: () => {
                Taro.showToast({ title: '预览失败', icon: 'error' })
                resolve(false)
              }
            })
          },
          fail: () => {
            Taro.hideLoading()
            Taro.showToast({ title: '下载失败', icon: 'error' })
            resolve(false)
          }
        })
      })
    }

    Taro.previewImage({
      urls: [url],
      current: 0
    })
    return true
  } catch (err) {
    console.error('[InkService] previewImage failed', err)
    return false
  }
}
