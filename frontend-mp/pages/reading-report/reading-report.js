const { request } = require('../../utils/request')
const { formatDuration, formatDate } = require('../../utils/util')

Page({
  data: {
    reportId: null,
    period: 'week',
    report: null,
    loading: false,
    generating: false,
    isVip: false,
    showShareModal: false,
    posterImage: '',
    generatingPoster: false,
    dailyData: [],
    bookRank: [],
    showDeleteConfirm: false
  },

  onLoad(options) {
    const period = options.period || 'week'
    const reportId = options.id ? Number(options.id) : null
    this.setData({ period, reportId })

    const app = getApp()
    if (app.globalData.token) {
      this.loadData()
    } else {
      app.checkLogin().then(() => {
        this.loadData()
      })
    }
  },

  onShow() {
    if (this.data.reportId) {
      this.loadReportDetail()
    }
  },

  loadData() {
    if (this.data.reportId) {
      this.loadReportDetail()
    } else {
      this.generateReport()
    }
  },

  async generateReport() {
    this.setData({ generating: true })
    try {
      const res = await request({
        url: '/reading/report',
        data: { period: this.data.period }
      })
      this.processReportData(res.data)
    } catch (e) {
      console.error('生成报告失败', e)
      if (e.message && e.message.includes('会员')) {
        wx.showModal({
          title: '会员专属',
          content: e.message,
          confirmText: '升级会员',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({ url: '/pages/membership/membership' })
            } else {
              wx.navigateBack()
            }
          }
        })
      } else {
        wx.showToast({ title: '生成报告失败', icon: 'none' })
      }
    } finally {
      this.setData({ generating: false })
    }
  },

  async loadReportDetail() {
    if (!this.data.reportId) return
    this.setData({ loading: true })
    try {
      const res = await request({
        url: `/reading/report/${this.data.reportId}`
      })
      this.processReportData(res.data)
    } catch (e) {
      console.error('加载报告详情失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  processReportData(data) {
    const daily = data.dailyData || []
    const maxVal = Math.max(...daily.map(d => Number(d.total) || 0), 1)
    const dailyData = daily.map(d => {
      const total = Number(d.total) || 0
      const minutes = Math.round(total / 60)
      const height = Math.max(Math.round((total / maxVal) * 200), 8)
      const dateStr = String(d.date || '')
      const label = dateStr.length >= 10 ? dateStr.substring(5, 10) : dateStr
      return { date: dateStr, minutes, height, label }
    })

    const bookRank = (data.bookRank || []).map(item => ({
      ...item,
      totalDurationText: formatDuration(item.totalDuration || 0)
    }))

    const categoryStats = (data.categoryStats || []).map(item => ({
      ...item,
      totalDurationText: formatDuration(item.totalDuration || 0)
    }))

    this.setData({
      report: {
        ...data,
        bookRank,
        categoryStats,
        totalDurationText: formatDuration(data.totalDuration || 0)
      },
      dailyData,
      isVip: data.isVip || false
    })

    wx.setNavigationBarTitle({
      title: this.getReportTitle(data.reportType)
    })
  },

  getReportTitle(type) {
    const titles = {
      weekly: '周报',
      monthly: '月报',
      yearly: '年报'
    }
    return '阅读' + (titles[type] || '报告')
  },

  goMembership() {
    wx.navigateTo({ url: '/pages/membership/membership' })
  },

  showSharePoster() {
    this.setData({ showShareModal: true })
    this.generatePoster()
  },

  closeShareModal() {
    this.setData({ showShareModal: false })
  },

  async generatePoster() {
    this.setData({ generatingPoster: true })
    try {
      const posterUrl = await this.createPosterImage()
      this.setData({ posterImage: posterUrl })

      if (this.data.report && this.data.report.id) {
        request({
          url: `/reading/report/${this.data.report.id}/share`,
          method: 'POST'
        }).catch(() => {})
      }
    } catch (e) {
      console.error('生成海报失败', e)
      wx.showToast({ title: '生成海报失败', icon: 'none' })
    } finally {
      this.setData({ generatingPoster: false })
    }
  },

  createPosterImage() {
    return new Promise((resolve, reject) => {
      const query = wx.createSelectorQuery()
      query.select('#posterCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (!res || !res[0] || !res[0].node) {
            this.createPosterFallback().then(resolve).catch(reject)
            return
          }

          const canvas = res[0].node
          const ctx = canvas.getContext('2d')
          const dpr = wx.getSystemInfoSync().pixelRatio
          const width = 300
          const height = 500
          canvas.width = width * dpr
          canvas.height = height * dpr
          ctx.scale(dpr, dpr)

          this.drawPoster(ctx, width, height)

          setTimeout(() => {
            wx.canvasToTempFilePath({
              canvas: canvas,
              success: (res) => resolve(res.tempFilePath),
              fail: reject
            })
          }, 100)
        })
    })
  },

  createPosterFallback() {
    return new Promise((resolve, reject) => {
      const ctx = wx.createCanvasContext('posterCanvasFallback', this)
      const width = 300
      const height = 500

      this.drawPoster(ctx, width, height)

      ctx.draw(false, () => {
        setTimeout(() => {
          wx.canvasToTempFilePath({
            canvasId: 'posterCanvasFallback',
            success: (res) => resolve(res.tempFilePath),
            fail: reject
          }, this)
        }, 200)
      })
    })
  },

  drawPoster(ctx, width, height) {
    const report = this.data.report || {}
    const periodText = `${report.periodStart || ''} ~ ${report.periodEnd || ''}`
    const reportTypeText = this.getReportTitle(report.reportType)

    ctx.fillStyle = '#F5F0EB'
    ctx.fillRect(0, 0, width, height)

    ctx.fillStyle = '#6B4226'
    ctx.fillRect(0, 0, width, 120)

    ctx.fillStyle = '#fff'
    ctx.font = 'bold 20px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(reportTypeText, width / 2, 35)

    ctx.font = '12px sans-serif'
    ctx.fillStyle = 'rgba(255,255,255,0.8)'
    ctx.fillText(periodText, width / 2, 55)

    ctx.font = 'bold 32px sans-serif'
    ctx.fillStyle = '#fff'
    ctx.fillText(report.totalDurationText || '0分钟', width / 2, 95)

    ctx.font = '12px sans-serif'
    ctx.fillStyle = 'rgba(255,255,255,0.7)'
    ctx.fillText('总阅读时长', width / 2, 112)

    const cardY = 140
    const cardPadding = 15

    ctx.fillStyle = '#fff'
    this.roundRect(ctx, 15, cardY, width - 30, 120, 10)
    ctx.fill()

    ctx.fillStyle = '#333'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'left'
    ctx.fillText('阅读数据', cardPadding + 15, cardY + 25)

    const statY = cardY + 50
    const statWidth = (width - 30) / 3

    ctx.fillStyle = '#6B4226'
    ctx.font = 'bold 20px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(String(report.readingDays || 0), statWidth / 2 + 15, statY)
    ctx.fillStyle = '#999'
    ctx.font = '11px sans-serif'
    ctx.fillText('阅读天数', statWidth / 2 + 15, statY + 18)

    ctx.fillStyle = '#6B4226'
    ctx.font = 'bold 20px sans-serif'
    ctx.fillText(String(report.bookCount || 0), statWidth * 1.5 + 15, statY)
    ctx.fillStyle = '#999'
    ctx.font = '11px sans-serif'
    ctx.fillText('阅读书籍', statWidth * 1.5 + 15, statY + 18)

    ctx.fillStyle = '#6B4226'
    ctx.font = 'bold 20px sans-serif'
    ctx.fillText(String(report.annotationCount || 0), statWidth * 2.5 + 15, statY)
    ctx.fillStyle = '#999'
    ctx.font = '11px sans-serif'
    ctx.fillText('批注数', statWidth * 2.5 + 15, statY + 18)

    ctx.fillStyle = '#fff'
    this.roundRect(ctx, 15, cardY + 135, width - 30, 100, 10)
    ctx.fill()

    ctx.fillStyle = '#333'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'left'
    ctx.fillText('连续阅读', cardPadding + 15, cardY + 158)

    ctx.fillStyle = '#E8734A'
    ctx.font = 'bold 28px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText((report.currentStreakDays || 0) + '天', width / 2, cardY + 195)
    ctx.fillStyle = '#999'
    ctx.font = '11px sans-serif'
    ctx.fillText('最长连续 ' + (report.maxStreakDays || 0) + ' 天', width / 2, cardY + 215)

    if (report.compareData) {
      ctx.fillStyle = '#fff'
      this.roundRect(ctx, 15, cardY + 250, width - 30, 80, 10)
      ctx.fill()

      ctx.fillStyle = '#333'
      ctx.font = 'bold 14px sans-serif'
      ctx.textAlign = 'left'
      ctx.fillText('较上期', cardPadding + 15, cardY + 273)

      const changeText = report.compareData.totalDurationChangeText || '持平'
      const isUp = changeText.indexOf('↑') >= 0
      ctx.fillStyle = isUp ? '#E8734A' : '#666'
      ctx.font = '14px sans-serif'
      ctx.textAlign = 'right'
      ctx.fillText(changeText, width - 30, cardY + 273)
    }

    ctx.fillStyle = '#999'
    ctx.font = '10px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('小安的书店 · 阅读记录', width / 2, height - 20)
  },

  roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath()
    ctx.moveTo(x + r, y)
    ctx.lineTo(x + w - r, y)
    ctx.quadraticCurveTo(x + w, y, x + w, y + r)
    ctx.lineTo(x + w, y + h - r)
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
    ctx.lineTo(x + r, y + h)
    ctx.quadraticCurveTo(x, y + h, x, y + h - r)
    ctx.lineTo(x, y + r)
    ctx.quadraticCurveTo(x, y, x + r, y)
    ctx.closePath()
  },

  savePoster() {
    if (!this.data.posterImage) {
      wx.showToast({ title: '海报生成中...', icon: 'none' })
      return
    }

    wx.saveImageToPhotosAlbum({
      filePath: this.data.posterImage,
      success: () => {
        wx.showToast({ title: '已保存到相册', icon: 'success' })
      },
      fail: (err) => {
        if (err.errMsg.indexOf('auth deny') >= 0) {
          wx.showModal({
            title: '提示',
            content: '需要您授权保存图片到相册',
            confirmText: '去授权',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting()
              }
            }
          })
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' })
        }
      }
    })
  },

  onShareAppMessage() {
    const report = this.data.report || {}
    const title = `我的${this.getReportTitle(report.reportType)}：阅读${report.totalDurationText || '0分钟'}`
    return {
      title: title,
      path: `/pages/reading-report/reading-report?id=${report.id}`,
      imageUrl: this.data.posterImage || ''
    }
  },

  showDeleteDialog() {
    this.setData({ showDeleteConfirm: true })
  },

  hideDeleteDialog() {
    this.setData({ showDeleteConfirm: false })
  },

  async confirmDelete() {
    if (!this.data.report || !this.data.report.id) return

    try {
      await request({
        url: `/reading/report/${this.data.report.id}`,
        method: 'DELETE'
      })
      wx.showToast({ title: '已删除', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1000)
    } catch (e) {
      console.error('删除失败', e)
      wx.showToast({ title: '删除失败', icon: 'none' })
    }
    this.setData({ showDeleteConfirm: false })
  },

  goHistory() {
    wx.navigateTo({ url: '/pages/reading-history-report/reading-history-report' })
  }
})
