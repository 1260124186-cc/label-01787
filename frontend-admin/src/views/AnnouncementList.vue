<template>
  <div class="page-container">
    <div class="page-header">
      <h2>公告管理</h2>
      <el-button type="primary" @click="openSendDialog">
        <el-icon><Plus /></el-icon>
        发送公告
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="filterType" placeholder="消息类型" clearable style="width: 160px" @change="fetchData">
          <el-option label="系统通知" :value="1" />
          <el-option label="审核结果" :value="2" />
          <el-option label="计划提醒" :value="3" />
          <el-option label="小组动态" :value="4" />
        </el-select>
        <el-select v-model="filterRead" placeholder="阅读状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="fetchData">查询</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无消息数据">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收用户" width="120">
          <template #default="{ row }">
            {{ row.userId ? '指定用户' : '全体用户' }}
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="内容" prop="content" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'success' : 'warning'">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发送时间" prop="createdAt" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 发送公告弹窗 -->
    <el-dialog v-model="sendDialogVisible" title="发送公告" width="600px" @close="resetSendForm">
      <el-form :model="sendForm" :rules="sendRules" ref="sendFormRef" label-width="100px">
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="sendForm.type" placeholder="请选择消息类型" style="width: 100%">
            <el-option label="系统通知" :value="1" />
            <el-option label="审核结果" :value="2" />
            <el-option label="计划提醒" :value="3" />
            <el-option label="小组动态" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用模板">
          <el-select v-model="selectedTemplate" placeholder="选择模板（可选）" style="width: 100%" @change="onTemplateChange" clearable>
            <el-option v-for="tpl in templates" :key="tpl.id" :label="tpl.name" :value="tpl" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="sendForm.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="sendForm.content" type="textarea" :rows="4" placeholder="请输入内容" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="发送范围" prop="sendToAll">
          <el-radio-group v-model="sendForm.sendToAll">
            <el-radio :value="true">全体用户</el-radio>
            <el-radio :value="false">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!sendForm.sendToAll" label="选择用户" prop="userIds">
          <el-select v-model="sendForm.userIds" multiple filterable placeholder="请选择用户" style="width: 100%">
            <el-option v-for="user in userList" :key="user.id" :label="user.nickname || `用户${user.id}`" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSend" :loading="sendLoading">发送</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="消息详情" width="500px">
      <div class="detail-content">
        <div class="detail-item">
          <span class="label">类型：</span>
          <el-tag :type="getTypeTagType(detailData.type)">
            {{ getTypeName(detailData.type) }}
          </el-tag>
        </div>
        <div class="detail-item">
          <span class="label">标题：</span>
          <span class="value">{{ detailData.title }}</span>
        </div>
        <div class="detail-item">
          <span class="label">内容：</span>
        </div>
        <div class="detail-content-text">{{ detailData.content }}</div>
        <div class="detail-item">
          <span class="label">发送时间：</span>
          <span class="value">{{ detailData.createdAt }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getNotificationList, sendAnnouncement, getEnabledTemplates } from '@/api/notification'
import { getUserList } from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const filterType = ref(null)
const filterRead = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const sendDialogVisible = ref(false)
const sendFormRef = ref(null)
const sendLoading = ref(false)
const sendForm = ref({
  type: null,
  title: '',
  content: '',
  sendToAll: true,
  userIds: [],
  extraData: ''
})
const sendRules = {
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  sendToAll: [{ required: true, message: '请选择发送范围', trigger: 'change' }]
}

const detailDialogVisible = ref(false)
const detailData = ref({})

const templates = ref([])
const selectedTemplate = ref(null)
const userList = ref([])

const getTypeName = (type) => {
  const map = { 1: '系统通知', 2: '审核结果', 3: '计划提醒', 4: '小组动态' }
  return map[type] || '未知'
}

const getTypeTagType = (type) => {
  const map = { 1: 'primary', 2: 'warning', 3: 'success', 4: 'info' }
  return map[type] || ''
}

const fetchData = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      type: filterType.value,
      isRead: filterRead.value
    }
    const res = await getNotificationList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const openSendDialog = async () => {
  sendDialogVisible.value = true
  try {
    const [tplRes, userRes] = await Promise.all([getEnabledTemplates(), getUserList({ page: 1, size: 100 })])
    templates.value = tplRes.data || []
    userList.value = userRes.data.records || []
  } catch (e) {
    console.error('[Announcement] 获取数据失败', e)
  }
}

const onTemplateChange = (tpl) => {
  if (tpl) {
    sendForm.value.type = tpl.type
    sendForm.value.title = tpl.title
    sendForm.value.content = tpl.content
  }
}

const resetSendForm = () => {
  sendForm.value = {
    type: null,
    title: '',
    content: '',
    sendToAll: true,
    userIds: [],
    extraData: ''
  }
  selectedTemplate.value = null
}

const submitSend = async () => {
  if (!sendFormRef.value) return
  await sendFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (!sendForm.value.sendToAll && (!sendForm.value.userIds || sendForm.value.userIds.length === 0)) {
      ElMessage.warning('请选择接收用户')
      return
    }

    sendLoading.value = true
    try {
      await sendAnnouncement(sendForm.value)
      ElMessage.success('发送成功')
      sendDialogVisible.value = false
      fetchData()
    } finally {
      sendLoading.value = false
    }
  })
}

const viewDetail = (row) => {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.detail-content {
  padding: 8px 0;
}
.detail-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  line-height: 1.6;
}
.label {
  color: #666;
  min-width: 80px;
  flex-shrink: 0;
}
.value {
  color: #333;
  flex: 1;
}
.detail-content-text {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
