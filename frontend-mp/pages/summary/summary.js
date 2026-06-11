const { request } = require('../../utils/request')
const { formatDuration } = require('../../utils/util')

Page({
  data: {
    period: 'week',
    summary: {},
    dailyData: [],
    durationText: '0分钟',
    timeline: [],
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
      const [summaryRes, timelineRes] = await Promise.all([
        request({
          url: '/reading/summary',
          data: { period: this.data.period }
        }),
        request({
          url: '/reading/timeline',
          data: { period: this.data.period }
        })
      ])

      const data = summaryRes.data
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

      const timelineData = this.formatTimeline(timelineRes.data || [])

      this.setData({
        summary: data,
        dailyData,
        durationText,
        timeline: timelineData,
        isVip: data.isVip || false
      })
    } catch (e) {
      console.error('加载统计失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  formatTimeline(timelineList) {
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)

    return timelineList.map((day, index) => {
      const dateObj = new Date(day.date)
      const isToday = dateObj.toDateString() === today.toDateString()
      const isYesterday = dateObj.toDateString() === yesterday.toDateString()
      const dayOfWeek = weekDays[dateObj.getDay()]
      const dateLabel = isToday ? '今天' : isYesterday ? '昨天' : `${dateObj.getMonth() + 1}月${dateObj.getDate()}日`

      const books = (day.books || []).map(book => ({
        ...book,
        durationText: formatDuration(book.duration || 0)
      }))

      const totalDuration = books.reduce((sum, b) => sum + (b.duration || 0), 0)
      const totalMin = Math.round(totalDuration / 60)

      return {
        date: day.date,
        dateLabel,
        dayOfWeek,
        books,
        totalMin,
        isLast: index === timelineList.length - 1
      }
    }
  },

  openTimelineBook(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}` })
  },

  goMembership() {
    wx.navigateTo({ url: '/pages/membership/membership' })
  }
})
