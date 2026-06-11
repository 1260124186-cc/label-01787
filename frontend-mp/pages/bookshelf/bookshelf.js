const { request, uploadFile } = require('../../utils/request')
const { formatSize, formatDuration } = require('../../utils/util')

Page({
  data: {
    books: [],
    categories: [],
    currentCategory: '',
    continueList: [],
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false,
    showCopyrightModal: false,
    pendingFilePath: '',
    pendingFileName: '',
    copyrightDeclared: false
  },

  pendingFilePath: '',
  pendingFileName: '',

  noop() {},

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadCategories()
      this.loadContinueReading()
      if (!this.data.showCopyrightModal) {
        this.refreshBooks()
      }
    } else {
      this.setData({ isLogin: false, books: [], continueList: [] })
    }
  },

  onPullDownRefresh() {
    this.refreshBooks().then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadBooks()
    }
  },

  async loadContinueReading() {
    if (!this.data.isLogin) return
    try {
      const res = await request({
        url: '/reading/continue-list',
        data: { limit: 5 }
      })
      const list = (res.data || []).map(item => {
        const format = item.bookFormat || 'pdf'
        let progress = 0
        if (format === 'epub') {
          const total = Math.max(1, item.chapterCount || 0)
          const last = item.lastChapter || 0
          progress = total > 0 ? Math.round((last / total) * 100) : 0
        } else {
          const total = Math.max(1, item.pageCount || 0)
          const last = item.lastPage || 0
          progress = total > 0 ? Math.round((last / total) * 100) : 0
        }
        return {
          ...item,
          progress,
          durationText: formatDuration(item.totalDuration)
        }
      })
      this.setData({ continueList: list })
    } catch (e) {
      console.error('加载继续阅读列表失败', e)
    }
  },

  openContinueBook(e) {
    const id = e.currentTarget.dataset.id
    const page = e.currentTarget.dataset.page || 1
    const chapter = e.currentTarget.dataset.chapter || 0
    const format = e.currentTarget.dataset.format || 'pdf'
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}&page=${page}&chapter=${chapter}&format=${format}` })
  },

  async refreshBooks() {
    if (!this.data.isLogin) return
    this.setData({ page: 1, hasMore: true, books: [] })
    await this.loadBooks()
  },

  async loadBooks() {
    if (this.data.loading || !this.data.isLogin) return
    this.setData({ loading: true })
    try {
      const params = { page: this.data.page, size: 20 }
      if (this.data.currentCategory) {
        params.categoryId = this.data.currentCategory
      }
      const res = await request({ url: '/books', data: params })
      const records = (res.data.records || []).map(b => ({
        ...b,
        fileSizeText: formatSize(b.fileSize)
      }))
      this.setData({
        books: this.data.page === 1 ? records : [...this.data.books, ...records],
        page: this.data.page + 1,
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载书籍失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadCategories() {
    if (!this.data.isLogin) return
    try {
      const res = await request({ url: '/categories' })
      this.setData({ categories: res.data || [] })
    } catch (e) {
      console.error('加载分类失败', e)
    }
  },

  filterCategory(e) {
    const id = e.currentTarget.dataset.id
    this.setData({ currentCategory: id || '' })
    this.refreshBooks()
  },

  goCategory() {
    wx.navigateTo({ url: '/pages/category/category' })
  },

  openBook(e) {
    const id = e.currentTarget.dataset.id
    const format = e.currentTarget.dataset.format || 'pdf'
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}&format=${format}` })
  },

  showBookAction(e) {
    const index = e.currentTarget.dataset.index
    const book = this.data.books[index]
    wx.showActionSheet({
      itemList: ['删除书籍'],
      success: async (res) => {
        if (res.tapIndex === 0) {
          wx.showModal({
            title: '确认删除',
            content: `确定删除《${book.title}》？`,
            success: async (modalRes) => {
              if (modalRes.confirm) {
                try {
                  await request({ url: `/books/${book.id}`, method: 'DELETE' })
                  wx.showToast({ title: '已删除', icon: 'success' })
                  this.refreshBooks()
                } catch (e) {
                  console.error('删除失败', e)
                }
              }
            }
          })
        }
      }
    })
  },

  uploadBook() {
    const app = getApp()
    if (!app.globalData.token) {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.doUpload()
      }).catch(() => {
        wx.showToast({ title: '请先登录', icon: 'none' })
      })
      return
    }
    this.doUpload()
  },

  doUpload() {
    const FORMAT_MAX_SIZE = {
      pdf: 150 * 1024 * 1024,
      epub: 100 * 1024 * 1024,
      mobi: 100 * 1024 * 1024,
      azw3: 100 * 1024 * 1024
    }
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf', 'epub', 'mobi', 'azw3'],
      success: (res) => {
        const file = res.tempFiles[0]
        if (!file || !file.path) {
          wx.showToast({ title: '文件信息不完整，请重新选择', icon: 'none' })
          return
        }
        if (file.size < 1 * 1024 * 1024) {
          wx.showToast({ title: '文件不能小于1MB', icon: 'none' })
          return
        }

        const fileName = file.name || ''
        const lowerName = fileName.toLowerCase()
        let format = 'pdf'
        if (lowerName.endsWith('.epub')) format = 'epub'
        else if (lowerName.endsWith('.mobi')) format = 'mobi'
        else if (lowerName.endsWith('.azw3')) format = 'azw3'

        const maxSize = FORMAT_MAX_SIZE[format] || FORMAT_MAX_SIZE.pdf
        const maxSizeMB = Math.round(maxSize / (1024 * 1024))
        if (file.size > maxSize) {
          wx.showToast({ title: `${format.toUpperCase()}文件不能超过${maxSizeMB}MB`, icon: 'none' })
          return
        }

        let finalFileName = fileName
        if (!finalFileName) {
          const pathParts = file.path.split('/')
          finalFileName = pathParts[pathParts.length - 1] || '未命名文件'
        }

        this.pendingFilePath = file.path
        this.pendingFileName = finalFileName

        this.setData({
          showCopyrightModal: true,
          pendingFilePath: file.path,
          pendingFileName: finalFileName,
          copyrightDeclared: false
        })
      }
    })
  },

  toggleCopyright() {
    if (!this.pendingFilePath || !this.pendingFileName) {
      wx.showToast({ title: '文件信息已丢失，请重新选择', icon: 'none' })
      this.setData({ showCopyrightModal: false, copyrightDeclared: false })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      return
    }
    this.setData({ copyrightDeclared: !this.data.copyrightDeclared })
  },

  confirmUpload() {
    const { copyrightDeclared } = this.data
    if (!this.pendingFilePath || !this.pendingFileName) {
      wx.showToast({ title: '文件信息已丢失，请重新选择', icon: 'none' })
      this.setData({ showCopyrightModal: false, copyrightDeclared: false })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      return
    }
    if (!copyrightDeclared) {
      wx.showToast({ title: '请先勾选版权声明', icon: 'none' })
      return
    }

    let fileName = this.pendingFileName
    if (!fileName) {
      const pathParts = this.pendingFilePath.split('/')
      fileName = pathParts[pathParts.length - 1] || '未命名文件'
    }

    this.setData({ showCopyrightModal: false })
    wx.showLoading({ title: '上传中...' })
    const title = fileName
      .replace(/\.pdf$/i, '')
      .replace(/\.epub$/i, '')
      .replace(/\.mobi$/i, '')
      .replace(/\.azw3$/i, '')
    uploadFile(this.pendingFilePath, { title, copyrightDeclared: copyrightDeclared ? '1' : '0' }).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '上传成功', icon: 'success' })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      this.setData({ pendingFilePath: '', pendingFileName: '', copyrightDeclared: false })
      this.refreshBooks()
    }).catch((err) => {
      console.error('上传失败', err)
      wx.hideLoading()
      wx.showToast({ title: err && err.message ? err.message : '上传失败', icon: 'none' })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      this.setData({ pendingFilePath: '', pendingFileName: '', copyrightDeclared: false })
    })
  },

  cancelUpload() {
    this.pendingFilePath = ''
    this.pendingFileName = ''
    this.setData({ showCopyrightModal: false, pendingFilePath: '', pendingFileName: '', copyrightDeclared: false })
  }
})
