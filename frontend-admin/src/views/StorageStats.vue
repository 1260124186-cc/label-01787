<template>
  <div class="page-container">
    <div class="page-header">
      <h2>存储用量报表</h2>
      <div class="header-actions">
        <el-button type="primary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon primary">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon success">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalBooks || 0 }}</div>
              <div class="stat-label">总书籍数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon warning">
              <el-icon><Files /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalFileSizeText || '0 B' }}</div>
              <div class="stat-label">总存储量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon info">
              <el-icon><EditPen /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalAnnotations || 0 }}</div>
              <div class="stat-label">批注总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon purple">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalReadingRecords || 0 }}</div>
              <div class="stat-label">阅读记录总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon cyan">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalCategories || 0 }}</div>
              <div class="stat-label">分类总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>存储用户排行 Top 10</span>
            </div>
          </template>
          <div v-loading="loading" class="chart-container">
            <el-table :data="stats.topUsers || []" stripe empty-text="暂无数据">
              <el-table-column label="排名" width="80" align="center">
                <template #default="{ $index }">
                  <el-tag v-if="$index < 3" :type="getRankType($index)" size="small">
                    {{ $index + 1 }}
                  </el-tag>
                  <span v-else>{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="用户昵称" min-width="120" />
              <el-table-column prop="bookCount" label="书籍数" width="100" align="center" />
              <el-table-column prop="fileSizeText" label="存储量" width="120" />
              <el-table-column label="占比" width="150">
                <template #default="{ row }">
                  <el-progress :percentage="row.percentage?.toFixed(2) || 0" :stroke-width="12" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>近30天存储趋势</span>
            </div>
          </template>
          <div v-loading="loading" class="chart-container">
            <el-table :data="stats.dailyTrend || []" stripe empty-text="暂无数据">
              <el-table-column prop="date" label="日期" width="140" />
              <el-table-column label="当日新增" width="140">
                <template #default="{ row }">
                  <span class="text-success">+{{ formatSize(row.fileSize) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="bookCount" label="新增书籍" width="100" align="center" />
              <el-table-column prop="userCount" label="活跃用户" width="100" align="center" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>平台数据概览</span>
          <el-tag type="info">数据实时统计</el-tag>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="overview-item">
            <div class="overview-label">平均每用户书籍数</div>
            <div class="overview-value">{{ avgBooksPerUser }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="overview-item">
            <div class="overview-label">平均每用户存储量</div>
            <div class="overview-value">{{ avgStoragePerUser }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="overview-item">
            <div class="overview-label">平均每书籍批注数</div>
            <div class="overview-value">{{ avgAnnotationsPerBook }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, User, Collection, Files, EditPen, Timer, Folder } from '@element-plus/icons-vue'
import { getStorageStats } from '@/api/backup'

const loading = ref(false)
const stats = reactive({
  totalUsers: 0,
  totalBooks: 0,
  totalFileSize: 0,
  totalFileSizeText: '0 B',
  totalAnnotations: 0,
  totalReadingRecords: 0,
  totalCategories: 0,
  topUsers: [],
  dailyTrend: []
})

const avgBooksPerUser = computed(() => {
  if (!stats.totalUsers || !stats.totalBooks) return '0'
  return (stats.totalBooks / stats.totalUsers).toFixed(1)
})

const avgStoragePerUser = computed(() => {
  if (!stats.totalUsers || !stats.totalFileSize) return '0 B'
  return formatSize(stats.totalFileSize / stats.totalUsers)
})

const avgAnnotationsPerBook = computed(() => {
  if (!stats.totalBooks || !stats.totalAnnotations) return '0'
  return (stats.totalAnnotations / stats.totalBooks).toFixed(1)
})

const getRankType = (index) => {
  switch (index) {
    case 0: return 'warning'
    case 1: return 'info'
    case 2: return 'success'
    default: return 'info'
  }
}

const formatSize = (size) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const res = await getStorageStats()
    Object.assign(stats, res.data)
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;

  &.primary {
    background: linear-gradient(135deg, #409eff, #66b1ff);
  }
  &.success {
    background: linear-gradient(135deg, #67c23a, #85ce61);
  }
  &.warning {
    background: linear-gradient(135deg, #e6a23c, #f0c78a);
  }
  &.info {
    background: linear-gradient(135deg, #909399, #a6a9ad);
  }
  &.purple {
    background: linear-gradient(135deg, #8e44ad, #a569bd);
  }
  &.cyan {
    background: linear-gradient(135deg, #1abc9c, #48c9b0);
  }
}

.stat-content {
  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
  }
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  min-height: 300px;
}

.text-success {
  color: #67c23a;
  font-weight: 500;
}

.overview-item {
  text-align: center;
  padding: 20px 0;

  .overview-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  .overview-value {
    font-size: 28px;
    font-weight: 600;
    color: #409eff;
  }
}
</style>
