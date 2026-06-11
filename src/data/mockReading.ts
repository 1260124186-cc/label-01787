import type { ContinueReadingItem, ReadingTimelineDay } from '@/types/reading'

export const mockContinueReadingList: ContinueReadingItem[] = [
  {
    bookId: 1,
    bookTitle: '百年孤独',
    bookAuthor: '加西亚·马尔克斯',
    pageCount: 360,
    lastPage: 128,
    lastReadTime: '2026-06-10T08:30:00',
    totalDuration: 5400,
    readCount: 5
  },
  {
    bookId: 2,
    bookTitle: '人类简史',
    bookAuthor: '尤瓦尔·赫拉利',
    pageCount: 440,
    lastPage: 210,
    lastReadTime: '2026-06-09T21:15:00',
    totalDuration: 7200,
    readCount: 8
  },
  {
    bookId: 3,
    bookTitle: '三体',
    bookAuthor: '刘慈欣',
    pageCount: 520,
    lastPage: 356,
    lastReadTime: '2026-06-09T14:20:00',
    totalDuration: 10800,
    readCount: 12
  },
  {
    bookId: 4,
    bookTitle: '小王子',
    bookAuthor: '安托万·圣埃克苏佩里',
    pageCount: 96,
    lastPage: 72,
    lastReadTime: '2026-06-08T19:45:00',
    totalDuration: 3600,
    readCount: 3
  },
  {
    bookId: 5,
    bookTitle: '活着',
    bookAuthor: '余华',
    pageCount: 200,
    lastPage: 88,
    lastReadTime: '2026-06-07T10:00:00',
    totalDuration: 4200,
    readCount: 4
  }
]

export const mockReadingTimeline: ReadingTimelineDay[] = [
  {
    date: '2026-06-10',
    books: [
      { bookId: 1, bookTitle: '百年孤独', bookAuthor: '加西亚·马尔克斯', duration: 1800, lastPage: 128 },
      { bookId: 3, bookTitle: '三体', bookAuthor: '刘慈欣', duration: 900, lastPage: 356 }
    ]
  },
  {
    date: '2026-06-09',
    books: [
      { bookId: 2, bookTitle: '人类简史', bookAuthor: '尤瓦尔·赫拉利', duration: 2400, lastPage: 210 },
      { bookId: 3, bookTitle: '三体', bookAuthor: '刘慈欣', duration: 1200, lastPage: 350 },
      { bookId: 1, bookTitle: '百年孤独', bookAuthor: '加西亚·马尔克斯', duration: 600, lastPage: 120 }
    ]
  },
  {
    date: '2026-06-08',
    books: [
      { bookId: 4, bookTitle: '小王子', bookAuthor: '安托万·圣埃克苏佩里', duration: 1800, lastPage: 72 },
      { bookId: 2, bookTitle: '人类简史', bookAuthor: '尤瓦尔·赫拉利', duration: 1500, lastPage: 195 }
    ]
  },
  {
    date: '2026-06-07',
    books: [
      { bookId: 5, bookTitle: '活着', bookAuthor: '余华', duration: 2100, lastPage: 88 },
      { bookId: 3, bookTitle: '三体', bookAuthor: '刘慈欣', duration: 900, lastPage: 340 }
    ]
  },
  {
    date: '2026-06-06',
    books: [
      { bookId: 1, bookTitle: '百年孤独', bookAuthor: '加西亚·马尔克斯', duration: 1200, lastPage: 110 },
      { bookId: 5, bookTitle: '活着', bookAuthor: '余华', duration: 900, lastPage: 65 }
    ]
  },
  {
    date: '2026-06-05',
    books: [
      { bookId: 2, bookTitle: '人类简史', bookAuthor: '尤瓦尔·赫拉利', duration: 3000, lastPage: 180 }
    ]
  },
  {
    date: '2026-06-04',
    books: [
      { bookId: 3, bookTitle: '三体', bookAuthor: '刘慈欣', duration: 2400, lastPage: 320 },
      { bookId: 4, bookTitle: '小王子', bookAuthor: '安托万·圣埃克苏佩里', duration: 600, lastPage: 50 }
    ]
  }
]

export const mockReadingSummary = {
  totalDuration: 43200,
  bookCount: 5,
  dailyData: [
    { date: '2026-06-10', total: 2700 },
    { date: '2026-06-09', total: 4200 },
    { date: '2026-06-08', total: 3300 },
    { date: '2026-06-07', total: 3000 },
    { date: '2026-06-06', total: 2100 },
    { date: '2026-06-05', total: 3000 },
    { date: '2026-06-04', total: 3000 }
  ],
  period: 'week',
  periodStart: '2026-06-04',
  periodEnd: '2026-06-10',
  isVip: false,
  bookRank: [],
  categoryStats: [],
  avgDailyDuration: '61分钟',
  maxDayDuration: '70分钟',
  maxDayDate: '2026-06-09',
  readingDays: 7
}
