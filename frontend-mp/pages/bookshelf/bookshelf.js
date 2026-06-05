const { request, uploadFile } = require('../../utils/request')
const { formatSize } = require('../../utils/util')

Page({
  data: {
    books: [],
    categories: [],
    currentCategory: '',
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadCategories()
      this.refreshBooks()
    } else {
      this.setData({ isLogin: false, books: [] })
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
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}` })
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
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf'],
      success: (res) => {
        const file = res.tempFiles[0]
        // 检查文件大小 1MB - 150MB
        if (file.size < 1 * 1024 * 1024) {
          wx.showToast({ title: '文件不能小于1MB', icon: 'none' })
          return
        }
        if (file.size > 150 * 1024 * 1024) {
          wx.showToast({ title: '文件不能超过150MB', icon: 'none' })
          return
        }

        wx.showLoading({ title: '上传中...' })
        const title = file.name.replace('.pdf', '').replace('.PDF', '')
        uploadFile(file.path, { title }).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '上传成功', icon: 'success' })
          this.refreshBooks()
        }).catch(() => {
          wx.hideLoading()
        })
      }
    })
  }
})
