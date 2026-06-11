const { request } = require('../../utils/request')

Page({
  data: {
    excerpts: [],
    sortBy: 'latest',
    page: 1,
    hasMore: true,
    loading: false,
    isLogin: false
  },

  onLoad() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadExcerpts()
    } else {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.loadExcerpts()
      }).catch(() => {
        this.setData({ isLogin: false, excerpts: [] })
      })
    }
  },

  onShow() {
    if (this.data.isLogin && this.data.excerpts.length === 0) {
      this.loadExcerpts()
    }
  },

  onPullDownRefresh() {
    this.refreshList()
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadExcerpts()
    }
  },

  setSort(e) {
    const sortBy = e.currentTarget.dataset.sort
    this.setData({ sortBy })
    this.refreshList()
  },

  async refreshList() {
    this.setData({ page: 1, hasMore: true, excerpts: [] })
    await this.loadExcerpts()
    wx.stopPullDownRefresh()
  },

  async loadExcerpts() {
    if (this.data.loading || !this.data.isLogin) return
    this.setData({ loading: true })
    try {
      const res = await request({
        url: '/plaza/excerpts',
        data: {
          sortBy: this.data.sortBy,
          page: this.data.page,
          size: 20
        }
      })
      const records = res.data.records || []
      this.setData({
        excerpts: this.data.page === 1 ? records : [...this.data.excerpts, ...records],
        page: this.data.page + 1,
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载书摘失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/plaza-detail/plaza-detail?id=${id}` })
  },

  async toggleLike(e) {
    const id = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    try {
      const res = await request({
        url: `/plaza/excerpts/${id}/like`,
        method: 'POST'
      })
      const liked = res.data.liked
      const key = `excerpts[${index}].liked`
      const likesKey = `excerpts[${index}].likes`
      const currentLikes = this.data.excerpts[index].likes || 0
      this.setData({
        [key]: liked,
        [likesKey]: liked ? currentLikes + 1 : Math.max(0, currentLikes - 1)
      })
    } catch (e) {
      console.error('点赞失败', e)
    }
  },

  async toggleFavorite(e) {
    const id = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    try {
      const res = await request({
        url: `/plaza/excerpts/${id}/favorite`,
        method: 'POST'
      })
      const favorited = res.data.favorited
      const key = `excerpts[${index}].favorited`
      const favKey = `excerpts[${index}].favorites`
      const currentFavs = this.data.excerpts[index].favorites || 0
      this.setData({
        [key]: favorited,
        [favKey]: favorited ? currentFavs + 1 : Math.max(0, currentFavs - 1)
      })
    } catch (e) {
      console.error('收藏失败', e)
    }
  }
})
