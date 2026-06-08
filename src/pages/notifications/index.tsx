import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView, Button, PullToRefresh } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import classnames from 'classnames'
import styles from './index.module.scss'
import { Notification, NotificationType, UnreadCount } from '@/types/notification'
import { notificationTypes, mockUnreadCount, getMockNotifications } from '@/data/mockNotifications'

const NotificationsPage: React.FC = () => {
  const [activeType, setActiveType] = useState<number>(0)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState<UnreadCount>(mockUnreadCount)
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)

  const fetchUnreadCount = useCallback(async () => {
    try {
      console.log('[Notification] 获取未读数量')
      setUnreadCount(mockUnreadCount)
    } catch (err) {
      console.error('[Notification] 获取未读数量失败', err)
    }
  }, [])

  const fetchNotifications = useCallback(async (reset: boolean = false) => {
    if (loading) return
    
    const currentPage = reset ? 1 : page
    setLoading(true)
    
    try {
      console.log('[Notification] 获取消息列表, type:', activeType, 'page:', currentPage)
      
      const res = getMockNotifications(activeType || undefined, currentPage, 10)
      
      if (reset) {
        setNotifications(res.records)
        setPage(2)
      } else {
        setNotifications(prev => [...prev, ...res.records])
        setPage(prev => prev + 1)
      }
      setHasMore(currentPage < res.pages)
    } catch (err) {
      console.error('[Notification] 获取消息列表失败', err)
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [activeType, loading, page])

  const handleRefresh = () => {
    setRefreshing(true)
    fetchNotifications(true)
    fetchUnreadCount()
  }

  const loadMore = () => {
    if (!loading && hasMore) {
      fetchNotifications(false)
    }
  }

  const handleTypeClick = (type: number) => {
    setActiveType(type)
    setPage(1)
    setHasMore(true)
    setNotifications([])
  }

  const handleItemClick = (item: Notification) => {
    console.log('[Notification] 点击消息:', item.id)
    
    if (item.isRead === 0) {
      setNotifications(prev => prev.map(n => 
        n.id === item.id ? { ...n, isRead: 1 } : n
      ))
      const typeKey = item.type as keyof UnreadCount
      if (unreadCount[typeKey] > 0) {
        setUnreadCount(prev => ({
          ...prev,
          [typeKey]: prev[typeKey] - 1,
          0: prev[0] - 1
        }))
      }
    }
    
    Taro.navigateTo({
      url: `/pages/notifications-detail/index?id=${item.id}`
    })
  }

  const handleGoto = (e: React.MouseEvent, item: Notification) => {
    e.stopPropagation()
    console.log('[Notification] 跳转:', item.extraData)
    
    if (!item.extraData) {
      handleItemClick(item)
      return
    }
    
    try {
      const extra = JSON.parse(item.extraData)
      if (extra.page === 'reader' && extra.bookId) {
        Taro.showToast({ title: `跳转到书籍 ${extra.bookId}`, icon: 'none' })
      } else if (extra.page === 'bookshelf') {
        Taro.switchTab({ url: '/pages/bookshelf/bookshelf' })
      } else if (extra.page === 'group') {
        Taro.showToast({ title: `跳转到小组 ${extra.groupId}`, icon: 'none' })
      } else {
        handleItemClick(item)
      }
    } catch {
      handleItemClick(item)
    }
  }

  const handleMarkAllRead = async () => {
    try {
      const count = activeType ? unreadCount[activeType as keyof UnreadCount] : unreadCount[0]
      if (count === 0) {
        Taro.showToast({ title: '暂无未读消息', icon: 'none' })
        return
      }
      
      Taro.showModal({
        title: '提示',
        content: activeType ? `确定将所有${notificationTypes[activeType - 1]?.name}标记为已读？` : '确定将所有消息标记为已读？',
        success: (res) => {
          if (res.confirm) {
            console.log('[Notification] 全部已读, type:', activeType)
            setNotifications(prev => prev.map(n => ({ ...n, isRead: 1 })))
            if (activeType) {
              setUnreadCount(prev => ({
                ...prev,
                [activeType]: 0,
                0: prev[0] - prev[activeType as keyof UnreadCount]
              }))
            } else {
              setUnreadCount(prev => ({
                ...prev,
                0: 0,
                1: 0,
                2: 0,
                3: 0,
                4: 0
              }))
            }
            Taro.showToast({ title: '已标记为已读', icon: 'success' })
          }
        }
      })
    } catch (err) {
      console.error('[Notification] 标记已读失败', err)
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
    fetchNotifications(true)
    fetchUnreadCount()
  }, [activeType])

  useDidShow(() => {
    fetchUnreadCount()
  })

  return (
    <View className={styles.pageContainer}>
      <View className={styles.header}>
        <Text className={styles.title}>消息中心</Text>
        <Button className={styles.readAllBtn} onClick={handleMarkAllRead}>
          全部已读
        </Button>
      </View>

      <ScrollView className={styles.typeTabs} scrollX showScrollbar={false}>
        <View
          className={classnames(styles.tabItem, activeType === 0 && styles.active)}
          onClick={() => handleTypeClick(0)}
        >
          <Text className={styles.tabIcon}>📬</Text>
          <Text className={styles.tabName}>全部</Text>
          <Text className={styles.tabCount}>{unreadCount[0]}条未读</Text>
          {unreadCount[0] > 0 && (
            <View className={styles.unreadBadge}>{unreadCount[0] > 99 ? '99+' : unreadCount[0]}</View>
          )}
        </View>
        {notificationTypes.map((type: NotificationType) => (
          <View
            key={type.id}
            className={classnames(styles.tabItem, activeType === type.id && styles.active)}
            onClick={() => handleTypeClick(type.id)}
          >
            <Text className={styles.tabIcon}>{type.icon}</Text>
            <Text className={styles.tabName}>{type.name}</Text>
            <Text className={styles.tabCount}>{unreadCount[type.id as keyof UnreadCount]}条未读</Text>
            {unreadCount[type.id as keyof UnreadCount] > 0 && (
              <View className={styles.unreadBadge}>
                {unreadCount[type.id as keyof UnreadCount] > 99 ? '99+' : unreadCount[type.id as keyof UnreadCount]}
              </View>
            )}
          </View>
        ))}
      </ScrollView>

      <View className={styles.listHeader}>
        <Text className={styles.listTitle}>
          {activeType === 0 ? '全部消息' : notificationTypes[activeType - 1]?.name}
        </Text>
        <Text className={styles.listCount}>共 {notifications.length} 条</Text>
      </View>

      <PullToRefresh
        onRefresh={handleRefresh}
        isOpened={refreshing}
      >
        <ScrollView
          className={styles.notificationList}
          scrollY
          onScrollToLower={loadMore}
          lowerThreshold={100}
        >
          {notifications.length === 0 && !loading ? (
            <View className={styles.emptyState}>
              <Text className={styles.emptyIcon}>📭</Text>
              <Text className={styles.emptyText}>暂无消息</Text>
            </View>
          ) : (
            notifications.map((item: Notification) => (
              <View
                key={item.id}
                className={styles.notificationItem}
                onClick={() => handleItemClick(item)}
              >
                {item.isRead === 0 && <View className={styles.unreadDot} />}
                
                <View className={styles.itemHeader}>
                  <View
                    className={styles.itemIcon}
                    style={{ background: getTypeIconBg(item.type) }}
                  >
                    <Text>{notificationTypes[item.type - 1]?.icon}</Text>
                  </View>
                  <View className={styles.itemContent}>
                    <Text className={styles.itemTitle}>{item.title}</Text>
                    <Text className={styles.itemDesc}>{item.content}</Text>
                  </View>
                  <Text className={styles.itemTime}>
                    {item.createdAt.split(' ')[0]}
                  </Text>
                </View>
                
                <View className={styles.itemFooter}>
                  <View
                    className={styles.typeTag}
                    style={{ background: getTypeColor(item.type) }}
                  >
                    {item.typeName}
                  </View>
                  <Button
                    className={styles.gotoBtn}
                    onClick={(e) => handleGoto(e, item)}
                  >
                    查看详情
                  </Button>
                </View>
              </View>
            ))
          )}

          {loading && (
            <View className={styles.loadingState}>
              <Text className={styles.loadingText}>加载中...</Text>
            </View>
          )}
          
          {!hasMore && notifications.length > 0 && (
            <View className={styles.loadingState}>
              <Text className={styles.loadingText}>没有更多了</Text>
            </View>
          )}
        </ScrollView>
      </PullToRefresh>
    </View>
  )
}

export default NotificationsPage
