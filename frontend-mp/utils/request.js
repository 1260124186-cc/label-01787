const app = getApp()

/**
 * 封装请求
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data, header = {} } = options

    if (app.globalData.token) {
      header['Authorization'] = `Bearer ${app.globalData.token}`
    }
    header['Content-Type'] = header['Content-Type'] || 'application/json'

    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method,
      data,
      header,
      success: (res) => {
        if (res.data.code === 200) {
          resolve(res.data)
        } else if (res.data.code === 401) {
          // token 过期，重新登录
          wx.removeStorageSync('token')
          app.globalData.token = ''
          app.login().then(() => {
            // 重试
            request(options).then(resolve).catch(reject)
          }).catch(reject)
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 上传文件
 */
function uploadFile(filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    // 检查 token 是否存在
    if (!app.globalData.token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      reject(new Error('未登录'))
      return
    }

    wx.uploadFile({
      url: `${app.globalData.baseUrl}/books/upload`,
      filePath,
      name: 'file',
      formData,
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        // 检查 HTTP 状态码
        if (res.statusCode !== 200) {
          wx.showToast({ title: `服务器错误: ${res.statusCode}`, icon: 'none' })
          reject(new Error(`HTTP ${res.statusCode}`))
          return
        }
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data)
          } else if (data.code === 401) {
            // token 过期，尝试重新登录
            wx.removeStorageSync('token')
            app.globalData.token = ''
            wx.showToast({ title: '登录已过期，请重试', icon: 'none' })
            reject(data)
          } else {
            wx.showToast({ title: data.message || '上传失败', icon: 'none' })
            reject(data)
          }
        } catch (e) {
          console.error('解析响应失败', res.data)
          wx.showToast({ title: '服务器响应异常', icon: 'none' })
          reject(e)
        }
      },
      fail: (err) => {
        console.error('上传请求失败', err)
        wx.showToast({ title: '网络错误，上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request, uploadFile }
