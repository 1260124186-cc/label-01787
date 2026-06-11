<template>
  <div class="page-container">
    <div class="page-header">
      <h2>阅读报告统计</h2>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="days" size="default" @change="loadStats">
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
        <el-radio-button :value="90">近90天</el-radio-button>
      </el-radio-group>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #6B4226, #8b5e3c)">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalReports || 0 }}</div>
          <div class="stat-label">报告生成量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff)">
          <el-icon><Share /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalShares || 0 }}</div>
          <div class="stat-label">分享次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475)">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.activeUsers || 0 }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #eebe77)">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ shareRate || '0' }}%</div>
          <div class="stat-label">分享率</div>
        </div>
      </div>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #9C27B0, #ba68c8)">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.reportTypeCount?.weekly || 0 }}</div>
          <div class="stat-label">周报数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #E8734A, #f09070)">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.reportTypeCount?.monthly || 0 }}</div>
          <div class="stat-label">月报数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #00BCD4, #4dd0e1)">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.reportTypeCount?.yearly || 0 }}</div>
          <div class="stat-label">年报数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #F06292, #f48fb1)">
          <el-icon><PieChart /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ avgPerUser || '0' }}</div>
          <div class="stat-label">人均报告数</div>
        </div>
      </div>
    </div>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>每日报告生成趋势</span>
        </div>
      </template>
      <el-table :data="dailyStats" stripe style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="userCount" label="活跃用户数" width="120" />
        <el-table-column prop="totalDuration" label="总阅读时长(秒)" width="160" />
        <el-table-column prop="bookCount" label="阅读书籍数" width="120" />
      </el-table>
    </el-card>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>报告列表</span>
        </div>
      </template>
      <div class="table-filter">
        <el-select v-model="filterType" placeholder="报告类型" style="width: 150px" @change="loadReportList" clearable>
          <el-option label="全部" value="" />
          <el-option label="周报" value="weekly" />
          <el-option label="月报" value="monthly" />
          <el-option label="年报" value="yearly" />
        </el-select>
      </div>
      <el-table :data="reportList" stripe style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="reportType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.reportType)">
              {{ getTypeLabel(row.reportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="周期" min-width="200">
          <template #default="{ row }">
            {{ row.periodStart }} ~ {{ row.periodEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="totalDuration" label="总时长(秒)" width="120" />
        <el-table-column prop="bookCount" label="书籍数" width="80" />
        <el-table-column prop="annotationCount" label="批注数" width="80" />
        <el-table-column prop="shareCount" label="分享次数" width="100" />
        <el-table-column prop="createdAt" label="生成时间" width="180" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getReadingReportStats, getReadingReportList } from '@/api/admin'
import { Document, Share, User, Calendar, DataAnalysis, Trophy, PieChart, TrendCharts } from '@element-plus/icons-vue'

const days = ref(30)
const stats = ref({})
const reportList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterType = ref('')

const shareRate = computed(() => {
  if (!stats.value.totalReports || stats.value.totalReports === 0) return '0'
  return ((stats.value.totalShares / stats.value.totalReports) * 100).toFixed(1)
})

const avgPerUser = computed(() => {
  if (!stats.value.activeUsers || stats.value.activeUsers === 0) return '0'
  return (stats.value.totalReports / stats.value.activeUsers).toFixed(2)
})

const dailyStats = computed(() => {
  return (stats.value.dailyStats || []).slice().reverse()
})

function getTypeLabel(type) {
  const labels = {
    weekly: '周报',
    monthly: '月报',
    yearly: '年报'
  }
  return labels[type] || type
}

function getTypeTagType(type) {
  const types = {
    weekly: 'info',
    monthly: 'warning',
    yearly: 'success'
  }
  return types[type] || 'info'
}

async function loadStats() {
  try {
    const res = await getReadingReportStats(days.value)
    stats.value = res.data
  } catch (e) {
    // error handled by interceptor
  }
}

async function loadReportList() {
  try {
    const res = await getReadingReportList({
      page: currentPage.value,
      size: pageSize.value,
      reportType: filterType.value || undefined
    })
    reportList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    // error handled by interceptor
  }
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadReportList()
}

function handlePageChange(page) {
  currentPage.value = page
  loadReportList()
}

onMounted(() => {
  loadStats()
  loadReportList()
})
</script>

<style lang="scss" scoped>
.filter-bar {
  margin-bottom: 20px;
}

.chart-card {
  margin-top: 16px;

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

.table-filter {
  margin-bottom: 16px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
