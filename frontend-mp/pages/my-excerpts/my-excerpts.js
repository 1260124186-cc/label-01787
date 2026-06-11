const { request } = require('../../utils/request')

Page({
  data: {
    tab: 'published',
    excerpts: [],
    page: 1,
    hasMore: true,
    loading: false,
    isLogin: false
  },

  onLoad() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadData()
    } else {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.loadData()
      })
    }
  },

  onShow() {
    if (this.data.isLogin) {
      this.refreshData()
    }
  },

  onPullDownRefresh() {
    this.refreshData()
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadData()
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ tab })
    this.refreshData()
  },

  refreshData() {
    this.setData({ page: 1, hasMore: true, excerpts: [] })
    this.loadData()
    wx.stopPullDownRefresh()
  },

  async loadData() {
    if (this.data.loading || !this.data.isLogin) return
    this.setData({ loading: true })
    try {
      let url, data
      if (this.data.tab === 'published') {
        url = '/plaza/my/excerpts'
        data = { page: this.data.page, size: 20 }
      } else {
        url = '/plaza/my/favorites'
        data = { page: this.data.page, size: 20 }
      }
      const res = await request({ url, data })
      const records = res.data.records || []
      this.setData({
        excerpts: this.data.page === 1 ? records : [...this.data.excerpts, ...records],
        page: this.data.page + 1,
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/plaza-detail/plaza-detail?id=${id}` })
  },

  withdraw(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '撤回确认',
      content: '确定要撤回来这条书摘吗？撤回后将不再在广场展示。',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: `/plaza/${id}/withdraw`,
              method: 'POST'
            })
            wx.showToast({ title: '已撤回', icon: 'success' })
            this.refreshData()
          } catch (e) {
            console.error('撤回失败', e)
            wx.showToast({ title: '撤回失败', icon: 'none' })
          }
        }
      }
    })
  },

  async cancelFavorite(e) {
    const id = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    try {
      await request({
        url: `/plaza/excerpts/${id}/favorite`,
        method: 'POST'
      })
      const excerpts = this.data.excerpts
      excerpts.splice(index, 1)
      this.setData({ excerpts })
      wx.showToast({ title: '已取消收藏', icon: 'success' })
    } catch (e) {
      console.error('取消收藏失败', e)
    }
  }
})
