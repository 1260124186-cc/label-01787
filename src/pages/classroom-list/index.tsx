import React, { useState, useEffect } from 'react'
import { View, Text, ScrollView, Input } from '@tarojs/components'
import Taro from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import { getMyClassrooms, createClassroom, joinClassroom } from '@/services/classroom'
import type { Classroom } from '@/types/classroom'
import { CLASSROOM_STATUS, CLASSROOM_ROLE } from '@/types/classroom'

const STATUS_LABEL_MAP: Record<number, string> = {
  [CLASSROOM_STATUS.ACTIVE]: '进行中',
  [CLASSROOM_STATUS.CLOSED]: '已关闭'
}

const ROLE_LABEL_MAP: Record<number, string> = {
  [CLASSROOM_ROLE.TEACHER]: '教师',
  [CLASSROOM_ROLE.STUDENT]: '学生'
}

const ClassroomListPage: React.FC = () => {
  const [classrooms, setClassrooms] = useState<Classroom[]>([])
  const [loading, setLoading] = useState(false)
  const [showJoinDialog, setShowJoinDialog] = useState(false)
  const [inviteCode, setInviteCode] = useState('')
  const [studentNo, setStudentNo] = useState('')
  const [realName, setRealName] = useState('')
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [formName, setFormName] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formInstitution, setFormInstitution] = useState('')
  const [formGradeLevel, setFormGradeLevel] = useState('')

  const fetchClassrooms = async () => {
    setLoading(true)
    try {
      const data = await getMyClassrooms()
      setClassrooms(data)
    } catch (err) {
      console.error('[ClassroomList] fetchClassrooms failed', err)
      setClassrooms([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchClassrooms()
  }, [])

  const handleCardClick = (classroomId: number) => {
    Taro.navigateTo({
      url: `/pages/classroom-detail/index?classroomId=${classroomId}`
    })
  }

  const isTeacher = classrooms.some(c => c.myRole === CLASSROOM_ROLE.TEACHER)

  const handleFabClick = () => {
    if (isTeacher) {
      setShowCreateDialog(true)
    } else {
      setShowJoinDialog(true)
    }
  }

  const handleCreate = async () => {
    if (!formName.trim()) {
      Taro.showToast({ title: '请输入班级名称', icon: 'none' })
      return
    }
    try {
      const result = await createClassroom({
        name: formName.trim(),
        description: formDescription.trim() || undefined,
        institution: formInstitution.trim() || undefined,
        gradeLevel: formGradeLevel.trim() || undefined
      })
      if (result) {
        Taro.showToast({ title: '创建成功', icon: 'success' })
        setShowCreateDialog(false)
        resetCreateForm()
        fetchClassrooms()
      }
    } catch (err) {
      console.error('[ClassroomList] createClassroom failed', err)
    }
  }

  const handleJoin = async () => {
    if (!inviteCode.trim()) {
      Taro.showToast({ title: '请输入邀请码', icon: 'none' })
      return
    }
    try {
      const result = await joinClassroom({
        inviteCode: inviteCode.trim(),
        studentNo: studentNo.trim() || undefined,
        realName: realName.trim() || undefined
      })
      if (result) {
        Taro.showToast({ title: '加入成功', icon: 'success' })
        setShowJoinDialog(false)
        resetJoinForm()
        fetchClassrooms()
      }
    } catch (err) {
      console.error('[ClassroomList] joinClassroom failed', err)
    }
  }

  const resetCreateForm = () => {
    setFormName('')
    setFormDescription('')
    setFormInstitution('')
    setFormGradeLevel('')
  }

  const resetJoinForm = () => {
    setInviteCode('')
    setStudentNo('')
    setRealName('')
  }

  const closeCreateDialog = () => {
    setShowCreateDialog(false)
    resetCreateForm()
  }

  const closeJoinDialog = () => {
    setShowJoinDialog(false)
    resetJoinForm()
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.pageHeader}>
        <Text className={styles.headerTitle}>我的班级</Text>
      </View>

      {classrooms.length > 0 ? (
        <ScrollView scrollY className={styles.classroomList}>
          {classrooms.map((classroom) => (
            <View
              key={classroom.id}
              className={styles.classroomCard}
              onClick={() => handleCardClick(classroom.id)}
            >
              <View className={styles.classroomCardHeader}>
                <Text className={styles.classroomName}>{classroom.name}</Text>
                <View
                  className={classnames(
                    styles.statusBadge,
                    classroom.status === CLASSROOM_STATUS.ACTIVE && styles.statusActive,
                    classroom.status === CLASSROOM_STATUS.CLOSED && styles.statusClosed
                  )}
                >
                  <Text>{STATUS_LABEL_MAP[classroom.status]}</Text>
                </View>
              </View>

              {classroom.description ? (
                <Text className={styles.classroomDescription}>{classroom.description}</Text>
              ) : null}

              <View className={styles.classroomMeta}>
                {classroom.institution ? (
                  <View className={styles.classroomMetaItem}>
                    <Text className={styles.classroomMetaIcon}>🏫</Text>
                    <Text className={styles.classroomMetaText}>{classroom.institution}</Text>
                  </View>
                ) : null}
                {classroom.gradeLevel ? (
                  <View className={styles.classroomMetaItem}>
                    <Text className={styles.classroomMetaIcon}>📚</Text>
                    <Text className={styles.classroomMetaText}>{classroom.gradeLevel}</Text>
                  </View>
                ) : null}
              </View>

              <View className={styles.classroomFooter}>
                <View className={styles.teacherInfo}>
                  <View className={styles.teacherAvatar}>
                    <Text>{classroom.teacherNickname?.[0] || '师'}</Text>
                  </View>
                  <Text className={styles.teacherName}>
                    {classroom.teacherNickname}
                    {classroom.myRole ? ` · ${ROLE_LABEL_MAP[classroom.myRole]}` : ''}
                  </Text>
                </View>
                <Text className={styles.memberCount}>
                  <Text className={styles.memberCountValue}>{classroom.memberCount}</Text>
                  人
                </Text>
              </View>
            </View>
          ))}
        </ScrollView>
      ) : (
        <View className={styles.emptyState}>
          <Text className={styles.emptyIcon}>🎓</Text>
          <Text className={styles.emptyText}>暂无班级，创建或加入一个吧</Text>
        </View>
      )}

      <View className={styles.fabButton} onClick={handleFabClick}>
        <Text className={styles.fabIcon}>+</Text>
      </View>

      {showCreateDialog && (
        <View className={styles.dialogOverlay} onClick={closeCreateDialog}>
          <View className={styles.dialogContent} onClick={(e) => e.stopPropagation()}>
            <Text className={styles.dialogTitle}>创建班级</Text>
            <View className={styles.dialogForm}>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>班级名称</Text>
                <Input
                  className={styles.formInput}
                  value={formName}
                  onInput={(e) => setFormName(e.detail.value)}
                  placeholder="请输入班级名称"
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>班级描述</Text>
                <Input
                  className={styles.formInput}
                  value={formDescription}
                  onInput={(e) => setFormDescription(e.detail.value)}
                  placeholder="请输入班级描述（选填）"
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>所属机构</Text>
                <Input
                  className={styles.formInput}
                  value={formInstitution}
                  onInput={(e) => setFormInstitution(e.detail.value)}
                  placeholder="请输入所属机构（选填）"
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>年级</Text>
                <Input
                  className={styles.formInput}
                  value={formGradeLevel}
                  onInput={(e) => setFormGradeLevel(e.detail.value)}
                  placeholder="请输入年级（选填）"
                />
              </View>
            </View>
            <View className={styles.dialogActions}>
              <View className={classnames(styles.dialogButton, styles.dialogButtonCancel)} onClick={closeCreateDialog}>
                <Text>取消</Text>
              </View>
              <View className={classnames(styles.dialogButton, styles.dialogButtonConfirm)} onClick={handleCreate}>
                <Text>创建</Text>
              </View>
            </View>
          </View>
        </View>
      )}

      {showJoinDialog && (
        <View className={styles.dialogOverlay} onClick={closeJoinDialog}>
          <View className={styles.dialogContent} onClick={(e) => e.stopPropagation()}>
            <Text className={styles.dialogTitle}>加入班级</Text>
            <View className={styles.dialogForm}>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>邀请码</Text>
                <Input
                  className={styles.formInput}
                  value={inviteCode}
                  onInput={(e) => setInviteCode(e.detail.value)}
                  placeholder="请输入邀请码"
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>学号</Text>
                <Input
                  className={styles.formInput}
                  value={studentNo}
                  onInput={(e) => setStudentNo(e.detail.value)}
                  placeholder="请输入学号（选填）"
                />
              </View>
              <View className={styles.formItem}>
                <Text className={styles.formLabel}>真实姓名</Text>
                <Input
                  className={styles.formInput}
                  value={realName}
                  onInput={(e) => setRealName(e.detail.value)}
                  placeholder="请输入真实姓名（选填）"
                />
              </View>
            </View>
            <View className={styles.dialogActions}>
              <View className={classnames(styles.dialogButton, styles.dialogButtonCancel)} onClick={closeJoinDialog}>
                <Text>取消</Text>
              </View>
              <View className={classnames(styles.dialogButton, styles.dialogButtonConfirm)} onClick={handleJoin}>
                <Text>加入</Text>
              </View>
            </View>
          </View>
        </View>
      )}
    </View>
  )
}

export default ClassroomListPage
