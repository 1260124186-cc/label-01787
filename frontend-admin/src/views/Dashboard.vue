<template>
  <div class="page-container">
    <div class="page-header">
      <h2>数据概览</h2>
    </div>
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #6B4226, #8b5e3c)">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.userCount || 0 }}</div>
          <div class="stat-label">注册用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #E8734A, #f09070)">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.bookCount || 0 }}</div>
          <div class="stat-label">书籍总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475)">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ readingTimeText }}</div>
          <div class="stat-label">累计阅读时长</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #D4A574, #e2c9a0)">
          <el-icon><Notebook /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.logCount || 0 }}</div>
          <div class="stat-label">操作记录</div>
        </div>
      </div>
    </div>

    <div class="stat-cards" v-if="readingStats">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff)">
          <el-icon><View /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ readingStats.activeUsers || 0 }}</div>
          <div class="stat-label">近{{ readingStats.days || 7 }}日活跃阅读用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #eebe77)">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ avgDurationText }}</div>
          <div class="stat-label">人均日阅读时长</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #f89898)">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ totalReadingTimeText }}</div>
          <div class="stat-label">近{{ readingStats.days || 7 }}日总阅读时长</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #6B4226, #8B5E3C)">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ avgDailyText }}</div>
          <div class="stat-label">日均阅读时长</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475)">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ readingStats.totalBookReads || 0 }}</div>
          <div class="stat-label">近{{ readingStats.days || 7 }}日阅读书籍次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #9C27B0, #ba68c8)">
          <el-icon><PieChart /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ avgBooksPerDayText }}</div>
          <div class="stat-label">日均阅读书籍数</div>
        </div>
      </div>
    </div>

    <el-card class="recent-card" v-if="readingStats && readingStats.dailyStats && readingStats.dailyStats.length">
      <template #header>
        <div class="card-header">
          <span>近{{ readingStats.days || 7 }}日阅读趋势</span>
        </div>
      </template>
      <el-table :data="readingStats.dailyStats" stripe style="width: 100%">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="userCount" label="活跃用户" width="100" />
        <el-table-column prop="bookCount" label="阅读书籍数" width="120" />
        <el-table-column label="阅读时长" width="140">
          <template #default="{ row }">
            {{ formatSeconds(row.totalDuration) }}
          </template>
        </el-table-column>
        <el-table-column label="人均时长">
          <template #default="{ row }">
            {{ row.userCount > 0 ? formatSeconds(Math.round(row.totalDuration / row.userCount)) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="recent-card">
      <template #header>
        <div class="card-header">
          <span>最近操作日志</span>
        </div>
      </template>
      <el-table :data="recentLogs" stripe style="width: 100%" empty-text="暂无日志记录">
        <el-table-column prop="action" label="操作" width="160" />
        <el-table-column prop="target" label="目标" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboard, getLogList, getReadingStats } from '@/api/admin'

const stats = ref({})
const recentLogs = ref([])
const readingStats = ref(null)

const readingTimeText = computed(() => {
  const seconds = stats.value.totalReadingSeconds || 0
  if (seconds < 60) return seconds + '秒'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分钟`
})

const avgDurationText = computed(() => {
  if (!readingStats.value) return '-'
  return formatSeconds(readingStats.value.avgDurationPerUser || 0)
})

const totalReadingTimeText = computed(() => {
  if (!readingStats.value) return '-'
  return formatSeconds(readingStats.value.totalDuration || 0)
})

const avgDailyText = computed(() => {
  if (!readingStats.value) return '-'
  return formatSeconds(readingStats.value.avgDurationPerDay || 0)
})

const avgBooksPerDayText = computed(() => {
  if (!readingStats.value || !readingStats.value.avgBooksPerDay) return '0'
  return Number(readingStats.value.avgBooksPerDay).toFixed(1)
})

const formatSeconds = (seconds) => {
  if (!seconds || seconds <= 0) return '0分钟'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}时${minutes}分`
  return `${minutes}分钟`
}

onMounted(async () => {
  try {
    const dashRes = await getDashboard()
    stats.value = dashRes.data

    const logRes = await getLogList({ page: 1, size: 5 })
    recentLogs.value = logRes.data.records || []

    const readingRes = await getReadingStats({ days: 7 })
    readingStats.value = readingRes.data
  } catch (e) {
    // error handled by interceptor
  }
})
</script>

<style lang="scss" scoped>
.recent-card {
  margin-top: 16px;

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}
</style>
