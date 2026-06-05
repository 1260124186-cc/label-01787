const { request } = require('../../utils/request')

Page({
  data: {
    complainantName: '',
    complainantContact: '',
    selectedBookId: null,
    selectedBookTitle: '',
    bookTitle: '',
    reason: '',
    evidenceUrls: '',
    submitting: false,
    books: [],
    showBookPicker: false
  },

  onNameInput(e) {
    this.setData({ complainantName: e.detail.value })
  },

  onContactInput(e) {
    this.setData({ complainantContact: e.detail.value })
  },

  onBookTitleInput(e) {
    this.setData({ bookTitle: e.detail.value })
  },

  onReasonInput(e) {
    this.setData({ reason: e.detail.value })
  },

  onEvidenceInput(e) {
    this.setData({ evidenceUrls: e.detail.value })
  },

  async openBookPicker() {
    if (this.data.books.length === 0) {
      try {
        wx.showLoading({ title: '加载中...' })
        const res = await request({ url: '/books', data: { page: 1, size: 100 } })
        this.setData({ books: res.data.records || [] })
      } catch (e) {
        wx.showToast({ title: '加载失败', icon: 'none' })
        return
      } finally {
        wx.hideLoading()
      }
    }

    const books = this.data.books
    if (books.length === 0) {
      wx.showToast({ title: '您的书架暂无书籍', icon: 'none' })
      return
    }

    const bookTitles = books.map(b => `${b.title}${b.author ? ` - ${b.author}` : ''}`)
    wx.showActionSheet({
      itemList: bookTitles,
      success: (res) => {
        const book = books[res.tapIndex]
        this.setData({
          selectedBookId: book.id,
          selectedBookTitle: book.title,
          bookTitle: book.title
        })
      }
    })
  },

  clearSelectedBook() {
    this.setData({
      selectedBookId: null,
      selectedBookTitle: ''
    })
  },

  async submitComplaint() {
    const { complainantName, complainantContact, selectedBookId, bookTitle, reason, evidenceUrls } = this.data

    if (!complainantName.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    if (!complainantContact.trim()) {
      wx.showToast({ title: '请输入联系方式', icon: 'none' })
      return
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请输入申诉原因', icon: 'none' })
      return
    }
    if (!selectedBookId && !bookTitle.trim()) {
      wx.showToast({ title: '请选择书籍或填写书籍名称', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      const data = {
        complainantName: complainantName.trim(),
        complainantContact: complainantContact.trim(),
        reason: reason.trim(),
        evidenceUrls: evidenceUrls.trim()
      }
      if (selectedBookId) {
        data.bookId = selectedBookId
        data.bookTitle = selectedBookId ? this.data.selectedBookTitle : bookTitle.trim()
      } else {
        data.bookTitle = bookTitle.trim()
      }
      await request({
        url: '/compliance/complaint',
        method: 'POST',
        data
      })
      wx.showToast({ title: '申诉已提交', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (e) {
      console.error('提交申诉失败', e)
    } finally {
      this.setData({ submitting: false })
    }
  }
})
