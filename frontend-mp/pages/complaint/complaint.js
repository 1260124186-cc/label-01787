const { request } = require('../../utils/request')

Page({
  data: {
    complainantName: '',
    complainantContact: '',
    bookTitle: '',
    reason: '',
    evidenceUrls: '',
    submitting: false
  },

  onNameInput(e) {
    this.setData({ complainantName: e.detail.value })
  },

  onContactInput(e) {
    this.setData({ complainantContact: e.detail.value })
  },

  onBookTitleInput(e) {
    this.setData({ bookTitle: e.detail.value })
  },

  onReasonInput(e) {
    this.setData({ reason: e.detail.value })
  },

  onEvidenceInput(e) {
    this.setData({ evidenceUrls: e.detail.value })
  },

  async submitComplaint() {
    const { complainantName, complainantContact, bookTitle, reason, evidenceUrls } = this.data

    if (!complainantName.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    if (!complainantContact.trim()) {
      wx.showToast({ title: '请输入联系方式', icon: 'none' })
      return
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请输入申诉原因', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      await request({
        url: '/compliance/complaint',
        method: 'POST',
        data: {
          complainantName: complainantName.trim(),
          complainantContact: complainantContact.trim(),
          bookTitle: bookTitle.trim(),
          reason: reason.trim(),
          evidenceUrls: evidenceUrls.trim()
        }
      })
      wx.showToast({ title: '申诉已提交', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (e) {
      console.error('提交申诉失败', e)
    } finally {
      this.setData({ submitting: false })
    }
  }
})
