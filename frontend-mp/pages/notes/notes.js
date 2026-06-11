const { request } = require('../../utils/request')

const COLOR_MAP = {
  yellow: { bg: '#FFF9E6', border: '#FFD93D', name: '黄色' },
  green: { bg: '#E8F5E9', border: '#4CAF50', name: '绿色' },
  pink: { bg: '#FCE4EC', border: '#E91E63', name: '粉色' }
}

Page({
  data: {
    notes: [],
    noteType: '',
    currentTag: '',
    allTags: [],
    showTagFilter: false,
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false,
    showExportMenu: false,
    books: [],
    selectedBookId: null,
    generatingImage: false,
    searchKeyword: '',
    showBookFilter: false
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.refreshNotes()
      this.loadTags()
      this.loadBooks()
    } else {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.refreshNotes()
        this.loadTags()
        this.loadBooks()
      }).catch(() => {
        this.setData({ isLogin: false, notes: [] })
      })
    }
  },

  async loadBooks() {
    try {
      const res = await request({ url: '/books', data: { page: 1, size: 100 } })
      this.setData({ books: res.data.records || [] })
    } catch (e) {
      console.error('加载书籍失败', e)
    }
  },

  async loadTags() {
    if (!this.data.isLogin) return
    try {
      const res = await request({ url: '/annotations/tags' })
      this.setData({ allTags: res.data || [] })
    } catch (e) {
      console.error('加载标签失败', e)
    }
  },

  async refreshNotes() {
    if (!this.data.isLogin) return
    this.setData({ page: 1, hasMore: true, notes: [] })
    await this.loadNotes()
  },

  async loadNotes() {
    if (this.data.loading || !this.data.isLogin) return
    this.setData({ loading: true })
    try {
      const params = { page: this.data.page, size: 20 }
      if (this.data.noteType !== '') params.type = this.data.noteType
      if (this.data.currentTag) params.tag = this.data.currentTag
      if (this.data.selectedBookId) params.bookId = this.data.selectedBookId
      if (this.data.searchKeyword) params.keyword = this.data.searchKeyword
      const res = await request({ url: '/annotations', data: params })
      const records = res.data.records || []
      const processed = records.map(item => {
        const tagList = item.tags ? item.tags.split(',').filter(t => t.trim()) : []
        const colorInfo = COLOR_MAP[item.color] || COLOR_MAP.yellow
        return {
          ...item,
          tagList,
          colorInfo,
          isPinned: item.isPinned === 1
        }
      })
      this.setData({
        notes: this.data.page === 1 ? processed : [...this.data.notes, ...processed],
        page: this.data.page + 1,
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载笔记失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadNotes()
  },

  setFilter(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ noteType: type === '' ? '' : Number(type) })
    this.refreshNotes()
  },

  toggleTagFilter() {
    this.setData({ showTagFilter: !this.data.showTagFilter })
  },

  selectTag(e) {
    const tag = e.currentTarget.dataset.tag
    this.setData({ currentTag: tag === this.data.currentTag ? '' : tag, showTagFilter: false })
    this.refreshNotes()
  },

  toggleExportMenu() {
    this.setData({ showExportMenu: !this.data.showExportMenu })
  },

  selectBookForExport(e) {
    const bookId = e.currentTarget.dataset.id
    this.setData({ selectedBookId: bookId, showExportMenu: false })
    this.refreshNotes()
  },

  toggleBookFilter() {
    this.setData({ showBookFilter: !this.data.showBookFilter, showExportMenu: false })
  },

  selectBookFilter(e) {
    const bookId = e.currentTarget.dataset.id
    this.setData({
      selectedBookId: bookId === this.data.selectedBookId ? null : bookId,
      showBookFilter: false
    })
    this.refreshNotes()
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
  },

  onSearchConfirm() {
    this.refreshNotes()
  },

  clearSearch() {
    this.setData({ searchKeyword: '' })
    this.refreshNotes()
  },

  jumpToReader(e) {
    const note = e.currentTarget.dataset.note
    if (!note || !note.bookId) return
    const bookId = note.bookId
    const pageNum = note.pageNum
    const selectedText = note.selectedText || ''
    const highlight = encodeURIComponent(selectedText)
    const matchStart = selectedText ? 0 : 0
    const matchEnd = selectedText ? selectedText.length : 0
    wx.navigateTo({
      url: `/pages/reader/reader?id=${bookId}&page=${pageNum}&highlight=${highlight}&matchStart=${matchStart}&matchEnd=${matchEnd}`
    })
  },

  async exportMarkdown() {
    if (!this.data.selectedBookId) {
      wx.showToast({ title: '请先选择一本书', icon: 'none' })
      return
    }
    try {
      wx.showLoading({ title: '导出中...' })
      const res = await request({
        url: '/annotations/export/markdown',
        data: { bookId: this.data.selectedBookId }
      })
      const markdown = res.data
      wx.setClipboardData({
        data: markdown,
        success: () => {
          wx.hideLoading()
          wx.showToast({ title: '已复制到剪贴板', icon: 'success' })
        }
      })
    } catch (e) {
      wx.hideLoading()
      console.error('导出失败', e)
      wx.showToast({ title: '导出失败', icon: 'none' })
    }
  },

  async generateNoteImage() {
    if (!this.data.selectedBookId) {
      wx.showToast({ title: '请先选择一本书', icon: 'none' })
      return
    }
    try {
      this.setData({ generatingImage: true, showExportMenu: false })
      wx.showLoading({ title: '生成中...' })

      const [annotationsRes, bookRes] = await Promise.all([
        request({ url: `/annotations/book/${this.data.selectedBookId}/all` }),
        request({ url: `/books/${this.data.selectedBookId}` })
      ])

      const annotations = annotationsRes.data || []
      const book = bookRes.data || {}

      if (annotations.length === 0) {
        wx.hideLoading()
        wx.showToast({ title: '该书暂无笔记', icon: 'none' })
        return
      }

      this.drawNoteImage(book, annotations)
    } catch (e) {
      wx.hideLoading()
      this.setData({ generatingImage: false })
      console.error('生成图片失败', e)
      wx.showToast({ title: '生成失败', icon: 'none' })
    }
  },

  drawNoteImage(book, annotations) {
    const query = wx.createSelectorQuery()
    query.select('#noteCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) {
          wx.hideLoading()
          this.setData({ generatingImage: false })
          wx.showToast({ title: 'Canvas 初始化失败', icon: 'none' })
          return
        }

        const canvas = res[0].node
        const ctx = canvas.getContext('2d')

        const dpr = wx.getSystemInfoSync().pixelRatio
        const contentWidth = 600
        const padding = 40
        const headerHeight = 120
        const cardGap = 30
        const cardMinHeight = 120

        let totalHeight = headerHeight + padding * 2
        const cards = []

        const pageMap = {}
        annotations.forEach(ann => {
          if (!pageMap[ann.pageNum]) pageMap[ann.pageNum] = []
          pageMap[ann.pageNum].push(ann)
        })

        Object.keys(pageMap).sort((a, b) => a - b).forEach(pageNum => {
          const pageAnns = pageMap[pageNum]
          let pageCardHeight = 60

          pageAnns.forEach(ann => {
            let annHeight = 20
            if (ann.selectedText) {
              const textLines = this.calcTextLines(ctx, ann.selectedText, contentWidth - padding * 2 - 20, 24)
              annHeight += textLines * 32 + 20
            }
            if (ann.content) {
              const contentLines = this.calcTextLines(ctx, ann.content, contentWidth - padding * 2, 26)
              annHeight += contentLines * 38
            }
            if (ann.tags) {
              annHeight += 40
            }
            pageCardHeight += annHeight + 16
          })

          cards.push({ pageNum, annotations: pageAnns, height: Math.max(cardMinHeight, pageCardHeight) })
          totalHeight += Math.max(cardMinHeight, pageCardHeight) + cardGap
        })

        totalHeight += 60

        canvas.width = contentWidth * dpr
        canvas.height = totalHeight * dpr
        ctx.scale(dpr, dpr)

        ctx.fillStyle = '#F7F3EE'
        ctx.fillRect(0, 0, contentWidth, totalHeight)

        ctx.fillStyle = '#FFFFFF'
        ctx.fillRect(padding, padding, contentWidth - padding * 2, headerHeight - padding)

        ctx.fillStyle = '#6B4226'
        ctx.font = 'bold 32px sans-serif'
        ctx.textAlign = 'left'
        ctx.fillText(book.title || '读书笔记', padding + 24, padding + 50)

        ctx.fillStyle = '#999'
        ctx.font = '22px sans-serif'
        const authorText = book.author ? `作者：${book.author}` : ''
        const countText = `共 ${annotations.length} 条笔记`
        ctx.fillText(authorText + (authorText ? ' · ' : '') + countText, padding + 24, padding + 85)

        let yOffset = headerHeight + padding

        cards.forEach(card => {
          const cardX = padding
          const cardY = yOffset
          const cardW = contentWidth - padding * 2
          const cardH = card.height

          ctx.fillStyle = '#FFFFFF'
          this.roundRect(ctx, cardX, cardY, cardW, cardH, 16)
          ctx.fill()

          ctx.fillStyle = '#6B4226'
          ctx.font = 'bold 26px sans-serif'
          ctx.fillText(`第 ${card.pageNum} 页`, cardX + 24, cardY + 40)

          let annY = cardY + 60

          card.annotations.forEach(ann => {
            const colorInfo = COLOR_MAP[ann.color] || COLOR_MAP.yellow

            if (ann.selectedText) {
              ctx.fillStyle = colorInfo.bg
              this.roundRect(ctx, cardX + 24, annY, cardW - 48, 0, 8)
              const textLines = this.calcTextLines(ctx, ann.selectedText, cardW - 68, 24)
              const quoteHeight = textLines * 32 + 20
              this.roundRect(ctx, cardX + 24, annY, cardW - 48, quoteHeight, 8)
              ctx.fill()

              ctx.fillStyle = '#666'
              ctx.font = '24px sans-serif'
              this.wrapText(ctx, ann.selectedText, cardX + 36, annY + 28, cardW - 68, 32)

              annY += quoteHeight + 12
            }

            if (ann.content) {
              ctx.fillStyle = '#333'
              ctx.font = '26px sans-serif'
              const contentLines = this.calcTextLines(ctx, ann.content, cardW - 48, 26)
              this.wrapText(ctx, ann.content, cardX + 24, annY + 26, cardW - 48, 38)
              annY += contentLines * 38 + 10
            }

            if (ann.tags && ann.tags.trim()) {
              const tags = ann.tags.split(',').filter(t => t.trim())
              let tagX = cardX + 24
              tags.forEach(tag => {
                ctx.fillStyle = '#F5F0EB'
                this.roundRect(ctx, tagX, annY, 16 + tag.length * 22, 32, 16)
                ctx.fill()
                ctx.fillStyle = '#6B4226'
                ctx.font = '20px sans-serif'
                ctx.fillText('#' + tag.trim(), tagX + 8, annY + 22)
                tagX += 16 + tag.length * 22 + 10
              })
              annY += 40
            }

            annY += 16
          })

          yOffset += cardH + cardGap
        })

        ctx.fillStyle = '#BBB'
        ctx.font = '20px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('—— 由小安的书店生成 ——', contentWidth / 2, totalHeight - 30)

        wx.canvasToTempFilePath({
          canvas,
          success: (res) => {
            wx.hideLoading()
            this.setData({ generatingImage: false })
            wx.previewImage({
              urls: [res.tempFilePath],
              current: res.tempFilePath
            })
          },
          fail: (err) => {
            wx.hideLoading()
            this.setData({ generatingImage: false })
            console.error('生成图片失败', err)
            wx.showToast({ title: '生成失败', icon: 'none' })
          }
        })
      })
  },

  calcTextLines(ctx, text, maxWidth, fontSize) {
    if (!text) return 0
    ctx.font = `${fontSize}px sans-serif`
    let lines = 0
    let currentLine = ''
    for (let i = 0; i < text.length; i++) {
      const char = text[i]
      const testLine = currentLine + char
      const metrics = ctx.measureText(testLine)
      if (metrics.width > maxWidth && i > 0) {
        lines++
        currentLine = char
      } else {
        currentLine = testLine
      }
    }
    if (currentLine) lines++
    return lines
  },

  wrapText(ctx, text, x, y, maxWidth, lineHeight) {
    const chars = text.split('')
    let line = ''
    let currentY = y

    for (let i = 0; i < chars.length; i++) {
      const testLine = line + chars[i]
      const metrics = ctx.measureText(testLine)
      if (metrics.width > maxWidth && i > 0) {
        ctx.fillText(line, x, currentY)
        line = chars[i]
        currentY += lineHeight
      } else {
        line = testLine
      }
    }
    if (line) {
      ctx.fillText(line, x, currentY)
    }
  },

  roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath()
    ctx.moveTo(x + r, y)
    ctx.arcTo(x + w, y, x + w, y + h, r)
    ctx.arcTo(x + w, y + h, x, y + h, r)
    ctx.arcTo(x, y + h, x, y, r)
    ctx.arcTo(x, y, x + w, y, r)
    ctx.closePath()
  },

  deleteNote(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认删除',
      content: '确定删除该笔记？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({ url: `/annotations/${id}`, method: 'DELETE' })
            wx.showToast({ title: '已删除', icon: 'success' })
            this.refreshNotes()
            this.loadTags()
          } catch (e) {
            console.error('删除失败', e)
          }
        }
      }
    })
  },

  editNote(e) {
    const index = e.currentTarget.dataset.index
    const note = this.data.notes[index]
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '编辑笔记',
      editable: true,
      placeholderText: '修改内容',
      content: note.content,
      success: async (res) => {
        if (res.confirm && res.content) {
          try {
            await request({
              url: `/annotations/${id}`,
              method: 'PUT',
              data: {
                bookId: note.bookId,
                pageNum: note.pageNum,
                content: res.content,
                type: note.type,
                tags: note.tags,
                isPinned: note.isPinned ? 1 : 0,
                color: note.color
              }
            })
            wx.showToast({ title: '已更新', icon: 'success' })
            this.refreshNotes()
          } catch (e) {
            console.error('更新失败', e)
          }
        }
      }
    })
  },

  async togglePin(e) {
    const id = e.currentTarget.dataset.id
    try {
      await request({ url: `/annotations/${id}/pin`, method: 'PUT' })
      wx.showToast({ title: '操作成功', icon: 'success' })
      this.refreshNotes()
    } catch (e) {
      console.error('置顶失败', e)
    }
  },

  changeColor(e) {
    const id = e.currentTarget.dataset.id
    const colors = ['yellow', 'green', 'pink']
    wx.showActionSheet({
      itemList: ['黄色', '绿色', '粉色'],
      success: async (res) => {
        try {
          await request({
            url: `/annotations/${id}/color`,
            method: 'PUT',
            data: { color: colors[res.tapIndex] }
          })
          wx.showToast({ title: '已更新', icon: 'success' })
          this.refreshNotes()
        } catch (err) {
          console.error('更新颜色失败', err)
        }
      }
    })
  },

  shareExcerpt(e) {
    const index = e.currentTarget.dataset.index
    const note = this.data.notes[index]
    if (!note.selectedText) {
      wx.showToast({ title: '无书摘内容', icon: 'none' })
      return
    }
    if (note.type !== 1) {
      wx.showToast({ title: '仅评语类型可发布到广场', icon: 'none' })
      return
    }
    wx.showActionSheet({
      itemList: ['发布到广场', '复制分享'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.publishToPlaza(note)
        } else if (res.tapIndex === 1) {
          this.copyExcerpt(note)
        }
      }
    })
  },

  copyExcerpt(note) {
    wx.showModal({
      title: '分享书摘',
      content: `"${note.selectedText}"\n\n——来自小安的书店`,
      confirmText: '复制文字',
      success: (res) => {
        if (res.confirm) {
          wx.setClipboardData({
            data: `"${note.selectedText}"\n\n——来自小安的书店`,
            success: () => {
              wx.showToast({ title: '已复制到剪贴板', icon: 'success' })
            }
          })
        }
      }
    })
  },

  publishToPlaza(note) {
    wx.showModal({
      title: '发布到广场',
      content: '发布前请确认：\n\n1. 您分享的书摘内容拥有合法版权或属于合理使用范围\n2. 请勿分享完整章节或大量原文内容\n3. 分享内容将对所有用户公开可见\n\n是否确认发布？',
      confirmText: '确认发布',
      cancelText: '再想想',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: '/plaza/publish',
              method: 'POST',
              data: {
                annotationId: note.id,
                commentText: note.content
              }
            })
            wx.showToast({ title: '发布成功', icon: 'success' })
          } catch (e) {
            console.error('发布失败', e)
            wx.showToast({ title: e.message || '发布失败', icon: 'none' })
          }
        }
      }
    })
  },

  onShareAppMessage() {
    return {
      title: '小安的书店 - 读书笔记',
      path: '/pages/bookshelf/bookshelf'
    }
  }
})
