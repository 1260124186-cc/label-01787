import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import styles from './index.module.scss'
import { getUnreadCount } from '@/services/notification'
import { ensureLoggedIn } from '@/services/user'

const MENU_ITEMS = [
  { icon: '📖', label: '我的书架', action: 'bookshelf' },
  { icon: '📊', label: '阅读统计', action: 'stats' },
  { icon: '🔔', label: '消息通知', action: 'notifications' },
  { icon: '⭐', label: '会员中心', action: 'membership' },
  { icon: '⚙️', label: '设置', action: 'settings' }
]

const ProfilePage: React.FC = () => {
  const [unreadCount, setUnreadCount] = useState(0)

  const fetchUnread = async () => {
    try {
      console.log('[Profile] 获取未读消息数量')
      await ensureLoggedIn()
      const data = await getUnreadCount()
      setUnreadCount(data[0] || 0)
    } catch (err) {
      console.error('[Profile] 获取未读数量失败', err)
    }
  }

  useEffect(() => {
    fetchUnread()
  }, [])

  useDidShow(() => {
    fetchUnread()
  })

  const handleMenuClick = (action: string) => {
    switch (action) {
      case 'bookshelf':
        Taro.switchTab({ url: '/pages/bookshelf/index' })
        break
      case 'stats':
        Taro.switchTab({ url: '/pages/stats/index' })
        break
      case 'notifications':
        Taro.switchTab({ url: '/pages/notifications/index' })
        break
      default:
        Taro.showToast({ title: '功能开发中', icon: 'none' })
    }
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.profileHeader}>
        <View className={styles.avatar}>👤</View>
        <View>
          <Text className={styles.userName}>小安读者</Text>
          <Text className={styles.userDesc}>阅读让生活更美好</Text>
        </View>
      </View>

      <View className={styles.menuSection}>
        <View className={styles.menuCard}>
          {MENU_ITEMS.map((item) => (
            <View
              key={item.action}
              className={styles.menuItem}
              onClick={() => handleMenuClick(item.action)}
            >
              <View className={styles.menuLeft}>
                <Text className={styles.menuIcon}>{item.icon}</Text>
                <Text className={styles.menuLabel}>{item.label}</Text>
                {item.action === 'notifications' && unreadCount > 0 && (
                  <View className={styles.messageBadge}>
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </View>
                )}
              </View>
              <Text className={styles.menuArrow}>›</Text>
            </View>
          ))}
        </View>
      </View>
    </View>
  )
}

export default ProfilePage
