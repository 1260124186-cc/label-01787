const { request } = require('../../utils/request')

Page({
  data: {
    excerpt: null,
    excerptId: null,
    isOwner: false,
    showReportModal: false,
    selectedReason: '',
    reportDetail: '',
    reportReasons: [
      '内容违规',
      '侵权抄袭',
      '垃圾广告',
      '色情低俗',
      '其他原因'
    ]
  },

  onLoad(options) {
    this.setData({ excerptId: options.id })
    this.loadDetail()
  },

  onShareAppMessage() {
    if (this.data.excerpt) {
      return {
        title: `${this.data.excerpt.bookTitle} - 精彩书摘`,
        path: `/pages/plaza-detail/plaza-detail?id=${this.data.excerptId}`
      }
    }
    return {
      title: '书摘广场',
      path: '/pages/plaza/plaza'
    }
  },

  async loadDetail() {
    try {
      const res = await request({
        url: `/plaza/excerpts/${this.data.excerptId}`
      })
      const app = getApp()
      const isOwner = res.data.userId === (app.globalData.userId || 0)
      this.setData({ excerpt: res.data, isOwner })
    } catch (e) {
      console.error('加载详情失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  async toggleLike() {
    try {
      const res = await request({
        url: `/plaza/excerpts/${this.data.excerptId}/like`,
        method: 'POST'
      })
      const liked = res.data.liked
      const currentLikes = this.data.excerpt.likes || 0
      this.setData({
        'excerpt.liked': liked,
        'excerpt.likes': liked ? currentLikes + 1 : Math.max(0, currentLikes - 1)
      })
    } catch (e) {
      console.error('点赞失败', e)
    }
  },

  async toggleFavorite() {
    try {
      const res = await request({
        url: `/plaza/excerpts/${this.data.excerptId}/favorite`,
        method: 'POST'
      })
      const favorited = res.data.favorited
      const currentFavs = this.data.excerpt.favorites || 0
      this.setData({
        'excerpt.favorited': favorited,
        'excerpt.favorites': favorited ? currentFavs + 1 : Math.max(0, currentFavs - 1)
      })
      wx.showToast({
        title: favorited ? '已收藏' : '已取消收藏',
        icon: 'success'
      })
    } catch (e) {
      console.error('收藏失败', e)
    }
  },

  shareExcerpt() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
    if (this.data.excerpt) {
      wx.showModal({
        title: '分享书摘',
        content: `"${this.data.excerpt.excerptText}"\n\n——《${this.data.excerpt.bookTitle}》`,
        confirmText: '复制文字',
        success: (res) => {
          if (res.confirm) {
            wx.setClipboardData({
              data: `"${this.data.excerpt.excerptText}"\n\n——《${this.data.excerpt.bookTitle}》\n来自小安的书店`,
              success: () => {
                wx.showToast({ title: '已复制到剪贴板', icon: 'success' })
              }
            })
          }
        }
      })
    }
  },

  showReport() {
    this.setData({ showReportModal: true, selectedReason: '', reportDetail: '' })
  },

  hideReport() {
    this.setData({ showReportModal: false })
  },

  stopPropagation() {
  },

  selectReason(e) {
    const reason = e.currentTarget.dataset.reason
    this.setData({ selectedReason: reason })
  },

  onDetailInput(e) {
    this.setData({ reportDetail: e.detail.value })
  },

  async submitReport() {
    if (!this.data.selectedReason) return
    try {
      await request({
        url: '/plaza/report',
        method: 'POST',
        data: {
          excerptId: this.data.excerptId,
          reason: this.data.selectedReason,
          detail: this.data.reportDetail
        }
      })
      wx.showToast({ title: '举报已提交', icon: 'success' })
      this.setData({ showReportModal: false })
    } catch (e) {
      console.error('举报失败', e)
      wx.showToast({ title: '举报失败', icon: 'none' })
    }
  },

  withdraw() {
    wx.showModal({
      title: '撤回确认',
      content: '确定要撤回来这条书摘吗？撤回后将不再在广场展示。',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: `/plaza/${this.data.excerptId}/withdraw`,
              method: 'POST'
            })
            wx.showToast({ title: '已撤回', icon: 'success' })
            setTimeout(() => {
              wx.navigateBack()
            }, 1500)
          } catch (e) {
            console.error('撤回失败', e)
            wx.showToast({ title: '撤回失败', icon: 'none' })
          }
        }
      }
    })
  }
})
