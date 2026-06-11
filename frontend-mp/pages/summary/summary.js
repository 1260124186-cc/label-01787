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
    loading: false,
    goalProgress: null,
    todayDurationText: '0分钟',
    weekDurationText: '0分钟',
    showGoalSetting: false,
    dailyGoalMinutes: 30,
    weeklyGoalMinutes: 210,
    goalType: 1,
    tempDailyGoal: 30,
    tempWeeklyGoal: 210,
    canvasSize: 160,
    streakDays: 0,
    maxStreakDays: 0
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadAllData()
    } else {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.loadAllData()
      }).catch(() => {
        this.setData({ isLogin: false })
      })
    }
  },

  loadAllData() {
    this.loadSummary()
    this.loadGoalProgress()
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

      const bookRank = (data.bookRank || []).map(item => ({
        ...item,
        totalDurationText: formatDuration(item.totalDuration || 0)
      }))

      const categoryStats = (data.categoryStats || []).map(item => ({
        ...item,
        totalDurationText: formatDuration(item.totalDuration || 0)
      }))

      this.setData({
        summary: { ...data, bookRank, categoryStats },
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

  async loadGoalProgress() {
    if (!this.data.isLogin) return
    try {
      const res = await request({
        url: '/reading/goal/progress'
      })
      const progress = res.data
      const todayDurationText = formatDuration(progress.todayDuration || 0)
      const weekDurationText = formatDuration(progress.weekDuration || 0)

      this.setData({
        goalProgress: progress,
        todayDurationText,
        weekDurationText,
        dailyGoalMinutes: progress.dailyGoalMinutes || 30,
        weeklyGoalMinutes: progress.weeklyGoalMinutes || 210,
        goalType: progress.goalType || 1,
        streakDays: progress.currentStreakDays || 0,
        maxStreakDays: progress.maxStreakDays || 0
      })

      this.drawProgressRing()
    } catch (e) {
      console.error('加载目标进度失败', e)
    }
  },

  drawProgressRing() {
    const progress = this.data.goalProgress
    if (!progress) return

    const size = this.data.canvasSize
    const ctx = wx.createCanvasContext('progressCanvas', this)
    const centerX = size / 2
    const centerY = size / 2
    const radius = (size - 20) / 2
    const lineWidth = 12

    ctx.setLineWidth(lineWidth)
    ctx.setLineCap('round')

    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
    ctx.setStrokeStyle('#f0f0f0')
    ctx.stroke()

    const dailyProgress = progress.dailyProgress || 0
    const startAngle = -Math.PI / 2
    const endAngle = startAngle + (dailyProgress / 100) * 2 * Math.PI

    const gradient = ctx.createLinearGradient(0, 0, size, size)
    gradient.addColorStop(0, '#6B4226')
    gradient.addColorStop(1, '#D4A574')

    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, startAngle, endAngle)
    ctx.setStrokeStyle(gradient)
    ctx.stroke()

    ctx.draw()
  },

  openGoalSetting() {
    this.setData({
      showGoalSetting: true,
      tempDailyGoal: this.data.dailyGoalMinutes,
      tempWeeklyGoal: this.data.weeklyGoalMinutes
    })
  },

  closeGoalSetting() {
    this.setData({ showGoalSetting: false })
  },

  onDailyGoalChange(e) {
    this.setData({ tempDailyGoal: Number(e.detail.value) })
  },

  onWeeklyGoalChange(e) {
    this.setData({ tempWeeklyGoal: Number(e.detail.value) })
  },

  onGoalTypeChange(e) {
    this.setData({ goalType: Number(e.currentTarget.dataset.type) })
  },

  async saveGoal() {
    try {
      const res = await request({
        url: '/reading/goal',
        method: 'PUT',
        data: {
          dailyGoalMinutes: this.data.tempDailyGoal,
          weeklyGoalMinutes: this.data.tempWeeklyGoal,
          goalType: this.data.goalType
        }
      })
      wx.showToast({ title: '目标已更新', icon: 'success' })
      this.setData({ showGoalSetting: false })
      this.loadGoalProgress()
    } catch (e) {
      wx.showToast({ title: '保存失败', icon: 'none' })
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
    })
  },

  openTimelineBook(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}` })
  },

  goMembership() {
    wx.navigateTo({ url: '/pages/membership/membership' })
  },

  goReport() {
    wx.navigateTo({ url: `/pages/reading-report/reading-report?period=${this.data.period}` })
  }
})
