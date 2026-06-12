import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import { getClassroomDetail, getAssignments } from '@/services/classroom'
import type { Classroom, Assignment, ClassroomMemberVO } from '@/types/classroom'
import { CLASSROOM_ROLE, ASSIGNMENT_STATUS } from '@/types/classroom'

const ROLE_LABELS: Record<number, string> = {
  [CLASSROOM_ROLE.TEACHER]: '教师',
  [CLASSROOM_ROLE.STUDENT]: '学生'
}

const STATUS_LABELS: Record<number, string> = {
  [ASSIGNMENT_STATUS.DRAFT]: '草稿',
  [ASSIGNMENT_STATUS.ACTIVE]: '进行中',
  [ASSIGNMENT_STATUS.CLOSED]: '已结束'
}

const formatDeadline = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${m}-${day} ${h}:${min}`
}

const ClassroomDetailPage: React.FC = () => {
  const [classroom, setClassroom] = useState<Classroom | null>(null)
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [loading, setLoading] = useState(true)

  const classroomId = Number(Taro.getCurrentInstance().router?.params?.classroomId) || 0
  const isTeacher = classroom?.myRole === CLASSROOM_ROLE.TEACHER

  const fetchData = useCallback(async () => {
    if (!classroomId) return
    setLoading(true)
    const [classroomData, assignmentsData] = await Promise.all([
      getClassroomDetail(classroomId),
      getAssignments(classroomId)
    ])
    if (classroomData) setClassroom(classroomData)
    if (assignmentsData) setAssignments(assignmentsData)
    setLoading(false)
  }, [classroomId])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleAssignmentClick = (assignmentId: number) => {
    Taro.navigateTo({ url: `/pages/assignment-detail/index?assignmentId=${assignmentId}` })
  }

  const handleCreateAssignment = () => {
    Taro.navigateTo({ url: `/pages/assignment-create/index?classroomId=${classroomId}` })
  }

  const handleCopyInviteCode = () => {
    if (!classroom?.inviteCode) return
    Taro.setClipboardData({ data: classroom.inviteCode })
  }

  const handleViewStats = () => {
    Taro.navigateTo({ url: `/pages/classroom-stats/index?classroomId=${classroomId}` })
  }

  const getStatusClassName = (status: number) => {
    if (status === ASSIGNMENT_STATUS.ACTIVE) return styles.statusActive
    if (status === ASSIGNMENT_STATUS.DRAFT) return styles.statusDraft
    return styles.statusClosed
  }

  const renderMember = (member: ClassroomMemberVO) => {
    const isTeacherRole = member.role === CLASSROOM_ROLE.TEACHER
    return (
      <View className={styles.memberCard} key={member.userId}>
        <Image
          className={styles.memberAvatar}
          src={member.avatar || ''}
          mode='aspectFill'
        />
        <View className={styles.memberInfo}>
          <Text className={styles.memberNickname}>{member.nickname}</Text>
          <View className={styles.memberDetail}>
            {member.studentNo && (
              <Text className={styles.memberStudentNo}>{member.studentNo}</Text>
            )}
            {member.realName && (
              <Text className={styles.memberRealName}>{member.realName}</Text>
            )}
          </View>
        </View>
        <View
          className={classnames(
            styles.roleBadge,
            isTeacherRole ? styles.roleBadgeTeacher : styles.roleBadgeStudent
          )}
        >
          <Text>{ROLE_LABELS[member.role] || '未知'}</Text>
        </View>
      </View>
    )
  }

  const renderAssignment = (assignment: Assignment) => {
    const progressPercent = assignment.totalMembers > 0
      ? Math.round((assignment.submitCount / assignment.totalMembers) * 100)
      : 0
    return (
      <View
        className={styles.assignmentCard}
        key={assignment.id}
        onClick={() => handleAssignmentClick(assignment.id)}
      >
        <View className={styles.assignmentCardHeader}>
          <Text className={styles.assignmentBookTitle}>{assignment.bookTitle}</Text>
          <View className={classnames(styles.assignmentStatusBadge, getStatusClassName(assignment.status))}>
            <Text>{STATUS_LABELS[assignment.status] || '未知'}</Text>
          </View>
        </View>
        <Text className={styles.assignmentPageRange}>
          {`第${assignment.startPage}-${assignment.endPage}页`}
        </Text>
        <Text className={styles.assignmentDeadline}>
          {`截止: ${formatDeadline(assignment.deadline)}`}
        </Text>
        <View className={styles.assignmentProgress}>
          <Text className={styles.assignmentProgressText}>
            {`${assignment.submitCount}/${assignment.totalMembers} 已提交`}
          </Text>
          <View className={styles.assignmentProgressBar}>
            <View
              className={styles.assignmentProgressFill}
              style={{ width: `${progressPercent}%` }}
            />
          </View>
        </View>
      </View>
    )
  }

  if (loading) {
    return (
      <View className={styles.pageContainer}>
        <View className={styles.emptyText}>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!classroom) {
    return (
      <View className={styles.pageContainer}>
        <View className={styles.emptyText}>
          <Text>班级不存在</Text>
        </View>
      </View>
    )
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.classroomHeader}>
        <Text className={styles.className}>{classroom.name}</Text>
        {classroom.description && (
          <Text className={styles.classDescription}>{classroom.description}</Text>
        )}
        <View className={styles.headerMeta}>
          {classroom.institution && (
            <View className={styles.metaTag}>
              <Text>{classroom.institution}</Text>
            </View>
          )}
          {classroom.gradeLevel && (
            <View className={styles.metaTag}>
              <Text>{classroom.gradeLevel}</Text>
            </View>
          )}
          <View className={styles.metaTag}>
            <Text>{`${classroom.memberCount}人`}</Text>
          </View>
        </View>
        {isTeacher && classroom.inviteCode && (
          <View className={styles.inviteCodeRow}>
            <Text className={styles.inviteCodeLabel}>邀请码</Text>
            <Text className={styles.inviteCodeValue}>{classroom.inviteCode}</Text>
            <View className={styles.inviteCodeCopy} onClick={handleCopyInviteCode}>
              <Text>复制</Text>
            </View>
          </View>
        )}
        {isTeacher && (
          <View className={styles.statsButton} onClick={handleViewStats}>
            <Text>班级统计</Text>
          </View>
        )}
      </View>

      <View className={styles.memberList}>
        <View className={styles.sectionHeader}>
          <Text className={styles.sectionTitle}>班级成员</Text>
          <Text className={styles.memberCount}>{`${classroom.members?.length || 0}人`}</Text>
        </View>
        {classroom.members && classroom.members.length > 0 ? (
          classroom.members.map(renderMember)
        ) : (
          <View className={styles.emptyText}>
            <Text>暂无成员</Text>
          </View>
        )}
      </View>

      <View className={styles.assignmentSection}>
        <View className={styles.sectionHeader}>
          <Text className={styles.sectionTitle}>阅读作业</Text>
        </View>
        {assignments.length > 0 ? (
          assignments.map(renderAssignment)
        ) : (
          <View className={styles.emptyText}>
            <Text>暂无作业</Text>
          </View>
        )}
      </View>

      {isTeacher && (
        <View className={styles.fabButton} onClick={handleCreateAssignment}>
          <Text className={styles.fabIcon}>+</Text>
          <Text className={styles.fabLabel}>布置</Text>
        </View>
      )}
    </View>
  )
}

export default ClassroomDetailPage
