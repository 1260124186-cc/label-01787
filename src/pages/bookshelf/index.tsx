import React, { useState, useEffect } from 'react'
import { View, Text, Image, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import styles from './index.module.scss'
import ContinueReadingCard from '@/components/ContinueReadingCard'
import { mockContinueReadingList } from '@/data/mockReading'
import { mockBookList } from '@/data/mockBook'
import { getContinueReadingList } from '@/services/reading'
import type { ContinueReadingItem } from '@/types/reading'
import type { BookItem } from '@/types/book'

const BookshelfPage: React.FC = () => {
  const [continueList, setContinueList] = useState<ContinueReadingItem[]>([])
  const [bookList, setBookList] = useState<BookItem[]>([])

  const fetchData = async () => {
    console.log('[Bookshelf] 获取书架数据')
    try {
      const apiData = await getContinueReadingList(5)
      if (apiData.length > 0) {
        setContinueList(apiData)
      } else {
        setContinueList(mockContinueReadingList)
      }
    } catch (err) {
      console.error('[Bookshelf] API获取继续阅读列表失败，使用mock数据', err)
      setContinueList(mockContinueReadingList)
    }
    setBookList(mockBookList)
  }

  useEffect(() => {
    fetchData()
  }, [])

  useDidShow(() => {
    fetchData()
  })

  const handleContinueReading = (item: ContinueReadingItem) => {
    console.log('[Bookshelf] 继续阅读:', item.bookTitle, '页码:', item.lastPage)
    Taro.navigateTo({
      url: `/pages/reader/index?bookId=${item.bookId}&page=${item.lastPage}`
    })
  }

  const handleBookClick = (book: BookItem) => {
    console.log('[Bookshelf] 打开书籍:', book.title)
    if (book.lastPage > 0) {
      Taro.navigateTo({
        url: `/pages/reader/index?bookId=${book.id}&page=${book.lastPage}`
      })
    } else {
      Taro.navigateTo({
        url: `/pages/reader/index?bookId=${book.id}&page=1`
      })
    }
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.bookshelfHeader}>
        <Text className={styles.headerTitle}>我的书架</Text>
        <Text className={styles.headerCount}>{bookList.length} 本书</Text>
      </View>

      {continueList.length > 0 && (
        <View className={styles.section}>
          <View className={styles.sectionHeader}>
            <Text className={styles.sectionTitle}>继续阅读</Text>
            <Text className={styles.sectionMore}>更多</Text>
          </View>
          <ScrollView
            className={styles.scrollView}
            scrollX
            enhanced
            showScrollbar={false}
          >
            <View className={styles.scrollContent}>
              {continueList.map((item) => (
                <ContinueReadingCard
                  key={item.bookId}
                  item={item}
                  onClick={handleContinueReading}
                />
              ))}
            </View>
          </ScrollView>
        </View>
      )}

      {continueList.length === 0 && (
        <View className={styles.emptyContinue}>
          <Text className={styles.emptyText}>暂无正在阅读的书籍</Text>
        </View>
      )}

      <View className={styles.section}>
        <View className={styles.sectionHeader}>
          <Text className={styles.sectionTitle}>全部书籍</Text>
        </View>
        <View className={styles.bookListSection}>
          <View className={styles.bookGrid}>
            {bookList.map((book) => {
              const progress = book.pageCount > 0 ? Math.round((book.lastPage / book.pageCount) * 100) : 0
              const coverId = (book.id * 37 + 13) % 200 + 1
              return (
                <View
                  key={book.id}
                  className={styles.bookItem}
                  onClick={() => handleBookClick(book)}
                >
                  <Image
                    className={styles.bookCover}
                    src={`https://picsum.photos/id/${coverId}/200/260`}
                    mode="aspectFill"
                  />
                  <View className={styles.bookInfo}>
                    <Text className={styles.bookTitle}>{book.title}</Text>
                    <Text className={styles.bookAuthor}>{book.author}</Text>
                    {book.lastPage > 0 && (
                      <>
                        <View className={styles.bookProgress}>
                          <View className={styles.bookProgressFill} style={{ width: `${progress}%` }} />
                        </View>
                        <Text className={styles.bookProgressText}>已读{progress}%</Text>
                      </>
                    )}
                  </View>
                </View>
              )
            })}
          </View>
        </View>
      </View>
    </View>
  )
}

export default BookshelfPage
