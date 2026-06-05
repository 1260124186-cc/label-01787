const { request } = require('../../utils/request')

Page({
  data: {
    categories: []
  },

  onShow() {
    this.loadCategories()
  },

  async loadCategories() {
    try {
      const res = await request({ url: '/categories' })
      this.setData({ categories: res.data || [] })
    } catch (e) {
      console.error('加载分类失败', e)
    }
  },

  addCategory() {
    wx.showModal({
      title: '新建分类',
      editable: true,
      placeholderText: '请输入分类名称',
      success: async (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          try {
            await request({
              url: '/categories',
              method: 'POST',
              data: { name: res.content.trim() }
            })
            wx.showToast({ title: '创建成功', icon: 'success' })
            this.loadCategories()
          } catch (e) {
            console.error('创建失败', e)
          }
        }
      }
    })
  },

  editCategory(e) {
    const { id, name } = e.currentTarget.dataset
    wx.showModal({
      title: '编辑分类',
      editable: true,
      placeholderText: '请输入分类名称',
      content: name,
      success: async (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          try {
            await request({
              url: `/categories/${id}`,
              method: 'PUT',
              data: { name: res.content.trim() }
            })
            wx.showToast({ title: '修改成功', icon: 'success' })
            this.loadCategories()
          } catch (e) {
            console.error('修改失败', e)
          }
        }
      }
    })
  },

  deleteCategory(e) {
    const { id, name } = e.currentTarget.dataset
    wx.showModal({
      title: '确认删除',
      content: `确定删除分类「${name}」？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({ url: `/categories/${id}`, method: 'DELETE' })
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadCategories()
          } catch (e) {
            console.error('删除失败', e)
          }
        }
      }
    })
  }
})
