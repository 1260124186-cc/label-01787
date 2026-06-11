import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Input, Picker } from '@tarojs/components'
import Taro from '@tarojs/taro'
import styles from './index.module.scss'
import { getBookList } from '@/services/book'
import { createReadingPlan } from '@/services/readingPlan'
import type { BookItem } from '@/types/book'
import type { ReadingPlanCreateDTO } from '@/types/readingPlan'

const ReadingPlanCreatePage: React.FC = () => {
  const [bookList, setBookList] = useState<BookItem[]>([])
  const [filteredBooks, setFilteredBooks] = useState<BookItem[]>([])
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedBookId, setSelectedBookId] = useState<number | null>(null)
  const [targetDays, setTargetDays] = useState(30)
  const [dailyMinMinutes, setDailyMinMinutes] = useState(10)
  const [reminderTime, setReminderTime] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    fetchBooks()
  }, [])

  useEffect(() => {
    if (!searchKeyword.trim()) {
      setFilteredBooks(bookList)
      return
    }
    const kw = searchKeyword.trim().toLowerCase()
    setFilteredBooks(
      bookList.filter(
        (b) =>
          b.title.toLowerCase().includes(kw) ||
          b.author.toLowerCase().includes(kw)
      )
    )
  }, [searchKeyword, bookList])

  const fetchBooks = async () => {
    try {
      const list = await getBookList()
      setBookList(list)
      setFilteredBooks(list)
    } catch {
      Taro.showToast({ title: '获取书籍列表失败', icon: 'none' })
    }
  }

  const handleSelectBook = useCallback((book: BookItem) => {
    setSelectedBookId(book.id)
    setSearchKeyword(book.title)
  }, [])

  const handleTargetDaysChange = useCallback((e) => {
    const val = parseInt(e.detail.value, 10)
    if (!isNaN(val)) {
      setTargetDays(Math.min(365, Math.max(1, val)))
    }
  }, [])

  const handleDailyMinMinutesChange = useCallback((e) => {
    const val = parseInt(e.detail.value, 10)
    if (!isNaN(val) && val > 0) {
      setDailyMinMinutes(val)
    }
  }, [])

  const handleReminderTimeChange = useCallback((e) => {
    setReminderTime(e.detail.value)
  }, [])

  const handleSubmit = useCallback(async () => {
    if (!selectedBookId) {
      Taro.showToast({ title: '请选择一本书籍', icon: 'none' })
      return
    }
    if (targetDays < 1 || targetDays > 365) {
      Taro.showToast({ title: '目标天数需在1-365之间', icon: 'none' })
      return
    }

    const dto: ReadingPlanCreateDTO = {
      bookId: selectedBookId,
      targetDays,
      dailyMinDuration: dailyMinMinutes * 60,
    }
    if (reminderTime) {
      dto.reminderTime = reminderTime
    }

    setSubmitting(true)
    try {
      const result = await createReadingPlan(dto)
      if (result) {
        Taro.showToast({ title: '创建成功', icon: 'success' })
        setTimeout(() => {
          Taro.navigateBack()
        }, 1500)
      } else {
        Taro.showToast({ title: '创建失败，请重试', icon: 'none' })
      }
    } catch {
      Taro.showToast({ title: '创建失败，请重试', icon: 'none' })
    } finally {
      setSubmitting(false)
    }
  }, [selectedBookId, targetDays, dailyMinMinutes, reminderTime])

  const selectedBook = bookList.find((b) => b.id === selectedBookId)

  return (
    <View className={styles.pageContainer}>
      <View className={styles.section}>
        <Text className={styles.sectionTitle}>选择书籍</Text>
        <Input
          className={styles.searchInput}
          placeholder="搜索书名或作者"
          value={searchKeyword}
          onInput={(e) => setSearchKeyword(e.detail.value)}
        />
        {filteredBooks.length > 0 ? (
          <View className={styles.bookList}>
            {filteredBooks.map((book) => (
              <View
                key={book.id}
                className={`${styles.bookItem} ${
                  selectedBookId === book.id ? styles.bookItemSelected : ''
                }`}
                onClick={() => handleSelectBook(book)}
              >
                <Text className={styles.bookTitle}>{book.title}</Text>
                <View className={styles.bookMeta}>
                  <Text className={styles.bookAuthor}>{book.author}</Text>
                  <Text className={styles.bookPageCount}>
                    {book.pageCount}页
                  </Text>
                </View>
                {selectedBookId === book.id && (
                  <Text className={styles.selectedHint}>已选择</Text>
                )}
              </View>
            ))}
          </View>
        ) : (
          <View className={styles.emptyBookList}>
            <Text className={styles.emptyText}>未找到相关书籍</Text>
          </View>
        )}
      </View>

      <View className={styles.formCard}>
        <View className={styles.formItem}>
          <Text className={styles.formLabel}>目标天数</Text>
          <View className={styles.formInputRow}>
            <Input
              className={styles.formInput}
              type="number"
              value={String(targetDays)}
              onInput={handleTargetDaysChange}
              placeholder="1-365"
            />
            <Text className={styles.formUnit}>天</Text>
          </View>
        </View>

        <View className={styles.formItem}>
          <Text className={styles.formLabel}>每日最少阅读时长</Text>
          <View className={styles.formInputRow}>
            <Input
              className={styles.formInput}
              type="number"
              value={String(dailyMinMinutes)}
              onInput={handleDailyMinMinutesChange}
              placeholder="分钟"
            />
            <Text className={styles.formUnit}>分钟</Text>
          </View>
        </View>

        <View className={styles.formItem}>
          <Text className={styles.formLabel}>提醒时间（可选）</Text>
          <Picker mode="time" value={reminderTime || '08:00'} onChange={handleReminderTimeChange}>
            <View className={styles.timePickerTrigger}>
              {reminderTime ? (
                <Text>{reminderTime}</Text>
              ) : (
                <Text className={styles.timePickerPlaceholder}>选择提醒时间</Text>
              )}
            </View>
          </Picker>
        </View>
      </View>

      <View className={styles.footerBar}>
        <View
          className={`${styles.submitBtn} ${
            submitting || !selectedBookId ? styles.submitBtnDisabled : ''
          }`}
          onClick={submitting ? undefined : handleSubmit}
        >
          {submitting ? '提交中...' : '创建计划'}
        </View>
      </View>
    </View>
  )
}

export default ReadingPlanCreatePage
