<template>
  <div class="sys-config-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">系统配置</h2>
        <span class="page-desc">管理PDF渲染和阅读器性能相关参数</span>
      </div>
      <div class="header-right">
        <el-button :icon="Refresh" @click="handleRefreshCache">
          刷新缓存
        </el-button>
        <el-button type="primary" :icon="Check" :disabled="!hasChanges" @click="handleSaveAll">
          保存全部修改
        </el-button>
      </div>
    </div>

    <div class="category-tabs">
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane label="PDF渲染配置" name="pdf">
          <div class="config-card">
            <div class="card-header">
              <h3 class="card-title">PDF 渲染参数</h3>
              <span class="card-desc">控制PDF文件的渲染质量和预渲染行为</span>
            </div>
            <el-table :data="pdfConfigs" stripe style="width: 100%" class="config-table">
              <el-table-column label="配置项" width="200">
                <template #default="{ row }">
                  <div class="config-name">
                    <span class="name-text">{{ row.description }}</span>
                    <span class="key-text">{{ row.configKey }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <div class="config-value-cell">
                    <el-input-number
                      v-if="row.configType === 'number'"
                      v-model="row.editValue"
                      :min="getMinValue(row.configKey)"
                      :max="getMaxValue(row.configKey)"
                      :step="getStepValue(row.configKey)"
                      :controls="true"
                      size="default"
                      @change="() => markChanged(row)"
                    />
                    <el-switch
                      v-else-if="row.configType === 'boolean'"
                      v-model="row.editValue"
                      :active-value="true"
                      :inactive-value="false"
                      @change="() => markChanged(row)"
                    />
                    <el-input
                      v-else
                      v-model="row.editValue"
                      size="default"
                      @change="() => markChanged(row)"
                    />
                    <span class="value-unit" v-if="getUnit(row.configKey)">
                      {{ getUnit(row.configKey) }}
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="当前值" width="120">
                <template #default="{ row }">
                  <el-tag :type="getValueTagType(row)">
                    {{ formatValue(row.configValue, row.configType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <span v-if="row._changed" class="changed-badge">已修改</span>
                  <span v-else class="unchanged-badge">未修改</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    :disabled="!row._changed"
                    @click="handleSaveSingle(row)"
                  >
                    保存
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="config-tips">
            <el-alert
              title="配置说明"
              type="info"
              :closable="false"
              show-icon
            >
              <template #default>
                <ul class="tips-list">
                  <li><strong>PDF渲染DPI：</strong>数值越大渲染越清晰，但加载速度越慢、内存占用越高。建议范围 72-300。</li>
                  <li><strong>缩略图渲染DPI：</strong>书架封面缩略图的分辨率，建议范围 36-150。</li>
                  <li><strong>上传后预渲染页数：</strong>PDF上传后后台自动预渲染的页数，加快首次阅读速度。</li>
                  <li><strong>PDF页面缓存：</strong>启用后会将渲染后的页面缓存到本地，下次访问时直接使用。</li>
                </ul>
              </template>
            </el-alert>
          </div>
        </el-tab-pane>

        <el-tab-pane label="阅读器配置" name="reader">
          <div class="config-card">
            <div class="card-header">
              <h3 class="card-title">阅读器性能参数</h3>
              <span class="card-desc">控制阅读器的预加载策略和弱网体验</span>
            </div>
            <el-table :data="readerConfigs" stripe style="width: 100%" class="config-table">
              <el-table-column label="配置项" width="200">
                <template #default="{ row }">
                  <div class="config-name">
                    <span class="name-text">{{ row.description }}</span>
                    <span class="key-text">{{ row.configKey }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <div class="config-value-cell">
                    <el-input-number
                      v-if="row.configType === 'number'"
                      v-model="row.editValue"
                      :min="getMinValue(row.configKey)"
                      :max="getMaxValue(row.configKey)"
                      :step="getStepValue(row.configKey)"
                      :controls="true"
                      size="default"
                      @change="() => markChanged(row)"
                    />
                    <el-switch
                      v-else-if="row.configType === 'boolean'"
                      v-model="row.editValue"
                      :active-value="true"
                      :inactive-value="false"
                      @change="() => markChanged(row)"
                    />
                    <el-input
                      v-else
                      v-model="row.editValue"
                      size="default"
                      @change="() => markChanged(row)"
                    />
                    <span class="value-unit" v-if="getUnit(row.configKey)">
                      {{ getUnit(row.configKey) }}
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="当前值" width="120">
                <template #default="{ row }">
                  <el-tag :type="getValueTagType(row)">
                    {{ formatValue(row.configValue, row.configType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <span v-if="row._changed" class="changed-badge">已修改</span>
                  <span v-else class="unchanged-badge">未修改</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    :disabled="!row._changed"
                    @click="handleSaveSingle(row)"
                  >
                    保存
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="config-tips">
            <el-alert
              title="配置说明"
              type="info"
              :closable="false"
              show-icon
            >
              <template #default>
                <ul class="tips-list">
                  <li><strong>阅读器预加载偏移页数：</strong>阅读当前页时，后台自动预加载前后N页的内容。建议范围 1-5。</li>
                  <li><strong>阅读器预加载：</strong>启用后会在后台预加载相邻页面，提升翻页流畅度。</li>
                  <li><strong>弱网骨架屏：</strong>弱网环境下显示骨架屏占位，提升用户体验。</li>
                  <li><strong>弱网阈值：</strong>平均下载速度低于此值时判定为弱网。</li>
                </ul>
              </template>
            </el-alert>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import {
  getConfigList,
  updateConfig,
  batchUpdateConfigs,
  refreshConfigCache
} from '@/api/admin'

const activeCategory = ref('pdf')
const allConfigs = ref([])
const loading = ref(false)

const pdfConfigs = computed(() => {
  return allConfigs.value.filter(c => c.category === 'pdf').map(c => ({
    ...c,
    editValue: parseValue(c.configValue, c.configType),
    _changed: false
  }))
})

const readerConfigs = computed(() => {
  return allConfigs.value.filter(c => c.category === 'reader').map(c => ({
    ...c,
    editValue: parseValue(c.configValue, c.configType),
    _changed: false
  }))
})

const hasChanges = computed(() => {
  return allConfigs.value.some(c => c._changed)
})

function parseValue(value, type) {
  if (value === null || value === undefined) return ''
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

function formatValue(value, type) {
  switch (type) {
    case 'boolean':
      return value === 'true' || value === '1' || value === true ? '开启' : '关闭'
    default:
      return value ?? '-'
  }
}

function getValueTagType(row) {
  if (row.configType === 'boolean') {
    return (row.configValue === 'true' || row.configValue === '1') ? 'success' : 'info'
  }
  return ''
}

function getMinValue(key) {
  const minMap = {
    'pdf.render.dpi': 36,
    'pdf.thumbnail.dpi': 18,
    'pdf.prerender.pages': 0,
    'pdf.cache.expire_hours': 1,
    'reader.preload.offset': 0,
    'reader.weaknetwork.threshold_kb': 1
  }
  return minMap[key] ?? undefined
}

function getMaxValue(key) {
  const maxMap = {
    'pdf.render.dpi': 600,
    'pdf.thumbnail.dpi': 300,
    'pdf.prerender.pages': 100,
    'pdf.cache.expire_hours': 720,
    'reader.preload.offset': 10,
    'reader.weaknetwork.threshold_kb': 1000
  }
  return maxMap[key] ?? undefined
}

function getStepValue(key) {
  const stepMap = {
    'pdf.render.dpi': 10,
    'pdf.thumbnail.dpi': 5,
    'pdf.prerender.pages': 1,
    'pdf.cache.expire_hours': 1,
    'reader.preload.offset': 1,
    'reader.weaknetwork.threshold_kb': 10
  }
  return stepMap[key] ?? 1
}

function getUnit(key) {
  const unitMap = {
    'pdf.render.dpi': 'DPI',
    'pdf.thumbnail.dpi': 'DPI',
    'pdf.prerender.pages': '页',
    'pdf.cache.expire_hours': '小时',
    'reader.preload.offset': '页',
    'reader.weaknetwork.threshold_kb': 'KB/s'
  }
  return unitMap[key] || ''
}

function markChanged(row) {
  const original = allConfigs.value.find(c => c.configKey === row.configKey)
  if (original) {
    const originalValue = parseValue(original.configValue, original.configType)
    row._changed = JSON.stringify(originalValue) !== JSON.stringify(row.editValue)
  }
}

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getConfigList()
    allConfigs.value = (res.data || []).map(c => ({ ...c, _changed: false }))
  } catch (e) {
    console.error('加载配置失败', e)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSaveSingle(row) {
  try {
    const value = typeof row.editValue === 'boolean' 
      ? (row.editValue ? 'true' : 'false') 
      : String(row.editValue)
    await updateConfig(row.configKey, value)
    const original = allConfigs.value.find(c => c.configKey === row.configKey)
    if (original) {
      original.configValue = value
    }
    row._changed = false
    ElMessage.success('保存成功')
  } catch (e) {
    console.error('保存配置失败', e)
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleSaveAll() {
  const changedPdf = pdfConfigs.value.filter(c => c._changed)
  const changedReader = readerConfigs.value.filter(c => c._changed)
  const allChanged = [...changedPdf, ...changedReader]
  
  if (allChanged.length === 0) {
    ElMessage.warning('没有需要保存的修改')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要保存 ${allChanged.length} 项配置修改吗？`,
      '确认保存',
      { type: 'warning' }
    )
    
    const updates = {}
    for (const row of allChanged) {
      updates[row.configKey] = typeof row.editValue === 'boolean'
        ? (row.editValue ? 'true' : 'false')
        : String(row.editValue)
    }
    
    await batchUpdateConfigs(updates)
    ElMessage.success('全部保存成功')
    await loadConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量保存失败', e)
      ElMessage.error(e.message || '保存失败')
    }
  }
}

async function handleRefreshCache() {
  try {
    await ElMessageBox.confirm(
      '确定要刷新系统配置缓存吗？',
      '确认刷新',
      { type: 'warning' }
    )
    await refreshConfigCache()
    ElMessage.success('缓存已刷新')
    await loadConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('刷新缓存失败', e)
      ElMessage.error(e.message || '刷新缓存失败')
    }
  }
}

function handleCategoryChange() {
  // Category change handler
}

onMounted(() => {
  loadConfigs()
})
</script>

<style lang="scss" scoped>
.sys-config-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;

  .header-left {
    .page-title {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #333;
    }
    .page-desc {
      display: block;
      margin-top: 4px;
      font-size: 14px;
      color: #999;
    }
  }

  .header-right {
    display: flex;
    gap: 12px;
  }
}

.category-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }
}

.config-card {
  .card-header {
    margin-bottom: 20px;

    .card-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    .card-desc {
      display: block;
      margin-top: 4px;
      font-size: 13px;
      color: #999;
    }
  }
}

.config-table {
  :deep(.el-table__header th) {
    background: #fafafa;
    color: #666;
    font-weight: 500;
  }
}

.config-name {
  .name-text {
    display: block;
    font-size: 14px;
    color: #333;
    font-weight: 500;
  }
  .key-text {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: #999;
    font-family: 'SF Mono', Monaco, monospace;
  }
}

.config-value-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .value-unit {
    font-size: 13px;
    color: #666;
  }
}

.changed-badge {
  display: inline-block;
  padding: 2px 10px;
  background: #fef0f0;
  color: #f56c6c;
  border-radius: 10px;
  font-size: 12px;
}

.unchanged-badge {
  display: inline-block;
  padding: 2px 10px;
  background: #f0f9eb;
  color: #67c23a;
  border-radius: 10px;
  font-size: 12px;
}

.config-tips {
  margin-top: 24px;

  .tips-list {
    margin: 0;
    padding-left: 20px;
    line-height: 2;

    li {
      font-size: 13px;
      color: #666;

      strong {
        color: #333;
        font-weight: 500;
      }
    }
  }
}
</style>
