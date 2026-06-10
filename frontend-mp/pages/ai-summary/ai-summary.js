const { request } = require('../../utils/request')

Page({
  data: {
    bookId: null,
    bookTitle: '',
    summary: null,
    loading: false,
    generating: false,
    aiUsedToday: 0,
    aiDailyLimit: 5,
    isVip: false
  },

  onLoad(options) {
    const bookId = options.bookId
    const bookTitle = options.title || ''
    this.setData({ bookId, bookTitle })
    this.loadSummary()
  },

  async loadSummary() {
    if (!this.data.bookId) return
    this.setData({ loading: true })
    try {
      const res = await request({
        url: `/ai/summary/${this.data.bookId}`
      })
      if (res.data) {
        const summary = res.data
        summary.keyPointsList = summary.keyPoints ? summary.keyPoints.split('\n').filter(p => p.trim()) : []
        this.setData({ summary })
      }
      const quotaRes = await request({ url: '/membership/quota' })
      const quota = quotaRes.data || {}
      this.setData({
        aiUsedToday: quota.aiUsedToday || 0,
        aiDailyLimit: quota.aiDailyLimit || 5,
        isVip: quota.isVip || false
      })
    } catch (e) {
      console.error('加载摘要失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  async generateSummary() {
    if (this.data.generating) return
    if (!this.data.isVip && this.data.aiUsedToday >= this.data.aiDailyLimit) {
      wx.showModal({
        title: '次数不足',
        content: '今日AI使用次数已达上限，升级会员可享无限AI使用',
        confirmText: '升级会员',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/membership/membership' })
          }
        }
      })
      return
    }

    this.setData({ generating: true })
    try {
      const res = await request({
        url: '/ai/summary',
        method: 'POST',
        data: { bookId: this.data.bookId }
      })
      const summary = res.data
      summary.keyPointsList = summary.keyPoints ? summary.keyPoints.split('\n').filter(p => p.trim()) : []
      this.setData({
        summary,
        aiUsedToday: this.data.aiUsedToday + 1
      })
      wx.showToast({ title: '生成成功', icon: 'success' })
    } catch (e) {
      wx.showToast({
        title: e.message || '生成失败',
        icon: 'none'
      })
    } finally {
      this.setData({ generating: false })
    }
  },

  onShareAppMessage() {
    return {
      title: `《${this.data.bookTitle}》AI摘要`,
      path: `/pages/book-detail/book-detail?id=${this.data.bookId}`
    }
  }
})
