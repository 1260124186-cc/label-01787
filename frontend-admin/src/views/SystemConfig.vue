<template>
  <div class="system-config-page">
    <el-card class="config-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统配置管理</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Refresh" @click="refreshCache" :loading="refreshing">
              刷新缓存
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeCategory" class="config-tabs" @tab-change="handleCategoryChange">
        <el-tab-pane label="PDF渲染配置" name="pdf">
          <el-form :model="pdfConfigForms" label-width="220px" class="config-form">
            <el-form-item label="PDF渲染DPI（分辨率）">
              <el-input-number
                v-model="pdfConfigForms['pdf.render.dpi']"
                :min="72"
                :max="300"
                :step="10"
                size="default"
                :controls="true"
              />
              <span class="form-tip">建议值 72-300，值越高清晰度越好，文件越大</span>
            </el-form-item>

            <el-form-item label="缩略图渲染DPI">
              <el-input-number
                v-model="pdfConfigForms['pdf.thumbnail.dpi']"
                :min="36"
                :max="150"
                :step="10"
                size="default"
                :controls="true"
              />
              <span class="form-tip">建议值 36-150，书架封面缩略图的分辨率</span>
            </el-form-item>

            <el-form-item label="上传后预渲染页数">
              <el-input-number
                v-model="pdfConfigForms['pdf.prerender.pages']"
                :min="1"
                :max="100"
                :step="1"
                size="default"
                :controls="true"
              />
              <span class="form-tip">建议值 5-20，上传完成后异步预渲染前N页</span>
            </el-form-item>

            <el-form-item label="启用上传后异步预渲染">
              <el-switch v-model="pdfConfigForms['pdf.prerender.enabled']" />
              <span class="form-tip">开启后上传PDF将自动后台预渲染前N页</span>
            </el-form-item>

            <el-form-item label="启用PDF页面缓存">
              <el-switch v-model="pdfConfigForms['pdf.cache.enabled']" />
              <span class="form-tip">开启后渲染过的页面将缓存到磁盘</span>
            </el-form-item>

            <el-form-item label="PDF页面缓存过期时间（小时）">
              <el-input-number
                v-model="pdfConfigForms['pdf.cache.expire_hours']"
                :min="1"
                :max="720"
                :step="1"
                size="default"
                :controls="true"
              />
              <span class="form-tip">超过此时间的缓存页面将被清理</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="savePdfConfig" :loading="saving">
                保存PDF配置
              </el-button>
              <el-button @click="resetCategory('pdf')">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="阅读器配置" name="reader">
          <el-form :model="readerConfigForms" label-width="220px" class="config-form">
            <el-form-item label="阅读器预加载偏移页数">
              <el-input-number
                v-model="readerConfigForms['reader.preload.offset']"
                :min="0"
                :max="10"
                :step="1"
                size="default"
                :controls="true"
              />
              <span class="form-tip">当前页±N页后台加载，建议值 1-3</span>
            </el-form-item>

            <el-form-item label="启用阅读器预加载">
              <el-switch v-model="readerConfigForms['reader.preload.enabled']" />
              <span class="form-tip">开启后将在后台预加载相邻页面</span>
            </el-form-item>

            <el-form-item label="弱网时显示骨架屏">
              <el-switch v-model="readerConfigForms['reader.skeleton.enabled']" />
              <span class="form-tip">弱网环境下页面加载时显示占位骨架屏</span>
            </el-form-item>

            <el-form-item label="弱网阈值（KB/s）">
              <el-input-number
                v-model="readerConfigForms['reader.weaknetwork.threshold_kb']"
                :min="10"
                :max="500"
                :step="10"
                size="default"
                :controls="true"
              />
              <span class="form-tip">平均速度低于此值判定为弱网</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveReaderConfig" :loading="saving">
                保存阅读器配置
              </el-button>
              <el-button @click="resetCategory('reader')">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="全部配置" name="all">
          <el-table :data="allConfigs" v-loading="loading" stripe border size="default">
            <el-table-column prop="configKey" label="配置键" width="260">
              <template #default="scope">
                <span class="config-key">{{ scope.row.configKey }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="200">
              <template #default="scope">
                <span>{{ scope.row.description || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="100">
              <template #default="scope">
                <el-tag size="small" :type="getCategoryTagType(scope.row.category)">
                  {{ scope.row.category }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="配置值" min-width="200">
              <template #default="scope">
                <el-input
                  v-if="scope.row.isEditable !== 0"
                  v-model="scope.row.editValue"
                  :type="scope.row.configType === 'number' ? 'number' : 'text'"
                  :placeholder="getPlaceholder(scope.row)"
                  size="small"
                  style="width: 100%"
                />
                <span v-else>{{ scope.row.configValue }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="configType" label="类型" width="90">
              <template #default="scope">
                <el-tag size="small" :type="getTypeTagType(scope.row.configType)">
                  {{ scope.row.configType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="scope.row.isEditable !== 0"
                  type="primary"
                  link
                  size="small"
                  @click="updateSingleConfig(scope.row)"
                  :loading="scope.row.saving"
                >
                  保存
                </el-button>
                <span v-else class="locked-text">只读</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getConfigList,
  getConfigCategories,
  updateConfig,
  batchUpdateConfigs,
  refreshConfigCache
} from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const activeCategory = ref('pdf')

const allConfigs = ref([])
const categories = ref([])

const pdfConfigKeys = [
  'pdf.render.dpi',
  'pdf.thumbnail.dpi',
  'pdf.prerender.pages',
  'pdf.prerender.enabled',
  'pdf.cache.enabled',
  'pdf.cache.expire_hours'
]

const readerConfigKeys = [
  'reader.preload.offset',
  'reader.preload.enabled',
  'reader.skeleton.enabled',
  'reader.weaknetwork.threshold_kb'
]

const pdfConfigForms = reactive({})
const readerConfigForms = reactive({})
const originalPdfConfig = {}
const originalReaderConfig = {}

onMounted(() => {
  loadAllConfigs()
  loadCategories()
})

function getCategoryTagType(category) {
  const map = {
    pdf: 'danger',
    reader: 'primary',
    general: 'success'
  }
  return map[category] || 'info'
}

function getTypeTagType(type) {
  const map = {
    number: 'warning',
    boolean: 'success',
    string: 'info',
    json: 'danger'
  }
  return map[type] || 'info'
}

function getPlaceholder(row) {
  if (row.configType === 'boolean') return 'true / false'
  if (row.configType === 'number') return '请输入数字'
  return '请输入配置值'
}

async function loadCategories() {
  try {
    const res = await getConfigCategories()
    categories.value = res.data || []
  } catch (e) {
    console.error('加载配置分类失败', e)
  }
}

async function loadAllConfigs() {
  loading.value = true
  try {
    const res = await getConfigList({ page: 1, size: 100 })
    const list = res.data || []
    allConfigs.value = list.map(item => ({
      ...item,
      editValue: item.configValue,
      saving: false
    }))

    pdfConfigKeys.forEach(key => {
      const cfg = list.find(c => c.configKey === key)
      if (cfg) {
        const val = parseConfigValue(cfg.configValue, cfg.configType)
        pdfConfigForms[key] = val
        originalPdfConfig[key] = val
      }
    })

    readerConfigKeys.forEach(key => {
      const cfg = list.find(c => c.configKey === key)
      if (cfg) {
        const val = parseConfigValue(cfg.configValue, cfg.configType)
        readerConfigForms[key] = val
        originalReaderConfig[key] = val
      }
    })
  } catch (e) {
    ElMessage.error('加载配置失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

function parseConfigValue(value, type) {
  if (value == null) return ''
  switch (type) {
    case 'number':
      const num = Number(value)
      return isNaN(num) ? 0 : num
    case 'boolean':
      return value === 'true' || value === '1' || value === true
    default:
      return String(value)
  }
}

function formatConfigValue(value, type) {
  switch (type) {
    case 'boolean':
      return value ? 'true' : 'false'
    default:
      return String(value)
  }
}

async function savePdfConfig() {
  saving.value = true
  try {
    const updates = {}
    for (const key of pdfConfigKeys) {
      const cfg = allConfigs.value.find(c => c.configKey === key)
      if (cfg) {
        updates[key] = formatConfigValue(pdfConfigForms[key], cfg.configType)
        originalPdfConfig[key] = pdfConfigForms[key]
      }
    }
    await batchUpdateConfigs(updates)
    ElMessage.success('PDF配置保存成功')
    loadAllConfigs()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
    console.error(e)
  } finally {
    saving.value = false
  }
}

async function saveReaderConfig() {
  saving.value = true
  try {
    const updates = {}
    for (const key of readerConfigKeys) {
      const cfg = allConfigs.value.find(c => c.configKey === key)
      if (cfg) {
        updates[key] = formatConfigValue(readerConfigForms[key], cfg.configType)
        originalReaderConfig[key] = readerConfigForms[key]
      }
    }
    await batchUpdateConfigs(updates)
    ElMessage.success('阅读器配置保存成功')
    loadAllConfigs()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
    console.error(e)
  } finally {
    saving.value = false
  }
}

function resetCategory(category) {
  if (category === 'pdf') {
    Object.keys(originalPdfConfig).forEach(key => {
      pdfConfigForms[key] = originalPdfConfig[key]
    })
  } else if (category === 'reader') {
    Object.keys(originalReaderConfig).forEach(key => {
      readerConfigForms[key] = originalReaderConfig[key]
    })
  }
  ElMessage.info('已重置为修改前的值')
}

async function updateSingleConfig(row) {
  row.saving = true
  try {
    await updateConfig({ key: row.configKey, value: String(row.editValue) })
    row.configValue = String(row.editValue)
    ElMessage.success(`配置 ${row.configKey} 已更新`)
    loadAllConfigs()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    row.saving = false
  }
}

async function refreshCache() {
  refreshing.value = true
  try {
    await refreshConfigCache()
    ElMessage.success('配置缓存已刷新')
    loadAllConfigs()
  } catch (e) {
    ElMessage.error(e.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

function handleCategoryChange() {
}
</script>

<style lang="scss" scoped>
.system-config-page {
  .config-card {
    border-radius: 12px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .config-tabs {
    :deep(.el-tabs__item) {
      font-size: 15px;
    }
  }

  .config-form {
    max-width: 800px;
    padding: 16px 0;

    :deep(.el-form-item) {
      margin-bottom: 28px;
    }

    .form-tip {
      margin-left: 12px;
      font-size: 12px;
      color: #999;
    }
  }

  .config-key {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 13px;
    color: #6B4226;
    background: #F5F0EB;
    padding: 4px 8px;
    border-radius: 4px;
  }

  .locked-text {
    color: #c0c4cc;
    font-size: 12px;
  }
}
</style>
