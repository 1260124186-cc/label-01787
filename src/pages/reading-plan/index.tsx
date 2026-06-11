import React, { useState, useEffect } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import { getReadingPlanList } from '@/services/readingPlan'
import type { ReadingPlan } from '@/types/readingPlan'
import { PLAN_STATUS } from '@/types/readingPlan'

const STATUS_TABS = [
  { key: PLAN_STATUS.ACTIVE, label: '进行中' },
  { key: PLAN_STATUS.COMPLETED, label: '已完成' },
  { key: PLAN_STATUS.ABANDONED, label: '已放弃' }
]

const STATUS_LABEL_MAP: Record<number, string> = {
  [PLAN_STATUS.ACTIVE]: '进行中',
  [PLAN_STATUS.COMPLETED]: '已完成',
  [PLAN_STATUS.ABANDONED]: '已放弃'
}

const ReadingPlanPage: React.FC = () => {
  const [activeStatus, setActiveStatus] = useState<number>(PLAN_STATUS.ACTIVE)
  const [plans, setPlans] = useState<ReadingPlan[]>([])
  const [loading, setLoading] = useState(false)

  const fetchPlans = async (status: number) => {
    setLoading(true)
    try {
      const data = await getReadingPlanList(status)
      setPlans(data)
    } catch (err) {
      console.error('[ReadingPlan] 获取计划列表失败', err)
      setPlans([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPlans(activeStatus)
  }, [activeStatus])

  const handleStatusChange = (status: number) => {
    setActiveStatus(status)
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

  const getProgressPercent = (plan: ReadingPlan) => {
    if (plan.totalPages <= 0) return 0
    return Math.min(Math.round((plan.readPages / plan.totalPages) * 100), 100)
  }

  const getRemainingDays = (plan: ReadingPlan): number | null => {
    if (!plan.estimatedEndDate) return null
    const now = new Date()
    const end = new Date(plan.estimatedEndDate)
    const diff = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    return diff > 0 ? diff : 0
  }

  const formatDate = (dateStr: string | null): string => {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    return `${d.getMonth() + 1}月${d.getDate()}日`
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.pageHeader}>
        <Text className={styles.headerTitle}>阅读计划</Text>
      </View>

      <View className={styles.statusTabs}>
        {STATUS_TABS.map((tab) => (
          <View
            key={tab.key}
            className={classnames(
              styles.statusTab,
              activeStatus === tab.key && styles.statusTabActive
            )}
            onClick={() => handleStatusChange(tab.key)}
          >
            <Text>{tab.label}</Text>
          </View>
        ))}
      </View>

      {plans.length > 0 ? (
        <ScrollView scrollY className={styles.planList}>
          {plans.map((plan) => {
            const percent = getProgressPercent(plan)
            const remainingDays = getRemainingDays(plan)
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
                  <View
                    className={classnames(
                      styles.planStatusBadge,
                      plan.status === PLAN_STATUS.ACTIVE && styles.planStatusActive,
                      plan.status === PLAN_STATUS.COMPLETED && styles.planStatusCompleted,
                      plan.status === PLAN_STATUS.ABANDONED && styles.planStatusAbandoned
                    )}
                  >
                    <Text>{STATUS_LABEL_MAP[plan.status]}</Text>
                  </View>
                </View>

                <View className={styles.progressSection}>
                  <View className={styles.progressHeader}>
                    <Text className={styles.progressLabel}>
                      {plan.readPages}/{plan.totalPages}页
                    </Text>
                    <Text className={styles.progressValue}>{percent}%</Text>
                  </View>
                  <View className={styles.progressBar}>
                    <View
                      className={classnames(
                        styles.progressFill,
                        plan.status === PLAN_STATUS.COMPLETED && styles.progressFillCompleted,
                        plan.status === PLAN_STATUS.ABANDONED && styles.progressFillAbandoned
                      )}
                      style={{ width: `${percent}%` }}
                    />
                  </View>
                </View>

                <View className={styles.planStats}>
                  <View className={styles.planStatItem}>
                    <Text className={styles.planStatLabel}>连续打卡</Text>
                    <Text className={classnames(styles.planStatValue, styles.planStatValueHighlight)}>
                      {plan.streakDays}天
                    </Text>
                  </View>
                  {plan.status === PLAN_STATUS.ACTIVE && (
                    <>
                      <View className={styles.planStatItem}>
                        <Text className={styles.planStatLabel}>剩余天数</Text>
                        <Text className={styles.planStatValue}>
                          {remainingDays !== null ? `${remainingDays}天` : '-'}
                        </Text>
                      </View>
                      <View className={styles.planStatItem}>
                        <Text className={styles.planStatLabel}>预计完成</Text>
                        <Text className={styles.planStatValue}>
                          {formatDate(plan.estimatedEndDate)}
                        </Text>
                      </View>
                    </>
                  )}
                  {plan.status === PLAN_STATUS.COMPLETED && (
                    <View className={styles.planStatItem}>
                      <Text className={styles.planStatLabel}>完成日期</Text>
                      <Text className={styles.planStatValue}>
                        {formatDate(plan.endDate)}
                      </Text>
                    </View>
                  )}
                </View>
              </View>
            )
          })}
        </ScrollView>
      ) : (
        <View className={styles.emptyState}>
          <Text className={styles.emptyIcon}>📋</Text>
          <Text className={styles.emptyText}>
            {activeStatus === PLAN_STATUS.ACTIVE && '暂无进行中的计划'}
            {activeStatus === PLAN_STATUS.COMPLETED && '暂无已完成的计划'}
            {activeStatus === PLAN_STATUS.ABANDONED && '暂无已放弃的计划'}
          </Text>
        </View>
      )}

      <View className={styles.fabButton} onClick={handleCreatePlan}>
        <Text className={styles.fabIcon}>+</Text>
      </View>
    </View>
  )
}

export default ReadingPlanPage
