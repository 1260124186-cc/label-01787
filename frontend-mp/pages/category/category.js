const { request } = require('../../utils/request')

const PRESET_COLORS = [
  '#6B4226', '#E8734A', '#F5A623', '#F8E71C',
  '#7ED321', '#417505', '#4A90D9', '#50E3C2',
  '#9013FE', '#BD10E0', '#D0021B', '#F56C6C',
  '#5063AA', '#333333', '#8E8E93', '#C7C7CC'
]

const PRESET_ICONS = [
  'folder', 'book', 'heart', 'star',
  'bookmark', 'note', 'palette', 'lamp',
  'globe', 'code', 'music', 'camera',
  'travel', 'food', 'sport', 'work'
]

Page({
  data: {
    categories: [],
    isSortMode: false,
    sortList: [],
    movedIndex: -1,
    showColorPicker: false,
    showIconPicker: false,
    editingCategory: null,
    editName: '',
    editColor: '',
    editIcon: '',
    presetColors: PRESET_COLORS,
    presetIcons: PRESET_ICONS
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
    this.setData({
      editingCategory: null,
      editName: '',
      editColor: '#6B4226',
      editIcon: 'folder',
      showColorPicker: false,
      showIconPicker: false
    })
    this.showEditModal('新建分类')
  },

  editCategory(e) {
    const { id, name, color, icon } = e.currentTarget.dataset
    this.setData({
      editingCategory: { id },
      editName: name,
      editColor: color || '#6B4226',
      editIcon: icon || 'folder',
      showColorPicker: false,
      showIconPicker: false
    })
    this.showEditModal('编辑分类')
  },

  showEditModal(title) {
    const self = this
    const { editName, editColor, editIcon, presetColors, presetIcons, showColorPicker, showIconPicker } = self.data

    let colorChips = presetColors.map(c => {
      const sel = c === editColor ? '●' : '○'
      return sel + ' ' + c
    }).join('  ')

    let iconChips = presetIcons.map(i => {
      const sel = i === editIcon ? '✓' : ''
      return sel + i
    }).join('  ')

    wx.showModal({
      title: title,
      editable: true,
      placeholderText: '请输入分类名称',
      content: editName,
      confirmText: '保存',
      success: async (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          try {
            const dto = {
              name: res.content.trim(),
              color: self.data.editColor,
              icon: self.data.editIcon
            }
            if (self.data.editingCategory) {
              await request({
                url: `/categories/${self.data.editingCategory.id}`,
                method: 'PUT',
                data: dto
              })
              wx.showToast({ title: '修改成功', icon: 'success' })
            } else {
              await request({
                url: '/categories',
                method: 'POST',
                data: dto
              })
              wx.showToast({ title: '创建成功', icon: 'success' })
            }
            self.loadCategories()
          } catch (e) {
            console.error('操作失败', e)
          }
        }
      }
    })
  },

  toggleSortMode() {
    const isSortMode = !this.data.isSortMode
    if (isSortMode) {
      const sortList = this.data.categories.map((c, i) => ({ ...c, _sortIndex: i }))
      this.setData({ isSortMode: true, sortList })
    } else {
      this.setData({ isSortMode: false, sortList: [], movedIndex: -1 })
    }
  },

  onSortLongPress() {
    if (!this.data.isSortMode) {
      this.toggleSortMode()
    }
  },

  moveUp(e) {
    const idx = e.currentTarget.dataset.index
    if (idx <= 0) return
    const list = [...this.data.sortList]
    const temp = list[idx]
    list[idx] = list[idx - 1]
    list[idx - 1] = temp
    this.setData({ sortList: list, movedIndex: idx - 1 })
  },

  moveDown(e) {
    const idx = e.currentTarget.dataset.index
    const list = [...this.data.sortList]
    if (idx >= list.length - 1) return
    const temp = list[idx]
    list[idx] = list[idx + 1]
    list[idx + 1] = temp
    this.setData({ sortList: list, movedIndex: idx + 1 })
  },

  async saveSort() {
    const sortList = this.data.sortList.map((c, i) => ({
      id: c.id,
      sortOrder: i
    }))
    try {
      await request({
        url: '/categories/sort',
        method: 'PUT',
        data: sortList
      })
      wx.showToast({ title: '排序已保存', icon: 'success' })
      this.setData({ isSortMode: false, sortList: [], movedIndex: -1 })
      this.loadCategories()
    } catch (e) {
      console.error('保存排序失败', e)
    }
  },

  cancelSort() {
    this.setData({ isSortMode: false, sortList: [], movedIndex: -1 })
  },

  async deleteCategory(e) {
    const { id, name } = e.currentTarget.dataset
    try {
      const countRes = await request({ url: `/categories/${id}/book-count` })
      const bookCount = countRes.data.count || 0

      if (bookCount > 0) {
        wx.showModal({
          title: '删除分类',
          content: `分类「${name}」下有 ${bookCount} 本书，是否将书籍移至未分类并删除？`,
          confirmText: '移至未分类',
          cancelText: '取消删除',
          success: async (res) => {
            if (res.confirm) {
              try {
                await request({
                  url: `/categories/${id}?moveToUncategorized=true`,
                  method: 'DELETE'
                })
                wx.showToast({ title: '已删除', icon: 'success' })
                this.loadCategories()
              } catch (e) {
                console.error('删除失败', e)
              }
            }
          }
        })
      } else {
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
    } catch (e) {
      console.error('查询书籍数量失败', e)
    }
  }
})
