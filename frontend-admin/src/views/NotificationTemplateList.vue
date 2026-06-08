<template>
  <div class="page-container">
    <div class="page-header">
      <h2>通知模板</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建模板
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
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="fetchData">查询</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" empty-text="暂无模板数据">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="模板编码" prop="code" width="180" />
        <el-table-column label="模板名称" prop="name" width="160" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="内容" prop="content" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updatedAt" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openEditDialog(row)">编辑</el-button>
            <el-button type="warning" size="small" text @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 创建/编辑模板弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新建模板'" width="600px" @close="resetForm">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="模板编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入模板编码" :disabled="isEdit" maxlength="50" />
        </el-form-item>
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入模板名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择消息类型" style="width: 100%">
            <el-option label="系统通知" :value="1" />
            <el-option label="审核结果" :value="2" />
            <el-option label="计划提醒" :value="3" />
            <el-option label="小组动态" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="5" placeholder="请输入内容，支持占位符如 {变量名}" maxlength="2000" show-word-limit />
          <div class="form-tip">提示：使用 {变量名} 作为占位符，发送时会被替换为实际内容</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTemplateList,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  toggleTemplateStatus
} from '@/api/notification'

const tableData = ref([])
const loading = ref(false)
const filterType = ref(null)
const filterStatus = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const submitLoading = ref(false)
const formData = ref({
  code: '',
  name: '',
  type: null,
  title: '',
  content: '',
  status: 1
})
const formRules = {
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

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
      status: filterStatus.value
    }
    const res = await getTemplateList(params)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    const elapsed = Date.now() - start
    if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  editId.value = null
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  formData.value = {
    code: row.code,
    name: row.name,
    type: row.type,
    title: row.title,
    content: row.content,
    status: row.status
  }
  dialogVisible.value = true
}

const resetForm = () => {
  formData.value = {
    code: '',
    name: '',
    type: null,
    title: '',
    content: '',
    status: 1
  }
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateTemplate(editId.value, formData.value)
        ElMessage.success('更新成功')
      } else {
        await createTemplate(formData.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } finally {
      submitLoading.value = false
    }
  })
}

const handleToggleStatus = async (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}该模板？`, '提示', { type: 'warning' })
  try {
    await toggleTemplateStatus(row.id)
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('[Template] 切换状态失败', e)
    }
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该模板？删除后无法恢复', '提示', { type: 'warning' })
  try {
    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('[Template] 删除失败', e)
    }
  }
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
.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
