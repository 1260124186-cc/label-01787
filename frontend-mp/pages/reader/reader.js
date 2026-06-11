const { request } = require('../../utils/request')
const { formatSize } = require('../../utils/util')
const { THEMES, THEME_LIST, isValidTheme, getThemeConfig } = require('../../utils/theme')

const READER_CONFIG = {
  preloadOffset: 2,
  preloadEnabled: true,
  skeletonEnabled: true,
  weakNetworkThresholdKb: 50
}

const NETWORK_STATUS = {
  STRONG: 'strong',
  WEAK: 'weak',
  OFFLINE: 'offline'
}

const THEME_CONFIG = {
  white: { bgColor: '#FFFFFF', textColor: '#333333' },
  green: { bgColor: '#EBF9ED', textColor: '#2D4A2D' },
  dark:  { bgColor: '#1A1A2E', textColor: '#E0E0E0' },
  sepia: { bgColor: '#F5F0EB', textColor: '#5D4037' }
}

const BOOK_PREF_PREFIX = 'book_pref_'

const FONT_SIZE_OPTIONS = [
  { key: 'xs', name: '小', value: 24, scale: 0.85 },
  { key: 'sm', name: '标准', value: 28, scale: 0.95 },
  { key: 'md', name: '中', value: 32, scale: 1.0 },
  { key: 'lg', name: '大', value: 36, scale: 1.1 },
  { key: 'xl', name: '特大', value: 42, scale: 1.25 }
]

const LINE_HEIGHT_OPTIONS = [
  { key: 'tight', name: '紧凑', value: 1.5 },
  { key: 'normal', name: '标准', value: 1.8 },
  { key: 'loose', name: '宽松', value: 2.1 },
  { key: 'very-loose', name: '特松', value: 2.4 }
]

function getBookPrefKey(bookId) {
  return `${BOOK_PREF_PREFIX}${bookId}`
}

function loadBookPref(bookId) {
  try {
    const key = getBookPrefKey(bookId)
    const pref = wx.getStorageSync(key)
    if (pref && typeof pref === 'object') {
      return pref
    }
  } catch (e) {
    console.warn('加载书籍偏好失败', e)
  }
  return null
}

