const { request } = require('../../utils/request')
const { formatDuration } = require('../../utils/util')

Page({
  data: {
    period: 'week',
    summary: {},
    dailyData: [],
    durationText: '0分钟',
    isLogin: false
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
    this.setData({ period: e.currentTarget.dataset.period })
    this.loadSummary()
  },

  async loadSummary() {
    if (!this.data.isLogin) return
    try {
      const res = await request({
        url: '/reading/summary',
        data: { period: this.data.period }
      })
      const data = res.data
      const totalSeconds = data.totalDuration || 0
      const durationText = formatDuration(totalSeconds)

      // 处理每日数据用于柱状图
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
        durationText
      })
    } catch (e) {
      console.error('加载统计失败', e)
    }
  }
})
