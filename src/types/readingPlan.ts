export interface ReadingPlan {
  id: number
  bookId: number
  bookTitle: string
  bookAuthor: string
  bookFormat: string
  targetDays: number
  dailyMinDuration: number
  reminderTime: string
  readPages: number
  totalPages: number
  streakDays: number
  maxStreakDays: number
  status: number
  startDate: string
  endDate: string
  estimatedEndDate: string | null
  progress: number
  avgDailyPages: number
  badges: string[]
}

export interface ReadingPlanProgress {
  planId: number
  readPages: number
  totalPages: number
  progress: number
  streakDays: number
  maxStreakDays: number
  estimatedEndDate: string | null
  avgDailyPages: number
  remainingPages: number
  remainingDays: number | null
  checkinDates: string[]
}

export interface ReadingPlanBadge {
  id: number
  userId: number
  planId: number | null
  badgeType: string
  badgeName: string
  badgeIcon: string
  earnedAt: string
}

export interface ReadingPlanCreateDTO {
  bookId: number
  targetDays: number
  dailyMinDuration?: number
  reminderTime?: string
}

export interface ReadingPlanCheckinDTO {
  planId: number
  duration?: number
  pagesRead?: number
}

export interface ReadingPlanCheckin {
  id: number
  planId: number
  userId: number
  checkinDate: string
  duration: number
  pagesRead: number
}

export const PLAN_STATUS = {
  ABANDONED: 0,
  ACTIVE: 1,
  COMPLETED: 2
} as const

export const BADGE_INFO: Record<string, { name: string; icon: string; desc: string }> = {
  streak_3: { name: '三日笃学', icon: '🥉', desc: '连续打卡3天' },
  streak_7: { name: '七日坚持', icon: '🥈', desc: '连续打卡7天' },
  streak_14: { name: '两周不懈', icon: '🥇', desc: '连续打卡14天' },
  streak_30: { name: '月度书虫', icon: '💎', desc: '连续打卡30天' },
  streak_100: { name: '百日书圣', icon: '👑', desc: '连续打卡100天' },
  plan_complete: { name: '计划达成', icon: '🏆', desc: '完成一个阅读计划' }
}
