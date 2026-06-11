import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import styles from './index.module.scss'
import type { ContinueReadingItem } from '@/types/reading'

interface ContinueReadingCardProps {
  item: ContinueReadingItem
  onClick: (item: ContinueReadingItem) => void
}

const ContinueReadingCard: React.FC<ContinueReadingCardProps> = ({ item, onClick }) => {
  const progress = item.pageCount > 0 ? Math.round((item.lastPage / item.pageCount) * 100) : 0
  const durationMin = Math.round(item.totalDuration / 60)
  const coverId = (item.bookId * 37 + 13) % 200 + 1

  return (
    <View className={styles.card} onClick={() => onClick(item)}>
      <View className={styles.coverWrap}>
        <Image
          className={styles.cover}
          src={`https://picsum.photos/id/${coverId}/200/260`}
          mode="aspectFill"
        />
        <View className={styles.progressBadge}>
          <Text className={styles.progressText}>{progress}%</Text>
        </View>
      </View>
      <View className={styles.info}>
        <Text className={styles.title}>{item.bookTitle}</Text>
        <Text className={styles.author}>{item.bookAuthor}</Text>
        <View className={styles.progressBar}>
          <View className={styles.progressFill} style={{ width: `${progress}%` }} />
        </View>
        <View className={styles.meta}>
          <Text className={styles.metaText}>
            第{item.lastPage}/{item.pageCount}页
          </Text>
          <Text className={styles.metaText}>
            已读{durationMin}分钟
          </Text>
        </View>
      </View>
    </View>
  )
}

export default ContinueReadingCard
