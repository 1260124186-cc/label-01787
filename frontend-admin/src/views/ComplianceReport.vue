<template>
  <div class="page-container">
    <div class="page-header">
      <h2>合规审计报告</h2>
    </div>

    <el-card class="filter-card">
      <div class="date-filter">
        <span class="filter-label">时间范围：</span>
        <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至"
          start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss"
          @change="fetchReport" />
        <el-button type="primary" :loading="loading" @click="fetchReport" style="margin-left: 16px">生成报告</el-button>
      </div>
    </el-card>

    <template v-if="report">
      <el-row :gutter="16" class="stat-row">
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-title">内容审核</div>
            <div class="stat-value">{{ report.contentAudit?.total || 0 }}</div>
            <div class="stat-detail">
              <span class="pass">通过: {{ report.contentAudit?.pass || 0 }}</span>
              <span class="suspected">疑似: {{ report.contentAudit?.suspected || 0 }}</span>
              <span class="violation">违规: {{ report.contentAudit?.violation || 0 }}</span>
            </div>
            <div class="stat-rate">通过率: {{ report.contentAudit?.passRate || 'N/A' }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-title">版权申诉</div>
            <div class="stat-value">{{ report.copyrightComplaint?.total || 0 }}</div>
            <div class="stat-detail">
              <span class="pending">待处理: {{ report.copyrightComplaint?.pending || 0 }}</span>
              <span class="violation">下架: {{ report.copyrightComplaint?.takenDown || 0 }}</span>
              <span class="pass">驳回: {{ report.copyrightComplaint?.rejected || 0 }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-title">书籍合规</div>
            <div class="stat-value">{{ report.bookCompliance?.totalUploaded || 0 }}</div>
            <div class="stat-detail">
              <span class="pass">声明版权: {{ report.bookCompliance?.copyrightDeclared || 0 }}</span>
              <span class="violation">下架: {{ report.bookCompliance?.takenDown || 0 }}</span>
            </div>
            <div class="stat-rate">版权声明率: {{ report.bookCompliance?.copyrightRate || 'N/A' }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="detail-row">
        <el-col :span="12">
          <el-card>
            <template #header><span>内容审核详情</span></template>
            <el-table :data="auditList" stripe style="width: 100%" empty-text="暂无审核记录" max-height="400">
              <el-table-column prop="targetType" label="类型" width="80">
                <template #default="{ row }">
                  {{ targetTypeText(row.targetType) }}
                </template>
              </el-table-column>
              <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
              <el-table-column label="结果" width="90">
                <template #default="{ row }">
                  <el-tag :type="resultTagType(row.result)" size="small">{{ resultText(row.result) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="keywords" label="命中词" min-width="120" show-overflow-tooltip />
              <el-table-column prop="createdAt" label="时间" min-width="170" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><span>操作日志概览</span></template>
            <div class="op-summary">
              <div class="op-item">
                <span class="op-label">期间操作总数</span>
                <span class="op-value">{{ report.operationAudit?.totalOperations || 0 }}</span>
              </div>
              <div class="op-item">
                <span class="op-label">报告生成时间</span>
                <span class="op-value">{{ report.generatedAt || '-' }}</span>
              </div>
              <div class="op-item">
                <span class="op-label">统计周期</span>
                <span class="op-value">{{ report.period?.start?.substring(0, 10) }} ~ {{ report.period?.end?.substring(0, 10) }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getComplianceReport, getAuditList } from '@/api/admin'

const report = ref(null)
const auditList = ref([])
const loading = ref(false)
const dateRange = ref(null)

const targetTypeText = (type) => {
  const map = { 1: '书名', 2: '批注', 3: '书摘' }
  return map[type] ?? '未知'
}

const resultText = (result) => {
  const map = { 0: '通过', 1: '疑似', 2: '违规' }
  return map[result] ?? '未知'
}

const resultTagType = (result) => {
  const map = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[result] ?? ''
}

const fetchReport = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res = await getComplianceReport(params)
    report.value = res.data

    const auditRes = await getAuditList({ page: 1, size: 50 })
    auditList.value = auditRes.data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: 16px;
}
.date-filter {
  display: flex;
  align-items: center;
}
.filter-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
  .stat-title {
    font-size: 14px;
    color: #999;
    margin-bottom: 8px;
  }
  .stat-value {
    font-size: 36px;
    font-weight: 700;
    color: #6B4226;
    margin-bottom: 12px;
  }
  .stat-detail {
    font-size: 13px;
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-bottom: 8px;
  }
  .stat-rate {
    font-size: 13px;
    color: #999;
  }
}
.pass { color: #67c23a; }
.suspected { color: #e6a23c; }
.violation { color: #f56c6c; }
.pending { color: #e6a23c; }
.detail-row {
  margin-top: 0;
}
.op-summary {
  padding: 8px 0;
}
.op-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }
  .op-label { color: #666; font-size: 14px; }
  .op-value { color: #333; font-size: 14px; font-weight: 500; }
}
</style>
