const { request } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    activeTab: 'export',
    tasks: [],
    importResult: {
      categoryImported: 0,
      annotationImported: 0,
      recordImported: 0,
      categorySkipped: 0,
      annotationSkipped: 0,
      recordSkipped: 0,
      skippedTotal: 0,
      booksToLink: [],
      warnings: []
    },
    showResult: false,
    isLogin: false,
    loading: false,
    currentTask: null,
    refreshing: false
  },

  pollingTimer: null,

  onShow() {
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

  startProgressPolling() {
    var that = this
    this.stopProgressPolling()
    this.pollingTimer = setInterval(function() {
      var processing = that.data.tasks.some(function(t) {
        return t.status === 0 || t.status === 1
      })
      if (processing) {
        that.loadTasks(true)
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
    var tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    if (tab === 'tasks') {
      this.loadTasks()
    }
  },

  loadTasks: function(silent) {
    var that = this
    if (silent === undefined) silent = false
    if (!silent) {
      this.setData({ loading: true })
    }
    request({
      url: '/backup/tasks',
      data: { page: 1, size: 20 }
    }).then(function(res) {
      var records = (res.data && res.data.records) ? res.data.records : []
      var tasks = records.map(function(task) {
        return {
          id: task.id,
          taskType: task.taskType,
          taskTypeText: task.taskType === 1 ? '导出' : '导入',
          status: task.status,
          statusText: that.getStatusText(task.status),
          progress: task.progress || 0,
          fileName: task.fileName,
          fileSize: task.fileSize,
          fileSizeText: that.formatSize(task.fileSize),
          bookCount: task.bookCount,
          annotationCount: task.annotationCount,
          recordCount: task.recordCount,
          categoryCount: task.categoryCount,
          errorMessage: task.errorMessage,
          createdAt: that.formatDate(task.createdAt)
        }
      })
      that.setData({ tasks: tasks })
    }).catch(function(e) {
      console.error('加载任务列表失败', e)
      if (!silent) {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }).finally(function() {
      that.setData({ loading: false, refreshing: false })
    })
  },

  doExport() {
    var that = this
    wx.showModal({
      title: '确认导出',
      content: '将导出您的所有数据：\n• PDF原文件\n• 批注笔记\n• 阅读记录\n• 分类结构\n\n导出文件将保留7天，建议定期备份。',
      confirmText: '开始导出',
      success: function(res) {
        if (res.confirm) {
          that.createExportTask()
        }
      }
    })
  },

  createExportTask: function() {
    var that = this
    wx.showLoading({ title: '创建任务...' })
    request({
      url: '/backup/export',
      method: 'POST'
    }).then(function(res) {
      wx.hideLoading()
      wx.showToast({ title: '导出任务已创建', icon: 'success' })
      that.setData({ activeTab: 'tasks' })
      that.loadTasks()
    }).catch(function(e) {
      wx.hideLoading()
      console.error('创建导出任务失败', e)
      wx.showToast({ title: '创建失败', icon: 'none' })
    })
  },

  chooseFile() {
    var that = this
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['zip', 'json'],
      success: function(res) {
        var file = res.tempFiles[0]
        that.uploadImportFile(file)
      }
    })
  },

  uploadImportFile: function(file) {
    var that = this
    wx.showLoading({ title: '上传中...' })
    wx.uploadFile({
      url: app.globalData.baseUrl + '/backup/import',
      filePath: file.path,
      name: 'file',
      header: {
        'Authorization': 'Bearer ' + app.globalData.token
      },
      success: function(res) {
        try {
          var data = JSON.parse(res.data)
          if (data.code === 200) {
            wx.hideLoading()
            wx.showToast({ title: '导入任务已创建', icon: 'success' })
            that.setData({ activeTab: 'tasks' })
            that.loadTasks()
          } else {
            wx.hideLoading()
            wx.showToast({ title: data.message || '上传失败', icon: 'none' })
          }
        } catch (e) {
          wx.hideLoading()
          wx.showToast({ title: '上传失败', icon: 'none' })
        }
      },
      fail: function() {
        wx.hideLoading()
        wx.showToast({ title: '上传失败', icon: 'none' })
      }
    })
  },

  downloadTask: function(e) {
    var that = this
    var task = e.currentTarget.dataset.task
    if (task.status !== 2) {
      wx.showToast({ title: '任务未完成', icon: 'none' })
      return
    }
    wx.showLoading({ title: '准备下载...' })
    wx.downloadFile({
      url: app.globalData.baseUrl + '/backup/tasks/' + task.id + '/download',
      header: {
        'Authorization': 'Bearer ' + app.globalData.token
      },
      success: function(res) {
        wx.hideLoading()
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            success: function() {
              wx.showToast({ title: '下载完成', icon: 'success' })
            },
            fail: function() {
              wx.saveFile({
                tempFilePath: res.tempFilePath,
                success: function(saveRes) {
                  wx.showToast({ title: '已保存到' + saveRes.savedFilePath, icon: 'success' })
                }
              })
            }
          })
        } else {
          wx.showToast({ title: '下载失败', icon: 'none' })
        }
      },
      fail: function() {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
    })
  },

  viewImportResult: function(e) {
    var that = this
    var task = e.currentTarget.dataset.task
    if (task.taskType !== 2) return
    wx.showLoading({ title: '加载中...' })
    request({
      url: '/backup/tasks/' + task.id + '/import-result'
    }).then(function(res) {
      var result = res.data || {
        categoryImported: 0,
        annotationImported: 0,
        recordImported: 0,
        categorySkipped: 0,
        annotationSkipped: 0,
        recordSkipped: 0,
        booksToLink: [],
        warnings: []
      }
      result.skippedTotal = (result.categorySkipped || 0)
        + (result.annotationSkipped || 0)
        + (result.recordSkipped || 0)
      that.setData({
        importResult: result,
        showResult: true,
        currentTask: task
      })
      wx.hideLoading()
    }).catch(function(e) {
      wx.hideLoading()
      console.error('获取导入结果失败', e)
    })
  },

  closeResult() {
    this.setData({
      showResult: false,
      importResult: {
        categoryImported: 0,
        annotationImported: 0,
        recordImported: 0,
        categorySkipped: 0,
        annotationSkipped: 0,
        recordSkipped: 0,
        skippedTotal: 0,
        booksToLink: [],
        warnings: []
      }
    })
  },

  deleteTask: function(e) {
    var that = this
    var task = e.currentTarget.dataset.task
    wx.showModal({
      title: '确认删除',
      content: '确定删除此任务记录吗？',
      success: function(res) {
        if (res.confirm) {
          request({
            url: '/backup/tasks/' + task.id,
            method: 'DELETE'
          }).then(function() {
            wx.showToast({ title: '已删除', icon: 'success' })
            that.loadTasks()
          }).catch(function(e) {
            console.error('删除失败', e)
          })
        }
      }
    })
  },

  onRefresh() {
    this.setData({ refreshing: true })
    this.loadTasks()
  },

  doLogin() {
    var that = this
    wx.showLoading({ title: '登录中...' })
    app.login().then(function() {
      wx.hideLoading()
      that.setData({ isLogin: true })
      that.loadTasks()
    }).catch(function() {
      wx.hideLoading()
      wx.showToast({ title: '登录失败', icon: 'none' })
    })
  },

  getStatusText: function(status) {
    switch (status) {
      case 0: return '待处理'
      case 1: return '处理中'
      case 2: return '已完成'
      case 3: return '失败'
      default: return '未知'
    }
  },

  formatSize: function(size) {
    if (!size) return '0 B'
    if (size < 1024) return size + ' B'
    if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
    if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
    return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  },

  formatDate: function(dateStr) {
    if (!dateStr) return ''
    var date = new Date(dateStr)
    var month = String(date.getMonth() + 1).padStart(2, '0')
    var day = String(date.getDate()).padStart(2, '0')
    var hour = String(date.getHours()).padStart(2, '0')
    var minute = String(date.getMinutes()).padStart(2, '0')
    return month + '-' + day + ' ' + hour + ':' + minute
  }
})
