<template>
  <div class="page-container">
    <div class="page-header">
      <h2>阅读计划统计</h2>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #6B4226, #8b5e3c)">
          <el-icon><Notebook /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalPlans || 0 }}</div>
          <div class="stat-label">计划总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff)">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.activePlans || 0 }}</div>
          <div class="stat-label">进行中计划</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475)">
          <el-icon><Select /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completedPlans || 0 }}</div>
          <div class="stat-label">已完成计划</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #eebe77)">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completionRate || 0 }}%</div>
          <div class="stat-label">完成率</div>
        </div>
      </div>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #9C27B0, #ba68c8)">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.activePlanUsers || 0 }}</div>
          <div class="stat-label">参与计划用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #E8734A, #f09070)">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.monthCreated || 0 }}</div>
          <div class="stat-label">本月新建计划</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #95d475)">
          <el-icon><Finished /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.monthCompleted || 0 }}</div>
          <div class="stat-label">本月完成计划</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #79bbff)">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.monthCheckinUsers || 0 }}</div>
          <div class="stat-label">本月打卡用户</div>
        </div>
      </div>
    </div>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>本月每日计划创建趋势</span>
        </div>
      </template>
      <el-table :data="stats.dailyCreation || []" stripe style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="count" label="创建数" width="100" />
      </el-table>
    </el-card>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>本月每日计划完成趋势</span>
        </div>
      </template>
      <el-table :data="stats.dailyCompletion || []" stripe style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="count" label="完成数" width="100" />
      </el-table>
    </el-card>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>本月每日打卡统计</span>
        </div>
      </template>
      <el-table :data="stats.dailyCheckin || []" stripe style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="userCount" label="打卡用户数" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getReadingPlanStats } from '@/api/admin'

const stats = ref({})

onMounted(async () => {
  try {
    const res = await getReadingPlanStats()
    stats.value = res.data
  } catch (e) {
    // error handled by interceptor
  }
})
</script>

<style lang="scss" scoped>
.chart-card {
  margin-top: 16px;

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}
</style>
