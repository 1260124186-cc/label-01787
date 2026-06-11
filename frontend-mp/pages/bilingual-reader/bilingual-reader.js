const { request } = require('../../utils/request')
const { THEMES, THEME_LIST, isValidTheme, getThemeConfig } = require('../../utils/theme')

const STREAM_IMAGE = 'image'
const STREAM_HTML = 'html'
const STREAM_UNSUPPORTED = 'unsupported'

const THEME_CONFIG = {
  white: { bgColor: '#FFFFFF', textColor: '#333333' },
  green: { bgColor: '#EBF9ED', textColor: '#2D4A2D' },
  dark:  { bgColor: '#1A1A2E', textColor: '#E0E0E0' },
  sepia: { bgColor: '#F5F0EB', textColor: '#5D4037' }
}

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

const BOOK_PREF_PREFIX = 'bilingual_pref_'

function getPrefKey(pairId) {
  return `${BOOK_PREF_PREFIX}${pairId}`
}

function loadPref(pairId) {
  try {
    const p = wx.getStorageSync(getPrefKey(pairId))
    return p && typeof p === 'object' ? p : null
  } catch (e) { return null }
}

function savePref(pairId, pref) {
  try { wx.setStorageSync(getPrefKey(pairId), pref) } catch (e) {}
}

