const { request } = require('../../utils/request')
const { themes } = require('../../utils/util')
const { getUnreadCount } = require('../../utils/notification')

Page({
  data: {
    userInfo: {},
    isLogin: false,
    showTheme: false,
    showNicknamePanel: false,
    currentTheme: 'white',
    themeText: '白色',
    tempNickname: '',
    unreadCount: 0
  },

  onShow() {
    const app = getApp()
    const theme = app.globalData.theme || 'white'
    this.setData({
      currentTheme: theme,
      themeText: themes[theme]?.name || '白色'
    })

    if (app.globalData.token) {
      this.setData({ isLogin: true })
      this.loadProfile()
      this.fetchUnreadCount()
    } else {
      this.setData({
        isLogin: false,
        userInfo: { nickname: '未登录' },
        unreadCount: 0
      })
    }
  },

  async fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      this.setData({ unreadCount: res.data[0] || 0 })
    } catch (e) {
      console.error('获取未读数量失败', e)
    }
  },

  async loadProfile() {
    try {
      const res = await request({ url: '/user/profile' })
      this.setData({ userInfo: res.data })
    } catch (e) {
      console.error('加载用户信息失败', e)
    }
  },

  doLogin() {
    const app = getApp()
    wx.showLoading({ title: '登录中...' })
    app.login().then((data) => {
      wx.hideLoading()
      wx.showToast({ title: '登录成功', icon: 'success' })
      this.setData({ isLogin: true })
      this.loadProfile()
      this.fetchUnreadCount()
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '登录失败', icon: 'none' })
    })
  },

  goNotifications() {
    wx.switchTab({
      url: '/pages/notifications/notifications'
    })
  },

  /** 显示昵称修改面板 */
  editNickname() {
    if (!this.data.isLogin) {
      this.doLogin()
      return
    }
    this.setData({
      showNicknamePanel: true,
      tempNickname: this.data.userInfo.nickname || ''
    })
  },

  hideNicknamePanel() {
    this.setData({ showNicknamePanel: false })
  },

  /** 昵称输入回调 */
  onNicknameInput(e) {
    this.setData({ tempNickname: e.detail.value })
  },

  /** 保存昵称 */
  async saveNickname() {
    const { tempNickname } = this.data
    if (!tempNickname.trim()) {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }

    wx.showLoading({ title: '保存中...' })
    try {
      await request({
        url: '/user/profile',
        method: 'PUT',
        data: {
          nickname: tempNickname.trim()
        }
      })

      const app = getApp()
      app.updateUserInfo({ nickname: tempNickname.trim() })

      wx.hideLoading()
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.setData({ showNicknamePanel: false })
      this.loadProfile()
    } catch (e) {
      wx.hideLoading()
      console.error('保存失败', e)
      wx.showToast({ title: '保存失败', icon: 'none' })
    }
  },

  goCategory() {
    wx.navigateTo({ url: '/pages/category/category' })
  },

  goComplaint() {
    wx.navigateTo({ url: '/pages/complaint/complaint' })
  },

  goBackup() {
    wx.navigateTo({ url: '/pages/backup/backup' })
  },

  goMembership() {
    wx.navigateTo({ url: '/pages/membership/membership' })
  },

  goPoints() {
    wx.navigateTo({ url: '/pages/points/points' })
  },

  goPlaza() {
    if (!this.data.isLogin) {
      this.doLogin()
      return
    }
    wx.navigateTo({ url: '/pages/plaza/plaza' })
  },

  goMyExcerpts() {
    if (!this.data.isLogin) {
      this.doLogin()
      return
    }
    wx.navigateTo({ url: '/pages/my-excerpts/my-excerpts' })
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
      currentTheme: theme,
      themeText: themes[theme]?.name || '白色',
      showTheme: false
    })
    wx.showToast({ title: '主题已切换', icon: 'success' })
  },

  editNickname() {
    if (!this.data.isLogin) {
      this.doLogin()
      return
    }
    this.setData({
      showNicknamePanel: true,
      tempNickname: this.data.userInfo.nickname || ''
    })
  },

  /** 退出登录 */
  doLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          const app = getApp()
          app.globalData.token = ''
          app.globalData.userInfo = null
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          this.setData({
            isLogin: false,
            userInfo: { nickname: '未登录' }
          })
          wx.showToast({ title: '已退出登录', icon: 'success' })
        }
      }
    })
  }
})
