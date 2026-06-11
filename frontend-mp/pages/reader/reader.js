const { request } = require('../../utils/request')
const { formatSize } = require('../../utils/util')

const THEME_CONFIG = {
  white: { bgColor: '#FFFFFF', textColor: '#333333' },
  green: { bgColor: '#EBF9ED', textColor: '#2D4A2D' },
  dark:  { bgColor: '#1A1A2E', textColor: '#E0E0E0' }
}

const STREAM_IMAGE = 'image'
const STREAM_HTML = 'html'
const STREAM_UNSUPPORTED = 'unsupported'

Page({
  data: {
    book: {},
    bookId: null,
    toc: [],
    format: 'pdf',
    streamType: STREAM_IMAGE,
    loadedPages: [],
    loadedChapters: [],
    chapterTitleMap: {},
    totalUnits: 0,
    currentUnit: 1,
    displayUnit: 1,
    swiperIndex: 0,
    readMode: 'scroll',
    theme: 'white',
    themeClass: 'theme-white',
    bgColor: '#FFFFFF',
    showUI: false,
    showToc: false,
    showBookmark: false,
    showTheme: false,
    showNoteDialog: false,
    showBookmarkDialog: false,
    bookmarkEditMode: false,
    editingBookmarkId: null,
    bookmarkTitle: '',
    bookmarkRemark: '',
    bookmarkIsChapter: false,
    bookmarks: [],
    chapterBookmarks: [],
    currentBookmarked: false,
    currentBookmarkId: null,
    tocActiveTab: 'toc',
    noteText: '',
    noteContent: '',
    noteType: 2,
    readingRecordId: null,
    loading: false,
    startUnitNum: 1,
    _scrollTimer: null,
    _chapterHtmlCache: {},
    _longPressTimer: null
  },

  onLoad(options) {
    const app = getApp()
    const theme = app.globalData.theme || 'white'
    const startPage = Number(options.page) || 0
    const startChapter = Number(options.chapter) || 0
    const format = options.format || 'pdf'
    this.setData({
      bookId: options.id,
      format,
      startPageNum: startPage,
      startChapterNum: startChapter,
      theme,
      themeClass: `theme-${theme}`,
      bgColor: THEME_CONFIG[theme].bgColor
    })
    this.applyPageBgColor(theme)
    this.initReader()
  },

  async initReader() {
    await this.loadBook()
    await this.determineStreamType()
    await this.loadToc()
    await this.loadBookmarks()
    await this.loadStartContent()
    this.startReading()
  },

  async loadBookmarks() {
    try {
      const [bookmarksRes, chapterRes] = await Promise.all([
        request({ url: `/bookmarks/book/${this.data.bookId}?isChapter=0` }),
        request({ url: `/bookmarks/book/${this.data.bookId}?isChapter=1` })
      ])
      const bookmarks = bookmarksRes.data || []
      const chapterBookmarks = chapterRes.data || []
      this.setData({ bookmarks, chapterBookmarks })
      this.checkCurrentBookmark()
      if (this.data.toc.length === 0 && this.data.streamType === STREAM_IMAGE) {
        this.mergeTocWithChapterBookmarks()
      }
    } catch (e) {
      console.error('加载书签失败', e)
    }
  },

  mergeTocWithChapterBookmarks() {
    const merged = this.data.chapterBookmarks.map(bm => ({
      title: bm.title || `第${bm.pageNum}页`,
      page: bm.pageNum,
      isBookmark: true,
      bookmarkId: bm.id
    }))
    this.setData({ toc: merged })
  },

  async checkCurrentBookmark() {
    try {
      const res = await request({
        url: `/bookmarks/check?bookId=${this.data.bookId}&pageNum=${this.data.displayUnit}`
      })
      const bm = res.data
      this.setData({
        currentBookmarked: !!bm,
        currentBookmarkId: bm ? bm.id : null
      })
    } catch (e) {
      this.setData({ currentBookmarked: false, currentBookmarkId: null })
    }
  },

  async determineStreamType() {
    const format = this.data.book.bookFormat || this.data.format
    try {
      const res = await request({ url: `/books/${this.data.bookId}/stream-type` })
      let streamType = res.data || STREAM_IMAGE
      if (format === 'mobi' || format === 'azw3') {
        streamType = STREAM_UNSUPPORTED
      }
      this.setData({ streamType })
    } catch (e) {
      let streamType = STREAM_IMAGE
      if (format === 'epub') streamType = STREAM_HTML
      else if (format === 'mobi' || format === 'azw3') streamType = STREAM_UNSUPPORTED
      this.setData({ streamType })
    }
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
      const book = res.data
      const format = book.bookFormat || this.data.format
      const totalUnits = format === 'epub' ? (book.chapterCount || 0) : (book.pageCount || 0)
      this.setData({
        book: { ...book, fileSizeText: formatSize(book.fileSize) },
        format,
        totalUnits
      })
    } catch (e) {
      console.error('加载书籍失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  async loadStartContent() {
    const { streamType, format, book, startPageNum, startChapterNum } = this.data
    if (streamType === STREAM_UNSUPPORTED) return

    if (streamType === STREAM_IMAGE) {
      const lastPage = book.lastPage || 1
      const urlPage = Number(startPageNum) || 0
      const startPage = Math.max(1, urlPage > 0 ? urlPage : lastPage)
      this.setData({ currentUnit: startPage, displayUnit: startPage, startUnitNum: startPage })
      this.loadPageImages(startPage, Math.min(startPage + 2, this.data.totalUnits))
    } else if (streamType === STREAM_HTML) {
      const lastChapter = book.lastChapter || 0
      const urlChapter = Number(startChapterNum) || 0
      const startChapter = Math.max(0, urlChapter > 0 ? urlChapter : lastChapter)
      this.setData({
        currentUnit: startChapter,
        displayUnit: startChapter + 1,
        startUnitNum: startChapter
      })
      this.buildChapterTitleMap()
      this.loadChapters(startChapter, Math.min(startChapter + 1, this.data.totalUnits - 1))
    }
  },

  buildChapterTitleMap() {
    const map = {}
    const traverse = (items) => {
      if (!items || !items.length) return
      items.forEach(item => {
        if (typeof item.chapterIndex === 'number') {
          map[item.chapterIndex] = item.title
        }
        if (item.children && item.children.length) {
          traverse(item.children)
        }
      })
    }
    traverse(this.data.toc)
    this.setData({ chapterTitleMap: map })
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
    if (from > to) return
    const app = getApp()
    const pages = []
    for (let i = from; i <= to; i++) {
      pages.push(`${app.globalData.baseUrl}/books/${this.data.bookId}/page/${i}?token=${app.globalData.token}`)
    }
    const startUnitNum = this.data.startUnitNum || from
    this.setData({
      loadedPages: [...this.data.loadedPages, ...pages],
      currentUnit: to,
      startUnitNum
    })
  },

  loadMorePages() {
    if (this.data.loading) return
    const next = this.data.currentUnit + 1
    if (next > this.data.totalUnits) return
    const end = Math.min(next + 2, this.data.totalUnits)
    this.setData({ loading: true })
    this.loadPageImages(next, end)
    this.setData({ loading: false })
  },

  async loadChapters(from, to) {
    if (from > to || from < 0) return
    const chapters = []
    for (let i = from; i <= to; i++) {
      const html = await this.getChapterHtml(i)
      if (html) {
        chapters.push({
          chapterIndex: i,
          title: this.data.chapterTitleMap[i] || `第 ${i + 1} 章`,
          html: html
        })
      }
    }
    this.setData({
      loadedChapters: [...this.data.loadedChapters, ...chapters],
      currentUnit: to
    })
  },

  async getChapterHtml(chapterIndex) {
    const cacheKey = String(chapterIndex)
    if (this.data._chapterHtmlCache[cacheKey]) {
      return this.data._chapterHtmlCache[cacheKey]
    }
    try {
      const res = await request({ url: `/books/${this.data.bookId}/unit/${chapterIndex}` })
      const html = res.data || ''
      this.data._chapterHtmlCache[cacheKey] = html
      return html
    } catch (e) {
      console.error('加载章节失败', chapterIndex, e)
      return ''
    }
  },

  async loadMoreChapters() {
    if (this.data.loading) return
    const next = this.data.currentUnit + 1
    if (next >= this.data.totalUnits) return
    const end = Math.min(next + 1, this.data.totalUnits - 1)
    this.setData({ loading: true })
    try {
      await this.loadChapters(next, end)
    } finally {
      this.setData({ loading: false })
    }
  },

  onScroll(e) {
    if (this._scrollTimer) return
    this._scrollTimer = setTimeout(() => {
      this._scrollTimer = null
      this.calcCurrentUnit('.page-image-wrap')
    }, 200)
  },

  onScrollHtml(e) {
    if (this._scrollTimer) return
    this._scrollTimer = setTimeout(() => {
      this._scrollTimer = null
      this.calcCurrentUnit('.chapter-wrap')
    }, 200)
  },

  calcCurrentUnit(selector) {
    const query = this.createSelectorQuery()
    query.selectAll(selector).boundingClientRect()
    query.exec((res) => {
      if (!res || !res[0] || res[0].length === 0) return
      const rects = res[0]
      const screenMid = 300
      let visibleIndex = 0
      for (let i = 0; i < rects.length; i++) {
        if (rects[i].top <= screenMid && rects[i].bottom > 0) {
          visibleIndex = i
        }
      }
      const newUnit = this.data.streamType === STREAM_HTML
        ? (this.data.loadedChapters[visibleIndex]?.chapterIndex ?? 0) + 1
        : this.data.startUnitNum + visibleIndex
      if (newUnit !== this.data.displayUnit && newUnit > 0) {
        this.setData({ displayUnit: newUnit })
      }
    })
  },

  onSwiperChange(e) {
    const idx = e.detail.current
    const displayUnit = this.data.streamType === STREAM_HTML
      ? (this.data.loadedChapters[idx]?.chapterIndex ?? 0) + 1
      : idx + 1
    this.setData({ swiperIndex: idx, displayUnit })
    if (idx >= this.data.loadedPages.length - 2 && this.data.currentUnit < this.data.totalUnits) {
      this.loadMorePages()
    }
  },

  onSwiperChangeHtml(e) {
    const idx = e.detail.current
    const chapter = this.data.loadedChapters[idx]
    const displayUnit = chapter ? chapter.chapterIndex + 1 : idx + 1
    this.setData({ swiperIndex: idx, displayUnit })
    if (idx >= this.data.loadedChapters.length - 2 && this.data.currentUnit < this.data.totalUnits - 1) {
      this.loadMoreChapters()
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
      const progressData = {
        bookId: Number(this.data.bookId),
        recordId: this.data.readingRecordId,
        lastPage: this.data.streamType === STREAM_IMAGE ? this.data.displayUnit : 0
      }
      await request({ url: '/reading/end', method: 'POST', data: progressData })
      const updateBody = {}
      if (this.data.streamType === STREAM_IMAGE) {
        updateBody.lastPage = this.data.displayUnit
      } else if (this.data.streamType === STREAM_HTML) {
        updateBody.lastChapter = this.data.displayUnit - 1
      }
      if (Object.keys(updateBody).length > 0) {
        await request({
          url: `/books/${this.data.bookId}/progress`,
          method: 'PUT',
          data: updateBody
        })
      }
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
    if (!page) return
    if (this.data.readMode === 'swipe') {
      if (page > this.data.loadedPages.length) {
        this.setData({ loadedPages: [], startUnitNum: page })
        this.loadPageImages(page, Math.min(page + 2, this.data.totalUnits))
        this.setData({ swiperIndex: 0, displayUnit: page })
      } else {
        this.setData({ swiperIndex: page - 1, displayUnit: page })
      }
    } else {
      this.setData({ loadedPages: [], startUnitNum: page })
      this.loadPageImages(page, Math.min(page + 2, this.data.totalUnits))
      this.setData({ displayUnit: page })
    }
  },

  async jumpToChapter(e) {
    const chapterIndex = Number(e.currentTarget.dataset.index)
    this.setData({ showToc: false })
    if (isNaN(chapterIndex) || chapterIndex < 0) return
    const existingIdx = this.data.loadedChapters.findIndex(c => c.chapterIndex === chapterIndex)
    if (this.data.readMode === 'swipe') {
      if (existingIdx >= 0) {
        this.setData({ swiperIndex: existingIdx, displayUnit: chapterIndex + 1 })
      } else {
        this.setData({ loadedChapters: [], currentUnit: chapterIndex, startUnitNum: chapterIndex })
        try {
          this.setData({ loading: true })
          await this.loadChapters(chapterIndex, Math.min(chapterIndex + 1, this.data.totalUnits - 1))
          this.setData({ swiperIndex: 0, displayUnit: chapterIndex + 1 })
        } finally {
          this.setData({ loading: false })
        }
      }
    } else {
      this.setData({ loadedChapters: [], currentUnit: chapterIndex, startUnitNum: chapterIndex })
      try {
        this.setData({ loading: true })
        await this.loadChapters(chapterIndex, Math.min(chapterIndex + 1, this.data.totalUnits - 1))
        this.setData({ displayUnit: chapterIndex + 1 })
      } finally {
        this.setData({ loading: false })
      }
    }
  },

  switchMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({ readMode: mode, swiperIndex: 0 })
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
          pageNum: this.data.displayUnit,
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
  },

  openAiAssistant() {
    wx.navigateTo({
      url: `/pages/ai-assistant/ai-assistant?id=${this.data.bookId}&page=${this.data.displayUnit}`
    })
  },

  onPageLongPress() {
    this.addBookmarkQuick()
  },

  onTouchStartPage() {
    if (this._longPressTimer) clearTimeout(this._longPressTimer)
    this._longPressTimer = setTimeout(() => {
      this.addBookmarkQuick()
    }, 600)
  },

  onTouchEndPage() {
    if (this._longPressTimer) {
      clearTimeout(this._longPressTimer)
      this._longPressTimer = null
    }
  },

  async addBookmarkQuick() {
    if (this.data.currentBookmarked) {
      wx.showToast({ title: '当前页已添加书签', icon: 'none' })
      return
    }
    const defaultTitle = this.data.streamType === STREAM_HTML
      ? `第${this.data.displayUnit}章`
      : `第${this.data.displayUnit}页`
    this.setData({
      showBookmarkDialog: true,
      bookmarkEditMode: false,
      editingBookmarkId: null,
      bookmarkTitle: defaultTitle,
      bookmarkRemark: '',
      bookmarkIsChapter: false
    })
  },

  async addBookmarkFromToolbar() {
    if (this.data.currentBookmarked) {
      wx.showActionSheet({
        itemList: ['取消当前书签', '编辑书签'],
        success: async (res) => {
          if (res.tapIndex === 0) {
            this.deleteBookmark(this.data.currentBookmarkId)
          } else if (res.tapIndex === 1) {
            this.editBookmark(this.data.currentBookmarkId)
          }
        }
      })
      return
    }
    this.addBookmarkQuick()
  },

  editBookmark(bookmarkId) {
    const bm = this.data.bookmarks.find(b => b.id === bookmarkId)
      || this.data.chapterBookmarks.find(b => b.id === bookmarkId)
    if (!bm) return
    this.setData({
      showBookmarkDialog: true,
      bookmarkEditMode: true,
      editingBookmarkId: bookmarkId,
      bookmarkTitle: bm.title || '',
      bookmarkRemark: bm.remark || '',
      bookmarkIsChapter: bm.isChapter === 1
    })
  },

  hideBookmarkDialog() {
    this.setData({ showBookmarkDialog: false })
  },

  onBookmarkTitleInput(e) {
    this.setData({ bookmarkTitle: e.detail.value })
  },

  onBookmarkRemarkInput(e) {
    this.setData({ bookmarkRemark: e.detail.value })
  },

  toggleBookmarkIsChapter() {
    this.setData({ bookmarkIsChapter: !this.data.bookmarkIsChapter })
  },

  async submitBookmark() {
    const pageNum = this.data.streamType === STREAM_HTML
      ? this.data.displayUnit - 1
      : this.data.displayUnit
    const unitType = this.data.streamType === STREAM_HTML ? 2 : 1

    try {
      if (this.data.bookmarkEditMode && this.data.editingBookmarkId) {
        await request({
          url: `/bookmarks/${this.data.editingBookmarkId}`,
          method: 'PUT',
          data: {
            bookId: Number(this.data.bookId),
            pageNum,
            title: this.data.bookmarkTitle,
            remark: this.data.bookmarkRemark,
            isChapter: this.data.bookmarkIsChapter ? 1 : 0
          }
        })
        wx.showToast({ title: '已更新', icon: 'success' })
      } else {
        await request({
          url: '/bookmarks',
          method: 'POST',
          data: {
            bookId: Number(this.data.bookId),
            bookTitle: this.data.book.title,
            pageNum,
            unitType,
            title: this.data.bookmarkTitle,
            remark: this.data.bookmarkRemark,
            isChapter: this.data.bookmarkIsChapter ? 1 : 0
          }
        })
        wx.showToast({ title: '书签已添加', icon: 'success' })
      }
      this.setData({ showBookmarkDialog: false })
      await this.loadBookmarks()
    } catch (e) {
      console.error('保存书签失败', e)
      wx.showToast({ title: '保存失败', icon: 'none' })
    }
  },

  async deleteBookmark(bookmarkId) {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个书签吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: `/bookmarks/${bookmarkId}`,
              method: 'DELETE'
            })
            wx.showToast({ title: '已删除', icon: 'success' })
            await this.loadBookmarks()
          } catch (e) {
            console.error('删除书签失败', e)
            wx.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      }
    })
  },

  toggleBookmarkPanel() {
    this.setData({
      showBookmark: !this.data.showBookmark,
      showToc: false,
      tocActiveTab: this.data.tocActiveTab || 'bookmark'
    })
  },

  switchTocTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ tocActiveTab: tab })
  },

  toggleTocPanel() {
    this.setData({
      showToc: !this.data.showToc,
      showBookmark: false,
      tocActiveTab: this.data.tocActiveTab || 'toc'
    })
  },

  jumpToBookmark(e) {
    const bm = e.currentTarget.dataset.bookmark
    const unitType = bm.unitType || 1
    const pageNum = bm.pageNum
    this.setData({ showBookmark: false, showToc: false })

    if (this.data.streamType === STREAM_HTML) {
      this.jumpToChapter({
        currentTarget: { dataset: { index: pageNum } }
      })
    } else {
      this.jumpToPage({
        currentTarget: { dataset: { page: pageNum } }
      })
    }
  },

  moveBookmarkUp(e) {
    const idx = e.currentTarget.dataset.index
    const type = e.currentTarget.dataset.type
    const list = type === 'chapter' ? [...this.data.chapterBookmarks] : [...this.data.bookmarks]
    if (idx <= 0) return
    const temp = list[idx]
    list[idx] = list[idx - 1]
    list[idx - 1] = temp
    this.updateBookmarkOrder(list, type)
  },

  moveBookmarkDown(e) {
    const idx = e.currentTarget.dataset.index
    const type = e.currentTarget.dataset.type
    const list = type === 'chapter' ? [...this.data.chapterBookmarks] : [...this.data.bookmarks]
    if (idx >= list.length - 1) return
    const temp = list[idx]
    list[idx] = list[idx + 1]
    list[idx + 1] = temp
    this.updateBookmarkOrder(list, type)
  },

  async updateBookmarkOrder(list, type) {
    const ids = list.map(b => b.id)
    try {
      await request({
        url: '/bookmarks/reorder',
        method: 'PUT',
        data: ids
      })
      if (type === 'chapter') {
        this.setData({ chapterBookmarks: list })
        if (this.data.toc.length > 0 && this.data.toc[0].isBookmark) {
          this.mergeTocWithChapterBookmarks()
        }
      } else {
        this.setData({ bookmarks: list })
      }
    } catch (e) {
      console.error('排序失败', e)
      wx.showToast({ title: '排序失败', icon: 'none' })
    }
  },

  calcCurrentUnit(selector) {
    const query = this.createSelectorQuery()
    query.selectAll(selector).boundingClientRect()
    query.exec((res) => {
      if (!res || !res[0] || res[0].length === 0) return
      const rects = res[0]
      const screenMid = 300
      let visibleIndex = 0
      for (let i = 0; i < rects.length; i++) {
        if (rects[i].top <= screenMid && rects[i].bottom > 0) {
          visibleIndex = i
        }
      }
      const newUnit = this.data.streamType === STREAM_HTML
        ? (this.data.loadedChapters[visibleIndex]?.chapterIndex ?? 0) + 1
        : this.data.startUnitNum + visibleIndex
      if (newUnit !== this.data.displayUnit && newUnit > 0) {
        this.setData({ displayUnit: newUnit })
        this.checkCurrentBookmark()
      }
    })
  }
})
