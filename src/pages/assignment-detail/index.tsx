import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Input, Textarea, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import {
  getAssignmentDetail,
  getMySubmission,
  getSubmissions,
  submitAssignment,
  gradeSubmission,
  sendReminder,
  batchRemind
} from '@/services/classroom'
import type { Assignment, Submission, GradeDTO } from '@/types/classroom'
import { SUBMISSION_STATUS, ASSIGNMENT_STATUS, CLASSROOM_ROLE } from '@/types/classroom'
import { getUserInfo } from '@/services/user'

const formatDuration = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}

const ASSIGNMENT_STATUS_MAP: Record<number, { label: string; style: string }> = {
  [ASSIGNMENT_STATUS.DRAFT]: { label: '草稿', style: styles.statusDraft },
  [ASSIGNMENT_STATUS.ACTIVE]: { label: '进行中', style: styles.statusActive },
  [ASSIGNMENT_STATUS.CLOSED]: { label: '已结束', style: styles.statusClosed }
}

const SUBMISSION_STATUS_MAP: Record<number, { label: string; style: string }> = {
  [SUBMISSION_STATUS.NOT_SUBMITTED]: { label: '未提交', style: styles.submissionNotSubmitted },
  [SUBMISSION_STATUS.SUBMITTED]: { label: '已提交', style: styles.submissionSubmitted },
  [SUBMISSION_STATUS.GRADED]: { label: '已批改', style: styles.submissionGraded }
}

