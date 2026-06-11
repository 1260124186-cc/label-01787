const { THEMES, getSystemTheme, isValidTheme } = require('./utils/theme')

App({
  globalData: {
    baseUrl: 'http://localhost:8080/api/mp',
    token: '',
    userInfo: null,
    theme: 'white', // white | green | dark | sepia
    followSystemTheme: false,
    THEMES,
    loginPromise: null // 登录 Promise，防止重复登录
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    const followSystemTheme = wx.getStorageSync('followSystemTheme') || false
    const storedTheme = wx.getStorageSync('theme') || 'white'

    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
    }

    this.globalData.followSystemTheme = followSystemTheme

    let finalTheme = storedTheme
    if (followSystemTheme) {
      finalTheme = getSystemTheme()
    }
    if (!isValidTheme(finalTheme)) {
      finalTheme = 'white'
    }
    this.globalData.theme = finalTheme

    try {
      wx.onThemeChange && wx.onThemeChange((res) => {
        if (this.globalData.followSystemTheme) {
          const sysTheme = res.theme === 'dark' ? 'dark' : 'white'
          this.globalData.theme = sysTheme
          wx.setStorageSync('theme', sysTheme)
        }
      })
    } catch (e) {
      console.warn('onThemeChange not supported', e)
    }
  },

  /** 微信登录 */
  login() {
    // 防止重复登录
    if (this.globalData.loginPromise) {
      return this.globalData.loginPromise
    }

    this.globalData.loginPromise = new Promise((resolve, reject) => {
      wx.login({
        success: (loginRes) => {
          wx.request({
            url: `${this.globalData.baseUrl}/login`,
            method: 'POST',
            data: { code: loginRes.code },
            success: (res) => {
              if (res.data.code === 200) {
                const data = res.data.data
                this.globalData.token = data.token
                this.globalData.userInfo = data
                wx.setStorageSync('token', data.token)
                wx.setStorageSync('userInfo', data)
                resolve(data)
              } else {
                reject(res.data.message)
              }
            },
            fail: reject
          })
        },
        fail: reject
      })
    }).finally(() => {
      this.globalData.loginPromise = null
    })

    return this.globalData.loginPromise
  },

  /** 检查登录状态，返回是否已登录 */
  checkLogin() {
    return new Promise((resolve, reject) => {
      if (this.globalData.token) {
        resolve(this.globalData.userInfo)
      } else {
        reject(new Error('未登录'))
      }
    })
  },

  /** 更新用户信息（头像、昵称） */
  updateUserInfo(userInfo) {
    this.globalData.userInfo = { ...this.globalData.userInfo, ...userInfo }
    wx.setStorageSync('userInfo', this.globalData.userInfo)
  },

  /** 设置主题 */
  setTheme(theme) {
    if (!isValidTheme(theme)) return
    this.globalData.theme = theme
    this.globalData.followSystemTheme = false
    wx.setStorageSync('theme', theme)
    wx.setStorageSync('followSystemTheme', false)
  },

  /** 设置是否跟随系统深色模式 */
  setFollowSystemTheme(follow) {
    this.globalData.followSystemTheme = follow
    wx.setStorageSync('followSystemTheme', follow)
    if (follow) {
      const sysTheme = getSystemTheme()
      if (isValidTheme(sysTheme)) {
        this.globalData.theme = sysTheme
        wx.setStorageSync('theme', sysTheme)
      }
    }
  },

  /** 获取当前生效的主题配置 */
  getCurrentTheme() {
    return THEMES[this.globalData.theme] || THEMES.white
  }
})
