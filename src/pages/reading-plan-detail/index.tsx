import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import {
  getReadingPlanProgress,
  getCheckinCalendar,
  checkinReadingPlan,
  getReadingPlanBadges,
  abandonReadingPlan
} from '@/services/readingPlan'
import type { ReadingPlanProgress, ReadingPlanBadge } from '@/types/readingPlan'
import { BADGE_INFO } from '@/types/readingPlan'

const WEEKDAY_LABELS = ['日', '一', '二', '三', '四', '五', '六']

const formatToday = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const getCalendarDays = (year: number, month: number) => {
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const days: (number | null)[] = []
  for (let i = 0; i < firstDay; i++) {
    days.push(null)
  }
  for (let d = 1; d <= daysInMonth; d++) {
    days.push(d)
  }
  return days
}

const ReadingPlanDetailPage: React.FC = () => {
  const [progress, setProgress] = useState<ReadingPlanProgress | null>(null)
  const [checkedDates, setCheckedDates] = useState<string[]>([])
  const [badges, setBadges] = useState<ReadingPlanBadge[]>([])
  const [checking, setChecking] = useState(false)

  const planId = Number(Taro.getCurrentInstance().router?.params?.planId) || 0

  const todayStr = formatToday()
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth()
  const calendarDays = getCalendarDays(currentYear, currentMonth)
  const todayDate = now.getDate()

  const checkedSet = new Set(checkedDates)

  const isDayInPlan = (day: number) => {
    if (!progress) return true
    const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const startDate = progress.checkinDates?.[0]
    if (!startDate) return true
    return dateStr >= startDate
  }

  const hasCheckedInToday = checkedSet.has(todayStr)

  const fetchData = useCallback(async () => {
    if (!planId) return
    const [progressData, calendarData, badgesData] = await Promise.all([
      getReadingPlanProgress(planId),
      getCheckinCalendar(planId),
      getReadingPlanBadges()
    ])
    if (progressData) setProgress(progressData)
    if (calendarData) setCheckedDates(calendarData)
    if (badgesData) setBadges(badgesData)
  }, [planId])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleCheckin = async () => {
    if (checking || hasCheckedInToday) return
    setChecking(true)
    const result = await checkinReadingPlan({ planId, duration: 0, pagesRead: 0 })
    setChecking(false)
    if (result) {
      Taro.showToast({ title: '打卡成功', icon: 'success' })
      setCheckedDates((prev) => [...prev, todayStr])
      fetchData()
    }
  }

  const handleAbandon = () => {
    Taro.showModal({
      title: '确认放弃',
      content: '放弃后将无法恢复，确定要放弃此阅读计划吗？',
      confirmColor: '#f53f3f',
      success: async (res) => {
        if (res.confirm) {
          const success = await abandonReadingPlan(planId)
          if (success) {
            Taro.showToast({ title: '已放弃计划', icon: 'success' })
            setTimeout(() => {
              Taro.navigateBack()
            }, 1500)
          }
        }
      }
    })
  }

  const earnedBadgeTypes = new Set(badges.map((b) => b.badgeType))

  const progressPercent = progress ? Math.min(Math.round(progress.progress * 100), 100) : 0

  return (
    <ScrollView className={styles.pageContainer} scrollY>
      <View className={styles.planInfoCard}>
        <Text className={styles.bookTitle}>{progress?.readPages !== undefined ? '' : '加载中...'}</Text>
        {progress && (
          <>
            <Text className={styles.bookTitle}>{`${progress.readPages} / ${progress.totalPages} 页`}</Text>
            <View className={styles.progressRow}>
              <View className={styles.progressBar}>
                <View className={styles.progressFill} style={{ width: `${progressPercent}%` }} />
              </View>
              <Text className={styles.progressPercent}>{progressPercent}%</Text>
            </View>
          </>
        )}
      </View>

      <View className={styles.sectionCard}>
        <Text className={styles.sectionTitle}>打卡日历</Text>
        <View className={styles.calendarHeader}>
          {WEEKDAY_LABELS.map((label) => (
            <View key={label} className={styles.calendarHeaderCell}>
              <Text>{label}</Text>
            </View>
          ))}
        </View>
        <View className={styles.calendarGrid}>
          {calendarDays.map((day, idx) => {
            if (day === null) {
              return <View key={`empty-${idx}`} className={styles.calendarDay} />
            }
            const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
            const isChecked = checkedSet.has(dateStr)
            const isToday = day === todayDate
            const isOutside = !isDayInPlan(day)
            return (
              <View
                key={day}
                className={classnames(
                  styles.calendarDay,
                  isChecked && styles.calendarDayChecked,
                  isToday && styles.calendarDayToday,
                  isOutside && styles.calendarDayOutside
                )}
              >
                <Text>{day}</Text>
              </View>
            )
          })}
        </View>
      </View>

      <View
        className={classnames(
          styles.checkinButton,
          hasCheckedInToday && styles.checkinButtonDisabled
        )}
        onClick={handleCheckin}
      >
        <Text>{hasCheckedInToday ? '已打卡' : '打卡'}</Text>
      </View>

      {progress && (
        <View className={styles.sectionCard}>
          <Text className={styles.sectionTitle}>阅读进度</Text>
          <View className={styles.statsGrid}>
            <View className={styles.statItem}>
              <Text className={styles.statValue}>{progress.readPages}</Text>
              <Text className={styles.statLabel}>已读页数</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={styles.statValue}>{progress.remainingPages}</Text>
              <Text className={styles.statLabel}>剩余页数</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={styles.statValue}>{progress.streakDays}</Text>
              <Text className={styles.statLabel}>连续打卡</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={styles.statValue}>{progress.maxStreakDays}</Text>
              <Text className={styles.statLabel}>最长连续</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={styles.statValue}>{progress.avgDailyPages}</Text>
              <Text className={styles.statLabel}>日均页数</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={styles.statValue}>
                {progress.estimatedEndDate || '-'}
              </Text>
              <Text className={styles.statLabel}>预计完成</Text>
            </View>
          </View>
        </View>
      )}

      <View className={styles.sectionCard}>
        <Text className={styles.sectionTitle}>成就徽章</Text>
        {Object.keys(BADGE_INFO).length > 0 ? (
          <View className={styles.badgesGrid}>
            {Object.entries(BADGE_INFO).map(([key, info]) => {
              const earned = earnedBadgeTypes.has(key)
              return (
                <View
                  key={key}
                  className={classnames(
                    styles.badgeItem,
                    !earned && styles.badgeItemLocked
                  )}
                >
                  <Text className={styles.badgeIcon}>{info.icon}</Text>
                  <Text className={styles.badgeName}>{info.name}</Text>
                  <Text className={styles.badgeDesc}>{info.desc}</Text>
                </View>
              )
            })}
          </View>
        ) : (
          <View className={styles.emptyBadges}>
            <Text className={styles.emptyBadgesText}>暂无徽章</Text>
          </View>
        )}
      </View>

      <View className={styles.abandonButton} onClick={handleAbandon}>
        <Text>放弃计划</Text>
      </View>
    </ScrollView>
  )
}

export default ReadingPlanDetailPage
