const { request } = require('../../utils/request')
const { formatDuration, formatDate, formatDateTime } = require('../../utils/util')

Page({
  data: {
    reportList: [],
    loading: false,
    page: 1,
    size: 10,
    hasMore: true,
    showDeleteDialog: false,
    deleteReportId: null,
    filterType: 'all'
  },

  onLoad() {
    const app = getApp()
    if (app.globalData.token) {
      this.loadReportList()
    } else {
      app.checkLogin().then(() => {
        this.loadReportList()
      })
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true, reportList: [] })
    this.loadReportList().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadReportList()
    }
  },

  async loadReportList() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const res = await request({
        url: '/reading/report/list',
        data: {
          page: this.data.page,
          size: this.data.size
        }
      })

      const list = (res.data || []).map(item => ({
        ...item,
        totalDurationText: formatDuration(item.totalDuration || 0),
        periodText: this.formatPeriod(item),
        typeLabel: this.getTypeLabel(item.reportType),
        typeIcon: this.getTypeIcon(item.reportType),
        createdText: formatDateTime(item.createdAt)
      }))

      const filteredList = this.filterType === 'all'
        ? list
        : list.filter(item => item.reportType === this.filterType)

      const newList = this.data.page === 1 ? filteredList : [...this.data.reportList, ...filteredList]

      this.setData({
        reportList: newList,
        hasMore: list.length >= this.data.size,
        page: this.data.page + 1
      })
    } catch (e) {
      console.error('加载报告列表失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  formatPeriod(report) {
    const start = report.periodStart || ''
    const end = report.periodEnd || ''
    if (start && end) {
      return `${start} ~ ${end}`
    }
    return ''
  },

  getTypeLabel(type) {
    const labels = {
      weekly: '周报',
      monthly: '月报',
      yearly: '年报'
    }
    return labels[type] || '报告'
  },

  getTypeIcon(type) {
    const icons = {
      weekly: '📅',
      monthly: '📆',
      yearly: '🗓️'
    }
    return icons[type] || '📊'
  },

  setFilterType(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ filterType: type, page: 1, reportList: [], hasMore: true })
    this.loadReportList()
  },

  openReport(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/reading-report/reading-report?id=${id}`
    })
  },

  showDeleteDialog(e) {
    e.stopPropagation && e.stopPropagation()
    const id = e.currentTarget.dataset.id
    this.setData({ showDeleteDialog: true, deleteReportId: id })
  },

  hideDeleteDialog() {
    this.setData({ showDeleteDialog: false, deleteReportId: null })
  },

  async confirmDelete() {
    const reportId = this.data.deleteReportId
    if (!reportId) return

    try {
      await request({
        url: `/reading/report/${reportId}`,
        method: 'DELETE'
      })

      const newList = this.data.reportList.filter(item => item.id !== reportId)
      this.setData({ reportList: newList })

      wx.showToast({ title: '已删除', icon: 'success' })
    } catch (e) {
      console.error('删除失败', e)
      wx.showToast({ title: '删除失败', icon: 'none' })
    }

    this.setData({ showDeleteDialog: false, deleteReportId: null })
  },

  async shareReport(e) {
    e.stopPropagation && e.stopPropagation()
    const id = e.currentTarget.dataset.id
    try {
      await request({
        url: `/reading/report/${id}/share`,
        method: 'POST'
      })
    } catch (e) {
      console.error('记录分享失败', e)
    }

    const report = this.data.reportList.find(item => item.id === id)
    if (report) {
      report.shareCount = (report.shareCount || 0) + 1
      this.setData({ reportList: this.data.reportList })
    }
  },

  onShareAppMessage(e) {
    const id = e.target && e.target.dataset && e.target.dataset.id
    const report = id ? this.data.reportList.find(item => item.id === id) : null

    if (report) {
      return {
        title: `我的${this.getTypeLabel(report.reportType)}：阅读${formatDuration(report.totalDuration || 0)}`,
        path: `/pages/reading-report/reading-report?id=${report.id}`
      }
    }

    return {
      title: '我的阅读报告',
      path: '/pages/summary/summary'
    }
  },

  goGenerateReport() {
    wx.navigateTo({
      url: '/pages/reading-report/reading-report?period=week'
    })
  }
})
