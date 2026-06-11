const { request } = require('../../utils/request')

const TYPE_MAP = {
  1: { name: '摘要', loading: '正在生成摘要...' },
  2: { name: '解释', loading: '正在解释内容...' },
  3: { name: '翻译', loading: '正在翻译...' },
  4: { name: '出题自测', loading: '正在生成题目...' },
  5: { name: '章节大纲', loading: '正在生成大纲...' },
  6: { name: '知识卡片', loading: '正在生成知识卡片...' }
}

const SOURCE_TYPE_MAP = {
  1: '选中段落',
  2: '当前页',
  3: '全书'
}

Page({
  data: {
    bookId: null,
    book: null,
    currentPage: 1,
    activeTab: 'quick',
    sourceType: 1,
    sourceText: '',
    customPrompt: '',
    targetLang: '英文',
    showCustomPrompt: false,
    showLangSelector: false,
    showDisclaimer: true,
    showCopyrightModal: false,
    showResult: false,
    resultTitle: '',
    currentResult: '',
    loading: false,
    loadingText: '',
    sessions: [],
    activeSessionId: '',
    chatList: [],
    page: 1,
    hasMore: true,
    showChatMenu: false,
    selectedChatId: null,
    pendingFunctionType: null
  },

  onLoad(options) {
    const bookId = Number(options.id) || 0
    const page = Number(options.page) || 1
    this.setData({ bookId, currentPage: page })
    
    if (bookId) {
      this.loadBookInfo()
      this.checkCopyrightStatus()
    }
    
    if (options.text) {
      this.setData({ 
        sourceType: 1,
        sourceText: decodeURIComponent(options.text)
      })
    }
  },

  onShow() {
    if (this.data.activeTab === 'history') {
      this.loadSessions()
    }
  },

  onPullDownRefresh() {
    if (this.data.activeTab === 'history') {
      this.loadSessions()
    }
    wx.stopPullDownRefresh()
  },

  async loadBookInfo() {
    try {
      const res = await request({ url: `/books/${this.data.bookId}` })
      this.setData({ book: res.data })
    } catch (e) {
      console.error('加载书籍信息失败', e)
    }
  },

  async checkCopyrightStatus() {
    try {
      const res = await request({ url: `/books/${this.data.bookId}` })
      const book = res.data
      if (!book.copyrightDeclared || !book.copyrightAgreedAt) {
        this.setData({ showCopyrightModal: true })
      }
    } catch (e) {
      console.error('检查版权状态失败', e)
    }
  },

  hideCopyrightModal() {
    this.setData({ showCopyrightModal: false })
    wx.navigateBack()
  },

  async agreeCopyright() {
    try {
      await request({
        url: `/ai/copyright/agree/${this.data.bookId}`,
        method: 'POST'
      })
      wx.showToast({ title: '已确认版权声明', icon: 'success' })
      this.setData({ showCopyrightModal: false })
      if (this.data.pendingFunctionType) {
        const type = this.data.pendingFunctionType
        this.setData({ pendingFunctionType: null })
        this.executeFunction(type)
      }
    } catch (e) {
      console.error('确认版权声明失败', e)
      wx.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    if (tab === 'history') {
      this.loadSessions()
    }
  },

  selectSource(e) {
    const type = Number(e.currentTarget.dataset.type)
    this.setData({ sourceType: type })
  },

  onSourceTextInput(e) {
    this.setData({ sourceText: e.detail.value })
  },

  onPromptInput(e) {
    this.setData({ customPrompt: e.detail.value })
  },

  selectLang(e) {
    const lang = e.currentTarget.dataset.lang
    this.setData({ targetLang: lang })
  },

  handleFunction(e) {
    const type = Number(e.currentTarget.dataset.type)
    this.setData({
      showCustomPrompt: type === 2,
      showLangSelector: type === 3
    })
    this.checkAndExecute(type)
  },

  handleBookFunction(e) {
    const type = Number(e.currentTarget.dataset.type)
    this.setData({
      showCustomPrompt: false,
      showLangSelector: false
    })
    this.checkAndExecute(type)
  },

  checkAndExecute(type) {
    if (!this.data.bookId) {
      wx.showToast({ title: '请先选择书籍', icon: 'none' })
      return
    }

    if (this.data.sourceType === 1 && !this.data.sourceText.trim() && type <= 4) {
      wx.showToast({ title: '请输入要处理的文本', icon: 'none' })
      return
    }

    if (!this.data.book || !this.data.book.copyrightDeclared || !this.data.book.copyrightAgreedAt) {
      this.setData({ 
        showCopyrightModal: true,
        pendingFunctionType: type
      })
      return
    }

    this.executeFunction(type)
  },

  async executeFunction(type) {
    const typeInfo = TYPE_MAP[type]
    this.setData({
      loading: true,
      loadingText: typeInfo.loading
    })

    try {
      let sourceText = this.data.sourceText
      if (this.data.sourceType === 2 && type <= 4) {
        const textRes = await request({ 
          url: `/books/${this.data.bookId}/text/${this.data.currentPage}` 
        })
        sourceText = textRes.data || ''
      }

      const sourceType = type >= 5 ? 3 : this.data.sourceType

      const res = await request({
        url: '/ai/chat',
        method: 'POST',
        data: {
          bookId: this.data.bookId,
          type,
          sourceType,
          sourceText,
          pageNum: this.data.currentPage,
          userPrompt: this.data.customPrompt,
          targetLanguage: this.data.targetLang
        }
      })

      this.setData({
        showResult: true,
        resultTitle: typeInfo.name,
        currentResult: res.data.aiResponse
      })

      if (this.data.activeTab === 'history') {
        this.loadSessions()
      }
    } catch (e) {
      console.error('AI处理失败', e)
      wx.showToast({ title: e.message || '处理失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  hideResult() {
    this.setData({ showResult: false })
  },

  copyResult() {
    wx.setClipboardData({
      data: this.data.currentResult,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success' })
      }
    })
  },

  async loadSessions() {
    try {
      const res = await request({ url: '/ai/chat/sessions' })
      const sessions = (res.data || []).map(item => ({
        ...item,
        lastActiveText: this.formatTime(item.lastActive)
      }))
      this.setData({ sessions })
    } catch (e) {
      console.error('加载会话列表失败', e)
    }
  },

  selectSession(e) {
    const session = e.currentTarget.dataset.session
    this.setData({
      activeSessionId: session.sessionId,
      currentBookId: session.bookId,
      page: 1,
      chatList: [],
      hasMore: true
    })
    this.loadChatHistory(session.bookId)
  },

  async loadChatHistory(bookId) {
    this.setData({ loading: true, loadingText: '加载中...' })
    try {
      const res = await request({ 
        url: `/ai/chat/book/${bookId}?page=${this.data.page}&size=20` 
      })
      const records = res.data.records || []
      const chatList = records.map(item => ({
        ...item,
        typeText: TYPE_MAP[item.type]?.name || '未知',
        sourceText: item.pageNum ? `第${item.pageNum}页 · ${SOURCE_TYPE_MAP[item.sourceType]}` : SOURCE_TYPE_MAP[item.sourceType],
        createdAtText: this.formatTime(item.createdAt)
      }))

      this.setData({
        chatList: this.data.page === 1 ? chatList : [...this.data.chatList, ...chatList],
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载对话历史失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  loadMoreHistory() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ page: this.data.page + 1 })
    this.loadChatHistory(this.data.currentBookId)
  },

  showChatMenu(e) {
    const id = e.currentTarget.dataset.id
    this.setData({
      showChatMenu: true,
      selectedChatId: id
    })
  },

  hideChatMenu() {
    this.setData({ showChatMenu: false, selectedChatId: null })
  },

  async deleteChat() {
    if (!this.data.selectedChatId) return
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条对话记录吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: `/ai/chat/${this.data.selectedChatId}`,
              method: 'DELETE'
            })
            wx.showToast({ title: '已删除', icon: 'success' })
            this.setData({
              chatList: this.data.chatList.filter(item => item.id !== this.data.selectedChatId),
              showChatMenu: false,
              selectedChatId: null
            })
            this.loadSessions()
          } catch (e) {
            console.error('删除失败', e)
            wx.showToast({ title: '删除失败', icon: 'none' })
          }
        } else {
          this.hideChatMenu()
        }
      }
    })
  },

  clearSessionChats() {
    if (!this.data.currentBookId) return
    
    wx.showModal({
      title: '确认清空',
      content: '确定要清空这本书的所有对话记录吗？此操作不可恢复。',
      confirmColor: '#ff4d4f',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: `/ai/chat/book/${this.data.currentBookId}`,
              method: 'DELETE'
            })
            wx.showToast({ title: '已清空', icon: 'success' })
            this.setData({
              chatList: [],
              activeSessionId: '',
              currentBookId: null
            })
            this.loadSessions()
          } catch (e) {
            console.error('清空失败', e)
            wx.showToast({ title: '清空失败', icon: 'none' })
          }
        }
      }
    })
  },

  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now - date
    const day = 24 * 60 * 60 * 1000

    if (diff < day) {
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${hours}:${minutes}`
    } else if (diff < 7 * day) {
      const days = Math.floor(diff / day)
      return `${days}天前`
    } else {
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const dayNum = date.getDate().toString().padStart(2, '0')
      return `${month}-${dayNum}`
    }
  }
})
