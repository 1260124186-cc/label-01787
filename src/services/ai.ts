import Taro from '@tarojs/taro'
import { ensureLoggedIn } from './user'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Taro.request.Option = {}): Promise<T> => {
  try {
    await ensureLoggedIn()
  } catch (err) {
    console.error('[AI API] 登录失败', err)
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
      timeout: 30000,
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
    console.error('[AI API] 请求失败', url, err)
    throw err
  }
}

export interface AiChatDTO {
  bookId: number
  type: number
  sourceType: number
  sourceText?: string
  pageNum?: number
  userPrompt?: string
  targetLanguage?: string
}

export interface AiChatHistory {
  id: number
  userId: number
  bookId: number
  bookTitle: string
  sessionId: string
  type: number
  sourceType: number
  sourceText?: string
  pageNum?: number
  userPrompt?: string
  aiResponse: string
  extraData?: string
  status: number
  errorMsg?: string
  createdAt: string
  updatedAt: string
}

export interface ChatSession {
  bookId: number
  bookTitle: string
  sessionId: string
  chatCount: number
  lastActive: string
}

export const TYPE_MAP: Record<number, { name: string; loading: string }> = {
  1: { name: '摘要', loading: '正在生成摘要...' },
  2: { name: '解释', loading: '正在解释内容...' },
  3: { name: '翻译', loading: '正在翻译...' },
  4: { name: '出题自测', loading: '正在生成题目...' },
  5: { name: '章节大纲', loading: '正在生成大纲...' },
  6: { name: '知识卡片', loading: '正在生成知识卡片...' }
}

export const SOURCE_TYPE_MAP: Record<number, string> = {
  1: '选中段落',
  2: '当前页',
  3: '全书'
}

export async function chat(data: AiChatDTO): Promise<AiChatHistory> {
  return request<AiChatHistory>('/ai/chat', {
    method: 'POST',
    data
  })
}

export async function getChatSessions(): Promise<ChatSession[]> {
  return request<ChatSession[]>('/ai/chat/sessions')
}

export async function getChatHistoryByBook(
  bookId: number,
  page = 1,
  size = 20
): Promise<{ records: AiChatHistory[]; total: number; size: number; current: number }> {
  return request(`/ai/chat/book/${bookId}?page=${page}&size=${size}`)
}

export async function getChatDetail(id: number): Promise<AiChatHistory> {
  return request<AiChatHistory>(`/ai/chat/${id}`)
}

export async function deleteChat(id: number): Promise<void> {
  return request<void>(`/ai/chat/${id}`, {
    method: 'DELETE'
  })
}

export async function clearBookChats(bookId: number): Promise<void> {
  return request<void>(`/ai/chat/book/${bookId}`, {
    method: 'DELETE'
  })
}

export async function getDisclaimer(): Promise<{ disclaimer: string; copyrightReminder: string; version: string }> {
  return request('/ai/disclaimer')
}

export async function agreeCopyright(bookId: number): Promise<void> {
  return request<void>(`/ai/copyright/agree/${bookId}`, {
    method: 'POST'
  })
}

export async function getBookDetail(bookId: number): Promise<{
  id: number
  title: string
  author: string
  pageCount: number
  copyrightDeclared: number
  copyrightAgreedAt: string
}> {
  return request(`/books/${bookId}`)
}

export async function getPageText(bookId: number, pageNum: number): Promise<string> {
  return request(`/books/${bookId}/text/${pageNum}`)
}

export function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const day = 24 * 60 * 60 * 1000

  if (diff < day) {
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${hours}:${minutes}`
  } else if (diff < 7 * day) {
    const days = Math.floor(diff / day)
    return `${days}天前`
  } else {
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const dayNum = date.getDate().toString().padStart(2, '0')
    return `${month}-${dayNum}`
  }
}
