import React from 'react'
import { View, Text } from '@tarojs/components'
import styles from './index.module.scss'

const IndexPage: React.FC = () => {
  return (
    <View className={styles.pageContainer}>
      <View className={styles.header}>
        <Text className={styles.title}>小安的书店</Text>
        <Text className={styles.subtitle}>让阅读成为一种生活方式</Text>
      </View>
      <View className={styles.placeholderContent}>
        <Text className={styles.icon}>📚</Text>
        <Text className={styles.text}>首页功能</Text>
        <Text className={styles.tip}>
          功能正在开发中...{'\n'}
          点击底部「消息」tab 查看消息通知中心
        </Text>
      </View>
    </View>
  )
}

export default IndexPage
