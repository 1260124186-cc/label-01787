import React, { useState, useEffect } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import ReadingTimelineItem from '@/components/ReadingTimelineItem'
import { mockReadingTimeline, mockReadingSummary } from '@/data/mockReading'
import { getReadingTimeline, getReadingSummary } from '@/services/reading'
import { getReadingPlanList } from '@/services/readingPlan'
import type { ReadingTimelineDay, ReadingSummary } from '@/types/reading'
import type { ReadingPlan } from '@/types/readingPlan'
import { PLAN_STATUS, BADGE_INFO } from '@/types/readingPlan'

const PERIOD_TABS = [
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'year', label: '今年' }
]

const MAIN_TABS = [
  { key: 'stats', label: '统计' },
  { key: 'plans', label: '计划' }
]

const STATUS_TABS = [
  { key: PLAN_STATUS.ACTIVE, label: '进行中' },
  { key: PLAN_STATUS.COMPLETED, label: '已完成' },
  { key: PLAN_STATUS.ABANDONED, label: '已放弃' }
]

const StatsPage: React.FC = () => {
  const [activeMainTab, setActiveMainTab] = useState('stats')
  const [activePeriod, setActivePeriod] = useState('week')
  const [activePlanStatus, setActivePlanStatus] = useState<number>(PLAN_STATUS.ACTIVE)
  const [timeline, setTimeline] = useState<ReadingTimelineDay[]>([])
  const [summary, setSummary] = useState<ReadingSummary>(mockReadingSummary)
  const [plans, setPlans] = useState<ReadingPlan[]>([])

  const fetchStatsData = async (period: string) => {
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

  const fetchPlansData = async (status: number) => {
    try {
      const data = await getReadingPlanList(status)
      setPlans(data)
    } catch (err) {
      console.error('[Stats] 获取计划列表失败', err)
      setPlans([])
    }
  }

  useEffect(() => {
    if (activeMainTab === 'stats') {
      fetchStatsData(activePeriod)
    } else {
      fetchPlansData(activePlanStatus)
    }
  }, [activeMainTab, activePeriod, activePlanStatus])

  const handleBookClick = (bookId: number) => {
    Taro.navigateTo({
      url: `/pages/reader/index?bookId=${bookId}`
    })
  }

  const handlePlanClick = (planId: number) => {
    Taro.navigateTo({
      url: `/pages/reading-plan-detail/index?planId=${planId}`
    })
  }

  const handleCreatePlan = () => {
    Taro.navigateTo({
      url: '/pages/reading-plan-create/index'
    })
  }

  const totalDurationMin = Math.round(summary.totalDuration / 60)

  const getProgressPercent = (plan: ReadingPlan) => {
    if (plan.totalPages <= 0) return 0
    return Math.min(Math.round((plan.readPages / plan.totalPages) * 100), 100)
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.pageHeader}>
        <Text className={styles.headerTitle}>阅读统计</Text>
      </View>

      <View className={styles.mainTabs}>
        {MAIN_TABS.map((tab) => (
          <View
            key={tab.key}
            className={classnames(
              styles.mainTab,
              activeMainTab === tab.key && styles.mainTabActive
            )}
            onClick={() => setActiveMainTab(tab.key)}
          >
            <Text>{tab.label}</Text>
          </View>
        ))}
      </View>

      {activeMainTab === 'stats' && (
        <>
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
                onClick={() => setActivePeriod(tab.key)}
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
        </>
      )}

      {activeMainTab === 'plans' && (
        <>
          <View className={styles.periodTabs}>
            {STATUS_TABS.map((tab) => (
              <View
                key={tab.key}
                className={classnames(
                  styles.periodTab,
                  activePlanStatus === tab.key && styles.periodTabActive
                )}
                onClick={() => setActivePlanStatus(tab.key)}
              >
                <Text>{tab.label}</Text>
              </View>
            ))}
          </View>

          {plans.length > 0 ? (
            <ScrollView scrollY className={styles.planList}>
              {plans.map((plan) => {
                const percent = getProgressPercent(plan)
                return (
                  <View
                    key={plan.id}
                    className={styles.planCard}
                    onClick={() => handlePlanClick(plan.id)}
                  >
                    <View className={styles.planCardHeader}>
                      <View className={styles.planBookInfo}>
                        <Text className={styles.planBookTitle}>{plan.bookTitle}</Text>
                        <Text className={styles.planBookAuthor}>{plan.bookAuthor}</Text>
                      </View>
                      <Text className={styles.planProgressText}>{percent}%</Text>
                    </View>
                    <View className={styles.planProgressBar}>
                      <View
                        className={styles.planProgressFill}
                        style={{ width: `${percent}%` }}
                      />
                    </View>
                    <View className={styles.planStats}>
                      <Text className={styles.planStatLabel}>
                        {plan.readPages}/{plan.totalPages}页
                      </Text>
                      <Text className={styles.planStatLabel}>
                        连续{plan.streakDays}天
                      </Text>
                      {plan.badges.length > 0 && (
                        <Text className={styles.planStatLabel}>
                          {plan.badges.map((b) => BADGE_INFO[b]?.icon || '').join(' ')}
                        </Text>
                      )}
                    </View>
                  </View>
                )
              })}
            </ScrollView>
          ) : (
            <View className={styles.emptyTimeline}>
              <Text className={styles.emptyIcon}>📋</Text>
              <Text className={styles.emptyText}>暂无阅读计划</Text>
            </View>
          )}

          <View className={styles.createPlanButton} onClick={handleCreatePlan}>
            <Text className={styles.createPlanButtonText}>+ 创建阅读计划</Text>
          </View>
        </>
      )}
    </View>
  )
}

export default StatsPage
