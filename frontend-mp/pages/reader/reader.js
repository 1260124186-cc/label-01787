const { request } = require('../../utils/request')

const THEME_CONFIG = {
  white: { bgColor: '#FFFFFF', textColor: '#333333' },
  green: { bgColor: '#EBF9ED', textColor: '#2D4A2D' },
  dark:  { bgColor: '#1A1A2E', textColor: '#E0E0E0' }
}

Page({
  data: {
    book: {},
    bookId: null,
    toc: [],
    loadedPages: [],
    currentPage: 1,
    displayPage: 1,
    swiperIndex: 0,
    readMode: 'scroll', // scroll | swipe
    theme: 'white',
    themeClass: 'theme-white',
    bgColor: '#FFFFFF',
    showUI: false,
    showToc: false,
    showTheme: false,
    showNoteDialog: false,
    noteText: '',
    noteContent: '',
    noteType: 2,
    readingRecordId: null,
    loading: false,
    startPageNum: 1,
    _scrollTimer: null
  },

  onLoad(options) {
    const app = getApp()
    const theme = app.globalData.theme || 'white'
    this.setData({
      bookId: options.id,
      theme,
      themeClass: `theme-${theme}`,
      bgColor: THEME_CONFIG[theme].bgColor
    })
    this.applyPageBgColor(theme)
    this.loadBook()
    this.loadToc()
    this.startReading()
  },

  onUnload() {
    this.endReading()
  },

  onHide() {
    this.endReading()
  },

  async loadBook() {
    try {
      const res = await request({ url: `/books/${this.data.bookId}` })
      this.setData({ book: res.data })
      // 从上次阅读位置开始
      const startPage = Math.max(1, res.data.lastPage || 1)
      this.setData({ currentPage: startPage, displayPage: startPage })
      this.loadPageImages(startPage, Math.min(startPage + 2, res.data.pageCount))
    } catch (e) {
      console.error('加载书籍失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  async loadToc() {
    try {
      const res = await request({ url: `/books/${this.data.bookId}/toc` })
      this.setData({ toc: res.data || [] })
    } catch (e) {
      console.error('加载目录失败', e)
    }
  },

  loadPageImages(from, to) {
    const app = getApp()
    const pages = []
    for (let i = from; i <= to; i++) {
      pages.push(`${app.globalData.baseUrl}/books/${this.data.bookId}/page/${i}?token=${app.globalData.token}`)
    }
    // 记录起始页号（loadedPages 数组第0项对应的实际页码）
    const startPageNum = this.data.startPageNum || from
    this.setData({
      loadedPages: [...this.data.loadedPages, ...pages],
      currentPage: to,
      startPageNum
    })
  },

  loadMorePages() {
    // 防止重复加载
    if (this.data.loading) return
    
    const next = this.data.currentPage + 1
    if (next > this.data.book.pageCount) return
    
    const end = Math.min(next + 2, this.data.book.pageCount)
    this.setData({ loading: true })
    this.loadPageImages(next, end)
    this.setData({ loading: false })
  },

  onScroll(e) {
    // 节流：避免频繁查询节点
    if (this._scrollTimer) return
    this._scrollTimer = setTimeout(() => {
      this._scrollTimer = null
      this.calcCurrentPage()
    }, 200)
  },

  calcCurrentPage() {
    const query = this.createSelectorQuery()
    query.selectAll('.page-image-wrap').boundingClientRect()
    query.exec((res) => {
      if (!res || !res[0] || res[0].length === 0) return
      const rects = res[0]
      const screenMid = 300 // 大约屏幕中上部位置（rpx 转 px 后约 150px）
      let visibleIndex = 0
      for (let i = 0; i < rects.length; i++) {
        // 找到顶部在屏幕中部以上、底部在屏幕中部以下的图片
        if (rects[i].top <= screenMid && rects[i].bottom > 0) {
          visibleIndex = i
        }
      }
      const newPage = this.data.startPageNum + visibleIndex
      if (newPage !== this.data.displayPage) {
        this.setData({ displayPage: newPage })
      }
    })
  },

  onSwiperChange(e) {
    const idx = e.detail.current
    this.setData({ swiperIndex: idx, displayPage: idx + 1 })
    // 预加载：当滑到倒数第2页时加载更多
    if (idx >= this.data.loadedPages.length - 2 && this.data.currentPage < this.data.book.pageCount) {
      this.loadMorePages()
    }
  },

  async startReading() {
    try {
      const res = await request({
        url: '/reading/start',
        method: 'POST',
        data: { bookId: Number(this.data.bookId) }
      })
      this.setData({ readingRecordId: res.data.id })
    } catch (e) {
      console.error('记录阅读开始失败', e)
    }
  },

  async endReading() {
    if (!this.data.readingRecordId) return
    try {
      await request({
        url: '/reading/end',
        method: 'POST',
        data: {
          bookId: Number(this.data.bookId),
          recordId: this.data.readingRecordId,
          lastPage: this.data.displayPage
        }
      })
      // 更新阅读进度
      await request({
        url: `/books/${this.data.bookId}/progress`,
        method: 'PUT',
        data: { lastPage: this.data.displayPage }
      })
    } catch (e) {
      console.error('记录阅读结束失败', e)
    }
  },

  toggleUI() {
    this.setData({ showUI: !this.data.showUI })
  },

  toggleToc() {
    this.setData({ showToc: !this.data.showToc })
  },

  jumpToPage(e) {
    const page = e.currentTarget.dataset.page
    this.setData({ showToc: false })
    if (this.data.readMode === 'swipe') {
      // 确保页面已加载
      if (page > this.data.loadedPages.length) {
        this.setData({ loadedPages: [], startPageNum: page })
        this.loadPageImages(page, Math.min(page + 2, this.data.book.pageCount))
        this.setData({ swiperIndex: 0, displayPage: page })
      } else {
        this.setData({ swiperIndex: page - 1, displayPage: page })
      }
    } else {
      // 滚动模式 - 重新从目标页加载
      this.setData({ loadedPages: [], startPageNum: page })
      this.loadPageImages(page, Math.min(page + 2, this.data.book.pageCount))
      this.setData({ displayPage: page })
    }
  },

  switchMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({ readMode: mode })
  },

  showThemePicker() {
    this.setData({ showTheme: true })
  },

  hideThemePicker() {
    this.setData({ showTheme: false })
  },

  changeTheme(e) {
    const theme = e.currentTarget.dataset.theme
    const app = getApp()
    app.setTheme(theme)
    this.setData({
      theme,
      themeClass: `theme-${theme}`,
      bgColor: THEME_CONFIG[theme].bgColor,
      showTheme: false
    })
    this.applyPageBgColor(theme)
  },

  /** 设置页面级背景色，防止滚动到边界时露白 */
  applyPageBgColor(theme) {
    const color = THEME_CONFIG[theme].bgColor
    wx.setBackgroundColor({
      backgroundColor: color,
      backgroundColorTop: color,
      backgroundColorBottom: color
    })
  },

  goBack() {
    wx.navigateBack()
  },

  addNote() {
    this.setData({ showNoteDialog: true, noteText: '', noteContent: '', noteType: 2 })
  },

  hideNoteDialog() {
    this.setData({ showNoteDialog: false })
  },

  onNoteTextInput(e) {
    this.setData({ noteText: e.detail.value })
  },

  onNoteContentInput(e) {
    this.setData({ noteContent: e.detail.value })
  },

  setNoteType(e) {
    this.setData({ noteType: Number(e.currentTarget.dataset.type) })
  },

  async submitNote() {
    if (!this.data.noteContent.trim()) {
      wx.showToast({ title: '请输入内容', icon: 'none' })
      return
    }
    try {
      await request({
        url: '/annotations',
        method: 'POST',
        data: {
          bookId: Number(this.data.bookId),
          pageNum: this.data.displayPage,
          selectedText: this.data.noteText,
          content: this.data.noteContent,
          type: this.data.noteType
        }
      })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.setData({ showNoteDialog: false })
    } catch (e) {
      console.error('保存笔记失败', e)
    }
  }
})
