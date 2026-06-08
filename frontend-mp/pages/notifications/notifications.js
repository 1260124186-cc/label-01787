const app = getApp()
const { request } = require('../../utils/request')
const { notificationTypes, getTypeName, getTypeIcon, getTypeColor, getNotifications, getUnreadCount, markAsRead, markAllAsRead } = require('../../utils/notification')

Page({
  data: {
    activeType: 0,
    notificationTypes: [],
    notifications: [],
    unreadCount: { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 },
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false
  },

  onLoad() {
    this.setData({ notificationTypes })
    this.checkLoginAndLoad()
  },

  onShow() {
    if (this.data.isLogin) {
      this.fetchUnreadCount()
    } else {
      this.checkLoginAndLoad()
    }
  },

  async checkLoginAndLoad() {
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.fetchUnreadCount()
      this.fetchNotifications(true)
    } else {
      this.doLogin()
    }
  },

  doLogin() {
    wx.showLoading({ title: '登录中...' })
    app.login().then(() => {
      wx.hideLoading()
      this.setData({ isLogin: true })
      this.fetchUnreadCount()
      this.fetchNotifications(true)
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '登录失败', icon: 'none' })
      this.setData({ isLogin: false })
    })
  },

  async fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      this.setData({ unreadCount: res.data })
    } catch (e) {
      console.error('获取未读数量失败', e)
    }
  },

  async fetchNotifications(reset) {
    if (this.data.loading) return

    const currentPage = reset ? 1 : this.data.page
    this.setData({ loading: true })

    try {
      const type = this.data.activeType || undefined
      const res = await getNotifications(type, currentPage, 10)

      const records = res.data.records.map(item => ({
        ...item,
        typeName: getTypeName(item.type),
        typeIcon: getTypeIcon(item.type),
        typeColor: getTypeColor(item.type),
        showTime: this.formatTime(item.createdAt)
      }))

      if (reset) {
        this.setData({
          notifications: records,
          page: 2,
          hasMore: currentPage < res.data.pages
        })
      } else {
        this.setData({
          notifications: this.data.notifications.concat(records),
          page: this.data.page + 1,
          hasMore: currentPage < res.data.pages
        })
      }
    } catch (e) {
      console.error('获取消息列表失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false, refreshing: false })
      wx.stopPullDownRefresh()
    }
  },

  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now - date
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor(diff / (1000 * 60))

    if (days >= 7) {
      const month = date.getMonth() + 1
      const day = date.getDate()
      return `${month}月${day}日`
    } else if (days >= 1) {
      return `${days}天前`
    } else if (hours >= 1) {
      return `${hours}小时前`
    } else if (minutes >= 1) {
      return `${minutes}分钟前`
    } else {
      return '刚刚'
    }
  },

  async onPullDownRefresh() {
    this.setData({ refreshing: true })
    await this.fetchUnreadCount()
    await this.fetchNotifications(true)
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.fetchNotifications(false)
    }
  },

  switchType(e) {
    const type = e.currentTarget.dataset.type
    if (type === this.data.activeType) return
    this.setData({ activeType: type })
    this.fetchNotifications(true)
  },

  async handleItemClick(e) {
    const item = e.currentTarget.dataset.item
    if (!item) return

    if (item.isRead === 0) {
      try {
        await markAsRead(item.id)
        const notifications = this.data.notifications.map(n =>
          n.id === item.id ? { ...n, isRead: 1 } : n
        )
        const unreadCount = { ...this.data.unreadCount }
        if (unreadCount[item.type] > 0) {
          unreadCount[item.type]--
          unreadCount[0]--
        }
        this.setData({ notifications, unreadCount })
      } catch (e) {
        console.error('标记已读失败', e)
      }
    }

    wx.navigateTo({
      url: `/pages/notifications-detail/notifications-detail?id=${item.id}`
    })
  },

  handleMarkAllRead() {
    const { activeType, unreadCount } = this.data
    const count = activeType ? unreadCount[activeType] : unreadCount[0]
    if (count === 0) {
      wx.showToast({ title: '暂无未读消息', icon: 'none' })
      return
    }

    const typeName = activeType ? getTypeName(activeType) : '全部'
    wx.showModal({
      title: '提示',
      content: `确定将所有${typeName}标记为已读？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            const type = activeType || undefined
            await markAllAsRead(type)
            const notifications = this.data.notifications.map(n => ({ ...n, isRead: 1 }))
            const newUnreadCount = { ...this.data.unreadCount }
            if (activeType) {
              newUnreadCount[0] -= newUnreadCount[activeType]
              newUnreadCount[activeType] = 0
            } else {
              newUnreadCount[0] = 0
              newUnreadCount[1] = 0
              newUnreadCount[2] = 0
              newUnreadCount[3] = 0
              newUnreadCount[4] = 0
            }
            this.setData({ notifications, unreadCount: newUnreadCount })
            wx.showToast({ title: '已标记为已读', icon: 'success' })
          } catch (e) {
            console.error('标记已读失败', e)
            wx.showToast({ title: '操作失败', icon: 'none' })
          }
        }
      }
    })
  }
})
