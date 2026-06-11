import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import ReadingTimelineItem from '@/components/ReadingTimelineItem'
import { mockReadingTimeline, mockReadingSummary } from '@/data/mockReading'
import { getReadingTimeline, getReadingSummary } from '@/services/reading'
import type { ReadingTimelineDay, ReadingSummary } from '@/types/reading'

const PERIOD_TABS = [
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'year', label: '今年' }
]

const StatsPage: React.FC = () => {
  const [activePeriod, setActivePeriod] = useState('week')
  const [timeline, setTimeline] = useState<ReadingTimelineDay[]>([])
  const [summary, setSummary] = useState<ReadingSummary>(mockReadingSummary)

  const fetchData = async (period: string) => {
    console.log('[Stats] 获取统计数据, period:', period)
    try {
      const [apiTimeline, apiSummary] = await Promise.all([
        getReadingTimeline(period),
        getReadingSummary(period)
      ])
      if (apiTimeline.length > 0) {
        setTimeline(apiTimeline)
      } else {
        setTimeline(mockReadingTimeline)
      }
      if (apiSummary) {
        setSummary(apiSummary)
      } else {
        setSummary(mockReadingSummary)
      }
    } catch (err) {
      console.error('[Stats] API获取统计数据失败，使用mock数据', err)
      setTimeline(mockReadingTimeline)
      setSummary(mockReadingSummary)
    }
  }

  useEffect(() => {
    fetchData(activePeriod)
  }, [activePeriod])

  const handlePeriodChange = (period: string) => {
    setActivePeriod(period)
  }

  const handleBookClick = (bookId: number) => {
    console.log('[Stats] 点击书籍:', bookId)
    Taro.navigateTo({
      url: `/pages/reader/index?bookId=${bookId}`
    })
  }

  const totalDurationMin = Math.round(summary.totalDuration / 60)

  return (
    <View className={styles.pageContainer}>
      <View className={styles.pageHeader}>
        <Text className={styles.headerTitle}>阅读统计</Text>
      </View>

      <View className={styles.summaryCards}>
        <View className={styles.summaryCard}>
          <Text className={styles.summaryValue}>{totalDurationMin}</Text>
          <Text className={styles.summaryLabel}>阅读分钟</Text>
        </View>
        <View className={styles.summaryCard}>
          <Text className={styles.summaryValue}>{summary.bookCount}</Text>
          <Text className={styles.summaryLabel}>在读书籍</Text>
        </View>
        <View className={styles.summaryCard}>
          <Text className={styles.summaryValue}>{summary.readingDays}</Text>
          <Text className={styles.summaryLabel}>阅读天数</Text>
        </View>
      </View>

      <View className={styles.periodTabs}>
        {PERIOD_TABS.map((tab) => (
          <View
            key={tab.key}
            className={classnames(
              styles.periodTab,
              activePeriod === tab.key && styles.periodTabActive
            )}
            onClick={() => handlePeriodChange(tab.key)}
          >
            <Text>{tab.label}</Text>
          </View>
        ))}
      </View>

      {timeline.length > 0 ? (
        <View className={styles.timelineSection}>
          <Text className={styles.timelineTitle}>阅读足迹</Text>
          {timeline.map((day, index) => (
            <ReadingTimelineItem
              key={day.date}
              date={day.date}
              books={day.books}
              isLast={index === timeline.length - 1}
              onBookClick={handleBookClick}
            />
          ))}
        </View>
      ) : (
        <View className={styles.emptyTimeline}>
          <Text className={styles.emptyIcon}>📚</Text>
          <Text className={styles.emptyText}>暂无阅读记录</Text>
        </View>
      )}

      {summary.avgDailyDuration && (
        <View className={styles.insightCard}>
          <Text className={styles.insightTitle}>阅读洞察</Text>
          <View className={styles.insightRow}>
            <Text className={styles.insightLabel}>日均阅读</Text>
            <Text className={styles.insightValue}>{summary.avgDailyDuration}</Text>
          </View>
          <View className={styles.insightRow}>
            <Text className={styles.insightLabel}>最长阅读日</Text>
            <Text className={styles.insightValue}>{summary.maxDayDuration}</Text>
          </View>
          <View className={styles.insightRow}>
            <Text className={styles.insightLabel}>最长阅读日日期</Text>
            <Text className={styles.insightValue}>{summary.maxDayDate || '-'}</Text>
          </View>
        </View>
      )}
    </View>
  )
}

export default StatsPage
