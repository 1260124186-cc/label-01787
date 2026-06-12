import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import styles from './index.module.scss'
import { getClassroomStats } from '@/services/classroom'
import type { ClassroomStats, AssignmentStatsItem } from '@/types/classroom'

const ClassroomStatsPage: React.FC = () => {
  const [stats, setStats] = useState<ClassroomStats | null>(null)

  useEffect(() => {
    const params = Taro.getCurrentInstance().router?.params
    const classroomId = params?.classroomId ? Number(params.classroomId) : 0

    if (!classroomId) return

    const fetchStats = async () => {
      const data = await getClassroomStats(classroomId)
      if (data) {
        setStats(data)
      }
    }

    fetchStats()
  }, [])

  const formatScore = (score: number) => score.toFixed(1)

  const formatRate = (rate: number) => `${Math.round(rate * 100)}%`

  if (!stats) {
    return (
      <View className={styles.pageContainer}>
        <View className={styles.emptyState}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.overviewCards}>
        <View className={styles.overviewCard}>
          <Text className={styles.overviewValue}>{stats.totalStudents}</Text>
          <Text className={styles.overviewLabel}>学生总数</Text>
        </View>
        <View className={styles.overviewCard}>
          <Text className={styles.overviewValue}>{stats.totalAssignments}</Text>
          <Text className={styles.overviewLabel}>作业总数</Text>
        </View>
        <View className={styles.overviewCard}>
          <Text className={styles.overviewValue}>{stats.activeAssignments}</Text>
          <Text className={styles.overviewLabel}>进行中作业</Text>
        </View>
        <View className={styles.overviewCard}>
          <Text className={styles.overviewValue}>{formatScore(stats.overallAvgScore)}</Text>
          <Text className={styles.overviewLabel}>平均分</Text>
        </View>
        <View className={styles.overviewCard}>
          <Text className={styles.overviewValue}>{formatRate(stats.overallSubmitRate)}</Text>
          <Text className={styles.overviewLabel}>提交率</Text>
        </View>
      </View>

      <Text className={styles.sectionTitle}>作业统计</Text>

      {stats.assignmentStats.length > 0 ? (
        <View className={styles.assignmentStatsList}>
          {stats.assignmentStats.map((item: AssignmentStatsItem) => (
            <View key={item.assignmentId} className={styles.assignmentItem}>
              <View className={styles.assignmentHeader}>
                <Text className={styles.bookTitle}>{item.bookTitle}</Text>
                <View className={styles.assignmentMeta}>
                  <Text className={styles.submitInfo}>
                    {item.submitCount}/{item.totalMembers} 已提交
                  </Text>
                  <Text className={styles.avgScore}>
                    {formatScore(item.avgScore)}分
                  </Text>
                </View>
              </View>
              <View className={styles.progressBar}>
                <View
                  className={styles.progressFill}
                  style={{ width: formatRate(item.submitRate) }}
                />
              </View>
              <View className={styles.assignmentFooter}>
                <Text className={styles.footerItem}>
                  提交率 {formatRate(item.submitRate)}
                </Text>
                <Text className={styles.footerItem}>
                  已批改 {item.gradedCount}
                </Text>
              </View>
            </View>
          ))}
        </View>
      ) : (
        <View className={styles.emptyState}>
          <Text className={styles.emptyIcon}>📊</Text>
          <Text className={styles.emptyText}>暂无作业统计</Text>
        </View>
      )}
    </View>
  )
}

export default ClassroomStatsPage
