const { request } = require('../../utils/request')

const LANGUAGE_OPTIONS = [
  { key: '', name: '未设置' },
  { key: 'zh', name: '中文' },
  { key: 'en', name: 'English' },
  { key: 'ja', name: '日本語' },
  { key: 'ko', name: '한국어' },
  { key: 'fr', name: 'Français' },
  { key: 'de', name: 'Deutsch' },
  { key: 'es', name: 'Español' },
  { key: 'ru', name: 'Русский' }
]

const ALIGNMENT_OPTIONS = [
  { key: 1, name: '章节号对齐', desc: '按章节/页码序号对应，适合结构完全一致的译本' },
  { key: 2, name: 'AI段落对齐', desc: '使用AI分析语义匹配段落，适合译本差异较大的书籍' }
]

Page({
  data: {
    sourceBookId: null,
    loading: false,
    books: [],
    selectedLeftId: null,
    selectedRightId: null,
    leftBook: null,
    rightBook: null,
    leftLanguage: 'zh',
    rightLanguage: 'en',
    alignmentStrategy: 1,
    pairName: '',
    languageOptions: LANGUAGE_OPTIONS,
    alignmentOptions: ALIGNMENT_OPTIONS,
    showBookPicker: false,
    pickerSide: 'left',
    keyword: '',
    allPairs: []
  },

  onLoad(options) {
    const bookId = options.bookId ? Number(options.bookId) : null
    this.setData({ sourceBookId: bookId })
    if (bookId) {
      this.setData({ selectedLeftId: bookId })
      this.loadSingleBook(bookId, 'left')
    }
    this.loadBooks()
    this.loadAllPairs()
  },

  async loadBooks() {
    try {
      const res = await request({
        url: '/books',
        data: { page: 1, size: 200, sortBy: 'last_read' }
      })
      const records = res.data?.records || res.data || []
      this.setData({ books: records })
      if (this.data.sourceBookId) {
        const src = records.find(b => b.id === this.data.sourceBookId)
        if (src) {
          this.setData({ leftBook: src })
        }
      }
    } catch (e) {
      console.error('加载书籍列表失败', e)
      wx.showToast({ title: '加载书籍失败', icon: 'none' })
    }
  },

  async loadAllPairs() {
    try {
      const res = await request({
        url: '/bilingual/pairs',
        data: { page: 1, size: 100 }
      })
      this.setData({ allPairs: res.data?.records || res.data || [] })
    } catch (e) {
      console.warn('加载已有双语关联失败', e)
    }
  },

  async loadSingleBook(id, side) {
    try {
      const res = await request({ url: `/books/${id}` })
      const key = side === 'left' ? 'leftBook' : 'rightBook'
      this.setData({ [key]: res.data })
      this.updatePairName()
    } catch (e) {
      console.warn('加载单本书籍失败', e)
    }
  },

  updatePairName() {
    const { leftBook, rightBook } = this.data
    if (leftBook && rightBook) {
      this.setData({ pairName: `${leftBook.title} ↔ ${rightBook.title}` })
    }
  },

  onTapBookSlot(e) {
    const side = e.currentTarget.dataset.side
    this.setData({
      showBookPicker: true,
      pickerSide: side,
      keyword: ''
    })
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  selectBook(e) {
    const book = e.currentTarget.dataset.book
    const side = this.data.pickerSide
    const otherSide = side === 'left' ? 'right' : 'left'
    const otherId = side === 'left' ? this.data.selectedRightId : this.data.selectedLeftId

    if (book.id === otherId) {
      wx.showToast({ title: '不能选择同一本书', icon: 'none' })
      return
    }

    this.setData({
      [`selected${side === 'left' ? 'Left' : 'Right'}Id`]: book.id,
      [`${side}Book`]: book,
      showBookPicker: false
    })
    this.updatePairName()
  },

  swapBooks() {
    const { leftBook, rightBook, selectedLeftId, selectedRightId, leftLanguage, rightLanguage } = this.data
    if (!leftBook || !rightBook) return
    this.setData({
      selectedLeftId: selectedRightId,
      selectedRightId: selectedLeftId,
      leftBook: rightBook,
      rightBook: leftBook,
      leftLanguage: rightLanguage,
      rightLanguage: leftLanguage
    })
    this.updatePairName()
  },

  onLeftLanguageChange(e) {
    const idx = e.detail.value
    this.setData({ leftLanguage: this.data.languageOptions[idx].key })
  },

  onRightLanguageChange(e) {
    const idx = e.detail.value
    this.setData({ rightLanguage: this.data.languageOptions[idx].key })
  },

  onPairNameInput(e) {
    this.setData({ pairName: e.detail.value })
  },

  selectAlignment(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ alignmentStrategy: key })
  },

  closeBookPicker() {
    this.setData({ showBookPicker: false })
  },

  async submitPair() {
    const { selectedLeftId, selectedRightId, leftLanguage, rightLanguage, alignmentStrategy, pairName } = this.data
    if (!selectedLeftId || !selectedRightId) {
      wx.showToast({ title: '请选择两本书', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const res = await request({
        url: '/bilingual/pairs',
        method: 'POST',
        data: {
          leftBookId: selectedLeftId,
          rightBookId: selectedRightId,
          leftLanguage,
          rightLanguage,
          alignmentStrategy,
          name: pairName
        }
      })
      wx.showToast({ title: '关联创建成功', icon: 'success' })
      const pairId = res.data?.id
      setTimeout(() => {
        if (alignmentStrategy === 2) {
          wx.showModal({
            title: '是否启动AI对齐？',
            content: 'AI段落对齐需要一些时间进行分析，是否现在启动？',
            confirmText: '启动',
            cancelText: '稍后',
            success: (r) => {
              if (r.confirm) {
                this.startAiAlignment(pairId)
              } else {
                this.goReader(pairId)
              }
            }
          })
        } else {
          this.goReader(pairId)
        }
      }, 800)
    } catch (e) {
      console.error('创建关联失败', e)
      wx.showToast({ title: e.message || '创建失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async startAiAlignment(pairId) {
    try {
      await request({
        url: `/bilingual/pairs/${pairId}/ai-align`,
        method: 'POST'
      })
      wx.showToast({ title: 'AI对齐已启动', icon: 'none' })
    } catch (e) {
      console.warn('启动AI对齐失败', e)
    }
    this.goReader(pairId)
  },

  goReader(pairId) {
    wx.redirectTo({
      url: `/pages/bilingual-reader/bilingual-reader?pairId=${pairId}`
    })
  },

  openExistingPair(e) {
    const pair = e.currentTarget.dataset.pair
    wx.navigateTo({
      url: `/pages/bilingual-reader/bilingual-reader?pairId=${pair.id}`
    })
  },

  async deletePair(e) {
    e.stopPropagation && e.stopPropagation()
    const pair = e.currentTarget.dataset.pair
    wx.showModal({
      title: '删除关联',
      content: `确定删除"${pair.name}"吗？`,
      success: async (r) => {
        if (r.confirm) {
          try {
            await request({
              url: `/bilingual/pairs/${pair.id}`,
              method: 'DELETE'
            })
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadAllPairs()
          } catch (e) {
            console.error('删除失败', e)
          }
        }
      }
    })
  }
})
