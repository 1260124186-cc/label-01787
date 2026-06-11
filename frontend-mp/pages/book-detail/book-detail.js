const { request } = require('../../utils/request')

Page({
  data: {
    bookId: null,
    book: null,
    loading: true,
    showActionSheet: false,
    showDeleteConfirm: false
  },

  onLoad(options) {
    const bookId = options.id
    if (!bookId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.setData({ bookId })
    this.loadDetail()
  },

  onShow() {
    if (this.data.bookId) {
      this.loadDetail()
    }
  },

  async loadDetail() {
    this.setData({ loading: true })
    try {
      const res = await request({ url: `/books/${this.data.bookId}` })
      this.setData({ book: res.data, loading: false })
    } catch (e) {
      console.error('加载书籍详情失败', e)
      this.setData({ loading: false })
    }
  },

  startRead() {
    const { book } = this.data
    if (!book) return
    wx.navigateTo({
      url: `/pages/reader/reader?id=${book.id}&format=${book.format}`
    })
  },

  editBook() {
    const { book } = this.data
    if (!book) return
    wx.navigateTo({
      url: `/pages/book-edit/book-edit?id=${book.id}`
    })
  },

  openActionSheet() {
    this.setData({ showActionSheet: true })
  },

  closeActionSheet() {
    this.setData({ showActionSheet: false })
  },

  onActionTap(e) {
    const { action } = e.currentTarget.dataset
    this.closeActionSheet()
    if (action === 'edit') {
      this.editBook()
    } else if (action === 'delete') {
      this.setData({ showDeleteConfirm: true })
    }
  },

  async confirmDelete() {
    this.setData({ showDeleteConfirm: false })
    try {
      await request({
        url: `/books/${this.data.bookId}`,
        method: 'DELETE'
      })
      wx.showToast({ title: '已移入回收站', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1000)
    } catch (e) {
      console.error('删除失败', e)
    }
  },

  cancelDelete() {
    this.setData({ showDeleteConfirm: false })
  },

  goExcerpts() {
    const { book } = this.data
    if (!book) return
    wx.navigateTo({
      url: `/pages/my-excerpts/my-excerpts?bookId=${book.id}`
    })
  },

  onShareAppMessage() {
    const { book } = this.data
    return {
      title: book ? `${book.title} - ${book.author || '未知作者'}` : '好书推荐',
      path: `/pages/bookshelf/bookshelf`
    }
  }
})
