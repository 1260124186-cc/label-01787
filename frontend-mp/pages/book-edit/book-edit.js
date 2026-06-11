const { request } = require('../../utils/request')

Page({
  data: {
    bookId: null,
    title: '',
    author: '',
    categoryId: null,
    categories: [],
    showCategoryPicker: false,
    categoryIndex: -1,
    saving: false,
    loading: true
  },

  onLoad(options) {
    const bookId = options.id
    if (!bookId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.setData({ bookId })
    this.init()
  },

  async init() {
    try {
      const [detailRes, catRes] = await Promise.all([
        request({ url: `/books/${this.data.bookId}` }),
        request({ url: '/categories' })
      ])
      const book = detailRes.data
      const categories = catRes.data || []

      let categoryIndex = -1
      if (book.categoryId) {
        categoryIndex = categories.findIndex(c => c.id === book.categoryId)
      }

      this.setData({
        title: book.title || '',
        author: book.author || '',
        categoryId: book.categoryId || null,
        categories,
        categoryIndex,
        loading: false
      })
    } catch (e) {
      console.error('初始化失败', e)
      this.setData({ loading: false })
    }
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value })
  },

  onAuthorInput(e) {
    this.setData({ author: e.detail.value })
  },

  openCategoryPicker() {
    this.setData({ showCategoryPicker: true })
  },

  closeCategoryPicker() {
    this.setData({ showCategoryPicker: false })
  },

  onCategoryPick(e) {
    const { id, index } = e.currentTarget.dataset
    this.setData({
      categoryId: id || null,
      categoryIndex: id ? index : -1,
      showCategoryPicker: false
    })
  },

  async save() {
    const { title, author, categoryId, bookId, saving } = this.data
    if (saving) return

    if (!title || !title.trim()) {
      wx.showToast({ title: '请输入书名', icon: 'none' })
      return
    }

    this.setData({ saving: true })
    try {
      await request({
        url: `/books/${bookId}`,
        method: 'PUT',
        data: {
          title: title.trim(),
          author: author.trim() || null,
          categoryId
        }
      })
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 800)
    } catch (e) {
      console.error('保存失败', e)
    } finally {
      this.setData({ saving: false })
    }
  }
})
