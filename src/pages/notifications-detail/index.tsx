import React, { useState, useEffect } from 'react'
import { View, Text, Button } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import styles from './index.module.scss'
import { Notification } from '@/types/notification'
import { notificationTypes } from '@/data/mockNotifications'
import { getNotificationDetail } from '@/services/notification'

const NotificationDetailPage: React.FC = () => {
  const router = useRouter()
  const [notification, setNotification] = useState<Notification | null>(null)
  const [loading, setLoading] = useState(true)

  const id = Number(router.params.id)

  const fetchDetail = async () => {
    setLoading(true)
    try {
      console.log('[NotificationDetail] 获取消息详情, id:', id)
      
      const data = await getNotificationDetail(id)
      if (data) {
        setNotification(data)
      } else {
        Taro.showToast({ title: '消息不存在', icon: 'none' })
        setTimeout(() => Taro.navigateBack(), 1500)
      }
    } catch (err) {
      console.error('[NotificationDetail] 获取详情失败', err)
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  const handleGoto = () => {
    if (!notification) return
    
    console.log('[NotificationDetail] 跳转:', notification.extraData)
    
    if (!notification.extraData) {
      Taro.showToast({ title: '无需跳转', icon: 'none' })
      return
    }
    
    try {
      const extra = JSON.parse(notification.extraData)
      if (extra.page === 'reader' && extra.bookId) {
        Taro.showToast({ title: `跳转到书籍 ${extra.bookId}`, icon: 'none' })
      } else if (extra.page === 'bookshelf') {
        Taro.switchTab({ url: '/pages/bookshelf/bookshelf' })
      } else if (extra.page === 'group') {
        Taro.showToast({ title: `跳转到小组 ${extra.groupId}`, icon: 'none' })
      } else {
        Taro.showToast({ title: '跳转中...', icon: 'none' })
      }
    } catch (err) {
      console.error('[NotificationDetail] 解析跳转参数失败', err)
      Taro.showToast({ title: '参数错误', icon: 'none' })
    }
  }

  const getTypeIconBg = (type: number) => {
    const colors = ['#E8F3FF', '#FFF7E8', '#E8FFEA', '#F3E8FF']
    return colors[type - 1] || colors[0]
  }

  const getTypeColor = (type: number) => {
    const colors = ['#165DFF', '#FF7D00', '#00B42A', '#722ED1']
    return colors[type - 1] || colors[0]
  }

  useEffect(() => {
    fetchDetail()
  }, [id])

  if (loading || !notification) {
    return (
      <View className={styles.pageContainer}>
        <View className={styles.detailCard}>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className={styles.pageContainer}>
      <View className={styles.detailCard}>
        <View className={styles.detailHeader}>
          <View
            className={styles.typeIcon}
            style={{ background: getTypeIconBg(notification.type) }}
          >
            <Text>{notificationTypes[notification.type - 1]?.icon}</Text>
          </View>
          <View className={styles.headerContent}>
            <View
              className={styles.typeTag}
              style={{ background: getTypeColor(notification.type) }}
            >
              {notification.typeName}
            </View>
            <Text className={styles.title}>{notification.title}</Text>
            <Text className={styles.time}>{notification.createdAt}</Text>
          </View>
        </View>

        <View className={styles.detailContent}>
          <Text>{notification.content}</Text>
        </View>

        {notification.extraData && (
          <Button className={styles.actionBtn} onClick={handleGoto}>
            查看相关内容
          </Button>
        )}

        <View className={styles.readStatus}>
          <Text className={styles.statusIcon}>✓</Text>
          <Text className={styles.statusText}>
            {notification.isRead === 1 ? `已于 ${notification.readAt} 阅读` : '已标记为已读'}
          </Text>
        </View>
      </View>
    </View>
  )
}

export default NotificationDetailPage