Page({
  data: {
    pairId: null,
    pair: null,
    leftBook: null,
    rightBook: null,
    alignments: [],
    alignmentsMapLeft: {},
    alignmentsMapRight: {},

    leftStreamType: STREAM_IMAGE,
    rightStreamType: STREAM_IMAGE,
    leftFormat: 'pdf',
    rightFormat: 'pdf',

    leftLoadedUnits: [],
    rightLoadedUnits: [],
    leftLoadedChapters: [],
    rightLoadedChapters: [],

    leftTotalUnits: 0,
    rightTotalUnits: 0,
    leftCurrentUnit: 0,
    rightCurrentUnit: 0,
    leftDisplayUnit: 1,
    rightDisplayUnit: 1,

    leftStartUnitNum: 1,
    rightStartUnitNum: 1,

    readMode: 'scroll',
    syncEnabled: true,
    alignmentStrategy: 1,
    syncMode: 'click',

    theme: 'white',
    themeClass: 'theme-white',
    bgColor: '#FFFFFF',
    textColor: '#333333',
    fontSize: 'md',
    fontSizeValue: 32,
    lineHeight: 'normal',
    lineHeightValue: 1.8,
    imageScale: 1.0,
    keepScreenOn: true,

    showUI: false,
    showSettings: false,
    showTheme: false,
    showToc: false,
    tocSide: 'left',
    leftToc: [],
    rightToc: [],
    showAlignments: false,
    showSyncModal: false,

    loading: false,
    _syncLock: false,
    _syncFrom: null,
    _scrollTimer: null,
    _chapterHtmlCache: { left: {}, right: {} },
    _pageImageCache: { left: {}, right: {} },

    aiAlignmentStatus: 0,
    aiAlignmentProgress: 0,
    showAIPanel: false,

    leftChapterTitleMap: {},
    rightChapterTitleMap: {},
    activeAlignmentId: null
  },

  onLoad(options) {
    const pairId = options.pairId
    if (!pairId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    const app = getApp()
    const globalTheme = app.globalData.theme || 'white'

    const pref = loadPref(pairId)
    let finalTheme = globalTheme
    let finalFontSize = 'md'
    let finalLineHeight = 'normal'
    let finalSync = true
    let finalSyncMode = 'click'

    if (pref) {
      if (pref.theme && isValidTheme(pref.theme)) finalTheme = pref.theme
      if (pref.fontSize && FONT_SIZE_OPTIONS.find(f => f.key === pref.fontSize)) finalFontSize = pref.fontSize
      if (pref.lineHeight && LINE_HEIGHT_OPTIONS.find(l => l.key === pref.lineHeight)) finalLineHeight = pref.lineHeight
      if (typeof pref.syncEnabled === 'boolean') finalSync = pref.syncEnabled
      if (pref.syncMode) finalSyncMode = pref.syncMode
    }

    const fontOpt = FONT_SIZE_OPTIONS.find(f => f.key === finalFontSize) || FONT_SIZE_OPTIONS[2]
    const lhOpt = LINE_HEIGHT_OPTIONS.find(l => l.key === finalLineHeight) || LINE_HEIGHT_OPTIONS[1]
    const themeCfg = THEME_CONFIG[finalTheme] || THEME_CONFIG.white

    this.setData({
      pairId,
      theme: finalTheme,
      themeClass: `theme-${finalTheme}`,
      bgColor: themeCfg.bgColor,
      textColor: themeCfg.textColor,
      fontSize: finalFontSize,
      fontSizeValue: fontOpt.value,
      lineHeight: finalLineHeight,
      lineHeightValue: lhOpt.value,
      imageScale: fontOpt.scale,
      syncEnabled: finalSync,
      syncMode: finalSyncMode
    })

    this.applyPageBgColor(finalTheme)
    wx.setKeepScreenOn({ keepScreenOn: true })
    this.init()
  },

  async init() {
    this.setData({ loading: true })
    try {
      const [pairRes, progRes] = await Promise.all([
        request({ url: `/bilingual/pairs/${this.data.pairId}` }),
        request({ url: `/bilingual/pairs/${this.data.pairId}/progress` })
      ])
      const pair = pairRes.data
      const progress = progRes.data || {}

      const leftBookId = pair.leftBookId
      const rightBookId = pair.rightBookId
      const [leftRes, rightRes] = await Promise.all([
        request({ url: `/books/${leftBookId}` }),
        request({ url: `/books/${rightBookId}` })
      ])

      const leftBook = leftRes.data
      const rightBook = rightRes.data

      const leftFormat = leftBook.bookFormat || 'pdf'
      const rightFormat = rightBook.bookFormat || 'pdf'
      const leftTotal = leftFormat === 'epub' ? (leftBook.chapterCount || 0) : (leftBook.pageCount || 0)
      const rightTotal = rightFormat === 'epub' ? (rightBook.chapterCount || 0) : (rightBook.pageCount || 0)

      const [leftStreamRes, rightStreamRes] = await Promise.all([
        request({ url: `/books/${leftBookId}/stream-type` }).catch(() => ({ data: leftFormat === 'epub' ? STREAM_HTML : STREAM_IMAGE })),
        request({ url: `/books/${rightBookId}/stream-type` }).catch(() => ({ data: rightFormat === 'epub' ? STREAM_HTML : STREAM_IMAGE }))
      ])

      const leftStreamType = leftStreamRes.data || (leftFormat === 'epub' ? STREAM_HTML : STREAM_IMAGE)
      const rightStreamType = rightStreamRes.data || (rightFormat === 'epub' ? STREAM_HTML : STREAM_IMAGE)

      const startLeft = progress.leftUnit || pair.lastLeftUnit || 0
      const startRight = progress.rightUnit || pair.lastRightUnit || 0

      let displayLeft = 1, displayRight = 1
      let leftIdx = 0, rightIdx = 0

      if (leftStreamType === STREAM_HTML) {
        leftIdx = Math.min(Math.max(0, startLeft), Math.max(0, leftTotal - 1))
        displayLeft = leftIdx + 1
      } else {
        leftIdx = Math.max(1, startLeft || 1)
        displayLeft = leftIdx
      }
      if (rightStreamType === STREAM_HTML) {
        rightIdx = Math.min(Math.max(0, startRight), Math.max(0, rightTotal - 1))
        displayRight = rightIdx + 1
      } else {
        rightIdx = Math.max(1, startRight || 1)
        displayRight = rightIdx
      }

      this.setData({
        pair,
        leftBook,
        rightBook,
        leftFormat,
        rightFormat,
        leftTotalUnits: leftTotal,
        rightTotalUnits: rightTotal,
        leftStreamType,
        rightStreamType,
        leftCurrentUnit: leftIdx,
        rightCurrentUnit: rightIdx,
        leftDisplayUnit: displayLeft,
        rightDisplayUnit: displayRight,
        leftStartUnitNum: leftIdx,
        rightStartUnitNum: rightIdx,
        syncEnabled: pair.syncEnabled === 0 ? false : (progress.syncEnabled !== undefined ? !!progress.syncEnabled : this.data.syncEnabled),
        alignmentStrategy: pair.alignmentStrategy || 1,
        aiAlignmentStatus: pair.aiAlignmentStatus || 0,
        aiAlignmentProgress: pair.aiAlignmentProgress || 0
      })

      await Promise.all([
        this.loadTocFor('left'),
        this.loadTocFor('right'),
        this.loadAlignments(),
        this.loadInitialContent('left', leftIdx, leftStreamType, leftFormat),
        this.loadInitialContent('right', rightIdx, rightStreamType, rightFormat)
      ])

      this.startReading()
    } catch (e) {
      console.error('初始化双语阅读器失败', e)
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadTocFor(side) {
    try {
      const bookId = side === 'left' ? this.data.pair.leftBookId : this.data.pair.rightBookId
      const res = await request({ url: `/books/${bookId}/toc` })
      const toc = res.data || []
      const mapKey = `${side}Toc`
      const titleMapKey = `${side}ChapterTitleMap`
      this.setData({ [mapKey]: toc })

      if ((side === 'left' ? this.data.leftFormat : this.data.rightFormat) === 'epub') {
        const titleMap = {}
        const traverse = (items) => {
          if (!items || !items.length) return
          items.forEach(item => {
            if (typeof item.chapterIndex === 'number') {
              titleMap[item.chapterIndex] = item.title
            }
            if (item.children && item.children.length) traverse(item.children)
          })
        }
        traverse(toc)
        this.setData({ [titleMapKey]: titleMap })
      }
    } catch (e) {
      console.warn(`加载${side}目录失败`, e)
    }
  },

  async loadAlignments() {
    try {
      const res = await request({
        url: `/bilingual/pairs/${this.data.pairId}/alignments`
      })
      const alignments = res.data || []
      const mapLeft = {}
      const mapRight = {}
      alignments.forEach(a => {
        if (!mapLeft[a.leftUnitIndex]) mapLeft[a.leftUnitIndex] = []
        mapLeft[a.leftUnitIndex].push(a)
        if (!mapRight[a.rightUnitIndex]) mapRight[a.rightUnitIndex] = []
        mapRight[a.rightUnitIndex].push(a)
      })
      this.setData({
        alignments,
        alignmentsMapLeft: mapLeft,
        alignmentsMapRight: mapRight
      })
    } catch (e) {
      console.warn('加载对齐信息失败', e)
    }
  },

  async loadInitialContent(side, unitIdx, streamType, format) {
    if (streamType === STREAM_IMAGE) {
      const start = unitIdx < 1 ? 1 : unitIdx
      const end = Math.min(start + 2, side === 'left' ? this.data.leftTotalUnits : this.data.rightTotalUnits)
      await this.loadPageImages(side, start, end)
    } else if (streamType === STREAM_HTML) {
      const start = unitIdx
      const total = side === 'left' ? this.data.leftTotalUnits : this.data.rightTotalUnits
      const end = Math.min(start + 1, Math.max(0, total - 1))
      await this.loadChapters(side, start, end)
    }
  },

  async loadPageImages(side, from, to) {
    if (from > to) return
    const app = getApp()
    const bookId = side === 'left' ? this.data.pair.leftBookId : this.data.pair.rightBookId
    const pages = []
    const cache = this.data._pageImageCache[side]
    const loadedKey = side === 'left' ? 'leftLoadedUnits' : 'rightLoadedUnits'
    const currKey = side === 'left' ? 'leftCurrentUnit' : 'rightCurrentUnit'
    const startKey = side === 'left' ? 'leftStartUnitNum' : 'rightStartUnitNum'

    for (let i = from; i <= to; i++) {
      const cacheKey = `page_${i}`
      if (cache[cacheKey]) {
        pages.push(cache[cacheKey])
      } else {
        const url = `${app.globalData.baseUrl}/books/${bookId}/page/${i}?token=${app.globalData.token}`
        try {
          const res = await new Promise((resolve, reject) => {
            wx.downloadFile({
              url,
              success: (r) => r.statusCode === 200 ? resolve(r) : reject(new Error(`HTTP ${r.statusCode}`)),
              fail: reject
            })
          })
          if (res && res.tempFilePath) {
            cache[cacheKey] = res.tempFilePath
            pages.push(res.tempFilePath)
          } else {
            pages.push(url)
          }
        } catch (e) {
          console.error(`下载${side}页面失败`, i, e)
          pages.push(url)
        }
      }
    }

    const loaded = this.data[loadedKey]
    const startNum = this.data[startKey] || from
    this.setData({
      [loadedKey]: [...loaded, ...pages],
      [currKey]: to,
      [startKey]: startNum,
      [`_pageImageCache.${side}`]: cache
    })
  },

  async loadChapters(side, from, to) {
    if (from > to || from < 0) return
    const bookId = side === 'left' ? this.data.pair.leftBookId : this.data.pair.rightBookId
    const format = side === 'left' ? this.data.leftFormat : this.data.rightFormat
    const chapters = []
    const cache = this.data._chapterHtmlCache[side]
    const titleMap = side === 'left' ? this.data.leftChapterTitleMap : this.data.rightChapterTitleMap
    const loadedKey = side === 'left' ? 'leftLoadedChapters' : 'rightLoadedChapters'
    const currKey = side === 'left' ? 'leftCurrentUnit' : 'rightCurrentUnit'

    for (let i = from; i <= to; i++) {
      const cacheKey = String(i)
      let html = cache[cacheKey]
      if (!html) {
        try {
          const res = await request({ url: `/books/${bookId}/unit/${i}` })
          html = res.data || ''
          cache[cacheKey] = html
        } catch (e) {
          console.error(`加载${side}章节失败`, i, e)
          html = ''
        }
      }
      if (html) {
        chapters.push({
          chapterIndex: i,
          title: titleMap[i] || `第 ${i + 1} 章`,
          html
        })
      }
    }

    this.setData({
      [loadedKey]: [...this.data[loadedKey], ...chapters],
      [currKey]: to,
      [`_chapterHtmlCache.${side}`]: cache
    })
  },

  async loadMoreForSide(side) {
    if (this.data.loading) return
    const streamType = side === 'left' ? this.data.leftStreamType : this.data.rightStreamType
    const curr = side === 'left' ? this.data.leftCurrentUnit : this.data.rightCurrentUnit
    const total = side === 'left' ? this.data.leftTotalUnits : this.data.rightTotalUnits
    const next = curr + 1

    if (streamType === STREAM_IMAGE) {
      if (next > total) return
      const end = Math.min(next + 2, total)
      this.setData({ loading: true })
      try {
        await this.loadPageImages(side, next, end)
      } finally {
        this.setData({ loading: false })
      }
    } else if (streamType === STREAM_HTML) {
      if (next >= total) return
      const end = Math.min(next + 1, total - 1)
      this.setData({ loading: true })
      try {
        await this.loadChapters(side, next, end)
      } finally {
        this.setData({ loading: false })
      }
    }
  },

  onSideScroll(e) {
    const side = e.currentTarget.dataset.side
    if (this._scrollTimer) return
    this._scrollTimer = setTimeout(() => {
      this._scrollTimer = null
      this.calcCurrentUnit(side)
    }, 200)
  },

  calcCurrentUnit(side) {
    const streamType = side === 'left' ? this.data.leftStreamType : this.data.rightStreamType
    const loadedKey = streamType === STREAM_HTML
      ? (side === 'left' ? 'leftLoadedChapters' : 'rightLoadedChapters')
      : (side === 'left' ? 'leftLoadedUnits' : 'rightLoadedUnits')
    const displayKey = side === 'left' ? 'leftDisplayUnit' : 'rightDisplayUnit'
    const startKey = side === 'left' ? 'leftStartUnitNum' : 'rightStartUnitNum'
    const selector = streamType === STREAM_HTML ? `.bilingual-${side} .chapter-wrap` : `.bilingual-${side} .page-image-wrap`

    const query = wx.createSelectorQuery()
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

      let newUnit
      if (streamType === STREAM_HTML) {
        const loaded = this.data[loadedKey]
        newUnit = (loaded[visibleIndex]?.chapterIndex ?? 0) + 1
      } else {
        newUnit = this.data[startKey] + visibleIndex
      }

      if (newUnit !== this.data[displayKey] && newUnit > 0) {
        this.setData({ [displayKey]: newUnit })
        if (this.data.syncEnabled && this.data.syncMode === 'scroll' && !this.data._syncLock) {
          this.syncOtherSide(side, newUnit)
        }
      }
    })
  },

  syncOtherSide(fromSide, unitNum) {
    const thisIdx = fromSide === 'left' ? unitNum - (this.data.leftStreamType === STREAM_HTML ? 1 : 0) : unitNum - (this.data.rightStreamType === STREAM_HTML ? 1 : 0)
    const alignMap = fromSide === 'left' ? this.data.alignmentsMapLeft : this.data.alignmentsMapRight
    const alignList = alignMap[thisIdx] || []
    const otherSide = fromSide === 'left' ? 'right' : 'left'

    let targetUnit
    if (alignList.length > 0) {
      const best = alignList[0]
      targetUnit = fromSide === 'left' ? best.rightUnitIndex : best.leftUnitIndex
    } else {
      targetUnit = thisIdx
    }

    const otherStream = otherSide === 'left' ? this.data.leftStreamType : this.data.rightStreamType
    const displayTarget = otherStream === STREAM_HTML ? targetUnit + 1 : Math.max(1, targetUnit)

    this.setData({ _syncLock: true, _syncFrom: fromSide })
    setTimeout(() => {
      this.setData({ _syncLock: false, _syncFrom: null })
    }, 800)

    this.jumpToUnit(otherSide, displayTarget)
  },

  jumpToUnit(side, unitNum) {
    const streamType = side === 'left' ? this.data.leftStreamType : this.data.rightStreamType
    const format = side === 'left' ? this.data.leftFormat : this.data.rightFormat
    const displayKey = side === 'left' ? 'leftDisplayUnit' : 'rightDisplayUnit'
    const loadedKey = streamType === STREAM_HTML
      ? (side === 'left' ? 'leftLoadedChapters' : 'rightLoadedChapters')
      : (side === 'left' ? 'leftLoadedUnits' : 'rightLoadedUnits')
    const startKey = side === 'left' ? 'leftStartUnitNum' : 'rightStartUnitNum'
    const total = side === 'left' ? this.data.leftTotalUnits : this.data.rightTotalUnits

    this.setData({ [displayKey]: unitNum })

    if (streamType === STREAM_IMAGE) {
      const loaded = this.data[loadedKey]
      const start = this.data[startKey]
      const targetIdx = unitNum - start

      if (targetIdx >= 0 && targetIdx < loaded.length) {
        this.scrollToIndex(side, targetIdx)
        return
      }
      const loadStart = Math.max(1, unitNum)
      const loadEnd = Math.min(loadStart + 2, total)
      this.setData({ [loadedKey]: [], [startKey]: loadStart })
      this.loadPageImages(side, loadStart, loadEnd).then(() => {
        setTimeout(() => this.scrollToIndex(side, 0), 200)
      })
    } else {
      const targetIdx = Math.max(0, unitNum - 1)
      if (targetIdx < 0) return
      const loaded = this.data[loadedKey]
      const existingIdx = loaded.findIndex(c => c.chapterIndex === targetIdx)
      if (existingIdx >= 0) {
        this.scrollToIndex(side, existingIdx)
        return
      }
      const loadStart = targetIdx
      const loadEnd = Math.min(loadStart + 1, Math.max(0, total - 1))
      this.setData({ [loadedKey]: [] })
      this.loadChapters(side, loadStart, loadEnd).then(() => {
        setTimeout(() => this.scrollToIndex(side, 0), 200)
      })
    }
  },

  scrollToIndex(side, index) {
    const selector = `#bi-${side}-unit-${index}`
    wx.createSelectorQuery().select(selector).boundingClientRect().exec((res) => {
      if (res && res[0]) {
        const top = res[0].top
        wx.pageScrollTo({ scrollTop: top, duration: 300 })
      }
    })
    const scrollSelector = side === 'left' ? '#left-scroll' : '#right-scroll'
    wx.createSelectorQuery().select(scrollSelector).boundingClientRect().selectViewport().scrollOffset().exec((r) => {
      if (r && r[0] && r[1]) {
        const offset = r[0].top
        const curScroll = r[1].scrollTop
        const targetTop = (res[0]?.top || 0) - offset + curScroll
        wx.pageScrollTo({ scrollTop: Math.max(0, targetTop), duration: 300 })
      }
    })
  },

  onTapUnit(e) {
    const { side, index } = e.currentTarget.dataset
    const streamType = side === 'left' ? this.data.leftStreamType : this.data.rightStreamType
    const unitIdx = streamType === STREAM_HTML ? index : index + (this.data[`${side}StartUnitNum`] || 1)

    if (this.data.syncEnabled && this.data.syncMode === 'click') {
      this.setData({ activeAlignmentId: null })
      const alignMap = side === 'left' ? this.data.alignmentsMapLeft : this.data.alignmentsMapRight
      const alignList = alignMap[unitIdx] || []
      if (alignList.length > 0) {
        const best = alignList[0]
        this.setData({ activeAlignmentId: best.id })
        const otherSide = side === 'left' ? 'right' : 'left'
        const target = side === 'left' ? best.rightUnitIndex : best.leftUnitIndex
        const otherStream = otherSide === 'left' ? this.data.leftStreamType : this.data.rightStreamType
        const displayTarget = otherStream === STREAM_HTML ? target + 1 : Math.max(1, target)
        this.jumpToUnit(otherSide, displayTarget)
      }
    }
  },

  onScrollLower(e) {
    const side = e.currentTarget.dataset.side
    this.loadMoreForSide(side)
  },

  onSwiperChange(e) {
    const side = e.currentTarget.dataset.side
    const idx = e.detail.current
    const streamType = side === 'left' ? this.data.leftStreamType : this.data.rightStreamType
    const loadedKey = streamType === STREAM_HTML
      ? (side === 'left' ? 'leftLoadedChapters' : 'rightLoadedChapters')
      : (side === 'left' ? 'leftLoadedUnits' : 'rightLoadedUnits')
    const displayKey = side === 'left' ? 'leftDisplayUnit' : 'rightDisplayUnit'
    const total = side === 'left' ? this.data.leftTotalUnits : this.data.rightTotalUnits
    const curr = side === 'left' ? this.data.leftCurrentUnit : this.data.rightCurrentUnit
    const loaded = this.data[loadedKey]

    let display
    if (streamType === STREAM_HTML) {
      display = (loaded[idx]?.chapterIndex ?? 0) + 1
    } else {
      display = idx + 1
    }
    this.setData({ [displayKey]: display })

    if (idx >= loaded.length - 2 && curr < total - (streamType === STREAM_HTML ? 1 : 0)) {
      this.loadMoreForSide(side)
    }

    if (this.data.syncEnabled && this.data.syncMode === 'scroll' && !this.data._syncLock) {
      this.syncOtherSide(side, display)
    }
  },

  toggleUI() {
    this.setData({ showUI: !this.data.showUI })
  },

  goBack() {
    wx.navigateBack()
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
    this.savePref()
  },

  changeLineHeight(e) {
    const key = e.currentTarget.dataset.key
    const opt = LINE_HEIGHT_OPTIONS.find(l => l.key === key)
    if (!opt) return
    this.setData({ lineHeight: key, lineHeightValue: opt.value })
    this.savePref()
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
    const cfg = THEME_CONFIG[theme] || THEME_CONFIG.white
    this.setData({
      theme,
      themeClass: `theme-${theme}`,
      bgColor: cfg.bgColor,
      textColor: cfg.textColor,
      showTheme: false
    })
    this.applyPageBgColor(theme)
    this.savePref()
  },

  applyPageBgColor(theme) {
    const cfg = THEME_CONFIG[theme] || THEME_CONFIG.white
    const color = cfg.bgColor
    wx.setBackgroundColor({ backgroundColor: color, backgroundColorTop: color, backgroundColorBottom: color })
  },

  toggleSync() {
    const newVal = !this.data.syncEnabled
    this.setData({ syncEnabled: newVal })
    wx.showToast({ title: newVal ? '同步已开启' : '同步已关闭', icon: 'none' })
    this.savePref()
  },

  changeSyncMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({ syncMode: mode, showSyncModal: false })
    wx.showToast({ title: mode === 'scroll' ? '滚动时同步' : '点击时同步', icon: 'none' })
    this.savePref()
  },

  showSyncSettings() {
    this.setData({ showSyncModal: true, showSettings: false })
  },

  hideSyncModal() {
    this.setData({ showSyncModal: false })
  },

  toggleKeepScreenOn() {
    const newVal = !this.data.keepScreenOn
    this.setData({ keepScreenOn: newVal })
    wx.setKeepScreenOn({ keepScreenOn: newVal })
  },

  openToc(e) {
    const side = e.currentTarget.dataset.side
    this.setData({ showToc: true, tocSide: side })
  },

  closeToc() {
    this.setData({ showToc: false })
  },

  jumpFromToc(e) {
    const side = this.data.tocSide
    const unit = e.currentTarget.dataset.unit
    const isChapter = e.currentTarget.dataset.chapter !== undefined
    this.setData({ showToc: false })
    const displayUnit = isChapter ? unit + 1 : unit
    this.jumpToUnit(side, displayUnit)
  },

  switchMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({ readMode: mode })
    this.savePref()
  },

  savePref() {
    savePref(this.data.pairId, {
      theme: this.data.theme,
      fontSize: this.data.fontSize,
      lineHeight: this.data.lineHeight,
      syncEnabled: this.data.syncEnabled,
      syncMode: this.data.syncMode,
      readMode: this.data.readMode
    })
  },

  async startReading() {
    try {
      await request({
        url: '/reading/start',
        method: 'POST',
        data: { bookId: Number(this.data.pair.leftBookId) }
      })
      await request({
        url: '/reading/start',
        method: 'POST',
        data: { bookId: Number(this.data.pair.rightBookId) }
      })
    } catch (e) {
      console.warn('记录阅读开始失败', e)
    }
  },

  async endReading() {
    try {
      const leftDisplay = this.data.leftDisplayUnit
      const rightDisplay = this.data.rightDisplayUnit
      const leftStream = this.data.leftStreamType
      const rightStream = this.data.rightStreamType
      const lastLeft = leftStream === STREAM_HTML ? leftDisplay - 1 : leftDisplay
      const lastRight = rightStream === STREAM_HTML ? rightDisplay - 1 : rightDisplay

      await request({
        url: `/bilingual/pairs/${this.data.pairId}`,
        method: 'PUT',
        data: { lastLeftUnit: lastLeft, lastRightUnit: lastRight }
      })

      const leftUpdate = leftStream === STREAM_HTML ? { lastChapter: lastLeft } : { lastPage: lastLeft }
      const rightUpdate = rightStream === STREAM_HTML ? { lastChapter: lastRight } : { lastPage: lastRight }

      await request({
        url: `/books/${this.data.pair.leftBookId}/progress`,
        method: 'PUT',
        data: leftUpdate
      })
      await request({
        url: `/books/${this.data.pair.rightBookId}/progress`,
        method: 'PUT',
        data: rightUpdate
      })
    } catch (e) {
      console.warn('保存阅读进度失败', e)
    }
  },

  onUnload() {
    this.endReading()
    wx.setKeepScreenOn({ keepScreenOn: false })
  },

  onHide() {
    this.endReading()
  },

  async triggerAIAlignment() {
    wx.showModal({
      title: '启动AI段落对齐',
      content: 'AI会分析两本书的内容进行智能段落匹配，需要消耗AI额度。是否继续？',
      confirmText: '启动',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await request({
            url: `/bilingual/pairs/${this.data.pairId}/ai-align`,
            method: 'POST'
          })
          wx.showToast({ title: 'AI对齐已启动', icon: 'none' })
          this.setData({ aiAlignmentStatus: 1, aiAlignmentProgress: 0, showAIPanel: true })
          this.pollAIStatus()
        } catch (e) {
          wx.showToast({ title: e.message || '启动失败', icon: 'none' })
        }
      }
    })
  },

  pollAIStatus() {
    if (this._aiPollTimer) clearInterval(this._aiPollTimer)
    this._aiPollTimer = setInterval(async () => {
      try {
        const res = await request({ url: `/bilingual/pairs/${this.data.pairId}` })
        const pair = res.data
        this.setData({
          aiAlignmentStatus: pair.aiAlignmentStatus || 0,
          aiAlignmentProgress: pair.aiAlignmentProgress || 0
        })
        if (pair.aiAlignmentStatus === 2 || pair.aiAlignmentStatus === 3) {
          clearInterval(this._aiPollTimer)
          this._aiPollTimer = null
          if (pair.aiAlignmentStatus === 2) {
            await this.loadAlignments()
            wx.showToast({ title: 'AI对齐完成', icon: 'success' })
          } else {
            wx.showToast({ title: 'AI对齐失败', icon: 'none' })
          }
          setTimeout(() => this.setData({ showAIPanel: false }), 2000)
        }
      } catch (e) {
        console.warn('轮询AI状态失败', e)
      }
    }, 2000)
  },

  openAIPanel() {
    this.setData({ showAIPanel: true })
  },

  hideAIPanel() {
    this.setData({ showAIPanel: false })
  },

  openAlignmentsView() {
    this.setData({ showAlignments: true })
  },

  hideAlignments() {
    this.setData({ showAlignments: false })
  },

  onShareAppMessage() {
    const { pair } = this.data
    return {
      title: pair ? `双语对照：${pair.name}` : '双语对照阅读',
      path: `/pages/bookshelf/bookshelf`
    }
  }
})
