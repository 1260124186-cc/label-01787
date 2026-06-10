const { request } = require('../../utils/request')

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0GB'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb >= 1 ? gb.toFixed(1) + 'GB' : (bytes / (1024 * 1024)).toFixed(0) + 'MB'
}

Page({
  data: {
    quota: {},
    plans: [],
    pointsInfo: {},
    exchangeDays: 1,
    exchangeGB: 1,
    freePlan: null,
    vipPlan: null,
    purchasing: false
  },

  onShow() {
    this.loadQuota()
    this.loadPlans()
    this.loadPoints()
  },

  async loadQuota() {
    try {
      const res = await request({ url: '/membership/quota' })
      this.setData({ quota: res.data })
    } catch (e) {
      console.error('加载配额失败', e)
    }
  },

  async loadPlans() {
    try {
      const res = await request({ url: '/membership/plans' })
      const plans = res.data || []
      const freePlan = plans.find(p => p.code === 'free') || {}
      const vipPlan = plans.find(p => p.code === 'vip') || {}
      this.setData({ plans, freePlan, vipPlan })
    } catch (e) {
      console.error('加载方案失败', e)
    }
  },

  async loadPoints() {
    try {
      const res = await request({ url: '/points/info' })
      this.setData({ pointsInfo: res.data || {} })
    } catch (e) {
      console.error('加载积分失败', e)
    }
  },

  formatPlanFeature(plan) {
    if (!plan) return []
    const features = []
    if (plan.maxBooks === 0) {
      features.push('无限书籍')
    } else {
      features.push(plan.maxBooks + '本书籍')
    }
    features.push(formatBytes(plan.maxStorage) + '存储')
    if (plan.aiDailyLimit === 0) {
      features.push('无限AI使用')
    } else {
      features.push('每日' + plan.aiDailyLimit + '次AI')
    }
    if (plan.priorityQueue === 1) {
      features.push('优先转图队列')
    }
    if (plan.advancedStats === 1) {
      features.push('高级统计报告')
    }
    return features
  },

  async confirmPay(orderNo) {
    try {
      const res = await request({
        url: '/membership/pay-callback',
        method: 'POST',
        data: { orderNo }
      })
      return res.data
    } catch (e) {
      console.error('支付确认失败', e)
      return null
    }
  },

  async purchaseVip() {
    if (this.data.purchasing) return
    const vipPlan = this.data.vipPlan
    if (!vipPlan || !vipPlan.id) {
      wx.showToast({ title: '暂无会员方案', icon: 'none' })
      return
    }
    this.setData({ purchasing: true })
    try {
      const res = await request({
        url: '/membership/order',
        method: 'POST',
        data: { planId: vipPlan.id, orderType: 1 }
      })
      const paymentData = res.data
      const orderNo = paymentData.orderNo

      const hasPaymentParams = paymentData.timeStamp && paymentData.nonceStr && paymentData.package && paymentData.paySign

      if (hasPaymentParams) {
        wx.requestPayment({
          timeStamp: paymentData.timeStamp,
          nonceStr: paymentData.nonceStr,
          package: paymentData.package,
          signType: paymentData.signType || 'RSA',
          paySign: paymentData.paySign,
          success: async () => {
            const result = await this.confirmPay(orderNo)
            if (result && result.activated) {
              wx.showToast({ title: '开通成功', icon: 'success' })
            } else {
              wx.showToast({ title: '支付确认中，请稍后查看', icon: 'none' })
            }
            this.loadQuota()
            this.loadPoints()
          },
          fail: (err) => {
            if (err.errMsg !== 'requestPayment:fail cancel') {
              wx.showToast({ title: '支付失败', icon: 'none' })
            }
          }
        })
      } else {
        wx.showModal({
          title: '确认支付',
          content: '当前为模拟支付模式，确认支付¥' + ((paymentData.amount || 0) / 100).toFixed(2) + '开通会员？',
          success: async (modalRes) => {
            if (modalRes.confirm) {
              const result = await this.confirmPay(orderNo)
              if (result && result.activated) {
                wx.showToast({ title: '开通成功', icon: 'success' })
              } else {
                wx.showToast({ title: '开通失败，请重试', icon: 'none' })
              }
              this.loadQuota()
              this.loadPoints()
            }
          }
        })
      }
    } catch (e) {
      console.error('开通会员失败', e)
    } finally {
      this.setData({ purchasing: false })
    }
  },

  async purchaseStorage() {
    if (this.data.purchasing) return
    const vipPlan = this.data.vipPlan
    if (!vipPlan || !vipPlan.id) {
      wx.showToast({ title: '暂无存储包方案', icon: 'none' })
      return
    }
    this.setData({ purchasing: true })
    try {
      const res = await request({
        url: '/membership/order',
        method: 'POST',
        data: { planId: vipPlan.id, orderType: 2, storageGB: 10 }
      })
      const paymentData = res.data
      const orderNo = paymentData.orderNo

      const hasPaymentParams = paymentData.timeStamp && paymentData.nonceStr && paymentData.package && paymentData.paySign

      if (hasPaymentParams) {
        wx.requestPayment({
          timeStamp: paymentData.timeStamp,
          nonceStr: paymentData.nonceStr,
          package: paymentData.package,
          signType: paymentData.signType || 'RSA',
          paySign: paymentData.paySign,
          success: async () => {
            const result = await this.confirmPay(orderNo)
            if (result && result.activated) {
              wx.showToast({ title: '购买成功', icon: 'success' })
            } else {
              wx.showToast({ title: '支付确认中，请稍后查看', icon: 'none' })
            }
            this.loadQuota()
          },
          fail: (err) => {
            if (err.errMsg !== 'requestPayment:fail cancel') {
              wx.showToast({ title: '支付失败', icon: 'none' })
            }
          }
        })
      } else {
        wx.showModal({
          title: '确认支付',
          content: '当前为模拟支付模式，确认支付¥' + ((paymentData.amount || 0) / 100).toFixed(2) + '购买存储包？',
          success: async (modalRes) => {
            if (modalRes.confirm) {
              const result = await this.confirmPay(orderNo)
              if (result && result.activated) {
                wx.showToast({ title: '购买成功', icon: 'success' })
              } else {
                wx.showToast({ title: '购买失败，请重试', icon: 'none' })
              }
              this.loadQuota()
            }
          }
        })
      }
    } catch (e) {
      console.error('购买存储包失败', e)
    } finally {
      this.setData({ purchasing: false })
    }
  },

  onExchangeDaysInput(e) {
    this.setData({ exchangeDays: Number(e.detail.value) || 1 })
  },

  onExchangeGBInput(e) {
    this.setData({ exchangeGB: Number(e.detail.value) || 1 })
  },

  async exchangeVipDays() {
    const { exchangeDays, pointsInfo } = this.data
    const cost = exchangeDays * 100
    if (cost > (pointsInfo.balance || 0)) {
      wx.showToast({ title: '积分不足', icon: 'none' })
      return
    }
    try {
      await request({
        url: '/points/exchange',
        method: 'POST',
        data: { exchangeType: 1, value: exchangeDays }
      })
      wx.showToast({ title: '兑换成功', icon: 'success' })
      this.loadQuota()
      this.loadPoints()
    } catch (e) {
      console.error('兑换会员天数失败', e)
    }
  },

  async exchangeStorage() {
    const { exchangeGB, pointsInfo } = this.data
    const cost = exchangeGB * 200
    if (cost > (pointsInfo.balance || 0)) {
      wx.showToast({ title: '积分不足', icon: 'none' })
      return
    }
    try {
      await request({
        url: '/points/exchange',
        method: 'POST',
        data: { exchangeType: 2, value: exchangeGB }
      })
      wx.showToast({ title: '兑换成功', icon: 'success' })
      this.loadQuota()
      this.loadPoints()
    } catch (e) {
      console.error('兑换存储空间失败', e)
    }
  },

  formatBytes(bytes) {
    return formatBytes(bytes)
  }
})
