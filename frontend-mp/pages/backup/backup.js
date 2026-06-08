const { request } = require('../../utils/request')

Page({
  data: {
    activeTab: 'export',
    tasks: [],
    importResult: null,
    showResult: false,
    isLogin: false,
    loading: false,
    currentTask: null,
    refreshing: false
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadTasks()
    } else {
      this.setData({ isLogin: false })
    }
    this.startProgressPolling()
  },

  onHide() {
    this.stopProgressPolling()
  },

  onUnload() {
    this.stopProgressPolling()
  },

  pollingTimer: null,

  startProgressPolling() {
    this.stopProgressPolling()
    this.pollingTimer = setInterval(() => {
      const processing = this.data.tasks.some(t => t.status === 0 || t.status === 1)
      if (processing) {
        this.loadTasks(true)
      }
    }, 3000)
  },

  stopProgressPolling() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer)
      this.pollingTimer = null
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    if (tab === 'tasks') {
      this.loadTasks()
    }
  },

  async loadTasks(silent = false) {
    if (!silent) {
      this.setData({ loading: true })
    }
    try {
      const res = await request({
        url: '/backup/tasks',
        data: { page: 1, size: 20 }
      })
      const tasks = (res.data?.records || []).map(task => ({
        ...task,
        statusText: this.getStatusText(task.status),
        taskTypeText: task.taskType === 1 ? '导出' : '导入',
        fileSizeText: this.formatSize(task.fileSize),
        progress: task.progress || 0,
        createdAt: this.formatDate(task.createdAt)
      }))
      this.setData({ tasks })
    } catch (e) {
      console.error('加载任务列表失败', e)
      if (!silent) {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    } finally {
      this.setData({ loading: false, refreshing: false })
    }
  },

  async doExport() {
    wx.showModal({
      title: '确认导出',
      content: '将导出您的所有数据：\n• PDF原文件\n• 批注笔记\n• 阅读记录\n• 分类结构\n\n导出文件将保留7天，建议定期备份。',
      confirmText: '开始导出',
      success: async (res) => {
        if (res.confirm) {
          this.createExportTask()
        }
      }
    })
  },

  async createExportTask() {
    wx.showLoading({ title: '创建任务...' })
    try {
      const res = await request({
        url: '/backup/export',
        method: 'POST'
      })
      wx.hideLoading()
      wx.showToast({ title: '导出任务已创建', icon: 'success' })
      this.setData({ activeTab: 'tasks' })
      this.loadTasks()
    } catch (e) {
      wx.hideLoading()
      console.error('创建导出任务失败', e)
      wx.showToast({ title: '创建失败', icon: 'none' })
    }
  },

  chooseFile() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['zip', 'json'],
      success: async (res) => {
        const file = res.tempFiles[0]
        this.uploadImportFile(file)
      }
    })
  },

  async uploadImportFile(file) {
    wx.showLoading({ title: '上传中...' })
    try {
      const uploadRes = await new Promise((resolve, reject) => {
        const app = getApp()
        wx.uploadFile({
          url: app.globalData.baseUrl + '/backup/import',
          filePath: file.path,
          name: 'file',
          header: {
            'Authorization': 'Bearer ' + app.globalData.token
          },
          success: (res) => {
            try {
              const data = JSON.parse(res.data)
              if (data.code === 0) {
                resolve(data)
              } else {
                reject(new Error(data.message || '上传失败'))
              }
            } catch (e) {
              reject(e)
            }
          },
          fail: reject
        })
      })
      wx.hideLoading()
      wx.showToast({ title: '导入任务已创建', icon: 'success' })
      this.setData({ activeTab: 'tasks' })
      this.loadTasks()
    } catch (e) {
      wx.hideLoading()
      console.error('上传导入文件失败', e)
      wx.showToast({ title: '上传失败', icon: 'none' })
    }
  },

  async downloadTask(e) {
    const task = e.currentTarget.dataset.task
    if (task.status !== 2) {
      wx.showToast({ title: '任务未完成', icon: 'none' })
      return
    }
    wx.showLoading({ title: '准备下载...' })
    try {
      const app = getApp()
      wx.downloadFile({
        url: app.globalData.baseUrl + '/backup/tasks/' + task.id + '/download',
        header: {
          'Authorization': 'Bearer ' + app.globalData.token
        },
        success: (res) => {
          wx.hideLoading()
          if (res.statusCode === 200) {
            wx.openDocument({
              filePath: res.tempFilePath,
              showMenu: true,
              success: () => {
                wx.showToast({ title: '下载完成', icon: 'success' })
              },
              fail: () => {
                wx.saveFile({
                  tempFilePath: res.tempFilePath,
                  success: (saveRes) => {
                    wx.showToast({ title: '已保存到' + saveRes.savedFilePath, icon: 'success' })
                  }
                })
              }
            })
          } else {
            wx.showToast({ title: '下载失败', icon: 'none' })
          }
        },
        fail: () => {
          wx.hideLoading()
          wx.showToast({ title: '下载失败', icon: 'none' })
        }
      })
    } catch (e) {
      wx.hideLoading()
      console.error('下载失败', e)
    }
  },

  async viewImportResult(e) {
    const task = e.currentTarget.dataset.task
    if (task.taskType !== 2) return
    wx.showLoading({ title: '加载中...' })
    try {
      const res = await request({
        url: '/backup/tasks/' + task.id + '/import-result'
      })
      this.setData({
        importResult: res.data,
        showResult: true,
        currentTask: task
      })
      wx.hideLoading()
    } catch (e) {
      wx.hideLoading()
      console.error('获取导入结果失败', e)
    }
  },

  closeResult() {
    this.setData({ showResult: false, importResult: null })
  },

  async deleteTask(e) {
    const task = e.currentTarget.dataset.task
    wx.showModal({
      title: '确认删除',
      content: '确定删除此任务记录吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: '/backup/tasks/' + task.id,
              method: 'DELETE'
            })
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadTasks()
          } catch (e) {
            console.error('删除失败', e)
          }
        }
      }
    })
  },

  onRefresh() {
    this.setData({ refreshing: true })
    this.loadTasks()
  },

  doLogin() {
    const app = getApp()
    wx.showLoading({ title: '登录中...' })
    app.login().then(() => {
      wx.hideLoading()
      this.setData({ isLogin: true })
      this.loadTasks()
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '登录失败', icon: 'none' })
    })
  },

  getStatusText(status) {
    switch (status) {
      case 0: return '待处理'
      case 1: return '处理中'
      case 2: return '已完成'
      case 3: return '失败'
      default: return '未知'
    }
  },

  formatSize(size) {
    if (!size) return '0 B'
    if (size < 1024) return size + ' B'
    if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
    if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
    return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hour}:${minute}`
  }
})
