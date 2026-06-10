const { request } = require('../../utils/request')
const { formatDuration } = require('../../utils/util')

Page({
  data: {
    period: 'week',
    summary: {},
    dailyData: [],
    durationText: '0分钟',
    isLogin: false,
    isVip: false,
    loading: false
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadSummary()
    } else {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.loadSummary()
      }).catch(() => {
        this.setData({ isLogin: false })
      })
    }
  },

  setPeriod(e) {
    const period = e.currentTarget.dataset.period
    if (period === 'year' && !this.data.isVip) {
      wx.showModal({
        title: '会员专属',
        content: '年度统计为会员专属功能，升级会员即可查看',
        confirmText: '升级会员',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/membership/membership' })
          }
        }
      })
      return
    }
    this.setData({ period })
    this.loadSummary()
  },

  async loadSummary() {
    if (!this.data.isLogin) return
    this.setData({ loading: true })
    try {
      const res = await request({
        url: '/reading/summary',
        data: { period: this.data.period }
      })
      const data = res.data
      const totalSeconds = data.totalDuration || 0
      const durationText = formatDuration(totalSeconds)

      const daily = data.dailyData || []
      const maxVal = Math.max(...daily.map(d => Number(d.total) || 0), 1)
      const dailyData = daily.map(d => {
        const total = Number(d.total) || 0
        const minutes = Math.round(total / 60)
        const height = Math.max(Math.round((total / maxVal) * 240), 8)
        const dateStr = String(d.date || '')
        const label = dateStr.length >= 10 ? dateStr.substring(5, 10) : dateStr
        return { date: dateStr, minutes, height, label }
      })

      this.setData({
        summary: data,
        dailyData,
        durationText,
        isVip: data.isVip || false
      })
    } catch (e) {
      console.error('加载统计失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  goMembership() {
    wx.navigateTo({ url: '/pages/membership/membership' })
  }
})
