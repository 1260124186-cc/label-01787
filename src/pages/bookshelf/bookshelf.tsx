import React from 'react'
import { View, Text } from '@tarojs/components'
import styles from './bookshelf.module.scss'

const BookshelfPage: React.FC = () => {
  return (
    <View className={styles.pageContainer}>
      <View className={styles.placeholderContent}>
        <Text className={styles.icon}>📖</Text>
        <Text className={styles.text}>书架功能</Text>
        <Text className={styles.text}>功能正在开发中...</Text>
      </View>
    </View>
  )
}

export default BookshelfPage
