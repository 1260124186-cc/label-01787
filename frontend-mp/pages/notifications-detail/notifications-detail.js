const app = getApp()
const { getNotificationDetail, getTypeName, getTypeIcon, getTypeColor } = require('../../utils/notification')

Page({
  data: {
    notification: null,
    loading: true,
    notificationId: 0
  },

  onLoad(options) {
    const id = Number(options.id)
    if (id) {
      this.setData({ notificationId: id })
      this.checkLoginAndFetch(id)
    } else {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
    }
  },

  async checkLoginAndFetch(id) {
    if (app.globalData.token) {
      this.fetchDetail(id)
    } else {
      try {
        await app.login()
        this.fetchDetail(id)
      } catch (e) {
        console.error('登录失败', e)
        wx.showToast({ title: '登录失败', icon: 'none' })
        this.setData({ loading: false })
      }
    }
  },

  async fetchDetail(id) {
    this.setData({ loading: true })
    try {
      const res = await getNotificationDetail(id)
      const item = res.data
      const notification = {
        ...item,
        typeName: getTypeName(item.type),
        typeIcon: getTypeIcon(item.type),
        typeColor: getTypeColor(item.type),
        showTime: this.formatTime(item.createdAt),
        readTime: item.readAt ? this.formatTime(item.readAt) : ''
      }
      this.setData({ notification })
    } catch (e) {
      console.error('获取消息详情失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  },

  handleJump() {
    const { notification } = this.data
    if (!notification || !notification.extraData) {
      wx.showToast({ title: '无跳转链接', icon: 'none' })
      return
    }

    try {
      const extra = typeof notification.extraData === 'string'
        ? JSON.parse(notification.extraData)
        : notification.extraData

      if (extra && extra.url) {
        wx.navigateTo({
          url: extra.url,
          fail: () => {
            wx.showToast({ title: '跳转失败', icon: 'none' })
          }
        })
      } else {
        wx.showToast({ title: '无跳转链接', icon: 'none' })
      }
    } catch (e) {
      console.error('解析跳转参数失败', e)
      wx.showToast({ title: '跳转失败', icon: 'none' })
    }
  }
})