const AssignmentDetailPage: React.FC = () => {
  const [assignment, setAssignment] = useState<Assignment | null>(null)
  const [mySubmission, setMySubmission] = useState<Submission | null>(null)
  const [submissions, setSubmissions] = useState<Submission[]>([])
  const [isTeacher, setIsTeacher] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [readingDuration, setReadingDuration] = useState('')
  const [annotationSummary, setAnnotationSummary] = useState('')
  const [pageProgress, setPageProgress] = useState('')
  const [proofImages, setProofImages] = useState('')

  const assignmentId = Number(Taro.getCurrentInstance().router?.params?.assignmentId) || 0

  const fetchData = useCallback(async () => {
    if (!assignmentId) return
    const [assignmentData, mySubmissionData] = await Promise.all([
      getAssignmentDetail(assignmentId),
      getMySubmission(assignmentId)
    ])
    if (assignmentData) {
      setAssignment(assignmentData)
      const userInfo = getUserInfo()
      if (userInfo && (userInfo as any).userId === assignmentData.teacherId) {
        setIsTeacher(true)
        const submissionsData = await getSubmissions(assignmentId)
        if (submissionsData && Array.isArray(submissionsData.records)) {
          setSubmissions(submissionsData.records)
        } else if (Array.isArray(submissionsData)) {
          setSubmissions(submissionsData)
        }
      }
    }
    if (mySubmissionData) {
      setMySubmission(mySubmissionData)
    }
  }, [assignmentId])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleSubmit = async () => {
    if (submitting) return
    const duration = Number(readingDuration)
    if (!duration || duration <= 0) {
      Taro.showToast({ title: '请输入阅读时长', icon: 'none' })
      return
    }
    setSubmitting(true)
    const result = await submitAssignment(assignmentId, {
      readingDuration: duration,
      annotationSummary: annotationSummary || undefined,
      pageProgress: pageProgress ? Number(pageProgress) : undefined,
      proofImages: proofImages || undefined
    })
    setSubmitting(false)
    if (result) {
      Taro.showToast({ title: '提交成功', icon: 'success' })
      fetchData()
    }
  }

  const handleGrade = (submission: Submission) => {
    Taro.showModal({
      title: `批改 - ${submission.studentNickname}`,
      editable: true,
      placeholderText: '请输入评分',
      success: async (res) => {
        if (res.confirm && res.content) {
          const score = Number(res.content)
          if (isNaN(score) || score < 0) {
            Taro.showToast({ title: '请输入有效分数', icon: 'none' })
            return
          }
          const dto: GradeDTO = { score }
          const result = await gradeSubmission(submission.id, dto)
          if (result) {
            Taro.showToast({ title: '批改成功', icon: 'success' })
            fetchData()
          }
        }
      }
    })
  }

  const handleRemind = (studentId: number) => {
    Taro.showModal({
      title: '催交提醒',
      content: '确定要向该学生发送催交提醒吗？',
      success: async (res) => {
        if (res.confirm) {
          const success = await sendReminder(assignmentId, { studentId })
          if (success) {
            Taro.showToast({ title: '提醒已发送', icon: 'success' })
          }
        }
      }
    })
  }

  const handleBatchRemind = () => {
    Taro.showModal({
      title: '批量催交',
      content: '确定要向所有未提交的学生发送催交提醒吗？',
      success: async (res) => {
        if (res.confirm) {
          const success = await batchRemind(assignmentId)
          if (success) {
            Taro.showToast({ title: '批量提醒已发送', icon: 'success' })
          }
        }
      }
    })
  }

  const statusInfo = assignment
    ? ASSIGNMENT_STATUS_MAP[assignment.status] || ASSIGNMENT_STATUS_MAP[ASSIGNMENT_STATUS.ACTIVE]
    : null

  return (
    <ScrollView className={styles.pageContainer} scrollY>
      <View className={styles.assignmentHeader}>
        <Text className={styles.bookTitle}>{assignment?.bookTitle || '加载中...'}</Text>
        {assignment && (
          <>
            <Text className={styles.author}>{assignment.bookAuthor}</Text>
            <Text className={styles.pageRange}>
              阅读范围：第{assignment.startPage}页 - 第{assignment.endPage}页
            </Text>
            <Text className={styles.deadline}>
              截止时间：{assignment.deadline}
            </Text>
            <View className={classnames(styles.statusBadge, statusInfo?.style)}>
              <Text>{statusInfo?.label}</Text>
            </View>
            {assignment.description && (
              <Text className={styles.description}>{assignment.description}</Text>
            )}
          </>
        )}
      </View>

      {isTeacher && (
        <>
          <View className={styles.submissionSection}>
            <Text className={styles.sectionTitle}>
              提交列表（{assignment?.submitCount || 0}/{assignment?.totalMembers || 0}）
            </Text>
            {submissions.length > 0 ? (
              submissions.map((item) => {
                const subStatus = SUBMISSION_STATUS_MAP[item.status] || SUBMISSION_STATUS_MAP[SUBMISSION_STATUS.NOT_SUBMITTED]
                return (
                  <View key={item.id} className={styles.submissionItem}>
                    <View className={styles.studentInfo}>
                      <Text className={styles.studentName}>{item.studentNickname}</Text>
                      <Text className={styles.studentDuration}>
                        阅读时长：{formatDuration(item.readingDuration)}
                      </Text>
                    </View>
                    <View
                      className={classnames(styles.submissionStatusBadge, subStatus.style)}
                    >
                      <Text>{subStatus.label}</Text>
                    </View>
                    {item.score !== null && item.score !== undefined && (
                      <Text className={styles.submissionScore}>{item.score}分</Text>
                    )}
                    {item.status === SUBMISSION_STATUS.SUBMITTED && (
                      <View
                        className={styles.gradeButton}
                        onClick={() => handleGrade(item)}
                      >
                        <Text>批改</Text>
                      </View>
                    )}
                    {item.status === SUBMISSION_STATUS.NOT_SUBMITTED && (
                      <View
                        className={styles.remindButton}
                        onClick={() => handleRemind(item.studentId)}
                      >
                        <Text>催交</Text>
                      </View>
                    )}
                  </View>
                )
              })
            ) : (
              <Text className={styles.emptyText}>暂无提交记录</Text>
            )}
          </View>
          <View className={styles.batchRemindButton} onClick={handleBatchRemind}>
            <Text>批量催交</Text>
          </View>
        </>
      )}

      {!isTeacher && (
        <>
          {mySubmission && (
            <View className={styles.mySubmissionCard}>
              <Text className={styles.sectionTitle}>我的提交</Text>
              <Text className={styles.mySubmissionLabel}>阅读时长</Text>
              <Text className={styles.mySubmissionValue}>
                {formatDuration(mySubmission.readingDuration)}
              </Text>
              {mySubmission.annotationSummary && (
                <>
                  <Text className={styles.mySubmissionLabel}>阅读批注</Text>
                  <Text className={styles.mySubmissionValue}>
                    {mySubmission.annotationSummary}
                  </Text>
                </>
              )}
              {mySubmission.pageProgress > 0 && (
                <>
                  <Text className={styles.mySubmissionLabel}>阅读页数</Text>
                  <Text className={styles.mySubmissionValue}>
                    {mySubmission.pageProgress}页
                  </Text>
                </>
              )}
              <Text className={styles.mySubmissionLabel}>提交状态</Text>
              <View
                className={classnames(
                  styles.submissionStatusBadge,
                  SUBMISSION_STATUS_MAP[mySubmission.status]?.style
                )}
              >
                <Text>{SUBMISSION_STATUS_MAP[mySubmission.status]?.label}</Text>
              </View>
              {mySubmission.score !== null && mySubmission.score !== undefined && (
                <>
                  <Text className={styles.mySubmissionLabel}>得分</Text>
                  <Text className={styles.mySubmissionValue}>
                    {mySubmission.score}分
                  </Text>
                </>
              )}
              {mySubmission.teacherComment && (
                <>
                  <Text className={styles.mySubmissionLabel}>教师评语</Text>
                  <Text className={styles.mySubmissionValue}>
                    {mySubmission.teacherComment}
                  </Text>
                </>
              )}
            </View>
          )}

          {!mySubmission && (
            <View className={styles.submitSection}>
              <Text className={styles.sectionTitle}>提交作业</Text>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>阅读时长（秒）</Text>
                <Input
                  className={styles.formInput}
                  type='number'
                  placeholder='请输入阅读时长（秒）'
                  value={readingDuration}
                  onInput={(e) => setReadingDuration(e.detail.value)}
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>阅读批注</Text>
                <Textarea
                  className={styles.formTextarea}
                  placeholder='请输入阅读批注摘要'
                  value={annotationSummary}
                  onInput={(e) => setAnnotationSummary(e.detail.value)}
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>阅读页数</Text>
                <Input
                  className={styles.formInput}
                  type='number'
                  placeholder='请输入已阅读页数'
                  value={pageProgress}
                  onInput={(e) => setPageProgress(e.detail.value)}
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>阅读凭证图片</Text>
                <Input
                  className={styles.formInput}
                  placeholder='请输入图片链接'
                  value={proofImages}
                  onInput={(e) => setProofImages(e.detail.value)}
                />
              </View>
              <View
                className={classnames(
                  styles.submitButton,
                  submitting && styles.submitButtonDisabled
                )}
                onClick={handleSubmit}
              >
                <Text>{submitting ? '提交中...' : '提交作业'}</Text>
              </View>
            </View>
          )}
        </>
      )}
    </ScrollView>
  )
}

export default AssignmentDetailPage
