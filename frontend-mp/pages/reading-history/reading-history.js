const { request, getBaseUrl } = require('../../utils/request')
const { formatDuration, formatDateTime } = require('../../utils/util')

function getCoverThumbnailUrl(bookId, coverThumbnail) {
  if (!coverThumbnail || !bookId) {
    return ''
  }
  const baseUrl = getBaseUrl()
  if (coverThumbnail.startsWith('http://') || coverThumbnail.startsWith('https://')) {
    return coverThumbnail
  }
  return `${baseUrl}/api/mp/books/${bookId}/cover-thumbnail`
}

Page({
  data: {
    historyList: [],
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.refresh()
    } else {
      this.setData({ isLogin: false, historyList: [] })
    }
  },

  onPullDownRefresh() {
    this.refresh().then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadHistory()
    }
  },

  async refresh() {
    this.setData({ page: 1, hasMore: true, historyList: [] })
    await this.loadHistory()
  },

  async loadHistory() {
    if (this.data.loading || !this.data.isLogin) return
    this.setData({ loading: true })
    try {
      const res = await request({
        url: '/reading/history',
        data: {
          page: this.data.page,
          size: 20
        }
      })
      const records = (res.data.records || []).map(item => ({
        ...item,
        sessionDurationText: formatDuration(item.sessionDuration || 0),
        totalDurationText: formatDuration(item.totalDuration || 0),
        lastReadTimeText: formatDateTime(item.lastReadTime),
        coverThumbnailUrl: getCoverThumbnailUrl(item.bookId, item.coverThumbnail),
        coverLoadFailed: false
      }))
      this.setData({
        historyList: this.data.page === 1 ? records : [...this.data.historyList, ...records],
        page: this.data.page + 1,
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载阅读历史失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openBook(e) {
    const id = e.currentTarget.dataset.id
    const page = e.currentTarget.dataset.page || 1
    const chapter = e.currentTarget.dataset.chapter || 0
    const format = e.currentTarget.dataset.format || 'pdf'
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}&page=${page}&chapter=${chapter}&format=${format}` })
  },

  onCoverError(e) {
    const index = e.currentTarget.dataset.index
    const list = [...this.data.historyList]
    if (list[index]) {
      list[index].coverLoadFailed = true
      this.setData({ historyList: list })
    }
  }
})
