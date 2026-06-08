import React, { useState, useEffect } from 'react'
import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import styles from './profile.module.scss'
import { mockUnreadCount } from '@/data/mockNotifications'

const ProfilePage: React.FC = () => {
  const [unreadCount, setUnreadCount] = useState(mockUnreadCount[0])

  const fetchUnread = () => {
    console.log('[Profile] 获取未读消息数量')
    setUnreadCount(mockUnreadCount[0])
  }

  const goToMessages = () => {
    Taro.switchTab({ url: '/pages/notifications/index' })
  }

  useEffect(() => {
    fetchUnread()
  }, [])

  useDidShow(() => {
    fetchUnread()
  })

  return (
    <View className={styles.pageContainer}>
      <View className={styles.placeholderContent}>
        <Text className={styles.icon}>👤</Text>
        <Text className={styles.text}>个人中心</Text>
        <Text className={styles.text}>功能正在开发中...</Text>
        <Button onClick={goToMessages}>
          查看消息
          {unreadCount > 0 && (
            <View className={styles.messageBadge}>
              {unreadCount > 99 ? '99+' : unreadCount}
            </View>
          )}
        </Button>
      </View>
    </View>
  )
}

export default ProfilePage
