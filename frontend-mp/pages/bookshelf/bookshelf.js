const { request, uploadFile } = require('../../utils/request')
const { formatSize, formatDuration } = require('../../utils/util')

const CATEGORY_STORAGE_KEY = 'bookshelf_last_category'
const VIEW_STORAGE_KEY = 'bookshelf_view_mode'
const SORT_STORAGE_KEY = 'bookshelf_sort_by'

Page({
  data: {
    books: [],
    categories: [],
    currentCategory: '',
    categoryMap: {},
    continueList: [],
    loading: false,
    page: 1,
    hasMore: true,
    isLogin: false,
    showCopyrightModal: false,
    pendingFilePath: '',
    pendingFileName: '',
    copyrightDeclared: false,
    searchKeyword: '',
    searchFocus: false,
    sortBy: 'upload_time',
    sortOptions: [
      { key: 'upload_time', label: '上传时间' },
      { key: 'last_read', label: '最近阅读' },
      { key: 'title', label: '书名' }
    ],
    showSortPicker: false,
    viewMode: 'grid',
    batchMode: false,
    selectedIds: [],
    selectedCount: 0,
    showBatchMoveModal: false,
    moveCategoryId: '',
    showEditMetaModal: false,
    uploadedBook: null,
    editTitle: '',
    editAuthor: ''
  },

  pendingFilePath: '',
  pendingFileName: '',
  searchTimer: null,

  noop() {},

  onShow() {
    const app = getApp()
    if (app.globalData.token) {
      this.setData({ isLogin: true })
      const savedCategory = wx.getStorageSync(CATEGORY_STORAGE_KEY) || ''
      const savedView = wx.getStorageSync(VIEW_STORAGE_KEY) || 'grid'
      const savedSort = wx.getStorageSync(SORT_STORAGE_KEY) || 'upload_time'
      this.setData({
        currentCategory: savedCategory,
        viewMode: savedView,
        sortBy: savedSort,
        batchMode: false,
        selectedIds: [],
        selectedCount: 0
      })
      this.loadCategories()
      this.loadContinueReading()
      if (!this.data.showCopyrightModal && !this.data.showEditMetaModal) {
        this.refreshBooks()
      }
    } else {
      this.setData({ isLogin: false, books: [], continueList: [] })
    }
  },

  onPullDownRefresh() {
    this.refreshBooks().then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.batchMode) {
      this.loadBooks()
    }
  },

  async loadContinueReading() {
    if (!this.data.isLogin) return
    try {
      const res = await request({
        url: '/reading/continue-list',
        data: { limit: 5 }
      })
      const list = (res.data || []).map(item => {
        const format = item.bookFormat || 'pdf'
        let progress = 0
        if (format === 'epub') {
          const total = Math.max(1, item.chapterCount || 0)
          const last = item.lastChapter || 0
          progress = total > 0 ? Math.round((last / total) * 100) : 0
        } else {
          const total = Math.max(1, item.pageCount || 0)
          const last = item.lastPage || 0
          progress = total > 0 ? Math.round((last / total) * 100) : 0
        }
        return {
          ...item,
          progress,
          durationText: formatDuration(item.totalDuration)
        }
      })
      this.setData({ continueList: list })
    } catch (e) {
      console.error('加载继续阅读列表失败', e)
    }
  },

  openContinueBook(e) {
    const id = e.currentTarget.dataset.id
    const page = e.currentTarget.dataset.page || 1
    const chapter = e.currentTarget.dataset.chapter || 0
    const format = e.currentTarget.dataset.format || 'pdf'
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}&page=${page}&chapter=${chapter}&format=${format}` })
  },

  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })
    if (this.searchTimer) clearTimeout(this.searchTimer)
    this.searchTimer = setTimeout(() => {
      this.refreshBooks()
    }, 300)
  },

  onSearchClear() {
    this.setData({ searchKeyword: '', searchFocus: false })
    this.refreshBooks()
  },

  onSearchFocus() {
    this.setData({ searchFocus: true })
  },

  onSearchBlur() {
    this.setData({ searchFocus: false })
  },

  goSearch() {},

  toggleSortPicker() {
    this.setData({ showSortPicker: !this.data.showSortPicker })
  },

  selectSort(e) {
    const sortBy = e.currentTarget.dataset.key
    this.setData({ sortBy, showSortPicker: false })
    wx.setStorageSync(SORT_STORAGE_KEY, sortBy)
    this.refreshBooks()
  },

  toggleViewMode() {
    const viewMode = this.data.viewMode === 'grid' ? 'list' : 'grid'
    this.setData({ viewMode })
    wx.setStorageSync(VIEW_STORAGE_KEY, viewMode)
  },

  async refreshBooks() {
    if (!this.data.isLogin) return
    this.setData({ page: 1, hasMore: true, books: [] })
    await this.loadBooks()
  },

  async loadBooks() {
    if (this.data.loading || !this.data.isLogin) return
    this.setData({ loading: true })
    try {
      const params = {
        page: this.data.page,
        size: 20,
        sortBy: this.data.sortBy
      }
      if (this.data.currentCategory) {
        params.categoryId = this.data.currentCategory
      }
      if (this.data.searchKeyword && this.data.searchKeyword.trim()) {
        params.keyword = this.data.searchKeyword.trim()
      }
      const res = await request({ url: '/books', data: params })
      const categoryMap = this.data.categoryMap
      const records = (res.data.records || []).map(b => {
        const cat = categoryMap[b.categoryId]
        const format = b.bookFormat || 'pdf'
        let progress = 0
        if (format === 'epub') {
          const total = Math.max(1, b.chapterCount || 0)
          const last = b.lastChapter || 0
          progress = Math.min(100, Math.round((last / total) * 100))
        } else {
          const total = Math.max(1, b.pageCount || 0)
          const last = b.lastPage || 0
          progress = Math.min(100, Math.round((last / total) * 100))
        }
        return {
          ...b,
          fileSizeText: formatSize(b.fileSize),
          categoryColor: cat ? cat.color : '',
          categoryName: cat ? cat.name : '',
          progress,
          selected: this.data.selectedIds.indexOf(b.id) >= 0
        }
      })
      this.setData({
        books: this.data.page === 1 ? records : [...this.data.books, ...records],
        page: this.data.page + 1,
        hasMore: records.length >= 20
      })
    } catch (e) {
      console.error('加载书籍失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadCategories() {
    if (!this.data.isLogin) return
    try {
      const res = await request({ url: '/categories' })
      const categories = res.data || []
      const categoryMap = {}
      categories.forEach(c => {
        categoryMap[c.id] = c
      })
      this.setData({ categories, categoryMap })

      const savedCategory = this.data.currentCategory
      if (savedCategory) {
        const exists = categories.some(c => String(c.id) === String(savedCategory))
        if (!exists) {
          this.setData({ currentCategory: '' })
          wx.setStorageSync(CATEGORY_STORAGE_KEY, '')
        }
      }

      if (this.data.books.length > 0) {
        const books = this.data.books.map(b => {
          const cat = categoryMap[b.categoryId]
          return {
            ...b,
            categoryColor: cat ? cat.color : '',
            categoryName: cat ? cat.name : ''
          }
        })
        this.setData({ books })
      }
    } catch (e) {
      console.error('加载分类失败', e)
    }
  },

  filterCategory(e) {
    const id = e.currentTarget.dataset.id || ''
    this.setData({ currentCategory: id })
    wx.setStorageSync(CATEGORY_STORAGE_KEY, id)
    this.refreshBooks()
  },

  goCategory() {
    wx.navigateTo({ url: '/pages/category/category' })
  },

  openBook(e) {
    if (this.data.batchMode) {
      this.toggleSelect(e)
      return
    }
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/book-detail/book-detail?id=${id}` })
  },

  goReader(e) {
    if (this.data.batchMode) return
    e.stopPropagation && e.stopPropagation()
    const id = e.currentTarget.dataset.id
    const format = e.currentTarget.dataset.format || 'pdf'
    wx.navigateTo({ url: `/pages/reader/reader?id=${id}&format=${format}` })
  },

  toggleSelect(e) {
    const id = e.currentTarget.dataset.id
    let selectedIds = [...this.data.selectedIds]
    const idx = selectedIds.indexOf(id)
    if (idx >= 0) {
      selectedIds.splice(idx, 1)
    } else {
      selectedIds.push(id)
    }
    const books = this.data.books.map(b => ({
      ...b,
      selected: selectedIds.indexOf(b.id) >= 0
    }))
    this.setData({
      selectedIds,
      selectedCount: selectedIds.length,
      books
    })
  },

  toggleBatchMode() {
    this.setData({
      batchMode: !this.data.batchMode,
      selectedIds: [],
      selectedCount: 0
    })
    if (this.data.batchMode) {
      const books = this.data.books.map(b => ({ ...b, selected: false }))
      this.setData({ books })
    }
  },

  selectAll() {
    const total = this.data.books.length
    const selectedAll = this.data.selectedCount === total
    let selectedIds = []
    if (!selectedAll) {
      selectedIds = this.data.books.map(b => b.id)
    }
    const books = this.data.books.map(b => ({
      ...b,
      selected: !selectedAll
    }))
    this.setData({
      selectedIds,
      selectedCount: selectedIds.length,
      books
    })
  },

  showBatchMove() {
    if (this.data.selectedCount === 0) {
      wx.showToast({ title: '请先选择书籍', icon: 'none' })
      return
    }
    this.setData({ showBatchMoveModal: true, moveCategoryId: '' })
  },

  selectMoveCategory(e) {
    const id = e.currentTarget.dataset.id || ''
    this.setData({ moveCategoryId: id })
  },

  async confirmBatchMove() {
    if (this.data.selectedCount === 0) {
      wx.showToast({ title: '请先选择书籍', icon: 'none' })
      return
    }
    try {
      wx.showLoading({ title: '移动中...' })
      await request({
        url: '/books/batch-move-category',
        method: 'POST',
        data: {
          bookIds: this.data.selectedIds,
          categoryId: this.data.moveCategoryId ? Number(this.data.moveCategoryId) : null
        }
      })
      wx.hideLoading()
      wx.showToast({ title: '移动成功', icon: 'success' })
      this.setData({
        showBatchMoveModal: false,
        batchMode: false,
        selectedIds: [],
        selectedCount: 0
      })
      this.refreshBooks()
    } catch (e) {
      wx.hideLoading()
      console.error('批量移动失败', e)
    }
  },

  cancelBatchMove() {
    this.setData({ showBatchMoveModal: false })
  },

  async confirmBatchDelete() {
    if (this.data.selectedCount === 0) {
      wx.showToast({ title: '请先选择书籍', icon: 'none' })
      return
    }
    wx.showModal({
      title: '批量删除',
      content: `确定将 ${this.data.selectedCount} 本书移入回收站？7 天内可恢复。`,
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '删除中...' })
            await request({
              url: '/books/batch-delete',
              method: 'POST',
              data: { bookIds: this.data.selectedIds }
            })
            wx.hideLoading()
            wx.showToast({ title: '已移入回收站', icon: 'success' })
            this.setData({
              batchMode: false,
              selectedIds: [],
              selectedCount: 0
            })
            this.refreshBooks()
          } catch (e) {
            wx.hideLoading()
            console.error('批量删除失败', e)
          }
        }
      }
    })
  },

  showBookAction(e) {
    if (this.data.batchMode) return
    const index = e.currentTarget.dataset.index
    const book = this.data.books[index]
    const actions = ['查看详情', '编辑信息', '删除书籍']
    wx.showActionSheet({
      itemList: actions,
      success: async (res) => {
        if (res.tapIndex === 0) {
          wx.navigateTo({ url: `/pages/book-detail/book-detail?id=${book.id}` })
        } else if (res.tapIndex === 1) {
          wx.navigateTo({ url: `/pages/book-edit/book-edit?id=${book.id}` })
        } else if (res.tapIndex === 2) {
          wx.showModal({
            title: '确认删除',
            content: `确定删除《${book.title}》？可在回收站中 7 天内恢复。`,
            success: async (modalRes) => {
              if (modalRes.confirm) {
                try {
                  await request({ url: `/books/${book.id}`, method: 'DELETE' })
                  wx.showToast({ title: '已移入回收站', icon: 'success' })
                  this.refreshBooks()
                } catch (e) {
                  console.error('删除失败', e)
                }
              }
            }
          })
        }
      }
    })
  },

  goTrash() {
    wx.navigateTo({ url: '/pages/trash/trash' })
  },

  uploadBook() {
    const app = getApp()
    if (!app.globalData.token) {
      app.checkLogin().then(() => {
        this.setData({ isLogin: true })
        this.doUpload()
      }).catch(() => {
        wx.showToast({ title: '请先登录', icon: 'none' })
      })
      return
    }
    this.doUpload()
  },

  doUpload() {
    const FORMAT_MAX_SIZE = {
      pdf: 150 * 1024 * 1024,
      epub: 100 * 1024 * 1024,
      mobi: 100 * 1024 * 1024,
      azw3: 100 * 1024 * 1024
    }
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf', 'epub', 'mobi', 'azw3'],
      success: (res) => {
        const file = res.tempFiles[0]
        if (!file || !file.path) {
          wx.showToast({ title: '文件信息不完整，请重新选择', icon: 'none' })
          return
        }
        if (file.size < 1 * 1024 * 1024) {
          wx.showToast({ title: '文件不能小于1MB', icon: 'none' })
          return
        }

        const fileName = file.name || ''
        const lowerName = fileName.toLowerCase()
        let format = 'pdf'
        if (lowerName.endsWith('.epub')) format = 'epub'
        else if (lowerName.endsWith('.mobi')) format = 'mobi'
        else if (lowerName.endsWith('.azw3')) format = 'azw3'

        const maxSize = FORMAT_MAX_SIZE[format] || FORMAT_MAX_SIZE.pdf
        const maxSizeMB = Math.round(maxSize / (1024 * 1024))
        if (file.size > maxSize) {
          wx.showToast({ title: `${format.toUpperCase()}文件不能超过${maxSizeMB}MB`, icon: 'none' })
          return
        }

        let finalFileName = fileName
        if (!finalFileName) {
          const pathParts = file.path.split('/')
          finalFileName = pathParts[pathParts.length - 1] || '未命名文件'
        }

        this.pendingFilePath = file.path
        this.pendingFileName = finalFileName

        this.setData({
          showCopyrightModal: true,
          pendingFilePath: file.path,
          pendingFileName: finalFileName,
          copyrightDeclared: false
        })
      }
    })
  },

  toggleCopyright() {
    if (!this.pendingFilePath || !this.pendingFileName) {
      wx.showToast({ title: '文件信息已丢失，请重新选择', icon: 'none' })
      this.setData({ showCopyrightModal: false, copyrightDeclared: false })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      return
    }
    this.setData({ copyrightDeclared: !this.data.copyrightDeclared })
  },

  confirmUpload() {
    const { copyrightDeclared } = this.data
    if (!this.pendingFilePath || !this.pendingFileName) {
      wx.showToast({ title: '文件信息已丢失，请重新选择', icon: 'none' })
      this.setData({ showCopyrightModal: false, copyrightDeclared: false })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      return
    }
    if (!copyrightDeclared) {
      wx.showToast({ title: '请先勾选版权声明', icon: 'none' })
      return
    }

    let fileName = this.pendingFileName
    if (!fileName) {
      const pathParts = this.pendingFilePath.split('/')
      fileName = pathParts[pathParts.length - 1] || '未命名文件'
    }

    this.setData({ showCopyrightModal: false })
    wx.showLoading({ title: '上传中...' })
    const title = fileName
      .replace(/\.pdf$/i, '')
      .replace(/\.epub$/i, '')
      .replace(/\.mobi$/i, '')
      .replace(/\.azw3$/i, '')
    uploadFile(this.pendingFilePath, { title, copyrightDeclared: copyrightDeclared ? '1' : '0' }).then((res) => {
      wx.hideLoading()
      wx.showToast({ title: '上传成功', icon: 'success' })
      const uploadedBook = res.data || null
      this.pendingFilePath = ''
      this.pendingFileName = ''
      this.setData({
        pendingFilePath: '',
        pendingFileName: '',
        copyrightDeclared: false,
        uploadedBook,
        editTitle: uploadedBook ? uploadedBook.title : '',
        editAuthor: uploadedBook ? uploadedBook.author : '',
        showEditMetaModal: !!uploadedBook
      })
      this.refreshBooks()
    }).catch((err) => {
      console.error('上传失败', err)
      wx.hideLoading()
      wx.showToast({ title: err && err.message ? err.message : '上传失败', icon: 'none' })
      this.pendingFilePath = ''
      this.pendingFileName = ''
      this.setData({ pendingFilePath: '', pendingFileName: '', copyrightDeclared: false })
    })
  },

  onEditTitleInput(e) {
    this.setData({ editTitle: e.detail.value })
  },

  onEditAuthorInput(e) {
    this.setData({ editAuthor: e.detail.value })
  },

  async confirmEditMeta() {
    const { uploadedBook, editTitle, editAuthor } = this.data
    if (!uploadedBook) {
      this.setData({ showEditMetaModal: false })
      return
    }
    if (!editTitle || !editTitle.trim()) {
      wx.showToast({ title: '书名不能为空', icon: 'none' })
      return
    }
    try {
      wx.showLoading({ title: '保存中...' })
      await request({
        url: `/books/${uploadedBook.id}`,
        method: 'PUT',
        data: {
          title: editTitle.trim(),
          author: editAuthor ? editAuthor.trim() : ''
        }
      })
      wx.hideLoading()
      wx.showToast({ title: '已保存', icon: 'success' })
      this.setData({
        showEditMetaModal: false,
        uploadedBook: null,
        editTitle: '',
        editAuthor: ''
      })
      this.refreshBooks()
    } catch (e) {
      wx.hideLoading()
      console.error('保存失败', e)
    }
  },

  skipEditMeta() {
    this.setData({
      showEditMetaModal: false,
      uploadedBook: null,
      editTitle: '',
      editAuthor: ''
    })
  },

  cancelUpload() {
    this.pendingFilePath = ''
    this.pendingFileName = ''
    this.setData({ showCopyrightModal: false, pendingFilePath: '', pendingFileName: '', copyrightDeclared: false })
  }
})