function saveBookPref(bookId, pref) {
  try {
    const key = getBookPrefKey(bookId)
    wx.setStorageSync(key, pref)
  } catch (e) {
    console.warn('保存书籍偏好失败', e)
  }
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
    themeList: THEME_LIST,
    themeClass: 'theme-white',
    bgColor: '#FFFFFF',
    textColor: '#333333',
    followSystemTheme: false,
    fontSize: 'md',
    fontSizeOptions: FONT_SIZE_OPTIONS,
    fontSizeValue: 32,
    lineHeight: 'normal',
    lineHeightOptions: LINE_HEIGHT_OPTIONS,
    lineHeightValue: 1.8,
    imageScale: 1.0,
    keepScreenOn: true,
    showUI: false,
    showToc: false,
    showBookmark: false,
    showTheme: false,
    showSettings: false,
    showNoteDialog: false,
    noteTags: '',
    noteIsPinned: false,
    noteColor: 'yellow',
    colorOptions: [
      { key: 'yellow', name: '黄色', color: '#FFD93D' },
      { key: 'green', name: '绿色', color: '#4CAF50' },
      { key: 'pink', name: '粉色', color: '#E91E63' }
    ],
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
    _longPressTimer: null,
    highlightKeyword: '',
    highlightMatchStart: 0,
    highlightMatchEnd: 0,
    pageMatches: [],
    networkStatus: NETWORK_STATUS.STRONG,
    showSkeleton: false,
    preloadOffset: 2,
    preloadEnabled: true,
    skeletonEnabled: true,
    _pageImageCache: {},
    _preloadTimer: null,
    _requestTimestamps: [],
    weakNetworkThresholdKb: 50
  },

  onLoad(options) {
    const app = getApp()
    const bookId = options.id
    const globalTheme = app.globalData.theme || 'white'
    const followSystemTheme = app.globalData.followSystemTheme || false
    const startPage = Number(options.page) || 0
    const startChapter = Number(options.chapter) || 0
    const format = options.format || 'pdf'
    const highlight = options.highlight ? decodeURIComponent(options.highlight) : ''
    const matchStart = Number(options.matchStart) || 0
    const matchEnd = Number(options.matchEnd) || 0

    const bookPref = loadBookPref(bookId)
    let finalTheme = globalTheme
    let finalReadMode = 'scroll'
    let finalFontSize = 'md'
    let finalLineHeight = 'normal'
    let finalImageScale = 1.0

    if (bookPref) {
      if (bookPref.theme && isValidTheme(bookPref.theme) && !followSystemTheme) {
        finalTheme = bookPref.theme
      }
      if (bookPref.readMode === 'scroll' || bookPref.readMode === 'swipe') {
        finalReadMode = bookPref.readMode
      }
      if (bookPref.fontSize && FONT_SIZE_OPTIONS.find(f => f.key === bookPref.fontSize)) {
        finalFontSize = bookPref.fontSize
      }
      if (bookPref.lineHeight && LINE_HEIGHT_OPTIONS.find(l => l.key === bookPref.lineHeight)) {
        finalLineHeight = bookPref.lineHeight
      }
      if (typeof bookPref.imageScale === 'number') {
        finalImageScale = bookPref.imageScale
      }
    }

    const fontSizeOpt = FONT_SIZE_OPTIONS.find(f => f.key === finalFontSize) || FONT_SIZE_OPTIONS[2]
    const lineHeightOpt = LINE_HEIGHT_OPTIONS.find(l => l.key === finalLineHeight) || LINE_HEIGHT_OPTIONS[1]
    const themeCfg = THEME_CONFIG[finalTheme] || THEME_CONFIG.white

    this.setData({
      bookId,
      format,
      startPageNum: startPage,
      startChapterNum: startChapter,
      theme: finalTheme,
      themeClass: `theme-${finalTheme}`,
      bgColor: themeCfg.bgColor,
      textColor: themeCfg.textColor,
      followSystemTheme,
      readMode: finalReadMode,
      fontSize: finalFontSize,
      fontSizeValue: fontSizeOpt.value,
      lineHeight: finalLineHeight,
      lineHeightValue: lineHeightOpt.value,
      imageScale: finalImageScale,
      highlightKeyword: highlight,
      highlightMatchStart: matchStart,
      highlightMatchEnd: matchEnd
    })

    this.applyPageBgColor(finalTheme)
    this.applyKeepScreenOn(true)
    this.initReader()
    if (highlight) {
      this.loadPageMatches(highlight)
    }
  },

  async loadPageMatches(keyword) {
    try {
      const res = await request({
        url: '/search/page-matches',
        data: { bookId: this.data.bookId, keyword }
      })
      this.setData({ pageMatches: res.data || [] })
    } catch (e) {
      console.error('加载页面匹配位置失败', e)
    }
  },

  async initReader() {
    await this.loadReaderConfig()
    this.setupNetworkListener()
    await this.loadBook()
    await this.determineStreamType()
    await this.loadToc()
    await this.loadBookmarks()
    await this.loadStartContent()
    this.startReading()
  },

  async loadReaderConfig() {
    try {
      const res = await request({ url: '/reader/config' })
      const config = res.data || {}
      const newConfig = {
        preloadOffset: typeof config['reader.preload.offset'] === 'number' ? config['reader.preload.offset'] : READER_CONFIG.preloadOffset,
        preloadEnabled: typeof config['reader.preload.enabled'] === 'boolean' ? config['reader.preload.enabled'] : READER_CONFIG.preloadEnabled,
        skeletonEnabled: typeof config['reader.skeleton.enabled'] === 'boolean' ? config['reader.skeleton.enabled'] : READER_CONFIG.skeletonEnabled,
        weakNetworkThresholdKb: typeof config['reader.weaknetwork.threshold_kb'] === 'number' ? config['reader.weaknetwork.threshold_kb'] : READER_CONFIG.weakNetworkThresholdKb
      }
      this.setData(newConfig)
      console.log('阅读器配置已加载:', newConfig)
    } catch (e) {
      console.warn('加载阅读器配置失败，使用默认配置', e)
    }
  },

  setupNetworkListener() {
    try {
      wx.onNetworkStatusChange((res) => {
        const isConnected = res.isConnected
        const networkType = res.networkType
        let networkStatus = NETWORK_STATUS.STRONG

        if (!isConnected) {
          networkStatus = NETWORK_STATUS.OFFLINE
        } else if (networkType === '2g' || networkType === 'slow-2g') {
          networkStatus = NETWORK_STATUS.WEAK
        }

        this.setData({ networkStatus })
        console.log('网络状态变化:', networkType, networkStatus)
      })

      wx.getNetworkType({
        success: (res) => {
          const networkType = res.networkType
          let networkStatus = NETWORK_STATUS.STRONG
          if (networkType === '2g' || networkType === 'none') {
            networkStatus = networkType === 'none' ? NETWORK_STATUS.OFFLINE : NETWORK_STATUS.WEAK
          }
          this.setData({ networkStatus })
        }
      })
    } catch (e) {
      console.warn('设置网络监听失败', e)
    }
  },

  recordRequestSpeed(bytes, durationMs) {
    if (durationMs <= 0) return
    const speedKbps = (bytes * 8) / (durationMs / 1000) / 1024
    const timestamps = [...this.data._requestTimestamps, { speed: speedKbps, time: Date.now() }]
    const recent = timestamps.filter(t => Date.now() - t.time < 10000)
    this.setData({ _requestTimestamps: recent })

    if (recent.length >= 3) {
      const avgSpeed = recent.reduce((sum, t) => sum + t.speed, 0) / recent.length
      if (avgSpeed < this.data.weakNetworkThresholdKb) {
        if (this.data.networkStatus !== NETWORK_STATUS.WEAK) {
          console.log('检测到弱网，平均速度:', avgSpeed.toFixed(1), 'KB/s')
          this.setData({ networkStatus: NETWORK_STATUS.WEAK })
        }
      } else if (this.data.networkStatus === NETWORK_STATUS.WEAK && avgSpeed > this.data.weakNetworkThresholdKb * 2) {
        this.setData({ networkStatus: NETWORK_STATUS.STRONG })
      }
    }
  },

  shouldShowSkeleton() {
    return this.data.skeletonEnabled && this.data.networkStatus === NETWORK_STATUS.WEAK
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
    this.applyKeepScreenOn(false)
  },

  onHide() {
    this.endReading()
  },

  onShow() {
    this.applyKeepScreenOn(this.data.keepScreenOn)
  },

  saveCurrentBookPref() {
    if (!this.data.bookId) return
    const pref = {
      theme: this.data.theme,
      readMode: this.data.readMode,
      fontSize: this.data.fontSize,
      lineHeight: this.data.lineHeight,
      imageScale: this.data.imageScale,
      updatedAt: Date.now()
    }
    saveBookPref(this.data.bookId, pref)
  },

  applyKeepScreenOn(on) {
    try {
      wx.setKeepScreenOn({ keepScreenOn: !!on })
    } catch (e) {
      console.warn('setKeepScreenOn failed', e)
    }
  },

  showSettingsPanel() {
    this.setData({ showSettings: true })
  },

  hideSettingsPanel() {
    this.setData({ showSettings: false })
  },

  changeFontSize(e) {
    const key = e.currentTarget.dataset.key
    const opt = FONT_SIZE_OPTIONS.find(f => f.key === key)
    if (!opt) return
    this.setData({
      fontSize: key,
      fontSizeValue: opt.value,
      imageScale: opt.scale
    })
    this.saveCurrentBookPref()
  },

  increaseFontSize() {
    const idx = FONT_SIZE_OPTIONS.findIndex(f => f.key === this.data.fontSize)
    if (idx < FONT_SIZE_OPTIONS.length - 1) {
      const opt = FONT_SIZE_OPTIONS[idx + 1]
      this.setData({
        fontSize: opt.key,
        fontSizeValue: opt.value,
        imageScale: opt.scale
      })
      this.saveCurrentBookPref()
    }
  },

  decreaseFontSize() {
    const idx = FONT_SIZE_OPTIONS.findIndex(f => f.key === this.data.fontSize)
    if (idx > 0) {
      const opt = FONT_SIZE_OPTIONS[idx - 1]
      this.setData({
        fontSize: opt.key,
        fontSizeValue: opt.value,
        imageScale: opt.scale
      })
      this.saveCurrentBookPref()
    }
  },

  changeLineHeight(e) {
    const key = e.currentTarget.dataset.key
    const opt = LINE_HEIGHT_OPTIONS.find(l => l.key === key)
    if (!opt) return
    this.setData({
      lineHeight: key,
      lineHeightValue: opt.value
    })
    this.saveCurrentBookPref()
  },

  toggleKeepScreenOn() {
    const newVal = !this.data.keepScreenOn
    this.setData({ keepScreenOn: newVal })
    this.applyKeepScreenOn(newVal)
  },

  toggleFollowSystemTheme() {
    const app = getApp()
    const newFollow = !this.data.followSystemTheme
    app.setFollowSystemTheme(newFollow)
    const theme = app.globalData.theme
    const themeCfg = THEME_CONFIG[theme] || THEME_CONFIG.white
    this.setData({
      followSystemTheme: newFollow,
      theme,
      themeClass: `theme-${theme}`,
      bgColor: themeCfg.bgColor,
      textColor: themeCfg.textColor
    })
    this.applyPageBgColor(theme)
    this.saveCurrentBookPref()
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

    setTimeout(() => {
      this.scrollToHighlight()
    }, 800)
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

  async loadPageImages(from, to) {
    if (from > to) return
    const app = getApp()
    const showSkeleton = this.shouldShowSkeleton()

    if (showSkeleton) {
      const skeletonPages = []
      for (let i = from; i <= to; i++) {
        skeletonPages.push('')
      }
      const startUnitNum = this.data.startUnitNum || from
      this.setData({
        loadedPages: [...this.data.loadedPages, ...skeletonPages],
        currentUnit: to,
        startUnitNum
      })
    }

    const pages = []
    const cache = this.data._pageImageCache

    for (let i = from; i <= to; i++) {
      const cacheKey = `page_${i}`
      if (cache[cacheKey]) {
        pages.push(cache[cacheKey])
      } else {
        const startTime = Date.now()
        const url = `${app.globalData.baseUrl}/books/${this.data.bookId}/page/${i}?token=${app.globalData.token}`

        try {
          const res = await this.downloadPageImage(url)
          const duration = Date.now() - startTime
          if (res && res.tempFilePath) {
            this.recordRequestSpeed(res.size || 100000, duration)
            cache[cacheKey] = res.tempFilePath
            pages.push(res.tempFilePath)

            if (showSkeleton) {
              const idx = this.data.loadedPages.findIndex((p, idx) => {
                const pageNum = this.data.startUnitNum + idx
                return pageNum === i && p === ''
              })
              if (idx >= 0) {
                const newLoadedPages = [...this.data.loadedPages]
                newLoadedPages[idx] = res.tempFilePath
                this.setData({ loadedPages: newLoadedPages, _pageImageCache: cache })
              }
            }
          } else {
            pages.push(url)
          }
        } catch (e) {
          console.error('下载页面图片失败', i, e)
          pages.push(url)
        }
      }
    }

    if (!showSkeleton) {
      const startUnitNum = this.data.startUnitNum || from
      this.setData({
        loadedPages: [...this.data.loadedPages, ...pages],
        currentUnit: to,
        startUnitNum,
        _pageImageCache: cache
      })
    }

    this.schedulePreload(to)
  },

  async downloadPageImage(url) {
    return new Promise((resolve, reject) => {
      wx.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res)
          } else {
            reject(new Error(`HTTP ${res.statusCode}`))
          }
        },
        fail: reject
      })
    })
  },

  schedulePreload(currentPage) {
    if (!this.data.preloadEnabled) return

    if (this.data._preloadTimer) {
      clearTimeout(this.data._preloadTimer)
    }

    this.setData({
      _preloadTimer: setTimeout(() => {
        this.preloadAdjacentPages(currentPage)
      }, 300)
    })
  },

  async preloadAdjacentPages(currentPage) {
    if (!this.data.preloadEnabled || this.data.loading) return

    const offset = this.data.preloadOffset
    const startPage = Math.max(1, currentPage + 1)
    const endPage = Math.min(currentPage + offset, this.data.totalUnits)

    if (startPage > endPage) return

    const cache = this.data._pageImageCache
    const pagesToLoad = []

    for (let i = startPage; i <= endPage; i++) {
      const cacheKey = `page_${i}`
      if (!cache[cacheKey]) {
        pagesToLoad.push(i)
      }
    }

    if (pagesToLoad.length === 0) {
      const backwardStart = Math.max(1, currentPage - offset)
      const backwardEnd = Math.max(1, currentPage - 1)
      for (let i = backwardStart; i <= backwardEnd; i++) {
        const cacheKey = `page_${i}`
        if (!cache[cacheKey]) {
          pagesToLoad.push(i)
        }
      }
    }

    if (pagesToLoad.length > 0) {
      console.log('后台预加载页面:', pagesToLoad)
      for (const pageNum of pagesToLoad) {
        try {
          const cacheKey = `page_${pageNum}`
          const app = getApp()
          const url = `${app.globalData.baseUrl}/books/${this.data.bookId}/page/${pageNum}?token=${app.globalData.token}`
          const res = await this.downloadPageImage(url)
          if (res && res.tempFilePath) {
            cache[cacheKey] = res.tempFilePath
            this.setData({ _pageImageCache: cache })
          }
        } catch (e) {
          console.warn('预加载页面失败', pageNum, e)
        }
      }
    }
  },

  async loadMorePages() {
    if (this.data.loading) return
    const next = this.data.currentUnit + 1
    if (next > this.data.totalUnits) return
    const end = Math.min(next + this.data.preloadOffset, this.data.totalUnits)
    this.setData({ loading: true, showSkeleton: this.shouldShowSkeleton() })
    try {
      await this.loadPageImages(next, end)
    } finally {
      this.setData({ loading: false, showSkeleton: false })
    }
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
      let html = res.data || ''
      if (this.data.highlightKeyword) {
        html = this.highlightKeywordInHtml(html, this.data.highlightKeyword)
      }
      this.data._chapterHtmlCache[cacheKey] = html
      return html
    } catch (e) {
      console.error('加载章节失败', chapterIndex, e)
      return ''
    }
  },

  highlightKeywordInHtml(html, keyword) {
    if (!html || !keyword) return html
    const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi')
    const textParts = html.split(/(<[^>]+>)/g)
    for (let i = 0; i < textParts.length; i++) {
      if (!textParts[i].startsWith('<') && textParts[i].trim()) {
        textParts[i] = textParts[i].replace(regex, '<span class="search-highlight">$1</span>')
      }
    }
    return textParts.join('')
  },

  scrollToHighlight() {
    const { highlightKeyword, streamType, format, displayUnit } = this.data
    if (!highlightKeyword) return
    setTimeout(() => {
      if (streamType === STREAM_HTML) {
        const query = this.createSelectorQuery()
        query.select('.search-highlight').boundingClientRect()
        query.selectViewport().scrollOffset()
        query.exec((res) => {
          if (res && res[0] && res[1]) {
            const highlightTop = res[0].top
            const scrollTop = res[1].scrollTop
            const targetScroll = scrollTop + highlightTop - 200
            wx.pageScrollTo({ scrollTop: targetScroll, duration: 300 })
          }
        })
      } else if (streamType === STREAM_IMAGE) {
        const query = this.createSelectorQuery()
        query.selectAll('.page-image-wrap').boundingClientRect()
        query.selectViewport().scrollOffset()
        query.exec((res) => {
          if (res && res[0] && res[1] && res[0].length > 0) {
            const idx = displayUnit - this.data.startUnitNum
            if (idx >= 0 && idx < res[0].length) {
              const pageTop = res[0][idx].top
              const scrollTop = res[1].scrollTop
              wx.pageScrollTo({ scrollTop: scrollTop + pageTop - 100, duration: 300 })
            }
          }
        })
      }
    }, 500)
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
    this.saveCurrentBookPref()
  },

  showThemePicker() {
    this.setData({ showTheme: true, showSettings: false })
  },

  hideThemePicker() {
    this.setData({ showTheme: false })
  },

  changeTheme(e) {
    const theme = e.currentTarget.dataset.theme
    if (!isValidTheme(theme)) return
    const app = getApp()
    app.setTheme(theme)
    const cfg = THEME_CONFIG[theme] || THEME_CONFIG.white
    this.setData({
      theme,
      themeClass: `theme-${theme}`,
      bgColor: cfg.bgColor,
      textColor: cfg.textColor,
      showTheme: false
    })
    this.applyPageBgColor(theme)
    this.saveCurrentBookPref()
  },

  applyPageBgColor(theme) {
    const cfg = THEME_CONFIG[theme] || THEME_CONFIG.white
    const color = cfg.bgColor
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
    this.setData({
      showNoteDialog: true,
      noteText: '',
      noteContent: '',
      noteType: 2,
      noteTags: '',
      noteIsPinned: false,
      noteColor: 'yellow'
    })
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

  onNoteTagsInput(e) {
    this.setData({ noteTags: e.detail.value })
  },

  setNoteType(e) {
    this.setData({ noteType: Number(e.currentTarget.dataset.type) })
  },

  toggleNotePin() {
    this.setData({ noteIsPinned: !this.data.noteIsPinned })
  },

  selectNoteColor(e) {
    const color = e.currentTarget.dataset.color
    this.setData({ noteColor: color })
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
          type: this.data.noteType,
          tags: this.data.noteTags,
          isPinned: this.data.noteIsPinned ? 1 : 0,
          color: this.data.noteColor
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
