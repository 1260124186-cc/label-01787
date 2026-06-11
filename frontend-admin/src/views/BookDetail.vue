<template>
  <div class="page-container">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" style="margin-right: 12px;">返回</el-button>
      <h2>书籍详情</h2>
    </div>

    <el-card v-if="book" style="margin-bottom: 16px;">
      <div class="book-info">
        <div class="book-cover" v-if="book.cover">
          <img :src="book.cover" :alt="book.title" />
        </div>
        <div class="book-cover placeholder" v-else>
          <el-icon :size="64"><Reading /></el-icon>
        </div>
        <div class="book-meta">
          <h3 class="book-title">{{ book.title }}</h3>
          <div class="book-meta-grid">
            <div class="meta-item">
              <span class="meta-label">作者</span>
              <span class="meta-value">{{ book.author || '未知' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">格式</span>
              <span class="meta-value">
                <el-tag :type="getFormatTagType(book.bookFormat)" size="small">{{ (book.bookFormat || 'pdf').toUpperCase() }}</el-tag>
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">页数/章节</span>
              <span class="meta-value">{{ book.bookFormat === 'epub' ? (book.chapterCount || 0) + '章' : (book.pageCount || 0) + '页' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">文件大小</span>
              <span class="meta-value">{{ formatSize(book.fileSize) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">ISBN</span>
              <span class="meta-value">{{ book.isbn || '无' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">状态</span>
              <span class="meta-value">
                <el-tag v-if="book.status === 1" type="success" size="small">正常</el-tag>
                <el-tag v-else-if="book.status === 2" type="warning" size="small">已下架</el-tag>
                <el-tag v-else type="danger" size="small">已删除</el-tag>
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">上传时间</span>
              <span class="meta-value">{{ book.createdAt }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">总批注数</span>
              <span class="meta-value highlight">{{ totalAnnotations }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card>
      <div class="card-header">
        <h3>批注分布预览</h3>
        <div class="distribution-legend">
          <span class="legend-item">
            <span class="legend-dot level-0"></span>0
          </span>
          <span class="legend-item">
            <span class="legend-dot level-1"></span>1-5
          </span>
          <span class="legend-item">
            <span class="legend-dot level-2"></span>6-20
          </span>
          <span class="legend-item">
            <span class="legend-dot level-3"></span>21-50
          </span>
          <span class="legend-item">
            <span class="legend-dot level-4"></span>50+
          </span>
        </div>
      </div>

      <div class="distribution-toolbar">
        <el-input
          v-model="pageSearchKeyword"
          placeholder="搜索批注内容、书摘"
          clearable
          style="width: 300px"
          prefix-icon="Search"
          @keyup.enter="searchInAnnotations"
        />
        <el-button type="primary" @click="searchInAnnotations" style="margin-left: 12px;">
          搜索批注
        </el-button>
        <span class="tip-text">点击页码查看该页批注详情</span>
      </div>

      <div class="distribution-grid" v-loading="distributionLoading">
        <div
          v-for="page in displayPages"
          :key="page.pageNum"
          class="page-cell"
          :class="getPageCellClass(page.count)"
          @click="viewPageAnnotations(page.pageNum)"
        >
          <div class="page-num">第{{ page.pageNum }}页</div>
          <div class="page-count" v-if="page.count > 0">{{ page.count }}条</div>
        </div>
        <div class="page-cell empty" v-if="displayPages.length === 0 && !distributionLoading">
          暂无批注分布数据
        </div>
      </div>

      <div class="pagination-wrap" v-if="totalPages > pageSize">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="totalPages"
          :page-size="pageSize"
          v-model:current-page="distributionPage"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="pageDetailVisible"
      :title="`第${selectedPageNum}页批注详情`"
      width="900px"
      destroy-on-close
    >
      <div class="page-detail-content">
        <div class="page-detail-header">
          <span>共 {{ pageAnnotations.length }} 条批注</span>
          <el-select v-model="pageDetailTypeFilter" size="small" style="width: 140px" @change="filterPageAnnotations">
            <el-option label="全部类型" :value="null" />
            <el-option label="评语" :value="1" />
            <el-option label="笔记" :value="2" />
          </el-select>
        </div>

        <div class="annotation-list" v-loading="pageDetailLoading">
          <div v-for="item in filteredPageAnnotations" :key="item.id" class="annotation-card">
            <div class="annotation-header">
              <div class="annotation-type">
                <el-tag :type="item.type === 1 ? 'warning' : 'success'" size="small">
                  {{ item.type === 1 ? '评语' : '笔记' }}
                </el-tag>
                <span class="annotation-user">用户ID: {{ item.userId }}</span>
              </div>
              <div class="annotation-meta">
                <el-tag :type="getColorTagType(item.color)" size="small" effect="plain">
                  {{ getColorName(item.color) }}
                </el-tag>
                <span class="annotation-time">{{ item.createdAt }}</span>
              </div>
            </div>

            <div class="annotation-selected" v-if="item.selectedText">
              <span class="quote-mark">"</span>
              <span class="selected-text">{{ item.selectedText }}</span>
            </div>

            <div class="annotation-content" v-if="item.content">
              {{ item.content }}
            </div>

            <div class="annotation-actions">
              <el-button size="small" type="danger" link @click="handleDeleteAnnotation(item)">
                删除批注
              </el-button>
            </div>
          </div>

          <el-empty
            v-if="!pageDetailLoading && filteredPageAnnotations.length === 0"
            description="该页暂无批注"
          />
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="searchResultVisible"
      title="批注搜索结果"
      width="900px"
      destroy-on-close
    >
      <div class="search-result-content" v-loading="searchLoading">
        <div class="search-result-header">
          搜索关键词: <el-tag type="primary">{{ pageSearchKeyword }}</el-tag>
          <span class="result-count">共 {{ searchResults.length }} 条结果</span>
        </div>

        <div class="annotation-list">
          <div v-for="item in searchResults" :key="item.id" class="annotation-card">
            <div class="annotation-header">
              <div class="annotation-type">
                <el-tag :type="item.type === 1 ? 'warning' : 'success'" size="small">
                  {{ item.type === 1 ? '评语' : '笔记' }}
                </el-tag>
                <span class="annotation-user">用户ID: {{ item.userId }}</span>
                <span class="annotation-page">第{{ item.pageNum }}页</span>
              </div>
              <div class="annotation-meta">
                <span class="annotation-time">{{ item.createdAt }}</span>
              </div>
            </div>

            <div class="annotation-selected" v-if="item.selectedText">
              <span class="quote-mark">"</span>
              <span class="selected-text" v-html="highlightKeyword(item.selectedText)"></span>
            </div>

            <div class="annotation-content" v-if="item.content">
              <span v-html="highlightKeyword(item.content)"></span>
            </div>

            <div class="annotation-actions">
              <el-button size="small" type="primary" link @click="jumpToPageAnnotation(item)">
                查看该页
              </el-button>
              <el-button size="small" type="danger" link @click="handleDeleteAnnotation(item)">
                删除批注
              </el-button>
            </div>
          </div>

          <el-empty
            v-if="!searchLoading && searchResults.length === 0"
            description="未找到匹配的批注"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Reading, Search } from '@element-plus/icons-vue'
import {
  getBookDetail,
  getBookAnnotationDistribution,
  getBookAnnotationsByPage,
  getBookAnnotations,
  deleteAnnotation
} from '@/api/admin'

const route = useRoute()
const router = useRouter()
const bookId = route.params.id

const book = ref(null)
const distributionLoading = ref(false)
const pageDetailLoading = ref(false)
const searchLoading = ref(false)

const annotationDistribution = ref([])
const totalAnnotations = ref(0)
const distributionPage = ref(1)
const pageSize = 100

const pageSearchKeyword = ref('')
const searchResults = ref([])
const searchResultVisible = ref(false)

const pageDetailVisible = ref(false)
const selectedPageNum = ref(0)
const pageAnnotations = ref([])
const pageDetailTypeFilter = ref(null)

const displayPages = computed(() => {
  if (!book.value || !annotationDistribution.value.length) {
    const totalPages = book.value?.pageCount || book.value?.chapterCount || 0
    const pages = []
    const start = (distributionPage.value - 1) * pageSize
    const end = Math.min(start + pageSize, totalPages)
    for (let i = start; i < end; i++) {
      pages.push({ pageNum: i + 1, count: 0 })
    }
    return pages
  }

  const distMap = new Map()
  annotationDistribution.value.forEach(item => {
    distMap.set(item.pageNum, item.count)
  })

  const totalPages = book.value?.pageCount || book.value?.chapterCount || 0
  const pages = []
  const start = (distributionPage.value - 1) * pageSize
  const end = Math.min(start + pageSize, totalPages)
  for (let i = start; i < end; i++) {
    pages.push({
      pageNum: i + 1,
      count: distMap.get(i + 1) || 0
    })
  }
  return pages
})

const totalPages = computed(() => {
  return book.value?.pageCount || book.value?.chapterCount || 0
})

const filteredPageAnnotations = computed(() => {
  if (!pageAnnotations.value.length) return []
  if (pageDetailTypeFilter.value === null) return pageAnnotations.value
  return pageAnnotations.value.filter(a => a.type === pageDetailTypeFilter.value)
})

function goBack() {
  router.back()
}

function getFormatTagType(format) {
  const map = { pdf: 'primary', epub: 'success', mobi: 'warning' }
  return map[format] || 'info'
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function getPageCellClass(count) {
  if (count === 0) return 'level-0'
  if (count <= 5) return 'level-1'
  if (count <= 20) return 'level-2'
  if (count <= 50) return 'level-3'
  return 'level-4'
}

function getColorTagType(color) {
  const map = { yellow: 'warning', green: 'success', pink: 'danger' }
  return map[color] || 'info'
}

function getColorName(color) {
  const map = { yellow: '黄色', green: '绿色', pink: '粉色' }
  return map[color] || color
}

function highlightKeyword(text) {
  if (!text || !pageSearchKeyword.value) return text
  const kw = pageSearchKeyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(kw, 'gi'), '<span class="highlight">$&</span>')
}

async function fetchBookDetail() {
  try {
    const res = await getBookDetail(bookId)
    book.value = res.data
  } catch (e) {
    ElMessage.error('加载书籍详情失败')
  }
}

async function fetchAnnotationDistribution() {
  distributionLoading.value = true
  try {
    const res = await getBookAnnotationDistribution(bookId)
    annotationDistribution.value = res.data || []
    totalAnnotations.value = annotationDistribution.value.reduce((sum, item) => sum + item.count, 0)
  } catch (e) {
    ElMessage.error('加载批注分布失败')
  } finally {
    distributionLoading.value = false
  }
}

async function viewPageAnnotations(pageNum) {
  selectedPageNum.value = pageNum
  pageDetailLoading.value = true
  try {
    const res = await getBookAnnotationsByPage(bookId, pageNum)
    pageAnnotations.value = res.data || []
    pageDetailVisible.value = true
  } catch (e) {
    ElMessage.error('加载页面批注失败')
  } finally {
    pageDetailLoading.value = false
  }
}

function filterPageAnnotations() {
}

async function searchInAnnotations() {
  if (!pageSearchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searchLoading.value = true
  try {
    const res = await getBookAnnotations(bookId, {
      keyword: pageSearchKeyword.value.trim(),
      size: 1000
    })
    searchResults.value = res.data?.records || []
    searchResultVisible.value = true
  } catch (e) {
    ElMessage.error('搜索批注失败')
  } finally {
    searchLoading.value = false
  }
}

function jumpToPageAnnotation(item) {
  searchResultVisible.value = false
  viewPageAnnotations(item.pageNum)
}

async function handleDeleteAnnotation(item) {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条批注吗？删除后无法恢复。',
      '删除确认',
      { type: 'warning' }
    )
    await deleteAnnotation(item.id)
    ElMessage.success('删除成功')

    if (pageDetailVisible.value) {
      pageAnnotations.value = pageAnnotations.value.filter(a => a.id !== item.id)
    }
    if (searchResultVisible.value) {
      searchResults.value = searchResults.value.filter(a => a.id !== item.id)
    }
    fetchAnnotationDistribution()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchBookDetail()
  fetchAnnotationDistribution()
})
</script>

<style lang="scss" scoped>
.book-info {
  display: flex;
  gap: 24px;

  .book-cover {
    width: 160px;
    height: 220px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
    background: #f5f5f5;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &.placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999;
    }
  }

  .book-meta {
    flex: 1;

    .book-title {
      font-size: 24px;
      margin: 0 0 16px 0;
      color: #303133;
    }

    .book-meta-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 16px;

      .meta-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .meta-label {
          font-size: 13px;
          color: #909399;
        }

        .meta-value {
          font-size: 14px;
          color: #303133;

          &.highlight {
            color: #409eff;
            font-weight: 600;
            font-size: 18px;
          }
        }
      }
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 18px;
  }

  .distribution-legend {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #606266;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .legend-dot {
      width: 12px;
      height: 12px;
      border-radius: 2px;
      display: inline-block;

      &.level-0 { background: #f0f0f0; }
      &.level-1 { background: #d1ecf1; }
      &.level-2 { background: #a5d6a7; }
      &.level-3 { background: #ffb74d; }
      &.level-4 { background: #ef5350; }
    }
  }
}

.distribution-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;

  .tip-text {
    margin-left: auto;
    color: #909399;
    font-size: 13px;
  }
}

.distribution-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;

  .page-cell {
    aspect-ratio: 3 / 4;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &.level-0 {
      background: #f0f0f0;
      color: #909399;
    }

    &.level-1 {
      background: #d1ecf1;
      color: #0c5460;
      border-color: #bee5eb;
    }

    &.level-2 {
      background: #a5d6a7;
      color: #2e7d32;
      border-color: #81c784;
    }

    &.level-3 {
      background: #ffb74d;
      color: #e65100;
      border-color: #ffa726;
    }

    &.level-4 {
      background: #ef5350;
      color: #fff;
      border-color: #f44336;
    }

    .page-num {
      font-size: 11px;
      text-align: center;
    }

    .page-count {
      font-size: 12px;
      font-weight: 600;
      margin-top: 4px;
    }

    &.empty {
      aspect-ratio: auto;
      grid-column: 1 / -1;
      padding: 60px;
      cursor: default;

      &:hover {
        transform: none;
        box-shadow: none;
      }
    }
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.page-detail-content,
.search-result-content {
  .page-detail-header,
  .search-result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    font-size: 14px;
    color: #606266;

    .result-count {
      color: #909399;
      font-size: 13px;
    }
  }

  .annotation-list {
    max-height: 500px;
    overflow-y: auto;
    padding-right: 8px;
  }

  .annotation-card {
    background: #fafafa;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 12px;
    border-left: 4px solid #409eff;

    .annotation-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .annotation-type {
        display: flex;
        align-items: center;
        gap: 10px;

        .annotation-user,
        .annotation-page {
          font-size: 12px;
          color: #909399;
        }
      }

      .annotation-meta {
        display: flex;
        align-items: center;
        gap: 10px;

        .annotation-time {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .annotation-selected {
      background: #fffbe6;
      padding: 10px 12px;
      border-radius: 4px;
      margin-bottom: 10px;
      font-size: 13px;
      color: #6b4423;
      border-left: 3px solid #d4a574;

      .quote-mark {
        color: #d4a574;
        font-weight: 600;
        margin-right: 4px;
      }

      .selected-text {
        .highlight {
          background: #ffeb3b;
          padding: 0 2px;
          border-radius: 2px;
        }
      }
    }

    .annotation-content {
      font-size: 14px;
      color: #303133;
      line-height: 1.6;
      margin-bottom: 10px;

      .highlight {
        background: #ffeb3b;
        padding: 0 2px;
        border-radius: 2px;
      }
    }

    .annotation-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  }
}
</style>
