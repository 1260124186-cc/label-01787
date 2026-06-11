import React, { useState, useEffect, useRef } from 'react'
import { View, Text } from '@tarojs/components'
import { useRouter, useDidShow, useDidHide } from '@tarojs/taro'
import styles from './index.module.scss'
import { startReading, endReading, updateBookProgress } from '@/services/reading'
import { mockBookList } from '@/data/mockBook'

const ReaderPage: React.FC = () => {
  const router = useRouter()
  const [bookId, setBookId] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [bookTitle, setBookTitle] = useState('')
  const [totalPages, setTotalPages] = useState(0)
  const [recordId, setRecordId] = useState<number | null>(null)
  const [pageRestored, setPageRestored] = useState(false)
  const lastSavedPage = useRef(1)
  const progressTimer = useRef<ReturnType<typeof setInterval> | null>(null)

  const saveProgress = async (page: number) => {
    if (bookId <= 0 || page === lastSavedPage.current) return
    lastSavedPage.current = page
    console.log('[Reader] 保存进度: bookId=', bookId, 'page=', page)
    await updateBookProgress(bookId, page)
  }

  const endSession = async () => {
    if (recordId !== null) {
      console.log('[Reader] 结束阅读会话: recordId=', recordId, 'lastPage=', currentPage)
      await endReading(recordId, currentPage)
      setRecordId(null)
    }
    await saveProgress(currentPage)
  }

  useEffect(() => {
    const id = Number(router.params.bookId) || 0
    const page = Number(router.params.page) || 1
    setBookId(id)
    setCurrentPage(page)
    setPageRestored(page > 1)

    console.log('[Reader] 打开阅读器: bookId=', id, 'page=', page)

    const book = mockBookList.find((b) => b.id === id)
    if (book) {
      setBookTitle(book.title)
      setTotalPages(book.pageCount)
    }

    const initSession = async () => {
      if (id > 0) {
        console.log('[Reader] 开始阅读会话: bookId=', id)
        const result = await startReading(id)
        if (result) {
          setRecordId(result.id)
          console.log('[Reader] 阅读会话已创建: recordId=', result.id)
        }
      }
    }
    initSession()

    return () => {
      if (progressTimer.current) {
        clearInterval(progressTimer.current)
      }
    }
  }, [router.params])

  useEffect(() => {
    if (bookId > 0 && currentPage > 0) {
      if (progressTimer.current) {
        clearInterval(progressTimer.current)
      }
      progressTimer.current = setInterval(() => {
        saveProgress(currentPage)
      }, 30000)
    }
    return () => {
      if (progressTimer.current) {
        clearInterval(progressTimer.current)
      }
    }
  }, [bookId, currentPage])

  useDidHide(() => {
    console.log('[Reader] 页面隐藏，保存进度')
    endSession()
  })

  useDidShow(() => {
    if (bookId > 0 && recordId === null) {
      console.log('[Reader] 页面重新显示，恢复阅读会话: bookId=', bookId)
      startReading(bookId).then((result) => {
        if (result) {
          setRecordId(result.id)
        }
      })
    }
  })

  useEffect(() => {
    return () => {
      console.log('[Reader] 组件卸载，保存进度')
      endSession()
    }
  }, [])

  const handlePrevPage = () => {
    if (currentPage > 1) {
      setCurrentPage((prev) => prev - 1)
    }
  }

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage((prev) => prev + 1)
    }
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.readerHeader}>
        <Text className={styles.bookTitle}>{bookTitle || '阅读器'}</Text>
        {pageRestored && (
          <Text className={styles.pageInfo}>
            已恢复至第 {currentPage} 页
          </Text>
        )}
      </View>

      <View className={styles.readerContent}>
        <Text className={styles.readingText}>
          第 {currentPage} 页内容展示区域，功能正在开发中...
        </Text>
      </View>

      <View className={styles.readerFooter}>
        <View className={styles.pageControl}>
          <View className={styles.pageBtn} onClick={handlePrevPage}>
            <Text>-</Text>
          </View>
          <Text className={styles.pageIndicator}>
            {currentPage}/{totalPages || '?'}
          </Text>
          <View className={styles.pageBtn} onClick={handleNextPage}>
            <Text>+</Text>
          </View>
        </View>
        <View className={styles.startBtn}>
          <Text>开始阅读</Text>
        </View>
      </View>
    </View>
  )
}

export default ReaderPage
