const { request } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    scope: 'all',
    bookId: null,
    bookTitle: '',
    results: [],
    totalResults: 0,
    loading: false,
    autoFocus: true,
    expandedBooks: {},
    page: 1,
    hasMore: true
  },

  onLoad(options) {
    const bookId = options.bookId
    const bookTitle = options.bookTitle
    const keyword = options.keyword || ''

    this.setData({
      bookId: bookId ? Number(bookId) : null,
      bookTitle: bookTitle || '',
      keyword,
      scope: bookId ? 'book' : 'all'
    })

    if (bookTitle) {
      wx.setNavigationBarTitle({ title: `搜索 - ${bookTitle}` })
    }

    if (keyword) {
      this.onSearch()
    }
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  clearKeyword() {
    this.setData({ keyword: '', results: [], totalResults: 0 })
  },

  changeScope(e) {
    const scope = e.currentTarget.dataset.scope
    this.setData({ scope, page: 1, hasMore: true, results: [] })
    if (this.data.keyword) {
      this.onSearch()
    }
  },

  async onSearch() {
    const { keyword, scope, bookId, page } = this.data
    if (!keyword || !keyword.trim()) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    try {
      const params = {
        keyword: keyword.trim(),
        scope,
        page,
        size: 20
      }
      if (scope === 'book' && bookId) {
        params.bookId = bookId
      }

      const res = await request({
        url: '/search',
        data: params
      })

      const results = res.data || []
      const totalResults = results.reduce((sum, r) => sum + (r.totalMatches || 0), 0)

      const expandedBooks = {}
      results.forEach(r => { expandedBooks[r.bookId] = true })

      this.setData({
        results,
        totalResults,
        expandedBooks,
        hasMore: results.length >= 20
      })
    } catch (e) {
      console.error('搜索失败', e)
      wx.showToast({ title: '搜索失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  toggleBook(e) {
    const bookId = e.currentTarget.dataset.bookid
    const expandedBooks = { ...this.data.expandedBooks }
    expandedBooks[bookId] = !expandedBooks[bookId]
    this.setData({ expandedBooks })
  },

  jumpToMatch(e) {
    const { bookid, page, start, end, format, keyword } = e.currentTarget.dataset

    const params = [
      `id=${bookid}`,
      `page=${page}`,
      `chapter=${format === 'epub' ? page - 1 : 0}`,
      `format=${format}`,
      `highlight=${encodeURIComponent(keyword)}`,
      `matchStart=${start}`,
      `matchEnd=${end}`
    ].join('&')

    wx.navigateTo({
      url: `/pages/reader/reader?${params}`
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && this.data.keyword) {
      this.setData({ page: this.data.page + 1 })
      this.onSearch()
    }
  }
})
