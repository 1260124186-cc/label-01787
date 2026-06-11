export interface ContinueReadingItem {
  bookId: number
  bookTitle: string
  bookAuthor: string
  pageCount: number
  lastPage: number
  lastReadTime: string
  totalDuration: number
  readCount: number
}

export interface ReadingTimelineDay {
  date: string
  books: ReadingTimelineBook[]
}

export interface ReadingTimelineBook {
  bookId: number
  bookTitle: string
  bookAuthor: string
  duration: number
  lastPage: number
}

export interface ReadingSummary {
  totalDuration: number
  bookCount: number
  dailyData: DailyDataItem[]
  period: string
  periodStart: string
  periodEnd: string
  isVip: boolean
  bookRank: BookRankItem[]
  categoryStats: CategoryStatItem[]
  avgDailyDuration: string
  maxDayDuration: string
  maxDayDate: string
  readingDays: number
}

export interface DailyDataItem {
  date: string
  total: number
}

export interface BookRankItem {
  bookId: number
  bookTitle: string
  totalDuration: number
  readCount: number
}

export interface CategoryStatItem {
  categoryId: number
  categoryName: string
  totalDuration: number
  bookCount: number
}

export interface AdminReadingStats {
  activeUsers7d: number
  avgDurationPerUser: number
  dailyStats: AdminDailyStat[]
}

export interface AdminDailyStat {
  date: string
  userCount: number
  totalDuration: number
  bookCount: number
}
