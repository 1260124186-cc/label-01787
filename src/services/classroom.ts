import Taro from '@tarojs/taro'
import { ensureLoggedIn } from './user'
import type {
  Classroom,
  ClassroomCreateDTO,
  ClassroomJoinDTO,
  Assignment,
  AssignmentCreateDTO,
  Submission,
  SubmissionDTO,
  GradeDTO,
  ClassroomStats,
  ReminderDTO
} from '@/types/classroom'

const BASE_URL = 'http://localhost:8080/api/mp'

const request = async <T>(url: string, options: Taro.request.Option = {}): Promise<T> => {
  try {
    await ensureLoggedIn()
  } catch (err) {
    console.error('[Classroom API] 登录失败', err)
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
    console.error('[Classroom API] 请求失败', url, err)
    throw err
  }
}

export const createClassroom = async (dto: ClassroomCreateDTO): Promise<Classroom | null> => {
  try {
    return await request<Classroom>('/classrooms', {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ClassroomService] createClassroom failed', err)
    return null
  }
}

export const joinClassroom = async (dto: ClassroomJoinDTO): Promise<any | null> => {
  try {
    return await request('/classrooms/join', {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ClassroomService] joinClassroom failed', err)
    return null
  }
}

export const leaveClassroom = async (classroomId: number): Promise<boolean> => {
  try {
    await request(`/classrooms/${classroomId}/leave`, { method: 'POST' })
    return true
  } catch (err) {
    console.error('[ClassroomService] leaveClassroom failed', err)
    return false
  }
}

export const getMyClassrooms = async (): Promise<Classroom[]> => {
  try {
    const data = await request<Classroom[]>('/classrooms/my')
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ClassroomService] getMyClassrooms failed', err)
    return []
  }
}

export const getClassroomDetail = async (classroomId: number): Promise<Classroom | null> => {
  try {
    return await request<Classroom>(`/classrooms/${classroomId}`)
  } catch (err) {
    console.error('[ClassroomService] getClassroomDetail failed', err)
    return null
  }
}

export const createAssignment = async (classroomId: number, dto: AssignmentCreateDTO): Promise<Assignment | null> => {
  try {
    return await request<Assignment>(`/classrooms/${classroomId}/assignments`, {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ClassroomService] createAssignment failed', err)
    return null
  }
}

export const getAssignments = async (classroomId: number): Promise<Assignment[]> => {
  try {
    const data = await request<Assignment[]>(`/classrooms/${classroomId}/assignments`)
    return Array.isArray(data) ? data : []
  } catch (err) {
    console.error('[ClassroomService] getAssignments failed', err)
    return []
  }
}

export const getAssignmentDetail = async (assignmentId: number): Promise<Assignment | null> => {
  try {
    return await request<Assignment>(`/classrooms/assignments/${assignmentId}`)
  } catch (err) {
    console.error('[ClassroomService] getAssignmentDetail failed', err)
    return null
  }
}

export const submitAssignment = async (assignmentId: number, dto: SubmissionDTO): Promise<any | null> => {
  try {
    return await request(`/classrooms/assignments/${assignmentId}/submit`, {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ClassroomService] submitAssignment failed', err)
    return null
  }
}

export const getMySubmission = async (assignmentId: number): Promise<Submission | null> => {
  try {
    return await request<Submission>(`/classrooms/assignments/${assignmentId}/my-submission`)
  } catch (err) {
    console.error('[ClassroomService] getMySubmission failed', err)
    return null
  }
}

export const getSubmissions = async (assignmentId: number, page = 1, size = 20): Promise<any> => {
  try {
    return await request(`/classrooms/assignments/${assignmentId}/submissions?page=${page}&size=${size}`)
  } catch (err) {
    console.error('[ClassroomService] getSubmissions failed', err)
    return { records: [], total: 0 }
  }
}

export const gradeSubmission = async (submissionId: number, dto: GradeDTO): Promise<any | null> => {
  try {
    return await request(`/classrooms/submissions/${submissionId}/grade`, {
      method: 'POST',
      data: dto
    })
  } catch (err) {
    console.error('[ClassroomService] gradeSubmission failed', err)
    return null
  }
}

export const sendReminder = async (assignmentId: number, dto: ReminderDTO): Promise<boolean> => {
  try {
    await request(`/classrooms/assignments/${assignmentId}/remind`, {
      method: 'POST',
      data: dto
    })
    return true
  } catch (err) {
    console.error('[ClassroomService] sendReminder failed', err)
    return false
  }
}

export const batchRemind = async (assignmentId: number): Promise<boolean> => {
  try {
    await request(`/classrooms/assignments/${assignmentId}/batch-remind`, { method: 'POST' })
    return true
  } catch (err) {
    console.error('[ClassroomService] batchRemind failed', err)
    return false
  }
}

export const getClassroomStats = async (classroomId: number): Promise<ClassroomStats | null> => {
  try {
    return await request<ClassroomStats>(`/classrooms/${classroomId}/stats`)
  } catch (err) {
    console.error('[ClassroomService] getClassroomStats failed', err)
    return null
  }
}
