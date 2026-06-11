const { request } = require('../../utils/request')

const EXPIRE_DAYS = 7

Page({
  data: {
    books: [],
    loading: true,
    empty: false,
    showRestoreConfirm: false,
    restoreBook: null
  },

  onShow() {
    this.loadTrash()
  },

  async loadTrash() {
    this.setData({ loading: true })
    try {
      const res = await request({ url: '/trash/books' })
      const books = (res.data || []).map(b => ({
        ...b,
        remainDays: this.calcRemainDays(b.deletedAt)
      }))
      this.setData({
        books,
        empty: books.length === 0,
        loading: false
      })
    } catch (e) {
      console.error('加载回收站失败', e)
      this.setData({ loading: false })
    }
  },

  calcRemainDays(deletedAt) {
    if (!deletedAt) return EXPIRE_DAYS
    const now = new Date()
    const deleted = new Date(deletedAt.replace(/-/g, '/'))
    const diff = EXPIRE_DAYS - Math.floor((now - deleted) / (1000 * 60 * 60 * 24))
    return Math.max(0, diff)
  },

  openRestore(e) {
    const { book } = e.currentTarget.dataset
    this.setData({ restoreBook: book, showRestoreConfirm: true })
  },

  closeRestore() {
    this.setData({ showRestoreConfirm: false, restoreBook: null })
  },

  async confirmRestore() {
    const { restoreBook } = this.data
    if (!restoreBook) return
    try {
      await request({
        url: `/books/${restoreBook.id}/restore`,
        method: 'POST'
      })
      wx.showToast({ title: '已恢复', icon: 'success' })
      this.closeRestore()
      this.loadTrash()
    } catch (e) {
      console.error('恢复失败', e)
      this.closeRestore()
    }
  },

  onBookTap(e) {
    const { book } = e.currentTarget.dataset
    wx.showModal({
      title: book.title,
      content: `剩余可恢复 ${book.remainDays} 天，是否恢复此书？`,
      confirmText: '恢复',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.setData({ restoreBook: book, showRestoreConfirm: true })
        }
      }
    })
  },

  backToShelf() {
    wx.navigateBack()
  }
})
