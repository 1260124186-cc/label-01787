const { request } = require('../../utils/request')

const CATEGORY_ICONS = {
  daily_checkin: '\uD83D\uDCC5',
  upload_book: '\uD83D\uDCDA',
  share_excerpt: '\uD83D\uDCA1'
}

Page({
  data: {
    pointsInfo: {
      balance: 0,
      totalEarned: 0,
      totalConsumed: 0,
      todayCheckedIn: false
    },
    historyList: [],
    rules: [],
    page: 1,
    hasMore: true
  },

  onShow() {
    this.loadPointsInfo()
    this.loadRules()
    this.resetAndLoadHistory()
  },

  async loadPointsInfo() {
    try {
      const res = await request({ url: '/points/info' })
      this.setData({ pointsInfo: res.data || {} })
    } catch (e) {
      console.error('加载积分信息失败', e)
    }
  },

  async loadRules() {
    try {
      const res = await request({ url: '/points/rules' })
      const rules = (res.data || []).map(r => ({
        ...r,
        icon: CATEGORY_ICONS[r.category] || '\u2B50'
      }))
      this.setData({ rules })
    } catch (e) {
      console.error('加载积分规则失败', e)
    }
  },

  resetAndLoadHistory() {
    this.setData({ page: 1, hasMore: true, historyList: [] })
    this.loadHistory()
  },

  async loadHistory() {
    if (!this.data.hasMore) return
    try {
      const res = await request({
        url: '/points/history',
        data: { page: this.data.page, size: 20 }
      })
      const list = (res.data.records || []).map(r => ({
        ...r,
        amount: r.type === 1 ? r.points : -r.points,
        createTime: r.createdAt
      }))
      this.setData({
        historyList: this.data.page === 1 ? list : this.data.historyList.concat(list),
        hasMore: list.length >= 20,
        page: this.data.page + 1
      })
    } catch (e) {
      console.error('加载积分记录失败', e)
    }
  },

  async doCheckIn() {
    try {
      await request({ url: '/points/checkin', method: 'POST' })
      wx.showToast({ title: '签到成功', icon: 'success' })
      this.loadPointsInfo()
      this.resetAndLoadHistory()
    } catch (e) {
      console.error('签到失败', e)
    }
  },

  onReachBottom() {
    this.loadHistory()
  }
})
