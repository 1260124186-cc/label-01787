import React from 'react'
import { View, Text } from '@tarojs/components'
import classnames from 'classnames'
import styles from './index.module.scss'
import type { ReadingTimelineBook } from '@/types/reading'

interface ReadingTimelineItemProps {
  date: string
  books: ReadingTimelineBook[]
  isLast: boolean
  onBookClick: (bookId: number) => void
}

const ReadingTimelineItem: React.FC<ReadingTimelineItemProps> = ({ date, books, isLast, onBookClick }) => {
  const dateObj = new Date(date)
  const today = new Date()
  const isToday = dateObj.toDateString() === today.toDateString()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday = dateObj.toDateString() === yesterday.toDateString()

  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dayOfWeek = weekDays[dateObj.getDay()]

  const dateLabel = isToday ? '今天' : isYesterday ? '昨天' : `${dateObj.getMonth() + 1}月${dateObj.getDate()}日`

  const totalDuration = books.reduce((sum, b) => sum + b.duration, 0)
  const totalMin = Math.round(totalDuration / 60)

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.round((seconds % 3600) / 60)
    if (hours > 0) return `${hours}小时${minutes}分钟`
    return `${minutes}分钟`
  }

  return (
    <View className={styles.timelineItem}>
      <View className={styles.dateCol}>
        <Text className={styles.dateLabel}>{dateLabel}</Text>
        <Text className={styles.dayOfWeek}>{dayOfWeek}</Text>
        <View className={classnames(styles.dot, !isLast && styles.dotActive)} />
        {!isLast && <View className={styles.line} />}
      </View>
      <View className={styles.contentCol}>
        <View className={styles.dayHeader}>
          <Text className={styles.dayTotal}>共读 {totalMin} 分钟</Text>
          <Text className={styles.bookCount}>{books.length} 本书</Text>
        </View>
        {books.map((book) => (
          <View
            key={book.bookId}
            className={styles.bookCard}
            onClick={() => onBookClick(book.bookId)}
          >
            <View className={styles.bookInfo}>
              <Text className={styles.bookTitle}>{book.bookTitle}</Text>
              <Text className={styles.bookAuthor}>{book.bookAuthor}</Text>
            </View>
            <View className={styles.bookMeta}>
              <Text className={styles.bookDuration}>{formatDuration(book.duration)}</Text>
              <Text className={styles.bookPage}>至第{book.lastPage}页</Text>
            </View>
          </View>
        ))}
      </View>
    </View>
  )
}

export default ReadingTimelineItem
