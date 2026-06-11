const { request } = require('../../utils/request')

Page({
  data: {
    notes: [],
    noteType: '',
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false
  },

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.refreshNotes()
    } else {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.refreshNotes()
      }).catch(() => {
        this.setData({ isLogin: false, notes: [] })
      })
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
      const res = await request({ url: '/annotations', data: params })
      const records = res.data.records || []
      this.setData({
        notes: this.data.page === 1 ? records : [...this.data.notes, ...records],
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
                type: note.type
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
  }
})
