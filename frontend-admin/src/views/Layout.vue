<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar" :class="{ 'is-collapsed': isCollapsed }">
      <div class="sidebar-logo">
        <span class="logo-icon">📚</span>
        <span v-if="!isCollapsed" class="logo-text">小安的书店</span>
      </div>
      <el-menu :default-active="activeMenu" router background-color="#6B4226" text-color="#e2c9b0"
        active-text-color="#ffffff" class="sidebar-menu" :collapse="isCollapsed">
        <el-menu-item v-if="userStore.hasPermission('dashboard:view')" index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title><span>仪表盘</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('user:view')" index="/users">
          <el-icon><User /></el-icon>
          <template #title><span>用户管理</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('book:view')" index="/books">
          <el-icon><Document /></el-icon>
          <template #title><span>书籍管理</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('admin:view')" index="/admins">
          <el-icon><UserFilled /></el-icon>
          <template #title><span>管理员管理</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('role:view')" index="/roles">
          <el-icon><Lock /></el-icon>
          <template #title><span>角色权限</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('log:view')" index="/logs">
          <el-icon><Notebook /></el-icon>
          <template #title><span>操作日志</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('complaint:view')" index="/complaints">
          <el-icon><Warning /></el-icon>
          <template #title><span>版权申诉</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('audit:report')" index="/compliance">
          <el-icon><ShieldCheck /></el-icon>
          <template #title><span>合规审计</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('notification:view')" index="/notifications">
          <el-icon><Bell /></el-icon>
          <template #title><span>公告管理</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('template:view')" index="/notification-templates">
          <el-icon><DocumentCopy /></el-icon>
          <template #title><span>通知模板</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('backup:view') || userStore.isSuperAdmin" index="/backups">
          <el-icon><Folder /></el-icon>
          <template #title><span>备份管理</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('storage:view') || userStore.isSuperAdmin" index="/storage">
          <el-icon><Files /></el-icon>
          <template #title><span>存储统计</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('plan:view')" index="/membership-plans">
          <el-icon><PriceTag /></el-icon>
          <template #title><span>套餐配置</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('order:view')" index="/orders">
          <el-icon><List /></el-icon>
          <template #title><span>订单列表</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('member:view')" index="/members">
          <el-icon><Stamp /></el-icon>
          <template #title><span>会员管理</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('convert:view')" index="/convert-tasks">
          <el-icon><Picture /></el-icon>
          <template #title><span>转图任务</span></template>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('ai_summary:view')" index="/ai-summaries">
          <el-icon><MagicStick /></el-icon>
          <template #title><span>AI摘要</span></template>
        </el-menu-item>
        <el-sub-menu v-if="userStore.hasPermission('plaza:view') || userStore.hasPermission('plaza:report_view')" index="/plaza">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>书摘广场</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('plaza:view')" index="/plaza/excerpts">书摘管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('plaza:report_view')" index="/plaza/reports">举报管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="top-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <span class="user-name">{{ userStore.nickname }}</span>
          <el-button text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleResize = () => {
  if (window.innerWidth < 768) {
    isCollapsed.value = true
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

const handleLogout = async () => {
  await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
  userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background: #6B4226;
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  transition: width 0.3s ease;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  white-space: nowrap;
  overflow: hidden;

  .logo-icon { font-size: 24px; }
  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: #fff;
  }
}

.sidebar-menu {
  border-right: none !important;

  &:not(.el-menu--collapse) {
    width: 220px;
  }

  .el-menu-item {
    height: 50px;
    line-height: 50px;
    margin: 4px 8px;
    border-radius: 8px;

    &.is-active {
      background: rgba(255, 255, 255, 0.15) !important;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.1) !important;
    }
  }
}

.is-collapsed {
  .sidebar-menu {
    .el-menu-item {
      margin: 4px 0;
      border-radius: 0;
      display: flex;
      justify-content: center;
      padding: 0 !important;
    }
  }
}

.top-header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #666;
    transition: color 0.2s;

    &:hover {
      color: #6B4226;
    }
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .user-name {
      font-size: 14px;
      color: #666;
    }
  }
}

.main-content {
  background: #f5f0eb;
  padding: 24px;
  height: calc(100vh - 64px);
  overflow-y: auto;
}

@media (max-width: 768px) {
  .top-header {
    padding: 0 12px;
  }

  .main-content {
    padding: 12px;
  }

  .page-title {
    font-size: 16px !important;
  }

  .user-name {
    display: none;
  }
}
</style>
