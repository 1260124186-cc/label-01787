import { useState, useCallback } from 'react'
import { View, Text, Input, Picker } from '@tarojs/components'
import Taro from '@tarojs/taro'
import styles from './index.module.scss'
import { createAssignment } from '@/services/classroom'
import type { AssignmentCreateDTO } from '@/types/classroom'

const AssignmentCreatePage: React.FC = () => {
  const params = Taro.getCurrentInstance().router?.params || {}
  const classroomId = Number(params.classroomId)

  const [bookTitle, setBookTitle] = useState('')
  const [bookAuthor, setBookAuthor] = useState('')
  const [bookId, setBookId] = useState<number | undefined>(undefined)
  const [startPage, setStartPage] = useState('')
  const [endPage, setEndPage] = useState('')
  const [deadline, setDeadline] = useState('')
  const [description, setDescription] = useState('')
  const [totalScore, setTotalScore] = useState('100')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = useCallback(async () => {
    if (!bookTitle.trim()) {
      Taro.showToast({ title: '请输入书名', icon: 'none' })
      return
    }

    const start = Number(startPage)
    const end = Number(endPage)
    if (isNaN(start) || isNaN(end)) {
      Taro.showToast({ title: '请输入有效的页码', icon: 'none' })
      return
    }
    if (start >= end) {
      Taro.showToast({ title: '起始页必须小于结束页', icon: 'none' })
      return
    }

    if (!deadline) {
      Taro.showToast({ title: '请选择截止日期', icon: 'none' })
      return
    }

    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const deadlineDate = new Date(deadline)
    if (deadlineDate < today) {
      Taro.showToast({ title: '截止日期不能早于今天', icon: 'none' })
      return
    }

    const dto: AssignmentCreateDTO = {
      bookTitle: bookTitle.trim(),
      startPage: start,
      endPage: end,
      deadline,
      totalScore: Number(totalScore) || 100,
    }

    if (bookAuthor.trim()) {
      dto.bookAuthor = bookAuthor.trim()
    }
    if (bookId !== undefined) {
      dto.bookId = bookId
    }
    if (description.trim()) {
      dto.description = description.trim()
    }

    setSubmitting(true)
    try {
      const result = await createAssignment(classroomId, dto)
      if (result) {
        Taro.showToast({ title: '布置成功', icon: 'success' })
        setTimeout(() => {
          Taro.navigateBack()
        }, 1500)
      } else {
        Taro.showToast({ title: '布置失败，请重试', icon: 'none' })
      }
    } catch {
      Taro.showToast({ title: '布置失败，请重试', icon: 'none' })
    } finally {
      setSubmitting(false)
    }
  }, [bookTitle, bookAuthor, bookId, startPage, endPage, deadline, description, totalScore, classroomId])

  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  return (
    <View className={styles.pageContainer}>
      <View className={styles.formGroup}>
        <Text className={styles.label}>书名 *</Text>
        <Input
          className={styles.formInput}
          placeholder="请输入书名"
          value={bookTitle}
          onInput={(e) => setBookTitle(e.detail.value)}
        />
      </View>

      <View className={styles.formGroup}>
        <Text className={styles.label}>作者</Text>
        <Input
          className={styles.formInput}
          placeholder="请输入作者（可选）"
          value={bookAuthor}
          onInput={(e) => setBookAuthor(e.detail.value)}
        />
      </View>

      <View className={styles.formGroup}>
        <Text className={styles.label}>起始页 *</Text>
        <Input
          className={styles.formInput}
          type="number"
          placeholder="请输入起始页码"
          value={startPage}
          onInput={(e) => setStartPage(e.detail.value)}
        />
      </View>

      <View className={styles.formGroup}>
        <Text className={styles.label}>结束页 *</Text>
        <Input
          className={styles.formInput}
          type="number"
          placeholder="请输入结束页码"
          value={endPage}
          onInput={(e) => setEndPage(e.detail.value)}
        />
      </View>

      <View className={styles.formGroup}>
        <Text className={styles.label}>截止日期 *</Text>
        <Picker mode="date" start={todayStr} value={deadline} onChange={(e) => setDeadline(e.detail.value)}>
          <View className={styles.formInput}>
            {deadline || <Text style={{ color: 'var(--color-text-tertiary, #999)' }}>请选择截止日期</Text>}
          </View>
        </Picker>
      </View>

      <View className={styles.formGroup}>
        <Text className={styles.label}>描述</Text>
        <Input
          className={styles.formTextarea}
          placeholder="请输入作业描述（可选）"
          value={description}
          onInput={(e) => setDescription(e.detail.value)}
        />
      </View>

      <View className={styles.formGroup}>
        <Text className={styles.label}>总分</Text>
        <Input
          className={styles.formInput}
          type="number"
          placeholder="默认100分"
          value={totalScore}
          onInput={(e) => setTotalScore(e.detail.value)}
        />
      </View>

      <View className={styles.submitButton}>
        <View
          className={`${styles.btnInner} ${submitting ? styles.btnDisabled : ''}`}
          onClick={submitting ? undefined : handleSubmit}
        >
          {submitting ? '提交中...' : '布置作业'}
        </View>
      </View>
    </View>
  )
}

export default AssignmentCreatePage
