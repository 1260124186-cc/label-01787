export interface Classroom {
  id: number
  name: string
  description: string
  teacherId: number
  teacherNickname: string
  teacherAvatar: string
  inviteCode: string | null
  memberCount: number
  institution: string
  gradeLevel: string
  status: number
  myRole: number | null
  createdAt: string
  members?: ClassroomMemberVO[]
}

export interface ClassroomMemberVO {
  userId: number
  nickname: string
  avatar: string
  role: number
  studentNo: string
  realName: string
  joinedAt: string
}

export interface ClassroomCreateDTO {
  name: string
  description?: string
  institution?: string
  gradeLevel?: string
}

export interface ClassroomJoinDTO {
  inviteCode: string
  studentNo?: string
  realName?: string
}

export interface Assignment {
  id: number
  classroomId: number
  teacherId: number
  teacherNickname: string
  bookId: number | null
  bookTitle: string
  bookAuthor: string
  startPage: number
  endPage: number
  deadline: string
  description: string
  totalScore: number
  status: number
  submitCount: number
  gradedCount: number
  totalMembers: number
  avgScore: number
  createdAt: string
}

export interface AssignmentCreateDTO {
  bookTitle: string
  bookAuthor?: string
  bookId?: number
  startPage: number
  endPage: number
  deadline: string
  description?: string
  totalScore?: number
}

export interface Submission {
  id: number
  assignmentId: number
  studentId: number
  studentNickname: string
  studentAvatar: string
  readingDuration: number
  annotationSummary: string
  pageProgress: number
  proofImages: string
  submitAt: string | null
  status: number
  score: number | null
  teacherComment: string
  gradedAt: string | null
  gradedByName: string
}

export interface SubmissionDTO {
  readingDuration: number
  annotationSummary?: string
  pageProgress?: number
  proofImages?: string
}

export interface GradeDTO {
  score: number
  teacherComment?: string
}

export interface ReminderDTO {
  studentId: number
  message?: string
}

export interface ClassroomStats {
  classroomId: number
  classroomName: string
  totalStudents: number
  totalAssignments: number
  activeAssignments: number
  overallAvgScore: number
  overallSubmitRate: number
  assignmentStats: AssignmentStatsItem[]
}

export interface AssignmentStatsItem {
  assignmentId: number
  bookTitle: string
  submitCount: number
  totalMembers: number
  submitRate: number
  avgScore: number
  gradedCount: number
}

export const CLASSROOM_STATUS = {
  CLOSED: 0,
  ACTIVE: 1
} as const

export const ASSIGNMENT_STATUS = {
  DRAFT: 0,
  ACTIVE: 1,
  CLOSED: 2
} as const

export const SUBMISSION_STATUS = {
  NOT_SUBMITTED: 0,
  SUBMITTED: 1,
  GRADED: 2
} as const

export const CLASSROOM_ROLE = {
  TEACHER: 1,
  STUDENT: 2
} as const
