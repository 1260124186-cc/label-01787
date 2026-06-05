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
import { getDashboard, getLogList } from '@/api/admin'

const stats = ref({})
const recentLogs = ref([])

const readingTimeText = computed(() => {
  const seconds = stats.value.totalReadingSeconds || 0
  if (seconds < 60) return seconds + '秒'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分钟`
})

onMounted(async () => {
  try {
    const dashRes = await getDashboard()
    stats.value = dashRes.data

    const logRes = await getLogList({ page: 1, size: 5 })
    recentLogs.value = logRes.data.records || []
  } catch (e) {
    // 错误已处理
  }
})
</script>

<style lang="scss" scoped>
.recent-card {
  margin-top: 0;

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}
</style>
